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

package net.algart.executors.modules.core.matrices.statistics;

import net.algart.additions.arrays.UniformHistogram256Finder;
import net.algart.arrays.BitArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToSeveralNumbers;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ImageStatistics extends MultiMatrixToSeveralNumbers {
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_MEAN = "mean";
    public static final String OUTPUT_PERCENTILE_1 = "percentile_1";
    public static final String OUTPUT_PERCENTILE_2 = "percentile_2";
    public static final String OUTPUT_MEAN_BETWEEN_PERCENTILES = "mean_between_percentiles";
    public static final String OUTPUT_NUMBER_OF_CHECKED_PIXELS = "number_of_checked_pixels";
    public static final String OUTPUT_HISTOGRAM = "histogram";

    public enum HistogramMode {
        RANGE_BASED() {
            @Override
            HistogramFinder newFinder(PArray array, BitArray maskArray, UniformHistogram256Finder finder256) {
                return new HistogramFinder.RangeBased(array, maskArray);
            }
        },
        UNIFORM_256() {
            @Override
            HistogramFinder newFinder(PArray array, BitArray maskArray, UniformHistogram256Finder finder256) {
                return new HistogramFinder.Uniform(array, maskArray, finder256);
            }
        };

        public boolean histogram256FinderUsed() {
            return this == UNIFORM_256;
        }

        abstract HistogramFinder newFinder(PArray array, BitArray maskArray, UniformHistogram256Finder finder256);
    }

    private static final String[] OUTPUT_NAMES = {
            OUTPUT_MEAN,
            OUTPUT_PERCENTILE_1,
            OUTPUT_PERCENTILE_2,
            OUTPUT_MEAN_BETWEEN_PERCENTILES,
            OUTPUT_NUMBER_OF_CHECKED_PIXELS};

    private double percentile1 = 0.1;
    private Double percentile2 = null;
    private boolean centeredHistogram = false;
    private boolean rawValues = false;
    private HistogramMode histogramMode = HistogramMode.RANGE_BASED;

    public ImageStatistics() {
        useVisibleResultParameter();
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_MASK);
        addOutputNumbers(OUTPUT_MEAN);
        addOutputNumbers(OUTPUT_PERCENTILE_1);
        addOutputNumbers(OUTPUT_PERCENTILE_2);
        addOutputNumbers(OUTPUT_MEAN_BETWEEN_PERCENTILES);
        addOutputNumbers(OUTPUT_NUMBER_OF_CHECKED_PIXELS);
        addOutputNumbers(OUTPUT_HISTOGRAM);
    }

    public Double getPercentile1() {
        return percentile1;
    }

    public ImageStatistics setPercentile1(double percentile1) {
        this.percentile1 = inRange(percentile1, 0.0, 1.0);
        return this;
    }

    public Double getPercentile2() {
        return percentile2;
    }

    public ImageStatistics setPercentile2(Double percentile2) {
        if (percentile2 != null) {
            inRange(percentile2, 0.0, 1.0);
        }
        this.percentile2 = percentile2;
        return this;
    }

    public ImageStatistics setPercentile2(String percentile2) {
        return setPercentile2(doubleOrNull(percentile2));
    }

    public boolean isCenteredHistogram() {
        return centeredHistogram;
    }

    public ImageStatistics setCenteredHistogram(boolean centeredHistogram) {
        this.centeredHistogram = centeredHistogram;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public ImageStatistics setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public HistogramMode getHistogramMode() {
        return histogramMode;
    }

    public ImageStatistics setHistogramMode(HistogramMode histogramMode) {
        this.histogramMode = nonNull(histogramMode);
        return this;
    }

    @Override
    public void analyse(Map<String, SNumbers> results, MultiMatrix source) {
        analyse(results, source, getInputMat(INPUT_MASK, true).toMultiMatrix());
    }

    public void analyse(final Map<String, SNumbers> results, final MultiMatrix source, final MultiMatrix mask) {
        Objects.requireNonNull(results, "Null results");
        Objects.requireNonNull(source, "Null source");
        source.checkDimensionEquality(mask, "source", "mask");
        long t1 = debugTime();
        final List<Matrix<? extends PArray>> allChannels = source.allChannels();
        for (String output : OUTPUT_NAMES) {
            SNumbers result = results.get(output);
            if (result == null) {
                result = new SNumbers();
                results.put(output, result);
            }
            result.setToZeros(double.class, allChannels.size(), 1);
        }
        final SNumbers histogramNumbers = isOutputNecessary(OUTPUT_HISTOGRAM) ? getNumbers(OUTPUT_HISTOGRAM) : null;
        final double percentile2 = this.percentile2 == null ? 1.0 - percentile1 : this.percentile2;

        final BitArray maskArray = mask == null ? null : mask.nonZeroAnyChannelMatrix().array();
        final UniformHistogram256Finder histogram256Finder = histogramMode.histogram256FinderUsed() ?
                UniformHistogram256Finder.newInstance(true) :
                null;
        long t2 = debugTime();
        long tInitializing = t2 - t1, tHistogram = 0, tResults = 0;
        for (int channelIndex = 0; channelIndex < allChannels.size(); channelIndex++) {
            long tt1 = debugTime();
            PArray array = allChannels.get(channelIndex).array();
            final HistogramFinder finder = histogramMode.newFinder(array, maskArray, histogram256Finder);
            finder.find();
            long tt2 = debugTime();
            tHistogram += tt2 - tt1;

            double maxPossibleValue = rawValues ? 1.0 : source.maxPossibleValue();
            results.get(OUTPUT_MEAN).setValue(channelIndex, finder.mean() / maxPossibleValue);
            results.get(OUTPUT_NUMBER_OF_CHECKED_PIXELS).setValue(channelIndex, finder.count);
            if (histogramNumbers != null) {
                histogramNumbers.replaceColumnRange(
                        channelIndex, SNumbers.valueOfArray(finder.histogram), 0, 1);
            }
            if (finder.range.size() == 0.0) {
                double r = finder.from / maxPossibleValue;
                results.get(OUTPUT_PERCENTILE_1).setValue(channelIndex, r);
                results.get(OUTPUT_PERCENTILE_2).setValue(channelIndex, r);
                results.get(OUTPUT_MEAN_BETWEEN_PERCENTILES).setValue(channelIndex, r);
            } else {
                if (!rawValues && !source.isFloatingPoint()) {
                    maxPossibleValue += 1.0;
                }
                final double columnWidth = finder.columnWidth();
                if (centeredHistogram) {
                    finder.from -= 0.5 * columnWidth;
                }
                double p1Value = finder.from + finder.percentile(percentile1) * columnWidth;
                double p2Value = finder.from + finder.percentile(percentile2) * columnWidth;
                results.get(OUTPUT_PERCENTILE_1).setValue(channelIndex, p1Value / maxPossibleValue);
                results.get(OUTPUT_PERCENTILE_2).setValue(channelIndex, p2Value / maxPossibleValue);
                double meanBetweenRanks = finder.meanBetweenRanks(percentile1, percentile2);
                double truncatedMean = finder.from + meanBetweenRanks * columnWidth;
                results.get(OUTPUT_MEAN_BETWEEN_PERCENTILES).setValue(
                        channelIndex, truncatedMean / maxPossibleValue);

                logDebug(String.format(Locale.US,
                        "Image statistics for channel %d of %s calculated in %.3f ms:%n    %s",
                        channelIndex, source, (tt2 - tt1) * 1e-6, finder));
            }
            long tt3 = debugTime();
            tResults += tt3 - tt2;
        }
        long t3 = debugTime();
        if (LOGGABLE_DEBUG) {
            logDebug(String.format(Locale.US, "Image statistics of %s%s calculated in %.3f ms: "
                            + "%.3f initializing, "
                            + "%.3f finding histogram, "
                            + "%.3f making results",
                    source, mask != null ? " with mask" : "",
                    (t3 - t1) * 1e-6,
                    tInitializing * 1e-6,
                    tHistogram * 1e-6,
                    tResults * 1e-6));
        }
    }

    @Override
    protected boolean analyseOnlyRequested() {
        return false;
    }

}
