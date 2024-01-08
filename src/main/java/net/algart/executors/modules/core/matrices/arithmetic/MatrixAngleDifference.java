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

import net.algart.arrays.FloatArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;

import java.util.List;
import java.util.Objects;

public final class MatrixAngleDifference extends SeveralMultiMatricesOperation {
    public static final String INPUT_X1 = "x1";
    public static final String INPUT_Y1 = "y1";
    public static final String INPUT_X2 = "x2";
    public static final String INPUT_Y2 = "y2";

    private AngleDistanceMetric angleDistanceMetric = AngleDistanceMetric.R_SIN;

    public MatrixAngleDifference() {
        super(INPUT_X1, INPUT_Y1, INPUT_X2, INPUT_Y2);
    }

    public AngleDistanceMetric getAngleDistanceMetric() {
        return angleDistanceMetric;
    }

    public MatrixAngleDifference setAngleDistanceMetric(AngleDistanceMetric angleDistanceMetric) {
        this.angleDistanceMetric = nonNull(angleDistanceMetric);
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        final Matrix<? extends PArray> x1 = sources.get(0).asMultiMatrix2D().asFloatingPoint().intensityChannel();
        final Matrix<? extends PArray> y1 = sources.get(1).asMultiMatrix2D().asFloatingPoint().intensityChannel();
        final Matrix<? extends PArray> x2 = sources.get(2).asMultiMatrix2D().asFloatingPoint().intensityChannel();
        final Matrix<? extends PArray> y2 = sources.get(3).asMultiMatrix2D().asFloatingPoint().intensityChannel();
        return MultiMatrix.valueOf2DMono(
                angleDistanceMetric.asAngleDifference(x1, y1, x2, y2, FloatArray.class))
                .clone();
    }

}