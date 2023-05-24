/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class IRectangleFinderWithoutOptimization extends IRectangleFinder {
    @Override
    public IRectangleFinderWithoutOptimization compact() {
        return this;
    }

    @Override
    public void findContaining(int x, int y, IntConsumer indexConsumer) {
        for (int k = 0; k < n; k++) {
            if (minX(k) <= x && maxX(k) >= x && minY(k) <= y && maxY(k) >= y && indexActual(k)) {
                indexConsumer.accept(k);
            }
        }
    }

    @Override
    public void findContaining(double x, double y, IntConsumer indexConsumer) {
        for (int k = 0; k < n; k++) {
            if (minX(k) <= x && maxX(k) >= x && minY(k) <= y && maxY(k) >= y && indexActual(k)) {
                indexConsumer.accept(k);
            }
        }
    }

    @Override
    public void findIntersecting(int minX, int maxX, int minY, int maxY, IntConsumer indexConsumer) {
        if (minX > maxX || minY > maxY) {
            return;
        }
        for (int k = 0; k < n; k++) {
            if (minX(k) <= maxX && maxX(k) >= minX && minY(k) <= maxY && maxY(k) >= minY && indexActual(k)) {
                indexConsumer.accept(k);
            }
        }
    }

    @Override
    public String toString() {
        return "unoptimized integer rectangles finder for " + n + " rectangles";
    }

    @Override
    protected void setRanges() {
    }
}
