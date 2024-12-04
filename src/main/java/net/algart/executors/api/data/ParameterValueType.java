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

package net.algart.executors.api.data;

import jakarta.json.*;
import net.algart.executors.api.parameters.NoValidParameterException;
import net.algart.executors.api.parameters.Parameters;
import net.algart.json.Jsons;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public enum ParameterValueType {
    INT("int", int.class, "int32") {
        @Override
        public Integer toParameter(JsonValue jsonValue) {
            if (jsonValue instanceof JsonNumber jsonNumber) {
                return jsonNumber.intValue();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonIntValue(0);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJsonIntValue(parameters.getInteger(name));
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final int v;
            try {
                v = Parameters.smartParseInt(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.toJsonIntValue(v);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setInteger(name, value);

        }
    },
    LONG("long", long.class, "int64") {
        @Override
        public Long toParameter(JsonValue jsonValue) {
            if (jsonValue instanceof JsonNumber jsonNumber) {
                return jsonNumber.longValue();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonIntValue(0);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJsonLongValue(parameters.getLong(name));
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final long v;
            try {
                v = Parameters.smartParseLong(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.toJsonLongValue(v);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setLong(name, value);
        }
    },
    FLOAT("float", float.class) {
        @Override
        public Float toParameter(JsonValue jsonValue) {
            if (jsonValue instanceof JsonNumber jsonNumber) {
                return (float) jsonNumber.doubleValue();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonDoubleValue(0.0);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return DOUBLE.toJsonValue(parameters, name);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return DOUBLE.toJsonValue(scalar);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setDouble(name, value);
        }
    },
    DOUBLE("double", double.class) {
        @Override
        public Double toParameter(JsonValue jsonValue) {
            if (jsonValue instanceof JsonNumber jsonNumber) {
                return jsonNumber.doubleValue();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonDoubleValue(0.0);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJsonDoubleValue(parameters.getDouble(name));
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final double v;
            try {
                v = Double.parseDouble(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.toJsonDoubleValue(v);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setDouble(name, value);

        }
    },
    BOOLEAN("boolean", boolean.class) {
        @Override
        public Boolean toParameter(JsonValue jsonValue) {
            if (jsonValue == null) {
                return null;
            } else if (jsonValue.getValueType() == JsonValue.ValueType.FALSE) {
                return Boolean.FALSE;
            } else if (jsonValue.getValueType() == JsonValue.ValueType.TRUE) {
                return Boolean.TRUE;
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonBooleanValue(false);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJsonBooleanValue(parameters.getBoolean(name));
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final Boolean v = Parameters.smartParseBoolean(scalar);
            if (v == null) {
                return emptyJsonValue();
            }
            return Jsons.toJsonBooleanValue(v);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setBoolean(name, value);
        }
    },
    STRING("String", String.class, "scalar") {
        @Override
        public String toParameter(JsonValue jsonValue) {
            if (jsonValue == null) {
                return null;
            } else if (jsonValue instanceof JsonString jsonString) {
                return jsonString.getString();
            } else if (jsonValue.getValueType() == JsonValue.ValueType.FALSE) {
                return "false";
            } else if (jsonValue.getValueType() == JsonValue.ValueType.TRUE) {
                return "true";
            } else if (jsonValue instanceof JsonNumber) {
                return jsonValue.toString();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.toJsonStringValue("");
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJsonStringValue(parameters.getString(name));
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return Jsons.toJsonStringValue(scalar);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setString(name, value);

        }
    },
    /**
     * Actually, it is also String type, but in Java we have enum.
     */
    ENUM_STRING("String", Enum.class, "scalar") {
        @Override
        public Object toParameter(JsonValue jsonValue) {
            return STRING.toParameter(jsonValue);
        }

        @Override
        public JsonValue emptyJsonValue() {
            return STRING.emptyJsonValue();
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return STRING.toJsonValue(parameters, name);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return STRING.toJsonValue(scalar);
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setString(name, value);

        }
    },
    SETTINGS("settings", JsonObject.class) {
        @Override
        public Object toParameter(JsonValue jsonValue) {
            if (jsonValue instanceof JsonObject) {
                return jsonValue;
            } else if (jsonValue instanceof JsonString jsonString) {
                return jsonString.getString();
            } else {
                return null;
            }
        }

        @Override
        public JsonValue emptyJsonValue() {
            return Jsons.newEmptyJson();
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJson(parameters.getString(name), true);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            Objects.requireNonNull(scalar, "Null scalar");
            try {
                return Jsons.toJson(scalar, true);
            } catch (JsonException e) {
                return emptyJsonValue();
            }
        }

        @Override
        public void setParameter(Parameters parameters, String name, String value) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters.setString(name, value);

        }
    };

    private final String typeName;
    private final Class<?> javaType;
    private final String[] typeNameAliases;

    private static final Map<String, ParameterValueType> ALL_TYPES = new LinkedHashMap<>();
    private static final ParameterValueType[] FOR_SUITABLE_JAVA_OBJECT = {SETTINGS, STRING, DOUBLE, BOOLEAN};

    static {
        for (ParameterValueType type : values()) {
            if (type != ENUM_STRING) {
                ALL_TYPES.put(type.typeName, type);
                for (String alias : type.typeNameAliases) {
                    ALL_TYPES.put(alias, type);
                }
            }
        }
    }

    ParameterValueType(String typeName, Class<?> javaType, String... typeNameAliases) {
        this.typeName = typeName;
        this.javaType = javaType;
        this.typeNameAliases = typeNameAliases;
    }

    public String typeName() {
        return typeName;
    }

    public Class<?> javaType() {
        return javaType;
    }

    /**
     * Converts the given JSON value to this type and returns it as primitive type wrapper or <code>String</code>.
     * If the passed value has invalid type, returns <code>null</code>.
     * Note: for {@link #ENUM_STRING}, this method returns <code>String</code> (standard enum type name).
     *
     * @param jsonValue some JSON value; can be <code>null</code>, then the result will be <code>null</code>.
     * @return the corresponding primitive type / String, or <code>null</code> if it has invalid type.
     */
    public abstract Object toParameter(JsonValue jsonValue);

    public Object toSmartParameter(JsonValue jsonValue) {
        Object probe = toParameter(jsonValue);
        if (probe != null) {
            return probe;
        }
        for (ParameterValueType parameterValueType : FOR_SUITABLE_JAVA_OBJECT) {
            // note: for example, we should not check LONG before DOUBLE
            probe = parameterValueType.toParameter(jsonValue);
            if (probe != null) {
                return probe;
            }
        }
        return null;
    }

    /**
     * Creates JSON value for the simplest default value (zero for numbers, empty for strings).
     * Note: for {@link #ENUM_STRING}, this method returns <code>""</code>, that is usually incorrect
     * for actual enums.
     *
     * @return empty/zero value of this type.
     */
    public abstract JsonValue emptyJsonValue();

    /**
     * Creates JSON value for the parameter with given name.
     * Note: for {@link #ENUM_STRING}, this method returns a usual string value, that can be incorrect
     * for actual enums.
     *
     * @param parameters set of parameters.
     * @param name       name of the parameter.
     * @return value of the given parameter in the specified parameters set.
     */
    public abstract JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException;

    /**
     * Creates JSON value for the given scalar string or {@link #emptyJsonValue()},
     * if this scalar has an invalid format.
     * Note: for {@link #ENUM_STRING}, this method returns a usual string value, that can be incorrect
     * for actual enums.
     *
     * @param scalar scalar string.
     * @return JSON value of the scalar.
     */
    public abstract JsonValue toJsonValue(String scalar);

    /**
     * Sets the parameter inside the passed <code>parameters</code> object according this parameter type.
     * This function just calls the corresponding setter with <code>String</code> value argument of
     * <code>parameters</code> object.
     *
     * @param parameters set of parameters.
     * @param name       name of the parameter.
     * @param value      value to set.
     */
    public abstract void setParameter(Parameters parameters, String name, String value);

    // Note: in the future, some types that are not currently allowed can become allowed (supported).
    public boolean isAllowedInExecutor() {
        return this != SETTINGS;
    }

    public static ParameterValueType valueOfTypeName(String name) {
        final ParameterValueType result = ALL_TYPES.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Unknown parameter value type: " + name);
        }
        return result;
    }

    public static ParameterValueType valueOfTypeNameOrNull(String name) {
        return ALL_TYPES.get(name);
    }

    public static void main(String[] args) {
        for (ParameterValueType type : values()) {
            System.out.printf("%s: %s, %s, %s, %s%n",
                    type,
                    type.typeName(),
                    java.util.Arrays.toString(type.typeNameAliases),
                    valueOfTypeName(type.typeName()),
                    type.emptyJsonValue());
            for (String v : Arrays.asList("TRUE", "12", "12.3", "axd", "{\"key\":12}")) {
                System.out.printf("  %s: %s%n", v, type.toJsonValue(v));
            }
        }
    }
}
