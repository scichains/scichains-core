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

import net.algart.executors.modules.core.common.awt.AWTDrawer;

import javax.swing.*;
import java.awt.*;

public final class DrawText extends AWTDrawer {
    private double x = 0.0;
    private double y = 0.0;
    private boolean renderHTML = false;
    private String text = "";

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isRenderHTML() {
        return renderHTML;
    }

    public void setRenderHTML(boolean renderHTML) {
        this.renderHTML = renderHTML;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = nonNull(text);
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        g.translate(x, y);
        if (renderHTML) {
            if (!text.startsWith("<html>")) {
                text = "<html>" + text;
            }
            final JLabel label = new JLabel();
            label.setForeground(getColor());
            label.setText(text);
            final Dimension size = label.getPreferredSize();
            label.setBounds(0, 0, size.width, size.height);
            label.paint(g);
        } else {
            final int h = g.getFontMetrics().getHeight();
            g.drawString(text, 0, h);
        }
    }
}
