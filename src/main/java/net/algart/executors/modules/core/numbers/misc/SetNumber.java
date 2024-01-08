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

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;

public final class SetNumber extends Executor {
    private int blockIndex = 0;
    private int indexInBlock = 0;
    private Integer rawIndex = null;
    private double value = 0.0;

    public int getBlockIndex() {
        return blockIndex;
    }

    public SetNumber setBlockIndex(int blockIndex) {
        this.blockIndex = nonNegative(blockIndex);
        return this;
    }

    public int getIndexInBlock() {
        return indexInBlock;
    }

    public SetNumber setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public Integer getRawIndex() {
        return rawIndex;
    }

    public SetNumber setRawIndex(Integer rawIndex) {
        this.rawIndex = rawIndex == null? null : nonNegative(rawIndex);
        return this;
    }

    public double getValue() {
        return value;
    }

    public SetNumber setValue(double value) {
        this.value = value;
        return this;
    }

    @Override
    public void process() {
        final SNumbers numbers = getInputNumbers();
        final int indexInArray = rawIndex != null ? rawIndex : blockIndex * numbers.getBlockLength() + indexInBlock;
        numbers.setValue(indexInArray, value);
        getNumbers().exchange(numbers);
    }
}
