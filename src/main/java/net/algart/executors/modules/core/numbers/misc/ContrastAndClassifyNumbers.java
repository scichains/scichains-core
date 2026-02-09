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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.arrays.*;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;
import net.algart.executors.modules.core.matrices.misc.LimitInterpretation;
import net.algart.math.Range;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.RectangularFunc;

public final class ContrastAndClassifyNumbers extends NumberArrayFilter implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SELECTOR = "selector";
    public static final String OUTPUT_LOW_LIMIT_VALUE = "low_limit_value";
    public static final String OUTPUT_HIGH_LIMIT_VALUE = "high_limit_value";

    private double lowLimit = 0.0;
    private double highLimit = 1.0;
    private LimitInterpretation lowLimitInterpretation = LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX;
    private LimitInterpretation highLimitInterpretation = LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX;
    private double resultMin = 0.0;
    private double resultMax = 1.0;
    private boolean truncateOverflow = true;
    private boolean invertSelector = false;

    public ContrastAndClassifyNumbers() {
        addOutputNumbers(OUTPUT_SELECTOR);
        addOutputScalar(OUTPUT_LOW_LIMIT_VALUE);
        addOutputScalar(OUTPUT_HIGH_LIMIT_VALUE);
    }

    public double getLowLimit() {
        return lowLimit;
    }

    public ContrastAndClassifyNumbers setLowLimit(double lowLimit) {
        this.lowLimit = lowLimit;
        return this;
    }

    public double getHighLimit() {
        return highLimit;
    }

    public ContrastAndClassifyNumbers setHighLimit(double highLimit) {
        this.highLimit = highLimit;
        return this;
    }

    public LimitInterpretation getLowLimitInterpretation() {
        return lowLimitInterpretation;
    }

    public ContrastAndClassifyNumbers setLowLimitInterpretation(LimitInterpretation lowLimitInterpretation) {
        this.lowLimitInterpretation = nonNull(lowLimitInterpretation);
        return this;
    }

    public LimitInterpretation getHighLimitInterpretation() {
        return highLimitInterpretation;
    }

    public ContrastAndClassifyNumbers setHighLimitInterpretation(LimitInterpretation highLimitInterpretation) {
        this.highLimitInterpretation = highLimitInterpretation;
        return this;
    }

    public double getResultMin() {
        return resultMin;
    }

    public ContrastAndClassifyNumbers setResultMin(double resultMin) {
        this.resultMin = resultMin;
        return this;
    }

    public double getResultMax() {
        return resultMax;
    }

    public ContrastAndClassifyNumbers setResultMax(double resultMax) {
        this.resultMax = resultMax;
        return this;
    }

    public boolean isTruncateOverflow() {
        return truncateOverflow;
    }

    public ContrastAndClassifyNumbers setTruncateOverflow(boolean truncateOverflow) {
        this.truncateOverflow = truncateOverflow;
        return this;
    }

    public boolean isInvertSelector() {
        return invertSelector;
    }

    public ContrastAndClassifyNumbers setInvertSelector(boolean invertSelector) {
        this.invertSelector = invertSelector;
        return this;
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        if (lowLimit > highLimit) {
            throw new IllegalArgumentException("Illegal low (" + lowLimit + ") or high (" + highLimit
                    + ") limits: must be low <= high");
        }
        if (resultMin > resultMax) {
            throw new IllegalArgumentException("Illegal result minimum (" + resultMin + ") or maximum (" + resultMax
                    + "): must be result min <= max");
        }
        final Range rangeToContrast = rangeForContrasting(array);
        logDebug(() -> "Contrast of "
                + (lowLimitInterpretation == LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX ?
                "percentile" : "value")
                + " " + lowLimit + ".." + highLimit
                + " for " + array
                + " (range to contrast " + rangeToContrast + ")");
        getScalar(OUTPUT_LOW_LIMIT_VALUE).setTo(rangeToContrast.min());
        getScalar(OUTPUT_HIGH_LIMIT_VALUE).setTo(rangeToContrast.max());
        final PNumberArray result;
        final PNumberArray selector;
        if (rangeToContrast.size() == 0.0) {
            result = array;
            selector = Arrays.nByteCopies(array.length(), (byte) (invertSelector ? 1 : 0));
        } else {
            final Range destRange = Range.of(resultMin, resultMax);
            result = Arrays.asFuncArray(
                    truncateOverflow ?
                            new AbstractFunc() {
                                final double mult = destRange.size() / rangeToContrast.size();
                                final double b = destRange.min() - rangeToContrast.min() * mult;

                                @Override
                                public double get(double... x) {
                                    return get(x[0]);
                                }

                                @Override
                                public double get(double x0) {
                                    return destRange.cut(b + mult * x0);
                                }
                            } :
                            LinearFunc.getInstance(destRange, rangeToContrast),
                    array.type(),
                    array);
            final double inValue = invertSelector ? 0 : 1;
            final double outValue = invertSelector ? 1 : 0;
            selector = Arrays.asFuncArray(
                    RectangularFunc.getInstance(rangeToContrast, inValue, outValue),
                    ByteArray.class,
                    array);
        }
        if (isOutputNecessary(OUTPUT_SELECTOR)) {
            getNumbers(OUTPUT_SELECTOR).setTo(selector, blockLength);
        }
        return result;
    }

    public Range rangeForContrasting(PArray values) {
        final Range range = lowLimitInterpretation.isUseRange() || highLimitInterpretation.isUseRange() ?
                Arrays.rangeOf(values) :
                null;
        long[] histogram = null;
        if (lowLimitInterpretation.isUseHistogram() || highLimitInterpretation.isUseHistogram()) {
            assert range != null;
            histogram = new long[65536];
            final double increasedSize = range.size() * ((double) histogram.length / (histogram.length - 1.0));
            final double increasedMax = range.min() + increasedSize;
            // to guarantee that even the maximal element will be inside the histogram
            Arrays.histogramOf(values, histogram, range.min(), increasedMax);
        }
        return Range.of(
                lowLimitInterpretation.translateLimit(lowLimit, range, 1.0, histogram),
                highLimitInterpretation.translateLimit(highLimit, range, 1.0, histogram));
    }
}
