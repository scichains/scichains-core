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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.Executor;

public final class TruncateToRange extends Executor {
    public static final String OUTPUT_CHANGED = "changed";

    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    public TruncateToRange() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_CHANGED);
    }

    public double getMin() {
        return min;
    }

    public TruncateToRange setMin(double min) {
        this.min = min;
        return this;
    }

    public TruncateToRange setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public TruncateToRange setMax(double max) {
        this.max = max;
        return this;
    }

    public TruncateToRange setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    @Override
    public void process() {
        final double value = Double.parseDouble(getInputScalar().getValue());
        final double result = value < min ? min : Math.min(value, max);
        getScalar().setTo(result);
        getScalar(OUTPUT_CHANGED).setTo(result != value);
    }
}
