
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

import net.algart.arrays.Arrays;
import net.algart.arrays.*;
import net.algart.math.Range;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// All channels has the same dimensions and element type.
public interface MultiMatrix extends Cloneable {
    int MAX_NUMBER_OF_CHANNELS = 512; // - the same limit as in OpenCV

    double INTENSITY_R_WEIGHT = 0.299;
    double INTENSITY_B_WEIGHT = 0.114;
    double INTENSITY_G_WEIGHT = 1.0 - (INTENSITY_R_WEIGHT + INTENSITY_B_WEIGHT); // ~0.587

    int DEFAULT_R_CHANNEL = 0;
    int DEFAULT_G_CHANNEL = 1;
    int DEFAULT_B_CHANNEL = 2;
    int DEFAULT_ALPHA_CHANNEL = 3;

    List<Class<?>> SUPPORTED_ELEMENT_TYPES = List.of(
            byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class, char.class);

    List<Matrix<? extends PArray>> allChannels();

    Class<?> elementType();

    int numberOfChannels();

    Matrix<? extends PArray> channel(int channelIndex);

    default PArray channelArray(int channelIndex) {
        return channel(channelIndex).array();
    }

    default Matrix<PArray> mergeChannels() {
        return Matrices.mergeLayers(Arrays.SMM, allChannels());
    }

    default long[] dimensions() {
        return channel(0).dimensions();
    }

    default int dimCount() {
        return channel(0).dimCount();
    }

    default long dim(int n) {
        return channel(0).dim(n);
    }

    default Class<? extends PArray> arrayType() {
        return channel(0).type(PArray.class);
    }

    default double maxPossibleValue() {
        return Arrays.maxPossibleValue(arrayType(), 1.0);
    }

    default long size() {
        return channel(0).size();
    }

    default boolean isUnsigned() {
        return Arrays.isUnsignedElementType(elementType());
    }

    default boolean isFloatingPoint() {
        return Arrays.isFloatingPointElementType(elementType());
    }

    default int bitsPerElement() {
        return (int) channel(0).array().bitsPerElement();
    }

    long indexInArray(long... coordinates);

    /**
     * Returns equivalent {@link MultiMatrix2D} if this matrix is actually 2-dimensional
     * or throws an exception in another case.
     *
     * @return equivalent {@link MultiMatrix2D}.
     * @throws IllegalStateException if <code>{@link #dimCount()}!=2</code>.
     */
    default MultiMatrix2D asMultiMatrix2D() {
        if (this instanceof MultiMatrix2D) {
            return (MultiMatrix2D) this;
        }
        final List<Matrix<? extends PArray>> allChannels = allChannels();
        if (allChannels.get(0).dimCount() == 2) {
            return valueOf2D(allChannels);
        } else {
            throw new IllegalStateException("This matrix is not actually 2-dimensional: " + this);
        }
    }

    default PixelValue getPixel(long indexInArray) {
        return getPixel(indexInArray, null);
    }

    default PixelValue getPixel(long indexInArray, PixelValue result) {
        if (result == null) {
            result = PixelValue.newZeroPixelValue(elementType(), numberOfChannels());
        }
        result.read(this, indexInArray);
        return result;
    }

    default void setPixel(long indexInArray, PixelValue pixelValue) {
        pixelValue.write(this, indexInArray);
    }

    default double getPixelChannel(int channelIndex, long indexInArray) {
        return channelArray(channelIndex).getDouble(indexInArray);
    }

    default void setPixelChannel(int channelIndex, long indexInArray, double value) {
        ((UpdatablePArray) channelArray(channelIndex)).setDouble(indexInArray, value);
    }

    MultiMatrix asPrecision(Class<?> newElementType);

    MultiMatrix toPrecisionIfNot(Class<?> newElementType);

    default MultiMatrix asFloatingPoint() {
        return isFloatingPoint() ? this : asPrecision(float.class);
    }

