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
import net.algart.external.UsedForExternalCommunication;
import net.algart.io.awt.BufferedImageToMatrix;
import net.algart.io.awt.MatrixToBufferedImage;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.awt.image.BufferedImage;
import java.nio.*;
import java.util.Objects;
import java.util.function.Function;

/**
 * Simple matrix (multi-channel, N-dimensional).
 * If the storage is presented by {@link ConvertibleByteBufferMatrix} pointer,
 * it is supposed that R, G, B, A values are stored as the sequence BGRBGRBGR...
 * or BGRABGRABGRA... of 8-bit, 16-bit or other values (see {@link Depth}).
 *
 * <p>Note: this class <b>does not</b> contain methods, providing access to file system or other unsafe
 * resources. It is important while using inside user-defined scripts.
 */
public final class SMat extends Data {
    static final int MAX_NUMBER_OF_CHANNELS = MultiMatrix.MAX_NUMBER_OF_CHANNELS;
    private static final boolean OPTIMIZE_COPYING = true;
    // - should be true for better performance

    private static final Depth[] CODE_TO_DEPTH = new Depth[512];

    /**
     * Content of the matrix, stored in {@link SMat} object.
     *
     * <p>Note: this object may be not immutable (though the implementations {@link ConvertibleByteBufferMatrix}
     * and {@link ConvertibleMultiMatrix} are supposed to be immutable).
     * However, you <b>must not</b> use the same instance of this class, if you modify its content.
     * If you want to modify the matrix content "in place", you need to "forget" the instance of
     * this class, describing the matrix before modification, and to create a new instance of this class
     * after modification &mdash; because this object use caching to accelerate access to the data.
     * In particular, you may use {@link #copy()} method to create new instance before starting modifications.
     */
    public static abstract class Convertible {
        private volatile ByteBuffer cachedByteBuffer = null;
        private volatile MultiMatrix cachedMultiMatrix = null;

        /**
         * Returns this object, if it is immutable or supposed to be immutable, or a deep clone in other case.
         *
         * @return a copy of this object.
         */
        public abstract Convertible copy();

        /**
         * Returns this object, if it is stored in usual OS RAM, or its RAM copy if it is unsafe
         * for usage in any Java environment, for example, if it is located in GPU memory:
         * see {@link Data#serializeMemory()}.
         * In the second case, this method must automatically free memory (non-standard, like GPU memory),
         * occupied by the current instance, like in {@link #dispose()} method.
         *
         * @return a copy of this object, placed in usual memory and safe for any for of usage inside JVM.
         */
        public abstract Convertible copyToMemoryAndDisposePrevious();

        public abstract ByteBuffer toByteBuffer(SMat thisMatrix);

        public byte[] toByteArray(SMat thisMatrix) {
            final ByteBuffer byteBuffer = toByteBuffer(thisMatrix);
            byte[] result = new byte[byteBuffer.limit()];
            byteBuffer.rewind();
            byteBuffer.get(result);
            return result;
        }


        /**
         * Frees all resources associated with this object. You cannot use this object after disposing,
         * but may call this method several times. (It is necessary, because sometimes we need to make
         * a shallow copy of {@link SMat} object.)
         */
        public abstract void dispose();

        ByteBuffer getCachedByteBuffer(SMat thisMatrix) {
            assert thisMatrix != null;
            ByteBuffer result = this.cachedByteBuffer;
            if (result != null) {
//                System.out.println("!!! use cached " + result);
                return result;
            } else {
                return this.cachedByteBuffer = toByteBuffer(thisMatrix);
            }
        }

