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

class IRangeFinderWithoutOptimization extends IRangeFinder {
    @Override
    public IRangeFinderWithoutOptimization build() {
        return this;
    }

    @Override
    public IRangeFinderWithoutOptimization compact() {
        return this;
    }

    public void findContaining(int point, IRangeConsumer rangeConsumer) {
        findIntersectingWithoutOptimization(point, point, rangeConsumer);
    }

    public void findContaining(int point, IntConsumer indexConsumer) {
        findIntersectingWithoutOptimization(point, point, indexConsumer);
    }

    public void findIntersecting(int min, int max, IRangeConsumer rangeConsumer) {
        if (max < min) {
            max = min;
        }
        findIntersectingWithoutOptimization(min, max, rangeConsumer);
    }

    public void findIntersecting(int min, int max, IntConsumer indexConsumer) {
        if (max < min) {
            max = min;
        }
        findIntersectingWithoutOptimization(min, max, indexConsumer);
    }

    @Override
    public String toString() {
        return "unoptimized integer ranges finder for " + n + " ranges";
    }

    private void findIntersectingWithoutOptimization(int min, int max, IRangeConsumer rangeConsumer) {
        for (int k = 0; k < n; k++) {
            int left, right;
            if ((left = this.left(k)) <= max && (right = this.right(k)) >= min && indexActual(k)) {
                rangeConsumer.accept(k, left, right);
            }
        }
    }

    private void findIntersectingWithoutOptimization(int min, int max, IntConsumer indexConsumer) {
        for (int k = 0; k < n; k++) {
            if (this.left(k) <= max && this.right(k) >= min && indexActual(k)) {
                indexConsumer.accept(k);
            }
        }
    }
}
