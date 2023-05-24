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

import net.algart.arrays.FloatArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;

import java.util.List;
import java.util.Objects;

public final class MatrixArctangent extends SeveralMultiMatricesOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    public enum AngleRange {
        ZERO_2PI_AS_0_1 {
            @Override
            double correctAtan2(double atan2) {
                double angle = atan2 * MULTIPLIER;
                return angle < 0.0 ? angle + 1.0 : angle;
            }
        },
        MINUS_PI_PLUS_PI_AS_MINUS_HALF_PLUS_HALF {
            @Override
            double correctAtan2(double atan2) {
                return atan2 * MULTIPLIER;
            }
        },
        MINUS_PI_PLUS_PI_AS_0_1 {
            @Override
            double correctAtan2(double atan2) {
                return atan2 * MULTIPLIER + 0.5;
            }
        };

        private static final double MULTIPLIER = 1.0 / (2.0 * StrictMath.PI);

        abstract double correctAtan2(double atan2);
    }

    private AngleRange angleRange = AngleRange.ZERO_2PI_AS_0_1;
    private double epsilonForLittleSquare = -1.0;
    private double resultForLittleSquare = 0.0;

    public MatrixArctangent() {
        super(INPUT_X, INPUT_Y);
    }

    public AngleRange getAngleRange() {
        return angleRange;
    }

    public void setAngleRange(AngleRange angleRange) {
        this.angleRange = nonNull(angleRange);
    }

    public double getEpsilonForLittleSquare() {
        return epsilonForLittleSquare;
    }

    public void setEpsilonForLittleSquare(double epsilonForLittleSquare) {
        this.epsilonForLittleSquare = epsilonForLittleSquare;
    }

    public double getResultForLittleSquare() {
        return resultForLittleSquare;
    }

    public void setResultForLittleSquare(double resultForLittleSquare) {
        this.resultForLittleSquare = resultForLittleSquare;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        Matrix<? extends PArray> x = sources.get(0).asMultiMatrix2D().intensityChannel();
        Matrix<? extends PArray> y = sources.get(1).asMultiMatrix2D().intensityChannel();
        return MultiMatrix.valueOf2DMono(Matrices.asFuncMatrix(
                new AbstractFunc() {
                     @Override
                     public double get(double... x) {
                         return get(x[1], x[0]);
                     }

                     @Override
                     public double get(double x, double y) {
                         if (x * x + y * y <= epsilonForLittleSquare) {
                             return resultForLittleSquare;
                         }
                         return angleRange.correctAtan2(Math.atan2(y, x));
                     }
                 }, FloatArray.class, x, y)).clone();
    }
}