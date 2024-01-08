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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumbersToScalar;

public final class NumbersLength extends NumbersToScalar implements ReadOnlyExecutionInput {
    public static final String OUTPUT_BLOCK_LENGTH = "block_length";

    public enum LengthType {
        NUMBER_OF_BLOCKS,
        RAW_ARRAY_LENGTH
    }

    public NumbersLength() {
        addOutputScalar(OUTPUT_BLOCK_LENGTH);
    }

    private LengthType lengthType = LengthType.NUMBER_OF_BLOCKS;
    private boolean requireInput = false;

    public boolean isRequireInput() {
        return requireInput;
    }

    public NumbersLength setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    public LengthType getLengthType() {
        return lengthType;
    }

    public NumbersLength setLengthType(LengthType lengthType) {
        this.lengthType = nonNull(lengthType);
        return this;
    }

    @Override
    public SScalar analyse(SNumbers source) {
        getScalar(OUTPUT_BLOCK_LENGTH).setTo(source.getBlockLength());
        return SScalar.valueOf(lengthType == LengthType.NUMBER_OF_BLOCKS ? source.n() : source.getArrayLength());
    }

    @Override
    protected boolean allowUninitializedInput() {
        return !requireInput;
    }
}
