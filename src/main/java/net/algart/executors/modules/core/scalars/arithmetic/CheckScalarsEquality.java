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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.logic.ConditionStyle;

import java.util.Objects;

public final class CheckScalarsEquality extends Executor {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    public enum ActionOnFail {
        RETURN_FALSE,
        THROW_EXCEPTION
    }

    public enum ComparisonMode {
        EQUAL() {
            @Override
            public boolean compare(String x, String y) {
                return Objects.equals(x, y);
            }

            @Override
            public boolean compare(double x, double y) {
                return x == y;
            }
        },
        UNEQUAL() {
            @Override
            public boolean compare(String x, String y) {
                return !Objects.equals(x, y);
            }

            @Override
            public boolean compare(double x, double y) {
                return x != y;
            }
        };

        public abstract boolean compare(String x, String y);

        public abstract boolean compare(double x, double y);
    }

    private ComparisonMode comparisonMode = ComparisonMode.EQUAL;
    private ActionOnFail actionOnFail = ActionOnFail.RETURN_FALSE;
    private boolean numericComparison = false;
    private ConditionStyle booleanStyle = ConditionStyle.JAVA_LIKE;

    public CheckScalarsEquality() {
        addInputScalar(INPUT_X);
        addInputScalar(INPUT_Y);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    public CheckScalarsEquality setComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = nonNull(comparisonMode);
        return this;
    }

    public ActionOnFail getActionOnFail() {
        return actionOnFail;
    }

    public CheckScalarsEquality setActionOnFail(ActionOnFail actionOnFail) {
        this.actionOnFail = nonNull(actionOnFail);
        return this;
    }

    public boolean isNumericComparison() {
        return numericComparison;
    }

    public CheckScalarsEquality setNumericComparison(boolean numericComparison) {
        this.numericComparison = numericComparison;
        return this;
    }

    public ConditionStyle getBooleanStyle() {
        return booleanStyle;
    }

    public CheckScalarsEquality setBooleanStyle(ConditionStyle booleanStyle) {
        this.booleanStyle = nonNull(booleanStyle);
        return this;
    }

    @Override
    public void process() {
        final String x;
        final String y;
        final boolean result;
        if (numericComparison) {
            x = getInputScalar(INPUT_X, false).getValue();
            y = getInputScalar(INPUT_Y, false).getValue();
            result = comparisonMode.compare(Double.parseDouble(x), Double.parseDouble(y));
        } else {
            x = getInputScalar(INPUT_X, true).getValue();
            y = getInputScalar(INPUT_Y, true).getValue();
            result = comparisonMode.compare(x, y);
        }
        booleanStyle.setScalar(getScalar(), result);
        if (!result && actionOnFail == ActionOnFail.THROW_EXCEPTION) {
            throw new AssertionError("Different scalars: " + x + " and " + y);
        }
    }
}
