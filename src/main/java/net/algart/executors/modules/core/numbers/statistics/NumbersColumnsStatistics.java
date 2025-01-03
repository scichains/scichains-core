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

import net.algart.arrays.Arrays;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.IRange;

import java.util.Locale;
import java.util.stream.IntStream;

public final class NumbersColumnsStatistics extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String OUTPUT_HISTOGRAM = "histogram";
    public static final String OUTPUT_MEAN = "mean";
    public static final String OUTPUT_SUM = "sum";
    public static final String OUTPUT_VARIANCE = "variance";
    public static final String OUTPUT_STANDARD_DEVIATION = "standard_deviation";
    public static final String OUTPUT_PERCENTILES_PREFIX = "percentile_";
    public static final String OUTPUT_ALL_PERCENTILES = "percentiles";
    public static final String OUTPUT_TWO_PERCENTILES_DIFFERENCE = "two_percentiles_difference";
    public static final String OUTPUT_NUMBER_OF_BLOCKS = "number_of_blocks";
    public static final String OUTPUT_BLOCK_LENGTH = "block_length";
    public static final String OUTPUT_ARRAY_LENGTH = "array_length";

    private int numberOfHistogramColumns = 100;
    private Double histogramFrom = null;
    private Double histogramTo = null;
    private double[] percentileLevels = {};

    public NumbersColumnsStatistics() {
        useVisibleResultParameter();
        setDefaultOutputNumbers(OUTPUT_HISTOGRAM);
        addOutputNumbers(OUTPUT_MEAN);
        addOutputNumbers(OUTPUT_SUM);
        addOutputNumbers(OUTPUT_VARIANCE);
        addOutputNumbers(OUTPUT_STANDARD_DEVIATION);
        for (int k = 0; k < 5; k++) {
            addOutputNumbers(outputPercentilePortName(k));
        }
        addOutputNumbers(OUTPUT_ALL_PERCENTILES);
        addOutputNumbers(OUTPUT_TWO_PERCENTILES_DIFFERENCE);
        addOutputScalar(OUTPUT_NUMBER_OF_BLOCKS);
        addOutputScalar(OUTPUT_BLOCK_LENGTH);
        addOutputScalar(OUTPUT_ARRAY_LENGTH);
    }

    public int getNumberOfHistogramColumns() {
        return numberOfHistogramColumns;
    }

    public NumbersColumnsStatistics setNumberOfHistogramColumns(int numberOfHistogramColumns) {
        this.numberOfHistogramColumns = positive(numberOfHistogramColumns);
        return this;
    }

    public Double getHistogramFrom() {
        return histogramFrom;
    }

    public NumbersColumnsStatistics setHistogramFrom(Double histogramFrom) {
        this.histogramFrom = histogramFrom;
        return this;
    }

    public NumbersColumnsStatistics setHistogramFrom(String histogramFrom) {
        return setHistogramFrom(doubleOrNull(histogramFrom));
    }

    public Double getHistogramTo() {
        return histogramTo;
    }

    public NumbersColumnsStatistics setHistogramTo(Double histogramTo) {
        this.histogramTo = histogramTo;
        return this;
    }

    public NumbersColumnsStatistics setHistogramTo(String histogramTo) {
        return setHistogramTo(doubleOrNull(histogramTo));
    }

    public double[] getPercentileLevels() {
        return percentileLevels.clone();
    }

    public NumbersColumnsStatistics setPercentileLevels(double[] percentileLevels) {
        this.percentileLevels = nonNull(percentileLevels);
        return this;
    }

    public NumbersColumnsStatistics setPercentileLevels(String percentileLevels) {
        this.percentileLevels = new SScalar(nonNull(percentileLevels)).toDoubles();
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        long t1 = debugTime();
        final Object[] allColumns = getLengthInBlock() <= 0 ?
                source.allColumnsArrays() :
                source.columnRangeArrays(getIndexInBlock(), getLengthInBlock());
        long t2 = debugTime();
        final int blockLength = allColumns.length;
        final int n = source.n();
        final SNumbers histogramNumbers = isOutputNecessary(OUTPUT_HISTOGRAM) ?
                SNumbers.zeros(long.class, numberOfHistogramColumns, blockLength) :
                null;
        final SNumbers sumNumbers = create(OUTPUT_SUM, 1, blockLength);
        final SNumbers meanNumbers = create(OUTPUT_MEAN, 1, blockLength);
        final SNumbers varianceNumbers = create(OUTPUT_VARIANCE, 1, blockLength);
        final SNumbers standardDeviationNumbers = create(OUTPUT_STANDARD_DEVIATION, 1, blockLength);
        final SNumbers percentileNumbers = isOutputNecessary(OUTPUT_ALL_PERCENTILES)
                || isOutputNecessary(OUTPUT_TWO_PERCENTILES_DIFFERENCE)
                || IntStream.range(0, percentileLevels.length).anyMatch(this::isPercentileNecessary) ?
                SNumbers.zeros(double.class, percentileLevels.length, blockLength) :
                null;
        final boolean needVariance = varianceNumbers != null || standardDeviationNumbers != null;
        final boolean needSum = needVariance || sumNumbers != null || meanNumbers != null;
        long t3 = debugTime();
        IntStream.range(0, blockLength).parallel().forEach(c -> {
            final UpdatablePArray array = (UpdatablePArray) SimpleMemoryModel.asUpdatableArray(allColumns[c]);
            assert array.length() == n;
            if (histogramNumbers != null) {
                final long[] histogram = NumbersStatistics.analyseHistogram(
                        array, numberOfHistogramColumns, histogramFrom, histogramTo);
                IntStream.range(0, histogram.length).forEach(k -> histogramNumbers.setLongValue(k, c, histogram[k]));
            }
            final double sum = needSum ? Arrays.sumOf(array) : Double.NaN;
            if (sumNumbers != null) {
                sumNumbers.setValue(c, sum);
            }
            if (meanNumbers != null) {
                meanNumbers.setValue(c, sum / n);
            }
            final double variance = needVariance ?
                    NumbersStatistics.analyseVariance(array, sum / n) :
                    Double.NaN;
            if (varianceNumbers != null) {
                varianceNumbers.setValue(c, variance);
            }
            if (standardDeviationNumbers != null) {
                standardDeviationNumbers.setValue(c, Math.sqrt(variance));
            }
            if (percentileNumbers != null) {
                final double[] percentiles = NumbersStatistics.analysePercentiles(array, percentileLevels);
                IntStream.range(0, percentiles.length).forEach(k -> percentileNumbers.setValue(k, c, percentiles[k]));
            }
        });
        long t4 = debugTime();
        logDebug(String.format(Locale.US,
                "Calculating multi-column statistics for %dx%d numbers: "
                        + "%.3f ms = %.3f ms splitting + %.3f ms initializing + %.3f ms (%.3f ns/block) processing",
                blockLength, n,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6,
                (t4 - t3) * 1e-6, (double) (t4 - t3) / n));
        getScalar(OUTPUT_NUMBER_OF_BLOCKS).setTo(n);
        getScalar(OUTPUT_BLOCK_LENGTH).setTo(blockLength);
        getScalar(OUTPUT_ARRAY_LENGTH).setTo(source.getArrayLength());
        if (percentileNumbers != null) {
            if (isOutputNecessary(OUTPUT_ALL_PERCENTILES)) {
                getNumbers(OUTPUT_ALL_PERCENTILES).setTo(percentileNumbers);
            }
            for (int k = 0; k < percentileLevels.length; k++) {
                final String portName = outputPercentilePortName(k);
                if (isOutputNecessary(portName)) {
                    getNumbers(portName).setTo(percentileNumbers.blockRange(k, 1));
                }
            }
            if (isOutputNecessary(OUTPUT_TWO_PERCENTILES_DIFFERENCE) && percentileLevels.length >= 2) {
                final SNumbers difference = create(OUTPUT_TWO_PERCENTILES_DIFFERENCE, 1, blockLength);
                assert difference != null : "illegal change of output necessary flag: parallel thread?";
                for (int c = 0; c < blockLength; c++) {
                    final double percentile1 = percentileNumbers.getValue(blockLength + c);
                    final double percentile0 = percentileNumbers.getValue(c);
                    difference.setValue(c, percentile1 - percentile0);
                }
            }
        }
        return histogramNumbers;
    }

    private SNumbers create(String outputPortName, int n, int blockLength) {
        if (isOutputNecessary(outputPortName)) {
            return getNumbers(outputPortName).setToZeros(double.class, n, blockLength);
        } else {
            return null;
        }
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    @Override
    protected IRange selectedColumnRange() {
        // - we process columns range ourselves
        return null;
    }

    private boolean isPercentileNecessary(int index) {
        return isOutputNecessary(outputPercentilePortName(index));
    }

    private static String outputPercentilePortName(int index) {
        return OUTPUT_PERCENTILES_PREFIX + (index + 1);
    }
}
