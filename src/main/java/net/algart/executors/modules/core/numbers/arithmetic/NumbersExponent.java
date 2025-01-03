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
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;
import net.algart.math.functions.ExpFunc;

import java.util.function.DoubleUnaryOperator;

public final class NumbersExponent extends NumberArrayFilter implements ReadOnlyExecutionInput {
    public enum ExponentBase {
        BASE_10(operand -> 10.0),
        BASE_E(operand -> Math.E),
        CUSTOM(operand -> operand);

        private final DoubleUnaryOperator baseProducer;

        ExponentBase(DoubleUnaryOperator baseProducer) {
            this.baseProducer = baseProducer;
        }
    }

    private ExponentBase exponentBase = ExponentBase.BASE_10;
    private double customBase = 2.0;

    public NumbersExponent() {
    }

    public ExponentBase getExponentBase() {
        return exponentBase;
    }

    public NumbersExponent setExponentBase(ExponentBase exponentBase) {
        this.exponentBase = nonNull(exponentBase);
        return this;
    }

    public double getCustomBase() {
        return customBase;
    }

    public NumbersExponent setCustomBase(double customBase) {
        this.customBase = customBase;
        return this;
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        final double base = exponentBase.baseProducer.applyAsDouble(customBase);
        // Note: LogFunc.getInstance has special branches for Math.E
        return Arrays.clone(Arrays.asFuncArray(
                ExpFunc.getInstance(base), resultClass(array), array));
    }

    static Class<? extends PArray> resultClass(PArray array) {
        return array instanceof DoubleArray ? DoubleArray.class : FloatArray.class;
    }
}
