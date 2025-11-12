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

import net.algart.arrays.MutablePArray;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.io.FileOperation;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReadCSVNumbers extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_HEADERS = "headers";
    public static final String AUTO_ELEMENT_TYPE = "auto";

    private static final Charset[] POSSIBLE_CSV_CHARSETS = {
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_8,
    };

    private Class<?> elementType = null;
    private int numberOfSkippedInitialLines = 0;

    public ReadCSVNumbers() {
        addFileOperationPorts();
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_HEADERS);
    }

    public static ReadCSVNumbers getInstance() {
        return new ReadCSVNumbers();
    }

    public static ReadCSVNumbers getSecureInstance() {
        final ReadCSVNumbers result = new ReadCSVNumbers();
        result.setSecure(true);
        return result;
    }

    @Override
    public ReadCSVNumbers setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public ReadCSVNumbers setFile(Path file) {
        super.setFile(file);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public ReadCSVNumbers setElementType(Class<?> elementType) {
        this.elementType = elementType;
        return this;
    }

    public ReadCSVNumbers setElementType(String elementType) {
        return setElementType(AUTO_ELEMENT_TYPE.equals(elementType) ? null : SNumbers.elementType(elementType));
    }

    public int getNumberOfSkippedInitialLines() {
        return numberOfSkippedInitialLines;
    }

    public ReadCSVNumbers setNumberOfSkippedInitialLines(int numberOfSkippedInitialLines) {
        this.numberOfSkippedInitialLines = numberOfSkippedInitialLines;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            final SNumbers result = readCSV();
            if (result != null) {
                getNumbers().setTo(result);
            } // in another case, stay non-initialized output container
        }
    }

    public SNumbers readCSV() {
        final Path csvFile = completeFilePath();
        try {
            if (skipIfMissingFileOrThrow(csvFile)) {
                return null;
            }
            logDebug(() -> "Reading number array from " + csvFile.toAbsolutePath());
            final List<String> resultHeaders = new ArrayList<>();
            final SNumbers result = readCSV(csvFile, resultHeaders);
            getScalar(OUTPUT_HEADERS).setTo(String.join("\n", resultHeaders));
            return result;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public SNumbers readCSV(Path csvFile) throws IOException {
        return readCSV(csvFile, new ArrayList<>());
    }

    public SNumbers readCSV(Path csvFile, List<String> resultHeaders) throws IOException {
        Objects.requireNonNull(csvFile, "Null file path");
        SNumbers largestResult = null;
        List<String> largestHeaders = null;
        Exception exception = null;
        for (Charset POSSIBLE_CSV_CHARSET : POSSIBLE_CSV_CHARSETS) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(csvFile.toFile()), POSSIBLE_CSV_CHARSET))) {
                final List<String> headers = new ArrayList<>();
                final AtomicBoolean goodHeader = new AtomicBoolean();
                try {
                    final SNumbers numbers = readCSV(reader, headers, goodHeader);
                    if (goodHeader.get()) {
                        // if at least 1 attempt leads to good header without other exception, use it!
                        resultHeaders.clear();
                        resultHeaders.addAll(headers);
                        return numbers;
                    }
                    if (largestResult == null || numbers.getArrayLength() > largestResult.getArrayLength()) {
                        largestResult = numbers;
                        largestHeaders = new ArrayList<>(headers);
                    }
                } catch (IOException | NumberFormatException e) {
                    if (goodHeader.get()) {
                        // if header has been successfully read, this charset is correct, but file is corrupted
                        throw e;
                    }
                    exception = e;
                }
            }
        }
        if (exception == null) {
            assert largestResult != null && largestHeaders != null;
            // use some result, that didn't lead to exception (probably only one from 3)
            resultHeaders.clear();
            resultHeaders.addAll(largestHeaders);
            return largestResult;
        } else {
            if (largestResult != null && largestResult.getArrayLength() > 0) {
                // there was an exception, but with other charset we had successfully read at least 1 number:
                // probably, that charset was correct and we should use that result
                resultHeaders.clear();
                resultHeaders.addAll(largestHeaders);
                return largestResult;
            }
            // all attempts threw exceptions or returned empty array: probably the last exception is actual
            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw (RuntimeException) exception;
            }
        }
    }

    public SNumbers readCSV(BufferedReader reader) throws IOException, NumberFormatException {
        return readCSV(reader, null, new AtomicBoolean());
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getOutputPort(OUTPUT_HEADERS));
    }

    private SNumbers readCSV(BufferedReader reader, List<String> resultHeaders, AtomicBoolean goodHeader)
            throws IOException, NumberFormatException {
        Objects.requireNonNull(reader, "Null reader argument");
        skipBOM(reader);
        goodHeader.set(false);
        for (int k = 0; k < numberOfSkippedInitialLines; k++) {
            final String skipped = reader.readLine();
            if (skipped == null) {
                throw new IOException("Empty CSV file");
            }
        }
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Empty CSV file");
        }
        String[] headers = line.trim().split("([,;\\s]\\s*)");
        final int blockLength = headers.length;
        final Class<?> autoDetectedElementType = parseSNumbersHeader(headers);
        if (autoDetectedElementType != null) {
            goodHeader.set(true);
        }
        final Class<?> elementType = this.elementType != null ? this.elementType :
                autoDetectedElementType != null ? autoDetectedElementType : float.class;
        final double[] singleBlock = new double[blockLength];
        MutablePArray array = MutablePArray.newArray(elementType);
        for (long count = 0; ; count++) {
            final boolean firstLineWithUnknownHeader = count == 0 && !goodHeader.get();
            if (!firstLineWithUnknownHeader) {
                // in another case, maybe the 1st line contains numbers
                line = reader.readLine();
            }
            if (line == null) {
                break;
            }
            final String[] items = line.trim().split("([,;\\s]\\s*)");
            try {
                for (int k = 0; k < singleBlock.length; k++) {
                    singleBlock[k] = k < items.length ? Double.parseDouble(items[k].trim()) : 0.0;
                }
            } catch (NumberFormatException e) {
                if (firstLineWithUnknownHeader) {
                    // it is probably other header line, not in our SNumbers format
                    continue;
                } else {
                    throw e;
                }
            }
            if (firstLineWithUnknownHeader) {
                // it is really numbers, not a header: numbers were successfully parsed
                headers = new String[0];
            }
            final long currentLength = array.length();
            array.length(currentLength + singleBlock.length);
            array.setData(currentLength, SNumbers.ofArray(singleBlock).toPrecision(elementType).getArray());
        }
        if (resultHeaders != null) {
            resultHeaders.clear();
            resultHeaders.addAll(java.util.Arrays.asList(headers));
        }
        return SNumbers.ofArray(array.ja(), blockLength);
    }

    private static void skipBOM(Reader reader) throws IOException {
        reader.mark(1);
        char[] possibleBOM = new char[1];
        reader.read(possibleBOM);
        if (possibleBOM[0] != '\ufeff') {
            reader.reset();
//        } else {
//            System.out.println("SKIPPING BOM!");
        }
    }

    private static Class<?> parseSNumbersHeader(String[] headers) throws IOException {
        if (headers.length == 0) {
            throw new IOException("Invalid CSV header: zero number of columns");
        }
        Class<?> result = null;
        for (Class<?> elementType : SNumbers.SUPPORTED_ELEMENT_TYPES) {
            if (headers[0].startsWith(elementType.getSimpleName())) {
                result = elementType;
                break;
                // - we detect the type on the base of 1st element
            }
        }
        return result;
    }
}