        MultiMatrix getCachedMultiMatrix(
                SMat thisMat,
                boolean autoConvertUnsupportedDepth,
                ChannelOrder channelOrder) {
            assert thisMat != null;
            assert thisMat.isInitialized() : "getCachedMultiMatrix must not be called for non-initialized " + thisMat;
            MultiMatrix result = cachedMultiMatrix;
            final boolean doCache =
                    OPTIMIZE_COPYING && channelOrder == ChannelOrder.STANDARD && thisMat.depth.isAlgARTCompatible();
            // - if depth is incompatible with AlgART (rare case), don't try to optimize:
            // result should depend on autoConvertUnsupportedDepth argument
            if (doCache && result != null) {
//                System.out.println("!!! use cached " + cachedMultiMatrix);
                return result;
            }
            final Matrix<? extends PArray> m = thisMat.toInterleavedMatrix(autoConvertUnsupportedDepth);
            assert m != null : "toInterleavedMatrix cannot be null for initialized SMat";
            if (m.dim(0) == 1) {
                Matrix<? extends PArray> matrix = m.array().matrix(removeFirstElement(m.dimensions()));
                if (!SimpleMemoryModel.isSimpleArray(matrix.array())) {
                    matrix = matrix.clone();
                }
                result = MultiMatrix.valueOfMono(matrix);
            } else {
                var channels = Matrices.separate(null, m, MultiMatrix.MAX_NUMBER_OF_CHANNELS);
                result = channelOrder == ChannelOrder.ORDER_IN_PACKED_BYTE_BUFFER ?
                        MultiMatrix.valueOf(channels) :
                        MultiMatrix.valueOfBGRA(channels);
            }
            if (doCache) {
                cachedMultiMatrix = result;
            }
            return result;
        }
    }

    public enum ChannelOrder {
        /**
         * Channels order, corresponding to channels packed into ByteBuffer.
         * In current version, it is BGR/BGRA for color images.
         */
        ORDER_IN_PACKED_BYTE_BUFFER,
        /**
         * Standard channels order, compatible with {@link MultiMatrix}. For color images, it is RGB/RGBA.
         */
        STANDARD
    }

    /**
     * depth of matrix (byteBuffer) elements
     */
    public enum Depth {
        U8(0, byte.class, 8, false, bb -> bb),
        S8(1, byte.class, 8, false, bb -> bb),
        // - incompatible with AlgART
        U16(2, short.class, 16, false, ByteBuffer::asShortBuffer),
        S16(3, short.class, 16, false, ByteBuffer::asShortBuffer),
        // - incompatible with AlgART
        S32(4, int.class, 32, false, ByteBuffer::asIntBuffer),
        F32(5, float.class, 32, true, ByteBuffer::asFloatBuffer),
        F64(6, double.class, 64, true, ByteBuffer::asDoubleBuffer),
        BIT(101, boolean.class, 1, false, bb -> bb);
        // - incompatible with OpenCV

        private final int code;
        private final Class<?> elementType;
        private final int bitsPerElement;
        private final boolean floatingPoint;
        private final Function<ByteBuffer, Buffer> converter;

        Depth(
                int code,
                Class<?> elementType,
                int bitsPerElement,
                boolean floatingPoint,
                Function<ByteBuffer, Buffer> converter) {
            this.code = code;
            this.elementType = elementType;
            this.bitsPerElement = bitsPerElement;
            this.floatingPoint = floatingPoint;
            this.converter = converter;
            CODE_TO_DEPTH[code] = this;
        }

        public int code() {
            return code;
        }

        public Class<?> elementType() {
            return elementType;
        }

        public Class<?> elementType(boolean requireAlgARTCompatibility) {
            if (requireAlgARTCompatibility && !isAlgARTCompatible()) {
                throw new IllegalStateException("Signed types less than 32-bit are not supported in AlgART"
                        + " (" + this + ")");
            }
            return elementType;
        }

        public boolean isAlgARTCompatible() {
            return bitsPerElement() > 16 || isUnsigned();
        }

        public boolean isOpenCVCompatible() {
            return this != BIT;
        }

        public int bitsPerElement() {
            return bitsPerElement;
        }

        public boolean isUnsigned() {
            return this == BIT || this == U8 || this == U16;
        }

        public boolean isFloatingPoint() {
            return floatingPoint;
        }

