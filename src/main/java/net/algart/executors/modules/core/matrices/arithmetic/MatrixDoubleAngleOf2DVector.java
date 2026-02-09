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

import net.algart.arrays.FloatArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;
import java.util.List;

public final class MatrixDoubleAngleOf2DVector extends SeveralMultiMatricesOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";
    public static final String OUTPUT_X = "x";
    public static final String OUTPUT_Y = "y";
    public static final String OUTPUT_MAGNITUDE = "magnitude";
    public static final String OUTPUT_XY = "xy";

    private MatrixNormalize2DVector.ResultForZeroVector resultForZeroVector =
            MatrixNormalize2DVector.ResultForZeroVector.X0_Y1;
    private boolean normalizedSource = false;
    private boolean normalizedResult = false;

    public MatrixDoubleAngleOf2DVector() {
        super(INPUT_X, INPUT_Y);
        addOutputMat(OUTPUT_X);
        addOutputMat(OUTPUT_Y);
        addOutputMat(OUTPUT_XY);
        setDefaultOutputMat(OUTPUT_MAGNITUDE);
    }

    public MatrixNormalize2DVector.ResultForZeroVector getResultForZeroVector() {
        return resultForZeroVector;
    }

    public MatrixDoubleAngleOf2DVector setResultForZeroVector(MatrixNormalize2DVector.ResultForZeroVector resultForZeroVector) {
        this.resultForZeroVector = nonNull(resultForZeroVector);
        return this;
    }

    public boolean isNormalizedSource() {
        return normalizedSource;
    }

    public MatrixDoubleAngleOf2DVector setNormalizedSource(boolean normalizedSource) {
        this.normalizedSource = normalizedSource;
        return this;
    }

    public boolean isNormalizedResult() {
        return normalizedResult;
    }

    public MatrixDoubleAngleOf2DVector setNormalizedResult(boolean normalizedResult) {
        this.normalizedResult = normalizedResult;
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final boolean necessaryXY = isOutputNecessary(OUTPUT_XY);
        final boolean requestX = isOutputNecessary(OUTPUT_X) || necessaryXY;
        final boolean requestY = isOutputNecessary(OUTPUT_Y) || necessaryXY;
        final MultiMatrix2D[] result = process(MultiMatrix.asMultiMatrices2D(sources), requestX, requestY);
        if (requestX) {
            getMat(OUTPUT_X).setTo(result[1]);
        }
        if (requestY) {
            getMat(OUTPUT_Y).setTo(result[2]);
        }
        if (necessaryXY) {
            getMat(OUTPUT_XY).setTo(MultiMatrix.of2D(Arrays.asList(
                    result[1].channel(0),
                    result[2].channel(0))));
        }
        return result[0];
    }

    public MultiMatrix2D[] process(List<MultiMatrix2D> sources, boolean requestX, boolean requestY) {
        final Matrix<? extends PArray> x = sources.get(0).intensityChannel();
        final Matrix<? extends PArray> y = sources.get(1).intensityChannel();
        final Matrix<? extends PArray> cos, sin, magnitude;
        if (normalizedSource) {
            cos = x;
            sin = y;
            magnitude = Matrices.constantMatrix(1.0, FloatArray.class, x.dimensions());
        } else {
            try (MatrixNormalize2DVector normalize2DVector = new MatrixNormalize2DVector()) {
                normalize2DVector.setResultForZeroVector(resultForZeroVector);
                magnitude = normalize2DVector.magnitude(x, y);
                cos = normalize2DVector.normalizeX(x, y, magnitude);
                sin = normalize2DVector.normalizeY(x, y, magnitude);
            }
        }
        final MultiMatrix2D[] result = new MultiMatrix2D[3];
        // - filled by null
        result[0] = MultiMatrix.of2DMono(magnitude);
        if (requestX) {
            if (!normalizedSource && !normalizedResult) {
                result[1] = MultiMatrix.of2DMono(Matrices.asFuncMatrix(new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double cos, double sin, double r) {
                        return (cos * cos - sin * sin) * r;
                    }
                }, FloatArray.class, cos, sin, magnitude)).clone();
            } else {
                result[1] = MultiMatrix.of2DMono(Matrices.asFuncMatrix(new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1]);
                    }

                    @Override
                    public double get(double cos, double sin) {
                        return cos * cos - sin * sin;
                    }
                }, FloatArray.class, cos, sin)).clone();
            }
        }
        if (requestY) {
            if (!normalizedSource && !normalizedResult) {
                result[2] = MultiMatrix.of2DMono(Matrices.asFuncMatrix(new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1], x[2]);
                    }

                    @Override
                    public double get(double cos, double sin, double r) {
                        return 2 * cos * sin * r;
                    }
                }, FloatArray.class, cos, sin, magnitude)).clone();
            } else {
                result[2] = MultiMatrix.of2DMono(Matrices.asFuncMatrix(new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0], x[1]);
                    }

                    @Override
                    public double get(double cos, double sin) {
                        return 2 * cos * sin;
                    }
                }, FloatArray.class, cos, sin)).clone();
            }
        }
        return result;
    }
}