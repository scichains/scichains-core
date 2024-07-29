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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.arrays.*;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.SeveralNumberArraysOperation;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.List;

// a*x [+ b*y if y exists] + summand
public final class LinearCombinationOfTwoNumbers extends SeveralNumberArraysOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    private double a = 1.0;
    private double b = 1.0;
    private double summand = 0.0;
    private boolean absoluteValue = false;

    public LinearCombinationOfTwoNumbers() {
        super(INPUT_X, INPUT_Y);
    }

    public double getA() {
        return a;
    }

    public LinearCombinationOfTwoNumbers setA(double a) {
        this.a = a;
        return this;
    }

    public double getB() {
        return b;
    }

    public LinearCombinationOfTwoNumbers setB(double b) {
        this.b = b;
        return this;
    }

    public double getSummand() {
        return summand;
    }

    public LinearCombinationOfTwoNumbers setSummand(double summand) {
        this.summand = summand;
        return this;
    }

    public boolean isAbsoluteValue() {
        return absoluteValue;
    }

    public void setAbsoluteValue(boolean absoluteValue) {
        this.absoluteValue = absoluteValue;
    }

    @Override
    public PArray process(List<PNumberArray> sources, int... blockLengths) {
        PNumberArray x = sources.get(0);
        PNumberArray y = sources.get(1);
        Class<? extends PNumberArray> resultType = x instanceof DoubleArray ? DoubleArray.class : FloatArray.class;
        PArray result;
        if (y == null) {
            result = Arrays.asFuncArray(LinearFunc.getInstance(summand, a), resultType, x);
        } else {
            result = Arrays.asFuncArray(LinearFunc.getInstance(summand, a, b), resultType, x, y);
        }
        if (absoluteValue) {
            result = Arrays.asFuncArray(Func.ABS, resultType, result);
        }
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex > 0;
    }
}
