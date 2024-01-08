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

package net.algart.executors.api.data;

import net.algart.arrays.*;

import java.util.Objects;

abstract class BandsSequentialPacker extends AbstractBandsSequentialProcessor {
    final PArray[] bands;
    final UpdatablePArray packed;

    BandsSequentialPacker(PArray[] bands, UpdatablePArray packed) {
        super(Objects.requireNonNull(bands), Objects.requireNonNull(packed));
        this.bands = bands;
        this.packed = packed;
    }

    static BandsSequentialPacker getInstance(PArray[] bands, UpdatablePArray packed) {
        if (allBandsDirect(bands)) {
            // - most typical situation
            if (packed instanceof CharArray) {
                return DirectForChars.getInstance(bands, packed);
            } else if (packed instanceof ByteArray) {
                return DirectForBytes.getInstance(bands, packed);
            } else if (packed instanceof ShortArray) {
                return DirectForShorts.getInstance(bands, packed);
            } else if (packed instanceof IntArray) {
                return DirectForInts.getInstance(bands, packed);
            } else if (packed instanceof LongArray) {
                return DirectForLongs.getInstance(bands, packed);
            } else if (packed instanceof FloatArray) {
                return DirectForFloats.getInstance(bands, packed);
            } else if (packed instanceof DoubleArray) {
                return DirectForDoubles.getInstance(bands, packed);
            } else {
                assert packed instanceof BitArray;
                // - and go to usual branch
            }
        }
        if (packed instanceof BitArray) {
            return new ForBooleans(bands, packed);
        } else if (packed instanceof CharArray) {
            return new ForChars(bands, packed);
        } else if (packed instanceof ByteArray) {
            return new ForBytes(bands, packed);
        } else if (packed instanceof ShortArray) {
            return new ForShorts(bands, packed);
        } else if (packed instanceof IntArray) {
            return new ForInts(bands, packed);
        } else if (packed instanceof LongArray) {
            return new ForLongs(bands, packed);
        } else if (packed instanceof FloatArray) {
            return new ForFloats(bands, packed);
        } else if (packed instanceof DoubleArray) {
            return new ForDoubles(bands, packed);
        } else {
            throw new AssertionError("Illegal " + packed);
        }
    }

    @Override
    public abstract void close();

    @Override
    protected abstract void processSubArr(long position, int count, int threadIndex);

    /*Repeat() Booleans ==> Chars,,Bytes,,Shorts,,Ints,,Longs,,Floats,,Doubles;;
               boolean  ==> char,,byte,,short,,int,,long,,float,,double;;
               BOOLEAN ==> CHAR,,BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE
     */
    static class ForBooleans extends BandsSequentialPacker {
        private final boolean[][] threadBandArray;
        private final boolean[][] threadPackedArray;

