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

package net.algart.json;

public abstract class PropertyChecker  {
    public static <T> T nonNull(T value, String valueName) {
        if (value == null) {
            throw new NullPointerException("Null " + valueName);
        }
        return value;
    }

    public static <T> T nonNull(T value) {
        if (value == null) {
            throw new NullPointerException("Null " + propertyName());
        }
        return value;
    }

    public static String nonEmpty(String value, String valueName) {
        nonNull(value, valueName);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty " + valueName + ", but non-empty string required");
        }
        return value;
    }

    public static String nonEmpty(String value) {
        nonNull(value);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty string \"" + propertyName()
                    + "\", but non-empty string required");
        }
        return value;
    }

    public static String nonEmptyTrimmed(String value, String valueName) {
        value = nonNull(value, valueName).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty/blank string " + valueName + ", but non-empty string required");
        }
        return value;
    }

    public static String nonEmptyTrimmed(String value) {
        value = nonNull(value).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty/blank string \"" + propertyName()
                    + "\", but non-empty string required");
        }
        return value;
    }

    /*Repeat() double ==> long,,int*/
    public static double inRange(double value, double min, double max, String valueName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + valueName + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static double inRange(double value, double min, double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + propertyName() + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static double nonLessThan(double value, double min, String valueName) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " < " + min);
        }
        return value;
    }

    public static double nonLessThan(double value, double min) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " < " + min);
        }
        return value;
    }

    public static double greaterThan(double value, double min, String valueName) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " <= " + min);
        }
        return value;
    }

    public static double greaterThan(double value, double min) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " <= " + min);
        }
        return value;
    }

    public static double nonNegative(double value, String valueName) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + valueName + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static double nonNegative(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + propertyName() + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static double positive(double value, String valueName) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + valueName + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }

    public static double positive(double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + propertyName() + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public static long inRange(long value, long min, long max, String valueName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + valueName + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static long inRange(long value, long min, long max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + propertyName() + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static long nonLessThan(long value, long min, String valueName) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " < " + min);
        }
        return value;
    }

    public static long nonLessThan(long value, long min) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " < " + min);
        }
        return value;
    }

    public static long greaterThan(long value, long min, String valueName) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " <= " + min);
        }
        return value;
    }

    public static long greaterThan(long value, long min) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " <= " + min);
        }
        return value;
    }

    public static long nonNegative(long value, String valueName) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + valueName + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static long nonNegative(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + propertyName() + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static long positive(long value, String valueName) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + valueName + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }

    public static long positive(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + propertyName() + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }


    public static int inRange(int value, int min, int max, String valueName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + valueName + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static int inRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Illegal " + propertyName() + " = " + value
                    + ": it must be in " + min + ".." + max + " range");
        }
        return value;
    }

    public static int nonLessThan(int value, int min, String valueName) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " < " + min);
        }
        return value;
    }

    public static int nonLessThan(int value, int min) {
        if (value < min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " < " + min);
        }
        return value;
    }

    public static int greaterThan(int value, int min, String valueName) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + valueName + " = " + value + " <= " + min);
        }
        return value;
    }

    public static int greaterThan(int value, int min) {
        if (value <= min) {
            throw new IllegalArgumentException("Too small " + propertyName() + " = " + value + " <= " + min);
        }
        return value;
    }

    public static int nonNegative(int value, String valueName) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + valueName + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static int nonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative " + propertyName() + " = " + value + " (it is prohibited)");
        }
        return value;
    }

    public static int positive(int value, String valueName) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + valueName + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }

    public static int positive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Zero or negative " + propertyName() + " = " + value
                    + " (it is prohibited)");
        }
        return value;
    }

    /*Repeat.AutoGeneratedEnd*/

    public static double doubleOrNegativeInfinity(String value) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? Double.NEGATIVE_INFINITY : Double.parseDouble(value);
    }

    public static double doubleOrPositiveInfinity(String value) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? Double.POSITIVE_INFINITY : Double.parseDouble(value);
    }

    public static Integer intOrNull(String value) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? null : Integer.valueOf(value);
    }

    public static Long longOrNull(String value) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? null : Long.valueOf(value);
    }

    public static Double doubleOrNull(String value) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? null : Double.valueOf(value);
    }

    public static int intOrDefault(String value, int defaultValue) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    public static long longOrDefault(String value, long defaultValue) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? defaultValue : Long.parseLong(value);
    }

    public static double doubleOrDefault(String value, double defaultValue) {
        value = value == null ? "" : value.trim();
        return value.isEmpty() ? defaultValue : Double.parseDouble(value);
    }


    public static String propertyName() {
        return PropertyCheckerHelper.findPropertyNameFromCurrentSetter("value");
    }

}