        public Buffer asBuffer(ByteBuffer byteBuffer) {
            Objects.requireNonNull(byteBuffer, "Null byteBuffer");
            return converter.apply(byteBuffer);
        }

        @Override
        public String toString() {
            return "Depth " + name() + " [code " + code + "]: " + bitsPerElement + " bits"
                    + (isUnsigned() ? " (unsigned)" : "")
                    + (floatingPoint ? " (float)" : "");
        }

        public static Depth valueOf(int code) {
            final Depth result;
            if (code < 0 || code >= CODE_TO_DEPTH.length || (result = CODE_TO_DEPTH[code]) == null) {
                throw new IllegalArgumentException("Unsupported depth code: " + code);
            }
            return result;
        }

        public static Depth valueOf(Class<?> elementType) {
            return valueOf(
                    elementType,
                    elementType == byte.class || elementType == short.class);
        }

        public static Depth valueOf(Class<?> elementType, boolean unsigned) {
            Objects.requireNonNull(elementType, "Null elementType");
            if (elementType == boolean.class) {
                return BIT;
            } else if (elementType == byte.class) {
                return unsigned ? U8 : S8;
            } else if (elementType == short.class) {
                return unsigned ? U16 : S16;
            } else {
                if (unsigned) {
                    throw new IllegalArgumentException("Only 8-bit and 16-bit unsigned integers supported");
                }
                if (elementType == int.class) {
                    return S32;
                } else if (elementType == float.class) {
                    return F32;
                } else if (elementType == double.class) {
                    return F64;
                } else {
                    throw new IllegalArgumentException("Unsupported element type " + elementType);
                }
            }
        }
    }

    private long[] dimensions = new long[2];
    // - cannot be null or long[0]
    private Depth depth;
    private int numberOfChannels;
    private Convertible pointer;

    @UsedForExternalCommunication
    public SMat() {
    }

    @UsedForExternalCommunication
    public long[] getDimensions() {
        return this.dimensions.clone();
    }

    @UsedForExternalCommunication
    public int getDimCount() {
        return this.dimensions.length;
    }

    @UsedForExternalCommunication
    public long getDim(int k) {
        return k < this.dimensions.length ? this.dimensions[k] : 1;
    }

    @UsedForExternalCommunication
    public long getDimX() {
        return dimensions[0];
    }

    @UsedForExternalCommunication
    public long getDimY() {
        if (dimensions.length <= 1) {
            return 1;
        }
        return dimensions[1];
    }

    public Depth getDepth() {
        return depth;
    }

    @UsedForExternalCommunication
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    @UsedForExternalCommunication
    public int getDepthCode() {
        return depth.code;
    }

    /**
     * Returns content of this matrix.
     *
     * @return content of this matrix.
     */
    public Convertible getPointer() {
        return pointer;
    }

    /**
     * Returns content of this matrix in a form of <code>ByteBuffer</code>.
     *
     * <p>Note: it is considered to be <b>immutable</b>, and you <b>must not write anything to this buffer</b>.
     *
     * @return content of this matrix.
     */
    @UsedForExternalCommunication
    public ByteBuffer getByteBuffer() {
        return pointer.getCachedByteBuffer(this);
    }

    public Buffer getBuffer() {
        return depth.asBuffer(getByteBuffer());
    }

    public SMat setAll(
            long[] dimensions,
            Depth depth,
            int numberOfChannels,
            Convertible pointer) {
        Objects.requireNonNull(depth, "Null depth");
        checkNumberOfChannels(numberOfChannels);
        Objects.requireNonNull(pointer, "Null pointer");
        this.setDimensions(dimensions);
        this.depth = depth;
        this.numberOfChannels = numberOfChannels;
        this.pointer = pointer;
        this.setInitializedAndResetFlags(true);
        return this;
    }

