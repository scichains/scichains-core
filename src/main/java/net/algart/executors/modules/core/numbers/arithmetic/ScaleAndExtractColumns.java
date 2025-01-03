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

package net.algart.executors.modules.core.numbers.arithmetic;

import jakarta.json.*;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.json.Jsons;

import java.util.Arrays;
import java.util.stream.IntStream;

public final class ScaleAndExtractColumns extends NumbersFilter {
    public static final String INPUT_WEIGHTS = "weights";
    public static final String INPUT_COLUMN_NAMES = "column_names";
    public static final String INPUT_WEIGHTS_JSON = "weights_json";
    public static final String OUTPUT_COLUMN_NAMES = "column_names";

    private boolean removeColumnsWithZeroWeight = false;
    private boolean removeColumnsAbsentInWeightJson = false;
    private boolean removeColumnsFilledByNaN = false;
    private boolean requireNonEmptyResult = true;

    public ScaleAndExtractColumns() {
        addInputNumbers(INPUT_WEIGHTS);
        addInputScalar(INPUT_COLUMN_NAMES);
        addInputScalar(INPUT_WEIGHTS_JSON);
        addOutputScalar(OUTPUT_COLUMN_NAMES);
    }

    public boolean isRemoveColumnsWithZeroWeight() {
        return removeColumnsWithZeroWeight;
    }

    public ScaleAndExtractColumns setRemoveColumnsWithZeroWeight(boolean removeColumnsWithZeroWeight) {
        this.removeColumnsWithZeroWeight = removeColumnsWithZeroWeight;
        return this;
    }

    public boolean isRemoveColumnsAbsentInWeightJson() {
        return removeColumnsAbsentInWeightJson;
    }

    public ScaleAndExtractColumns setRemoveColumnsAbsentInWeightJson(boolean removeColumnsAbsentInWeightJson) {
        this.removeColumnsAbsentInWeightJson = removeColumnsAbsentInWeightJson;
        return this;
    }

    public boolean isRemoveColumnsFilledByNaN() {
        return removeColumnsFilledByNaN;
    }

    public ScaleAndExtractColumns setRemoveColumnsFilledByNaN(boolean removeColumnsFilledByNaN) {
        this.removeColumnsFilledByNaN = removeColumnsFilledByNaN;
        return this;
    }

    public boolean isRequireNonEmptyResult() {
        return requireNonEmptyResult;
    }

    public ScaleAndExtractColumns setRequireNonEmptyResult(boolean requireNonEmptyResult) {
        this.requireNonEmptyResult = requireNonEmptyResult;
        return this;
    }

    @Override
    public SNumbers processNumbers(SNumbers source) {
        final SNumbers weightsNumbers = getInputNumbers(INPUT_WEIGHTS, true);
        final String[] columnNames = getInputScalar(INPUT_COLUMN_NAMES, true)
                .toTrimmedLinesWithoutCommentsArray();
        final String s = getInputScalar(INPUT_WEIGHTS_JSON, true).getValue();
        final JsonObject weightsJson = s == null ? null : Jsons.toJson(s, true);
        return scaleAndExtractColumns(source, weightsNumbers, columnNames, weightsJson);
    }

    public SNumbers scaleAndExtractColumns(
            SNumbers source,
            SNumbers weightsNumbers,
            String[] columnNames,
            JsonObject weightsJson) {
        if (!source.isInitialized()) {
            return source;
        }
        final int blockLength = source.getBlockLength();
        if (weightsJson != null && columnNames == null) {
            throw new IllegalArgumentException("Column names are not specified, but they are required "
                    + "when weights JSON is used");
        }
        if (columnNames != null) {
            if (columnNames.length < blockLength) {
                throw new IllegalArgumentException("Too short list of column names: only "
                        + columnNames.length + " names, but array has " + blockLength + " columns");
            }
            if (columnNames.length > blockLength) {
                columnNames = Arrays.copyOf(columnNames, blockLength);
            }
        }
        final double[] weights = weightsJson != null ?
                jsonToWeightsArray(weightsJson, columnNames) :
                numbersToWeightsArray(weightsNumbers, blockLength);
        final boolean[] removeColumns = findRemovedColumns(weights, weightsJson, columnNames, source);
        final int resultBlockLength = resultBlockLength(removeColumns);
        if (columnNames != null) {
            getScalar(OUTPUT_COLUMN_NAMES).setTo(extractColumnNames(columnNames, removeColumns));
        }
        if (resultBlockLength == 0) {
            if (requireNonEmptyResult) {
                throw new IllegalArgumentException("No columns in the result; "
                        + "maybe source weights are empty or incorrect");
            }
            return null;
        }
        if (resultBlockLength == blockLength && Arrays.stream(weights).allMatch(w -> w == 1.0)) {
            return source;
        }
        final SNumbers result = SNumbers.zeros(source.elementType(), source.n(), resultBlockLength);
        final double[] values = new double[blockLength];
        final double[] resultValues = new double[resultBlockLength];
        final int length = source.getArrayLength();
        for (int disp = 0, resultDisp = 0; disp < length; disp += blockLength, resultDisp += resultBlockLength) {
            source.getDoubleValues(disp, blockLength, values);
            for (int i = 0, k = 0; i < values.length; i++) {
                if (!removeColumns[i]) {
                    resultValues[k++] = values[i] * weights[i];
                }
            }
            result.setDoubleValues(resultDisp, resultBlockLength, resultValues);
        }
        return result;
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getOutputPort(OUTPUT_COLUMN_NAMES));
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    private static int resultBlockLength(boolean[] removeColumns) {
        return (int) IntStream.range(0, removeColumns.length).filter(k -> !removeColumns[k]).count();
    }

