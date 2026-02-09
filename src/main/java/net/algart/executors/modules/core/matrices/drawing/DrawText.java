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

package net.algart.executors.modules.core.matrices.drawing;

import net.algart.executors.modules.core.common.awt.AWTDrawer;

import javax.swing.*;
import java.awt.*;

public final class DrawText extends AWTDrawer {
    private boolean percents = false;
    private double x = 0.0;
    private double y = 0.0;
    private String fontName = "SansSerif";
    private int fontSize = 16;
    private boolean italic = false;
    private boolean bold = false;
    private boolean renderHTML = false;
    private String text = "";

    public boolean isPercents() {
        return percents;
    }

    public DrawText setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX() {
        return x;
    }

    public DrawText setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public DrawText setY(double y) {
        this.y = y;
        return this;
    }

    public String getFontName() {
        return fontName;
    }

    public DrawText setFontName(String fontName) {
        this.fontName = nonEmpty(fontName);
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public DrawText setFontSize(int fontSize) {
        this.fontSize = nonNegative(fontSize);
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public DrawText setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public DrawText setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public boolean isRenderHTML() {
        return renderHTML;
    }

    public DrawText setRenderHTML(boolean renderHTML) {
        this.renderHTML = renderHTML;
        return this;
    }

    public String getText() {
        return text;
    }

    public DrawText setText(String text) {
        this.text = nonNull(text);
        return this;
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        final double x = percents ? this.x / 100.0 * (dimX - 1) : this.x;
        final double y = percents ? this.y / 100.0 * (dimY - 1) : this.y;
        g.translate(x, y);
        final int fontStyle = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
        @SuppressWarnings("MagicConstant") final Font font = new Font(fontName, fontStyle, fontSize);
        g.setFont(font);
        String text = this.text;
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
