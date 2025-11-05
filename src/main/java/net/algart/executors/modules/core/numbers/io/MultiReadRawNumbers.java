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

import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.io.FileOperation;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class MultiReadRawNumbers extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_COLUMN_NAMES = ReadRawNumbers.OUTPUT_COLUMN_NAMES;
    public static final String OUTPUT_COLUMN_INDEXES = ReadRawNumbers.OUTPUT_COLUMN_INDEXES;

    private String globPattern = "*.dat";
    private int blockLength = 1;
    private Class<?> elementType = float.class;
    private WriteRawNumbers.ByteOrder byteOrder = WriteRawNumbers.ByteOrder.BIG_ENDIAN;
    private boolean readMetadataFile = false;

    public MultiReadRawNumbers() {
        addFileOperationPorts();
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_COLUMN_NAMES);
        addOutputScalar(OUTPUT_COLUMN_INDEXES);
    }

    public static MultiReadRawNumbers getInstance() {
        return new MultiReadRawNumbers();
    }

    @Override
    public MultiReadRawNumbers setFile(String file) {
        super.setFile(file);
        return this;
    }

    public String getGlobPattern() {
        return globPattern;
    }

    public MultiReadRawNumbers setGlobPattern(String globPattern) {
        this.globPattern = nonEmpty(globPattern);
        return this;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public MultiReadRawNumbers setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public MultiReadRawNumbers setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
        return this;
    }

    public MultiReadRawNumbers setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public WriteRawNumbers.ByteOrder getByteOrder() {
        return byteOrder;
    }

    public MultiReadRawNumbers setByteOrder(WriteRawNumbers.ByteOrder byteOrder) {
        this.byteOrder = nonNull(byteOrder);
        return this;
    }

    public boolean isReadMetadataFile() {
        return readMetadataFile;
    }

    public MultiReadRawNumbers setReadMetadataFile(boolean readMetadataFile) {
        this.readMetadataFile = readMetadataFile;
        return this;
    }

    @Override
    public void process() {
        final Path path = completeFilePath().toAbsolutePath();
        final ReadRawNumbers readRawNumbers = ReadRawNumbers.getInstance();
        readRawNumbers.setBlockLength(blockLength)
                .setElementType(elementType)
                .setByteOrder(byteOrder)
                .setReadMetadataFile(readMetadataFile)
                .setFileExistenceRequired(true);
        final Accumulator accumulator = new Accumulator(file -> readRawNumbers.setFile(file).readRaw());
        processFiles(path, globPattern, accumulator);
        final SNumbers result = accumulator.join();
        if (result != null) {
            getNumbers().exchange(result);
        }
        getScalar(OUTPUT_COLUMN_INDEXES).exchange(readRawNumbers.getScalar(OUTPUT_COLUMN_INDEXES));
        getScalar(OUTPUT_COLUMN_NAMES).exchange(readRawNumbers.getScalar(OUTPUT_COLUMN_NAMES));
    }

    static void processFiles(Path path, String globPattern, Accumulator accumulator) {
        if (Files.isRegularFile(path)) {
            accumulator.processFile(path);
        } else if (Files.isDirectory(path)) {
            final List<Path> files = new ArrayList<>();
            final PathMatcher matcher = path.getFileSystem().getPathMatcher("glob:" + globPattern);
            try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(path)) {
                for (Path f : fileStream) {
                    if (Files.isDirectory(f) || (Files.isRegularFile(f) && matcher.matches(f.getFileName()))) {
                        files.add(f);
                    }
                }
            } catch (IOException e) {
                throw new IOError(e);
            }
            Collections.sort(files);
            for (Path f : files) {
                if (Files.isRegularFile(f)) {
                    accumulator.processFile(f);
                }
            }
            for (Path subfolder : files) {
                if (Files.isDirectory(subfolder)) {
                    processFiles(subfolder, globPattern, accumulator);
                }
            }
        }
    }

    static class Accumulator {
        private final Function<Path, SNumbers> readFunction;

        List<SNumbers> numbersList = new ArrayList<>();
        private Path firstFile = null;
        private SNumbers firstNumbers = null;
        private long totalLength = 0;
        private int n = 0;

        Accumulator(Function<Path, SNumbers> readFunction) {
            this.readFunction = Objects.requireNonNull(readFunction);
        }

        void processFile(Path file) {
            final SNumbers numbers = readFunction.apply(file);
            assert numbers != null : "ReadRawNumbers must not return null when fileExistenceRequired=true";
            if (firstNumbers == null) {
                firstFile = file;
                firstNumbers = numbers;
            } else {
                if (firstNumbers.getBlockLength() != numbers.getBlockLength()) {
                    throw new IllegalArgumentException("Different block length in the source number arrays: "
                            + numbers.getBlockLength() + " (file " + file + ") != "
                            + firstNumbers.getBlockLength() + " (file " + firstFile + ")");
                }
                if (firstNumbers.elementType() != numbers.elementType()) {
                    throw new IllegalArgumentException("Different element type in the source number arrays: "
                            + numbers.elementType() + " (file " + file + ") != "
                            + firstNumbers.elementType() + " (file " + firstFile + ")");
                }
            }
            totalLength += numbers.getArrayLength();
            if (totalLength > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large summary number of elements in " + numbersList.size()
                        + "arrays: >2^31-1");
            }
            n += numbers.n();
            numbersList.add(numbers);
        }

        SNumbers join() {
            if (numbersList.isEmpty()) {
                return null;
            }
            final SNumbers result = SNumbers.zeros(firstNumbers.elementType(), n, firstNumbers.getBlockLength());
            for (int k = 0, m = numbersList.size(), disp = 0; k < m; k++) {
                final SNumbers numbers = numbersList.get(k);
                final int length = numbers.n();
                result.replaceBlockRange(disp, numbers, 0, length);
                disp += length;
            }
            return result;
        }
    }
}
