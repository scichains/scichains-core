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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.SeveralScalarsOperation;

import java.util.List;

public final class LinearCombinationOfTwoScalars extends SeveralScalarsOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    private double a = 1.0;
    private double b = 1.0;
    private double summand = 0.0;
    private boolean absoluteValue = false;
    private RoundScalar.RoundingMode roundingMode = RoundScalar.RoundingMode.NONE;

    public LinearCombinationOfTwoScalars() {
        super(INPUT_X, INPUT_Y);
    }

    public double getA() {
        return a;
    }

    public LinearCombinationOfTwoScalars setA(double a) {
        this.a = a;
        return this;
    }

    public double getB() {
        return b;
    }

    public LinearCombinationOfTwoScalars setB(double b) {
        this.b = b;
        return this;
    }

    public double getSummand() {
        return summand;
    }

    public LinearCombinationOfTwoScalars setSummand(double summand) {
        this.summand = summand;
        return this;
    }

    public boolean isAbsoluteValue() {
        return absoluteValue;
    }

    public LinearCombinationOfTwoScalars setAbsoluteValue(boolean absoluteValue) {
        this.absoluteValue = absoluteValue;
        return this;
    }

    public RoundScalar.RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public LinearCombinationOfTwoScalars setRoundingMode(RoundScalar.RoundingMode roundingMode) {
        this.roundingMode = nonNull(roundingMode);
        return this;
    }

    @Override
    public SScalar process(List<SScalar> sources) {
        double x = sources.get(0).toDouble();
        Double y = sources.get(1).toDoubleOrNull();
        double r = y == null ?
                a * x + summand :
                a * x + b * y + summand;
        if (absoluteValue) {
            r = -r;
        }
        final Number resultNumber = roundingMode.round(r);
        return SScalar.valueOf(resultNumber);
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex > 0;
    }
}
