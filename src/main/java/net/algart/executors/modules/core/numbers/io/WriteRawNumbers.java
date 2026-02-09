/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jakarta.json.*;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.WriteFileOperation;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.json.Jsons;

import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WriteRawNumbers extends WriteFileOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_COLUMN_NAMES = "column_names";
    public static final String METADATA_FILE_SUFFIX = ".meta";

    public enum ByteOrder {
        BIG_ENDIAN(java.nio.ByteOrder.BIG_ENDIAN),
        LITTLE_ENDIAN(java.nio.ByteOrder.LITTLE_ENDIAN),
        NATIVE(java.nio.ByteOrder.nativeOrder());

        private final java.nio.ByteOrder order;

        private static final String BIG_ENDIAN_NAME = java.nio.ByteOrder.BIG_ENDIAN.toString();
        private static final String LITTLE_ENDIAN_NAME = java.nio.ByteOrder.LITTLE_ENDIAN.toString();

        ByteOrder(java.nio.ByteOrder order) {
            this.order = order;
        }

        public java.nio.ByteOrder order() {
            return order;
        }

        public static java.nio.ByteOrder toByteOrder(String name) {
            Objects.requireNonNull(name, "Null name");
            if (BIG_ENDIAN_NAME.equals(name)) {
                return java.nio.ByteOrder.BIG_ENDIAN;
            }
            if (LITTLE_ENDIAN_NAME.equals(name)) {
                return java.nio.ByteOrder.LITTLE_ENDIAN;
            }
            throw new IllegalArgumentException("Illegal byte order: " + name);
        }
    }

    private boolean inputRequired = false;
    private boolean clearFileOnReset = false;
    private boolean appendToExistingFile = false;
    // - if true, the results are appended after the end of the file if it exists
    private boolean deleteFileIfNonInitialized = false;
    private String copyOfPreviousFileIfNonInitialized = DEFAULT_EMPTY_FILE;
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private boolean writeMetadataFile = false;
    private IndexingBase columnsIndexingBaseInMetadata = IndexingBase.ZERO_BASED;
    private String columnNames = "";

    public WriteRawNumbers() {
        addFileOperationPorts();
        addInputNumbers(DEFAULT_INPUT_PORT);
        addInputScalar(INPUT_COLUMN_NAMES);
    }

    public static WriteRawNumbers getInstance() {
        return new WriteRawNumbers();
    }

    @Override
    public WriteRawNumbers setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public WriteRawNumbers setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    public boolean isClearFileOnReset() {
        return clearFileOnReset;
    }

    public WriteRawNumbers setClearFileOnReset(boolean clearFileOnReset) {
        this.clearFileOnReset = clearFileOnReset;
        return this;
    }

    public boolean isAppendToExistingFile() {
        return appendToExistingFile;
    }

    public WriteRawNumbers setAppendToExistingFile(boolean appendToExistingFile) {
        this.appendToExistingFile = appendToExistingFile;
        return this;
    }

    public boolean isDeleteFileIfNonInitialized() {
        return deleteFileIfNonInitialized;
    }

    public WriteRawNumbers setDeleteFileIfNonInitialized(boolean deleteFileIfNonInitialized) {
        this.deleteFileIfNonInitialized = deleteFileIfNonInitialized;
        return this;
    }

    public String getCopyOfPreviousFileIfNonInitialized() {
        return copyOfPreviousFileIfNonInitialized;
    }

    public WriteRawNumbers setCopyOfPreviousFileIfNonInitialized(String copyOfPreviousFileIfNonInitialized) {
        this.copyOfPreviousFileIfNonInitialized = nonNull(copyOfPreviousFileIfNonInitialized);
        return this;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public WriteRawNumbers setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = nonNull(byteOrder);
        return this;
    }

    public boolean isWriteMetadataFile() {
        return writeMetadataFile;
    }

    public WriteRawNumbers setWriteMetadataFile(boolean writeMetadataFile) {
        this.writeMetadataFile = writeMetadataFile;
        return this;
    }

    public IndexingBase getColumnsIndexingBaseInMetadata() {
        return columnsIndexingBaseInMetadata;
    }

    public WriteRawNumbers setColumnsIndexingBaseInMetadata(IndexingBase columnsIndexingBaseInMetadata) {
        this.columnsIndexingBaseInMetadata = nonNull(columnsIndexingBaseInMetadata);
        return this;
    }

    public String getColumnNames() {
        return columnNames;
    }

    public WriteRawNumbers setColumnNames(String columnNames) {
        this.columnNames = nonNull(columnNames);
        return this;
    }

    public String columnNamesOrNull() {
        final String result = columnNames.trim();
        return result.isEmpty() ? null : result;
    }

    @Override
    public void initialize() {
        if (clearFileOnReset) {
            completeFilePath().toFile().delete();
        }
    }

    @Override
    public void process() {
        final SNumbers numbers = getInputNumbers(deleteFileIfNonInitialized || !inputRequired);
        if (inputRequired || numbers.isInitialized()) {
            writeRaw(numbers);
        }
    }

    public void writeRaw(SNumbers numbers) {
        final Path rawFile = completeFilePath();
        final Path metadataFile = Paths.get(rawFile + METADATA_FILE_SUFFIX);
        try {
            final boolean exists = Files.exists(rawFile);
            if (!numbers.isInitialized() && deleteFileIfNonInitialized) {
                if (exists) {
                    final String copyOfPreviousFile = this.copyOfPreviousFileIfNonInitialized.trim();
                    if (!copyOfPreviousFile.isEmpty()) {
                        Files.copy(rawFile, Paths.get(copyOfPreviousFile), StandardCopyOption.REPLACE_EXISTING);
                    }
                    logDebug(() -> "Removing file " + rawFile.toAbsolutePath());
                    Files.delete(rawFile);
                    if (writeMetadataFile && Files.exists(metadataFile)) {
                        logDebug(() -> "Removing metadata file " + metadataFile.toAbsolutePath());
                        Files.delete(metadataFile);
                    }
                }
            } else {
                logDebug(
                        () -> "Writing number array (" + numbers + ") to file " + rawFile.toAbsolutePath());
                try (final FileOutputStream stream = new FileOutputStream(rawFile.toFile(), appendToExistingFile)) {
                    writeRaw(stream, numbers);
                }
                if (writeMetadataFile) {
                    String columnNames = getInputScalar(INPUT_COLUMN_NAMES, true).getValue();
                    if (columnNames == null) {
                        columnNames = columnNamesOrNull();
                    }
                    final String[] columnNamesArray = columnNames == null ?
                            null :
                            SScalar.splitJsonOrTrimmedLinesWithoutCommentsArray(columnNames);
                    final JsonObject metadata = createMetadata(numbers, columnNamesArray);
                    logDebug(() -> "Writing metadata to file " + metadataFile.toAbsolutePath());
                    Files.writeString(metadataFile, Jsons.toPrettyString(metadata));
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void writeRaw(FileOutputStream outputStream, SNumbers numbers) throws IOException {
        Objects.requireNonNull(numbers, "Null numbers");
        Objects.requireNonNull(outputStream, "Null outputStream argument");
        Objects.requireNonNull(numbers, "Null numbers argument");
        outputStream.getChannel().write(numbers.toByteBuffer(byteOrder.order()));
    }

    public JsonObject createMetadata(SNumbers numbers, String[] columnNames) {
        return createMetadata(numbers, byteOrder, columnNames);
    }

    public JsonObject createMetadata(SNumbers numbers, ByteOrder byteOrder, String[] columnNames) {
        Objects.requireNonNull(numbers, "Null numbers");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("blockLength", numbers.getBlockLength());
        builder.add("n", numbers.n());
        builder.add("elementType", numbers.elementType().getSimpleName());
        builder.add("byteOrder", byteOrder.name());
        builder.add("order", byteOrder.order().toString());
        if (columnNames != null) {
            final JsonArrayBuilder columnNamesBuilder = Json.createArrayBuilder();
            final JsonObjectBuilder columnIndexesBuilder = Json.createObjectBuilder();
            for (int k = 0; k < columnNames.length; k++) {
                columnNamesBuilder.add(columnNames[k]);
                columnIndexesBuilder.add(columnNames[k], columnsIndexingBaseInMetadata.start + k);
            }
            builder.add("columnNames", columnNamesBuilder.build());
            builder.add("columnIndexes", columnIndexesBuilder.build());
        }
        return builder.build();
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT)
                .addPorts(getInputPort(INPUT_COLUMN_NAMES));
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }

    public static int getMetadataBlockLength(JsonObject metadata) {
        return Jsons.reqInt(metadata, "blockLength");
    }

    public static Class<?> getMetadataElementType(JsonObject metadata) {
        final String elementTypeName = Jsons.reqString(metadata, "elementType");
        for (Class<?> elementType : SNumbers.SUPPORTED_ELEMENT_TYPES) {
            if (elementType.getSimpleName().equals(elementTypeName)) {
                return elementType;
            }
        }
        throw new JsonException("Unknown element type \"" + elementTypeName + "\"");
    }

    public static java.nio.ByteOrder getMetadataByteOrder(JsonObject metadata) {
        return ByteOrder.toByteOrder(Jsons.reqString(metadata, "order"));
    }

    public static List<String> getMetadataColumnNames(JsonObject metadata) {
        final JsonArray columnNames = metadata.getJsonArray("columnNames");
        if (columnNames == null) {
            return null;
        }
        return columnNames.stream().map(jsonValue -> {
            if (!(jsonValue instanceof JsonString)) {
                throw new JsonException("Invalid type \"" + jsonValue.getValueType()
                        + "\" of header \"" + jsonValue + "\"");
            }
            return ((JsonString) jsonValue).getString();
        }).collect(Collectors.toList());
    }

    public static JsonObject getMetadataColumnIndexes(JsonObject metadata) {
        return metadata.getJsonObject("columnIndexes");
    }
}
