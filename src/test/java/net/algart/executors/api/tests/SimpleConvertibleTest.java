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
import net.algart.executors.modules.core.demo.ExampleMultiMatrixGradients;
import net.algart.external.MatrixIO;
import net.algart.multimatrix.MultiMatrix;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class SimpleConvertibleTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: %s some_test.bmp%n", SimpleConvertibleTest.class.getName());
            return;
        }
        final Path resultFile = Path.of(args[0]);

        //noinspection resource
        ExampleMultiMatrixGradients e = new ExampleMultiMatrixGradients();
        e.setElementType(byte.class);
        MultiMatrix mm = e.create(1000, 1000, 1);
        SMat m = SMat.valueOf(mm, SMat.ChannelOrder.STANDARD);
        ByteBuffer byteBuffer = m.getByteBuffer();
        byte[] array = m.getPointer().toByteArray(m);
        System.out.printf("Multi-matrix: %s%n", mm);
        System.out.printf("SMat: %s%n", m);
        System.out.printf("byteBufeer: %s%n", byteBuffer);
        System.out.printf("array: byte[%d]%n", array.length);

        SMat copy = new SMat().setAll(
                m.getDimensions(), m.getDepth(), m.getNumberOfChannels(), byteBuffer, false);
        BufferedImage bi = copy.toBufferedImage();
        assert bi != null;
        System.out.printf("BufferedImage: %s%n", bi);
        MatrixIO.writeBufferedImage(resultFile, bi);
        System.out.printf("Written to %s%n", resultFile);
    }
}

