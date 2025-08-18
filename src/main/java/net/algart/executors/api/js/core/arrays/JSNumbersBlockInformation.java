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

package net.algart.executors.api.js.core.arrays;

import net.algart.executors.api.data.SNumbers;

/**
 * Full information about some block of SNumbers for access from JavaScript.
 */
public final class JSNumbersBlockInformation {
    public static final int MIN_BLOCK_DATA_ARRAY_LENGTH = 32;

    /**
     * Contains <code>true</code> if this block is created with non-null initialized <code>SNumbers</code> array,
     * or <code>false</code> if the constructor argument was null or not-initialized.
     */
    public final boolean initialized;
    /**
     * All values in this block or empty array double[MIN_BLOCK_DATA_ARRAY_LENGTH] if <code>!initialized</code>.
     */
    public final double[] x;
    /**
     * First value: x[0].
     */
    public double v;
    /**
     * Index in the block.
     */
    public int i = -1;
    /**
     * Actual block length. (Length of <code>x</code> array is usually greater: it is >=MIN_BLOCK_DATA_ARRAY_LENGTH.)
     */
    public final int len;
    /**
     * All numbers in all blocks (<code>toFloatArray</code>)  or empty array float[0] if <code>!initialized</code>.
     */
    public final float[] all;

    private final SNumbers numbers;
    private double originalX0;

    public JSNumbersBlockInformation(SNumbers numbers) {
        this(numbers, numbers != null && numbers.isInitialized() ? numbers.toFloatArray() : new float[0]);
    }

    public JSNumbersBlockInformation(SNumbers numbers, float[] all) {
        this.initialized = numbers != null && numbers.isInitialized();
        if (initialized) {
            this.numbers = numbers;
            this.all = all;
            this.len = numbers.getBlockLength();
            this.x = new double[Math.max(len, MIN_BLOCK_DATA_ARRAY_LENGTH)];
        } else {
            this.numbers = null;
            this.all = all;
            this.len = 0;
            this.x = new double[MIN_BLOCK_DATA_ARRAY_LENGTH];
        }
    }

    public SNumbers getNumbers() {
        return numbers;
    }

    /**
     * Loads the block and fills {@link #x}, {@link #v} and {@link #i}.
     *
     * @param blockIndex new index of loaded block.
     */
    public void readBlock(int blockIndex) {
        i = blockIndex;
        if (initialized) {
            numbers.getBlockDoubleValues(blockIndex, x);
            // may lead to excepion: i and v will stay unchanged
            v = originalX0 = x[0];
        }
    }

    /**
     * Writes the block back to numbers array.
     */
    public void writeBlock() {
        if (initialized && i >= 0 && i < numbers.n()) {
            if (x[0] == originalX0 && v != originalX0) {
                x[0] = v;
            }
            numbers.setBlockDoubleValues(i, x);
        }
    }
}
