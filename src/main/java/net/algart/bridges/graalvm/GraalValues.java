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

package net.algart.bridges.graalvm;

import net.algart.executors.api.data.SScalar;
import org.graalvm.polyglot.Value;

import java.util.Objects;

public class GraalValues {
    private GraalValues() {
    }

    public static boolean toSmartBoolean(Value value) {
        Objects.requireNonNull(value, "Null value");
        if (value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        final String s = String.valueOf(value.as(Object.class));
        return Boolean.parseBoolean(s);
    }

    public static double toSmartDouble(Value value) {
        Objects.requireNonNull(value, "Null value");
        if (value.isNumber()) {
            if (value.fitsInDouble()) {
                return value.asDouble();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
        }
        final Object object = value.isNull() ? null : value.as(Object.class);
        return object == null ? Double.NaN : Double.parseDouble(object.toString());
    }

    public static String toSmartString(Value value, boolean briefIntegerForm) {
        Objects.requireNonNull(value, "Null value");
        if (value.isNull()) {
            return null;
        }
        if (briefIntegerForm && value.isNumber() && value.fitsInDouble()) {
            final double v = value.asDouble();
            if (v == (int) v) {
                return String.valueOf((int) v);
            }
            return String.valueOf(v);
        }
        final Object object = value.as(Object.class);
        if (object instanceof final SScalar scalar) {
            return scalar.getValue();
        }
        return String.valueOf(object);
    }
}