    // Usually cloneByteBuffer must be true: it is too dangerous to share ByteBuffer from unknown sources,
    // like OpenCV Mat (that can be deallocated in native code)
    public SMat setAll(
            long[] dimensions,
            Depth depth,
            int numberOfChannels,
            ByteBuffer byteBuffer,
            boolean cloneByteBuffer) {
        return setAll(
                dimensions,
                depth,
                numberOfChannels,
                new ConvertibleByteBufferMatrix(cloneByteBuffer ? cloneByteBuffer(byteBuffer) : byteBuffer));
    }

    public boolean isChannelsOrderCompatibleWithMultiMatrix() {
        return !(numberOfChannels == 3 || numberOfChannels == 4);
    }

    public SMat setTo(SMat mat) {
        return setTo(mat, true);
    }

    public SMat setTo(SMat mat, boolean cloneData) {
        Objects.requireNonNull(mat, "Null mat");
        this.flags = mat.flags;
        setInitialized(mat.isInitialized());
        // Note: we does not use setXxx methods to allow copy also incorrect SMat (for example, not initialized)
        this.dimensions = mat.dimensions.clone();
        this.depth = mat.depth;
        this.numberOfChannels = mat.numberOfChannels;
        this.pointer = mat.pointer != null && cloneData ? mat.pointer.copy() : mat.pointer;
        return this;
    }

    public SMat setTo(BufferedImage bufferedImage) {
        return setToInterleavedMatrix(bufferedImageToInterleavedBGRA(bufferedImage));
    }

    public SMat setTo(MultiMatrix multiMatrix) {
        return setTo(multiMatrix, ChannelOrder.STANDARD);
    }

    public SMat setTo(MultiMatrix multiMatrix, ChannelOrder channelOrder) {
        Objects.requireNonNull(multiMatrix, "Null multi-matrix");
        Objects.requireNonNull(channelOrder, "Null channelOrder");
        if (OPTIMIZE_COPYING && channelOrder == ChannelOrder.STANDARD) {
            // - don't try to optimize non-standard order: it is used very rarely
            setNumberOfChannels(multiMatrix.numberOfChannels());
            setDimensions(multiMatrix.dimensions());
            setDepth(SMat.Depth.valueOf(multiMatrix.elementType()));
            setPointer(new ConvertibleMultiMatrix(multiMatrix));
            setInitializedAndResetFlags(true);
            return this;
        }
        Matrix<PArray> interleave = Matrices.interleave(
                ArrayContext.getSimpleContext(BufferMemoryModel.getInstance(), false),
                channelOrder == ChannelOrder.ORDER_IN_PACKED_BYTE_BUFFER ?
                        multiMatrix.allChannels() :
                        multiMatrix.allChannelsInBGRAOrder());
        return setToInterleavedMatrix(interleave);
    }

    /**
     * Loads data from AlgART matrix in the same elements order. The first dimension <code>dim(0)</code>
     * is the number of channels.
     *
     * @param interleavedChannels interleaved matrix; for color matrices, the order must be the same
     *                            as in {@link #getByteBuffer()} (BGR/BGRA for this class).
     * @return the reference to this objects.
     */
    private SMat setToInterleavedMatrix(Matrix<? extends PArray> interleavedChannels) {
        Objects.requireNonNull(interleavedChannels, "Null BGR[A] matrix");
        final int dimCount = interleavedChannels.dimCount();
        if (dimCount < 2) {
            throw new IllegalArgumentException("Interleaved BGR[A] matrix cannot be 1-dimensional: " + interleavedChannels
                    + " (the 1st dimension is used to store channels)");
        }
        final long numberOfChannels = interleavedChannels.dim(0);
        if (numberOfChannels > MAX_NUMBER_OF_CHANNELS) {
            throw new IllegalArgumentException("Number of channels cannot be >"
                    + MAX_NUMBER_OF_CHANNELS + ": " + interleavedChannels);
        }
        Array array = interleavedChannels.array();
        if (!(BufferMemoryModel.isBufferArray(array) && BufferMemoryModel.getBufferOffset(array) == 0)) {
            // Important: if offset != 0, it is a subarray, and we must create its copy before storing in SMat!
            array = array.updatableClone(BufferMemoryModel.getInstance());
        }
        assert BufferMemoryModel.isBufferArray(array);
        setNumberOfChannels((int) numberOfChannels);
        setDimensions(removeFirstElement(interleavedChannels.dimensions()));
        setDepth(SMat.Depth.valueOf(interleavedChannels.elementType()));
        setByteBuffer(BufferMemoryModel.getByteBuffer(array));
        setInitializedAndResetFlags(true);
//        System.out.println("Returning data: " + interleavedChannels.array() + ": "
//            + Arrays.toString(interleavedChannels.array(),",",1000));
//        System.out.println("Returning bytes: " + byteBuffer.order() + ": " + Arrays.toHexString(
//            BufferMemoryModel.asUpdatableByteArray(byteBuffer), ",", 1000));
        return this;
    }

