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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;

import java.util.Locale;

public final class ExtractNumbers extends Executor implements ReadOnlyExecutionInput {
    private int blockIndex = 0;
    private int numberOfBlocks = 1;

    public ExtractNumbers() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public ExtractNumbers setBlockIndex(int blockIndex) {
        this.blockIndex = nonNegative(blockIndex);
        return this;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public ExtractNumbers setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = nonNegative(numberOfBlocks);
        return this;
    }

    @Override
    public void process() {
        final SNumbers source = getInputNumbers();
        int n = source.n();
        final int blockIndex = Math.min(this.blockIndex, n);
        final int numberOfBlocks;
        if (this.numberOfBlocks != 0) {
            numberOfBlocks = Math.min(this.numberOfBlocks, n - blockIndex);
        } else {
            numberOfBlocks = n - blockIndex;
        }
        long t1 = debugTime();
        getNumbers().setTo(source.blockRange(blockIndex, numberOfBlocks));
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Extracting block%s #%d%s of number array %s: "
                        + "%.3f ms",
                numberOfBlocks == 1 ? "" : "s",
                blockIndex,
                numberOfBlocks == 1 ? "" : ".." + (blockIndex + numberOfBlocks - 1),
                source,
                (t2 - t1) * 1e-6));
    }
}
