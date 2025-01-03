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
import net.algart.executors.modules.core.common.ChannelOperation;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiMatrixChannelFilter extends MultiMatrixFilter implements ChannelOperation {
    private int currentChannel = 0;
    private int numberOfChannels = 0;

    protected MultiMatrixChannelFilter() {
    }

    public final int currentChannel() {
        return currentChannel;
    }

    public final int numberOfChannels() {
        return numberOfChannels;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        this.numberOfChannels = sourceChannels.size();
        for (this.currentChannel = 0; this.currentChannel < numberOfChannels; this.currentChannel++) {
            Matrix<? extends PArray> m = sourceChannels.get(currentChannel);
            result.add(processChannel(m));
        }
        return MultiMatrix.valueOf(result);
    }

    // This method is not public: usually it's direct call, not from process(), can lead to
    // incorrect results, because currentChannel and numberOfChannels are not set properly
    protected abstract Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m);
}
