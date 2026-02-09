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
import net.algart.arrays.PArray;
import net.algart.arrays.PNumberArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumberArraysOperation;
import net.algart.math.functions.AbstractFunc;

import java.util.List;

public final class NumbersEquality extends SeveralNumberArraysOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    private Class<?> elementType = float.class;
    private double trueValue = 1.0;
    private double falseValue = 0.0;
    private boolean invert = false;

    public NumbersEquality() {
        super(INPUT_X, INPUT_Y);
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public NumbersEquality setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public NumbersEquality setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public double getTrueValue() {
        return trueValue;
    }

    public NumbersEquality setTrueValue(double trueValue) {
        this.trueValue = trueValue;
        return this;
    }

    public double getFalseValue() {
        return falseValue;
    }

    public NumbersEquality setFalseValue(double falseValue) {
        this.falseValue = falseValue;
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public NumbersEquality setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        final var x = sources.get(0);
        final var y = sources.get(1);
        if (x.elementType() != long.class || y.elementType() != long.class) {
            return super.processNumbers(sources);
        }
        // For long types, we prefer maybe not so efficient, but more exact processing,
        // allowing to calculate precise result even for very large long integers,
        // that cannot be represented exactly by double type
        assert x.getBlockLength() == y.getBlockLength() : "must be checked by superclass";
        assert x.getArrayLength() == y.getArrayLength() : "must be checked by superclass";
        final long[] xLongs = (long[]) x.getArray();
        final long[] yLongs = (long[]) y.getArray();
        final SNumbers result = SNumbers.zeros(elementType, x.n(), x.getBlockLength());
        final double equalValue = invert ? falseValue : trueValue;
        final double nonEqualValue = invert ? trueValue : falseValue;
        for (int k = 0; k < xLongs.length; k++) {
            result.setValue(k, xLongs[k] == yLongs[k] ? equalValue : nonEqualValue);
        }
        return result;
    }

    @Override
    public PArray process(List<PNumberArray> sources, int... blockLengths) {
        final var x = sources.get(0);
        final var y = sources.get(1);
        final double equalValue = invert ? falseValue : trueValue;
        final double nonEqualValue = invert ? trueValue : falseValue;
        return Arrays.asFuncArray(new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0], x[1]);
            }

            @Override
            public double get(double x0, double x1) {
                return x0 == x1 ? equalValue : nonEqualValue;
            }
        }, Arrays.type(PNumberArray.class, elementType), x, y);
    }
}
