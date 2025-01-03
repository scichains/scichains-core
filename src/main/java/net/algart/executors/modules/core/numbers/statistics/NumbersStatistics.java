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

package net.algart.executors.modules.core.numbers.statistics;

import net.algart.arrays.*;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;
import net.algart.math.Range;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;

import java.util.Locale;

public final class NumbersStatistics extends NumberArrayFilter implements ReadOnlyExecutionInput {
    public static final String OUTPUT_HISTOGRAM = "histogram";
    public static final String OUTPUT_MEAN = "mean";
    public static final String OUTPUT_SUM = "sum";
    public static final String OUTPUT_VARIANCE = "variance";
    public static final String OUTPUT_STANDARD_DEVIATION = "standard_deviation";
    public static final String OUTPUT_PERCENTILES_PREFIX = "percentile_";
    public static final String OUTPUT_ALL_PERCENTILES = "percentiles";
    public static final String OUTPUT_NUMBER_OF_BLOCKS = "number_of_blocks";
    public static final String OUTPUT_BLOCK_LENGTH = "block_length";
    public static final String OUTPUT_ARRAY_LENGTH = "array_length";
    public static final String OUTPUT_HASH = "hash";

    private int numberOfHistogramColumns = 100;
    private Double histogramFrom = null;
    private Double histogramTo = null;
    private double[] percentileLevels = {};

    public NumbersStatistics() {
        useVisibleResultParameter();
        setDefaultOutputNumbers(OUTPUT_HISTOGRAM);
        addOutputScalar(OUTPUT_MEAN);
        addOutputScalar(OUTPUT_SUM);
        addOutputScalar(OUTPUT_VARIANCE);
        addOutputScalar(OUTPUT_STANDARD_DEVIATION);
        addOutputScalar(outputPercentilePortName(0));
        addOutputScalar(outputPercentilePortName(1));
        addOutputNumbers(OUTPUT_ALL_PERCENTILES);
        addOutputScalar(OUTPUT_NUMBER_OF_BLOCKS);
        addOutputScalar(OUTPUT_BLOCK_LENGTH);
        addOutputScalar(OUTPUT_ARRAY_LENGTH);
        addOutputScalar(OUTPUT_HASH);
    }

    public int getNumberOfHistogramColumns() {
        return numberOfHistogramColumns;
    }

    public NumbersStatistics setNumberOfHistogramColumns(int numberOfHistogramColumns) {
        this.numberOfHistogramColumns = positive(numberOfHistogramColumns);
        return this;
    }

    public Double getHistogramFrom() {
        return histogramFrom;
    }

    public NumbersStatistics setHistogramFrom(Double histogramFrom) {
        this.histogramFrom = histogramFrom;
        return this;
    }

    public NumbersStatistics setHistogramFrom(String histogramFrom) {
        return setHistogramFrom(doubleOrNull(histogramFrom));
    }

    public Double getHistogramTo() {
        return histogramTo;
    }

    public NumbersStatistics setHistogramTo(Double histogramTo) {
        this.histogramTo = histogramTo;
        return this;
    }

    public NumbersStatistics setHistogramTo(String histogramTo) {
        return setHistogramTo(doubleOrNull(histogramTo));
    }

    public double[] getPercentileLevels() {
        return percentileLevels.clone();
    }

    public NumbersStatistics setPercentileLevels(double[] percentileLevels) {
        this.percentileLevels = nonNull(percentileLevels);
        return this;
    }

    public NumbersStatistics setPercentileLevels(String percentileLevels) {
        this.percentileLevels = new SScalar(nonNull(percentileLevels)).toDoubles();
        return this;
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        long t1 = debugTime();
        final PArray result;
        if (isOutputNecessary(defaultOutputPortName())) {
            final long[] histogram = analyseHistogram(array, numberOfHistogramColumns, histogramFrom, histogramTo);
            result = Arrays.asFuncArray(Func.IDENTITY,
                    IntArray.class,
                    SimpleMemoryModel.asUpdatableLongArray(histogram));
        } else {
            result = null;
        }
        final double sum = Arrays.sumOf(array);
        getScalar(OUTPUT_SUM).setTo(sum);
        final double average = sum / (double) array.length();
        getScalar(OUTPUT_MEAN).setTo(average);
        final double variance = analyseVariance(array, average);
        getScalar(OUTPUT_VARIANCE).setTo(variance);
        getScalar(OUTPUT_STANDARD_DEVIATION).setTo(Math.sqrt(variance));
        final double[] percentiles = analysePercentiles(array, percentileLevels);
        for (int k = 0; k < percentiles.length; k++) {
            final String portName = outputPercentilePortName(k);
            if (hasOutputPort(portName)) {
                getScalar(portName).setTo(percentiles[k]);
            }
        }
        getNumbers(OUTPUT_ALL_PERCENTILES).setTo(percentiles, 1);
        long t2 = debugTime();
        logDebug(String.format(Locale.US,
                "Calculating statistics for %dx%d numbers: %.3f ms (%.3f ns/block)",
                blockLength, numberOfBlocks,
                (t2 - t1) * 1e-6, (double) (t2 - t1) / numberOfBlocks));
        getScalar(OUTPUT_NUMBER_OF_BLOCKS).setTo(numberOfBlocks);
        getScalar(OUTPUT_BLOCK_LENGTH).setTo(blockLength);
        getScalar(OUTPUT_ARRAY_LENGTH).setTo(array.length());
        if (isOutputNecessary(OUTPUT_HASH)) {
            getScalar(OUTPUT_HASH).setTo(array.hashCode());
        }
        return result;
    }

