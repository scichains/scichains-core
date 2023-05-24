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

package net.algart.executors.api.data;

import net.algart.arrays.Arrays;
import net.algart.arrays.DirectAccessible;
import net.algart.arrays.JArrayPool;
import net.algart.arrays.PArray;

abstract class AbstractBandsSequentialProcessor extends Arrays.ParallelExecutor implements AutoCloseable {
    private static final int BUFFER_SIZE = 32768;

    static final JArrayPool BOOLEAN_BUFFERS = JArrayPool.getInstance(boolean.class, BUFFER_SIZE);
    static final JArrayPool CHAR_BUFFERS = JArrayPool.getInstance(char.class, BUFFER_SIZE);
    static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, BUFFER_SIZE);
    static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, BUFFER_SIZE);
    static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, BUFFER_SIZE);
    static final JArrayPool LONG_BUFFERS = JArrayPool.getInstance(long.class, BUFFER_SIZE);
    static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, BUFFER_SIZE);
    static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, BUFFER_SIZE);

    AbstractBandsSequentialProcessor(PArray[] bands, PArray packed) {
        super(
                null,
                null,
                bands[0],
                BUFFER_SIZE / bands.length,
                0,
                0);
        // - note: we pass bands[0] as src argument, though while unpacking it is the destination!
        // We need this only to allow ParallelExecutor to calculate suitable blocks
        assert bands.length <= SMat.MAX_NUMBER_OF_CHANNELS;
        assert blockSize * bands.length <= BUFFER_SIZE;
        assert bands[0].elementType() == packed.elementType();
        assert packed.length() == bands.length * bands[0].length();
    }

    @Override
    public abstract void close();

    @Override
    protected abstract void processSubArr(long position, int count, int threadIndex);

    static boolean allBandsDirect(PArray[] bands) {
        for (PArray band : bands) {
            if (!(band instanceof DirectAccessible && ((DirectAccessible) band).hasJavaArray())) {
                return false;
            }
        }
        return true;
    }
}
