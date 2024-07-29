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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.executors.modules.core.scalars.arithmetic.ProductOfTwoPowers;
import net.algart.math.functions.MultiplyingFunc;
import net.algart.math.functions.PowerFunc;

import java.util.List;

public final class MatrixProductOfTwoPowers extends SeveralMultiMatricesChannelOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    private double a = 1.0;
    private double b = 1.0;
    private double m = 1.0;

    public MatrixProductOfTwoPowers() {
        super(INPUT_X, INPUT_Y);
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public void setA(String a) {
        setA(ProductOfTwoPowers.smartParseDouble(a));
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public void setB(String b) {
        setB(ProductOfTwoPowers.smartParseDouble(b));
    }

    public double getM() {
        return m;
    }

    public void setM(double m) {
        this.m = m;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> matrices) {
        Matrix<? extends PArray> x = matrices.get(0);
        Matrix<? extends PArray> y = matrices.get(1);
        if (x.array().maxPossibleValue(1.0) != 1.0) {
            x = Matrices.asPrecision(x, float.class);
        }
        if (y == null) {
            return Matrices.asFuncMatrix(PowerFunc.getInstance(a, m), FloatArray.class, x);
        } else {
            if (y.array().maxPossibleValue(1.0) != 1.0) {
                y = Matrices.asPrecision(y, float.class);
            }
            x = Matrices.asFuncMatrix(PowerFunc.getInstance(a, 1.0), DoubleArray.class, x);
            y = Matrices.asFuncMatrix(PowerFunc.getInstance(b, 1.0), DoubleArray.class, y);
            return Matrices.clone(Matrices.asFuncMatrix(MultiplyingFunc.getInstance(m), FloatArray.class, x, y));
        }
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex > 0;
    }
}