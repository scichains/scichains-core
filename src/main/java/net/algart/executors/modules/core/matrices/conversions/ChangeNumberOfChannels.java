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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.multimatrix.MultiMatrix;

public final class ChangeNumberOfChannels extends MultiMatrixFilter {
    private int numberOfChannels = 0;
    private boolean requireInput = false;

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public ChangeNumberOfChannels setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = nonNegative(numberOfChannels);
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public ChangeNumberOfChannels setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (source == null) {
            return null;
        }
        if (numberOfChannels == 0) {
            // - unchanged
            return source;
        }
        logDebug(() -> "Changing number of channels " + source.numberOfChannels() + " -> "
                + numberOfChannels + " for matrix " + source);
        return source.asOtherNumberOfChannels(numberOfChannels).clone();
    }

    @Override
    protected boolean allowUninitializedInput() {
        return !requireInput;
    }

    @Override
    protected boolean resultRequired() {
        return requireInput;
    }
}
