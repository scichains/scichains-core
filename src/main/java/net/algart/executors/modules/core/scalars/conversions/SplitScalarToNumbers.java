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

package net.algart.executors.modules.core.scalars.conversions;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.Executor;

public final class SplitScalarToNumbers extends Executor {
    private int blockLength = 1;
    private boolean singleBlock = true;

    public SplitScalarToNumbers() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public SplitScalarToNumbers setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public boolean isSingleBlock() {
        return singleBlock;
    }

    public SplitScalarToNumbers setSingleBlock(boolean singleBlock) {
        this.singleBlock = singleBlock;
        return this;
    }

    @Override
    public void process() {
        final SScalar scalar = getInputScalar();
        double[] array = scalar.toDoubles(0);
        getNumbers().setTo(array, singleBlock ? array.length : blockLength);
    }
}
