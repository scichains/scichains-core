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

package net.algart.executors.modules.core.common.matrices;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;
import java.util.List;

// Override this class if you need information about source color matrices.
public abstract class BitMultiMatrixOperationWithOptionalResult extends BitMultiMatrixProcessing {
    protected BitMultiMatrixOperationWithOptionalResult(String... inputPortNames) {
        super(inputPortNames);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public final void analyse(MultiMatrix2D... sources) {
        process(Arrays.asList(sources), false);
    }

    public final MultiMatrix2D process(MultiMatrix2D... sources) {
        return process(Arrays.asList(sources), true);
    }

    @Override
    public MultiMatrix2D process(List<MultiMatrix> sources, boolean resultRequired) {
        return (MultiMatrix2D) super.process(sources, resultRequired);
    }

    // This method should not be called directly, so it is protected
    @Override
    protected final MultiMatrix2D process(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources,
            boolean resultRequired) {
        final Matrix<? extends PArray> result = processMatrix(bitMatrices, sources, resultRequired);
        return result == null ? null : MultiMatrix.of2DMono(reduce(result));
    }

    protected abstract Matrix<? extends PArray> processMatrix(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources,
            boolean resultRequired);

    @Override
    protected boolean resultRequired() {
        return isOutputNecessary(defaultOutputPortName());
    }

    @Override
    void setNonNullResult(Object result) {
        assert result instanceof MultiMatrix2D;
        getMat().setTo((MultiMatrix2D) result);
    }
}
