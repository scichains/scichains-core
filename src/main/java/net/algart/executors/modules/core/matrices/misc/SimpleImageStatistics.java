/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.matrices.misc;

import net.algart.additions.arrays.ArrayMinMaxFinder;
import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.Range;
import net.algart.multimatrix.MultiMatrix;

import java.util.function.ToIntFunction;

public enum SimpleImageStatistics {
    MEAN("mean", m -> 1),
    SUM("sum", m -> 1),
    MIN("min", m -> 1),
    MAX("max", m -> 1),
    MIN_POSITION("min_position", MultiMatrix::dimCount),
    MAX_POSITION("max_position", MultiMatrix::dimCount),
    RANGE("range", m -> 2),
    NONZERO_RANGE("non_zero_range", m -> 2),
    HASH("hash", m -> 1);

    private static final boolean DEBUG_MODE = false;
    private static final boolean LEGACY_MODE = false;
    // - both constants should be false, excepting debugging

    private final String statisticsName;
    private final ToIntFunction<MultiMatrix> channelBlockLength;

    SimpleImageStatistics(String statisticsName, ToIntFunction<MultiMatrix> channelBlockLength) {
        this.statisticsName = statisticsName;
        this.channelBlockLength = channelBlockLength;
    }

    public String statisticsName() {
        return statisticsName;
    }

    public boolean isLongResult() {
        return this == MIN_POSITION || this == MAX_POSITION || this == HASH;
    }

    public int channelBlockLength(MultiMatrix m) {
        return channelBlockLength.applyAsInt(m);
    }

    public double[] statistics(Matrix<? extends PArray> m, boolean rawValues) {
        double scale = rawValues ? 1.0 : m.array().maxPossibleValue(1.0);
        return switch (this) {
            case MEAN -> new double[]{Arrays.sumOf(m.array()) / ((double) m.size() * scale)};
            case SUM -> new double[]{Arrays.sumOf(m.array()) / scale};
            case MIN -> new double[]{findMin(m.array()) / scale};
            case MAX -> new double[]{findMax(m.array()) / scale};
            case RANGE -> {
                final double[] range = findRange(m.array());
                yield new double[]{range[0] / scale, range[1] / scale};
            }
            case NONZERO_RANGE -> {
                final Range range = MultiMatrix.nonZeroRangeOf(null, m.array());
                yield range != null ?
                        new double[]{range.min() / scale, range.max() / scale} :
                        new double[]{Double.NaN, Double.NaN};
            }
            default -> throw new UnsupportedOperationException("Unknown double statistics " + this);
        };
    }

    public long[] longStatistics(Matrix<? extends PArray> m) {
        return switch (this) {
            case MIN_POSITION -> findPositionOfMin(m);
            case MAX_POSITION -> findPositionOfMax(m);
            case HASH -> new long[]{m.hashCode()};
            default -> throw new UnsupportedOperationException("Unknown long statistics " + this);
        };
    }

    public Object allChannelsStatistics(MultiMatrix multiMatrix, boolean rawValues) {
        if (this.isLongResult()) {
            if (multiMatrix == null) {
                return new long[0];
            }
            final long[] result = new long[multiMatrix.numberOfChannels() * channelBlockLength(multiMatrix)];
            int disp = 0;
            for (Matrix<? extends PArray> m : multiMatrix.allChannels()) {
                for (long v : longStatistics(m)) {
                    result[disp++] = v;
                }
            }
            return result;
        } else {
            if (multiMatrix == null) {
                return new double[0];
            }
            final double[] result = new double[multiMatrix.numberOfChannels() * channelBlockLength(multiMatrix)];
            int disp = 0;
            for (Matrix<? extends PArray> m : multiMatrix.allChannels()) {
                for (double v : statistics(m, rawValues)) {
                    result[disp++] = v;
                }
            }
            return result;
        }
    }

