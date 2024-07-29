/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;

import java.util.*;
import java.util.stream.Collectors;

public final class ExtractNumbersColumnsByNames extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_COLUMN_NAMES = "column_names";
    public static final String OUTPUT_COLUMN_NAMES = "column_names";
    public static final String OUTPUT_COLUMN_PREFIX = "column_";

    private String extractedColumnNames = "";

    public ExtractNumbersColumnsByNames() {
        useVisibleResultParameter();
        setDefaultInputNumbers(DEFAULT_INPUT_PORT);
        addInputScalar(INPUT_COLUMN_NAMES);
        setDefaultOutputNumbers(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_COLUMN_NAMES);
    }

    public String getExtractedColumnNames() {
        return extractedColumnNames;
    }

    public ExtractNumbersColumnsByNames setExtractedColumnNames(String extractedColumnNames) {
        this.extractedColumnNames = nonNull(extractedColumnNames);
        return this;
    }

    @Override
    public void process() {
        final SNumbers source = getInputNumbers();
        final List<String> columnNames = getInputScalar(INPUT_COLUMN_NAMES, true)
                .toTrimmedLinesWithoutComments();
        final List<String> extractedNames = SScalar.splitJsonOrTrimmedLinesWithoutComments(extractedColumnNames);
        final List<SNumbers> resultList = new ArrayList<>();
        for (int k = 0; ; k++) {
            final String portName = outputPortName(k);
            if (!hasOutputPort(portName)) {
                break;
            }
            resultList.add(isOutputNecessary(portName) ? getNumbers(portName) : null);
        }
        final int[] columnIndexes = findColumnIndexes(columnNames, extractedNames, source.blockLength());
        final SNumbers result = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? getNumbers() : null;
        long t1 = debugTime();
        extractColumns(result, resultList, source, columnIndexes);
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Extracting %d columns %s by names from number array %s: "
                        + "%.3f ms (%.1f ns/block)",
                columnIndexes.length,
                extractedNames.stream().collect(Collectors.joining("\", \"", "\"", "\"")),
                source,
                (t2 - t1) * 1e-6, (double) (t2 - t1) / source.n()));
        getScalar(OUTPUT_COLUMN_NAMES).setTo(String.join("\n", extractedNames));
    }

    public int[] findColumnIndexes(List<String> columnNames, List<String> extractedNames, int blockLength) {
        Objects.requireNonNull(extractedNames, "Null extractedNames");
        if (blockLength < 1) {
            throw new IllegalArgumentException("Zero or negative " + blockLength);
        }
        final int[] result = new int[extractedNames.size()];
        int k = 0;
        for (String extractedName : extractedNames) {
            ++k;
            int index;
            try {
                index = Integer.parseInt(extractedName);
            } catch (NumberFormatException ignored) {
                Objects.requireNonNull(columnNames, "No column names parameter, but it is required "
                        + "to find the result column #" + k + " by its name \"" + extractedName + "\"");
                index = columnNames.indexOf(extractedName);
                if (index == -1) {
                    throw new IllegalArgumentException("Name \"" + extractedName + "\" of the result column #"
                            + k + " is not found in the full list of column names");
                }
                index++;
                // - starting from 1 for the following code
            }
            if (index == 0) {
                throw new IllegalArgumentException("Zero index is not allowed (result column #" + k + ")");
            }
            if (Math.abs(index) > blockLength) {
                throw new IllegalArgumentException("Index " + index + " of column \"" + extractedName
                        + "\" is out of range -blockLength..blockLength = -"
                        + blockLength + ".." + blockLength + " (result column #" + k + ")");
            }
            result[k - 1] = index >= 0 ? index - 1 : blockLength + index;
        }
        return result;
    }

    // Note: columnIndexes are already zero-based
    public void extractColumns(SNumbers result, List<SNumbers> resultColumns, SNumbers source, int[] columnIndexes) {
        Objects.requireNonNull(source, "Null source");
        Objects.requireNonNull(columnIndexes, "Null columnIndexes");
        Objects.requireNonNull(resultColumns, "Null resultColumns");
        final SNumbers[] resultColumnArray = resultColumns.toArray(new SNumbers[0]);
        final int n = source.n();
        Object[] javaArraysForResultColumns = new Object[resultColumns.size()];
        Arrays.setAll(javaArraysForResultColumns,
                k -> k >= columnIndexes.length || resultColumnArray[k] == null ?
                        null :
                        source.newCompatibleJavaArray(n));
        source.columnsByIndexes(result, javaArraysForResultColumns, columnIndexes);
        for (int k = 0; k < resultColumnArray.length; k++) {
            if (javaArraysForResultColumns[k] != null) {
                resultColumnArray[k].setToArray(javaArraysForResultColumns[k], 1);
            }
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getOutputPort(OUTPUT_COLUMN_NAMES));
    }

    private String outputPortName(int inputIndex) {
        return OUTPUT_COLUMN_PREFIX + (inputIndex + 1);
    }
}
