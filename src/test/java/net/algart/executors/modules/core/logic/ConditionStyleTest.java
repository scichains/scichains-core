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

package net.algart.executors.modules.core.logic;

import net.algart.executors.api.data.SScalar;

import java.util.function.BiFunction;

public class ConditionStyleTest {
    private static void test(BiFunction<String, Boolean, Boolean> converter, String s, boolean expected) {
        if (converter.apply(s, false) != expected) {
            throw new AssertionError();
        }
        if (converter.apply(s, true) != expected) {
            throw new AssertionError();
        }
        if (converter.apply(null, false)) {
            throw new AssertionError();
        }
        if (!converter.apply(null, true)) {
            throw new AssertionError();
        }
    }

    public static void main(String[] args) {
        test(SScalar::toCommonBoolean, "1", true);
        test(SScalar::toCommonBoolean, "0", false);
        test(SScalar::toCommonBoolean, "0.00", false);
        test(SScalar::toCommonBoolean, "0.01", true);
        test(SScalar::toCommonBoolean, "true", true);
        test(SScalar::toCommonBoolean, "False", false);
        test(SScalar::toCommonBoolean, "false", false);
        test(SScalar::toCommonBoolean, "", false);
        test(SScalar::toCommonBoolean, " ", true);

        test(ConditionStyle.C_LIKE::toBoolean, "1", true);
        test(ConditionStyle.C_LIKE::toBoolean, "0", false);
        test(ConditionStyle.C_LIKE::toBoolean, "0.00", false);
        test(ConditionStyle.C_LIKE::toBoolean, "0.01", true);
        test(ConditionStyle.C_LIKE::toBoolean, "true", true);
        test(ConditionStyle.C_LIKE::toBoolean, "false", true);
        test(ConditionStyle.C_LIKE::toBoolean, "", false);
        test(ConditionStyle.C_LIKE::toBoolean, " ", true);

        test(ConditionStyle.JAVA_LIKE::toBoolean, "1", false);
        test(ConditionStyle.JAVA_LIKE::toBoolean, "0", false);
        test(ConditionStyle.JAVA_LIKE::toBoolean, "true", true);
        test(ConditionStyle.JAVA_LIKE::toBoolean, "TRUE", true);
        test(ConditionStyle.JAVA_LIKE::toBoolean, "false", false);
        test(ConditionStyle.JAVA_LIKE::toBoolean, " ", false);
        test(ConditionStyle.JAVA_LIKE::toBoolean, "", false);
    }
}
