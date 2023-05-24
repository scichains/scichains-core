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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.executors.modules.core.common.OptionalArguments;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.arrays.*;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public final class MatrixSumOfPowers extends SeveralMultiMatricesChannelOperation {
    public enum Mode {
        SUM,
        MEAN,
        CUSTOM_DIVIDER
    }

    private double power = 2.0;
    private double powerOfSum = 0.5;
    private Mode mode = Mode.SUM;
    private double customDividerOfSum = 1.0;
    // - if it equals to the number of summands, the result is averaging

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public void setPower(String power) {
        setPower(smartParseDouble(power));
    }

    public double getPowerOfSum() {
        return powerOfSum;
    }

    public void setPowerOfSum(double powerOfSum) {
        this.powerOfSum = powerOfSum;
    }

    public void setPowerOfSum(String powerOfSum) {
        setPowerOfSum(smartParseDouble(powerOfSum));
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = nonNull(mode);
    }

    public double getCustomDividerOfSum() {
        return customDividerOfSum;
    }

    public void setCustomDividerOfSum(double customDividerOfSum) {
        this.customDividerOfSum = customDividerOfSum;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m) {
        final double scale = Arrays.maxPossibleValue(sampleType(), 1.0);
        final List<Matrix<? extends PArray>> nonNull = new OptionalArguments<>(m).extract();
        final int n = nonNull.size();
        final PowerFunc powerFunc = PowerFunc.getInstance(power, 1.0 / StrictMath.pow(scale, power));
        final double mult = mode == Mode.SUM ? 1.0
            : mode == Mode.MEAN ? 1.0 / n
            : 1.0 / customDividerOfSum;
        final LinearFunc averagingFunc = LinearFunc.getInstance(0.0,
            DoubleStream.generate(() -> mult).limit(n).toArray());
        final PowerFunc sumPowerFunc = PowerFunc.getInstance(powerOfSum, scale);
        if (currentChannel() == 0) {
            logDebug(() -> "Sum of powers "
                + "((m1^" + power + "+...+mN^" + power + ") * " + mult + ")^" + powerOfSum
                + ", " + "N=" + n + " for matrices "
                + numberOfChannels() + "x" + nonNull.get(0).dimX() + "x" + nonNull.get(0).dimY());
        }
        final List<Matrix<? extends PArray>> powered = nonNull.stream().map(
            matrix -> Matrices.asFuncMatrix(powerFunc, DoubleArray.class, matrix)).collect(Collectors.toList());
        final Matrix<? extends PArray> sum = Matrices.asFuncMatrix(averagingFunc, DoubleArray.class, powered);
        return Matrices.clone(Matrices.asFuncMatrix(sumPowerFunc, sampleType(), sum));
    }

    static double smartParseDouble(String s) {
        s = s.trim();
        final int p = s.indexOf("/");
        if (p == -1) {
            return Double.parseDouble(s);
        }
        final String left = s.substring(0, p).trim();
        final String right = s.substring(p + 1).trim();
        return Double.parseDouble(left) / Double.parseDouble(right);
    }
}
