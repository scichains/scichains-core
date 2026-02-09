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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import net.algart.external.UsedForExternalCommunication;
import net.algart.json.Jsons;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class Parameters implements Map<String, Object> {
    private final Map<String, Object> map = Collections.synchronizedMap(new LinkedHashMap<>());

    public Parameters() {
    }

    public Parameters(Map<String, Object> m) {
        map.putAll(m);
    }

    public boolean getBoolean(String name) throws NoValidParameterException {
        final Object o = getAndCheckNull(name, "boolean");
        final String s = o.toString();
        final Boolean result = Parameters.smartParseBoolean(s);
        if (result != null) {
            return result;
        } else {
            throw new NoValidParameterException("Parameter \""
                    + name + "\" is not a boolean: \"" + s + "\"");
        }
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        final String s = o.toString();
        final Boolean result = Parameters.smartParseBoolean(s);
        return result != null ? result : defaultValue;
    }

    @UsedForExternalCommunication
    public Parameters setBoolean(String name, boolean value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
        return this;
    }

    public Parameters setBoolean(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        if (value == null) {
            throw new IllegalArgumentException("Cannot set boolean parameter \"" + name + "\" to null value");
        }
        final Boolean booleanValue = Parameters.smartParseBoolean(value);
        if (booleanValue != null) {
            setBoolean(name, booleanValue);
        } else {
            throw new NoValidParameterException("Cannot set boolean parameter \""
                    + name + "\" to non-boolean value \"" + value + "\"");
        }
        return this;
    }

    public int getInteger(String name) throws NoValidParameterException {
        final Object o = getAndCheckNull(name, "integer");
        if (o instanceof Integer) {
            return (Integer) o;
        }
        final String s = o.toString();
        try {
            return Parameters.smartParseInt(s);
        } catch (NumberFormatException e) {
            throw new NoValidParameterException("Parameter \""
                    + name + "\" is not a valid integer value: \"" + s + "\"");
        }
    }

    public int getInteger(String name, int defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        final String s = o.toString();
        try {
            return Parameters.smartParseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @UsedForExternalCommunication
    public Parameters setInteger(String name, int value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
        return this;
    }

    public Parameters setInteger(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        if (value == null) {
            throw new IllegalArgumentException("Cannot set int parameter \"" + name + "\" to null value");
        }
        try {
            final int integerValue = Parameters.smartParseInt(value);
            setInteger(name, integerValue);
        } catch (NumberFormatException e) {
            throw new NoValidParameterException("Cannot set int parameter \""
                    + name + "\" to non-integer value \"" + value + "\"");
        }
        return this;
    }

    public long getLong(String name) throws NoValidParameterException {
        final Object o = getAndCheckNull(name, "long integer");
        if (o instanceof Long) {
            return (Long) o;
        }
        final String s = o.toString();
        try {
            return Parameters.smartParseLong(s);
        } catch (NumberFormatException e) {
            throw new NoValidParameterException("Parameter \""
                    + name + "\" is not a valid long integer value: \"" + s + "\"");
        }
    }

    public long getLong(String name, long defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        final String s = o.toString();
        try {
            return Parameters.smartParseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @UsedForExternalCommunication
    public Parameters setLong(String name, long value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
        return this;
    }

    public Parameters setLong(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        if (value == null) {
            throw new IllegalArgumentException("Cannot set long parameter \"" + name + "\" to null value");
        }
        try {
            final long longValue = Parameters.smartParseLong(value);
            setLong(name, longValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot set long parameter \""
                    + name + "\" to non-long value \"" + value + "\"");
        }
        return this;
    }

    public double getDouble(String name) throws NoValidParameterException {
        final Object o = getAndCheckNull(name, "double");
        if (o instanceof Double) {
            return (Double) o;
        }
        final String s = o.toString();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new NoValidParameterException("Parameter \""
                    + name + "\" is not a valid double value: \"" + s + "\"");
        }
    }

    public double getDouble(String name, double defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        if (o instanceof Double) {
            return (Double) o;
        }
        final String s = o.toString();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @UsedForExternalCommunication
    public Parameters setDouble(String name, double value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
        return this;
    }

    public Parameters setDouble(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        if (value == null) {
            throw new IllegalArgumentException("Cannot set double parameter \"" + name + "\" to null value");
        }
        try {
            final double doubleValue = Double.parseDouble(value);
            setDouble(name, doubleValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot set double parameter \""
                    + name + "\" to non-double value \"" + value + "\"");
        }
        return this;
    }

    public String getString(String name) throws NoValidParameterException {
        final Object o = getAndCheckNull(name, "string");
        return o.toString();
    }

    public String getString(String name, String defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        return o.toString();
    }

    @UsedForExternalCommunication
    public Parameters setString(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
        return this;
    }

    public Parameters set(String name, Object value) {
        put(name, value);
        return this;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Parameters that = (Parameters) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }

    public JsonValue getJsonValue(String name) {
        return smartParseValue(get(name));
    }

    public JsonObject toJson() {
        return toJson(this);
    }

    private Object getAndCheckNull(String name, String typeTitle) {
        final Object result = get(name);
        if (result == null) {
            if (!containsKey(name)) {
                throw new NoValidParameterException("No required " + typeTitle + " parameter \"" + name + "\"");
            } else {
                throw new NoValidParameterException("Parameter \"" +
                        name + "\" is null, it is not a valid " + typeTitle + " value");
            }
        }
        return result;
    }

    public static JsonObject toJson(Map<String, Object> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            builder.add(entry.getKey(), smartParseValue(entry.getValue()));
        }
        return builder.build();
    }

    public static JsonValue smartParseValue(Object o) {
        if (o == null) {
            return JsonValue.NULL;
        }
        if (o instanceof Boolean) {
            return (Boolean) o ? JsonValue.TRUE : JsonValue.FALSE;
        }
        if (o instanceof Float || o instanceof Double) {
            return Jsons.doubleValue(((Number) o).doubleValue());
        }
        if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long) {
            return Jsons.longValue(((Number) o).longValue());
        }
        return Jsons.stringValue(o.toString());
    }

    public static Boolean smartParseBoolean(String s) {
        if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        } else if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    public static long smartParseLong(String s) throws NumberFormatException {
        NumberFormatException exception;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            exception = e;
        }
        double value = Double.parseDouble(s);
        long result = (long) value;
        if (result == value) {
            return result;
        } else {
            throw exception;
        }
    }

    public static int smartParseInt(String s) throws NumberFormatException {
        NumberFormatException exception;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            exception = e;
        }
        double value = Double.parseDouble(s);
        int result = (int) value;
        if (result == value) {
            return result;
        } else {
            throw exception;
        }
    }
}
