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

package net.algart.multimatrix;

import net.algart.arrays.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class SimpleMultiMatrix implements MultiMatrix {
    final List<Matrix<? extends PArray>> channels;
    private final PArray[] channelArrays;
    private final Class<?> elementType;

    SimpleMultiMatrix(List<? extends Matrix<? extends PArray>> channels) {
        Objects.requireNonNull(channels, "Null channels");
        this.channels = new ArrayList<>(channels);
        checkNumberOfChannels(channels, false);
        Matrices.checkDimensionEquality(this.channels, true);
        this.elementType = channels.get(0).elementType();
        this.channelArrays = new PArray[channels.size()];
        for (int k = 0; k < channelArrays.length; k++) {
            channelArrays[k] = channels.get(k).array();
        }
    }

    @Override
    public Class<?> elementType() {
        return elementType;
    }

    @Override
    public int numberOfChannels() {
        return channelArrays.length;
    }

    @Override
    public List<Matrix<? extends PArray>> allChannels() {
        return Collections.unmodifiableList(channels);
    }

    @Override
    public Matrix<? extends PArray> channel(int channelIndex) {
        return channels.get(channelIndex);
    }

    @Override
    public PArray channelArray(int channelIndex) {
        return channelArrays[channelIndex];
    }

    @Override
    public long indexInArray(long... coordinates) {
        return channels.get(0).index(coordinates);
    }

    @Override
    public MultiMatrix asPrecision(Class<?> newElementType) {
        if (newElementType == elementType()) {
            return this;
        }
        return new SimpleMultiMatrix(asPrecision(channels, newElementType));
    }

    @Override
    public MultiMatrix toPrecisionIfNot(Class<?> newElementType) {
        Objects.requireNonNull(newElementType, "Null newElementType");
        if (newElementType == elementType()) {
            return this;
        }
        return new SimpleMultiMatrix(toPrecision(channels, newElementType));
    }

    @Override
    public MultiMatrix asMono() {
        return numberOfChannels() == 1 ?
                this :
                new SimpleMultiMatrix(Collections.singletonList(intensityChannel()));
    }

    public MultiMatrix asOtherNumberOfChannels(int newNumberOfChannels, boolean fillAlphaWithMaxValue) {
        return newNumberOfChannels == numberOfChannels() ?
                this :
                new SimpleMultiMatrix(otherNumberOfChannels(newNumberOfChannels, fillAlphaWithMaxValue));
    }

    @Override
    public MultiMatrix clone() {
        return new SimpleMultiMatrix(MultiMatrix.cloneMatrices(channels));
    }

    @Override
    public MultiMatrix actualizeLazy() {
        return new SimpleMultiMatrix(MultiMatrix.actualizeLazyMatrices(channels));
    }

    @Override
    public String toString() {
        final long[] dimensions = dimensions();
        return "multi-matrix " + elementType()
                + " (" + numberOfChannels() + " channels, "
                + (dimensions.length == 1 ?
                dimensions[0] + "(x1)" :
                JArrays.toString(dimensions, "x", 1000)) + ")";
    }

    List<Matrix<? extends PArray>> otherNumberOfChannels(
            int newNumberOfChannels,
            boolean fillAlphaWithMaxValue) {
        return otherNumberOfChannels(newNumberOfChannels, fillAlphaWithMaxValue ? 3 : Integer.MAX_VALUE);
    }

    List<Matrix<? extends PArray>> otherNumberOfChannels(
            int newNumberOfChannels,
            int firstChannelIndexToFillWithMaxValue) {
        if (firstChannelIndexToFillWithMaxValue < 0) {
            throw new IllegalArgumentException("Negative firstChannelIndexToFillWithMaxValue");
        }
        if (newNumberOfChannels == 1 && isColor()) {
            return Collections.singletonList(intensityChannel());
        } else if (newNumberOfChannels < numberOfChannels()) {
            // - source is RGBA, but result is RGB only; 4th channels will be just ignored
            return allChannels().subList(0, newNumberOfChannels);
        } else {
            assert newNumberOfChannels > numberOfChannels();
            // in the current implementation (possible 1, 3, 4 channels) it means
            // that the result is RGBA, but the source is grayscale or RGB
            final List<Matrix<? extends PArray>> newChannels = new ArrayList<>(this.channels);
            final int n = Math.min(firstChannelIndexToFillWithMaxValue, newNumberOfChannels);
            for (int i = newChannels.size(); i < n; i++) {
                // suppose that 1st firstChannelIndexToFillWithMaxValue channels are identical (probably intensity)
                newChannels.add(this.channels.getFirst());
            }
            // if newNumberOfChannels > firstChannelIndexToFillWithMaxValue, let's append it by alpha=1.0 (opacity)
            for (int i = newChannels.size(); i < newNumberOfChannels; i++) {
                newChannels.add(constantMatrix(maxPossibleValue()));
            }
            return newChannels;
        }
    }

    static void checkNumberOfChannels(List<? extends Matrix<? extends PArray>> channels, boolean illegalState) {
        Objects.requireNonNull(channels, "Null channels");
        final int n = channels.size();
        checkNumberOfChannels(n, illegalState);
    }

    static void checkNumberOfChannels(long n, boolean illegalState) {
        if (n <= 0 || n > MAX_NUMBER_OF_CHANNELS) {
            final String message = "Number of channels must be in range 1.." + MAX_NUMBER_OF_CHANNELS
                    + " (for example, 1 for monochrome, 3 for RGB, 4 for RGB+alpha), "
                    + "but " + n + " channels specified";
            throw illegalState ? new IllegalStateException(message) : new IllegalArgumentException(message);
        }
    }

    static List<Matrix<? extends PArray>> flipRB(List<? extends Matrix<? extends PArray>> channels) {
        final List<Matrix<? extends PArray>> newChannels = new ArrayList<>(channels);
        int n = newChannels.size();
        if (n == 3 || n == 4) {
            Matrix<? extends PArray> temp = newChannels.get(0);
            newChannels.set(0, newChannels.get(2));
            newChannels.set(2, temp);
        }
        return newChannels;
    }

    static List<Matrix<? extends PArray>> asPrecision(
            List<Matrix<? extends PArray>> channels,
            Class<?> newElementType) {
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        for (Matrix<? extends PArray> c : channels) {
            result.add(Matrices.asPrecision(c, newElementType));
        }
        return result;
    }

    static List<Matrix<? extends PArray>> toPrecision(
            List<Matrix<? extends PArray>> channels,
            Class<?> newElementType) {
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        for (Matrix<? extends PArray> c : channels) {
            final Matrix<? extends UpdatablePArray> m = Arrays.SMM.newMatrix(
                    UpdatablePArray.class, newElementType, c.dimensions());
            Matrices.applyPrecision(null, m, c);
            result.add(m);
        }
        return result;
    }

}
