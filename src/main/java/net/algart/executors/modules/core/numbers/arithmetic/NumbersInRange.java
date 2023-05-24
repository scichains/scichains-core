/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

import java.util.stream.IntStream;

public final class NumbersInRange extends NumbersFilter implements ReadOnlyExecutionInput {
    private Class<?> elementType = float.class;
    private double min = 0.0;
    private double max = Double.POSITIVE_INFINITY;
    private double trueValue = 1.0;
    private boolean addTrueValueToIndex = false;
    private double falseValue = 0.0;
    private boolean addFalseValueToIndex = false;
    private boolean invert = false;

    public Class<?> getElementType() {
        return elementType;
    }

    public NumbersInRange setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public NumbersInRange setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public double getMin() {
        return min;
    }

    public NumbersInRange setMin(double min) {
        this.min = min;
        return this;
    }

    public NumbersInRange setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public NumbersInRange setMax(double max) {
        this.max = max;
        return this;
    }

    public NumbersInRange setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    public double getTrueValue() {
        return trueValue;
    }

    public NumbersInRange setTrueValue(double trueValue) {
        this.trueValue = trueValue;
        return this;
    }

    public boolean isAddTrueValueToIndex() {
        return addTrueValueToIndex;
    }

    public NumbersInRange setAddTrueValueToIndex(boolean addTrueValueToIndex) {
        this.addTrueValueToIndex = addTrueValueToIndex;
        return this;
    }

    public double getFalseValue() {
        return falseValue;
    }

    public NumbersInRange setFalseValue(double falseValue) {
        this.falseValue = falseValue;
        return this;
    }

    public boolean isAddFalseValueToIndex() {
        return addFalseValueToIndex;
    }

    public NumbersInRange setAddFalseValueToIndex(boolean addFalseValueToIndex) {
        this.addFalseValueToIndex = addFalseValueToIndex;
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public NumbersInRange setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        float[] array = source.toFloatArray();
        final double inValue = invert ? falseValue : trueValue;
        final double outValue = invert ? trueValue : falseValue;
        final boolean addIn = invert ? addFalseValueToIndex : addTrueValueToIndex;
        final boolean addOut = invert ? addTrueValueToIndex : addFalseValueToIndex;
        IntStream.range(0, array.length).parallel().forEach(k -> {
            final double v = array[k];
            final boolean inRange = v >= min && v <= max;
            if (inRange) {
                array[k] = (float) (addIn ? k + inValue : inValue);
            } else {
                array[k] = (float) (addOut ? k + outValue : outValue);
            }
        });
        return SNumbers.valueOfArray(array, source.getBlockLength()).toPrecision(elementType);
    }
}