    @Override
    public void setTo(Data other, boolean cloneData) {
        if (!(other instanceof SMat)) {
            throw new IllegalArgumentException("Cannot assign " + other.getClass() + " to " + getClass());
        }
        setTo((SMat) other, cloneData);
    }

    @Override
    public SMat exchange(Data other) {
        Objects.requireNonNull(other, "Null other objects");
        if (!(other instanceof SMat otherMat)) {
            throw new IllegalArgumentException("Cannot exchange with another data type: " + other.getClass());
        }
        final long tempFlags = this.flags;
        final long[] tempDimensions = this.dimensions;
        final Depth tempDepth = this.depth;
        final int tempNumberOfChannels = this.numberOfChannels;
        final Convertible tempPointer = this.pointer;
        this.flags = otherMat.flags;
        this.dimensions = otherMat.dimensions;
        this.depth = otherMat.depth;
        this.numberOfChannels = otherMat.numberOfChannels;
        this.pointer = otherMat.pointer;
        otherMat.flags = tempFlags;
        otherMat.dimensions = tempDimensions;
        otherMat.depth = tempDepth;
        otherMat.numberOfChannels = tempNumberOfChannels;
        otherMat.pointer = tempPointer;
        return this;
    }

    @Override
    public void serializeMemory() {
        if (isInitialized()) {
            this.pointer = this.pointer.copyToMemoryAndDisposePrevious();
        }
    }

    public BufferedImage toBufferedImage() {
        if (!isInitialized()) {
            return null;
        }
        if (numberOfChannels != 1 && numberOfChannels != 3 && numberOfChannels != 4) {
            throw new IllegalStateException("Cannot convert " + numberOfChannels + "-channel matrix to BufferedImage ("
                    + this + "): number of channels must be 1, 3 or 4, but it is " + numberOfChannels);
        }
        if (dimensions.length != 2) {
            throw new IllegalStateException("Cannot convert " + dimensions.length + "D matrix to BufferedImage ("
                    + this + "): only 2-dimensional matrices can be converted");
        }
        return interleavedBGRAToBufferedImage(toInterleavedMatrix(false));
    }

    public MultiMatrix2D toMultiMatrix2D() {
        return toMultiMatrix2D(false);
    }

    public MultiMatrix2D toMultiMatrix2D(ChannelOrder channelOrder) {
        return toMultiMatrix2D(false, channelOrder);
    }

    public MultiMatrix2D toMultiMatrix2D(boolean autoConvertUnsupportedDepth) {
        return toMultiMatrix2D(autoConvertUnsupportedDepth, ChannelOrder.STANDARD);
    }

    public MultiMatrix2D toMultiMatrix2D(boolean autoConvertUnsupportedDepth, ChannelOrder channelOrder) {
        final MultiMatrix result = toMultiMatrix(autoConvertUnsupportedDepth, channelOrder);
        return result == null ? null : result.asMultiMatrix2D();
    }

    public MultiMatrix toMultiMatrix() {
        return toMultiMatrix(false);
    }

