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

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

public final class WriteCSVNumbers extends WriteFileOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_HEADERS = "headers";

    public enum LineDelimiter {
        CRLF("\r\n"),
        LF("\n"),
        SYSTEM(String.format("%n"));

        private final String delimiter;

        LineDelimiter(String delimiter) {
            this.delimiter = delimiter;
        }
    }

    private boolean requireInput = false;
    private boolean clearFileOnReset = false;
    private boolean appendToExistingFile = false;
    // - if true, the results are appended after the end of the file if it exists
    private boolean deleteFileIfNonInitialized = false;
    private String copyOfPreviousFileIfNonInitialized = DEFAULT_EMPTY_FILE;
    private String delimiter = ",";
    private LineDelimiter lineDelimiter = LineDelimiter.CRLF;
    private String format = "";
    private boolean simpleFormatForIntegers = true;

    public WriteCSVNumbers() {
        addFileOperationPorts();
        addInputNumbers(DEFAULT_INPUT_PORT);
        addInputScalar(INPUT_HEADERS);
    }

    public static WriteCSVNumbers getInstance() {
        return new WriteCSVNumbers();
    }

    @Override
    public WriteCSVNumbers setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public WriteCSVNumbers setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    public boolean isClearFileOnReset() {
        return clearFileOnReset;
    }

    public WriteCSVNumbers setClearFileOnReset(boolean clearFileOnReset) {
        this.clearFileOnReset = clearFileOnReset;
        return this;
    }

    public boolean isAppendToExistingFile() {
        return appendToExistingFile;
    }

    public WriteCSVNumbers setAppendToExistingFile(boolean appendToExistingFile) {
        this.appendToExistingFile = appendToExistingFile;
        return this;
    }

    public boolean isDeleteFileIfNonInitialized() {
        return deleteFileIfNonInitialized;
    }

    public WriteCSVNumbers setDeleteFileIfNonInitialized(boolean deleteFileIfNonInitialized) {
        this.deleteFileIfNonInitialized = deleteFileIfNonInitialized;
        return this;
    }

    public String getCopyOfPreviousFileIfNonInitialized() {
        return copyOfPreviousFileIfNonInitialized;
    }

    public WriteCSVNumbers setCopyOfPreviousFileIfNonInitialized(String copyOfPreviousFileIfNonInitialized) {
        this.copyOfPreviousFileIfNonInitialized = nonNull(copyOfPreviousFileIfNonInitialized);
        return this;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public WriteCSVNumbers setDelimiter(String delimiter) {
        this.delimiter = nonEmpty(delimiter);
        return this;
    }

    public LineDelimiter getLineDelimiter() {
        return lineDelimiter;
    }

    public WriteCSVNumbers setLineDelimiter(LineDelimiter lineDelimiter) {
        this.lineDelimiter = nonNull(lineDelimiter);
        return this;
    }

    public String getFormat() {
        return format;
    }

    public WriteCSVNumbers setFormat(String format) {
        this.format = nonNull(format).trim();
        return this;
    }

    public boolean isSimpleFormatForIntegers() {
        return simpleFormatForIntegers;
    }

    public WriteCSVNumbers setSimpleFormatForIntegers(boolean simpleFormatForIntegers) {
        this.simpleFormatForIntegers = simpleFormatForIntegers;
        return this;
    }

    @Override
    public void initialize() {
        if (clearFileOnReset) {
            try {
                Files.deleteIfExists(completeFilePath());
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
    }

    @Override
    public void process() {
        final SNumbers numbers = getInputNumbers(deleteFileIfNonInitialized || !requireInput);
        if (requireInput || numbers.isInitialized()) {
            final SScalar inputHeader = getInputScalar(INPUT_HEADERS, true);
            writeCSV(numbers, inputHeader.toTrimmedLinesWithoutCommentsArray());
        }
    }

    public void writeCSV(SNumbers numbers, String[] headers) {
        Objects.requireNonNull(numbers, "Null numbers");
        final Path csvFile = completeFilePath();
        try {
            final boolean exists = Files.exists(csvFile);
            if (!numbers.isInitialized() && deleteFileIfNonInitialized) {
                if (exists) {
                    final String copyOfPreviousFile = this.copyOfPreviousFileIfNonInitialized.trim();
                    if (!copyOfPreviousFile.isEmpty()) {
                        Files.copy(csvFile, Paths.get(copyOfPreviousFile), StandardCopyOption.REPLACE_EXISTING);
                    }
                    logDebug(() -> "Removing file " + csvFile.toAbsolutePath());
                    Files.delete(csvFile);
                }
            } else {
                logDebug(() -> "Writing number array (" + numbers + ") to file "
                        + csvFile.toAbsolutePath());
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(csvFile.toFile(), appendToExistingFile),
                        StandardCharsets.UTF_8))) {
                    writeCSV(writer, numbers, headers, !(exists && appendToExistingFile));
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT)
                .addPorts(getInputPort(INPUT_HEADERS));
    }

    public void writeCSV(Writer writer, SNumbers numbers, String[] headers, boolean addHeader)
            throws IOException {
        Objects.requireNonNull(writer, "Null writer argument");
        Objects.requireNonNull(numbers, "Null numbers argument");
        if (headers == null) {
            headers = new String[0];
        }
        if (addHeader) {
            writer.write(makeHeader(numbers, headers));
        }
        final SNumbers.Formatter formatter = numbers.getFormatter(
                        format.isEmpty() ? SNumbers.FormattingType.SIMPLE : SNumbers.FormattingType.PRINTF,
                        Locale.US)
                .setSimpleFormatForIntegers(simpleFormatForIntegers)
                .setLinesDelimiter(lineDelimiter.delimiter)
                .setElementsFormat(format)
                .setElementsDelimiter(delimiter);
        writer.write(formatter.format());
//        for (int i = 0, n = numbers.n(); i < n; i++) {
//            writer.write(makeLine(numbers, i));
//        }
        writer.flush();
    }

    private String makeHeader(SNumbers numbers, String[] headers) {
        final StringBuilder sb = new StringBuilder();
        for (int j = 0, blockLength = numbers.getBlockLength(); j < blockLength; j++) {
            if (j > 0) {
                sb.append(delimiter);
            }
            if (j < headers.length) {
                sb.append(headers[j].trim());
            } else {
                sb.append(numbers.elementType()).append("_").append(j + 1);
            }
        }
        sb.append(lineDelimiter.delimiter);
        return sb.toString();
    }

    private String makeLine(SNumbers numbers, int k) {
        final StringBuilder sb = new StringBuilder();
        for (int j = 0, blockLength = numbers.getBlockLength(); j < blockLength; j++) {
            if (j > 0) {
                sb.append(delimiter);
            }
            final double v = numbers.getValue(k, j);
            if (simpleFormatForIntegers && v == (long) v) {
                sb.append((long) v);
            } else if (!format.isEmpty()) {
                sb.append(String.format(Locale.US, format, v));
            } else {
                sb.append(v);
                // - standard Java representation with decimal point '.' (not ',')
            }
        }
        sb.append(lineDelimiter.delimiter);
        return sb.toString();
    }
}