    private boolean[] findRemovedColumns(
            double[] weights,
            JsonObject weightsJson,
            String[] columnNames,
            SNumbers source) {
        final int blockLength = source.getBlockLength();
        assert blockLength == weights.length;
        final boolean[] removeColumns = new boolean[blockLength];
        // - zero-filled by Java
        if (weightsJson != null) {
            assert columnNames != null;
            for (int k = 0; k < blockLength; k++) {
                if (removeColumnsAbsentInWeightJson && !weightsJson.containsKey(columnNames[k])) {
                    removeColumns[k] = true;
                }
            }
        }
        if (removeColumnsWithZeroWeight) {
            for (int k = 0; k < blockLength; k++) {
                if (weights[k] == 0.0) {
                    removeColumns[k] = true;
                }
            }
        }
        final int length = source.getArrayLength();
        if (removeColumnsFilledByNaN && length > 0 && source.isFloatingPoint()) {
            for (int k = 0; k < blockLength; k++) {
                if (!removeColumns[k]) {
                    boolean remove = true;
                    for (int disp = k; disp < length; disp += blockLength) {
                        if (!Double.isNaN(source.getValue(disp))) {
                            remove = false;
                        }
                    }
                    removeColumns[k] = remove;
                }
            }
        }
        return removeColumns;
    }

    private static String extractColumnNames(String[] columnNames, boolean[] removeColumns) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < removeColumns.length; i++) {
            if (!removeColumns[i]) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(columnNames[i]);
            }
        }
        return sb.toString();
    }

    private static double[] jsonToWeightsArray(JsonObject weightsJson, String[] columnNames) {
        assert weightsJson != null;
        final double[] result = new double[columnNames.length];
        for (int k = 0; k < result.length; k++) {
            final String name = columnNames[k];
            final JsonValue jsonValue = weightsJson.get(name);
            if (jsonValue == null) {
                result[k] = 1.0;
                // - not change, if removeColumnsAbsentInWeightsJson is not set
                continue;
            }
            final Double weight = parseDoubleIfPossible(jsonValue);
            if (weight == null) {
                throw new JsonException("Weights JSON for column name \"" + name
                        + "\" contains non-numeric value " + jsonValue
                        + ", that cannot be converted to a reasonable number");
            }
            result[k] = weight;
        }
        return result;
    }

    private static double[] numbersToWeightsArray(SNumbers weightsNumbers, int blockLength) {
        double[] result = weightsNumbers.toDoubleArray();
        if (result == null) {
            result = new double[0];
        }
        if (result.length != blockLength) {
            final int oldLength = result.length;
            result = Arrays.copyOf(result, blockLength);
            for (int i = oldLength; i < blockLength; i++) {
                // correct also if oldLength > blockLength
                result[i] = 1.0;
            }
        }
        return result;
    }

    private static Double parseDoubleIfPossible(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
            case NUMBER: {
                return ((JsonNumber) jsonValue).doubleValue();
            }
            case STRING: {
                try {
                    return Double.parseDouble(((JsonString) jsonValue).getString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case FALSE: {
                return 0.0;
            }
            case TRUE: {
                return 1.0;
            }
            default: {
                return null;
            }
        }
    }
}
