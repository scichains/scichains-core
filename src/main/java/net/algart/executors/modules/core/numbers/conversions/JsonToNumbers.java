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

import jakarta.json.*;
import net.algart.arrays.MutableDoubleArray;
import net.algart.arrays.MutableLongArray;
import net.algart.arrays.MutablePNumberArray;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.json.Jsons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class JsonToNumbers extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_JSON = "json";
    public static final String INPUT_KEY_TO_FLAG_TABLE_JSON = "key_to_flag_table_json";
    public static final String INPUT_FLAGS_JSON = "flags_json";
    public static final String OUTPUT_KEYS = "keys";
    public static final String OUTPUT_JSON = "json";

    public enum ExtractedTypes {
        ALL(jsonValue -> true),

        NUMBERS(jsonValue -> jsonValue.getValueType() == JsonValue.ValueType.NUMBER),

        NON_ZERO_NUMBERS(jsonValue -> jsonValue.getValueType() == JsonValue.ValueType.NUMBER
                && ((JsonNumber) jsonValue).doubleValue() != 0.0),

        NUMBERS_AND_NUMERIC_STRINGS(jsonValue -> smartExtractDouble(jsonValue) != null),

        NON_ZERO_NUMBERS_AND_NUMERIC_STRINGS(jsonValue -> {
            final Double v = smartExtractDouble(jsonValue);
            return v != null && v != 0.0;
        }),

        BOOLEANS(jsonValue -> {
            JsonValue.ValueType valueType = jsonValue.getValueType();
            return valueType == JsonValue.ValueType.FALSE || valueType == JsonValue.ValueType.TRUE;
        }),

        TRUE_BOOLEANS(jsonValue -> jsonValue.getValueType() == JsonValue.ValueType.TRUE);

        private final Predicate<JsonValue> doAccept;

        ExtractedTypes(Predicate<JsonValue> doAccept) {
            this.doAccept = doAccept;
        }

        public final boolean accept(JsonValue jsonValue) {
            return doAccept.test(jsonValue);
        }

        ;
    }

    public enum SelectionMode {
        ALL() {
            @Override
            public boolean accept(String key, JsonObject keyToFlagTableJson, JsonObject flagsJson) {
                return true;
            }
        },
        SELECTED_ONLY() {
            @Override
            public boolean accept(String key, JsonObject keyToFlagTableJson, JsonObject flagsJson) {
                final String flagName = keyToFlagTableJson.getString(key, null);
                if (flagName == null) {
                    throw new JsonException("Key-to-flag table does not contain flag name (string value) "
                            + "for the key \"" + key + "\"");
                }
                return flagsJson.getBoolean(flagName, false);
            }
        };

        public abstract boolean accept(String key, JsonObject keyToFlagTableJson, JsonObject flagsJson);

    }

    private int blockLength = 1;
    private boolean singleBlock = true;
    private Class<?> elementType = double.class;
    private int indexOfFirstElement = 0;
    private Integer numberOfElements = null;
    private ExtractedTypes extractedTypes = ExtractedTypes.ALL;
    private SelectionMode extractingMode = SelectionMode.ALL;

    public JsonToNumbers() {
        setDefaultInputScalar(INPUT_JSON);
        addInputScalar(INPUT_KEY_TO_FLAG_TABLE_JSON);
        addInputScalar(INPUT_FLAGS_JSON);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_KEYS);
        addOutputScalar(OUTPUT_JSON);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public JsonToNumbers setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public boolean isSingleBlock() {
        return singleBlock;
    }

    public JsonToNumbers setSingleBlock(boolean singleBlock) {
        this.singleBlock = singleBlock;
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public JsonToNumbers setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public JsonToNumbers setElementType(String elementType) {
        setElementType(SNumbers.elementType(elementType));
        return this;
    }

    public int getIndexOfFirstElement() {
        return indexOfFirstElement;
    }

    public JsonToNumbers setIndexOfFirstElement(int indexOfFirstElement) {
        this.indexOfFirstElement = nonNegative(indexOfFirstElement);
        return this;
    }

    public Integer getNumberOfElements() {
        return numberOfElements;
    }

    public JsonToNumbers setNumberOfElements(Integer numberOfElements) {
        if (numberOfElements != null) {
            nonNegative(numberOfElements);
        }
        this.numberOfElements = numberOfElements;
        return this;
    }

    public ExtractedTypes getExtractedTypes() {
        return extractedTypes;
    }

    public JsonToNumbers setExtractedTypes(ExtractedTypes extractedTypes) {
        this.extractedTypes = nonNull(extractedTypes);
        return this;
    }

    public SelectionMode getExtractingMode() {
        return extractingMode;
    }

    public JsonToNumbers setExtractingMode(SelectionMode extractingMode) {
        this.extractingMode = nonNull(extractingMode);
        return this;
    }

    @Override
    public void process() {
        final JsonObject json = Jsons.toJson(getInputScalar(INPUT_JSON).getValue().trim());
        JsonObject keyToFlagTableJson = null;
        JsonObject flagsJson = null;
        if (extractingMode != SelectionMode.ALL) {
            keyToFlagTableJson = Jsons.toJson(getInputScalar(INPUT_KEY_TO_FLAG_TABLE_JSON).getValue().trim());
            flagsJson = Jsons.toJson(getInputScalar(INPUT_FLAGS_JSON).getValue().trim());
        }
        final MutablePNumberArray values = elementType == double.class || elementType == float.class ?
                MutableDoubleArray.newArray() :
                MutableLongArray.newArray();
        final List<String> keys = new ArrayList<>();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        int index = 0;
        int count = numberOfElements == null ? -157 : numberOfElements;
        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            if (index++ < indexOfFirstElement) {
                continue;
            }
            if (numberOfElements != null && count-- == 0) {
                break;
            }
            final String key = entry.getKey();
            final JsonValue jsonValue = entry.getValue();
            if (!extractedTypes.accept(jsonValue)) {
                continue;
            }
            if (!extractingMode.accept(key, keyToFlagTableJson, flagsJson)) {
                continue;
            }
            keys.add(key);
            builder.add(key, entry.getValue());
            switch (jsonValue.getValueType()) {
                case FALSE: {
                    if (values instanceof MutableDoubleArray) {
                        ((MutableDoubleArray) values).pushDouble(0.0);
                    } else {
                        ((MutableLongArray) values).pushLong(0);
                    }
                    break;
                }
                case TRUE: {
                    if (values instanceof MutableDoubleArray) {
                        ((MutableDoubleArray) values).pushDouble(1.0);
                    } else {
                        ((MutableLongArray) values).pushLong(1);
                    }
                    break;
                }
                case NUMBER: {
                    JsonNumber number = (JsonNumber) jsonValue;
                    if (values instanceof MutableDoubleArray) {
                        ((MutableDoubleArray) values).pushDouble(number.doubleValue());
                    } else {
                        ((MutableLongArray) values).pushLong(number.longValue());
                    }
                    break;
                }
                case STRING: {
                    final String s = ((JsonString) jsonValue).getString();
                    if (values instanceof MutableDoubleArray) {
                        ((MutableDoubleArray) values).pushDouble(smartParseDouble(s));
                    } else {
                        ((MutableLongArray) values).pushLong(smartParseLong(s));
                    }
                    break;
                }
                default: {
                    if (values instanceof MutableDoubleArray) {
                        ((MutableDoubleArray) values).pushDouble(Double.NaN);
                    } else {
                        ((MutableLongArray) values).pushLong(0);
                    }
                    break;
                }
            }
        }
        getScalar(OUTPUT_KEYS).setTo(String.join("\n", keys));
        getScalar(OUTPUT_JSON).setTo(Jsons.toPrettyString(builder.build()));
        try (CastNumbers castNumbers = new CastNumbers()) {
            getNumbers().setToArray(
                    castNumbers.setElementType(elementType).processUnstructuredJavaArray(values.toJavaArray()),
                    singleBlock ? (int) values.length() : blockLength);
        }
    }

    private static long smartParseLong(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double smartParseDouble(String s) {
        if (s == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static Double smartExtractDouble(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
            case NUMBER: {
                return ((JsonNumber) jsonValue).doubleValue();
            }
            case STRING: {
                final String s = ((JsonString) jsonValue).getString();
                try {
                    return Double.valueOf(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            default: {
                return null;
            }
        }
    }
}
