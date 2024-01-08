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

package net.algart.executors.api.tests;

import net.algart.executors.api.data.SMat;
import net.algart.arrays.*;
import net.algart.external.ExternalAlgorithmCaller;
import net.algart.math.functions.RectangularFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SMatBitTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: %s source_image%n", SMatBitTest.class);
            return;
        }
        final File sourceFile = new File(args[0]);
        final MultiMatrix2D multiMatrix = MultiMatrix.valueOf2DRGBA(ExternalAlgorithmCaller.readImage(sourceFile));
        final Matrix<? extends PArray> intensity = multiMatrix.intensityChannel();
        Matrix<BitArray> bits = Matrices.asFuncMatrix(
            RectangularFunc.getInstance(
                0.0, intensity.array().maxPossibleValue(1.0) / 2,
                0.0, 1.0),
            BitArray.class, intensity);
        final UpdatableBitArray container = BufferMemoryModel.getInstance().newBitArray(
            2 * bits.array().length());
        final UpdatableBitArray array = container.subArr(bits.array().length() / 3, bits.array().length());
        array.copy(bits.array());
        // - specific situation: matrix based on ByteBuffer, but not from very beginning
        bits = Matrices.matrix(array, bits.dimensions());
        final MultiMatrix2D bitsMultiMatrix = MultiMatrix.valueOf2DMono(bits);
        System.out.printf("Created MultiMatrix: %s - %s%n", bitsMultiMatrix, bitsMultiMatrix.intensityChannel());
        ExternalAlgorithmCaller.writeImage(new File(sourceFile + ".aa.bit.png"),
            bitsMultiMatrix.allChannelsInRGBAOrder());

        assert bitsMultiMatrix.numberOfChannels() == 1;
        final SMat mat = new SMat().setTo(bitsMultiMatrix);
        System.out.printf("-> mat: %s%n", mat);
        mat.getByteBuffer().limit((int) ((bits.array().length() + 7) / 8));
        // - emulating minimal correct length
        final MultiMatrix2D restoredMultiMatrix = mat.toMultiMatrix2D(true);
        System.out.printf("-> MultiMatrix: %s%n", restoredMultiMatrix);
        ExternalAlgorithmCaller.writeImage(new File(sourceFile + ".2aa.bit.png"),
            restoredMultiMatrix.allChannelsInRGBAOrder());

        if (!bitsMultiMatrix.intensityChannel().equals(restoredMultiMatrix.intensityChannel())) {
            throw new AssertionError("Error while restoring multi-matrix");
        }

        final BufferedImage bufferedImage = mat.toBufferedImage();
        System.out.printf("-> bufferedImage: %s%n", bufferedImage);
        ImageIO.write(bufferedImage, "png", new File(sourceFile + "2bb.bit.png"));
    }
}
