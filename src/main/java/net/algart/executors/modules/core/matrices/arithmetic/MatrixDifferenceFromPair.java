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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.math.functions.DividingFunc;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;

public final class MatrixDifferenceFromPair extends SeveralMultiMatricesOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_A = "a";
    public static final String INPUT_B = "b";

    private static final Func DIVIDING_FUNC = DividingFunc.getInstance(1.0);

    public enum Operation {
        RGB_DISTANCE_RELATION {
            @Override
            Matrix<? extends PArray> calculate(MultiMatrix x, MultiMatrix a, MultiMatrix b) {
                final Matrix<? extends PArray> distanceToA = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, a), true);
                final Matrix<? extends PArray> distanceToB = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, b), true);
                return Matrices.clone(
                        Matrices.asFuncMatrix(DIVIDING_FUNC, FloatArray.class, distanceToA, distanceToB));
            }
        },
        RGB_DISTANCE_SQUARE_DIVIDED_BY_SUM {
            @Override
            Matrix<? extends PArray> calculate(MultiMatrix x, MultiMatrix a, MultiMatrix b) {
                Matrix<? extends PArray> distanceSquareToA = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, a), false);
                Matrix<? extends PArray> distanceSquareToB = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, b), false);
                return Matrices.clone(Matrices.asFuncMatrix(DIVIDING_FUNC, FloatArray.class,
                        distanceSquareToA,
                        Matrices.asFuncMatrix(Func.X_PLUS_Y, DoubleArray.class, distanceSquareToA, distanceSquareToB)));
            }
        },
        RGB_DISTANCE_DIVIDED_BY_SUM {
            @Override
            Matrix<? extends PArray> calculate(MultiMatrix x, MultiMatrix a, MultiMatrix b) {
                Matrix<? extends PArray> distanceToA = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, a), true);
                Matrix<? extends PArray> distanceToB = MatrixDifference.sumOfChannelsSquares(
                        x.asFunc(Func.ABS_DIFF, b), true);
                return Matrices.clone(Matrices.asFuncMatrix(DIVIDING_FUNC, FloatArray.class,
                        distanceToA,
                        Matrices.asFuncMatrix(Func.X_PLUS_Y, DoubleArray.class, distanceToA, distanceToB)));
            }
        };

        abstract Matrix<? extends PArray> calculate(MultiMatrix x, MultiMatrix a, MultiMatrix b);
    }

    private Operation operation = Operation.RGB_DISTANCE_SQUARE_DIVIDED_BY_SUM;

    public MatrixDifferenceFromPair() {
        super(INPUT_X, INPUT_A, INPUT_B);
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = nonNull(operation);
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final MultiMatrix x = sources.get(0);
        final MultiMatrix a = sources.get(1);
        final MultiMatrix b = sources.get(2);
        logDebug(() -> "Difference from pair: " + operation + " for matrices " + x + "; " + a + "; " + b);
        return MultiMatrix.of2DMono(operation.calculate(x, a, b));
    }
}
