/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.JArrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelGenerator;
import net.algart.executors.modules.core.matrices.drawing.DrawArrow;
import net.algart.multimatrix.MultiMatrix2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ExampleKimRotemDatan extends MultiMatrixChannelGenerator {
    private static final String KIM_COLOR = "#bebe00ff";
    private static final String ROTEM_COLOR = "#8e0000ff";
    private static final String DATAN_COLOR = "#00005eff";
    // - RGBA format (see decodeRGBA() method)

    public ExampleKimRotemDatan() {
        setNumberOfChannels(4);
        setElementType(byte.class);
    }

    @Override
    public void process() {
        super.process();
        if (parameters().getBoolean("arrows", false)) {
            final BufferedImage bufferedImage = getMat().toBufferedImage();
            addArrows(bufferedImage);
            getMat().setTo(bufferedImage);
        }
    }

    @Override
    protected Matrix<? extends PArray> createChannel() {
        final double maxPossibleValue = 255;
        final int dimX = (int) getDimX();
        final int dimY = (int) getDimY();
        final int yFrom = dimY / 10;
        final int y3StreamsFrom = dimY / 3;
        final int y3StreamsTo = 2 * dimY / 3;
        final int kimValue = (int) colorChannel(KIM_COLOR, maxPossibleValue);
        final int rotemValue = (int) colorChannel(ROTEM_COLOR, maxPossibleValue);
        final int datanValue = (int) colorChannel(DATAN_COLOR, maxPossibleValue);
        final int transparentValue = currentChannel() == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL ? 0x00 : 0xFF;
        final int[] ryb = new int[dimX];
        for (int x = 0; x < dimX; x++) {
            ryb[x] = x < dimX / 3 ? kimValue : x < 2 * dimX / 3 ? rotemValue : datanValue;
        }
        final int[] transparent = new int[dimX];
        for (int x = 0; x < dimX; x++) {
            transparent[x] = transparentValue;
        }
        final int[] spectrum = new int[dimX];
        for (int x = 0; x < dimX; x++) {
            final double h = 1.0 - (double) x / (double) dimX + 3.0 / 6.0;
            final Color color = Color.getHSBColor((float) h, 1f, 1f);
            spectrum[x] = (int) colorChannel(color, maxPossibleValue);
        }
        final byte[] bytes = new byte[dimX * dimY];
        JArrays.fill(bytes, (byte) transparentValue);
        for (int y = 0; y < yFrom; y++) {
            final double alpha = (double) y / (double) yFrom;
            final double beta = (double) y / (double) y3StreamsFrom;
            final double width = dimX * (0.5 + 0.5 * beta);
            compressLine(bytes, y * dimX, spectrum, Math.pow(alpha, 0.5), transparent, width);
        }
        for (int y = yFrom; y < y3StreamsFrom; y++) {
            final double alpha = (double) (y - yFrom) / (double) (y3StreamsFrom - yFrom);
            final double beta = (double) y / (double) y3StreamsFrom;
            final double width = dimX * (0.5 + 0.5 * beta);
            compressLine(bytes, y * dimX, ryb, alpha, spectrum, width);
        }
        for (int y = y3StreamsFrom, disp = y * dimX; y < y3StreamsTo; y++) {
            for (int x = 0; x < dimX; x++, disp++) {
                bytes[disp] = (byte) ryb[x];
            }
        }
        for (int y = y3StreamsTo; y < dimY; y++) {
            final double alpha = (double) (dimY - 1 - y) / (double) (dimY - y3StreamsTo);
            final double width = dimX * (0.5 + 0.5 * alpha);
            compressLine(bytes, y * dimX, ryb, Math.pow(alpha, 0.5), transparent, width);
        }
        return Matrix.as(bytes, dimX, dimY);
    }

    private void addArrows(BufferedImage image) {
        final int dimX = (int) getDimX();
        final int dimY = (int) getDimY();
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        int length = (int) (0.1 * dimY);
        int yCenter = (int) (0.55 * dimY);
        for (int k = 0; k < 3; k++) {
            int x = (int) Math.round((1.0 / 6.0 + k / 3.0) * dimX);
            DrawArrow.drawArrowLine(g,
                    x, yCenter + length, x, yCenter - length,
                    5, 0.3 * length, 0.1 * length);
        }
    }

    private static void compressLine(
            byte[] result,
            int resultOffset,
            int[] data1,
            double alpha1,
            int[] data2,
            double compressedWidth) {
        final int dimX = data1.length;
        final double alpha2 = 1.0 - alpha1;
        for (int x = 0, disp = resultOffset; x < dimX; x++, disp++) {
            int i = dimX / 2 + (int) ((double) (x - dimX / 2) * (double) dimX / compressedWidth);
            if (i >= 0 && i < dimX) {
                result[disp] = (byte) (data1[i] * alpha1 + data2[i] * alpha2);
            }
        }
    }

    private static String getFileExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        if (p == -1) {
            return "bmp";
        }
        return fileName.substring(p + 1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: %s resultFile.png%n",
                    ExampleKimRotemDatan.class.getName());
            return;
        }
        final File resultFile = new File(args[0]);
        BufferedImage result;
        try (ExampleKimRotemDatan executor = new ExampleKimRotemDatan()) {
            executor.setDimX(250);
            executor.setDimY(500);
            executor.setAllOutputsNecessary(true);
            executor.execute();
            result = executor.getMat().toBufferedImage();
        }
        ImageIO.write(result, getFileExtension(resultFile.getName()), resultFile);
    }
}
