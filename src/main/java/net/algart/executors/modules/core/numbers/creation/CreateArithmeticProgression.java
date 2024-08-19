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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.functions.Func1;

public final class CreateArithmeticProgression extends Executor implements ReadOnlyExecutionInput {
    private int blockLength = 1;
    private int numberOfBlocks = 100;
    private Class<?> elementType = float.class;
    private double startValue = 0.0;
    private double increment = 0.0;

    public CreateArithmeticProgression() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public CreateArithmeticProgression setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public CreateArithmeticProgression setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = nonNegative(numberOfBlocks);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public CreateArithmeticProgression setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
        return this;
    }

    public CreateArithmeticProgression setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public double getStartValue() {
        return startValue;
    }

    public CreateArithmeticProgression setStartValue(double startValue) {
        this.startValue = startValue;
        return this;
    }

    public double getIncrement() {
        return increment;
    }

    public CreateArithmeticProgression setIncrement(double increment) {
        this.increment = increment;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            final Object result = Arrays.asIndexFuncArray(
                            (Func1) x0 -> {
                                final int blockIndex = ((int) x0) / blockLength;
                                return startValue + (double) blockIndex * increment;
                            },
                            Arrays.type(PArray.class, elementType),
                            (long) blockLength * (long) numberOfBlocks)
                    .toJavaArray();
            setEndProcessingTimeStamp();
            getNumbers().setToArray(result, blockLength);
        }
    }
}
