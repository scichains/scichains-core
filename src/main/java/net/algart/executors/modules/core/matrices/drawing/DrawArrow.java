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
import net.algart.executors.modules.core.common.awt.AWTDrawer;

import java.awt.*;

public final class DrawArrow extends AWTDrawer {
    private boolean percents = false;
    private double x1 = 0.0;
    private double y1 = 0.0;
    private double x2 = 0.0;
    private double y2 = 0.0;
    private double thickness = 1;
    private double arrowLength = 20;
    private double arrowWidth = 5;

    public boolean isPercents() {
        return percents;
    }

    public DrawArrow setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX1() {
        return x1;
    }

    public DrawArrow setX1(double x1) {
        this.x1 = x1;
        return this;
    }

    public double getY1() {
        return y1;
    }

    public DrawArrow setY1(double y1) {
        this.y1 = y1;
        return this;
    }

    public double getX2() {
        return x2;
    }

    public DrawArrow setX2(double x2) {
        this.x2 = x2;
        return this;
    }

    public double getY2() {
        return y2;
    }

    public DrawArrow setY2(double y2) {
        this.y2 = y2;
        return this;
    }

    public double getThickness() {
        return thickness;
    }

    public DrawArrow setThickness(double thickness) {
        this.thickness = positive(thickness);
        return this;
    }

    public double getArrowLength() {
        return arrowLength;
    }

    public DrawArrow setArrowLength(double arrowLength) {
        this.arrowLength = positive(arrowLength);
        return this;
    }

    public double getArrowWidth() {
        return arrowWidth;
    }

    public DrawArrow setArrowWidth(double arrowWidth) {
        this.arrowWidth = positive(arrowWidth);
        return this;
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        final double x1 = percents ? this.x1 / 100.0 * (dimX - 1) : this.x1;
        final double y1 = percents ? this.y1 / 100.0 * (dimY - 1) : this.y1;
        final double x2 = percents ? this.x2 / 100.0 * (dimX - 1) : this.x2;
        final double y2 = percents ? this.y2 / 100.0 * (dimY - 1) : this.y2;
        drawArrowLine(g,
                Arrays.round32(x1), Arrays.round32(y1), Arrays.round32(x2), Arrays.round32(y2),
                thickness, arrowLength, arrowWidth);
    }

    public static void drawArrowLine(
            Graphics2D graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            double thickness,
            double arrowLength,
            double arrowWidth) {
        int dx = x2 - x1, dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        double xm = length - arrowLength, xn = xm, ym = arrowWidth, yn = -arrowWidth;
        double sin = dy / length, cos = dx / length;

        double x = xm * cos - ym * sin + x1;
        ym = xm * sin + ym * cos + y1;
        xm = x;

        x = xn * cos - yn * sin + x1;
        yn = xn * sin + yn * cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        graphics.setStroke(new BasicStroke((float) thickness));
        graphics.drawLine(x1, y1, (int) ((xm + xn) / 2), (int) ((ym + yn) / 2));
        graphics.setStroke(new BasicStroke());
        graphics.drawLine(x1, y1, x2, y2);
        // - to be on the safe side
        graphics.fillPolygon(xpoints, ypoints, 3);
    }
}
