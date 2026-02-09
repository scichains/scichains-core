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

import net.algart.arrays.Histogram;
import net.algart.math.Range;

public enum LimitInterpretation {
    ABSOLUTE_VALUE(false, false) {
        @Override
        public double translateLimit(double limit, Range range, double scale, long[] histogram) {
            return limit * scale;
        }
    },
    VALUE_BETWEEN_MIN_AND_MAX(true, false) {
        @Override
        public double translateLimit(double limit, Range range, double scale, long[] histogram) {
            return range.min() + limit * range.size();
        }
    },
    PERCENTILE_BETWEEN_MIN_AND_MAX(true, true) {
        @Override
        public double translateLimit(double limit, Range range, double scale, long[] histogram) {
            final long sum = Histogram.sumOf(histogram);
            return range.min() + Histogram.percentile(histogram, sum, limit)
                    * range.size() / (histogram.length - 1.0);
        }
    };

    private final boolean useRange;
    private final boolean useHistogram;

    LimitInterpretation(boolean useRange, boolean useHistogram) {
        this.useRange = useRange;
        this.useHistogram = useHistogram;
    }

    public abstract double translateLimit(double limit, Range range, double scale, long[] histogram);

    public boolean isUseRange() {
        return useRange;
    }

    public boolean isUseHistogram() {
        return useHistogram;
    }
}
