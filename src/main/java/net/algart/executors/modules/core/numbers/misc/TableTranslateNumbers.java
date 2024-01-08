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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.numbers.IndexingBase;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TableTranslateNumbers extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_INDEXES = "indexes";
    public static final String INPUT_TABLE_1 = "table";
    public static final String INPUT_TABLE_2 = "table_2";
    public static final String INPUT_TABLE_3 = "table_3";
    public static final String INPUT_TABLE_4 = "table_4";
    public static final String OUTPUT_VALUES_1 = "values";
    public static final String OUTPUT_VALUES_2 = "values_2";
    public static final String OUTPUT_VALUES_3 = "values_3";
    public static final String OUTPUT_VALUES_4 = "values_4";
    public static final String OUTPUT_N = "n";
    public static final String OUTPUT_CHANGED = "changed";

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private Double replacementForNotExisting = null;
    private boolean invertIndexes = false;
    private boolean requireTable = false;

    public TableTranslateNumbers() {
        useVisibleResultParameter();
        setDefaultInputNumbers(INPUT_INDEXES);
        addInputNumbers(INPUT_TABLE_1);
        addInputNumbers(INPUT_TABLE_2);
        addInputNumbers(INPUT_TABLE_3);
        addInputNumbers(INPUT_TABLE_4);
        setDefaultOutputNumbers(OUTPUT_VALUES_1);
        addOutputNumbers(OUTPUT_VALUES_2);
        addOutputNumbers(OUTPUT_VALUES_3);
        addOutputNumbers(OUTPUT_VALUES_4);
        addOutputScalar(OUTPUT_N);
        addOutputScalar(OUTPUT_CHANGED);
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public TableTranslateNumbers setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public Double getReplacementForNotExisting() {
        return replacementForNotExisting;
    }

    public TableTranslateNumbers setReplacementForNotExisting(Double replacementForNotExisting) {
        this.replacementForNotExisting = replacementForNotExisting;
        return this;
    }

    public boolean isInvertIndexes() {
        return invertIndexes;
    }

    public TableTranslateNumbers setInvertIndexes(boolean invertIndexes) {
        this.invertIndexes = invertIndexes;
        return this;
    }

    public boolean isRequireTable() {
        return requireTable;
    }

    public TableTranslateNumbers setRequireTable(boolean requireTable) {
        this.requireTable = requireTable;
        return this;
    }

    @Override
    public void process() {
        final SNumbers source = getInputNumbers();
        final SNumbers[] result = process(source,
                new SNumbers[]{
                        getInputNumbers(INPUT_TABLE_1, !requireTable),
                        getInputNumbers(INPUT_TABLE_2, true),
                        getInputNumbers(INPUT_TABLE_3, true),
                        getInputNumbers(INPUT_TABLE_4, true)
                });
        getNumbers(OUTPUT_VALUES_1).exchange(result[0]);
        getNumbers(OUTPUT_VALUES_2).exchange(result[1]);
        getNumbers(OUTPUT_VALUES_3).exchange(result[2]);
        getNumbers(OUTPUT_VALUES_4).exchange(result[3]);
    }

    public SNumbers[] process(SNumbers indexes, SNumbers[] translationTables) {
        Objects.requireNonNull(indexes, "Null indexes");
        Objects.requireNonNull(translationTables, "Null translationTables array");
        if (requireTable) {
            Objects.requireNonNull(translationTables[0], "Null 1st translation table");
        }
        long t1 = debugTime();
        final int indexesBlockLength = indexes.getBlockLength();
        int[] indexArray = indexes.toIntArrayOrReference();
        int resultN = indexes.n();
        long t2 = debugTime();
        if (invertIndexes) {
            if (indexesBlockLength != 1) {
                throw new IllegalArgumentException("\"Invert indexes\" mode requires 1-column indexes array");
            }
            indexArray = InvertTable.invert(indexArray, indexingBase.start);
            resultN = indexArray.length;
            indexes = null;
            // - to avoid accidental usage
        }
        getScalar(OUTPUT_N).setTo(resultN);
        long t3 = debugTime();
        final SNumbers[] results = new SNumbers[translationTables.length];
        boolean changed = false;
        for (int tableIndex = 0; tableIndex < translationTables.length; tableIndex++) {
            final SNumbers translationTable = translationTables[tableIndex];
            if (translationTable == null || !translationTable.isInitialized()) {
                results[tableIndex] = tableIndex == 0 && !requireTable ?
                        SNumbers.valueOfArray(indexArray, indexesBlockLength) :
                        new SNumbers();
                // - note: we MUST NOT return a reference to source indexes in ReadOnlyExecutionInput
                continue;
            }
            final long resultBlockLength = (long) translationTable.getBlockLength() * (long) indexesBlockLength;
            SNumbers.checkDimensions(resultN, resultBlockLength);
            final SNumbers result;
            if (translationTable.getBlockLength() == 1 && translationTable.isIntArray()) {
                final int[] translated = translateIntNumbers(indexArray, translationTable);
                result = SNumbers.arrayAsNumbers(translated, indexesBlockLength);
                if (isOutputNecessary(OUTPUT_CHANGED) && !changed) {
                    changed = !Arrays.equals(indexArray, translated);
                }
            } else {
                result = SNumbers.zeros(translationTable.elementType(), resultN, (int) resultBlockLength);
                translateNumbers(result, indexArray, translationTable);
                if (isOutputNecessary(OUTPUT_CHANGED)) {
                    getScalar(OUTPUT_CHANGED).setTo(!result.equals(indexes));
                }
            }
            results[tableIndex] = result;
        }
        if (isOutputNecessary(OUTPUT_CHANGED)) {
            getScalar(OUTPUT_CHANGED).setTo(changed);
        }
        long t4 = debugTime();
        if (LOGGABLE_DEBUG) {
            logDebug(String.format(Locale.US,
                    "Translating numbers by table(s) %s: "
                            + "%.3f ms = %.3f ms reading indexes + "
                            + "%.3f ms inversion + "
                            + "%.3f ms translation",
                    Stream.of(results).filter(Data::isInitialized).collect(Collectors.toList()),
                    (t4 - t1) * 1e-6,
                    (t2 - t1) * 1e-6,
                    (t3 - t2) * 1e-6,
                    (t4 - t3) * 1e-6));
        }
        return results;
    }

    private void translateNumbers(SNumbers result, int[] indexes, SNumbers translationTable) {
        IntStream.range(0, (indexes.length + 255) >>> 8).parallel().forEach(block -> {
            // note: splitting to blocks helps to provide normal speed
            final int start = indexingBase.start;
            final int step = translationTable.getBlockLength();
            // - note: it is only block length of the table, not of the result!
            if (step == 1) {
                translateRangeStep1(result, indexes, translationTable, block, start, replacementForNotExisting);
            } else {
                translateRange(result, indexes, translationTable, block, step, start, replacementForNotExisting);
            }
        });
    }

    private int[] translateIntNumbers(int[] indexes, SNumbers translationTable) {
        int[] table = (int[]) translationTable.arrayReference();
        final int[] result = new int[indexes.length];
        IntStream.range(0, (indexes.length + 255) >>> 8).parallel().forEach(block -> {
            // note: splitting to blocks helps to provide normal speed
            final int start = indexingBase.start;
            translateIntRangeStep1(result, indexes, table, block, start, replacementForNotExisting);
        });
        return result;
    }

    private static void translateRange(
            SNumbers result,
            int[] indexes,
            SNumbers translationTable,
            int block,
            int step,
            int start,
            Double replacementForNotExisting) {
        final int n = indexes.length;
        final int tableN = translationTable.n();
        final int blockLength = translationTable.blockLength();
        for (int i = block << 8, disp = i * step, to = (int) Math.min((long) i + 256, n); i < to; i++, disp += step) {
            final int originalValue = indexes[i];
            final int index = originalValue - start;
            if (index >= 0 && index < tableN) {
                for (int j = 0, tableDisp = index * blockLength; j < step; j++, tableDisp++) {
                    result.setValue(disp + j, translationTable.getValue(tableDisp));
                }
            } else {
                final double replacement = replacementForNotExisting != null ?
                        replacementForNotExisting :
                        originalValue;
                for (int j = 0; j < step; j++) {
                    result.setValue(disp + j, replacement);
                }
            }
        }
    }

    private static void translateRangeStep1(
            SNumbers result,
            int[] indexes,
            SNumbers translationTable,
            int block,
            int start,
            Double replacementForNotExisting) {
        assert translationTable.blockLength() == 1;
        final int n = indexes.length;
        final int tableN = translationTable.n();
        for (int i = block << 8, to = (int) Math.min((long) i + 256, n); i < to; i++) {
            final int originalValue = indexes[i];
            final int index = originalValue - start;
            if (index >= 0 && index < tableN) {
                result.setValue(i, translationTable.getValue(index));
            } else {
                result.setValue(i, replacementForNotExisting != null ?
                        replacementForNotExisting :
                        originalValue);
            }
        }
    }

    private static void translateIntRangeStep1(
            int[] result,
            int[] indexes,
            int[] table,
            int block,
            int start,
            Double replacementForNotExisting) {
        final boolean useReplacement = replacementForNotExisting != null;
        final int replacement = useReplacement ? (int) replacementForNotExisting.doubleValue() : 157;
        final int tableN = table.length;
        for (int i = block << 8, to = (int) Math.min((long) i + 256, indexes.length); i < to; i++) {
            final int originalValue = indexes[i];
            final int index = originalValue - start;
            if (index >= 0 && index < tableN) {
                result[i] = table[index];
            } else {
                result[i] = useReplacement ? replacement : originalValue;
            }
        }
    }

}
