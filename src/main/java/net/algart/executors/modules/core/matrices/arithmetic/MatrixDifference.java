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
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.math.Point;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.Morphology;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;
import java.util.stream.DoubleStream;

public final class MatrixDifference extends SeveralMultiMatricesChannelOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    public enum Operation {
        ABSOLUTE_DIFFERENCE(Func.ABS_DIFF),
        POSITIVE_DIFFERENCE(Func.POSITIVE_DIFF),
        SUBTRACT(Func.X_MINUS_Y),
        REVERSE_SUBTRACT(Func.Y_MINUS_X),
        RGB_DISTANCE(Func.ABS_DIFF);

        private final Func diffFunc;

        Operation(Func diffFunc) {
            this.diffFunc = diffFunc;
        }
    }

    public enum Postprocessing {
        NONE {
            @Override
            MultiMatrix postprocess(MultiMatrix result) {
                return result;
            }
        },
        CONTRAST {
            @Override
            MultiMatrix postprocess(MultiMatrix result) {
                return result.contrast();
            }
        },
        NONZERO_PIXELS {
            @Override
            MultiMatrix postprocess(MultiMatrix result) {
                return result.nonZeroPixels(false);
            }
        },
        ZERO_PIXELS {
            @Override
            MultiMatrix postprocess(MultiMatrix result) {
                return result.zeroPixels(false);
            }
        };

        abstract MultiMatrix postprocess(MultiMatrix result);
    }

    private static final Func SQR_FUNC = PowerFunc.getInstance(2.0, 1.0);
    private static final Func SQRT_FUNC = PowerFunc.getInstance(0.5, 1.0);

    private Operation operation = Operation.ABSOLUTE_DIFFERENCE;
    private double multiplier = 1.0;
    private Postprocessing postprocessing = Postprocessing.NONE;
    private int dilationSize = 0;
    private boolean floatResult = false;
    private boolean requireInput = false;

    public MatrixDifference() {
        super(INPUT_X, INPUT_Y);
    }

    public Operation getOperation() {
        return operation;
    }

    public MatrixDifference setOperation(Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public MatrixDifference setMultiplier(double multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public Postprocessing getPostprocessing() {
        return postprocessing;
    }

    public MatrixDifference setPostprocessing(Postprocessing postprocessing) {
        this.postprocessing = nonNull(postprocessing);
        return this;
    }

    public int getDilationSize() {
        return dilationSize;
    }

    public MatrixDifference setDilationSize(int dilationSize) {
        this.dilationSize = nonNegative(dilationSize);
        return this;
    }

    public boolean isFloatResult() {
        return floatResult;
    }

    public MatrixDifference setFloatResult(boolean floatResult) {
        this.floatResult = floatResult;
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public MatrixDifference setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final MultiMatrix x = sources.get(0);
        final MultiMatrix y = sources.get(1);
        if (x == null || y == null) {
            return null;
        }
        MultiMatrix result;
        if (multiplier == 1.0 && !floatResult) {
            // simplest way (mostly for debugging)
            result = x.asFunc(operation.diffFunc, y);
        } else {
            result = super.process(sources);
        }
        if (operation == Operation.RGB_DISTANCE) {
            if (result.numberOfChannels() > 1) {
                final Matrix<? extends PArray> sum = sumOfChannelsSquares(result, false);
                result = MultiMatrix.ofMono(Matrices.asFuncMatrix(SQRT_FUNC, result.arrayType(), sum));
            }
        }
        result = postprocessing.postprocess(result);
        if (dilationSize > 0) {
            final Morphology morphology = BasicMorphology.getInstance(null);
            final Pattern pattern = Patterns.newSphereIntegerPattern(
                    Point.origin(result.dimCount()),
                    Math.max(0.0, 0.5 * (dilationSize + 1) - 0.2));
            result = result.apply(m -> morphology.dilation(m, pattern));
        }
        return result;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m) {
        final Class<? extends PArray> resultType = resultType(sampleType());
        final Matrix<? extends PArray> result;
        if (multiplier == 1.0) {
            result = Matrices.asFuncMatrix(operation.diffFunc, resultType, m);
        } else {
            final Matrix<DoubleArray> difference = Matrices.asFuncMatrix(operation.diffFunc, DoubleArray.class, m);
            result = Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, multiplier), resultType, difference);
        }
        final double srcMaxValue = Arrays.maxPossibleValue(sampleType(), 1.0);
        final double destMaxValue = Arrays.maxPossibleValue(resultType, 1.0);
        return srcMaxValue == destMaxValue ?
                result :
                Matrices.asFuncMatrix(
                        LinearFunc.getInstance(0.0, destMaxValue / srcMaxValue), resultType, result);
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return !requireInput;
    }

    // Note: if sqrtFromResult, 1-channel matrix is supposed to be non-negative
    static Matrix<? extends PArray> sumOfChannelsSquares(MultiMatrix matrix, boolean sqrtFromResult) {
        if (matrix.numberOfChannels() == 1) {
            return sqrtFromResult ?
                    matrix.channel(0) :
                    Matrices.asFuncMatrix(SQR_FUNC, DoubleArray.class, matrix.channel(0));
        } else {
            final LinearFunc sumFunc = LinearFunc.getInstance(0.0,
                    DoubleStream.generate(() -> 1.0).limit(matrix.numberOfChannels()).toArray());
            final MultiMatrix squares = matrix.apply(m -> Matrices.asFuncMatrix(SQR_FUNC, DoubleArray.class, m));
            Matrix<? extends PArray> sum = Matrices.asFuncMatrix(sumFunc, DoubleArray.class, squares.allChannels());
            return Matrices.clone(sqrtFromResult ?
                    Matrices.asFuncMatrix(SQRT_FUNC, DoubleArray.class, sum) :
                    sum);
            // - parallel actualization useful, especially because we will use it twice
        }
    }

    private Class<? extends PArray> resultType(Class<? extends PArray> sampleType) {
        return floatResult && !DoubleArray.class.isAssignableFrom(sampleType) ? FloatArray.class : sampleType;
    }
}