    default MultiMatrix asFloat() {
        return elementType() == float.class ? this : asPrecision(float.class);
    }

    default MultiMatrix toFloatIfNot() {
        return elementType() == float.class ? this : toPrecisionIfNot(float.class);
    }

    default boolean isMono() {
        return numberOfChannels() == 1;
    }

    default boolean isColor() {
        final int n = numberOfChannels();
        return n == 3 || n == 4;
    }

    default boolean isSimpleMemoryModel() {
        return allChannels().stream().allMatch(channel -> SimpleMemoryModel.isSimpleArray(channel.array()));
    }

    MultiMatrix asMono();

    default MultiMatrix toMonoIfNot() {
        return isMono() ? this : asMono().clone();
    }

    MultiMatrix asOtherNumberOfChannels(int numberOfChannels);

    /**
     * Returns an exact updatable clone of this multi-matrix.
     *
     * @return exact updatable clone of this multi-matrix.
     */
    MultiMatrix clone();

    /**
     * Returns an exact clone of this multi-matrix for every channel, which is not created by
     * {@link SimpleMemoryModel}.
     *
     * <p>Note: this operation can optimize access to this matrix in many times, if it consists of lazy-calculated
     * channels! It performs cloning with maximal speed via multithreading optimization. We recommend to call
     * it after lazy calculations.</p>
     *
     * @return exact clone of this multi-matrix.
     */
    MultiMatrix actualizeLazy();

    default MultiMatrix nonZeroPixels(boolean checkOnlyRGBChannels) {
        return valueOfMono(nonZeroPixelsMatrix(checkOnlyRGBChannels));
    }

    default MultiMatrix zeroPixels(boolean checkOnlyRGBChannels) {
        return valueOfMono(zeroPixelsMatrix(checkOnlyRGBChannels));
    }

    default MultiMatrix nonZeroAnyChannel() {
        return valueOfMono(nonZeroAnyChannelMatrix());
    }

    default MultiMatrix zeroAllChannels() {
        return valueOfMono(zeroAllChannelsMatrix());
    }

    default MultiMatrix nonZeroRGB() {
        return MultiMatrix.valueOfMono(nonZeroRGBMatrix());
    }

    default MultiMatrix zeroRGB() {
        return MultiMatrix.valueOfMono(zeroRGBMatrix());
    }

    default Matrix<BitArray> nonZeroAnyChannelMatrix() {
        return nonZeroPixelsMatrix(false);
    }

    default Matrix<BitArray> nonZeroRGBMatrix() {
        return nonZeroPixelsMatrix(true);
    }

    default Matrix<BitArray> nonZeroPixelsMatrix(boolean checkOnlyRGBChannels) {
        if (numberOfChannels() == 1 && elementType() == boolean.class) {
            // - avoid extra cloning
            return channel(0).cast(BitArray.class);
        }
        final List<Matrix<BitArray>> nonZeroPixelsInChannels = new ArrayList<>();
        for (Matrix<? extends PArray> m : allChannels()) {
            if (checkOnlyRGBChannels && nonZeroPixelsInChannels.size() >= 3) {
                break;
            }
            nonZeroPixelsInChannels.add(MultiMatrix.nonZeroPixels(m));
        }
        return nonZeroPixelsInChannels.size() == 1 ?
                nonZeroPixelsInChannels.get(0) :
                Matrices.asFuncMatrix(Func.MAX, BitArray.class, nonZeroPixelsInChannels)
                        .cast(BitArray.class).clone();
    }

    default Matrix<BitArray> zeroAllChannelsMatrix() {
        return zeroPixelsMatrix(false);
    }

    default Matrix<BitArray> zeroRGBMatrix() {
        return zeroPixelsMatrix(true);
    }

    default Matrix<BitArray> zeroPixelsMatrix(boolean checkOnlyRGBChannels) {
        return Matrices.asFuncMatrix(Func.REVERSE, BitArray.class, nonZeroPixelsMatrix(checkOnlyRGBChannels))
                .cast(BitArray.class).clone();
    }

