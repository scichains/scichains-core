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

package net.algart.additions.arrays;

import net.algart.arrays.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public final class UniformHistogram256Finder {
    private static final int DEFAULT_BUFFER_LENGTH = 16384;

    private final boolean multithreading;
    private final int bufferLength;
    private final long[][] threadHistograms;
    private final boolean[][] threadMaskBuffers;
    private final long[] splitters;

    private long[] resultHistogram = null;
    private long resultCardinality = -1;
    private double resultBarWidth = Double.NaN;
    private double resultSum = Double.NaN;

    private UniformHistogram256Finder(int bufferLength, boolean multithreading) {
        if (bufferLength < 256) {
            throw new IllegalArgumentException("Too little buffer length " + bufferLength
                    + " (must not be < 256)");
        }
        this.bufferLength = bufferLength;
        final int cpuCount = net.algart.arrays.Arrays.SystemSettings.cpuCount();
        final int numberOfTasks = !multithreading || cpuCount == 1 ? 1 : 4 * cpuCount;
        this.multithreading = numberOfTasks > 1;
        this.threadHistograms = new long[numberOfTasks][256];
        // - too large for bits, but this case is very unusual and not important
        this.threadMaskBuffers = new boolean[numberOfTasks][bufferLength];
        this.splitters = new long[numberOfTasks + 1];
    }

    public static UniformHistogram256Finder newInstance(boolean multithreading) {
        return newInstance(DEFAULT_BUFFER_LENGTH, multithreading);
    }

    public static UniformHistogram256Finder newInstance(int bufferLength, boolean multithreading) {
        return new UniformHistogram256Finder(bufferLength, multithreading);
    }

    public UniformHistogram256Finder find(Matrix<? extends PArray> data, Matrix<? extends BitArray> mask) {
        Objects.requireNonNull(data, "Null data matrix");
        if (mask != null && !data.dimEquals(mask)) {
            throw new IllegalArgumentException("Data and mask matrices have different dimensions: "
                    + data + " and " + mask);
        }
        return find(data.array(), mask == null ? null : mask.array());
    }

    public UniformHistogram256Finder find(PArray data, BitArray mask) {
        return clear().accumulate(data, mask);
    }

    public UniformHistogram256Finder clear() {
        for (long[] histogram : threadHistograms) {
            Arrays.fill(histogram, 0);
        }
        return this;
    }

    public UniformHistogram256Finder accumulate(PArray data, BitArray mask) {
        Objects.requireNonNull(data, "Null data");
        return accumulate(data, mask, data.length());
    }

    public UniformHistogram256Finder accumulate(PArray data, BitArray mask, long length) {
        Objects.requireNonNull(data, "Null data");
        if (length < 0) {
            throw new IllegalArgumentException("Negative length = " + length);
        }
        if (data.length() < length) {
            throw new IllegalArgumentException("Length of data " + data.length()
                    + " is less than specified length " + length);
        }
        if (mask != null && mask.length() != data.length()) {
            throw new IllegalArgumentException("Different lengths of data (" + data.length()
                    + ") and mask (" + mask.length() + ")");
        }
        net.algart.arrays.Arrays.splitToRanges(splitters, length);
        final Class<?> elementType = data.elementType();
        IntStream stream = IntStream.range(0, splitters.length - 1);
        if (multithreading) {
            stream = stream.parallel();
        }
        if (elementType == char.class) {
            final CharArray array = (CharArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfChars(array, mask, rangeIndex);
            });
        } else if (elementType == boolean.class) {
            final BitArray array = (BitArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfBits(array, mask, rangeIndex);
            });
        } else if (elementType == byte.class) {
            final ByteArray array = (ByteArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfBytes(array, mask, rangeIndex);
            });
        } else if (elementType == short.class) {
            final ShortArray array = (ShortArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfShorts(array, mask, rangeIndex);
            });
        } else if (elementType == int.class) {
            final IntArray array = (IntArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfInts(array, mask, rangeIndex);
            });
        } else if (elementType == long.class) {
            final LongArray array = (LongArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfLongs(array, mask, rangeIndex);
            });
        } else if (elementType == float.class) {
            final FloatArray array = (FloatArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfFloats(array, mask, rangeIndex);
            });
        } else if (elementType == double.class) {
            final DoubleArray array = (DoubleArray) data;
            stream.forEach(rangeIndex -> {
                histogramOfDoubles(array, mask, rangeIndex);
            });
        } else {
            throw new AssertionError("Impossible element type " + elementType);
        }
        buildHistogram(elementType);
        findCardinality();
        findSumAndBarWidth(elementType);
        return this;
    }

    public long[] histogram() {
        checkReady();
        return resultHistogram;
    }

    public double barWidth() {
        checkReady();
        return resultBarWidth;
    }

    public long cardinality() {
        checkReady();
        return resultCardinality;
    }

    public double sum() {
        checkReady();
        return resultSum;
    }

    public double mean() {
        checkReady();
        return resultSum / resultCardinality;
    }

    public void checkReady() {
        if (resultHistogram == null) {
            throw new IllegalStateException("Histogram is not calculated yet");
        }
    }

    public static double barWidth(final Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (elementType == char.class) {
            return 256.0;
        } else if (elementType == boolean.class) {
            return 1.0;
        } else if (elementType == byte.class) {
            return 1.0;
        } else if (elementType == short.class) {
            return 256.0;
        } else if (elementType == int.class) {
            return 1 << 23;
        } else if (elementType == long.class) {
            return (double) (1L << 47);
        } else if (elementType == float.class) {
            return 1.0 / 256.0;
        } else if (elementType == double.class) {
            return 1.0 / 256.0;
        } else {
            throw new UnsupportedOperationException("Non-primitive element type " + elementType + " is not supported");
        }
    }

    private void buildHistogram(final Class<?> elementType) {
        final int numberOfBars = elementType == boolean.class ? 2 : 256;
        if (resultHistogram == null || resultHistogram.length != numberOfBars) {
            resultHistogram = new long[numberOfBars];
        } else {
            Arrays.fill(resultHistogram, 0);
        }
        for (long[] histogram : threadHistograms) {
            for (int k = 0; k < resultHistogram.length; k++) {
                resultHistogram[k] += histogram[k];
            }
        }
    }

    private void findCardinality() {
        resultCardinality = Arrays.stream(resultHistogram).sum();
    }

    private void findSumAndBarWidth(final Class<?> elementType) {
        double sum = 0.0;
        for (int i = 1; i < resultHistogram.length; i++) {
            // - note: there is no sense to start from index 0
            sum += (double) i * (double) resultHistogram[i];
        }
        resultBarWidth = barWidth(elementType);
        if (elementType == char.class) {
            resultSum = sum * resultBarWidth;
        } else if (elementType == boolean.class) {
            resultSum = sum;
        } else if (elementType == byte.class) {
            resultSum = sum;
        } else if (elementType == short.class) {
            resultSum = sum * resultBarWidth;
        } else if (elementType == int.class) {
            resultSum = sum * resultBarWidth;
        } else if (elementType == long.class) {
            resultSum = sum * resultBarWidth;
        } else if (elementType == float.class) {
            resultSum = sum / 256.0;
        } else if (elementType == double.class) {
            resultSum = sum / 256.0;
        } else {
            throw new UnsupportedOperationException("Non-primitive element type " + elementType + " is not supported");
        }
    }

    /*Repeat() Char   ==> Bit,,Byte,,Short,,Int,,Long,,Float,,Double;;
               char   ==> boolean,,byte,,short,,int,,long,,float,,double;;
               v >> 8 ==> v ? 1 : 0,,v & 0xFF,,(v & 0xFFFF) >> 8,,
                          v < 0 ? 0 : v >>> 23,,
                          v < 0 ? 0 : (int) (v >>> 47),,
                          v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0),,
                          v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);;
                byte(\s+v = data\.getByte) ==> int$1,,...;;
                short(\s+v = data\.getShort) ==> int$1,,... */

    private void histogramOfChars(CharArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final char[] array = (char[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    charRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    charRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final char[] array = (char[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                charRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                charRange(data, histogram, from, to - from);
            }
        }
    }

    private static void charRange(CharArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final char v = data.getChar(p + k);
                final int index = v >> 8;
                histogram[index]++;
            }
        }
    }

    private static void charRange(char[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final char v = data[p + k];
                final int index = v >> 8;
                histogram[index]++;
            }
        }
    }

    private static void charRange(CharArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final char v = data.getChar(i);
            final int index = v >> 8;
            histogram[index]++;
        }
    }

    private static void charRange(char[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final char v = data[i];
            final int index = v >> 8;
            histogram[index]++;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private void histogramOfBits(BitArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final boolean[] array = (boolean[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    booleanRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    booleanRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final boolean[] array = (boolean[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                booleanRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                booleanRange(data, histogram, from, to - from);
            }
        }
    }

    private static void booleanRange(BitArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final boolean v = data.getBit(p + k);
                final int index = v ? 1 : 0;
                histogram[index]++;
            }
        }
    }

    private static void booleanRange(boolean[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final boolean v = data[p + k];
                final int index = v ? 1 : 0;
                histogram[index]++;
            }
        }
    }

    private static void booleanRange(BitArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final boolean v = data.getBit(i);
            final int index = v ? 1 : 0;
            histogram[index]++;
        }
    }

    private static void booleanRange(boolean[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final boolean v = data[i];
            final int index = v ? 1 : 0;
            histogram[index]++;
        }
    }

    private void histogramOfBytes(ByteArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final byte[] array = (byte[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    byteRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    byteRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final byte[] array = (byte[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                byteRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                byteRange(data, histogram, from, to - from);
            }
        }
    }

    private static void byteRange(ByteArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final int v = data.getByte(p + k);
                final int index = v & 0xFF;
                histogram[index]++;
            }
        }
    }

    private static void byteRange(byte[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final byte v = data[p + k];
                final int index = v & 0xFF;
                histogram[index]++;
            }
        }
    }

    private static void byteRange(ByteArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getByte(i);
            final int index = v & 0xFF;
            histogram[index]++;
        }
    }

    private static void byteRange(byte[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final byte v = data[i];
            final int index = v & 0xFF;
            histogram[index]++;
        }
    }

    private void histogramOfShorts(ShortArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final short[] array = (short[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    shortRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    shortRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final short[] array = (short[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                shortRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                shortRange(data, histogram, from, to - from);
            }
        }
    }

    private static void shortRange(ShortArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final int v = data.getShort(p + k);
                final int index = (v & 0xFFFF) >> 8;
                histogram[index]++;
            }
        }
    }

    private static void shortRange(short[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final short v = data[p + k];
                final int index = (v & 0xFFFF) >> 8;
                histogram[index]++;
            }
        }
    }

    private static void shortRange(ShortArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getShort(i);
            final int index = (v & 0xFFFF) >> 8;
            histogram[index]++;
        }
    }

    private static void shortRange(short[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final short v = data[i];
            final int index = (v & 0xFFFF) >> 8;
            histogram[index]++;
        }
    }

    private void histogramOfInts(IntArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final int[] array = (int[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    intRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    intRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final int[] array = (int[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                intRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                intRange(data, histogram, from, to - from);
            }
        }
    }

    private static void intRange(IntArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final int v = data.getInt(p + k);
                final int index = v < 0 ? 0 : v >>> 23;
                histogram[index]++;
            }
        }
    }

    private static void intRange(int[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final int v = data[p + k];
                final int index = v < 0 ? 0 : v >>> 23;
                histogram[index]++;
            }
        }
    }

    private static void intRange(IntArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getInt(i);
            final int index = v < 0 ? 0 : v >>> 23;
            histogram[index]++;
        }
    }

    private static void intRange(int[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            final int index = v < 0 ? 0 : v >>> 23;
            histogram[index]++;
        }
    }

    private void histogramOfLongs(LongArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final long[] array = (long[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    longRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    longRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final long[] array = (long[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                longRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                longRange(data, histogram, from, to - from);
            }
        }
    }

    private static void longRange(LongArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final long v = data.getLong(p + k);
                final int index = v < 0 ? 0 : (int) (v >>> 47);
                histogram[index]++;
            }
        }
    }

    private static void longRange(long[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final long v = data[p + k];
                final int index = v < 0 ? 0 : (int) (v >>> 47);
                histogram[index]++;
            }
        }
    }

    private static void longRange(LongArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final long v = data.getLong(i);
            final int index = v < 0 ? 0 : (int) (v >>> 47);
            histogram[index]++;
        }
    }

    private static void longRange(long[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final long v = data[i];
            final int index = v < 0 ? 0 : (int) (v >>> 47);
            histogram[index]++;
        }
    }

    private void histogramOfFloats(FloatArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final float[] array = (float[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    floatRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    floatRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final float[] array = (float[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                floatRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                floatRange(data, histogram, from, to - from);
            }
        }
    }

    private static void floatRange(FloatArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final float v = data.getFloat(p + k);
                final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
                histogram[index]++;
            }
        }
    }

    private static void floatRange(float[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final float v = data[p + k];
                final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
                histogram[index]++;
            }
        }
    }

    private static void floatRange(FloatArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final float v = data.getFloat(i);
            final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
            histogram[index]++;
        }
    }

    private static void floatRange(float[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final float v = data[i];
            final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
            histogram[index]++;
        }
    }

    private void histogramOfDoubles(DoubleArray data, BitArray mask, int rangeIndex) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final long[] histogram = threadHistograms[rangeIndex];
        DirectAccessible da;
        if (mask != null) {
            boolean[] maskBuffer = threadMaskBuffers[rangeIndex];
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final double[] array = (double[]) da.javaArray();
                final int intTo = (int) to;
                assert intTo == to;
                for (int p = (int) from; p < to; ) {
                    final int length = Math.min(bufferLength, intTo - p);
                    mask.getData(p, maskBuffer, 0, length);
                    doubleRange(array, histogram, maskBuffer, offset + p, length);
                    p += length;
                }
            } else {
                for (long p = from; p < to; ) {
                    final int length = (int) Math.min(bufferLength, to - p);
                    mask.getData(p, maskBuffer, 0, length);
                    doubleRange(data, histogram, maskBuffer, p, length);
                    p += length;
                }
            }
        } else {
            if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
                final int offset = da.javaArrayOffset();
                final double[] array = (double[]) da.javaArray();
                final int intTo = (int) to;
                final int intFrom = (int) from;
                assert intFrom == from && intTo == to;
                doubleRange(array, histogram, offset + intFrom, intTo - intFrom);
            } else {
                doubleRange(data, histogram, from, to - from);
            }
        }
    }

    private static void doubleRange(DoubleArray data, long[] histogram, boolean[] mask, long p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final double v = data.getDouble(p + k);
                final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
                histogram[index]++;
            }
        }
    }

    private static void doubleRange(double[] data, long[] histogram, boolean[] mask, int p, int length) {
        for (int k = 0; k < length; k++) {
            if (mask[k]) {
                final double v = data[p + k];
                final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
                histogram[index]++;
            }
        }
    }

    private static void doubleRange(DoubleArray data, long[] histogram, long p, long length) {
        for (long i = p, to = p + length; i < to; i++) {
            final double v = data.getDouble(i);
            final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
            histogram[index]++;
        }
    }

    private static void doubleRange(double[] data, long[] histogram, int p, int length) {
        for (int i = p, to = p + length; i < to; i++) {
            final double v = data[i];
            final int index = v < 0.0 ? 0 : v > 0.999999 ? 255 : (int) (v * 256.0);
            histogram[index]++;
        }
    }

    /*Repeat.AutoGeneratedEnd*/
}
