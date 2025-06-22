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
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.awt.AWTDrawer;
import net.algart.executors.modules.core.common.awt.AWTFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class DrawImage extends AWTFilter {
    public static final String INPUT_IMAGE = "image";
    private boolean requireImage = false;
    private boolean percents = false;
    private double x = 0;
    private double y = 0;
    private boolean autoExpand = true;

    public boolean isRequireImage() {
        return requireImage;
    }

    public DrawImage setRequireImage(boolean requireImage) {
        this.requireImage = requireImage;
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

    public boolean isAutoExpand() {
        return autoExpand;
    }

    public DrawImage setAutoExpand(boolean autoExpand) {
        this.autoExpand = autoExpand;
        return this;
    }

    @Override
    public BufferedImage process(BufferedImage source) {
        final BufferedImage image = getInputMat(INPUT_IMAGE, !requireImage).toBufferedImage();
        if (image != null) {
            final Graphics2D g = source.createGraphics();
            try {
                final int dimX = source.getWidth();
                final int dimY = source.getHeight();
                final int x = Arrays.round32(percents ? this.x / 100.0 * (dimX - 1) : this.x);
                final int y = Arrays.round32(percents ? this.y / 100.0 * (dimY - 1) : this.y);
                g.drawImage(image, x, y, null);
            } finally {
                g.dispose();
            }
        }
        return source;
    }

    @Override
    protected SMat correctInput(SMat input) {
        input = super.correctInput(input);
        if (autoExpand) {
            final SMat image = getInputMat(INPUT_IMAGE, !requireImage);
            if (image != null) {
                //TODO!!
            }
        }
        return input;
    }
}
