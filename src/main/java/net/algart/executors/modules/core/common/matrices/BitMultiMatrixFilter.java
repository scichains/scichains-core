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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public abstract class BitMultiMatrixFilter extends BitMultiMatrixOperationWithRequiredResult {
    protected BitMultiMatrixFilter() {
        super(DEFAULT_INPUT_PORT);
        // - Providing correct requiredNumberOfInputs()=1 and creating default input port.
        // The value is not too important, because inputPortName is overridden.
        // Note that overriding allows to use defaultInputPortName() method (that also
        // can be overridden); here, in the constructor, we have no such ability.
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources) {
        return processMatrix(bitMatrices.get(0));
    }

    protected abstract Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> bitMatrix);

    @Override
    protected String inputPortName(int inputIndex) {
        assert inputIndex == 0;
        return defaultInputPortName();
    }
}
