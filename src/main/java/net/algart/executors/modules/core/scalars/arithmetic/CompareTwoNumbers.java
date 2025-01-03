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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.logic.ConditionStyle;

public final class CompareTwoNumbers extends Executor {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";
    public static final String OUTPUT_EQUAL = "equal";
    public static final String OUTPUT_NON_EQUAL = "non_equal";
    public static final String OUTPUT_LESS = "less";
    public static final String OUTPUT_LESS_OR_EQUAL = "less_or_equal";
    public static final String OUTPUT_GREATER = "greater";
    public static final String OUTPUT_GREATER_OR_EQUAL = "greater_or_equal";

    private ConditionStyle booleanStyle = ConditionStyle.JAVA_LIKE;
    private double defaultX = 0.0;
    private double defaultY = 0.0;

    public CompareTwoNumbers() {
        addInputScalar(INPUT_X);
        addInputScalar(INPUT_Y);
        addOutputScalar(OUTPUT_EQUAL);
        addOutputScalar(OUTPUT_NON_EQUAL);
        addOutputScalar(OUTPUT_LESS);
        addOutputScalar(OUTPUT_LESS_OR_EQUAL);
        addOutputScalar(OUTPUT_GREATER);
        addOutputScalar(OUTPUT_GREATER_OR_EQUAL);
        useVisibleResultParameter();
    }

    public ConditionStyle getBooleanStyle() {
        return booleanStyle;
    }

    public CompareTwoNumbers setBooleanStyle(ConditionStyle booleanStyle) {
        this.booleanStyle = nonNull(booleanStyle);
        return this;
    }

    public double getDefaultX() {
        return defaultX;
    }

    public CompareTwoNumbers setDefaultX(double defaultX) {
        this.defaultX = defaultX;
        return this;
    }

    public double getDefaultY() {
        return defaultY;
    }

    public CompareTwoNumbers setDefaultY(double defaultY) {
        this.defaultY = defaultY;
        return this;
    }

    @Override
    public void process() {
        final double x = getInputScalar(INPUT_X, true).toDoubleOrDefault(defaultX);
        final double y = getInputScalar(INPUT_Y, true).toDoubleOrDefault(defaultY);
        booleanStyle.setScalar(getScalar(OUTPUT_EQUAL), x == y);
        booleanStyle.setScalar(getScalar(OUTPUT_NON_EQUAL), x != y);
        booleanStyle.setScalar(getScalar(OUTPUT_LESS), x < y);
        booleanStyle.setScalar(getScalar(OUTPUT_LESS_OR_EQUAL), x <= y);
        booleanStyle.setScalar(getScalar(OUTPUT_GREATER), x > y);
        booleanStyle.setScalar(getScalar(OUTPUT_GREATER_OR_EQUAL), x >= y);
    }
}
