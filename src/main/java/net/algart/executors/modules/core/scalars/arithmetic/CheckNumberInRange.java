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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.Executor;
import net.algart.executors.api.HighLevelException;
import net.algart.executors.modules.core.logic.ConditionStyle;

public final class CheckNumberInRange extends Executor {
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private boolean invert = false;
    private CheckScalarsEquality.ActionOnFail actionOnFail = CheckScalarsEquality.ActionOnFail.RETURN_FALSE;
    private ConditionStyle booleanStyle = ConditionStyle.JAVA_LIKE;

    public CheckNumberInRange() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public double getMin() {
        return min;
    }

    public CheckNumberInRange setMin(double min) {
        this.min = min;
        return this;
    }

    public CheckNumberInRange setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public CheckNumberInRange setMax(double max) {
        this.max = max;
        return this;
    }

    public CheckNumberInRange setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public CheckNumberInRange setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    public CheckScalarsEquality.ActionOnFail getActionOnFail() {
        return actionOnFail;
    }

    public CheckNumberInRange setActionOnFail(CheckScalarsEquality.ActionOnFail actionOnFail) {
        this.actionOnFail = nonNull(actionOnFail);
        return this;
    }

    public ConditionStyle getBooleanStyle() {
        return booleanStyle;
    }

    public CheckNumberInRange setBooleanStyle(ConditionStyle booleanStyle) {
        this.booleanStyle = nonNull(booleanStyle);
        return this;
    }

    @Override
    public void process() {
        final String scalar = getInputScalar().getValue();
        final double value;
        try {
            value = Double.parseDouble(scalar);
        } catch (NumberFormatException e) {
            throw new HighLevelException(new IllegalArgumentException(
                    "Input value " + scalar + " is not a number " +
                            "(it must be a number " +
                            (!invert ? "inside" : "outside") + " the range "
                            + min + ".." + max + ")"));
        }
        boolean result = min <= value && value <= max;
        if (invert) {
            result = !result;
        }
        booleanStyle.setScalar(getScalar(), result);
        if (!result && actionOnFail == CheckScalarsEquality.ActionOnFail.THROW_EXCEPTION) {
            throw new HighLevelException(new IllegalArgumentException(
                    "Input number " + value + " is "
                            + (invert ? "inside" : "outside") + " the range "
                            + min + ".." + max));
        }
    }
}
