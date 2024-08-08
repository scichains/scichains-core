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

package net.algart.executors.modules.core.common.matrices;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.List;

abstract class BitMultiMatrixProcessing extends SeveralMultiMatricesProcessing {
    BitMultiMatrixProcessing(String... predefinedInputPortNames) {
        super(predefinedInputPortNames);
        if (predefinedInputPortNames.length == 0) {
            throw new IllegalArgumentException("Empty predefinedInputPortNames");
        }
    }

    public Matrix<? extends PArray> extend(Matrix<? extends PArray> matrix) {
        final int d = zeroExtendingValue();
        return d == 0 ? matrix :
                matrix.subMatrix(-d, -d, matrix.dimX() + d, matrix.dimY() + d,
                        Matrix.ContinuationMode.ZERO_CONSTANT);
        // - important: after toBit call this constant stays zero
    }

    public Matrix<? extends PArray> reduce(Matrix<? extends PArray> matrix) {
        final int d = zeroExtendingValue();
        return d == 0 ?
                matrix :
                matrix.subMatrix(d, d, matrix.dimX() - d, matrix.dimY() - d).clone();
        // - important: cloning improves performance in many solutions with non-binary results like int[] labels
    }

    public void clearBorderInExtended(Matrix<? extends UpdatablePArray> matrix) {
        final int d = zeroExtendingValue();
        if (d <= 0) {
            return;
        }
        final long dimY = matrix.dimY();
        final long dimX = matrix.dimX();
        if (dimX < 2L * d || dimY < 2L * d) {
            throw new IllegalArgumentException("The matrix was not extended by " + d + " pixels - " + matrix);
        }
        Arrays.zeroFill(matrix.subMatrix(0, 0, dimX, d).array());
        Arrays.zeroFill(matrix.subMatrix(0, dimY - d, dimX, dimY).array());
        Arrays.zeroFill(matrix.subMatrix(0, d, d, dimY - d).array());
        Arrays.zeroFill(matrix.subMatrix(dimX - d, d, dimX, dimY - d).array());
    }

    protected boolean bitInput(int inputIndex) {
        return true;
    }

    protected boolean convertToBit(int inputIndex) {
        return true;
    }

    // Necessary for operations to provide zeroes for some optimized bit scanning algorithms
    protected boolean zeroExtending() {
        return false;
    }

    // Usually should not be changed
    protected int zeroExtendingValue() {
        return zeroExtending() ? 1 : 0;
    }

    @Override
    Object process(List<MultiMatrix> sources, boolean resultRequired) {
        final List<Matrix<? extends UpdatablePArray>> bitMatrices = new ArrayList<>();
        final List<MultiMatrix2D> sources2D = MultiMatrix.asMultiMatrices2D(sources);
        for (int i = 0, n = sources2D.size(); i < n; i++) {
            final MultiMatrix2D source2D = sources2D.get(i);
            if (source2D == null) {
                bitMatrices.add(null);
            } else {
                Matrix<? extends PArray> intensity = extend(source2D.intensityChannel());
                bitMatrices.add(!bitInput(i) ? null
                        : convertToBit(i) ? toBit(intensity)
                        : Matrices.clone(intensity));
            }
        }
        return process(bitMatrices, sources2D, resultRequired);
    }

    // Some of bitMatrices will be null, if !bitInput(index)
    abstract Object process(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources,
            boolean resultRequired);

    public static Matrix<UpdatableBitArray> toBit(final Matrix<? extends PArray> intensity) {
        if (intensity == null) {
            return null;
        }
        final Matrix<UpdatableBitArray> result = Arrays.SMM.newBitMatrix(intensity.dimensions());
        Matrices.applyFunc(null, Func.IDENTITY, result, intensity);
        return result;
    }

    public static Matrix<UpdatableBitArray> asBit(final Matrix<? extends PArray> intensity) {
        if (intensity == null) {
            return null;
        }
        return intensity.cast(UpdatableBitArray.class);
    }

    public static Matrix<UpdatableBitArray> cloneBit(Matrix<? extends BitArray> matrix) {
        if (matrix == null) {
            return null;
        }
        Matrix<UpdatableBitArray> clone = Arrays.SMM.newBitMatrix(matrix.dimensions());
        Matrices.copy(null, clone, matrix);
        return clone;
    }
}