    public MultiMatrix toMultiMatrix(ChannelOrder channelOrder) {
        return toMultiMatrix(false, channelOrder);
    }

    public MultiMatrix toMultiMatrix(boolean autoConvertUnsupportedDepth) {
        return toMultiMatrix(autoConvertUnsupportedDepth, ChannelOrder.STANDARD);
    }

    public MultiMatrix toMultiMatrix(boolean autoConvertUnsupportedDepth, ChannelOrder channelOrder) {
        Objects.requireNonNull(channelOrder, "Null channelOrder");
        if (!isInitialized()) {
            return null;
        }
        return pointer.getCachedMultiMatrix(this, autoConvertUnsupportedDepth, channelOrder);
    }

    /**
     * Return data as AlgART matrix with the same elements order. AlgART matrix will be (n+1)-dimensional
     * (n = {@link #getDimCount()}); <code>dim(0)</code> is the number of channels.
     *
     * @return interleaved 3D AlgART matrix; for color matrices, the order will be the same
     * as in {@link #getByteBuffer()} (BGR/BGRA for this class).
     */
    public Matrix<? extends PArray> toInterleavedMatrix(boolean autoConvertUnsupportedDepth) {
        if (!isInitialized()) {
            return null;
        }
        final long[] newDimensions = addFirstElement(numberOfChannels, getDimensions());
        final long size = Arrays.longMul(newDimensions);
        if (size == Long.MIN_VALUE) {
            throw new TooLargeArrayException("Too large dimensions: dim[0] * dim[1] * ... > Long.MAX_VALUE");
        }
        if (depth == Depth.BIT) {
            ByteBuffer byteBuffer = getByteBuffer();
            //        final int byteCount = (int) ((bitArraySize + 7L) / 8);
//        long[] bits = new long[(int) (((long) byteCount + 7) / 8)];
//        ByteBuffer bb = byteBuffer.duplicate().order(byteBuffer.order());
//        bb.rewind();
//        LongBuffer lb = bb.asLongBuffer();
//        final int wholeLongCount = byteCount / 8;
//        lb.get(bits, 0, wholeLongCount);
//        if (wholeLongCount < bits.length) {
//            ByteBuffer eightBytes = ByteBuffer.allocate(8).order(bb.order());
//            bb.position(wholeLongCount * 8);
//            eightBytes.put(bb);
//            eightBytes.rewind();
//            lb = eightBytes.asLongBuffer();
//            lb.get(bits, wholeLongCount, 1);
//        }
//        final UpdatableBitArray result = Arrays.SMM.newUnresizableBitArray(bitArraySize);
//        result.setBits(0, bits, 0, bitArraySize);
//        return result;
            return ((BitArray) BitArray.as(PackedBitArraysPer8.toLongArray(byteBuffer), size)).matrix(newDimensions);
        } else {
            ByteBuffer bb = getByteBuffer();
            Class<?> elementType = depth.elementType(!autoConvertUnsupportedDepth);
            if (autoConvertUnsupportedDepth && !depth.isAlgARTCompatible()) {
                bb = toByteBufferF32(bb, depth);
                elementType = float.class;
            }
            final UpdatablePArray array = (UpdatablePArray) BufferMemoryModel.asUpdatableArray(bb, elementType);
//        System.out.println("Reading bytes: " + bb.order() + ": " + Arrays.toHexString(
//            BufferMemoryModel.asUpdatableByteArray(bb), ",", 1000));
//        System.out.println("Reading data: " + getDepth() +"," + array
//            + ": " + Arrays.toString(array,",",1000));
            return array.matrix(newDimensions);
        }
    }

    public SMat autoContrast() {
        if (depth == SMat.Depth.BIT || numberOfChannels == 2 || numberOfChannels > 4) {
            return this;
        } else {
            final MultiMatrix matrix = toMultiMatrix(true);
            return matrix == null ? this : valueOf(matrix.contrast());
            // matrix == null if not initialized
        }
    }

