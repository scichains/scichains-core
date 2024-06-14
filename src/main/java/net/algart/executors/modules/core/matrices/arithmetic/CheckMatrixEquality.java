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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.arrays.BitArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;

import java.util.ArrayList;
import java.util.List;

public final class CheckMatrixEquality extends SeveralMultiMatricesOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    public enum Operation {
        E0_N_ASSERTION(maxValue -> (equal, x, y) -> {
            if (equal) {
                return 0.0;
            } else {
                throw new AssertionError("Different matrices: " + x + " != " + y);
            }
        }),
        E0_N1(maxValue -> (equal, x, y) -> equal ? 0.0 : maxValue),
        // - means "if Equal, then 0, if Non-equal, then 1"
        E0_NX(maxValue -> (equal, x, y) -> equal ? 0.0 : x),
        E0_NY(maxValue -> (equal, x, y) -> equal ? 0.0 : y),
        E1_N0(maxValue -> (equal, x, y) -> equal ? maxValue : 0.0),
        E1_NX(maxValue -> (equal, x, y) -> equal ? maxValue : x),
        E1_NY(maxValue -> (equal, x, y) -> equal ? maxValue : y),
        EXY_N0(maxValue -> (equal, x, y) -> equal ? x : 0.0),
        // "EXY" means "if Equal, then X (or, that is the same, Y)"
        EXY_N1(maxValue -> (equal, x, y) -> equal ? x : maxValue);

        private final ComparisonFuncFactory comparisonFuncFactory;

        Operation(ComparisonFuncFactory comparisonFuncFactory) {
            this.comparisonFuncFactory = comparisonFuncFactory;
        }

        private interface ComparisonFuncFactory {
            SelectionFuncForEqual getSelectionFuncForEqual(double maxValue);

            default Func getSelectionForNonEquals(double maxValue) {
                final SelectionFuncForEqual selectionFunc = getSelectionFuncForEqual(maxValue);

                return new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return selectionFunc.select(x[0] == 0.0, x[1], x[2]);
                    }
                };
            }
        }
    }

    private Operation operation = Operation.E0_NY;
    private boolean requireInput = false;

    public CheckMatrixEquality() {
        super(INPUT_X, INPUT_Y);
    }

    public Operation getOperation() {
        return operation;
    }

    public CheckMatrixEquality setOperation(Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public CheckMatrixEquality setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final MultiMatrix sourceX = sources.get(0);
        final MultiMatrix sourceY = sources.get(1);
        if (sourceX == null || sourceY == null) {
            return null;
        }
        final MultiMatrix x = sourceX;
        final int n = x.numberOfChannels();
        final MultiMatrix y = sourceY.asPrecision(x.elementType()).asOtherNumberOfChannels(n);
        final List<Matrix<? extends PArray>> nonEqualBitChannels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            nonEqualBitChannels.add(Matrices.asFuncMatrix(
                    Func.ABS_DIFF, BitArray.class, x.channel(k), y.channel(k)));
            // - actually it is 1 if x!=y or 0 if x==y
        }
        final Matrix<BitArray> nonEquals = Matrices.clone(
                Matrices.asFuncMatrix(Func.MAX, BitArray.class, nonEqualBitChannels))
                .cast(BitArray.class);
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            channels.add(Matrices.asFuncMatrix(
                    operation.comparisonFuncFactory.getSelectionForNonEquals(x.maxPossibleValue()),
                    x.arrayType(),
                    nonEquals,
                    x.channel(k),
                    y.channel(k)));
        }
        return MultiMatrix.valueOf(channels).clone();
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return !requireInput;
    }

    private interface SelectionFuncForEqual {
        double select(boolean equal, double x, double y);
    }
}