    public static long[] analyseHistogram(
            PArray array,
            int numberOfHistogramColumns,
            Double histogramFrom,
            Double histogramTo) {
        final long[] histogram = new long[numberOfHistogramColumns];
        // - filled by zero by Java
        if (numberOfHistogramColumns == 0) {
            // - to be on the safe side
            return histogram;
        }
        Range range = histogramFrom == null || histogramTo == null ? Arrays.rangeOf(array) : null;
        final double from, to;
        if (histogramFrom == null) {
            from = range.min();
        } else {
            from = histogramFrom;
        }
        if (histogramTo == null) {
            final double increasedSize = (range.max() - from) * ((double) histogram.length / (histogram.length - 1.0));
            to = from + increasedSize;
            // to guarantee that even the maximal element will be inside the histogram
        } else {
            to = histogramTo;
        }
        if (from == to) {
            // special case: histogramOf skips processing, but we prefer to count elements equal to from/to
            BitArray nonEqual = Arrays.asFuncArray(LinearFunc.getInstance(from, -1.0), BitArray.class, array);
            histogram[0] = array.length() - Arrays.cardinality(nonEqual);
        } else {
            Arrays.histogramOf(array, histogram, from, to);
        }
        return histogram;
    }

    public static double analyseVariance(PArray array, double average) {
        final DoubleArray arrayMinusAverage = Arrays.asFuncArray(
                LinearFunc.getInstance(-average, 1.0), DoubleArray.class, array);
        final DoubleArray differenceSquares = Arrays.asFuncArray(
                PowerFunc.getInstance(2.0), DoubleArray.class, arrayMinusAverage);
        return Arrays.sumOf(differenceSquares) / (double) array.length();
    }

    // Note: modifies the passed array
    public static double[] analysePercentiles(UpdatablePArray array, double[] percentileLevels) {
        for (int k = 0; k < percentileLevels.length; k++) {
            if (percentileLevels[k] < 0.0 || percentileLevels[k] > 1.0) {
                throw new IllegalArgumentException("Illegal percentile level #" + k + " = "
                        + percentileLevels[k] + ": it is out of range 0..1");
            }
        }
        final double[] result = new double[percentileLevels.length];
        if (result.length == 0) {
            return result;
        }
        sort(array);
        final long n = array.length();
        for (int k = 0; k < result.length; k++) {
            result[k] = n == 0 ? Double.NaN : array.getDouble(Math.round(percentileLevels[k] * (n - 1)));
        }
        return result;
    }

    private static void sort(UpdatablePArray array) {
        if (array instanceof DirectAccessible da && ((DirectAccessible) array).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final int length = da.javaArrayLength();
            if (array instanceof ByteArray) {
                java.util.Arrays.parallelSort((byte[]) da.javaArray(), offset, offset + length);
            } else if (array instanceof ShortArray) {
                java.util.Arrays.parallelSort((short[]) da.javaArray(), offset, offset + length);
            } else if (array instanceof IntArray) {
                java.util.Arrays.parallelSort((int[]) da.javaArray(), offset, offset + length);
            } else if (array instanceof LongArray) {
                java.util.Arrays.parallelSort((long[]) da.javaArray(), offset, offset + length);
            } else if (array instanceof FloatArray) {
                java.util.Arrays.parallelSort((float[]) da.javaArray(), offset, offset + length);
            } else if (array instanceof DoubleArray) {
                java.util.Arrays.parallelSort((double[]) da.javaArray(), offset, offset + length);
            } else {
                Arrays.sort(array, Arrays.normalOrderComparator(array));
            }
        } else {
            Arrays.sort(array, Arrays.normalOrderComparator(array));
        }
    }

    @Override
    protected Integer resultBlockLength() {
        return 1;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    private static String outputPercentilePortName(int index) {
        return OUTPUT_PERCENTILES_PREFIX + (index + 1);
    }
}
