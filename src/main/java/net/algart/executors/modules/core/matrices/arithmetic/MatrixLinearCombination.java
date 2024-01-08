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

import net.algart.executors.modules.core.common.OptionalArguments;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.arrays.Arrays;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.LinearFunc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MatrixLinearCombination extends SeveralMultiMatricesChannelOperation {
    private Map<Integer, Double> a = new HashMap<>();
    private double b = 0.0;
    private boolean rawValues = false;

    public double getA(int index) {
        return a.getOrDefault(index,  0.0);
    }

    public void setA(int index, double a) {
        this.a.put(index, a);
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public void setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.startsWith("a_")) {
            final int index;
            try {
                index = Integer.parseInt(name.substring("a_".length())) - 1;
            } catch (NumberFormatException ignored) {
                return;
            }
            setA(index, parameters().getDouble(name));
            return;
        }
        super.onChangeParameter(name);
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m) {
        final OptionalArguments<Matrix<? extends PArray>> arguments = new OptionalArguments<>(m);
        final List<Matrix<? extends PArray>> mNonNull = arguments.extract();
        final double[] aForNonNull = arguments.extractParallelDoubles(this.a, 1.0);
        final double scale = rawValues ? 1.0 : Arrays.maxPossibleValue(sampleType(), 1.0);
        return Matrices.clone(
                Matrices.asFuncMatrix(LinearFunc.getInstance(scale * b, aForNonNull), sampleType(), mNonNull));
    }
}