    public boolean dimEquals(SMat m) {
        Objects.requireNonNull(m, "Null matrix");
        return java.util.Arrays.equals(dimensions, m.dimensions);
    }

    @Override
    public DataType type() {
        return DataType.MAT;
    }

    @Override
    public String toString() {
        if (!isInitialized()) {
            return super.toString();
        } else {
            return super.toString() + " " + numberOfChannels + " channels, "
                    + (dimensions.length == 1 ?
                    dimensions[0] + "(x1)" :
                    JArrays.toString(dimensions, "x", 1000)) + ", "
                    + depth + "; data: " + pointer;
        }
    }

    public static SMat valueOf(
            long dimX,
            long dimY,
            Depth depth,
            int numberOfChannels,
            ByteBuffer byteBuffer) {
        return valueOf(new long[]{dimX, dimY}, depth, numberOfChannels, byteBuffer);
    }

    public static SMat valueOf(
            long[] dimensions,
            Depth depth,
            int numberOfChannels,
            ByteBuffer byteBuffer) {
        return new SMat().setAll(dimensions, depth, numberOfChannels, byteBuffer, true);
    }

    public static SMat valueOf(BufferedImage bufferedImage) {
        return new SMat().setTo(bufferedImage);
    }

    public static SMat valueOf(MultiMatrix multiMatrix) {
        return new SMat().setTo(multiMatrix);
    }

    public static SMat valueOf(MultiMatrix multiMatrix, ChannelOrder channelOrder) {
        return new SMat().setTo(multiMatrix, channelOrder);
    }

    public static SMat valueOfInterleavedMatrix(Matrix<? extends PArray> interleavedChannels) {
        return new SMat().setToInterleavedMatrix(interleavedChannels);
    }

    public static ByteBuffer cloneByteBuffer(ByteBuffer byteBuffer) {
        return cloneByteBuffer(byteBuffer, true);
        // - preferred choice for SMat is direct byte buffer
    }

    public static ByteBuffer cloneByteBuffer(ByteBuffer byteBuffer, boolean directByteBuffer) {
        final ByteOrder byteOrder = byteBuffer.order();
        byteBuffer = byteBuffer.duplicate();
        // - note: byteOrder may be changed here, we need to read if before!
        final ByteBuffer result = directByteBuffer ?
                ByteBuffer.allocateDirect(byteBuffer.capacity()) :
                ByteBuffer.allocate(byteBuffer.capacity());
        result.order(byteOrder);
        byteBuffer.rewind();
        result.put(byteBuffer);
        result.rewind();
        return result;
    }

    public static Matrix<? extends PArray> bufferedImageToInterleavedBGRA(BufferedImage bufferedImage) {
        return new BufferedImageToMatrix.ToInterleavedBGR().toMatrix(bufferedImage);
    }

    public static BufferedImage interleavedBGRAToBufferedImage(Matrix<? extends PArray> interleavedBGRA) {
        return new MatrixToBufferedImage.InterleavedBGRToInterleaved().toBufferedImage(interleavedBGRA);
    }

    @Override
    protected void freeResources() {
        if (this.pointer != null) {
            pointer.dispose();
            this.pointer = null;
        }
        this.dimensions = new long[2]; // - zero-filled by Java
        this.depth = null;
        this.numberOfChannels = 0;
    }

    @UsedForExternalCommunication
    private void setDimensions(long... dimensions) {
        Objects.requireNonNull(dimensions, "Null dimensions array");
        if (dimensions.length == 0) {
            throw new IllegalArgumentException("Empty dimensions Java array");
        }
        dimensions = dimensions.clone();
        for (int k = 0; k < dimensions.length; k++) {
            checkDimension(k, dimensions[k]);
        }
        this.dimensions = dimensions;
    }

    @UsedForExternalCommunication
    private void setDim(int k, long dimension) {
        checkDimension(k, dimension);
        if (k >= dimensions.length) {
            throw new IndexOutOfBoundsException("Dimension index "
                    + k + " >= number of dimension " + dimensions.length);
        }
        dimensions[k] = dimension;
    }

