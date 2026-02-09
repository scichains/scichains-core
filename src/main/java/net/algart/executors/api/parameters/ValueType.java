/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.api.parameters;

import jakarta.json.*;
import net.algart.json.Jsons;

import java.util.*;

public enum ValueType {
    INT("int", int.class, "int32") {
        @Override
        public Integer toParameter(String stringValue) {
            if (stringValue == null) {
                return null;
            }
            try {
                return Parameters.smartParseInt(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

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
            return Jsons.intValue(0);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final int v;
            try {
                v = Parameters.smartParseInt(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.intValue(v);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.intValue(parameters.getInteger(name));
        }

    },
    LONG("long", long.class, "int64") {
        @Override
        public Long toParameter(String stringValue) {
            if (stringValue == null) {
                return null;
            }
            try {
                return Parameters.smartParseLong(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

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
            return Jsons.intValue(0);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final long v;
            try {
                v = Parameters.smartParseLong(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.longValue(v);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.longValue(parameters.getLong(name));
        }

    },
    FLOAT("float", float.class) {
        @Override
        public Float toParameter(String stringValue) {
            if (stringValue == null) {
                return null;
            }
            try {
                return (float) Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

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
            return Jsons.doubleValue(0.0);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return DOUBLE.toJsonValue(scalar);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return DOUBLE.toJsonValue(parameters, name);
        }

    },
    DOUBLE("double", double.class) {
        @Override
        public Double toParameter(String stringValue) {
            if (stringValue == null) {
                return null;
            }
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

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
            return Jsons.doubleValue(0.0);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final double v;
            try {
                v = Double.parseDouble(scalar);
            } catch (NumberFormatException e) {
                return emptyJsonValue();
            }
            return Jsons.doubleValue(v);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.doubleValue(parameters.getDouble(name));
        }

    },
    BOOLEAN("boolean", boolean.class) {
        @Override
        public Boolean toParameter(String stringValue) {
            if (stringValue == null) {
                return null;
            }
            return Parameters.smartParseBoolean(stringValue);
        }

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
            return Jsons.booleanValue(false);
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            final Boolean v = Parameters.smartParseBoolean(scalar);
            if (v == null) {
                return emptyJsonValue();
            }
            return Jsons.booleanValue(v);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.booleanValue(parameters.getBoolean(name));
        }

    },
    STRING("String", String.class, "scalar") {
        @Override
        public String toParameter(String stringValue) {
            return stringValue;
        }

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
            return Jsons.stringValue("");
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return Jsons.stringValue(scalar);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.stringValue(parameters.getString(name));
        }

    },
    /**
     * Actually, it is also the String type, but in Java we have enum.
     */
    ENUM_STRING("String", Enum.class, "scalar") {
        @Override
        public String toParameter(String stringValue) {
            return stringValue;
        }

        @Override
        public Object toParameter(JsonValue jsonValue) {
            return STRING.toParameter(jsonValue);
        }

        @Override
        public JsonValue emptyJsonValue() {
            return STRING.emptyJsonValue();
        }

        @Override
        public JsonValue toJsonValue(String scalar) {
            return STRING.toJsonValue(scalar);
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return STRING.toJsonValue(parameters, name);
        }

    },
    SETTINGS("settings", JsonObject.class) {
        @Override
        public String toParameter(String stringValue) {
            return stringValue;
        }

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
        public JsonValue toJsonValue(String scalar) {
            Objects.requireNonNull(scalar, "Null scalar");
            try {
                return Jsons.toJson(scalar, true);
            } catch (JsonException e) {
                return emptyJsonValue();
            }
        }

        @Override
        public JsonValue toJsonValue(Parameters parameters, String name) throws NoValidParameterException {
            return Jsons.toJson(parameters.getString(name, null), true);
            // - unlike strings, null is an allowed SETTINGS parameter, meaning "empty JSON"
        }

    };

    private final String typeName;
    private final Class<?> javaType;
    private final String[] typeNameAliases;

    private static final Map<String, ValueType> ALL_TYPES = new LinkedHashMap<>();
    private static final List<String> ALL_RECOMMENDED_TYPE_NAMES = new ArrayList<>();
    private static final ValueType[] FOR_SUITABLE_JAVA_OBJECT = {SETTINGS, STRING, DOUBLE, BOOLEAN};

    static {
        for (ValueType type : values()) {
            if (type != ENUM_STRING) {
                ALL_RECOMMENDED_TYPE_NAMES.add(type.typeName);
                ALL_TYPES.put(type.typeName, type);
                for (String alias : type.typeNameAliases) {
                    ALL_TYPES.put(alias, type);
                }
            }
        }
    }

    ValueType(String typeName, Class<?> javaType, String... typeNameAliases) {
        this.typeName = Objects.requireNonNull(typeName);
        this.javaType = Objects.requireNonNull(javaType);
        this.typeNameAliases = Objects.requireNonNull(typeNameAliases);
    }

    public static Collection<String> typeNames() {
        return Collections.unmodifiableList(ALL_RECOMMENDED_TYPE_NAMES);
    }

    public String typeName() {
        return typeName;
    }

    public List<String> aliases() {
        return List.of(typeNameAliases);
    }

    public Class<?> javaType() {
        return javaType;
    }

    /**
     * Converts the given string to this type and returns it as primitive type wrapper or <code>String</code>.
     * If the passed value has invalid type, returns <code>null</code>.
     * Note: for {@link #ENUM_STRING}, this method returns <code>String</code> (standard enum type name).
     *
     * @param stringValue some String value; can be <code>null</code>, then the result will be <code>null</code>.
     * @return the corresponding primitive type / String, or <code>null</code> if it has invalid type.
     */
    public abstract Object toParameter(String stringValue);

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
        for (ValueType valueType : FOR_SUITABLE_JAVA_OBJECT) {
            // note: for example, we should not check LONG before DOUBLE
            probe = valueType.toParameter(jsonValue);
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
     * @throws NoValidParameterException if this argument does not exist or contains non-allowed value
     * (note: <code>null</code> is not allowed for all types except {@link #SETTINGS}).
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
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    public abstract JsonValue toJsonValue(String scalar);

    // Note: in the future, some types that are not currently allowed can become allowed (supported).
    public boolean isAllowedInExecutor() {
        return this != SETTINGS;
    }

    public boolean isSettings() {
        return this == SETTINGS;
    }

    public static ValueType ofTypeName(String typeName) {
        Objects.requireNonNull(typeName, "Null type name");
        return fromTypeName(typeName).orElseThrow(
                () -> new IllegalArgumentException("Unknown parameter value type \"" + typeName + "\""));
    }

    /**
     * Returns an {@link Optional} containing the {@link ValueType} with the given {@link #typeName()}.
     * <p>If no value type with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param typeName the value type name; may be {@code null}.
     * @return an optional value type.
     */
    public static Optional<ValueType> fromTypeName(String typeName) {
        return Optional.ofNullable(ALL_TYPES.get(typeName));
    }
}
