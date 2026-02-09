/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;

public final class ExtractChannelsGroup extends MultiMatrixFilter {
    private int indexOfFirstChannel = 0;
    private int numberOfExtractedChannels = 1;

    public int getIndexOfFirstChannel() {
        return indexOfFirstChannel;
    }

    public void setIndexOfFirstChannel(int indexOfFirstChannel) {
        this.indexOfFirstChannel = indexOfFirstChannel;
    }

    public int getNumberOfExtractedChannels() {
        return numberOfExtractedChannels;
    }

    public void setNumberOfExtractedChannels(int numberOfExtractedChannels) {
        this.numberOfExtractedChannels = nonNegative(numberOfExtractedChannels);
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (numberOfExtractedChannels == 0) {
            // unchanged
            return source;
        }
        assert numberOfExtractedChannels > 0;
        logDebug(() -> "Extracting channels " + indexOfFirstChannel + ".."
                + (indexOfFirstChannel + numberOfExtractedChannels - 1)
                + " from matrix " + source);
        final Matrix<? extends PArray> zeroConstant = source.constantMatrix(0.0);
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        final List<Matrix<? extends PArray>> channels = source.allChannels();
        for (int k = 0; k < numberOfExtractedChannels; k++) {
            final int i = k + indexOfFirstChannel;
            result.add(i >= 0 && i < channels.size() ? channels.get(i) : zeroConstant);
            // note that (impossible) overflow in k+indexOfFirstChannel is also processed correctly (i<0)
        }
        return MultiMatrix.of(result);
    }
}
