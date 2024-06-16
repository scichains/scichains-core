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

package net.algart.executors.modules.core.demo;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;
import net.algart.multimatrix.MultiMatrix;

public final class ExampleMultiMatrixGradients extends MultiMatrixGenerator {
    private int shift = 0;
    private double multiplierForFP = 1.5;

    public ExampleMultiMatrixGradients() {
    }

    public int getShift() {
        return shift;
    }

    public ExampleMultiMatrixGradients setShift(int shift) {
        this.shift = shift;
        return this;
    }

    public double getMultiplierForFP() {
        return multiplierForFP;
    }

    public ExampleMultiMatrixGradients setMultiplierForFP(double multiplierForFP) {
        this.multiplierForFP = multiplierForFP;
        return this;
    }

    @Override
    public MultiMatrix create() {
        return create(getDimX(), getDimY(), getNumberOfChannels());
    }

    public MultiMatrix create(long dimX, long dimY, int numberOfChannels) {
        return MultiMatrix.valueOfMerged(
                Matrices.matrix(makeSamples(dimX, dimY, numberOfChannels),
                        dimX, dimY, numberOfChannels));
    }

    private PArray makeSamples(long dimX, long dimY, int numberOfChannels) {
        if (dimX > Integer.MAX_VALUE || dimY > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Matrix sizes " + dimX + "*" + dimY + " > 2^31-1");
        }
        final int w = (int) dimX;
        final int h = (int) dimY;
        Class<?> elementType = getElementType();
        if (elementType == boolean.class) {
            UpdatableBitArray channels = BitArray.newArray(dimX * dimY * numberOfChannels);
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (long c = cr.c1(); c <= cr.c2(); c++) {
                    final long channelDisp = c * dimX * dimY;
                    for (long x = 0, disp = (long) y * w + channelDisp; x < w; x++, disp++) {
                        channels.setBitNoSync(disp, ((shift + x + y) & 0xFF) >= 128);
                    }
                }
            }
            return channels;
        }
        if (dimX * dimY > Integer.MAX_VALUE / numberOfChannels) {
            throw new TooLargeArrayException("Matrix size " + dimX + "*" + dimY + "*" +
                    numberOfChannels + " > 2^31-1");
        }
        final int matrixSize = w * h;
        if (elementType == byte.class) {
            byte[] channels = new byte[matrixSize * numberOfChannels];
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (int c = cr.c1(); c <= cr.c2(); c++) {
                    for (int x = 0, disp = y * w; x < w; x++, disp++) {
                        channels[disp + c * matrixSize] = (byte) (shift + x + y);
                    }
                }
            }
            return SimpleMemoryModel.asUpdatableByteArray(channels);
        } else if (elementType == short.class) {
            short[] channels = new short[matrixSize * numberOfChannels];
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (int c = cr.c1(); c <= cr.c2(); c++) {
                    for (int x = 0, disp = y * w; x < w; x++, disp++) {
                        channels[disp + c * matrixSize] = (short) (157 * (shift + x + y));
                    }
                }
            }
            return SimpleMemoryModel.asUpdatableShortArray(channels);
        } else if (elementType == int.class) {
            int[] channels = new int[matrixSize * numberOfChannels];
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (int c = cr.c1(); c <= cr.c2(); c++) {
                    for (int x = 0, disp = y * w; x < w; x++, disp++) {
                        channels[disp + c * matrixSize] = 157 * 65536 * (shift + x + y);
                    }
                }

            }
            return SimpleMemoryModel.asUpdatableIntArray(channels);
        } else if (elementType == float.class) {
            float[] channels = new float[matrixSize * numberOfChannels];
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (int c = cr.c1(); c <= cr.c2(); c++) {
                    for (int x = 0, disp = y * w; x < w; x++, disp++) {
                        int v = (shift + x + y) & 0xFF;
                        channels[disp + c * matrixSize] = (float) (0.5 + multiplierForFP * (v / 256.0 - 0.5));
                    }
                }
            }
            return SimpleMemoryModel.asUpdatableFloatArray(channels);
        } else if (elementType == double.class) {
            double[] channels = new double[matrixSize * numberOfChannels];
            for (int y = 0; y < h; y++) {
                final ChannelsRange cr = channelsRange(y, numberOfChannels);
                for (int c = cr.c1(); c <= cr.c2(); c++) {
                    for (int x = 0, disp = y * w; x < w; x++, disp++) {
                        int v = (shift + x + y) & 0xFF;
                        channels[disp + c * matrixSize] = (float) (0.5 + multiplierForFP * (v / 256.0 - 0.5));
                    }
                }
            }
            return SimpleMemoryModel.asUpdatableDoubleArray(channels);
        } else {
            throw new UnsupportedOperationException("Unsupported sampleType = " + elementType);
        }
    }

    private static ChannelsRange channelsRange(int y, int numberOfChannels) {
        int c1 = (y / 32) % (numberOfChannels + 1) - 1;
        int c2 = c1;
        if (c1 == -1) {
            c1 = 0;
            c2 = numberOfChannels - 1;
        }
        return new ChannelsRange(c1, c2);
    }

    private record ChannelsRange(int c1, int c2) {
    }
}
