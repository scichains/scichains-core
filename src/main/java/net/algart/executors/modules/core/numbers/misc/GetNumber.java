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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumbersToScalar;

public final class GetNumber extends NumbersToScalar implements ReadOnlyExecutionInput {
    private int blockIndex = 0;
    private int indexInBlock = 0;
    private Integer rawIndex = null;

    public int getBlockIndex() {
        return blockIndex;
    }

    public GetNumber setBlockIndex(int blockIndex) {
        this.blockIndex = nonNegative(blockIndex);
        return this;
    }

    public int getIndexInBlock() {
        return indexInBlock;
    }

    public GetNumber setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public Integer getRawIndex() {
        return rawIndex;
    }

    public GetNumber setRawIndex(Integer rawIndex) {
        this.rawIndex = rawIndex == null ? null : nonNegative(rawIndex);
        return this;
    }

    @Override
    public SScalar analyse(SNumbers source) {
        final int indexInArray = rawIndex != null ? rawIndex : blockIndex * source.getBlockLength() + indexInBlock;
        if (source.isLongArray()) {
            return SScalar.of(((long[]) source.arrayReference())[indexInArray]);
            // - in all other cases, double type always stores the result precisely always
        } else if (source.isFloatingPoint()) {
            return SScalar.of(source.getValue(indexInArray));
        } else {
            return SScalar.of((long) source.getValue(indexInArray));
        }
    }
}
