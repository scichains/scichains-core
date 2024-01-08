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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.arrays.Arrays;
import net.algart.arrays.BitArray;
import net.algart.arrays.PNumberArray;
import net.algart.math.functions.RectangularFunc;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.NumberArrayToScalar;

public final class CheckNumbersInRange extends NumberArrayToScalar implements ReadOnlyExecutionInput {
    private double min = 0.0;
    private double max = Double.POSITIVE_INFINITY;
    private boolean invert = false;
    private CheckNumbersEquality.ActionOnFail actionOnFail = CheckNumbersEquality.ActionOnFail.THROW_EXCEPTION;

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public CheckNumbersEquality.ActionOnFail getActionOnFail() {
        return actionOnFail;
    }

    public void setActionOnFail(CheckNumbersEquality.ActionOnFail actionOnFail) {
        this.actionOnFail = nonNull(actionOnFail);
    }

    @Override
    public Object analyseArray(PNumberArray array, int blockLength, int numberOfBlocks) {
        final double inValue = invert ? 1 : 0;
        final double outValue = invert ? 0 : 1;
        final BitArray elementsAreOutOfRange = Arrays.asFuncArray(
                RectangularFunc.getInstance(min, max, inValue, outValue), BitArray.class, array);
        final boolean allInRange = elementsAreOutOfRange.isZeroFilled();
        if (allInRange) {
            return true;
        } else {
            switch (actionOnFail) {
                case RETURN_FALSE: {
                    return false;
                }
                case THROW_EXCEPTION: {
                    getScalar().setTo(false);
                    throw new AssertionError("Input array contains numbers "
                            + (invert ? "inside" : "outside") + " the range "
                            + min + ".." + max + ": " + Arrays.toString(array, ", ", 1000));
                }
                default: {
                    throw new UnsupportedOperationException("Invalid " + actionOnFail);
                }
            }
        }
    }
}
