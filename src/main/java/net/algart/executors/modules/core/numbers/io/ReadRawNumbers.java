/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.executors.modules.core.numbers.io;

import jakarta.json.JsonObject;
import net.algart.arrays.Arrays;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.json.Jsons;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public final class ReadRawNumbers extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_COLUMN_NAMES = "column_names";
    public static final String OUTPUT_COLUMN_INDEXES = "column_indexes";

    private int blockLength = 1;
    private Class<?> elementType = float.class;
    private WriteRawNumbers.ByteOrder byteOrder = WriteRawNumbers.ByteOrder.BIG_ENDIAN;
    private boolean readMetadataFile = true;

    public ReadRawNumbers() {
        addFileOperationPorts();
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_COLUMN_NAMES);
        addOutputScalar(OUTPUT_COLUMN_INDEXES);
    }

    public static ReadRawNumbers getInstance() {
        return new ReadRawNumbers();
    }

    public static ReadRawNumbers getSecureInstance() {
        final ReadRawNumbers result = new ReadRawNumbers();
        result.setSecure(true);
        return result;
    }

    @Override
    public ReadRawNumbers setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public ReadRawNumbers setFile(Path file) {
        super.setFile(file);
        return this;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public ReadRawNumbers setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public ReadRawNumbers setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
        return this;
    }

    public ReadRawNumbers setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public WriteRawNumbers.ByteOrder getByteOrder() {
        return byteOrder;
    }

    public ReadRawNumbers setByteOrder(WriteRawNumbers.ByteOrder byteOrder) {
        this.byteOrder = nonNull(byteOrder);
        return this;
    }

    public boolean isReadMetadataFile() {
        return readMetadataFile;
    }

    public ReadRawNumbers setReadMetadataFile(boolean readMetadataFile) {
        this.readMetadataFile = readMetadataFile;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            final SNumbers result = readRaw();
            if (result != null) {
                getNumbers().setTo(result);
            } // in another case, stay non-initialized output container
        }
    }

    public SNumbers readRaw() {
        final Path rawFile = completeFilePath();
        final Path metadataFile = Paths.get(rawFile + WriteRawNumbers.METADATA_FILE_SUFFIX);
        try {
            if (skipIfMissingOrThrow(rawFile)) {
                return null;
            }
            JsonObject metadata = null;
            if (readMetadataFile) {
                if (!Files.exists(metadataFile)) {
                    throw new FileNotFoundException("Metadata file not found: " + metadataFile.toAbsolutePath());
                }
                logDebug(() -> "Reading metadata file " + metadataFile.toAbsolutePath());
                metadata = Jsons.readJson(metadataFile);
                final List<String> columnNames = WriteRawNumbers.getMetadataColumnNames(metadata);
                if (columnNames != null) {
                    getScalar(OUTPUT_COLUMN_NAMES).setTo(String.join("\n", columnNames));
                }
                final JsonObject columnIndexes = WriteRawNumbers.getMetadataColumnIndexes(metadata);
                if (columnIndexes != null) {
                    getScalar(OUTPUT_COLUMN_INDEXES).setTo(Jsons.toPrettyString(columnIndexes));
                }
            }
            logDebug(() -> "Reading number array from " + rawFile.toAbsolutePath());
            try (final FileInputStream stream = new FileInputStream(rawFile.toFile())) {
                SNumbers result;
                try {
                    result = readRaw(stream, metadata, rawFile.toAbsolutePath().toString());
                } catch (RuntimeException e) {
                    throw new IOException("Cannot load numbers from file " + rawFile + ": " + e.getMessage(), e);
                }
                return result;
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public SNumbers readRaw(FileInputStream inputStream, JsonObject metadata) throws IOException {
        return readRaw(inputStream, metadata, null);
    }

    public static SNumbers readRaw(
            FileInputStream inputStream,
            ByteOrder byteOrder,
            Class<?> elementType,
            int blockLength) throws IOException {
        return readRaw(inputStream, byteOrder, elementType, blockLength, null);
    }

    private SNumbers readRaw(FileInputStream inputStream, JsonObject metadata, String fileName) throws IOException {
        return metadata == null ?
                readRaw(inputStream, byteOrder.order(), elementType, blockLength, fileName) :
                readRaw(
                        inputStream,
                        WriteRawNumbers.getMetadataByteOrder(metadata),
                        WriteRawNumbers.getMetadataElementType(metadata),
                        WriteRawNumbers.getMetadataBlockLength(metadata),
                        fileName);
    }

    private static SNumbers readRaw(
            FileInputStream inputStream,
            ByteOrder byteOrder,
            Class<?> elementType,
            int blockLength,
            String fileName) throws IOException {
        Objects.requireNonNull(inputStream, "Null outputStream argument");
        Objects.requireNonNull(byteOrder, "Null byteOrder");
        Objects.requireNonNull(elementType, "Null elementType");
        if (!SNumbers.isElementTypeSupported(elementType)) {
            throw new IllegalArgumentException("Unsupported element type " + elementType);
        }
        fileName = fileName == null ? "" : "\"" + fileName + "\" ";
        final FileChannel channel = inputStream.getChannel();
        final long size = channel.size();
        if (size > Integer.MAX_VALUE) {
            throw new IOException("Cannot read too large file " + fileName +
                    "to SNumbers: it's size " + size + " >= 2^31");
        }
        final int bitsPerElement = (int) Arrays.bitsPerElement(elementType);
        if (bitsPerElement < 8) {
            throw new AssertionError("SNumbers.isElementTypeSupported(" + elementType +
                    ") must be false! (bitsPerElement=" + bitsPerElement + ")");
        }
        final int bytesPerElement = bitsPerElement / 8;
        if (size % (bytesPerElement * (long) blockLength) != 0) {
            throw new IOException("The size " + size + " of the file " + fileName +
                    "is not a multiple of " +
                    "(block length) * (bytes per element) = " + blockLength + " * " + bytesPerElement +
                    ", probably block length (" + blockLength + ") or element type (" + elementType + ") is invalid");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) size);
        byteBuffer.order(byteOrder);
        channel.read(byteBuffer);
        return new SNumbers().setTo(byteBuffer, elementType, blockLength);
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getOutputPort(OUTPUT_COLUMN_NAMES));
    }
}
