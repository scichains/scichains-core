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

package net.algart.executors.modules.core.common.numbers;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;

import java.util.Objects;

public abstract class NumbersFilter extends Executor {
    private int indexInBlock = 0;
    private int lengthInBlock = 0;
    private boolean replaceColumnRangeInInput = false;

    protected NumbersFilter() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public final int getIndexInBlock() {
        return indexInBlock;
    }

    public final NumbersFilter setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public final int getLengthInBlock() {
        return lengthInBlock;
    }

    public final NumbersFilter setLengthInBlock(int lengthInBlock) {
        this.lengthInBlock = nonNegative(lengthInBlock);
        return this;
    }

    public final boolean isReplaceColumnRangeInInput() {
        return replaceColumnRangeInInput;
    }

    public final NumbersFilter setReplaceColumnRangeInInput(boolean replaceColumnRangeInInput) {
        this.replaceColumnRangeInInput = replaceColumnRangeInInput;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(allowUninitializedInput());
        setStartProcessingTimeStamp();
        final SNumbers result = process(input);
        setEndProcessingTimeStamp();
        if (result != null) {
            getNumbers().exchange(result);
        } else if (hasDefaultOutputPort()) {
            getNumbers().remove();
            // - important to clear result port, if it was not cleared from the previous call
        }
    }

    public SNumbers process(SNumbers input) {
        SNumbers originalInput = Objects.requireNonNull(input, "Null input");
        final IRange columnRange = selectedColumnRange();
        if (columnRange != null && input.isInitialized()) {
            input = input.columnRange((int) columnRange.min(), (int) columnRange.size());
        }
        SNumbers result = processNumbers(input);
        if (result == null) {
            if (resultRequired()) {
                throw new AssertionError("Invalid process implementation: "
                        + "it returned null, though resultRequired=true");
            }
            return null;
        }
        if (replaceColumnRangeInInput() && columnRange != null) {
            final SNumbers corrected = originalInput.toPrecision(result.elementType());
            corrected.replaceColumnRange((int) columnRange.min(), result, 0, result.getBlockLength());
            result = corrected;
        }
        return result;
    }

    protected abstract SNumbers processNumbers(SNumbers source);

    // May be overridden
    protected boolean allowUninitializedInput() {
        return false;
    }

    protected boolean resultRequired() {
        return true;
    }

    // If returns some range, the input array will be replaced by its columns from this range.
    // In this case, the corresponding blockLength in process() method will be the size of this range.
    protected IRange selectedColumnRange() {
        return lengthInBlock <= 0 ? null : IRange.of(indexInBlock, indexInBlock + lengthInBlock - 1);
    }

    // If true, the result is produced by replacing selectedColumnRange() in the input array
    protected boolean replaceColumnRangeInInput() {
        return replaceColumnRangeInInput;
    }
}