    private void setDepth(Depth depth) {
        this.depth = Objects.requireNonNull(depth, "Null depth");
    }

    /**
     * invoked from native code
     *
     * @param depth byteBuffer depth
     */
    @UsedForExternalCommunication
    private void setDepthCode(int depth) {
        this.depth = Depth.valueOf(depth);
    }

    @UsedForExternalCommunication
    private void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = checkNumberOfChannels(numberOfChannels);
    }

    @UsedForExternalCommunication
    private void setByteBuffer(ByteBuffer byteBuffer) {
        setPointer(new ConvertibleByteBufferMatrix(byteBuffer));
    }

    @UsedForExternalCommunication
    private void setMatrix(long[] dimensions, int numberOfChannels, int depthCode, ByteBuffer byteBuffer) {
        setDimensions(dimensions);
        setNumberOfChannels(numberOfChannels);
        setDepthCode(depthCode);
        setByteBuffer(byteBuffer);
    }

    @UsedForExternalCommunication
    private void setMatrix(long dimX, long dimY, int numberOfChannels, int depthCode, ByteBuffer byteBuffer) {
        setDimensions(dimX, dimY);
        setNumberOfChannels(numberOfChannels);
        setDepthCode(depthCode);
        setByteBuffer(byteBuffer);
    }

    private void setPointer(Convertible pointer) {
//        System.out.println("Setting pointer: " + pointer);
        Objects.requireNonNull(pointer, "Null pointer");
        this.pointer = pointer;
    }

    static long[] addFirstElement(long newFirstElement, long[] values) {
        final long[] result = new long[values.length + 1];
        System.arraycopy(values, 0, result, 1, values.length);
        result[0] = newFirstElement;
        return result;
    }

    static long[] removeFirstElement(long[] values) {
        return java.util.Arrays.copyOfRange(values, 1, values.length);
    }

    private static long checkDimension(int k, long dimension) {
        if (k < 0) {
            throw new IllegalArgumentException("Negative dimension index " + k);
        }
        // OpenCV can create Mat with zero sizes, but work with it is not comfortable: data will be null.
        if (dimension <= 0) {
            throw new IllegalArgumentException("Zero or negative dim[" + k + "] = " + dimension);
        }
        return dimension;
    }

    private static int checkNumberOfChannels(int numberOfChannels) {
        if (numberOfChannels <= 0 || numberOfChannels > MAX_NUMBER_OF_CHANNELS) {
            throw new IllegalArgumentException("Number of channels " + numberOfChannels + " not in 1.."
                    + MAX_NUMBER_OF_CHANNELS + " range");
        }
        return numberOfChannels;
    }

    private static ByteBuffer toByteBufferF32(ByteBuffer source, Depth sourceDepth) {
        assert sourceDepth == Depth.S8 || sourceDepth == Depth.S16;
        source = source.duplicate().order(source.order());
        source.rewind();
        final int limit = source.limit();
        final int length = sourceDepth == Depth.S8 ? limit : limit / 2;
        final long newLimit = 4 * (long) length;
        if (newLimit > Integer.MAX_VALUE) {
            // overflow
            throw new TooLargeArrayException("Cannot convert " + length + " byte/short to int values:"
                    + " the result will be greater than 2^31-1 bytes");
        }
        final ByteBuffer result = ByteBuffer.allocateDirect((int) newLimit);
        final FloatBuffer resultBuffer = result.asFloatBuffer();
        switch (sourceDepth) {
            case S8: {
                for (int k = 0; k < length; k++) {
                    float v = (float) source.get();
                    resultBuffer.put(v);
                }
                break;
            }
            case S16:
                final ShortBuffer sourceBuffer = source.asShortBuffer();
                for (int k = 0; k < length; k++) {
                    float v = (float) sourceBuffer.get();
                    resultBuffer.put(v);
                }
                break;
        }
        return result;
    }
}
