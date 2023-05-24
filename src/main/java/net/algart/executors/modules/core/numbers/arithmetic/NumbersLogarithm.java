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

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePNumberArray;
import net.algart.math.functions.LogFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;

import java.util.function.DoubleUnaryOperator;

public final class NumbersLogarithm extends NumberArrayFilter implements ReadOnlyExecutionInput {
    public enum LogarithmBase {
        BASE_10(operand -> 10.0),
        BASE_E(operand -> Math.E),
        CUSTOM(operand -> operand);

        private final DoubleUnaryOperator baseProducer;

        LogarithmBase(DoubleUnaryOperator baseProducer) {
            this.baseProducer = baseProducer;
        }
    }

    private LogarithmBase logarithmBase = LogarithmBase.BASE_10;
    private double customBase = 2.0;

    public NumbersLogarithm() {
    }

    public LogarithmBase getLogarithmBase() {
        return logarithmBase;
    }

    public NumbersLogarithm setLogarithmBase(LogarithmBase logarithmBase) {
        this.logarithmBase = nonNull(logarithmBase);
        return this;
    }

    public double getCustomBase() {
        return customBase;
    }

    public NumbersLogarithm setCustomBase(double customBase) {
        this.customBase = customBase;
        return this;
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        final double base = logarithmBase.baseProducer.applyAsDouble(customBase);
        // Note: LogFunc.getInstance has special branches for Math.E and 10.0.
        return MultiMatrix.cloneArray(Arrays.asFuncArray(
                LogFunc.getInstance(base), NumbersExponent.resultClass(array), array));
    }
}
