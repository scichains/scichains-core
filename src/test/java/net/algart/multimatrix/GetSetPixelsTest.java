/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.api.data.SMat;
import net.algart.io.MatrixIO;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class GetSetPixelsTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: %s result_image.png%n", GetSetPixelsTest.class);
            return;
        }
        final String fileName = args[0];
        final double[] color = {1.0, 1.0, 0.0};
        MultiMatrix2D m = MultiMatrix.newMultiMatrix2D(double.class, 3, 300, 200);
        for (long y = 0; y < m.dimY(); y++) {
            for (long x = 0; x < m.dimX(); x++) {
                for (int k = 0; k < m.numberOfChannels(); k++) {
                    final double value = color[k] * (x + y) / 300;
                    m.setPixelChannel(k, x, y, value);
                    if (m.getPixelChannel(k, x, y) != value) {
                        throw new AssertionError(k + "; " + x + ", " + y + ": "
                                + m.getPixelChannel(k, x, y) + "!=" + value);
                    }
                }
            }
        }
        for (long y = 0; y < m.dimY(); y++) {
            for (long x = 0; x < m.dimX(); x++) {
                final MultiMatrix.PixelValue pixel = m.getPixel(m.indexInArray(x, y));
                for (int k = 0; k < m.numberOfChannels(); k++) {
                    if (pixel.getChannel(k) != m.getPixelChannel(k, x, y)) {
                        throw new AssertionError(k + "; " + x + ", " + y + ": "
                                + m.getPixelChannel(k, x, y) + "!=" + pixel.getChannel(k));
                    }
                }
                m.setPixel(m.indexInArray(x, y), pixel); // - really does not change anything
            }
        }
        final BufferedImage image = SMat.of(m).toBufferedImage();
        System.out.printf("Writing %s%n", fileName);
        assert image != null;
        MatrixIO.writeBufferedImage(Path.of(fileName), image);
        for (Class<?> elementType : MultiMatrix.SUPPORTED_ELEMENT_TYPES) {
            System.out.printf("Pixel at the center: %s (in %s)%n",
                    m.asPrecision(elementType).getPixel(m.dimX() / 2, m.dimY() / 2), m);
        }
    }
}