        ForBooleans(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new boolean[numberOfTasks()][];
            this.threadPackedArray = new boolean[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (boolean[]) BOOLEAN_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (boolean[]) BOOLEAN_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                BOOLEAN_BUFFERS.releaseArray(this.threadPackedArray[k]);
                BOOLEAN_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final boolean[] bandArray = threadBandArray[threadIndex];
            final boolean[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class ForChars extends BandsSequentialPacker {
        private final char[][] threadBandArray;
        private final char[][] threadPackedArray;

        ForChars(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new char[numberOfTasks()][];
            this.threadPackedArray = new char[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (char[]) CHAR_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (char[]) CHAR_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                CHAR_BUFFERS.releaseArray(this.threadPackedArray[k]);
                CHAR_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final char[] bandArray = threadBandArray[threadIndex];
            final char[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForBytes extends BandsSequentialPacker {
        private final byte[][] threadBandArray;
        private final byte[][] threadPackedArray;

        ForBytes(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new byte[numberOfTasks()][];
            this.threadPackedArray = new byte[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (byte[]) BYTE_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (byte[]) BYTE_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                BYTE_BUFFERS.releaseArray(this.threadPackedArray[k]);
                BYTE_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final byte[] bandArray = threadBandArray[threadIndex];
            final byte[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForShorts extends BandsSequentialPacker {
        private final short[][] threadBandArray;
        private final short[][] threadPackedArray;

        ForShorts(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new short[numberOfTasks()][];
            this.threadPackedArray = new short[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (short[]) SHORT_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (short[]) SHORT_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                SHORT_BUFFERS.releaseArray(this.threadPackedArray[k]);
                SHORT_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final short[] bandArray = threadBandArray[threadIndex];
            final short[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForInts extends BandsSequentialPacker {
        private final int[][] threadBandArray;
        private final int[][] threadPackedArray;

        ForInts(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new int[numberOfTasks()][];
            this.threadPackedArray = new int[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (int[]) INT_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (int[]) INT_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                INT_BUFFERS.releaseArray(this.threadPackedArray[k]);
                INT_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final int[] bandArray = threadBandArray[threadIndex];
            final int[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForLongs extends BandsSequentialPacker {
        private final long[][] threadBandArray;
        private final long[][] threadPackedArray;

        ForLongs(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new long[numberOfTasks()][];
            this.threadPackedArray = new long[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (long[]) LONG_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (long[]) LONG_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                LONG_BUFFERS.releaseArray(this.threadPackedArray[k]);
                LONG_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final long[] bandArray = threadBandArray[threadIndex];
            final long[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForFloats extends BandsSequentialPacker {
        private final float[][] threadBandArray;
        private final float[][] threadPackedArray;

        ForFloats(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new float[numberOfTasks()][];
            this.threadPackedArray = new float[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (float[]) FLOAT_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (float[]) FLOAT_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                FLOAT_BUFFERS.releaseArray(this.threadPackedArray[k]);
                FLOAT_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final float[] bandArray = threadBandArray[threadIndex];
            final float[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }

    static class ForDoubles extends BandsSequentialPacker {
        private final double[][] threadBandArray;
        private final double[][] threadPackedArray;

        ForDoubles(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.threadBandArray = new double[numberOfTasks()][];
            this.threadPackedArray = new double[numberOfTasks()][];
            for (int k = 0; k < threadBandArray.length; k++) {
                this.threadBandArray[k] = (double[]) DOUBLE_BUFFERS.requestArray();
                // - note: only first part 1/bands.length will be used in bandArray
                this.threadPackedArray[k] = (double[]) DOUBLE_BUFFERS.requestArray();
            }
        }

        @Override
        public void close() {
            for (int k = threadBandArray.length - 1; k >= 0; k--) {
                DOUBLE_BUFFERS.releaseArray(this.threadPackedArray[k]);
                DOUBLE_BUFFERS.releaseArray(this.threadBandArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final double[] bandArray = threadBandArray[threadIndex];
            final double[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                bands[b].getData(position, bandArray, 0, count);
                for (int k = 0, disp = b; k < count; k++, disp += step) {
                    packedArray[disp] = bandArray[k];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() Chars ==> Bytes,,Shorts,,Ints,,Longs,,Floats,,Doubles;;
               char ==> byte,,short,,int,,long,,float,,double;;
               CHAR ==> BYTE,,SHORT,,INT,,LONG,,FLOAT,,DOUBLE
     */
    static class DirectForChars extends BandsSequentialPacker {
        final char[][] bandArrays;
        final int[] bandArraysOffsets;
        final char[][] threadPackedArray;

        DirectForChars(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new char[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (char[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new char[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (char[]) CHAR_BUFFERS.requestArray();
            }
        }

        static DirectForChars getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForChars(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                CHAR_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final char[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final char[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForChars {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final char[] packedArray = threadPackedArray[threadIndex];
                final char[] band0 = bandArrays[0];
                final char[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForChars {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final char[] packedArray = threadPackedArray[threadIndex];
                final char[] band0 = bandArrays[0];
                final char[] band1 = bandArrays[1];
                final char[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForChars {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final char[] packedArray = threadPackedArray[threadIndex];
                final char[] band0 = bandArrays[0];
                final char[] band1 = bandArrays[1];
                final char[] band2 = bandArrays[2];
                final char[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static class DirectForBytes extends BandsSequentialPacker {
        final byte[][] bandArrays;
        final int[] bandArraysOffsets;
        final byte[][] threadPackedArray;

        DirectForBytes(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new byte[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (byte[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new byte[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (byte[]) BYTE_BUFFERS.requestArray();
            }
        }

        static DirectForBytes getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForBytes(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                BYTE_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final byte[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final byte[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForBytes {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final byte[] packedArray = threadPackedArray[threadIndex];
                final byte[] band0 = bandArrays[0];
                final byte[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForBytes {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final byte[] packedArray = threadPackedArray[threadIndex];
                final byte[] band0 = bandArrays[0];
                final byte[] band1 = bandArrays[1];
                final byte[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForBytes {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final byte[] packedArray = threadPackedArray[threadIndex];
                final byte[] band0 = bandArrays[0];
                final byte[] band1 = bandArrays[1];
                final byte[] band2 = bandArrays[2];
                final byte[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }

    static class DirectForShorts extends BandsSequentialPacker {
        final short[][] bandArrays;
        final int[] bandArraysOffsets;
        final short[][] threadPackedArray;

        DirectForShorts(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new short[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (short[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new short[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (short[]) SHORT_BUFFERS.requestArray();
            }
        }

        static DirectForShorts getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForShorts(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                SHORT_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final short[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final short[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForShorts {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final short[] packedArray = threadPackedArray[threadIndex];
                final short[] band0 = bandArrays[0];
                final short[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForShorts {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final short[] packedArray = threadPackedArray[threadIndex];
                final short[] band0 = bandArrays[0];
                final short[] band1 = bandArrays[1];
                final short[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForShorts {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final short[] packedArray = threadPackedArray[threadIndex];
                final short[] band0 = bandArrays[0];
                final short[] band1 = bandArrays[1];
                final short[] band2 = bandArrays[2];
                final short[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }

    static class DirectForInts extends BandsSequentialPacker {
        final int[][] bandArrays;
        final int[] bandArraysOffsets;
        final int[][] threadPackedArray;

        DirectForInts(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new int[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (int[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new int[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (int[]) INT_BUFFERS.requestArray();
            }
        }

        static DirectForInts getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForInts(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                INT_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final int[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final int[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForInts {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final int[] packedArray = threadPackedArray[threadIndex];
                final int[] band0 = bandArrays[0];
                final int[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForInts {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final int[] packedArray = threadPackedArray[threadIndex];
                final int[] band0 = bandArrays[0];
                final int[] band1 = bandArrays[1];
                final int[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForInts {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final int[] packedArray = threadPackedArray[threadIndex];
                final int[] band0 = bandArrays[0];
                final int[] band1 = bandArrays[1];
                final int[] band2 = bandArrays[2];
                final int[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }

    static class DirectForLongs extends BandsSequentialPacker {
        final long[][] bandArrays;
        final int[] bandArraysOffsets;
        final long[][] threadPackedArray;

        DirectForLongs(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new long[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (long[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new long[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (long[]) LONG_BUFFERS.requestArray();
            }
        }

        static DirectForLongs getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForLongs(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                LONG_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final long[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final long[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForLongs {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final long[] packedArray = threadPackedArray[threadIndex];
                final long[] band0 = bandArrays[0];
                final long[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForLongs {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final long[] packedArray = threadPackedArray[threadIndex];
                final long[] band0 = bandArrays[0];
                final long[] band1 = bandArrays[1];
                final long[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForLongs {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final long[] packedArray = threadPackedArray[threadIndex];
                final long[] band0 = bandArrays[0];
                final long[] band1 = bandArrays[1];
                final long[] band2 = bandArrays[2];
                final long[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }

    static class DirectForFloats extends BandsSequentialPacker {
        final float[][] bandArrays;
        final int[] bandArraysOffsets;
        final float[][] threadPackedArray;

        DirectForFloats(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new float[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (float[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new float[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (float[]) FLOAT_BUFFERS.requestArray();
            }
        }

        static DirectForFloats getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForFloats(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                FLOAT_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final float[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final float[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForFloats {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final float[] packedArray = threadPackedArray[threadIndex];
                final float[] band0 = bandArrays[0];
                final float[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForFloats {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final float[] packedArray = threadPackedArray[threadIndex];
                final float[] band0 = bandArrays[0];
                final float[] band1 = bandArrays[1];
                final float[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForFloats {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final float[] packedArray = threadPackedArray[threadIndex];
                final float[] band0 = bandArrays[0];
                final float[] band1 = bandArrays[1];
                final float[] band2 = bandArrays[2];
                final float[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }

    static class DirectForDoubles extends BandsSequentialPacker {
        final double[][] bandArrays;
        final int[] bandArraysOffsets;
        final double[][] threadPackedArray;

        DirectForDoubles(PArray[] bands, UpdatablePArray packed) {
            super(bands, packed);
            this.bandArrays = new double[bands.length][];
            this.bandArraysOffsets = new int[bands.length];
            for (int b = 0; b < bands.length; b++) {
                this.bandArrays[b] = (double[]) ((DirectAccessible) bands[b]).javaArray();
                this.bandArraysOffsets[b] = ((DirectAccessible) bands[b]).javaArrayOffset();
            }
            this.threadPackedArray = new double[numberOfTasks()][];
            for (int k = 0; k < threadPackedArray.length; k++) {
                this.threadPackedArray[k] = (double[]) DOUBLE_BUFFERS.requestArray();
            }
        }

        static DirectForDoubles getInstance(PArray[] bands, UpdatablePArray packed) {
            switch (bands.length) {
                case 2:
                    return new For2Channels(bands, packed);
                case 3:
                    return new For3Channels(bands, packed);
                case 4:
                    return new For4Channels(bands, packed);
                default:
                    return new DirectForDoubles(bands, packed);
            }
        }

        @Override
        public void close() {
            for (int k = threadPackedArray.length - 1; k >= 0; k--) {
                DOUBLE_BUFFERS.releaseArray(this.threadPackedArray[k]);
            }
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            assert count <= blockSize: "Illegal count in processSubArr";
            final double[] packedArray = threadPackedArray[threadIndex];
            final int step = bands.length;
            for (int b = 0; b < step; b++) {
                final double[] bandArray = bandArrays[b];
                int p = (int) position + bandArraysOffsets[b];
                for (int pTo = p + count, disp = b; p < pTo; p++, disp += step) {
                    packedArray[disp] = bandArray[p];
                }
            }
            packed.setData(position * step, packedArray, 0, count * step);
        }

        static class For2Channels extends DirectForDoubles {
            For2Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 2;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final double[] packedArray = threadPackedArray[threadIndex];
                final double[] band0 = bandArrays[0];
                final double[] band1 = bandArrays[1];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                }
                packed.setData(2 * position, packedArray, 0, disp);
            }
        }

        static class For3Channels extends DirectForDoubles {
            For3Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 3;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final double[] packedArray = threadPackedArray[threadIndex];
                final double[] band0 = bandArrays[0];
                final double[] band1 = bandArrays[1];
                final double[] band2 = bandArrays[2];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                }
                packed.setData(3 * position, packedArray, 0, disp);
            }
        }

        static class For4Channels extends DirectForDoubles {
            For4Channels(PArray[] bands, UpdatablePArray packed) {
                super(bands, packed);
                assert bands.length == 4;
            }

            @Override
            protected void processSubArr(long position, int count, int threadIndex) {
                assert count <= blockSize: "Illegal count in processSubArr";
                final double[] packedArray = threadPackedArray[threadIndex];
                final double[] band0 = bandArrays[0];
                final double[] band1 = bandArrays[1];
                final double[] band2 = bandArrays[2];
                final double[] band3 = bandArrays[3];
                int p0 = (int) position + bandArraysOffsets[0];
                int p1 = (int) position + bandArraysOffsets[1];
                int p2 = (int) position + bandArraysOffsets[2];
                int p3 = (int) position + bandArraysOffsets[3];
                final int p0To = p0 + count;
                int disp = 0;
                for (; p0 < p0To; ) {
                    packedArray[disp++] = band0[p0++];
                    packedArray[disp++] = band1[p1++];
                    packedArray[disp++] = band2[p2++];
                    packedArray[disp++] = band3[p3++];
                }
                packed.setData(4 * position, packedArray, 0, disp);
            }
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}
