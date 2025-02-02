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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;

public final class RoundScalar extends ScalarFilter {
    @FunctionalInterface
    private interface DoubleToNumber {
        Number apply(double value);
    }

    public enum RoundingMode {
        SKIP_OPERATION(null),
        NONE(Double::valueOf),
        FLOOR_TO_DOUBLE(StrictMath::floor),
        CEIL_TO_DOUBLE(StrictMath::ceil),
        EVEN_ROUND_TO_DOUBLE(StrictMath::rint),
        CAST_TO_LONG(value -> (long) value),
        CAST_TO_INT(value -> (int) value),
        ROUND_TO_LONG(StrictMath::round),
        ROUND_TO_INT(value -> {
            final long i = StrictMath.round(value);
            return i < Integer.MIN_VALUE ? Integer.MIN_VALUE : i > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) i;
        }),
        EVEN_ROUND_TO_LONG(value -> (long) StrictMath.rint(value)),
        EVEN_ROUND_TO_INT(value -> (int) StrictMath.rint(value));

        private final DoubleToNumber rounder;

        RoundingMode(DoubleToNumber rounder) {
            this.rounder = rounder;
        }

        public Number round(double value) {
            return rounder.apply(value);
        }
    }

    private RoundingMode roundingMode;

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public RoundScalar setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = nonNull(roundingMode);
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        if (roundingMode.rounder == null) {
            return source;
        }
        final Number result = roundingMode.round(source.toDouble());
        return SScalar.of(result);
    }
}
