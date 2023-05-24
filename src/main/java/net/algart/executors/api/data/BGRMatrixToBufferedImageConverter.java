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

import net.algart.arrays.ByteArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.TooLargeArrayException;
import net.algart.external.MatrixToBufferedImageConverter;

import java.awt.color.ColorSpace;
import java.awt.image.*;

//TODO!! implement this to AlgART libraries
class BGRMatrixToBufferedImageConverter extends MatrixToBufferedImageConverter.Packed3DToPackedRGB {
    static final BGRMatrixToBufferedImageConverter INSTANCE = new BGRMatrixToBufferedImageConverter();

    private BGRMatrixToBufferedImageConverter() {
        super(false);
    }

    public final BufferedImage toBufferedImageCorrected(Matrix<? extends PArray> packedMatrix) {
        // avoiding problem with monochrome image
        checkMatrix(packedMatrix);
        java.awt.image.DataBuffer dataBuffer = toDataBuffer(packedMatrix);
        final int dimX = getWidth(packedMatrix);
        final int dimY = getHeight(packedMatrix);
        final int bandCount = getBandCount(packedMatrix);
        int[] bandMasks = rgbAlphaMasks(bandCount);
        if (bandMasks != null) {
            WritableRaster wr = Raster.createPackedRaster(dataBuffer, dimX, dimY, dimX, bandMasks, null);
            DirectColorModel cm = bandMasks.length > 3 ?
                    new DirectColorModel(32, bandMasks[0], bandMasks[1], bandMasks[2], bandMasks[3]) :
                    new DirectColorModel(24, bandMasks[0], bandMasks[1], bandMasks[2], 0);
            return new BufferedImage(cm, wr, false, null);
        }
        byte[][] palette = palette();
        if (palette != null) {
            if (palette.length < 3)
                throw new AssertionError("palette() method must return palette with 3 or 4 bands");
            IndexColorModel cm = palette.length == 3 ?
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2]) :
                    new IndexColorModel(Byte.SIZE, 256, palette[0], palette[1], palette[2], palette[3]);
            WritableRaster wr = Raster.createInterleavedRaster(dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null);
            return new BufferedImage(cm, wr, false, null);
        }
        int[] indexes = new int[dataBuffer.getNumBanks()];
        int[] offsets = new int[dataBuffer.getNumBanks()];
        for (int k = 0; k < indexes.length; k++) {
            indexes[k] = k;
            offsets[k] = 0;
        }
        WritableRaster wr = indexes.length == 1 ?
                Raster.createInterleavedRaster(dataBuffer, dimX, dimY, dimX, 1, new int[]{0}, null) :
                // - important! in other case,
                // createGraphics().drawXxx method will use incorrect (translated) intensity
                Raster.createBandedRaster(dataBuffer, dimX, dimY, dimX, indexes, offsets, null);
        ColorSpace cs = ColorSpace.getInstance(indexes.length == 1 ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        boolean hasAlpha = indexes.length > 3;
        ComponentColorModel cm = new ComponentColorModel(cs, null,
                hasAlpha, false, hasAlpha ? ColorModel.TRANSLUCENT : ColorModel.OPAQUE, dataBuffer.getDataType());
        return new BufferedImage(cm, wr, false, null);
    }

    @Override
    protected int[] rgbAlphaMasks(int bandCount) {
        if (bandCount == 4 || bandCount == 2) {
            return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000};
        } else if (bandCount == 3) {
            return new int[]{0x000000ff, 0x0000ff00, 0x00ff0000};
        } else {
            return null;
        }
    }

    private void checkMatrix(Matrix<? extends PArray> packedMatrix) {
        if (packedMatrix == null)
            throw new NullPointerException("Null packed matrix");
        if (packedMatrix.dimCount() != 3 && packedMatrix.dimCount() != 2)
            throw new IllegalArgumentException("Packed matrix must be 2- or 3-dimensional");
        long bandCount = packedMatrix.dimCount() == 2 ? 1 : packedMatrix.dim(0);
        if (bandCount < 1 || bandCount > 4)
            throw new IllegalArgumentException("The number of color channels(RGBA) must be in 1..4 range");
        if (packedMatrix.dim(1) > Integer.MAX_VALUE || packedMatrix.dim(2) > Integer.MAX_VALUE)
            throw new TooLargeArrayException("Too large packed " + packedMatrix
                    + ": dim(1)/dim(2) must be in <=Integer.MAX_VALUE");
        PArray array = packedMatrix.array();
        if (!(array instanceof ByteArray) && byteArrayRequired())
            throw new IllegalArgumentException("ByteArray required");
    }
}
