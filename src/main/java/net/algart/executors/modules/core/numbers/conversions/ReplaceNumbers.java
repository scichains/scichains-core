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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;

public final class ReplaceNumbers extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_REPLACE_WITH = "replace_with";

    private int blockIndex = 0;
    private int blockIndexInReplacer = 0;
    private int numberOfBlocks = 0;

    public ReplaceNumbers() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addInputNumbers(INPUT_REPLACE_WITH);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public ReplaceNumbers setBlockIndex(int blockIndex) {
        this.blockIndex = nonNegative(blockIndex);
        return this;
    }

    public int getBlockIndexInReplacer() {
        return blockIndexInReplacer;
    }

    public ReplaceNumbers setBlockIndexInReplacer(int blockIndexInReplacer) {
        this.blockIndexInReplacer = nonNegative(blockIndexInReplacer);
        return this;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public ReplaceNumbers setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = nonNegative(numberOfBlocks);
        return this;
    }

    @Override
    public void process() {
        final SNumbers inputPortNumbers = getInputNumbers(defaultInputPortName(), true);
        final SNumbers inputPortNumbersReplacer = getInputNumbers(INPUT_REPLACE_WITH);
        final int numberOfBlocks = this.numberOfBlocks == 0 ?
                inputPortNumbersReplacer.n() :
                this.numberOfBlocks;
        logDebug(() -> "Replacing blocks #" + blockIndex
                + (numberOfBlocks == 1 ? "" : ".." + (blockIndex + numberOfBlocks - 1))
                + " of input number array " + inputPortNumbers
                + " with blocks #" + blockIndexInReplacer
                + (numberOfBlocks == 1 ? "" : ".." + (blockIndexInReplacer + numberOfBlocks - 1))
                + " of replacer array: " + inputPortNumbersReplacer);
        getNumbers()
                .setTo(inputPortNumbers)
                .replaceBlockRange(blockIndex, inputPortNumbersReplacer, blockIndexInReplacer, numberOfBlocks);
    }
}
