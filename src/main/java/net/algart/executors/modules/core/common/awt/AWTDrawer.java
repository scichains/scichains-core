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

package net.algart.executors.modules.core.common.awt;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class AWTDrawer extends AWTFilter {
    private Color color = Color.WHITE;
    private boolean antialiasing = true;
    private boolean clearSource = false;

    protected AWTDrawer() {
    }

    public Color getColor() {
        return color;
    }

    public AWTDrawer setColor(Color color) {
        this.color = nonNull(color);
        return this;
    }

    public boolean isClearSource() {
        return clearSource;
    }

    public AWTDrawer setClearSource(boolean clearSource) {
        this.clearSource = clearSource;
        return this;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public AWTDrawer setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
        return this;
    }

    public AWTDrawer setConvertMonoToColor(boolean convertMonoToColor) {
        super.setConvertMonoToColor(convertMonoToColor);
        return this;
    }

    @Override
    public BufferedImage process(BufferedImage source) {
        final Graphics2D g = source.createGraphics();
        try {
            if (antialiasing) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            final int dimX = source.getWidth();
            final int dimY = source.getHeight();
            if (clearSource) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, dimX, dimY);
            }
            g.setColor(color);
            process(g, dimX, dimY);
        } finally {
            g.dispose();
        }
        return source;
    }

    public abstract void process(Graphics2D g, int dimX, int dimY);

    public static float truncateColor01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
