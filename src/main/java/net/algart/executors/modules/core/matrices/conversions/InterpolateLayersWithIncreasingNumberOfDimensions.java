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

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.math.functions.LinearFunc;

import java.util.List;
import java.util.stream.IntStream;

public final class InterpolateLayersWithIncreasingNumberOfDimensions extends SeveralMultiMatricesChannelOperation {
    public static final String INPUT_FRONT = "front";
    public static final String INPUT_REAR = "rear";

    private long newDimension = 2;

    public InterpolateLayersWithIncreasingNumberOfDimensions() {
        super(INPUT_FRONT, INPUT_REAR);
    }

    public long getNewDimension() {
        return newDimension;
    }

    public InterpolateLayersWithIncreasingNumberOfDimensions setNewDimension(long newDimension) {
        this.newDimension = positive(newDimension);
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m) {
        final Matrix<? extends PArray> front = m.get(0);
        final Matrix<? extends PArray> rear = m.get(1);
        assert rear == null || front.elementType() == rear.elementType() : "Superclass didn't correct precision";
        final int newDimCount = front.dimCount() + 1;
        final long[] newDimensions = java.util.Arrays.copyOf(front.dimensions(), newDimCount);
        newDimensions[newDimCount - 1] = this.newDimension;
        Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(
                UpdatablePArray.class, front.elementType(), newDimensions);
        assert newDimension == (int) newDimension;
        // - in other case, SMM cannot create a matrix
        final long step = front.size();
        IntStream.range(0, (int) newDimension).parallel().forEach(coord -> {
            final long p = coord * step;
            final UpdatablePArray resultArray = result.array().subArr(p, step);
            if (coord == 0 || rear == null) {
                Arrays.copy(null, resultArray, front.array());
            } else if (coord == newDimension - 1) {
                Arrays.copy(null, resultArray, rear.array());
            } else {
                assert newDimension > 1;
                final double alpha = (double) coord / (double) (newDimension - 1);
                Arrays.applyFunc(
                        ArrayContext.DEFAULT_SINGLE_THREAD,
                        LinearFunc.getInstance(0.0, 1.0 - alpha, alpha),
                        resultArray, front.array(), rear.array());
            }
        });
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex >= 1;
    }
}
