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

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.json.Jsons;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class JsonToIndexValueTable extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_JSON_TABLE = "json_table";
    public static final String INPUT_JSON_NAMED_INDEXES = "json_named_indexes";
    public static final String INPUT_JSON_NAMED_VALUES = "json_named_values";
    public static final String OUTPUT_TABLE = "table";

    public static final int MAX_ALLOWED_INDEX = 16777216;
    // - no reasons to create longer tables!

    private Class<?> valueElementType = int.class;
    private int minimalTableLength = 0;
    private String tableFiller = "0.0";
    private IndexingBase indexingBase = IndexingBase.ZERO_BASED;
    private boolean allowTooLowIndexes = false;

    public JsonToIndexValueTable() {
        setDefaultInputScalar(INPUT_JSON_TABLE);
        addInputScalar(INPUT_JSON_NAMED_INDEXES);
        addInputScalar(INPUT_JSON_NAMED_VALUES);
        setDefaultOutputNumbers(OUTPUT_TABLE);
    }

    public Class<?> getValueElementType() {
        return valueElementType;
    }

    public JsonToIndexValueTable setValueElementType(Class<?> valueElementType) {
        this.valueElementType = nonNull(valueElementType, "element type");
        return this;
    }

    public JsonToIndexValueTable setElementType(String elementType) {
        setValueElementType(SNumbers.elementType(elementType));
        return this;
    }

    public int getMinimalTableLength() {
        return minimalTableLength;
    }

    public JsonToIndexValueTable setMinimalTableLength(int minimalTableLength) {
        this.minimalTableLength = nonNegative(minimalTableLength);
        return this;
    }

    public String getTableFiller() {
        return tableFiller;
    }

    public JsonToIndexValueTable setTableFiller(String tableFiller) {
        this.tableFiller = nonEmpty(tableFiller);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public JsonToIndexValueTable setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public boolean isAllowTooLowIndexes() {
        return allowTooLowIndexes;
    }

    public JsonToIndexValueTable setAllowTooLowIndexes(boolean allowTooLowIndexes) {
        this.allowTooLowIndexes = allowTooLowIndexes;
        return this;
    }

    @Override
    public void process() {
        final JsonObject jsonTable = Jsons.toJson(getInputScalar(INPUT_JSON_TABLE).getValue().trim());
        final JsonObject jsonIndexNames = Jsons.toJson(
                getInputScalar(INPUT_JSON_NAMED_INDEXES, true).getValue(), true);
        final JsonObject jsonValueNames = Jsons.toJson(
                getInputScalar(INPUT_JSON_NAMED_VALUES, true).getValue(), true);
        final int maxLength = Math.max(minimalTableLength, findTableLength(
                jsonTable, jsonIndexNames, indexingBase.start, INPUT_JSON_NAMED_INDEXES, allowTooLowIndexes));
        final Object resultTable = valueElementType == double.class || valueElementType == float.class ?
                new double[maxLength] :
                new long[maxLength];
        if (resultTable instanceof double[]) {
            Arrays.fill((double[]) resultTable, tableFiller(jsonValueNames));
        } else {
            Arrays.fill((long[]) resultTable, (long) tableFiller(jsonValueNames));
        }
        for (Map.Entry<String, JsonValue> entry : jsonTable.entrySet()) {
            final int index = toIndex(jsonIndexNames, entry.getKey(), INPUT_JSON_NAMED_INDEXES);
            final int k = index - indexingBase.start;
            if (k < 0) {
                // - possible when allowTooLowIndexes=true
                continue;
            }
            final JsonValue jsonValue = entry.getValue();
            switch (jsonValue.getValueType()) {
                case NUMBER: {
                    if (resultTable instanceof double[]) {
                        ((double[]) resultTable)[k] = ((JsonNumber) jsonValue).doubleValue();
                    } else {
                        ((long[]) resultTable)[k] = ((JsonNumber) jsonValue).longValueExact();
                    }
                    break;
                }
                case STRING: {
                    final String name = ((JsonString) jsonValue).getString();
                    if (resultTable instanceof double[]) {
                        ((double[]) resultTable)[k] = toDoubleValue(jsonValueNames, name, INPUT_JSON_NAMED_VALUES);
                    } else {
                        ((long[]) resultTable)[k] = toLongValue(jsonValueNames, name, INPUT_JSON_NAMED_VALUES);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Table must contain integer or string values, but for key \""
                            + entry.getKey() + "\" it contains \"" + jsonValue + "\"");
                }
            }
        }
        try (CastNumbers castNumbers = new CastNumbers()) {
            getNumbers(OUTPUT_TABLE).setToArray(
                    castNumbers.setElementType(valueElementType).processUnstructuredJavaArray(
                            resultTable), 1);
        }
    }

    static int findTableLength(
            JsonObject jsonTable,
            JsonObject jsonIndexNames,
            int indexingBase,
            String nameOfJsonIndexNames,
            boolean allowTooLowIndexes) {
        int maxLength = 0;
        for (String key : jsonTable.keySet()) {
            final int index = toIndex(jsonIndexNames, key, nameOfJsonIndexNames);
            if (index < indexingBase && !allowTooLowIndexes) {
                throw new IllegalArgumentException("Too small index " + index + ", corresponding to key \"" + key
                        + "\", is not allowed: all indexes must be >=" + indexingBase + " (indexing base)");
            }
            if (index > MAX_ALLOWED_INDEX + indexingBase) {
                throw new IllegalArgumentException("Too large index " + index + ", corresponding to key \"" + key
                        + "\": indexes must be <=" + (MAX_ALLOWED_INDEX + indexingBase));
            }
            maxLength = Math.max(index - indexingBase + 1, maxLength);
            // - overflow impossible due to MAX_ALLOWED_INDEX
        }
        return maxLength;
    }

    static int toIndex(JsonObject namesToNumber, String name, String namesJsonName) {
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException ignored) {
        }
        return toNumber(namesToNumber, name, namesJsonName).intValueExact();
    }

    private double tableFiller(JsonObject namesToNumber) {
        try {
            return Double.parseDouble(tableFiller);
        } catch (NumberFormatException ignored) {
        }
        return toNumber(namesToNumber, tableFiller, INPUT_JSON_NAMED_VALUES).doubleValue();
    }

    private static double toDoubleValue(JsonObject namesToNumber, String name, String namesJsonName) {
        final JsonNumber value = toNumberOrNull(namesToNumber, name, namesJsonName);
        if (value == null) {
            try {
                return Double.parseDouble(name);
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("There is a symbolic name \"" + name + "\", but JSON names map \""
                        + namesJsonName + "\" " +
                        (namesToNumber.isEmpty() ? "is empty" : "does not contain this name"));
            }
        }
        return value.doubleValue();
    }

    private static long toLongValue(JsonObject namesToNumber, String name, String namesJsonName) {
        final JsonNumber value = toNumberOrNull(namesToNumber, name, namesJsonName);
        if (value == null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("There is a symbolic name \"" + name + "\", but JSON names map \""
                        + namesJsonName + "\" " +
                        (namesToNumber.isEmpty() ? "is empty" : "does not contain this name"));
            }
        }
        return value.longValueExact();
    }

    private static JsonNumber toNumber(JsonObject namesToNumber, String name, String namesJsonName) {
        final JsonNumber value = toNumberOrNull(namesToNumber, name, namesJsonName);
        if (value == null) {
            throw new IllegalArgumentException("There is a symbolic name \"" + name + "\", but JSON names map \""
                    + namesJsonName + "\" " + (namesToNumber.isEmpty() ? "is empty" : "does not contain this name"));
        }
        return value;
    }

    private static JsonNumber toNumberOrNull(JsonObject namesToNumber, String name, String namesJsonName) {
        Objects.requireNonNull(namesToNumber);
        final JsonValue value = namesToNumber.isEmpty() ? null : namesToNumber.get(name);
        if (value != null && !(value instanceof JsonNumber)) {
            throw new IllegalArgumentException("Invalid JSON names map \""
                    + namesJsonName + "\": it must contain numeric values, but it contains not a number \""
                    + value + "\" for key \"" + name + "\"");
        }
        return (JsonNumber) value;
    }
}