    private static double findMin(PArray array) {
        if (LEGACY_MODE) {
            return Arrays.rangeOf(array).min();
        }
        ArrayMinMaxFinder.Min finder = ArrayMinMaxFinder.newInstance(true).getMinFinder();
        double min = finder.find(array).min();
        if (DEBUG_MODE) {
            long indexOfMin = finder.indexOfMin();
            Arrays.MinMaxInfo minMaxInfo = new Arrays.MinMaxInfo();
            Arrays.rangeOf(null, array, minMaxInfo);
            if (!minMaxInfo.allNaN()) {
                if (minMaxInfo.min() != min || minMaxInfo.indexOfMin() != indexOfMin) {
                    throw new AssertionError("Different min: "
                            + min + " at " + indexOfMin + " != " + minMaxInfo);
                }
            }
        }
        return min;
    }

    private static double findMax(PArray array) {
        if (LEGACY_MODE) {
            return Arrays.rangeOf(array).max();
        }
        ArrayMinMaxFinder.Max finder = ArrayMinMaxFinder.newInstance(true).getMaxFinder();
        double max = finder.find(array).max();
        if (DEBUG_MODE) {
            long indexOfMax = finder.indexOfMax();
            Arrays.MinMaxInfo minMaxInfo = new Arrays.MinMaxInfo();
            Arrays.rangeOf(null, array, minMaxInfo);
            if (!minMaxInfo.allNaN()) {
                if (minMaxInfo.max() != max || minMaxInfo.indexOfMax() != indexOfMax) {
                    throw new AssertionError("Different max: "
                            + max + " at " + indexOfMax + " != " + minMaxInfo);
                }
            }
        }
        return max;
    }

    private static long[] findPositionOfMin(Matrix<? extends PArray> m) {
        Arrays.MinMaxInfo minMaxInfo = new Arrays.MinMaxInfo();
        if (LEGACY_MODE) {
            Arrays.rangeOf(m.array(), minMaxInfo);
            final long index = minMaxInfo.indexOfMin();
            return index < 0 ? new long[0] : m.coordinates(index, null);
        }
        ArrayMinMaxFinder.Min finder = ArrayMinMaxFinder.newInstance(true).getMinFinder();
        finder.find(m.array());
        return finder.isMinFound() ? m.coordinates(finder.indexOfMin(), null) : new long[0];
    }

    private static long[] findPositionOfMax(Matrix<? extends PArray> m) {
        Arrays.MinMaxInfo minMaxInfo = new Arrays.MinMaxInfo();
        if (LEGACY_MODE) {
            Arrays.rangeOf(m.array(), minMaxInfo);
            final long index = minMaxInfo.indexOfMax();
            return index < 0 ? new long[0] : m.coordinates(index, null);
        }
        ArrayMinMaxFinder.Max finder = ArrayMinMaxFinder.newInstance(true).getMaxFinder();
        finder.find(m.array());
        return finder.isMaxFound() ? m.coordinates(finder.indexOfMax(), null) : new long[0];
    }

    private static double[] findRange(PArray array) {
        if (LEGACY_MODE) {
            final Range range = Arrays.rangeOf(array);
            return new double[]{range.min(), range.max()};
        }
        ArrayMinMaxFinder.MinMax finder = ArrayMinMaxFinder.newInstance(true).getMinMaxFinder();
        finder.find(array);
        double min = finder.min();
        double max = finder.max();
        if (DEBUG_MODE) {
            long indexOfMin = finder.indexOfMin();
            long indexOfMax = finder.indexOfMax();
            Arrays.MinMaxInfo minMaxInfo = new Arrays.MinMaxInfo();
            Arrays.rangeOf(null, array, minMaxInfo);
            if (!minMaxInfo.allNaN()) {
                // if all NaN, Arrays.rangeOf returns range -inf..+inf, but new method returns invalid range +inf..-inf
                if (minMaxInfo.min() != min || minMaxInfo.indexOfMin() != indexOfMin) {
                    throw new AssertionError("Different min: "
                            + min + " at " + indexOfMin + " != " + minMaxInfo);
                }
                if (minMaxInfo.max() != max || minMaxInfo.indexOfMax() != indexOfMax) {
                    throw new AssertionError("Different max: "
                            + max + " at " + indexOfMax + " != " + minMaxInfo);
                }
            }
        }
        return new double[]{min, max};
    }
}
