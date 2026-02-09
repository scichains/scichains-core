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

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.math.Range;
import net.algart.multimatrix.MultiMatrix;

public final class Contrast extends MultiMatrixFilter {

    private double lowLimit = 0.0;
    private double highLimit = 1.0;
    private LimitInterpretation lowLimitInterpretation = LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX;
    private LimitInterpretation highLimitInterpretation = LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX;

    public double getLowLimit() {
        return lowLimit;
    }

    public Contrast setLowLimit(double lowLimit) {
        this.lowLimit = lowLimit;
        return this;
    }

    public double getHighLimit() {
        return highLimit;
    }

    public Contrast setHighLimit(double highLimit) {
        this.highLimit = highLimit;
        return this;
    }

    public LimitInterpretation getLowLimitInterpretation() {
        return lowLimitInterpretation;
    }

    public Contrast setLowLimitInterpretation(LimitInterpretation lowLimitInterpretation) {
        this.lowLimitInterpretation = nonNull(lowLimitInterpretation);
        return this;
    }

    public LimitInterpretation getHighLimitInterpretation() {
        return highLimitInterpretation;
    }

    public Contrast setHighLimitInterpretation(LimitInterpretation highLimitInterpretation) {
        this.highLimitInterpretation = highLimitInterpretation;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (lowLimit < 0.0 || highLimit > 1.0 || lowLimit >= highLimit) {
            throw new IllegalArgumentException("Illegal low (" + lowLimit + ") or high (" + highLimit
                    + ") limits: must be 0 <= low < high <= 1");
        }
        final Matrix<? extends PArray> intensityChannel = source.intensityChannelOrNull();
        final Range rangeToContrast = rangeToContrast(intensityChannel);
        logDebug(() -> "Contrast of "
                + (lowLimitInterpretation == LimitInterpretation.PERCENTILE_BETWEEN_MIN_AND_MAX ?
                "percentile" : "value")
                + " " + lowLimit + ".." + highLimit
                + " for " + sourceMultiMatrix()
                + " (range to contrast " + rangeToContrast + ")");
        return source.contrast(rangeToContrast, true);
    }

    public Range rangeToContrast(Matrix<? extends PArray> intensity) {
        if (intensity == null) {
            return null;
        }
        final Range range = lowLimitInterpretation.isUseRange() || highLimitInterpretation.isUseRange() ?
                Arrays.rangeOf(intensity.array()) :
                null;
        final double maxPossibleValue = intensity.array().maxPossibleValue(1.0);
        long[] histogram = null;
        if (lowLimitInterpretation.isUseHistogram() || highLimitInterpretation.isUseHistogram()) {
            assert range != null;
            histogram = new long[65536];
            final double increasedSize = range.size() * ((double) histogram.length / (histogram.length - 1.0));
            final double increasedMax = range.min() + increasedSize;
            // to guarantee that even the maximal element will be inside the histogram
            Arrays.histogramOf(intensity.array(), histogram, range.min(), increasedMax);
        }
        return Range.of(
                lowLimitInterpretation.translateLimit(lowLimit, range, maxPossibleValue, histogram),
                highLimitInterpretation.translateLimit(highLimit, range, maxPossibleValue, histogram));
    }
}
