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

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.PNumberArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumberArraysOperation;
import net.algart.math.functions.Func;

import java.util.List;

public final class NumbersDifference extends SeveralNumberArraysOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    public enum Operation {
        ABSOLUTE_DIFFERENCE(Func.ABS_DIFF, false),
        POSITIVE_DIFFERENCE(Func.POSITIVE_DIFF, false),
        REVERSE_POSITIVE_DIFFERENCE(Func.POSITIVE_DIFF, true),
        SUBTRACT(Func.X_MINUS_Y, false),
        REVERSE_SUBTRACT(Func.X_MINUS_Y, true);

        private final Func diffFunc;
        private final boolean reverseOrder;

        Operation(Func diffFunc, boolean reverseOrder) {
            this.diffFunc = diffFunc;
            this.reverseOrder = reverseOrder;
        }
    }

    private Operation operation = Operation.ABSOLUTE_DIFFERENCE;
    private Class<?> elementType = float.class;

    public NumbersDifference() {
        super(INPUT_X, INPUT_Y);
    }

    public Operation getOperation() {
        return operation;
    }

    public NumbersDifference setOperation(Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public NumbersDifference setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public NumbersDifference setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }


    @Override
    public PArray process(List<PNumberArray> sources, int... blockLengths) {
        final var x = sources.get(0);
        final var y = sources.get(1);
        return operation.reverseOrder ?
                Arrays.asFuncArray(operation.diffFunc, Arrays.type(PNumberArray.class, elementType), y, x) :
                Arrays.asFuncArray(operation.diffFunc, Arrays.type(PNumberArray.class, elementType), x, y);
    }
}
