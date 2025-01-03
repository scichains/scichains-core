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

package net.algart.executors.modules.core.logic.control;

import net.algart.executors.api.Executor;

public final class CopySeveral extends Executor {
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
    public static final String S6 = "s6";
    public static final String X6 = "x6";
    public static final String M6 = "m6";
    public static final String S7 = "s7";
    public static final String X7 = "x7";
    public static final String M7 = "m7";
    public static final String S8 = "s8";
    public static final String X8 = "x8";
    public static final String M8 = "m8";
    public static final String S9 = "s9";
    public static final String X9 = "x9";
    public static final String M9 = "m9";
    public static final String S10 = "s10";
    public static final String X10 = "x10";
    public static final String M10 = "m10";

    public CopySeveral() {
        useVisibleResultParameter();
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
        addOutputScalar(S6);
        addOutputNumbers(X6);
        addOutputMat(M6);
        addOutputScalar(S7);
        addOutputNumbers(X7);
        addOutputMat(M7);
        addOutputScalar(S8);
        addOutputNumbers(X8);
        addOutputMat(M8);
        addOutputScalar(S9);
        addOutputNumbers(X9);
        addOutputMat(M9);
        addOutputScalar(S10);
        addOutputNumbers(X10);
        addOutputMat(M10);
    }

    @Override
    public void process() {
        getScalar(S1).exchange(getInputScalar(S1, true));
        getScalar(S2).exchange(getInputScalar(S2, true));
        getScalar(S3).exchange(getInputScalar(S3, true));
        getScalar(S4).exchange(getInputScalar(S4, true));
        getScalar(S5).exchange(getInputScalar(S5, true));
        getScalar(S6).exchange(getInputScalar(S6, true));
        getScalar(S7).exchange(getInputScalar(S7, true));
        getScalar(S8).exchange(getInputScalar(S8, true));
        getScalar(S9).exchange(getInputScalar(S9, true));
        getScalar(S10).exchange(getInputScalar(S10, true));
        getNumbers(X1).exchange(getInputNumbers(X1, true));
        getNumbers(X2).exchange(getInputNumbers(X2, true));
        getNumbers(X3).exchange(getInputNumbers(X3, true));
        getNumbers(X4).exchange(getInputNumbers(X4, true));
        getNumbers(X5).exchange(getInputNumbers(X5, true));
        getNumbers(X6).exchange(getInputNumbers(X6, true));
        getNumbers(X7).exchange(getInputNumbers(X7, true));
        getNumbers(X8).exchange(getInputNumbers(X8, true));
        getNumbers(X9).exchange(getInputNumbers(X9, true));
        getNumbers(X10).exchange(getInputNumbers(X10, true));
        getMat(M1).exchange(getInputMat(M1, true));
        getMat(M2).exchange(getInputMat(M2, true));
        getMat(M3).exchange(getInputMat(M3, true));
        getMat(M4).exchange(getInputMat(M4, true));
        getMat(M5).exchange(getInputMat(M5, true));
        getMat(M6).exchange(getInputMat(M6, true));
        getMat(M7).exchange(getInputMat(M7, true));
        getMat(M8).exchange(getInputMat(M8, true));
        getMat(M9).exchange(getInputMat(M9, true));
        getMat(M10).exchange(getInputMat(M10, true));
    }
}