    default MultiMatrix min(MultiMatrix other) {
        return asFunc(Func.MIN, other);
    }

    default MultiMatrix max(MultiMatrix other) {
        return asFunc(Func.MAX, other);
    }

    default MultiMatrix asFunc(Func funcOfOneArgument) {
        return asFunc(funcOfOneArgument, this.arrayType());
    }

    default MultiMatrix asFunc(Func funcOfOneArgument, Class<? extends PArray> requiredType) {
        final int n = this.numberOfChannels();
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            channels.add(Matrices.asFuncMatrix(funcOfOneArgument, requiredType, channel(k)));
        }
        return MultiMatrix.valueOf(channels);
    }

    default MultiMatrix asFunc(Func funcOfTwoArguments, MultiMatrix other) {
        return asFunc(funcOfTwoArguments, other, this.arrayType());
    }

    default MultiMatrix asFunc(Func funcOfTwoArguments, MultiMatrix other, Class<? extends PArray> requiredType) {
        Objects.requireNonNull(other, "Null other multi-matrix");
        final int n = this.numberOfChannels();
        other = other.asOtherNumberOfChannels(n).asPrecision(this.elementType());
        assert other.numberOfChannels() == n : "Invalid asOtherNumberOfChannels implementation";
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            channels.add(Matrices.asFuncMatrix(funcOfTwoArguments, requiredType, channel(k), other.channel(k)));
        }
        return MultiMatrix.valueOf(channels);
    }

    default MultiMatrix mapChannels(Function<Matrix<? extends PArray>, Matrix<? extends PArray>> function) {
        return MultiMatrix.valueOf(allChannels().stream().map(function::apply).collect(Collectors.toList()));
    }

    default List<Matrix<? extends PArray>> allChannelsInRGBAOrder() {
        List<Matrix<? extends PArray>> channels = allChannels();
        SimpleMultiMatrix.checkNumberOfChannels(channels, true);
        return channels;
    }

    default List<Matrix<? extends PArray>> allChannelsInBGRAOrder() {
        List<Matrix<? extends PArray>> channels = allChannels();
        SimpleMultiMatrix.checkNumberOfChannels(channels, true);
        return SimpleMultiMatrix.flipRB(channels);
    }

    /**
     * Returns the only channel if {@link #numberOfChannels()}==1, in another case returns
     * lazy intensity matrix created from R, G, B channels.
     *
     * @return intensity grays-scale matrix equivalent to this multi-matrix.
     */
    default Matrix<? extends PArray> intensityChannel() {
        final Matrix<? extends PArray> result = intensityChannelOrNull();
        if (result == null) {
            throw new IllegalStateException("Cannot convert " + numberOfChannels()
                    + "-channel multichannel matrix into monochrome (intensity) one: "
                    + "automatic conversion to monochrome "
                    + "is possible only for 3- or 4-channel matrix (RGB or RGBA)");
        }
        return result;
    }

    default Matrix<? extends PArray> intensityChannelOrNull() {
        final int n = numberOfChannels();
        if (n == 1) {
            return channel(0);
        }
        if (n != 3 && n != 4) {
            return null;
        }
        final Matrix<? extends PArray> g = channel(DEFAULT_G_CHANNEL);
        final Matrix<? extends PArray> r = channel(DEFAULT_R_CHANNEL);
        final Matrix<? extends PArray> b = channel(DEFAULT_B_CHANNEL);
        final Class<? extends PArray> resultType = g.type(PArray.class);
        return Matrices.asFuncMatrix(
                LinearFunc.getInstance(
                        isFloatingPoint() ? 0.0 : 0.5,
                        INTENSITY_R_WEIGHT, INTENSITY_G_WEIGHT, INTENSITY_B_WEIGHT),
                resultType, r, g, b);
        // - for integer matrices, we prefer rounding (by adding 0.5)
    }

    default Range rangeOfIntensityOrNull() {
        final Matrix<? extends PArray> intensityChannel = intensityChannelOrNull();
        return intensityChannel == null ? null : Arrays.rangeOf(intensityChannel.array());
    }

    default Range nonZeroRangeOf(int channelIndex) {
        return nonZeroRangeOf(null, channel(channelIndex).array());
    }

    default Matrix<? extends PArray> constantMatrix(double value) {
        return Matrices.constantMatrix(value, arrayType(), dimensions()).clone();
    }

    default MultiMatrix contrast() {
        final Range range = rangeOfIntensityOrNull();
        if (range == null) {
            return this;
        }
        return contrast(range, false);
    }

    default MultiMatrix contrast(Range sourceRangeToContrast, boolean requireMonochromeOrColor) {
        final Range destRange = Range.valueOf(0.0, maxPossibleValue());
        final LinearFunc function = sourceRangeToContrast == null || sourceRangeToContrast.size() == 0 ?
                null :
                LinearFunc.getInstance(destRange, sourceRangeToContrast);
        return correctIntensity(function, requireMonochromeOrColor);
    }

    default MultiMatrix correctIntensity(
            LinearFunc intensityCorrectingFunctionOfOneArgument,
            boolean requireMonochromeOrColor) {
        final int n = numberOfChannels();
        if (n != 1 && n != 3 && n != 4) {
            if (requireMonochromeOrColor) {
                throw new IllegalStateException("Cannot correct intensity of " + n + "-channel multichannel matrix: "
                        + "it is possible only for 1-, 3- or 4-channel matrix (monochrome, RGB or RGBA)");
            } else {
                return this;
            }
        }
        if (intensityCorrectingFunctionOfOneArgument == null) {
            return this;
        }
        if (n == 1) {
            return MultiMatrix.valueOfMono(Matrices.asFuncMatrix(
                    intensityCorrectingFunctionOfOneArgument,
                    channel(0).type(PArray.class),
                    channel(0)));
        }
        final Matrix<? extends PArray> i = intensityChannel();
        final List<Matrix<? extends PArray>> channels = new ArrayList<>(allChannels());
        final double a = intensityCorrectingFunctionOfOneArgument.a(0);
        final double b = intensityCorrectingFunctionOfOneArgument.b();
        for (int k = 0; k < 3; k++) {
            // - Important: not "k < n"! We MUST NOT attempt to "contrast" alpha-channel,
            // it is senseless and can lead to invisible results
            final Matrix<? extends PArray> m = channels.get(k);
            final Range destRange = Range.valueOf(0.0, m.array().maxPossibleValue(1.0));
            Func f = new AbstractFunc() {
                public double get(double... x) {
                    // result = x * ic / i, where ic = ai+b; so result = x * (a+b/i)
                    return x[1] == 0.0 ? 0.0 : destRange.cut(x[0] * (a + b / x[1]));
                }

                @Override
                public double get(double x0, double x1) {
                    return x1 == 0.0 ? 0.0 : destRange.cut(x0 * (a + b / x1));
                }
            };
            Matrix<? extends PArray> result = Matrices.asFuncMatrix(f, m.type(PArray.class), m, i);
            channels.set(k, result);
        }
        return MultiMatrix.valueOf(channels);

    }

    default boolean dimEquals(MultiMatrix other) {
        Objects.requireNonNull(other, "Null multi-matrix");
        int dimCount = dimCount();
        if (other.dimCount() != dimCount) {
            return false;
        }
        for (int k = 0; k < dimCount; k++) {
            if (other.dim(k) != dim(k)) {
                return false;
            }
        }
        return true;
    }

    default void checkDimensionEquality(MultiMatrix other, String thisMatrixName, String otherMatrixName)
            throws SizeMismatchException {
        if (other == null) {
            return;
        }
        if (!dimEquals(other)) {
            throw new SizeMismatchException("The " + thisMatrixName + " and " + otherMatrixName
                    + " multi-matrix dimensions mismatch: " + this + " and " + other);
        }
    }

    default void freeResources() {
        for (Matrix<? extends PArray> m : allChannels()) {
            m.freeResources();
        }
    }

    static MultiMatrix valueOf(List<? extends Matrix<? extends PArray>> channels) {
        return new SimpleMultiMatrix(channels);
    }

    static MultiMatrix valueOfRGBA(List<? extends Matrix<? extends PArray>> channels) {
        SimpleMultiMatrix.checkNumberOfChannels(channels, false);
        return new SimpleMultiMatrix(channels);
    }

    static MultiMatrix valueOfBGRA(List<? extends Matrix<? extends PArray>> channels) {
        SimpleMultiMatrix.checkNumberOfChannels(channels, false);
        return new SimpleMultiMatrix(SimpleMultiMatrix.flipRB(channels));
    }

    static MultiMatrix valueOfMono(Matrix<? extends PArray> singleChannel) {
        Objects.requireNonNull(singleChannel, "Null single-channel matrix");
        return new SimpleMultiMatrix(Collections.singletonList(singleChannel));
    }

    static MultiMatrix valueOfMerged(Matrix<? extends PArray> mergedChannels) {
        Objects.requireNonNull(mergedChannels, "Null mergedChannels");
        return new SimpleMultiMatrix(Matrices.asLayers(mergedChannels, MAX_NUMBER_OF_CHANNELS));
    }

    static MultiMatrix2D newMultiMatrix2D(Class<?> elementType, int numberOfChannels, long dimX, long dimY) {
        return newMultiMatrix2D(elementType, numberOfChannels, dimX, dimY, true);
    }

    static MultiMatrix2D zeroConstant2D(Class<?> elementType, int numberOfChannels, long dimX, long dimY) {
        return newMultiMatrix2D(elementType, numberOfChannels, dimX, dimY, false);
    }

    static MultiMatrix2D newMultiMatrix2D(
            Class<?> elementType,
            int numberOfChannels,
            long dimX,
            long dimY,
            boolean updatable) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (numberOfChannels <= 0) {
            throw new IllegalArgumentException("Zero or negative numberOfChannels=" + numberOfChannels);
        }
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < numberOfChannels; k++) {
            channels.add(updatable ?
                    Arrays.SMM.newMatrix(UpdatablePArray.class, elementType, dimX, dimY) :
                    Matrices.constantMatrix(0.0, Arrays.type(PArray.class, elementType), dimX, dimY));
        }
        return new SimpleMultiMatrix2D(channels);
    }

    static MultiMatrix2D valueOf2D(List<? extends Matrix<? extends PArray>> channels) {
        return new SimpleMultiMatrix2D(channels);
    }

    static MultiMatrix2D valueOf2DRGBA(List<? extends Matrix<? extends PArray>> channels) {
        SimpleMultiMatrix.checkNumberOfChannels(channels, false);
        return new SimpleMultiMatrix2D(channels);
    }

    static MultiMatrix2D valueOf2DBGRA(List<? extends Matrix<? extends PArray>> channels) {
        SimpleMultiMatrix.checkNumberOfChannels(channels, false);
        return new SimpleMultiMatrix2D(SimpleMultiMatrix.flipRB(channels));
    }

    static MultiMatrix2D valueOf2DMono(Matrix<? extends PArray> singleChannel) {
        Objects.requireNonNull(singleChannel, "Null single-channel matrix");
        return new SimpleMultiMatrix2D(Collections.singletonList(singleChannel));
    }

    static MultiMatrix2D valueOf2DMerged(Matrix<? extends PArray> packedChannels) {
        Objects.requireNonNull(packedChannels, "Null packedChannels");
        return new SimpleMultiMatrix2D(Matrices.asLayers(packedChannels, MAX_NUMBER_OF_CHANNELS));
    }

    static Matrix<BitArray> nonZeroPixels(Matrix<? extends PArray> matrix) {
        if (matrix.elementType() == boolean.class) {
            return matrix.cast(BitArray.class);
        }
        return Matrices.asFuncMatrix(Func.IDENTITY, BitArray.class, matrix);
    }

    static Matrix<BitArray> zeroPixels(Matrix<? extends PArray> matrix) {
        return Matrices.asFuncMatrix(Func.REVERSE, BitArray.class, nonZeroPixels(matrix));
    }

    static List<Matrix<? extends UpdatablePArray>> cloneMatrices(List<Matrix<? extends PArray>> channels) {
        final List<Matrix<? extends UpdatablePArray>> result = new ArrayList<>();
        for (Matrix<? extends PArray> c : channels) {
            result.add(Matrices.clone(c));
        }
        return result;
    }

    static List<Matrix<? extends PArray>> actualizeLazyMatrices(List<Matrix<? extends PArray>> channels) {
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        for (Matrix<? extends PArray> c : channels) {
            result.add(SimpleMemoryModel.isSimpleArray(c.array()) ? c : Matrices.clone(c));
        }
        return result;
    }

    static List<MultiMatrix2D> asMultiMatrices2D(Collection<MultiMatrix> matrices) {
        final List<MultiMatrix2D> result = new ArrayList<>();
        for (MultiMatrix m : matrices) {
            result.add(m == null ? null : m.asMultiMatrix2D());
        }
        return result;
    }

    // Note: returns null if there are no non-zero values
    static Range nonZeroRangeOf(PArray array) {
        return nonZeroRangeOf(null, array);
    }

    // Note: returns null if there are no non-zero values
    static Range nonZeroRangeOf(ArrayContext context, PArray array) {
        final NonZeroRangeCalculator calculator = new NonZeroRangeCalculator(context, array);
        calculator.process();
        return calculator.resultRange;
    }

    /**
     * Pixel value: primitive Java array of a channel + hashCode/equals.
     * Note that this class is partially mutable: it allows modifying elements of channels.
     * If you modify them, multithreaded usage requires external synchronization.
     */
    abstract class PixelValue {
        private int hashCode = -1;
        boolean hashCodeCalculated = false;

        private PixelValue() {
        }

        public static PixelValue valueOf(Object channelsJavaArray) {
            return valueOf(channelsJavaArray, true);
        }

        public static PixelValue newZeroPixelValue(Class<?> elementType, int numberOfChannels) {
            return valueOf(java.lang.reflect.Array.newInstance(elementType, numberOfChannels), false);
        }

        static PixelValue valueOf(Object channelsJavaArray, boolean doClone) {
            Objects.requireNonNull(channelsJavaArray, "Null channels java array");
            if (channelsJavaArray instanceof boolean[] channels) {
                return new Bit(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof char[] channels) {
                return new Char(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof byte[] channels) {
                return new Byte(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof short[] channels) {
                return new Short(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof int[] channels) {
                return new Int(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof long[] channels) {
                return new Long(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof float[] channels) {
                return new Float(doClone ? channels.clone() : channels);
            } else if (channelsJavaArray instanceof double[] channels) {
                return new Double(doClone ? channels.clone() : channels);
            } else {
                throw new IllegalArgumentException("The passed java-array argument is not boolean[], char[], "
                        + "byte[], short[], int[], long[], float[] or double[] (it is "
                        + channelsJavaArray.getClass().getSimpleName() + ")");
            }
        }

        @Override
        public final int hashCode() {
            if (!hashCodeCalculated) {
                hashCode = hashCodeImpl();
                hashCodeCalculated = true;
            }
            return hashCode;
        }

        @Override
        public abstract boolean equals(Object obj);

        public abstract int numberOfChannels();

        public abstract Class<?> elementType();

        public Object getChannels() {
            return getChannels(null);
        }

        public Object getChannels(Object result) {
            if (result == null) {
                result = java.lang.reflect.Array.newInstance(elementType(), numberOfChannels());
            }
            System.arraycopy(channelsRef(), 0, result, 0, numberOfChannels());
            return result;
        }

        public void setChannels(Object channels) {
            hashCodeCalculated = false;
            System.arraycopy(channels, 0, channelsRef(), 0, numberOfChannels());
        }

        public abstract double getChannel(int channelIndex);

        public abstract void setChannel(int channelIndex, double value);

        /**
         * Reads this pixel value from the specified multi-matrix at the given array position.
         *
         * @param multiMatrix  some multi-matrix
         * @param indexInArray in the built-in matrix array.
         * @throws IndexOutOfBoundsException if this {@link #numberOfChannels()} is less than
         *                                   number of channels in the multi-matrix.
         * @throws ClassCastException        if this {@link #elementType()} differs from
         *                                   the element type of the multi-matrix.
         */
        public abstract void read(MultiMatrix multiMatrix, long indexInArray);

        /**
         * Stores this pixel value in the specified multi-matrix at the given array position.
         * All channels in the multi-matrix must be updatable ({@code Matrix<? extends UpdatablePArray>}).
         *
         * @param multiMatrix  some multi-matrix
         * @param indexInArray in the built-in matrix array.
         * @throws IndexOutOfBoundsException if this {@link #numberOfChannels()} is less than
         *                                   number of channels in the multi-matrix.
         * @throws ClassCastException        if this {@link #elementType()} differs from
         *                                   the element type of the multi-matrix
         *                                   or if some channels of the multi-matrix are not updatable.
         */
        public abstract void write(MultiMatrix multiMatrix, long indexInArray);

        public double[] getDoubleChannels() {
            return getDoubleChannels(null);
        }

        public double[] getDoubleChannels(double[] result) {
            if (result == null) {
                result = new double[numberOfChannels()];
            }
            for (int k = 0; k < result.length; k++) {
                result[k] = getChannel(k);
            }
            return result;
        }

        public float[] getFloatChannels() {
            return getFloatChannels(null);
        }

        public float[] getFloatChannels(float[] result) {
            if (result == null) {
                result = new float[numberOfChannels()];
            }
            for (int k = 0; k < result.length; k++) {
                result[k] = (float) getChannel(k);
            }
            return result;
        }

        public int[] getIntChannels() {
            return getIntChannels(null);
        }

        public int[] getIntChannels(int[] result) {
            if (result == null) {
                result = new int[numberOfChannels()];
            }
            for (int k = 0; k < result.length; k++) {
                result[k] = (int) getChannel(k);
            }
            return result;
        }

        abstract Object channelsRef();

        abstract int hashCodeImpl();

        public static class Bit extends PixelValue {
            private final boolean[] channels;

            Bit(boolean[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return boolean.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex] ? 1.0 : 0.0;
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = value != 0.0;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((BitArray) multiMatrix.channelArray(k)).getBit(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableBitArray) multiMatrix.channelArray(k)).setBit(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("bit pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(channels[k] ? '1' : '0');
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Bit aBit = (Bit) o;
                return java.util.Arrays.equals(channels, aBit.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Char extends PixelValue {
            private final char[] channels;

            Char(char[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return char.class;
            }

            @Override
            public char[] channelsRef() {
                return channels;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex];
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (char) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((CharArray) multiMatrix.channelArray(k)).getChar(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableCharArray) multiMatrix.channelArray(k)).setChar(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("char pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format(Locale.US, "'\\u%04X'", (int) channels[k]));
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Char aChar = (Char) o;
                return java.util.Arrays.equals(channels, aChar.channels);
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Byte extends PixelValue {
            private final byte[] channels;

            Byte(byte[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return byte.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex] & 0xFF;
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (byte) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = (byte) ((ByteArray) multiMatrix.channelArray(k)).getByte(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableByteArray) multiMatrix.channelArray(k)).setByte(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("byte pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format(Locale.US, "0x%02X", channels[k] & 0xFF));
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Byte aByte = (Byte) o;
                return java.util.Arrays.equals(channels, aByte.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Short extends PixelValue {
            private final short[] channels;

            Short(short[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return short.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex] & 0xFFFF;
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (short) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = (short) ((ShortArray) multiMatrix.channelArray(k)).getShort(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableShortArray) multiMatrix.channelArray(k)).setShort(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("short pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format(Locale.US, "0x%04X", channels[k] & 0xFFFF));
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Short aShort = (Short) o;
                return java.util.Arrays.equals(channels, aShort.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Int extends PixelValue {
            private final int[] channels;

            Int(int[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return int.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex];
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (int) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((IntArray) multiMatrix.channelArray(k)).getInt(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableIntArray) multiMatrix.channelArray(k)).setInt(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("int pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    if (channels[k] == 0) {
                        sb.append('0');
                    } else {
                        sb.append("0x").append(Integer.toHexString(channels[k]).toUpperCase());
                    }
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Int aInt = (Int) o;
                return java.util.Arrays.equals(channels, aInt.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Long extends PixelValue {
            private final long[] channels;

            Long(long[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return long.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex];
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (long) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((LongArray) multiMatrix.channelArray(k)).getLong(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableLongArray) multiMatrix.channelArray(k)).setLong(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("long pixel [");
                for (int k = 0; k < channels.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    if (channels[k] == 0) {
                        sb.append('0');
                    } else {
                        sb.append("0x").append(java.lang.Long.toHexString(channels[k]).toUpperCase());
                    }
                }
                return sb.append("]").toString();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Long aLong = (Long) o;
                return java.util.Arrays.equals(channels, aLong.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Float extends PixelValue {
            private final float[] channels;

            Float(float[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return float.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex];
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = (float) value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((FloatArray) multiMatrix.channelArray(k)).getFloat(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableFloatArray) multiMatrix.channelArray(k)).setFloat(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                return "float pixel " + java.util.Arrays.toString(channels);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Float aFloat = (Float) o;
                return java.util.Arrays.equals(channels, aFloat.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }

        public static class Double extends PixelValue {
            private final double[] channels;

            Double(double[] channels) {
                this.channels = Objects.requireNonNull(channels, "Null channels");
            }

            @Override
            public int numberOfChannels() {
                return channels.length;
            }

            @Override
            public Class<?> elementType() {
                return double.class;
            }

            @Override
            public double getChannel(int channelIndex) {
                return channels[channelIndex];
            }

            @Override
            public void setChannel(int channelIndex, double value) {
                hashCodeCalculated = false;
                channels[channelIndex] = value;
            }

            @Override
            public void read(MultiMatrix multiMatrix, long indexInArray) {
                hashCodeCalculated = false;
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    channels[k] = ((DoubleArray) multiMatrix.channelArray(k)).getDouble(indexInArray);
                }
            }

            @Override
            public void write(MultiMatrix multiMatrix, long indexInArray) {
                for (int k = 0, n = multiMatrix.numberOfChannels(); k < n; k++) {
                    ((UpdatableDoubleArray) multiMatrix.channelArray(k)).setDouble(indexInArray, channels[k]);
                }
            }

            @Override
            public String toString() {
                return "double pixel " + java.util.Arrays.toString(channels);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Double aDouble = (Double) o;
                return java.util.Arrays.equals(channels, aDouble.channels);
            }

            @Override
            Object channelsRef() {
                return channels;
            }

            @Override
            int hashCodeImpl() {
                return java.util.Arrays.hashCode(channels);
            }
        }
    }
}
