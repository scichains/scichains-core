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

package net.algart.executors.modules.core.numbers.copying;

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePNumberArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;
import net.algart.math.functions.AbstractFunc;

public final class FillNumbersColumnsByArithmeticProgression extends NumberArrayFilter
        implements ReadOnlyExecutionInput {
    private double startValue = 0.0;
    private double increment = 0.0;

    public double getStartValue() {
        return startValue;
    }

    public FillNumbersColumnsByArithmeticProgression setStartValue(double startValue) {
        this.startValue = startValue;
        return this;
    }

    public double getIncrement() {
        return increment;
    }

    public FillNumbersColumnsByArithmeticProgression setIncrement(double increment) {
        this.increment = increment;
        return this;
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        return Arrays.asIndexFuncArray(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        final int blockIndex = ((int) x0) / blockLength;
                        return startValue + (double) blockIndex * increment;
                    }
                }, array.type(), array.length());
    }
}
