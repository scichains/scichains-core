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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.arrays.IntArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;

import java.util.ArrayList;
import java.util.List;

public final class MergeBinaryMasks extends SeveralMultiMatricesOperation {
    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final List<Matrix<? extends PArray>> mNonNull = new ArrayList<>();
        final List<Double> aForNonNull = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            final MultiMatrix multiMatrix = sources.get(i);
            if (multiMatrix != null) {
                aForNonNull.add((double) (i + 1));
                mNonNull.add(multiMatrix.intensityChannel());
            }
        }
        final double[] a = new double[aForNonNull.size()];
        for (int k = 0; k < a.length; k++) {
            a[k] = aForNonNull.get(k);
        }
        return MultiMatrix.valueOfMono(
            Matrices.asFuncMatrix(new AbstractFunc() {
                @Override
                public double get(double... x) {
                    for (int index = x.length - 1; index >= 0; index--) {
                        if (x[index] != 0.0) {
                            return a[index];
                        }
                    }
                    return 0.0;
                }
            }, IntArray.class, mNonNull));
    }
}