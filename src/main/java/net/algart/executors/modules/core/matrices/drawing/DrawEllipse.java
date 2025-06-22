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

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.awt.AWTDrawer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public final class DrawEllipse extends AWTDrawer {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_COLORS = "colors";

    private boolean percents = false;
    private double x = 0.0;
    private double y = 0.0;
    private double width = 0.0;
    private double height = 0.0;
    private double arcStartInDegree = 0.0;
    private double arcExtentInDegree = 360.0;
    private double thickness = 0;
    // - zero means filling

    public DrawEllipse() {
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_COLORS);
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawEllipse setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX() {
        return x;
    }

    public DrawEllipse setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public DrawEllipse setY(double y) {
        this.y = y;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public DrawEllipse setWidth(double width) {
        this.width = width;
        return this;
    }

    public double getHeight() {
        return height;
    }

    public DrawEllipse setHeight(double height) {
        this.height = height;
        return this;
    }

    public double getArcStartInDegree() {
        return arcStartInDegree;
    }

    public DrawEllipse setArcStartInDegree(double arcStartInDegree) {
        this.arcStartInDegree = arcStartInDegree;
        return this;
    }

    public double getArcExtentInDegree() {
        return arcExtentInDegree;
    }

    public DrawEllipse setArcExtentInDegree(double arcExtentInDegree) {
        this.arcExtentInDegree = nonNegative(arcExtentInDegree);
        return this;
    }

    public double getThickness() {
        return thickness;
    }

    public DrawEllipse setThickness(double thickness) {
        this.thickness = nonNegative(thickness);
        return this;
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double centerX = percents ? this.x / 100.0 * (dimX - 1) : this.x;
        final double centerY = percents ? this.y / 100.0 * (dimY - 1) : this.y;
        final double width = percents ? this.width / 100.0 * dimX : this.width;
        final double height = percents ? this.height / 100.0 * dimY : this.height;

        final PositionsAndColors positionsAndColors = new PositionsAndColors(
                positions, colors, new double[]{centerX, centerY}, Double.NaN, 2);
        for (int k = 0; k < positionsAndColors.n; k++) {
            final double[] xywh = positionsAndColors.xyAndOthers(k);
            final float[] rgb = positionsAndColors.colorRGB(k);
            g.setColor(rgb == null ?
                    getColor() :
                    new Color(truncateColor01(rgb[0]), truncateColor01(rgb[1]), truncateColor01(rgb[2])));
            final double w = xywh.length >= 3 ? xywh[2] : width;
            final double h = xywh.length >= 4 ? xywh[3] : xywh.length >= 3 ? xywh[2] : height;
            final double x = xywh[0];
            final double y = xywh[1];
            Shape shape = arcExtentInDegree >= 360.0 ?
                    new Ellipse2D.Double(x - 0.5 * w, y - 0.5 * h, w, h) :
                    new Arc2D.Double(x - 0.5 * w, y - 0.5 * h, w, h,
                            arcStartInDegree, arcExtentInDegree, Arc2D.PIE);
            if (thickness <= 0) {
                g.fill(shape);
            } else {
                g.setStroke(new BasicStroke((float) thickness));
                g.draw(shape);
            }
        }
    }
}
