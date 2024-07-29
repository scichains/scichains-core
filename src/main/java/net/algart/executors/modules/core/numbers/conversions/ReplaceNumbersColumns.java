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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;

public final class ReplaceNumbersColumns extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_REPLACE_WITH = "replace_with";

    private int indexInBlock = 0;
    private int indexInReplacer = 0;
    private int lengthInBlock = 0;

    public ReplaceNumbersColumns() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addInputNumbers(INPUT_REPLACE_WITH);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getIndexInBlock() {
        return indexInBlock;
    }

    public ReplaceNumbersColumns setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public int getIndexInReplacer() {
        return indexInReplacer;
    }

    public ReplaceNumbersColumns setIndexInReplacer(int indexInReplacer) {
        this.indexInReplacer = nonNegative(indexInReplacer);
        return this;
    }

    public int getLengthInBlock() {
        return lengthInBlock;
    }

    public ReplaceNumbersColumns setLengthInBlock(int lengthInBlock) {
        this.lengthInBlock = nonNegative(lengthInBlock);
        return this;
    }

    @Override
    public void process() {
        final SNumbers inputPortNumbers = getInputNumbers(defaultInputPortName(), true);
        final SNumbers inputPortNumbersReplacer = getInputNumbers(INPUT_REPLACE_WITH);
        final int lengthInBlock = this.lengthInBlock == 0 ?
                inputPortNumbersReplacer.getBlockLength() :
                this.lengthInBlock;
        logDebug(() -> "Replacing column" + (lengthInBlock == 1 ? "" : "s")
                + " (elements #" + indexInBlock + (lengthInBlock == 1 ? "" : ".." + (indexInBlock + lengthInBlock - 1))
                + " in each block) of input number array " + inputPortNumbers
                + " with column" + (lengthInBlock == 1 ? "" : "s")
                + " (elements #" + indexInReplacer
                + (lengthInBlock == 1 ? "" : ".." + (indexInReplacer + lengthInBlock - 1))
                + " from each block) of replacer array: " + inputPortNumbersReplacer);
        getNumbers()
                .setTo(inputPortNumbers)
                .replaceColumnRange(indexInBlock, inputPortNumbersReplacer, indexInReplacer, lengthInBlock);
    }
}
