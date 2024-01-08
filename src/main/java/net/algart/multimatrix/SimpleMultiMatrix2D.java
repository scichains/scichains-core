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

package net.algart.multimatrix;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;

import java.util.Collections;
import java.util.List;

class SimpleMultiMatrix2D extends SimpleMultiMatrix implements MultiMatrix2D {
    private final long dimX;
    private final long dimY;

    SimpleMultiMatrix2D(List<? extends Matrix<? extends PArray>> channels) {
        super(channels);
        final Matrix<? extends PArray> ch = this.channels.get(0);
        if (ch.dimCount() != 2) {
            throw new IllegalArgumentException("2-dimensional matrices allowed only");
        }
        this.dimX = ch.dimX();
        this.dimY = ch.dimY();
    }

    @Override
    public long dimX() {
        return dimX;
    }

    @Override
    public long dimY() {
        return dimY;
    }

    @Override
    public MultiMatrix2D asPrecision(Class<?> newElementType) {
        if (newElementType == elementType()) {
            return this;
        }
        return new SimpleMultiMatrix2D(asPrecision(channels, newElementType));
    }

    @Override
    public MultiMatrix2D toPrecisionIfNot(Class<?> newElementType) {
        if (newElementType == elementType()) {
            return this;
        }
        return new SimpleMultiMatrix2D(toPrecision(channels, newElementType));
    }

    @Override
    public MultiMatrix2D asMono() {
        return numberOfChannels() == 1 ?
                this :
                new SimpleMultiMatrix2D(Collections.singletonList(intensityChannel()));
    }

    public MultiMatrix2D asOtherNumberOfChannels(int newNumberOfChannels) {
        return newNumberOfChannels == numberOfChannels() ?
                this :
                new SimpleMultiMatrix2D(otherNumberOfChannels(newNumberOfChannels));
    }

    @Override
    public MultiMatrix2D clone() {
        return new SimpleMultiMatrix2D(MultiMatrix.cloneMatrices(channels));
    }

    @Override
    public MultiMatrix2D actualizeLazy() {
        return new SimpleMultiMatrix2D(MultiMatrix.actualizeLazyMatrices(channels));
    }

    @Override
    public String toString() {
        return "multi-matrix " + elementType() + "[" + numberOfChannels() + "x" + dimX + "x" + dimY + "]";
    }

}
