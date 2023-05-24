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

package net.algart.bridges.jep.api;

import jep.DirectNDArray;
import jep.NDArray;
import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.data.SMat;

import java.nio.*;
import java.util.Objects;

public class Jep2SMat {
    private Jep2SMat() {
    }

    public static DirectNDArray<Buffer> toNDArray(SMat matrix) {
        Objects.requireNonNull(matrix, "Null matrix");
        if (!matrix.isInitialized()) {
            throw new IllegalArgumentException("Not initialized matrix");
        }
        if (!matrix.getDepth().isOpenCVCompatible()) {
            throw new IllegalArgumentException("Matrix element type is not supported: " + matrix);
        }
        final int dimCount = matrix.getDimCount();
        final int[] ndDimensions = new int[dimCount + 1];
        for (int k = 0; k < dimCount; k++) {
            final long dim = matrix.getDim(dimCount - 1 - k);
            if (dim > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large matrix: dimension #" + (dimCount - 1 - k) + " >= 2^31");
            }
            ndDimensions[k] = (int) dim;
        }
        ndDimensions[dimCount] = matrix.getNumberOfChannels();
        final SMat.Depth depth = matrix.getDepth();
        final ByteBuffer byteBuffer = SMat.cloneByteBuffer(matrix.getByteBuffer());
        return new DirectNDArray<>(depth.asBuffer(byteBuffer), depth.isUnsigned(), ndDimensions);
    }

    public static SMat toSMat(Object ndArray) {
        return setToArray(new SMat(), ndArray);
    }

    public static SMat setToArray(SMat result, Object array) {
        Objects.requireNonNull(array, "Null array");
        if (array instanceof NDArray<?>) {
            return setTo(result, (NDArray<?>) array);
        } else if (array instanceof DirectNDArray<?>) {
            return setTo(result, (DirectNDArray<?>) array);
        } else {
            throw new UnsupportedOperationException("Unsupported type of array: "
                    + array.getClass().getCanonicalName() + " (NDArray/DirectNDArray expected)");
        }
    }

    public static SMat setTo(SMat result, DirectNDArray<?> ndArray) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(ndArray, "Null ndArray");
        setTo(result, ndArray.getData(), ndArray.isUnsigned(), ndArray.getDimensions());
        return result;
    }

    public static SMat setTo(SMat result, NDArray<?> ndArray) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(ndArray, "Null ndArray");
        final Buffer buffer;
        final Object data = ndArray.getData();
        if (data instanceof byte[]) {
            buffer = ByteBuffer.wrap((byte[]) data);
        } else if (data instanceof short[]) {
            buffer = ShortBuffer.wrap((short[]) data);
        } else if (data instanceof int[]) {
            buffer = IntBuffer.wrap((int[]) data);
        } else if (data instanceof long[]) {
            buffer = LongBuffer.wrap((long[]) data);
        } else if (data instanceof float[]) {
            buffer = FloatBuffer.wrap((float[]) data);
        } else if (data instanceof double[]) {
            buffer = DoubleBuffer.wrap((double[]) data);
        } else {
            throw new IllegalArgumentException("Unsupported type of NDArray data: " + data.getClass().getSimpleName());
            // - should not occur: see NDArray.validate()
        }
        setTo(result, buffer, ndArray.isUnsigned(), ndArray.getDimensions());
        return result;
    }

    private static void setTo(SMat result, Buffer buffer, boolean unsigned, int[] ndDimensions) {
        final SMat.Depth depth;
        if (buffer instanceof ByteBuffer) {
            depth = unsigned ? SMat.Depth.U8 : SMat.Depth.S8;
        } else if (buffer instanceof ShortBuffer) {
            depth = unsigned ? SMat.Depth.U16 : SMat.Depth.S16;
        } else if (buffer instanceof IntBuffer) {
            depth = SMat.Depth.S32;
        } else if (buffer instanceof FloatBuffer) {
            depth = SMat.Depth.F32;
        } else if (buffer instanceof DoubleBuffer) {
            depth = SMat.Depth.F64;
        } else {
            throw new IllegalArgumentException("Unsupported element type of NDArray data: buffer "
                    + buffer.getClass().getSimpleName());
            // - theoretically possible for LongBuffer
        }
        final int numberOfChannels;
        final long[] dimensions;
        if (ndDimensions.length == 2 || (ndDimensions.length == 3 && ndDimensions[2] == 1)) {
            // - probably grayscale image
            numberOfChannels = 1;
            dimensions = new long[2];
        } else if (ndDimensions.length == 3 && ndDimensions[2] <= 4) {
            // - probably color image (with or wirhout alpha)
            numberOfChannels = ndDimensions[2];
            dimensions = new long[2];
        } else {
            numberOfChannels = 1;
            dimensions = new long[ndDimensions.length];
        }
        final int bytesPerElement = depth.bitsPerElement() / 8;
        final long maxSize = Integer.MAX_VALUE / bytesPerElement;
        long size = numberOfChannels;
        for (int k = 0; k < dimensions.length; k++) {
            final int dim = ndDimensions[dimensions.length - 1 - k];
            size *= dim;
            if (size > maxSize) {
                throw new TooLargeArrayException("Too large matrix: > " + maxSize + " "
                        + depth.elementType().getSimpleName() + " elements");
                // - theoretically possible for very large non-byte Java array
            }
            dimensions[k] = dim;
        }
        final ByteBuffer resultBuffer = ByteBuffer.allocateDirect((int) (size * bytesPerElement));
        resultBuffer.order(ByteOrder.nativeOrder());
        resultBuffer.rewind();
        buffer = buffer.duplicate();
        buffer.rewind();
        if (buffer instanceof ByteBuffer) {
            resultBuffer.put((ByteBuffer) buffer);
        } else if (buffer instanceof ShortBuffer) {
            resultBuffer.asShortBuffer().put((ShortBuffer) buffer);
        } else if (buffer instanceof IntBuffer) {
            resultBuffer.asIntBuffer().put((IntBuffer) buffer);
        } else if (buffer instanceof FloatBuffer) {
            resultBuffer.asFloatBuffer().put((FloatBuffer) buffer);
        } else if (buffer instanceof DoubleBuffer) {
            resultBuffer.asDoubleBuffer().put((DoubleBuffer) buffer);
        } else {
            throw new AssertionError("Was already checked little above!");
        }
        result.setAll(dimensions, depth, numberOfChannels, resultBuffer, false);
    }

//    public static void main(String[] args) {
//        System.out.println(toSMat(new NDArray<>(new long[2])));
//        System.out.println(toSMat(new NDArray<>(new int[2_000_000_000])));
//    }
}
