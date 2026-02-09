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

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.awt.AWTDrawer;

import java.awt.*;

public final class DrawStraight extends AWTDrawer {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_COLORS = "colors";

    private boolean percents = false;
    private double x = 0.0;
    private double y = 0.0;
    private double angleInDegree = 0.0;
    private double thickness = 1;

    public DrawStraight() {
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_COLORS);
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawStraight setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX() {
        return x;
    }

    public DrawStraight setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public DrawStraight setY(double y) {
        this.y = y;
        return this;
    }

    public double getAngleInDegree() {
        return angleInDegree;
    }

    public void setAngleInDegree(double angleInDegree) {
        this.angleInDegree = angleInDegree;
    }

    public double getThickness() {
        return thickness;
    }

    public DrawStraight setThickness(double thickness) {
        this.thickness = nonNegative(thickness);
        return this;
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double defaultX = percents ? this.x / 100.0 * (dimX - 1) : this.x;
        final double defaultY = percents ? this.y / 100.0 * (dimY - 1) : this.y;
        final double angle = StrictMath.toRadians(angleInDegree);
        final double cos = StrictMath.cos(angle);
        final double sin = StrictMath.sin(angle);
        final PositionsAndColors positionsAndColors = new PositionsAndColors(
                positions, colors,
                new double[]{defaultX, defaultY},
                Double.NaN, 2);
        g.setStroke(new BasicStroke((float) thickness));
        for (int k = 0; k < positionsAndColors.n; k++) {
            final double x = positionsAndColors.x(k);
            final double y = positionsAndColors.y(k);
            final float[] rgb = positionsAndColors.colorRGB(k);
            g.setColor(rgb == null ?
                    getColor() :
                    new Color(truncateColor01(rgb[0]), truncateColor01(rgb[1]), truncateColor01(rgb[2])));
            final double[] x1y1x2y2 = findX1Y1X2Y2(x, y, cos, sin, dimX, dimY);
            g.drawLine((int) x1y1x2y2[0], (int) x1y1x2y2[1], (int) x1y1x2y2[2], (int) x1y1x2y2[3]);
        }
    }

    private static double[] findX1Y1X2Y2(double x0, double y0, double cos, double sin, double dimX, double dimY) {
        // Note: we may create line greater than necessary
        if (Math.abs(cos) >= Math.abs(sin)) {
            // solve: (y1-y0)/(0-x0) = sin/cos, (y2-y0)/(dimX-1-x0) = sin/cos
            double tangent = sin / cos;
            double y1 = y0 - tangent * x0;
            double y2 = y0 + tangent * (dimX - 1.0 - x0);
            return new double[]{0, y1, dimX - 1.0, y2};
        } else {
            // solve: (x1-x0)/(0-y0) = cos/sin, (x2-x0)/(dimY-1-y0) = cos/sin
            double cotangent = cos / sin;
            double x1 = x0 - cotangent * y0;
            double x2 = x0 + cotangent * (dimY - 1.0 - y0);
            return new double[]{x1, 0, x2, dimY - 1.0};
        }
    }
}
