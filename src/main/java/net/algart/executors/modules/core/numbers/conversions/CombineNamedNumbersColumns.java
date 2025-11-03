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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.*;
import java.util.stream.IntStream;

public final class CombineNamedNumbersColumns extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String COLUMN_NAMES_PREFIX = "columnNames";
    public static final String OUTPUT_COLUMN_NAMES = "column_names";

    private static final int INPUT_PORT_PREFIX_LENGTH = INPUT_PORT_PREFIX.length();

    private boolean allColumnsRequired = false;
    private MergeNumbers.ResultElementType resultElementType = MergeNumbers.ResultElementType.FIRST_INPUT;
    private String resultColumnNames = "";
    private List<String> resultColumnNamesList = Collections.emptyList();
    private Set<String> resultColumnNamesSet = Collections.emptySet();
    private final Map<Integer, List<String>> columnNames = new HashMap<>();

    public CombineNamedNumbersColumns() {
        addOutputScalar(OUTPUT_COLUMN_NAMES);
    }

    public boolean requireAllColumns() {
        return allColumnsRequired;
    }

    public CombineNamedNumbersColumns setAllColumnsRequired(boolean allColumnsRequired) {
        this.allColumnsRequired = allColumnsRequired;
        return this;
    }

    public MergeNumbers.ResultElementType getResultElementType() {
        return resultElementType;
    }

    public CombineNamedNumbersColumns setResultElementType(MergeNumbers.ResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
        return this;
    }

    public String resultColumnNames() {
        return resultColumnNames;
    }

    public CombineNamedNumbersColumns setResultColumnNames(String resultColumnNames) {
        this.resultColumnNamesList = SScalar.splitJsonOrTrimmedLinesWithoutComments(nonNull(resultColumnNames));
        // - also checks possible exceptions
        this.resultColumnNames = resultColumnNames;
        this.resultColumnNamesSet = new HashSet<>(this.resultColumnNamesList);
        return this;
    }

    public List<String> getColumnNames(int indexStartingFrom1) {
        return columnNames.getOrDefault(indexStartingFrom1, Collections.emptyList());
    }

    public CombineNamedNumbersColumns setColumnNames(int indexStartingFrom1, List<String> columnNames) {
        this.columnNames.put(indexStartingFrom1, nonNull(columnNames));
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.startsWith(COLUMN_NAMES_PREFIX)) {
            final int index;
            try {
                index = Integer.parseInt(name.substring(COLUMN_NAMES_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                return;
            }
            final String value = parameters().getString(name);
            //noinspection resource
            setColumnNames(index, SScalar.splitJsonOrTrimmedLinesWithoutComments(value));
            return;
        }
        super.onChangeParameter(name);
    }


    @Override
    public Boolean checkInputNecessary(Port inputPort) {
        if (inputPort == null) {
            return null;
        }
        final String inputPortName = inputPort.getName();
        final int length = inputPortName.length();
        if (length < INPUT_PORT_PREFIX_LENGTH ||
                !INPUT_PORT_PREFIX.equals(inputPortName.substring(0, INPUT_PORT_PREFIX_LENGTH))) {
            return null;
        }
        final int index = Integer.parseInt(inputPortName.substring(INPUT_PORT_PREFIX_LENGTH));
        return isSourceNecessary(index);
    }

    @Override
    public SNumbers processNumbers(List<SNumbers> sources) {
        if (resultColumnNamesList.isEmpty()) {
            return new SNumbers();
        }
        if (sources.stream().noneMatch(Objects::nonNull)) {
            // - no inputs: we cannot create an even empty result, because we don't know array length
            if (allColumnsRequired) {
                throw new IllegalArgumentException("There are no initialized input arrays");
            } else {
                return new SNumbers();
            }
        }
        final Class<?> resultElementType = MergeNumbers.findElementType(sources, this.resultElementType);
        assert resultElementType != null : "must not be null if there are non-null sources: " + sources;
        final int n = sources.stream().filter(Objects::nonNull).findFirst().map(SNumbers::n).orElse(0);
        final List<SNumbers> actualSources = new ArrayList<>();
        long t1 = debugTime();
        for (int i = 0, m = sources.size(); i < m; i++) {
            SNumbers source = sources.get(i);
            if (source != null && source.elementType() != resultElementType
                    && isSourceNecessary(i + 1)) {
                source = source.toPrecision(resultElementType);
            }
            actualSources.add(source);
        }
        long t2 = debugTime();
        final List<ColumnIndex> columnIndexes = findColumnIndexes(actualSources);
        final boolean allFloat = resultElementType == float.class
                && columnIndexes.stream().allMatch(ColumnIndex::isFloatOrNull);
        final int resultBlockLength = columnIndexes.size();
        final SNumbers result = SNumbers.zeros(resultElementType, n, resultBlockLength);
        if (!allFloat && !columnIndexes.stream().allMatch(Objects::nonNull)) {
            result.fillValue(Double.NaN);
        }
        long t3 = debugTime();
        if (allFloat) {
            combineFloats(result, columnIndexes);
        } else {
            for (int j = 0; j < resultBlockLength; j++) {
                ColumnIndex columnIndex = columnIndexes.get(j);
                if (columnIndex != null) {
                    result.replaceColumnRange(j, columnIndex.source, columnIndex.index, 1);
                }
            }
        }
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Combining %d named columns (%d rows): "
                        + "%.3f ms = %.3f ms cast + "
                        + "%.3f ms creating  + "
                        + "%.3f ms combining",
                result.getBlockLength(), result.n(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6,
                (t4 - t3) * 1e-6));
        getScalar(OUTPUT_COLUMN_NAMES).setTo(String.join("\n", resultColumnNamesList));
        return result;
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getOutputPort(OUTPUT_COLUMN_NAMES));
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireAllColumns") ? "allColumnsRequired" : name;
    }

    @Override
    protected boolean allowAllUninitializedInputs() {
        return true;
    }

    @Override
    protected boolean numberOfBlocksEqualityRequired() {
        return true;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

    private Boolean isSourceNecessary(int indexStartingFrom1) {
        for (String name : getColumnNames(indexStartingFrom1)) {
            if (resultColumnNamesSet.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static void combineFloats(SNumbers result, List<ColumnIndex> columnIndexes) {
        final float[] resultArray = (float[]) result.arrayReference();
        final ColumnIndex[] columnIndexesArray = columnIndexes.toArray(new ColumnIndex[0]);
        final int n = result.n();
        final int resultBlockLength = result.getBlockLength();
        assert resultBlockLength == columnIndexesArray.length;
        final boolean allNonNull = columnIndexes.stream().allMatch(Objects::nonNull);
        IntStream.range(0, (n + 255) >>> 8).parallel().forEach(
                allNonNull ? block ->
                {
                    for (int i = block << 8, disp = i * resultBlockLength, to = (int) Math.min((long) i + 256, n);
                         i < to; i++) {
                        for (ColumnIndex columnIndex : columnIndexesArray) {
                            resultArray[disp++] = columnIndex.getValue(i);
                        }
                    }
                } : block -> {
                    for (int i = block << 8, disp = i * resultBlockLength, to = (int) Math.min((long) i + 256, n);
                         i < to; i++) {
                        for (ColumnIndex columnIndex : columnIndexesArray) {
                            resultArray[disp++] = columnIndex == null ? Float.NaN : columnIndex.getValue(i);
                        }
                    }
                });
    }

    private List<ColumnIndex> findColumnIndexes(List<SNumbers> sources) {
        int i = 0;
        for (SNumbers source : sources) {
            final List<String> sourceNames = getColumnNames(++i);
            if (source != null && sourceNames.size() > source.getBlockLength()) {
                // Note: source may be null AS A RESULT of the fact that it is not necessary
                throw new IllegalArgumentException("The input array #" + i + " has only "
                        + source.getBlockLength() + " columns, but " + sourceNames.size()
                        + " column names are specified for it: " + sourceNames);
            }
        }
        final List<ColumnIndex> result = new ArrayList<>();
        for (String name : resultColumnNamesList) {
            ColumnIndex columnIndex = null;
            i = 0;
            for (SNumbers source : sources) {
                final List<String> sourceNames = getColumnNames(++i);
                final int index = sourceNames.indexOf(name);
                if (index != -1) {
                    if (source == null) {
                        throw new IllegalArgumentException("The input array #" + i + " is not initialized, "
                                + "though it is required for calculating result column \"" + name + "\"");
                    }
                    columnIndex = new ColumnIndex(source, index);
                    break;
                }
            }
            if (columnIndex == null && allColumnsRequired) {
                throw new IllegalArgumentException("Result column name \"" + name + "\" is not found "
                        + "among column names of the source arrays");
            }
            result.add(columnIndex);
        }
        return result;
    }

    private static class ColumnIndex {
        final SNumbers source;
        final int index;
        final float[] floats;
        final int blockLength;

        ColumnIndex(SNumbers source, int index) {
            this.source = Objects.requireNonNull(source);
            this.index = index;
            this.floats = source.isFloatArray() ? (float[]) source.arrayReference() : null;
            this.blockLength = source.getBlockLength();
        }

        float getValue(int rowIndex) {
            return floats[rowIndex * blockLength + index];
        }

        static boolean isFloatOrNull(ColumnIndex columnIndex) {
            return columnIndex == null || columnIndex.source.isFloatArray();
        }
    }
}
