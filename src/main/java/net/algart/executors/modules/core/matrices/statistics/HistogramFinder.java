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

package net.algart.executors.modules.core.matrices.statistics;

import net.algart.arrays.*;
import net.algart.math.Range;
import net.algart.additions.arrays.UniformHistogram256Finder;

abstract class HistogramFinder {
    final PArray array;
    final BitArray maskArray;
    final Class<?> elementType;
    final long bitsPerElement;
    final boolean floatingPoint;

    long[] histogram;
    double sum = 0.0;
    long count = 0;
    double from, to;
    Range range;

    HistogramFinder(PArray array, BitArray maskArray) {
        this.array = array;
        this.elementType = array.elementType();
        this.bitsPerElement = array.bitsPerElement();
        this.maskArray = maskArray;
        this.floatingPoint = Arrays.isFloatingPointElementType(elementType);
    }

    abstract void find();

    double mean() {
        return sum / (double) count;
    }

    double columnWidth() {
        return (to - from) / histogram.length;
    }

    double percentile(double level) {
        return Histogram.percentile(histogram, count, level);
    }

    double meanBetweenRanks(double level1, double level2) {
        double r1 = level1 * count;
        double r2 = level2 * count;
        return SummingHistogram.preciseIntegralBetweenRanks(histogram, r1, r2) / (r2 - r1);
    }

    @Override
    public String toString() {
        return "sum=" + sum + ", count=" + count + ", mean=" + mean()
                + ", from=" + from + ", to=" + to + ", range=" + range
                + " " + histogramToString();
    }

    private String histogramToString() {
        final StringBuilder sb = new StringBuilder("[");
        int p = 0;
        while (p < histogram.length && histogram[p] == 0) {
            p++;
        }
        if (p > 0) {
            sb.append(p).append("x0");
        }
        for (int to = Math.min(p + 16, histogram.length); p < to; p++) {
            if (p > 0) {
                sb.append(",");
            }
            sb.append(histogram[p]);
        }
        if (p < histogram.length) {
            sb.append(",...(elements #").append(p).append("...").append(histogram.length - 1).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    static class Uniform extends HistogramFinder {
        final UniformHistogram256Finder histogram256Finder;

        Uniform(PArray array, BitArray maskArray, UniformHistogram256Finder histogram256Finder) {
            super(array, maskArray);
            this.histogram256Finder = histogram256Finder;
        }

        @Override
        void find() {
            from = 0;
            to = array.maxPossibleValue(1.0);
            range = Range.valueOf(from, to);
            if (!floatingPoint) {
                to += 1.0;
            }
            histogram256Finder.find(array, maskArray);
            histogram = histogram256Finder.histogram();
            count = histogram256Finder.cardinality();
            sum = histogram256Finder.sum();
        }

        @Override
        public String toString() {
            return "uniform finder: " + super.toString();
        }
    }

    static class RangeBased extends HistogramFinder {
        final boolean shortIntegers;

        RangeBased(PArray array, BitArray maskArray) {
            super(array, maskArray == null ? Arrays.nBitCopies(array.length(), true) : maskArray);
            shortIntegers = elementType == boolean.class || elementType == char.class
                    || elementType == byte.class || elementType == short.class;
        }

        @Override
        void find() {
            final long bitsPerElement = array.bitsPerElement();
            double multiplier;
            if (shortIntegers) {
                assert bitsPerElement <= 16;
                histogram = new long[1 << bitsPerElement]; // zero-filled
                multiplier = Double.NaN;
                from = 0;
                to = 1 << bitsPerElement;
                range = Range.valueOf(from, to);
            } else {
                histogram = new long[65536]; // zero-filled
                range = Arrays.rangeOf(array);
                from = range.min();
                to = Arrays.isFloatingPointElementType(elementType) ? range.max() : range.max() + 1;
                multiplier = histogram.length / (to - from);
            }
            sum = 0.0;
            count = 0;
            final DataBuffer buffer = array.buffer(DataBuffer.AccessMode.READ, 65536);
            final DataBitBuffer maskBuffer = maskArray.buffer(DataBuffer.AccessMode.READ, 65536);
            for (long p = 0, n = array.length(); p < n; p += buffer.count()) {
                buffer.map(p);
                maskBuffer.map(p);
                Object data = buffer.data();
                final long[] bits = maskBuffer.data();
                if (elementType == boolean.class) {
                    assert histogram.length == 2;
                    long[] d = (long[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            int v = PackedBitArrays.getBit(d, j) ? 1 : 0;
                            histogram[v]++;
                            sum += v;
                        }
                    }
                } else if (elementType == char.class) {
                    char[] d = (char[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            int v = d[j];
                            histogram[v]++;
                            sum += v;
                        }
                    }
                } else if (elementType == byte.class) {
                    byte[] d = (byte[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            int v = d[j] & 0xFF;
                            histogram[v]++;
                            sum += v;
                        }
                    }
                } else if (elementType == short.class) {
                    short[] d = (short[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            int v = d[j] & 0xFFFF;
                            histogram[v]++;
                            sum += v;
                        }
                    }
                } else if (elementType == int.class) {
                    int[] d = (int[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            double v = (double) d[j];
                            final int index = (int) ((v - from) * multiplier);
                            assert index >= 0;
                            histogram[index >= histogram.length ? histogram.length - 1 : index]++;
                            // - check index to be on the safe side, for a case of rounding errors
                            sum += v;
                        }
                    }
                } else if (elementType == long.class) {
                    long[] d = (long[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            double v = (double) d[j];
                            final int index = (int) ((v - from) * multiplier);
                            assert index >= 0;
                            histogram[index >= histogram.length ? histogram.length - 1 : index]++;
                            // - check index to be on the safe side, for a case of rounding errors
                            sum += v;
                        }
                    }
                } else if (elementType == float.class) {
                    float[] d = (float[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            double v = d[j];
                            final int index = (int) ((v - from) * multiplier);
                            assert index >= 0;
                            histogram[index >= histogram.length ? histogram.length - 1 : index]++;
                            // - check index to be on the safe side, for a case of rounding errors
                            sum += v;
                        }
                    }
                } else if (elementType == double.class) {
                    double[] d = (double[]) data;
                    for (int i = maskBuffer.from(), j = buffer.from(), jMax = buffer.to(); j < jMax; i++, j++) {
                        if (PackedBitArrays.getBit(bits, i)) {
                            double v = d[j];
                            final int index = (int) ((v - from) * multiplier);
                            assert index >= 0;
                            histogram[index >= histogram.length ? histogram.length - 1 : index]++;
                            // - check index to be on the safe side, for a case of rounding errors
                            sum += v;
                        }
                    }
                } else {
                    throw new AssertionError("Unallowed element type: " + elementType);
                }
                count += PackedBitArrays.cardinality(bits, maskBuffer.fromIndex(), maskBuffer.toIndex());
            }
        }

        @Override
        public String toString() {
            return "range based finder: " + super.toString();
        }
    }
}
