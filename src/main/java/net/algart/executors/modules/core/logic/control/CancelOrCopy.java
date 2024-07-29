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

package net.algart.executors.modules.core.logic.control;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.logic.ConditionStyle;

public final class CancelOrCopy extends Executor {
    public static final String INPUT_CONDITION = "if";
    public static final String OUTPUT_CHECKED_CONDITION = "checked_condition";
    public static final String S1 = "s";
    public static final String X1 = "x";
    public static final String M1 = "m";
    public static final String S2 = "s2";
    public static final String X2 = "x2";
    public static final String M2 = "m2";
    public static final String S3 = "s3";
    public static final String X3 = "x3";
    public static final String M3 = "m3";
    public static final String S4 = "s4";
    public static final String X4 = "x4";
    public static final String M4 = "m4";
    public static final String S5 = "s5";
    public static final String X5 = "x5";
    public static final String M5 = "m5";

    private ConditionStyle conditionStyle = ConditionStyle.JAVA_LIKE;
    private boolean invert = false;

    public CancelOrCopy() {
        useVisibleResultParameter();
        addInputScalar(INPUT_CONDITION);
        addInputScalar(S1);
        addInputNumbers(X1);
        addInputMat(M1);
        addInputScalar(S2);
        addInputNumbers(X2);
        addInputMat(M2);
        addInputScalar(S3);
        addInputNumbers(X3);
        addInputMat(M3);
        addInputScalar(S4);
        addInputNumbers(X4);
        addInputMat(M4);
        addInputScalar(S5);
        addInputNumbers(X5);
        addInputMat(M5);
        addOutputScalar(OUTPUT_CHECKED_CONDITION);
        addOutputScalar(S1);
        addOutputNumbers(X1);
        addOutputMat(M1);
        addOutputScalar(S2);
        addOutputNumbers(X2);
        addOutputMat(M2);
        addOutputScalar(S3);
        addOutputNumbers(X3);
        addOutputMat(M3);
        addOutputScalar(S4);
        addOutputNumbers(X4);
        addOutputMat(M4);
        addOutputScalar(S5);
        addOutputNumbers(X5);
        addOutputMat(M5);
    }

    public ConditionStyle getConditionStyle() {
        return conditionStyle;
    }

    public CancelOrCopy setConditionStyle(ConditionStyle conditionStyle) {
        this.conditionStyle = nonNull(conditionStyle);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public CancelOrCopy setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    public void process() {
        final boolean condition = condition();
        if (condition) {
            requestCancellingFurtherExecution();
        } else {
            // - by default, cancellation flags in outputs were cleared by execute() method before calling this method
            getScalar(S1).exchange(getInputScalar(S1, true));
            getScalar(S2).exchange(getInputScalar(S2, true));
            getScalar(S3).exchange(getInputScalar(S3, true));
            getScalar(S4).exchange(getInputScalar(S4, true));
            getScalar(S5).exchange(getInputScalar(S5, true));
            getNumbers(X1).exchange(getInputNumbers(X1, true));
            getNumbers(X2).exchange(getInputNumbers(X2, true));
            getNumbers(X3).exchange(getInputNumbers(X3, true));
            getNumbers(X4).exchange(getInputNumbers(X4, true));
            getNumbers(X5).exchange(getInputNumbers(X5, true));
            getMat(M1).exchange(getInputMat(M1, true));
            getMat(M2).exchange(getInputMat(M2, true));
            getMat(M3).exchange(getInputMat(M3, true));
            getMat(M4).exchange(getInputMat(M4, true));
            getMat(M5).exchange(getInputMat(M5, true));
        }
        getScalar(OUTPUT_CHECKED_CONDITION).setTo(condition);
        // - overrides cancelling request in this port
    }

    public boolean condition() {
        final String conditionString = getInputScalar(INPUT_CONDITION, true).getValue();
        return conditionStyle.toBoolean(conditionString, false) != invert;
    }
}
