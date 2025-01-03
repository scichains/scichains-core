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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.arrays.*;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.OptionalArguments;
import net.algart.executors.modules.core.common.numbers.SeveralNumberArraysOperation;
import net.algart.executors.modules.core.scalars.arithmetic.ProductOfTwoPowers;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;

import java.util.List;
import java.util.stream.Collectors;

public final class NumbersSumOfPowers extends SeveralNumberArraysOperation implements ReadOnlyExecutionInput {
    private double power = 2.0;
    private double powerOfSum = 0.5;
    private double dividerOfSum = 1.0;

    public double getPower() {
        return power;
    }

    public NumbersSumOfPowers setPower(double power) {
        this.power = power;
        return this;
    }

    public NumbersSumOfPowers setPower(String power) {
        return setPower(ProductOfTwoPowers.smartParseDouble(power));
    }

    public double getPowerOfSum() {
        return powerOfSum;
    }

    public NumbersSumOfPowers setPowerOfSum(double powerOfSum) {
        this.powerOfSum = powerOfSum;
        return this;
    }

    public NumbersSumOfPowers setPowerOfSum(String powerOfSum) {
        return setPowerOfSum(ProductOfTwoPowers.smartParseDouble(powerOfSum));
    }

    public double getDividerOfSum() {
        return dividerOfSum;
    }

    public NumbersSumOfPowers setDividerOfSum(double dividerOfSum) {
        this.dividerOfSum = dividerOfSum;
        return this;
    }

    @Override
    public PArray process(List<PNumberArray> sources, int... blockLengths) {
        final List<PNumberArray> nonNull = new OptionalArguments<>(sources).extract();
        final PowerFunc powerFunc = PowerFunc.getInstance(power, 1.0 / dividerOfSum);
        final LinearFunc averagingFunc = LinearFunc.getNonweightedInstance(0.0, 1.0, nonNull.size());
        final PowerFunc sumPowerFunc = PowerFunc.getInstance(powerOfSum);
        final List<PArray> powered = nonNull.stream().map(
                array -> Arrays.asFuncArray(powerFunc, DoubleArray.class, array)).collect(Collectors.toList());
        final PArray sum = Arrays.asFuncArray(averagingFunc, DoubleArray.class, powered.toArray(new PArray[0]));
        return Arrays.asFuncArray(sumPowerFunc, FloatArray.class, sum);
    }
}
