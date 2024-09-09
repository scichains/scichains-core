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
        test((scalar8, defaultCondition8) -> SScalar.toCommonBoolean(scalar8, defaultCondition8), "1", true);
        test((scalar7, defaultCondition7) -> SScalar.toCommonBoolean(scalar7, defaultCondition7), "0", false);
        test((scalar6, defaultCondition6) -> SScalar.toCommonBoolean(scalar6, defaultCondition6), "0.00", false);
        test((scalar5, defaultCondition5) -> SScalar.toCommonBoolean(scalar5, defaultCondition5), "0.01", true);
        test((scalar4, defaultCondition4) -> SScalar.toCommonBoolean(scalar4, defaultCondition4), "true", true);
        test((scalar3, defaultCondition3) -> SScalar.toCommonBoolean(scalar3, defaultCondition3), "False", false);
        test((scalar2, defaultCondition2) -> SScalar.toCommonBoolean(scalar2, defaultCondition2), "false", false);
        test((scalar1, defaultCondition1) -> SScalar.toCommonBoolean(scalar1, defaultCondition1), "", false);
        test((scalar, defaultCondition) -> SScalar.toCommonBoolean(scalar, defaultCondition), " ", true);

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
