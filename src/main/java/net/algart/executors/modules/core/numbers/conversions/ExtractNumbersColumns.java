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

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;

import java.util.Locale;

public final class ExtractNumbersColumns extends Executor implements ReadOnlyExecutionInput {
    private int indexInBlock = 0;
    private int lengthInBlock = 1;

    public ExtractNumbersColumns() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getIndexInBlock() {
        return indexInBlock;
    }

    public ExtractNumbersColumns setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public int getLengthInBlock() {
        return lengthInBlock;
    }

    public ExtractNumbersColumns setLengthInBlock(int lengthInBlock) {
        this.lengthInBlock = nonNegative(lengthInBlock);
        return this;
    }

    @Override
    public void process() {
        final SNumbers source = getInputNumbers();
        final int lengthInBlock = this.lengthInBlock != 0 ?
                this.lengthInBlock :
                Math.max(1, source.getBlockLength() - indexInBlock);
        long t1 = debugTime();
        getNumbers().setToColumnRange(source, indexInBlock, lengthInBlock);
//      // For debugging:
//        int[] columnIndexes = new int[lengthInBlock];
//        for (int j = 0; j < columnIndexes.length; j++) {
//            columnIndexes[j] = indexInBlock + j;
//        }
//        getNumbers().exchange(source.columnsByIndexes(columnIndexes));
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Extracting column%s (elements #%d%s from each block) of number array %s: "
                + "%.3f ms (%.1f ns/block)",
                lengthInBlock == 1 ? "" : "s",
                indexInBlock,
                lengthInBlock == 1 ? "" : ".." + (indexInBlock + lengthInBlock - 1),
                source,
                (t2 - t1) * 1e-6, (double) (t2 - t1) / source.n()));
    }
}
