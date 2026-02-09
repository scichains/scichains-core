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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.arrays.Arrays;
import net.algart.arrays.FloatArray;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePNumberArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;
import net.algart.math.functions.AbstractFunc;

public final class NumbersInSeveralRanges extends NumberArrayFilter implements ReadOnlyExecutionInput {
    private double[] thresholds = {};
    private double[] values = {1.0};

    public double[] getThresholds() {
        return thresholds.clone();
    }

    public NumbersInSeveralRanges setThresholds(double[] thresholds) {
        this.thresholds = nonNull(thresholds).clone();
        return this;
    }

    public NumbersInSeveralRanges setThresholds(String thresholds) {
        this.thresholds = new SScalar(nonNull(thresholds)).toDoubles();
        return this;
    }

    public double[] getValues() {
        return values.clone();
    }

    public NumbersInSeveralRanges setValues(double[] values) {
        nonNull(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("At least 1 value must be specifed");
        }
        this.values = values.clone();
        return this;
    }

    public NumbersInSeveralRanges setValues(String values) {
        return setValues(new SScalar(nonNull(values)).toDoubles());
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        final int length = Math.min(values.length - 1, thresholds.length);
        return Arrays.asFuncArray(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        double result = values[0];
                        for (int k = 0; k < length; k++) {
                            if (x0 >= thresholds[k]) {
                                result = values[k + 1];
                            }
                        }
                        return result;
                    }
                },
                FloatArray.class,
                array);
    }
}
