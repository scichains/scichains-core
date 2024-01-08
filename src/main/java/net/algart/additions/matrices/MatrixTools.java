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

package net.algart.additions.matrices;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.MultiplyingFunc;
import net.algart.math.patterns.Patterns;
import net.algart.math.patterns.UniformGridPattern;
import net.algart.matrices.morphology.BasicRankMorphology;
import net.algart.matrices.morphology.ContinuedRankMorphology;
import net.algart.matrices.morphology.RankMorphology;
import net.algart.matrices.morphology.RankPrecision;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.ConnectivityType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class MatrixTools {
    private MatrixTools() {
    }

    public static void percentileBySquare(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            long size,
            long percentileIndex,
            Matrix.ContinuationMode continuationMode,
            boolean multithreading) {
        percentileByRectangle(result, source, size, size, percentileIndex, continuationMode, multithreading);
    }

    public static void percentileByRectangle(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            long sizeX,
            long sizeY,
            long percentileIndex,
            Matrix.ContinuationMode continuationMode,
            boolean multithreading) {
        if (sizeX <= 0 || sizeY <= 0) {
            throw new IllegalArgumentException("Zero or negative sizeX=" + sizeX + " or sizeY=" + sizeY);
        }
        final IPoint min = IPoint.valueOf(-sizeX / 2, -sizeY / 2);
        final IPoint max = IPoint.valueOf(min.x() + sizeX - 1, min.y() + sizeY - 1);
        final UniformGridPattern pattern = Patterns.newRectangularIntegerPattern(min, max);
        RankMorphology morphology =
                BasicRankMorphology.getInstance(
                        multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                        1.0,
                        RankPrecision.BITS_8);
        if (continuationMode != null) {
            morphology = ContinuedRankMorphology.getInstance(morphology, continuationMode);
        }
        morphology.percentile(result, source, percentileIndex, pattern);
    }

    public static boolean getBit(Matrix<? extends BitArray> matrix, IPoint position) {
        Objects.requireNonNull(matrix, "Null matrix");
        Objects.requireNonNull(position, "Null position");
        return matrix.array().getBit(matrix.index(position.coordinates()));
    }

    public static void bitOr(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitOrToOther(result, result, other);
    }

    public static void bitOrToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.MAX, result, a, b);
    }

    public static void bitAnd(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitAndToOther(result, result, other);
    }

    public static void bitAndToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.MIN, result, a, b);
    }

    public static void bitXor(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitXorToOther(result, result, other);
    }

    public static void bitXorToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.ABS_DIFF, result, a, b);
        // - actually this and other functions are performed by classes like ArraysDiffGetDataOp
        // via maximally efficient binary operations
    }

    public static void bitDiff(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> other) {
        bitDiffToOther(result, result, other);
    }

    public static void bitDiffToOther(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> a,
            Matrix<? extends BitArray> b) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.POSITIVE_DIFF, result, a, b);
    }

    public static void bitNot(Matrix<? extends UpdatableBitArray> bitMatrix) {
        bitNotToOther(bitMatrix, bitMatrix);
    }

    public static void bitNotToOther(Matrix<? extends UpdatableBitArray> result, Matrix<? extends BitArray> source) {
        Matrices.applyFunc(ArrayContext.DEFAULT_SINGLE_THREAD, Func.REVERSE, result, source);
    }

    public static void increment(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            double increment,
            boolean multithreading) {
        Matrices.applyFunc(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        return x0 + increment;
                    }
                }, result, source);
    }

    public static void truncateByRange(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            double min,
            double max) {
        truncateByRange(result, source, min, max, false);
    }

    public static void truncateByRange(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source,
            double min,
            double max,
            boolean multithreading) {
        Matrices.applyFunc(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        return x0 < min ? min : x0 > max ? max : x0;
                    }
                }, result, source);
    }

    public static void subtract(
            Matrix<? extends UpdatablePArray> a,
            Matrix<? extends PArray> b) {
        subtract(a, a, b, false);
    }

    public static void subtract(
            Matrix<? extends UpdatablePArray> a,
            Matrix<? extends PArray> b,
            boolean multithreading) {
        subtract(a, a, b, multithreading);
    }

    public static void subtract(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> a,
            Matrix<? extends PArray> b) {
        subtract(result, a, b, false);
    }

    public static void subtract(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> a,
            Matrix<? extends PArray> b,
            boolean multithreading) {
        Matrices.applyFunc(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                Func.X_MINUS_Y, result, a, b);
    }

    public static void multiply(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> a,
            Matrix<? extends PArray> b,
            double multiplier,
            boolean multithreading) {
        Matrices.applyFunc(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                MultiplyingFunc.getInstance(multiplier), result, a, b);
    }

    public static void unpackBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler0,
            double filler1,
            boolean multithreading) {
        Arrays.unpackBits(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), bits.array(), filler0, filler1);
    }

    public static void unpackUnitBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler1,
            boolean multithreading) {
        Arrays.unpackUnitBits(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), bits.array(), filler1);
    }

    public static void unpackZeroBits(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends BitArray> bits,
            double filler0,
            boolean multithreading) {
        Arrays.unpackZeroBits(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), bits.array(), filler0);
    }

    public static void packLess(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold,
            boolean multithreading) {
        Arrays.packBitsLess(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), intensities.array(), threshold);
    }

    public static void packGreater(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold,
            boolean multithreading) {
        Arrays.packBitsGreater(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), intensities.array(), threshold);
    }

    public static void packNotLess(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold,
            boolean multithreading) {
        Arrays.packBitsGreaterOrEqual(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), intensities.array(), threshold);
    }

    public static void packNotGreater(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends PArray> intensities,
            double threshold,
            boolean multithreading) {
        Arrays.packBitsLessOrEqual(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD,
                result.array(), intensities.array(), threshold);
    }

    public static void fillPores(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            ConnectivityType connectivityType) {
        final Boundary2DScanner scanner = Boundary2DScanner.getMainBoundariesScanner(source, result, connectivityType);
        Matrices.clear(result);
        while (scanner.nextBoundary()) {
            scanner.scanBoundary();
        }
    }

    public static IPoint position(Matrix<?> matrix, long index) {
        return IPoint.valueOf(matrix.coordinates(index, null));
    }

    public static long cardinality(Matrix<? extends BitArray> matrix) {
        return cardinality(matrix, false);
    }

    public static long cardinality(Matrix<? extends BitArray> matrix, boolean multithreading) {
        return Arrays.cardinality(multithreading ? null : ArrayContext.DEFAULT_SINGLE_THREAD, matrix.array());
    }

    public static long numberOfBitsInCircle(Matrix<? extends BitArray> matrix2d, int r, long centerX, long centerY) {
        return numberOfBitsInCircle(matrix2d, r, centerX, centerY, null);
    }

    public static long numberOfBitsInCircle(
            Matrix<? extends BitArray> matrix2d,
            int r,
            long centerX,
            long centerY,
            AtomicLong areaOfCircle) {
        Objects.requireNonNull(matrix2d, "Null matrix2d");
        if (matrix2d.dimCount() != 2) {
            throw new IllegalArgumentException("Matrix is not 2-dimensional: " + matrix2d);
        }
        final BitArray array = matrix2d.array();
        final long dimX = matrix2d.dimX();
        final long dimY = matrix2d.dimY();
        final double rSqr = (double) r * (double) r;
        long area = 0;
        long count = 0;
        for (long y = Math.max(centerY - r, 0), yMax = Math.min(centerY + r, dimY - 1); y <= yMax; y++) {
            final long dy = y - centerY;
            final long dx = StrictMath.round(StrictMath.sqrt(rSqr - (double) dy * (double) dy));
            final long xMin = Math.max(centerX - dx, 0), xMax = Math.min(centerX + dx, dimX - 1);
            long offset = matrix2d.index(xMin, y);
            final long offsetMax = offset + (xMax - xMin);
            for (; offset <= offsetMax; offset++) {
                area++;
                if (array.getBit(offset)) {
                    count++;
                }
            }
        }
        if (areaOfCircle != null) {
            areaOfCircle.set(area);
        }
        return count;
    }

    public static void fillCircle(
            Matrix<? extends UpdatablePArray> matrix2d,
            long r,
            long centerX,
            long centerY,
            double filler) {
        Objects.requireNonNull(matrix2d, "Null matrix2d");
        if (matrix2d.dimCount() != 2) {
            throw new IllegalArgumentException("Matrix is not 2-dimensional: " + matrix2d);
        }
        final UpdatablePArray array = matrix2d.array();
        final long dimX = matrix2d.dimX();
        final long dimY = matrix2d.dimY();
        final double rSqr = (double) r * (double) r;
        for (long y = Math.max(centerY - r, 0), yMax = Math.min(centerY + r, dimY - 1); y <= yMax; y++) {
            final long dy = y - centerY;
            final long dx = StrictMath.round(StrictMath.sqrt(rSqr - (double) dy * (double) dy));
            final long xMin = Math.max(centerX - dx, 0), xMax = Math.min(centerX + dx, dimX - 1);
            long offset = matrix2d.index(xMin, y);
            array.fill(offset, xMax - xMin + 1, filler);
        }
    }
}
