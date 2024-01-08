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

package net.algart.additions.math;

import java.util.function.IntConsumer;

public class IntArrayAppender implements IntConsumer {
    private final int[] array;
    private int offset;

    // Works maximally quickly, without any checks: can be used in internal loops
    public IntArrayAppender(int[] array) {
        this.array = array;
        this.offset = 0;
    }

    // Works maximally quickly, without any checks: can be used in internal loops
    public IntArrayAppender(int[] array, int offset) {
        this.array = array;
        this.offset = offset;
    }

    public int[] array() {
        return array;
    }

    public int offset() {
        return offset;
    }

    public IntArrayAppender reset(int offset) {
        this.offset = offset;
        return this;
    }

    public IntArrayAppender reset() {
        this.offset = 0;
        return this;
    }

    public void accept(int value) {
        array[offset++] = value;
    }
}
