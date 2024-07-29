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


import java.util.*;

/**
 * @author mnogono
 * Created on 31.05.2017.
 */
class ParametersImpl implements Parameters {
    private final Map<String, Object> properties = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public boolean getBoolean(String name) throws NoValidParameterException {
        final Object o = get(name);
        if (o == null) {
            throw new NoValidParameterException("No required boolean parameter \"" + name + "\"");
        }
        final String s = o.toString();
        final Boolean result = Parameters.smartParseBoolean(s);
        if (result != null) {
            return result;
        } else {
            throw new NoValidParameterException("Parameter \""
                    + name + "\" is not a boolean: \"" + s + "\"");
        }
    }

    @Override
    public boolean getBoolean(String name, boolean defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        final String s = o.toString();
        final Boolean result = Parameters.smartParseBoolean(s);
        return result != null ? result : defaultValue;
    }

    @Override
    public void setBoolean(String name, boolean value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
    }

    @Override
    public void setBoolean(String name, String value) {
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
    }

    @Override
    public int getInteger(String name) throws NoValidParameterException {
        final Object o = get(name);
        if (o == null) {
            throw new NoValidParameterException("No required integer parameter \"" + name + "\"");
        }
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

    @Override
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

    @Override
    public void setInteger(String name, int value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
    }

    @Override
    public void setInteger(String name, String value) {
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
    }

    @Override
    public long getLong(String name) throws NoValidParameterException {
        final Object o = get(name);
        if (o == null) {
            throw new NoValidParameterException("No required long integer parameter \"" + name + "\"");
        }
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

    @Override
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

    @Override
    public void setLong(String name, long value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
    }

    @Override
    public void setLong(String name, String value) {
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
    }

    @Override
    public double getDouble(String name) throws NoValidParameterException {
        final Object o = get(name);
        if (o == null) {
            throw new NoValidParameterException("No required double parameter \"" + name + "\"");
        }
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

    @Override
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

    @Override
    public void setDouble(String name, double value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
    }

    @Override
    public void setDouble(String name, String value) {
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
    }

    @Override
    public String getString(String name) throws NoValidParameterException {
        final Object o = get(name);
        if (o == null) {
            throw new NoValidParameterException("No required parameter \"" + name + "\"");
        }
        return o.toString();
    }

    @Override
    public String getString(String name, String defaultValue) {
        final Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        return o.toString();
    }

    @Override
    public void setString(String name, String value) {
        Objects.requireNonNull(name, "Null parameter name");
        put(name, value);
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return properties.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return properties.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return properties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        properties.putAll(m);
    }

    @Override
    public void clear() {
        properties.clear();
    }

    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public Collection<Object> values() {
        return properties.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    public boolean equals(Object o) {
        return properties.equals(o);
    }

    public int hashCode() {
        return properties.hashCode();
    }
}
