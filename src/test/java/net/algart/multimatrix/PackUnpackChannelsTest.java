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

package net.algart.multimatrix;

import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.api.data.SMat;
import net.algart.external.ExternalAlgorithmCaller;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PackUnpackChannelsTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s some_image.png matrix_folder%n", PackUnpackChannelsTest.class);
            return;
        }
        final File sourceFile = new File(args[0]);
        final File matrixFolder = new File(args[1]);
        BufferedImage image = ImageIO.read(sourceFile);
        MultiMatrix multiMatrix = SMat.valueOf(image).toMultiMatrix();
        Matrix<UpdatablePArray> matrix = multiMatrix.packChannels();
        ExternalAlgorithmCaller.writeAlgARTImage(matrixFolder, List.of(matrix));
        MultiMatrix unpackedChannels = MultiMatrix.unpackChannels(matrix);
        if (!unpackedChannels.dimEquals(multiMatrix)) {
            throw new AssertionError("Dimensions mismatch!");
        }
        for (int k = 0; k < multiMatrix.numberOfChannels(); k++) {
            if (!multiMatrix.channel(k).equals(unpackedChannels.channel(k))) {
                throw new AssertionError("Channels #" + k + " mismatch!");
            }
        }
    }
}
