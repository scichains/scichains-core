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

package net.algart.executors.api.parameters;

import net.algart.external.UsedByNativeCode;
import net.algart.json.Jsons;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.util.Map;
import java.util.Objects;

/**
 * @author mnogono
 * Created on 31.05.2017.
 */
public interface Parameters extends Map<String, Object> {
    static Parameters newInstance() {
        return new ParametersImpl();
    }

    boolean getBoolean(String name) throws NoValidParameterException;

    boolean getBoolean(String name, boolean defaultValue);

    @UsedByNativeCode
    void setBoolean(String name, boolean value);

    void setBoolean(String name, String value);

    int getInteger(String name) throws NoValidParameterException;

    int getInteger(String name, int defaultValue);

    @UsedByNativeCode
    void setInteger(String name, int value);

    void setInteger(String name, String value);

    long getLong(String name) throws NoValidParameterException;

    long getLong(String name, long defaultValue);

    @UsedByNativeCode
    void setLong(String name, long value);

    void setLong(String name, String value);

    double getDouble(String name) throws NoValidParameterException;

    double getDouble(String name, double defaultValue);

    @UsedByNativeCode
    void setDouble(String name, double value);

    void setDouble(String name, String value);

    String getString(String name) throws NoValidParameterException;

    String getString(String name, String defaultValue);

    @UsedByNativeCode
    void setString(String name, String value);

    default JsonValue getJsonValue(String name) {
        return smartParseValue(get(name));
    }

    default JsonObject toJson() {
        return toJson(this);
    }

    static JsonObject toJson(Map<String, Object> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            builder.add(entry.getKey(), smartParseValue(entry.getValue()));
        }
        return builder.build();
    }

    static JsonValue smartParseValue(Object o) {
        if (o == null) {
            return JsonValue.NULL;
        }
        if (o instanceof Boolean) {
            return (Boolean) o ? JsonValue.TRUE : JsonValue.FALSE;
        }
        if (o instanceof Float || o instanceof Double) {
            return Jsons.toJsonDoubleValue(((Number) o).doubleValue());
        }
        if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long) {
            return Jsons.toJsonLongValue(((Number) o).longValue());
        }
        return Jsons.toJsonStringValue(o.toString());
    }

    static Boolean smartParseBoolean(String s) {
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equalsIgnoreCase("false")) {
            return false;
        } else {
            return null;
        }
    }

    static long smartParseLong(String s) throws NumberFormatException  {
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

    static int smartParseInt(String s) throws NumberFormatException {
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
