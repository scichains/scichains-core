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

import net.algart.arrays.Arrays;
import net.algart.arrays.BitArray;
import net.algart.arrays.PArray;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.functions.RectangularFunc;

public final class ExtractNumbersInRange extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_SELECTOR = "selector";

    private int checkedIndexInSelectorBlocks = 0;
    private double min = 0.0;
    private double max = Double.POSITIVE_INFINITY;
    private boolean invert = false;

    public ExtractNumbersInRange() {
        addInputNumbers(INPUT_SELECTOR);
    }

    public int getCheckedIndexInSelectorBlocks() {
        return checkedIndexInSelectorBlocks;
    }

    public ExtractNumbersInRange setCheckedIndexInSelectorBlocks(int checkedIndexInSelectorBlocks) {
        this.checkedIndexInSelectorBlocks = checkedIndexInSelectorBlocks;
        return this;
    }

    public double getMin() {
        return min;
    }

    public ExtractNumbersInRange setMin(double min) {
        this.min = min;
        return this;
    }

    public ExtractNumbersInRange setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public ExtractNumbersInRange setMax(double max) {
        this.max = max;
        return this;
    }

    public ExtractNumbersInRange setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public ExtractNumbersInRange setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        SNumbers selector = getInputNumbers(INPUT_SELECTOR, true);
        if (!selector.isInitialized()) {
            selector = source;
        }
        return processNumbers(source, selector);
    }

    public SNumbers processNumbers(SNumbers source, SNumbers selector) {
        final double inValue = invert ? 0 : 1;
        final double outValue = invert ? 1 : 0;
        final SNumbers column = selector.column(checkedIndexInSelectorBlocks);
        final BitArray bits = min > max ?
                Arrays.nBitCopies(column.n(), false) :
                Arrays.asFuncArray(
                        RectangularFunc.getInstance(min, max, inValue, outValue),
                        BitArray.class,
                        (PArray) SimpleMemoryModel.asUpdatableArray(column.getArray()));
        return source.selectBlockSet(bits);
    }
}
