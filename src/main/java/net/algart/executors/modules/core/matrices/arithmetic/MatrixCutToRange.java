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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.math.Range;
import net.algart.math.functions.Func1;
import net.algart.multimatrix.MultiMatrix;

public final class MatrixCutToRange extends MultiMatrixFilter {
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    public double getMin() {
        return min;
    }

    public MatrixCutToRange setMin(double min) {
        this.min = min;
        return this;
    }

    public MatrixCutToRange setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public MatrixCutToRange setMax(double max) {
        this.max = max;
        return this;
    }

    public MatrixCutToRange setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        final double scale = source.maxPossibleValue();
        final Range range = Range.valueOf(min * scale, max * scale);
        return source.asFunc((Func1) range::cut).clone();
    }
}
