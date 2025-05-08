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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.math.Range;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

// All channels are 2-dimensional
public interface MultiMatrix2D extends MultiMatrix {
    long dimX();

    long dimY();

    default long indexInArray(long x, long y) {
        final long dimX = dimX(), dimY = dimY();
        if (x < 0 || x >= dimX)
            throw new IndexOutOfBoundsException("X-coordinate (" + x
                    + (x < 0 ? ") < 0" : ") >= dimX() (" + dimX + ")") + " in " + this);
        if (y < 0 || y >= dimY)
            throw new IndexOutOfBoundsException("Y-coordinate (" + y
                    + (y < 0 ? ") < 0" : ") >= dimY() (" + dimY + ")") + " in " + this);
        return y * dimX + x;
    }

    @Override
    default MultiMatrix2D asMultiMatrix2D() {
        return this;
    }

    default PixelValue getPixel(long x, long y) {
        return getPixel(indexInArray(x, y));
    }

    default void setPixel(long x, long y, PixelValue pixelValue) {
        setPixel(indexInArray(x, y), pixelValue);
    }

    default double getPixelChannel(int channelIndex, long x, long y) {
        return channelArray(channelIndex).getDouble(indexInArray(x, y));
    }

    default void setPixelChannel(int channelIndex, long x, long y, double value) {
        ((UpdatablePArray) channelArray(channelIndex)).setDouble(indexInArray(x, y), value);
    }

    MultiMatrix2D asPrecision(Class<?> newElementType);

    MultiMatrix2D toPrecisionIfNot(Class<?> newElementType);

    default MultiMatrix2D asFloatingPoint() {
        return isFloatingPoint() ? this : asPrecision(float.class);
    }

    default MultiMatrix2D asFloat() {
        return elementType() == float.class ? this : asPrecision(float.class);
    }

    default MultiMatrix2D toFloatIfNot() {
        return elementType() == float.class ? this : toPrecisionIfNot(float.class);
    }

    MultiMatrix2D asMono();

    default MultiMatrix2D toMonoIfNot() {
        return isMono() ? this : asMono().clone();
    }

    MultiMatrix2D asOtherNumberOfChannels(int numberOfChannels);

    MultiMatrix2D clone();

    default MultiMatrix2D nonZeroPixels(boolean checkOnlyRGBChannels) {
        return MultiMatrix.of2DMono(nonZeroPixelsMatrix(checkOnlyRGBChannels));
    }

    default MultiMatrix2D zeroPixels(boolean checkOnlyRGBChannels) {
        return MultiMatrix.of2DMono(zeroPixelsMatrix(checkOnlyRGBChannels));
    }

    default MultiMatrix2D nonZeroAnyChannel() {
        return MultiMatrix.of2DMono(nonZeroAnyChannelMatrix());
    }

    default MultiMatrix2D zeroAllChannels() {
        return MultiMatrix.of2DMono(zeroAllChannelsMatrix());
    }

    default MultiMatrix2D nonZeroRGB() {
        return MultiMatrix.of2DMono(nonZeroRGBMatrix());
    }

    default MultiMatrix2D zeroRGB() {
        return MultiMatrix.of2DMono(zeroRGBMatrix());
    }

    default MultiMatrix2D min(MultiMatrix2D other) {
        return asFunc(Func.MIN, other);
    }

    default MultiMatrix2D max(MultiMatrix2D other) {
        return asFunc(Func.MAX, other);
    }

    default MultiMatrix2D asFunc(Func funcOfOneArgument) {
        return asFunc(funcOfOneArgument, this.arrayType());
    }

    default MultiMatrix2D asFunc(Func funcOfOneArgument, Class<? extends PArray> requiredType) {
        final int n = this.numberOfChannels();
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            channels.add(Matrices.asFuncMatrix(funcOfOneArgument, requiredType, channel(k)));
        }
        return MultiMatrix.of2D(channels);
    }

    default MultiMatrix2D asFunc(Func funcOfTwoArguments, MultiMatrix2D other) {
        return asFunc(funcOfTwoArguments, other, this.arrayType());
    }

    default MultiMatrix2D asFunc(Func funcOfTwoArguments, MultiMatrix2D other, Class<? extends PArray> requiredType) {
        Objects.requireNonNull(other, "Null other multi-matrix");
        final int n = this.numberOfChannels();
        other = other.asOtherNumberOfChannels(n).asPrecision(this.elementType());
        assert other.numberOfChannels() == n : "Invalid asOtherNumberOfChannels implementation";
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            channels.add(Matrices.asFuncMatrix(funcOfTwoArguments, requiredType, channel(k), other.channel(k)));
        }
        return MultiMatrix.of2D(channels);
    }

    default MultiMatrix apply2D(Function<Matrix<? extends PArray>, Matrix<? extends PArray>> function) {
        return apply(function).asMultiMatrix2D();
    }

    default MultiMatrix2D contrast() {
        final Range range = rangeOfIntensityOrNull();
        if (range == null) {
            return this;
        }
        return contrast(range, false);
    }

    default MultiMatrix2D contrast(Range sourceRangeToContrast, boolean requireMonochromeOrColor) {
        final Range destRange = Range.valueOf(0.0, maxPossibleValue());
        final LinearFunc function = sourceRangeToContrast == null || sourceRangeToContrast.size() == 0 ?
                null :
                LinearFunc.getInstance(destRange, sourceRangeToContrast);
        return correctIntensity(function, requireMonochromeOrColor).asMultiMatrix2D();
    }
}
