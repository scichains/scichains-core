/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;
import java.util.Objects;

public final class MatrixNormalize2DVector extends SeveralMultiMatricesOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";
    public static final String OUTPUT_X = "x";
    public static final String OUTPUT_Y = "y";
    public static final String OUTPUT_MAGNITUDE = "magnitude";

    public enum ResultForZeroVector {
        ZERO(0.0, 0.0),
        X1_Y0(1.0, 0.0),
        X0_Y1(0.0, 1.0),
        NAN(Double.NaN, Double.NaN);

        private final double defaultX;
        private final double defaultY;

        ResultForZeroVector(double defaultX, double defaultY) {
            this.defaultX = defaultX;
            this.defaultY = defaultY;
        }

        double normalizeTiny(double xOrY, double secondFromXOrY, boolean xComponent) {
            if (xOrY == 0.0) {
                if (secondFromXOrY == 0.0) {
                    return xComponent ? defaultX : defaultY;
                } else {
                    return 0.0;
                }
            } else {
                if (secondFromXOrY == 0.0) {
                    return xOrY > 0.0 ? 1.0 : -1.0;
                } else {
                    final double magnitude = Math.hypot(xOrY, secondFromXOrY);
                    // - No overflow or underflow! It must be strictly > 0.0
                    return xOrY / magnitude;
                }
            }
        }
    }

    private static final double COMPUTER_TINY_EPSILON = 1e-5;
    // - For less values we use another, more slow, but exact algorithm based on Math.hypot
    private static final Func MAGNITUDE_FUNC = new AbstractFunc() {
        @Override
        public double get(double... x) {
            return get(x[0], x[1]);
        }

        @Override
        public double get(double x, double y) {
            return Math.sqrt(x * x + y * y);
        }
    };

    private ResultForZeroVector resultForZeroVector = ResultForZeroVector.X0_Y1;

    public MatrixNormalize2DVector() {
        super(INPUT_X, INPUT_Y);
        addOutputMat(OUTPUT_X);
        addOutputMat(OUTPUT_Y);
        setDefaultOutputMat(OUTPUT_MAGNITUDE);
    }

    public ResultForZeroVector getResultForZeroVector() {
        return resultForZeroVector;
    }

    public MatrixNormalize2DVector setResultForZeroVector(ResultForZeroVector resultForZeroVector) {
        this.resultForZeroVector = nonNull(resultForZeroVector);
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        final Matrix<? extends PArray> x = sources.get(0).asMultiMatrix2D().intensityChannel();
        final Matrix<? extends PArray> y = sources.get(1).asMultiMatrix2D().intensityChannel();
        final Matrix<? extends PArray> magnitude = magnitude(x, y);
        if (isOutputNecessary(OUTPUT_X)) {
            getMat(OUTPUT_X).setTo(MultiMatrix.of2DMono(normalizeX(x, y, magnitude)));
        }
        if (isOutputNecessary(OUTPUT_Y)) {
            getMat(OUTPUT_Y).setTo(MultiMatrix.of2DMono(normalizeY(x, y, magnitude)));
        }
        return MultiMatrix.of2DMono(magnitude);
    }

    public Matrix<? extends PArray> magnitude(Matrix<? extends PArray> x, Matrix<? extends PArray> y) {
        final Matrix<? extends UpdatablePArray> result = Arrays.SMM.newFloatMatrix(x.dimensions());
        Matrices.applyFunc(null, MAGNITUDE_FUNC, result, x, y);
        return result;
    }

    public Matrix<? extends PArray> normalizeX(
            Matrix<? extends PArray> x,
            Matrix<? extends PArray> y,
            Matrix<? extends PArray> magnitude) {
        return normalizeXY(x, y, magnitude, true);
    }

    public Matrix<? extends PArray> normalizeY(
            Matrix<? extends PArray> x,
            Matrix<? extends PArray> y,
            Matrix<? extends PArray> magnitude) {
        return normalizeXY(x, y, magnitude, false);
    }

    private Matrix<? extends PArray> normalizeXY(
            Matrix<? extends PArray> x,
            Matrix<? extends PArray> y,
            Matrix<? extends PArray> magnitude,
            boolean xComponent) {
        return Matrices.clone(Matrices.asFuncMatrix(new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0], x[1], x[2]);
            }

            @Override
            public double get(double xOrY, double secondFromXOrY, double magnitude) {
                assert magnitude >= 0.0;
                if (magnitude <= COMPUTER_TINY_EPSILON && magnitude >= -COMPUTER_TINY_EPSILON) {
                    // - second check is necessary for a case of direct calling normalizeX/Y methods
                    return resultForZeroVector.normalizeTiny(xOrY, secondFromXOrY, xComponent);
                } else {
                    return xOrY / magnitude;
                }
            }
        }, FloatArray.class, xComponent ? x : y, xComponent ? y : x, magnitude));
    }
}