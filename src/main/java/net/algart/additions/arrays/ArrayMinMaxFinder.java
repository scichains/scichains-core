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

package net.algart.additions.arrays;

import net.algart.arrays.*;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class ArrayMinMaxFinder {
    public interface Min {
        boolean isMinFound();

        long indexOfMin();

        long indexOfMin(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound);

        double min();

        long exactMin();

        Min find(PArray data);

        Min find(Matrix<? extends PArray> data);
    }

    public interface Max {
        boolean isMaxFound();

        long indexOfMax();

        long indexOfMax(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound);

        double max();

        long exactMax();

        Max find(PArray data);

        Max find(Matrix<? extends PArray> data);
    }

    public interface MinMax extends Min, Max {
        MinMax find(PArray data);

        MinMax find(Matrix<? extends PArray> data);
    }

    private final boolean multithreading;
    private final ExtendedMinMaxInfo[] threadMinMax;
    private final ExtendedMinMaxInfo resultMinMax;
    private final long[] splitters;

    private ArrayMinMaxFinder(boolean multithreading) {
        final int cpuCount = Arrays.SystemSettings.cpuCount();
        final int numberOfTasks = !multithreading || cpuCount == 1 ? 1 : 4 * cpuCount;
        // - splitting into more than cpuCount ranges provides better performance
        this.multithreading = numberOfTasks > 1;
        this.resultMinMax = new ExtendedMinMaxInfo();
        this.threadMinMax = new ExtendedMinMaxInfo[numberOfTasks];
        if (numberOfTasks == 1) {
            this.threadMinMax[0] = this.resultMinMax;
            // - the same reference
        } else {
            java.util.Arrays.setAll(threadMinMax, k -> new ExtendedMinMaxInfo());
        }
        this.splitters = new long[numberOfTasks + 1];
    }

    public static ArrayMinMaxFinder newInstance(boolean multithreading) {
        return new ArrayMinMaxFinder(multithreading);
    }

    public Min getMinFinder() {
        return new Min() {
            @Override
            public boolean isMinFound() {
                return resultMinMax().isMinFound();
            }

            @Override
            public long indexOfMin() {
                return resultMinMax().getIndexOfMin();
            }

            @Override
            public long indexOfMin(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound) {
                return throwForNegative(indexOfMin(), exceptionSupplierWhenNotFound);
            }

            @Override
            public double min() {
                return resultMinMax().getMin();
            }

            @Override
            public long exactMin() {
                return resultMinMax().getExactMin();
            }

            @Override
            public Min find(PArray data) {
                doFind(data, true, false);
                return this;
            }

            @Override
            public Min find(Matrix<? extends PArray> matrix) {
                doFind(matrix, true, false);
                return this;
            }
        };
    }

    public Max getMaxFinder() {
        return new Max() {
            @Override
            public boolean isMaxFound() {
                return resultMinMax().isMaxFound();
            }

            @Override
            public long indexOfMax() {
                return resultMinMax().getIndexOfMax();
            }

            @Override
            public long indexOfMax(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound) {
                return throwForNegative(indexOfMax(), exceptionSupplierWhenNotFound);
            }

            @Override
            public double max() {
                return resultMinMax().getMax();
            }

            public long exactMax() {
                return resultMinMax().getExactMax();
            }

            @Override
            public Max find(PArray data) {
                doFind(data, false, true);
                return this;
            }

            @Override
            public Max find(Matrix<? extends PArray> matrix) {
                doFind(matrix, false, true);
                return this;
            }
        };
    }

    public MinMax getMinMaxFinder() {
        return new MinMax() {
            @Override
            public boolean isMinFound() {
                return resultMinMax().isMinFound();
            }

            @Override
            public long indexOfMin() {
                return resultMinMax().getIndexOfMin();
            }

            @Override
            public long indexOfMin(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound) {
                return throwForNegative(indexOfMin(), exceptionSupplierWhenNotFound);
            }

            @Override
            public double min() {
                return resultMinMax().getMin();
            }

            @Override
            public long exactMin() {
                return resultMinMax().getExactMin();
            }

            @Override
            public boolean isMaxFound() {
                return resultMinMax().isMaxFound();
            }

            @Override
            public long indexOfMax() {
                return resultMinMax().getIndexOfMax();
            }

            @Override
            public long indexOfMax(Supplier<? extends RuntimeException> exceptionSupplierWhenNotFound) {
                return throwForNegative(indexOfMax(), exceptionSupplierWhenNotFound);
            }

            @Override
            public double max() {
                return resultMinMax().getMax();
            }

            public long exactMax() {
                return resultMinMax().getExactMax();
            }

            @Override
            public MinMax find(PArray data) {
                doFind(data, true, true);
                return this;
            }

            @Override
            public MinMax find(Matrix<? extends PArray> matrix) {
                doFind(matrix, true, true);
                return this;
            }
        };
    }

    private void doFind(PArray data, boolean needMin, boolean needMax) {
        Objects.requireNonNull(data, "Null data");
        if (!needMin && !needMax) {
            throw new AssertionError("At least one of needMin/needMax must be set");
        }
        Arrays.splitToRanges(splitters, data.length());
        final Class<?> elementType = data.elementType();
        if (elementType == boolean.class) {
            Arrays.MinMaxInfo mm = new Arrays.MinMaxInfo();
            Arrays.rangeOf(data, mm);
            // - standard algorithm is good enough
            resultMinMax.setElementType(elementType)
                    .setExactAll(mm.indexOfMin(), (int) mm.min(), mm.indexOfMax(), (int) mm.max());
            return;
        }
        IntStream stream = IntStream.range(0, splitters.length - 1);
        if (multithreading) {
            stream = stream.parallel();
        }
        if (elementType == char.class) {
            final CharArray array = (CharArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForChars(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == byte.class) {
            final ByteArray array = (ByteArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForBytes(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == short.class) {
            final ShortArray array = (ShortArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForShorts(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == int.class) {
            final IntArray array = (IntArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForInts(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == long.class) {
            final LongArray array = (LongArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForLongs(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == float.class) {
            final FloatArray array = (FloatArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForFloats(array, rangeIndex, needMin, needMax);
            });
        } else if (elementType == double.class) {
            final DoubleArray array = (DoubleArray) data;
            stream.forEach(rangeIndex -> {
                minMaxForDoubles(array, rangeIndex, needMin, needMax);
            });
        } else {
            throw new AssertionError("Impossible element type " + elementType);
        }
        completeResult(needMin, needMax);
    }

    private void doFind(Matrix<? extends PArray> matrix, boolean needMin, boolean needMax) {
        Objects.requireNonNull(matrix, "Null data matrix");
        doFind(matrix.array(), needMin, needMax);
    }

    private void completeResult(boolean needMin, boolean needMax) {
        if (threadMinMax.length == 1) {
            // - nothing to do: results are already stored in this reference
            return;
        }
        resultMinMax.copyFrom(threadMinMax[0]);
        for (int k = 1; k < threadMinMax.length; k++) {
            if (needMin) {
                resultMinMax.updateMin(threadMinMax[k]);
            }
            if (needMax) {
                resultMinMax.updateMax(threadMinMax[k]);
            }
        }
    }

    private ExtendedMinMaxInfo resultMinMax() {
        checkReady();
        return resultMinMax;
    }

    private static long throwForNegative(
            long index,
            Supplier<? extends RuntimeException> exceptionSupplierForNegativeIndex) {
        if (index < 0) {
            throw exceptionSupplierForNegativeIndex.get();
        }
        return index;
    }

    private void checkReady() {
        if (!resultMinMax.isReady()) {
            throw new IllegalStateException("Minimum/maximum are not found yet");
        }
    }

    /*Repeat() Char   ==> Byte,,Short,,Int,,Long,,Float,,Double;;
               char   ==> byte,,short,,int,,long,,float,,double;;
               int\s+v\s*=\s*(.*?);     ==> int v = ($1) & 0xFF;,,int v = ($1) & 0xFFFF;,,
                            int v = $1;,,long v = $1;,,float v = $1;,,double v = $1; ;;
               (Integer.MAX_VALUE)      ==> $1,,$1,,$1,,Long.MAX_VALUE,,
                            Float.POSITIVE_INFINITY,,Double.POSITIVE_INFINITY;;
               (Integer.MIN_VALUE)      ==> $1,,$1,,$1,,Long.MIN_VALUE,,
                            Float.NEGATIVE_INFINITY,,Double.NEGATIVE_INFINITY;;
               int\s+(min|max)          ==> int $1,,int $1,,int $1,,long $1,,float $1,,double $1;;
               (setExact)(Min|Max|All)  ==> $1$2,,$1$2,,$1$2,,$1$2,,set$2,,...
     */
    private void minMaxForChars(CharArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(char.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final char[] array = (char[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                charRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                charRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                charRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                charRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                charRangeMax(data, minMax, from, to - from);
            } else {
                charRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void charRangeMin(CharArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getChar(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void charRangeMax(CharArray data, ExtendedMinMaxInfo result, long p, long length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getChar(i);
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void charRangeMinMax(CharArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getChar(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    private void charRangeMin(char[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void charRangeMax(char[] data, ExtendedMinMaxInfo result, int p, int length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void charRangeMinMax(char[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private void minMaxForBytes(ByteArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(byte.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final byte[] array = (byte[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                byteRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                byteRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                byteRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                byteRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                byteRangeMax(data, minMax, from, to - from);
            } else {
                byteRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void byteRangeMin(ByteArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getByte(i)) & 0xFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void byteRangeMax(ByteArray data, ExtendedMinMaxInfo result, long p, long length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getByte(i)) & 0xFF;
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void byteRangeMinMax(ByteArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getByte(i)) & 0xFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    private void byteRangeMin(byte[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void byteRangeMax(byte[] data, ExtendedMinMaxInfo result, int p, int length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFF;
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void byteRangeMinMax(byte[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }


    private void minMaxForShorts(ShortArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(short.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final short[] array = (short[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                shortRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                shortRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                shortRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                shortRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                shortRangeMax(data, minMax, from, to - from);
            } else {
                shortRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void shortRangeMin(ShortArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getShort(i)) & 0xFFFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void shortRangeMax(ShortArray data, ExtendedMinMaxInfo result, long p, long length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getShort(i)) & 0xFFFF;
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void shortRangeMinMax(ShortArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = (data.getShort(i)) & 0xFFFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    private void shortRangeMin(short[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFFFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void shortRangeMax(short[] data, ExtendedMinMaxInfo result, int p, int length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFFFF;
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void shortRangeMinMax(short[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = (data[i]) & 0xFFFF;
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }


    private void minMaxForInts(IntArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(int.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final int[] array = (int[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                intRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                intRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                intRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                intRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                intRangeMax(data, minMax, from, to - from);
            } else {
                intRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void intRangeMin(IntArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getInt(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void intRangeMax(IntArray data, ExtendedMinMaxInfo result, long p, long length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getInt(i);
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void intRangeMinMax(IntArray data, ExtendedMinMaxInfo result, long p, long length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final int v = data.getInt(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    private void intRangeMin(int[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void intRangeMax(int[] data, ExtendedMinMaxInfo result, int p, int length) {
        int max = Integer.MIN_VALUE;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void intRangeMinMax(int[] data, ExtendedMinMaxInfo result, int p, int length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final int v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }


    private void minMaxForLongs(LongArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(long.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final long[] array = (long[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                longRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                longRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                longRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                longRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                longRangeMax(data, minMax, from, to - from);
            } else {
                longRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void longRangeMin(LongArray data, ExtendedMinMaxInfo result, long p, long length) {
        long min = Long.MAX_VALUE;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final long v = data.getLong(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void longRangeMax(LongArray data, ExtendedMinMaxInfo result, long p, long length) {
        long max = Long.MIN_VALUE;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final long v = data.getLong(i);
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void longRangeMinMax(LongArray data, ExtendedMinMaxInfo result, long p, long length) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final long v = data.getLong(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }

    private void longRangeMin(long[] data, ExtendedMinMaxInfo result, int p, int length) {
        long min = Long.MAX_VALUE;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final long v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setExactMin(indexOfMin, min);
    }

    private void longRangeMax(long[] data, ExtendedMinMaxInfo result, int p, int length) {
        long max = Long.MIN_VALUE;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final long v = data[i];
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactMax(indexOfMax, max);
    }

    private void longRangeMinMax(long[] data, ExtendedMinMaxInfo result, int p, int length) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final long v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setExactAll(indexOfMin, min, indexOfMax, max);
    }


    private void minMaxForFloats(FloatArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(float.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final float[] array = (float[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                floatRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                floatRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                floatRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                floatRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                floatRangeMax(data, minMax, from, to - from);
            } else {
                floatRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void floatRangeMin(FloatArray data, ExtendedMinMaxInfo result, long p, long length) {
        float min = Float.POSITIVE_INFINITY;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final float v = data.getFloat(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setMin(indexOfMin, min);
    }

    private void floatRangeMax(FloatArray data, ExtendedMinMaxInfo result, long p, long length) {
        float max = Float.NEGATIVE_INFINITY;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final float v = data.getFloat(i);
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setMax(indexOfMax, max);
    }

    private void floatRangeMinMax(FloatArray data, ExtendedMinMaxInfo result, long p, long length) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final float v = data.getFloat(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setAll(indexOfMin, min, indexOfMax, max);
    }

    private void floatRangeMin(float[] data, ExtendedMinMaxInfo result, int p, int length) {
        float min = Float.POSITIVE_INFINITY;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final float v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setMin(indexOfMin, min);
    }

    private void floatRangeMax(float[] data, ExtendedMinMaxInfo result, int p, int length) {
        float max = Float.NEGATIVE_INFINITY;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final float v = data[i];
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setMax(indexOfMax, max);
    }

    private void floatRangeMinMax(float[] data, ExtendedMinMaxInfo result, int p, int length) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final float v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setAll(indexOfMin, min, indexOfMax, max);
    }


    private void minMaxForDoubles(DoubleArray data, int rangeIndex, boolean needMin, boolean needMax) {
        final long from = splitters[rangeIndex];
        final long to = splitters[rangeIndex + 1];
        final ExtendedMinMaxInfo minMax = threadMinMax[rangeIndex];
        minMax.setElementType(double.class);
        DirectAccessible da;
        if (data instanceof DirectAccessible && (da = (DirectAccessible) data).hasJavaArray()) {
            final int offset = da.javaArrayOffset();
            final double[] array = (double[]) da.javaArray();
            final int intTo = (int) to;
            final int intFrom = (int) from;
            assert intFrom == from && intTo == to;
            if (!needMax) {
                assert needMin;
                doubleRangeMin(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
            } else if (!needMin) {
                doubleRangeMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMax -= offset;
            } else {
                doubleRangeMinMax(array, minMax, offset + intFrom, intTo - intFrom);
                minMax.indexOfMin -= offset;
                minMax.indexOfMax -= offset;
            }
        } else {
            if (!needMax) {
                assert needMin;
                doubleRangeMin(data, minMax, from, to - from);
            } else if (!needMin) {
                doubleRangeMax(data, minMax, from, to - from);
            } else {
                doubleRangeMinMax(data, minMax, from, to - from);
            }
        }
    }

    private void doubleRangeMin(DoubleArray data, ExtendedMinMaxInfo result, long p, long length) {
        double min = Double.POSITIVE_INFINITY;
        long indexOfMin = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final double v = data.getDouble(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setMin(indexOfMin, min);
    }

    private void doubleRangeMax(DoubleArray data, ExtendedMinMaxInfo result, long p, long length) {
        double max = Double.NEGATIVE_INFINITY;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final double v = data.getDouble(i);
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setMax(indexOfMax, max);
    }

    private void doubleRangeMinMax(DoubleArray data, ExtendedMinMaxInfo result, long p, long length) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (long i = p, to = p + length; i < to; i++) {
            final double v = data.getDouble(i);
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setAll(indexOfMin, min, indexOfMax, max);
    }

    private void doubleRangeMin(double[] data, ExtendedMinMaxInfo result, int p, int length) {
        double min = Double.POSITIVE_INFINITY;
        long indexOfMin = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final double v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
        }
        result.setMin(indexOfMin, min);
    }

    private void doubleRangeMax(double[] data, ExtendedMinMaxInfo result, int p, int length) {
        double max = Double.NEGATIVE_INFINITY;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final double v = data[i];
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setMax(indexOfMax, max);
    }

    private void doubleRangeMinMax(double[] data, ExtendedMinMaxInfo result, int p, int length) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        long indexOfMin = -1;
        long indexOfMax = -1;
        for (int i = p, to = p + length; i < to; i++) {
            final double v = data[i];
            if (v < min) {
                min = v;
                indexOfMin = i;
            }
            if (v > max) {
                max = v;
                indexOfMax = i;
            }
        }
        result.setAll(indexOfMin, min, indexOfMax, max);
    }

    /*Repeat.AutoGeneratedEnd*/

    private static class ExtendedMinMaxInfo {
        private Class<?> elementType = null;
        private boolean floatingPoint;
        private double min;
        private double max;
        private long exactMin;
        private long exactMax;
        long indexOfMin = -157;
        long indexOfMax = -157;
        // - such values are impossible in ready results

        public boolean isReady() {
            return elementType != null;
        }

        public Class<?> getElementType() {
            return elementType;
        }

        public ExtendedMinMaxInfo setElementType(Class<?> elementType) {
            this.elementType = Objects.requireNonNull(elementType, "Null elementType");
            this.floatingPoint = elementType == float.class || elementType == double.class;
            return this;
        }

        public boolean isFloatingPoint() {
            return floatingPoint;
        }

        public boolean isMinFound() {
            return indexOfMin >= 0;
        }

        public long getIndexOfMin() {
            return indexOfMin;
        }

        public double getMin() {
            return min;
        }

        public ExtendedMinMaxInfo setMin(long indexOfMin, double min) {
            this.indexOfMin = indexOfMin;
            this.min = min;
            this.exactMin = (long) min;
            return this;
        }

        public long getIndexOfMax() {
            return indexOfMax;
        }

        public boolean isMaxFound() {
            return indexOfMax >= 0;
        }

        public double getMax() {
            return max;
        }

        public ExtendedMinMaxInfo setMax(long indexOfMax, double max) {
            this.indexOfMax = indexOfMax;
            this.max = max;
            this.exactMax = (long) max;
            return this;
        }

        public ExtendedMinMaxInfo setAll(long indexOfMin, double min, long indexOfMax, double max) {
            return setMin(indexOfMin, min).setMax(indexOfMax, max);
        }

        public long getExactMin() {
            checkFloatingPoint();
            return exactMin;
        }

        public long getExactMax() {
            checkFloatingPoint();
            return exactMax;
        }

        public ExtendedMinMaxInfo setExactMin(long indexOfMin, long min) {
            checkFloatingPoint();
            this.indexOfMin = indexOfMin;
            this.exactMin = min;
            this.min = (double) min;
            return this;
        }

        public ExtendedMinMaxInfo setExactMax(long indexOfMax, long max) {
            checkFloatingPoint();
            this.indexOfMax = indexOfMax;
            this.exactMax = max;
            this.max = (double) max;
            return this;
        }

        public ExtendedMinMaxInfo setExactAll(long indexOfMin, long min, long indexOfMax, long max) {
            return setExactMin(indexOfMin, min).setExactMax(indexOfMax, max);
        }

        public void copyFrom(ExtendedMinMaxInfo other) {
            this.setElementType(other.elementType);
            this.indexOfMin = other.indexOfMin;
            this.indexOfMax = other.indexOfMax;
            this.min = other.min;
            this.max = other.max;
            this.exactMin = other.exactMin;
            this.exactMax = other.exactMax;
        }

        public void updateMin(ExtendedMinMaxInfo other) {
            final boolean better = floatingPoint ?
                    other.min < min || (other.min == min && other.indexOfMin < indexOfMin) :
                    other.exactMin < exactMin || (other.exactMin == exactMin && other.indexOfMin < indexOfMin);
            // - note: we must also check indexes, for a case of calculating this information in several parallel threads
            if (better) {
                indexOfMin = other.indexOfMin;
                min = other.min;
                exactMin = other.exactMin;
            }
        }

        public void updateMax(ExtendedMinMaxInfo other) {
            final boolean better = floatingPoint ?
                    other.max > max || (other.max == max && other.indexOfMax < indexOfMax) :
                    other.exactMax > exactMax || (other.exactMax == exactMax && other.indexOfMax < indexOfMax);
            // - note: we must also check indexes, for a case of calculating this information in several parallel threads
            if (better) {
                indexOfMax = other.indexOfMax;
                max = other.max;
                exactMax = other.exactMax;
            }
        }

        private void checkFloatingPoint() {
            if (floatingPoint) {
                throw new IllegalStateException("Exact minimum/maximum are not allowed for "
                        + elementType.getSimpleName() + "[]");
            }
        }
    }
}
