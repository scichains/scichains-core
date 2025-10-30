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

package net.algart.executors.modules.core.matrices.drawing;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.awt.AWTFilter;
import net.algart.io.awt.MatrixToImage;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class DrawImage extends Executor {
    public static final String INPUT_IMAGE = "image";
    private boolean percents = false;
    private boolean imageRequired = false;
    private double x = 0;
    private double y = 0;
    private double opacity = 1.0;
    private boolean convertMonoToColor = false;
    private boolean autoExpand = true;

    public DrawImage() {
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_IMAGE);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public boolean isImageRequired() {
        return imageRequired;
    }

    public DrawImage setImageRequired(boolean imageRequired) {
        this.imageRequired = imageRequired;
        return this;
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawImage setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX() {
        return x;
    }

    public DrawImage setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public DrawImage setY(double y) {
        this.y = y;
        return this;
    }

    public double getOpacity() {
        return opacity;
    }

    public DrawImage setOpacity(double opacity) {
        if (opacity < 0.0 || opacity > 1.0) {
            throw new IllegalArgumentException("Opacity must be in range [0.0; 1.0]");
        }
        this.opacity = opacity;
        return this;
    }

    public boolean isConvertMonoToColor() {
        return convertMonoToColor;
    }

    public DrawImage setConvertMonoToColor(boolean convertMonoToColor) {
        this.convertMonoToColor = convertMonoToColor;
        return this;
    }

    public boolean isAutoExpand() {
        return autoExpand;
    }

    public DrawImage setAutoExpand(boolean autoExpand) {
        this.autoExpand = autoExpand;
        return this;
    }

    public void process() {
        SMat input = getInputMat();
        if (convertMonoToColor) {
            input = AWTFilter.convertMonoToColor(input);
        }
        final Matrix<? extends PArray> m = getInputMat(INPUT_IMAGE, !imageRequired)
                .toInterleavedBGR2D(false);
        if (m == null) {
            getMat().exchange(input);
            // Note: using "exchange" means that we must not implement ReadOnlyExecutionInput
            return;
        }
        Matrix<? extends PArray> source = input.toInterleavedBGR2D(false);
        assert source != null : "getInputMat() should not return non-initialized result";
        final int x = Arrays.round32(percents ? this.x / 100.0 * (input.getDimX() - 1) : this.x);
        final int y = Arrays.round32(percents ? this.y / 100.0 * (input.getDimY() - 1) : this.y);
        source = expandToFit(source,0, x + m.dim(1), y + m.dim(2));
        final BufferedImage baseImage = MatrixToImage.ofInterleavedBGR(source);
        final BufferedImage overlayImage = MatrixToImage.ofInterleavedBGR(m);
        final Graphics2D g = baseImage.createGraphics();
            try {
                if (opacity != 1.0) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
                }
                g.drawImage(overlayImage, x, y, null);
            } finally {
                g.dispose();
            }
        getMat().setTo(baseImage);
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireImage") ? "imageRequired" : name;
    }

    private static Matrix<? extends PArray> expandToFit(Matrix<? extends PArray> source, long... minDimensions) {
        long[] dimensions = source.dimensions();
        boolean needToExpand = false;
        for (int i = 0; i < minDimensions.length; i++) {
            if (dimensions[i] < minDimensions[i]) {
                dimensions[i] = minDimensions[i];
                needToExpand = true;
                break;
            }
        }
        return needToExpand ? source.subMatr(
                new long[dimensions.length], dimensions, Matrix.ContinuationMode.ZERO_CONSTANT) :
                source;
        // - for RGBA matrices ZERO_CONSTANT means transparent area
    }
}
