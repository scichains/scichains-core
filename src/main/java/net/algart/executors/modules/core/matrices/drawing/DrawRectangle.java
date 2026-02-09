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

import net.algart.arrays.*;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannel2DFilter;
import net.algart.math.IRectangularArea;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public final class DrawRectangle extends MultiMatrixChannel2DFilter {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_COLORS = "colors";

    private String color = "#FFFFFF";
    private boolean percents = false;
    private double left = 0.0;
    private double top = 0.0;
    private double right = 0.0;
    private double bottom = 0.0;
    // right/bottom are used if width/height <=0
    private double width = 0.0;
    private double height = 0.0;
    private int thickness = 0;
    // - zero means filling
    private boolean clearSource = false;

    public DrawRectangle() {
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_COLORS);
    }

    public String getColor() {
        return color;
    }

    public DrawRectangle setColor(String color) {
        this.color = nonNull(color);
        return this;
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawRectangle setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getLeft() {
        return left;
    }

    public DrawRectangle setLeft(double left) {
        this.left = left;
        return this;
    }

    public double getTop() {
        return top;
    }

    public DrawRectangle setTop(double top) {
        this.top = top;
        return this;
    }

    public double getRight() {
        return right;
    }

    public DrawRectangle setRight(double right) {
        this.right = right;
        return this;
    }

    public double getBottom() {
        return bottom;
    }

    public DrawRectangle setBottom(double bottom) {
        this.bottom = bottom;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public DrawRectangle setWidth(double width) {
        this.width = nonNegative(width);
        return this;
    }

    public double getHeight() {
        return height;
    }

    public DrawRectangle setHeight(double height) {
        this.height = nonNegative(height);
        return this;
    }

    public int getThickness() {
        return thickness;
    }

    public DrawRectangle setThickness(int thickness) {
        this.thickness = nonNegative(thickness);
        return this;
    }

    public boolean isClearSource() {
        return clearSource;
    }

    public DrawRectangle setClearSource(boolean clearSource) {
        this.clearSource = clearSource;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double maxPossibleValue = m.array().maxPossibleValue(1.0);
        final double defaultValue = colorChannel(color, maxPossibleValue);
        final PositionsAndColors positionsAndColors = new PositionsAndColors(positions, colors, defaultValue);
        Matrix<? extends UpdatablePArray> clone =
                clearSource ?
                        Arrays.SMM.newMatrix(UpdatablePArray.class, m) :
                        Matrices.matrix(m.array().updatableClone(Arrays.SMM), m.dimensions());
        if (clearSource && currentChannel() == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL) {
            clone.array().fill(clone.array().maxPossibleValue(1.0));
        }
        for (int k = 0; k < positionsAndColors.n; k++) {
            for (IRectangularArea r : filledRectangles(
                    m.dimX(),
                    m.dimY(),
                    positionsAndColors.xy,
                    positionsAndColors.xyBlockLength,
                    k)) {
                Matrices.Region region = Matrices.Region.getRectangle2D(r.range(0), r.range(1));
                double value = positionsAndColors.colorValue(k, currentChannel(), maxPossibleValue);
                if (clone.array() instanceof PFixedArray) {
                    value = Math.max(value, 0.0);
                    value = Math.min(value, maxPossibleValue);
                    // - without it, results of fillRegion will be relatively strange
                }
                Matrices.fillRegion(clone, region, value);
            }
        }
        return clone;
    }

    private Collection<IRectangularArea> filledRectangles(
            long dimX,
            long dimY,
            double[] positions,
            int blockLength,
            int index) {
        if (dimX <= 0 || dimY <= 0) {
            return Collections.emptyList();
        }
        long x1 = Math.round(percents ? left / 100.0 * (dimX - 1) : left);
        long x2 = Math.round(percents ? right / 100.0 * (dimX - 1) : right);
        long y1 = Math.round(percents ? top / 100.0 * (dimY - 1) : top);
        long y2 = Math.round(percents ? bottom / 100.0 * (dimY - 1) : bottom);
        long sizeX = Math.round(percents ? width / 100.0 * dimX : width);
        long sizeY = Math.round(percents ? height / 100.0 * dimY : height);
        if (x1 < 0) {
            x1 += dimX;
        }
        if (x2 < 0) {
            x2 += dimX;
        }
        if (y1 < 0) {
            y1 += dimY;
        }
        if (y2 < 0) {
            y2 += dimY;
        }
        if (sizeX > 0) {
            x2 = x1 + sizeX - 1;
        }
        if (sizeY > 0) {
            y2 = y1 + sizeY - 1;
        }
        sizeX = x2 - x1 + 1;
        sizeY = y2 - y1 + 1;
        if (positions != null && positions.length > 0) {
            // - use sizeX/sizeY, calculated above, if positions has no sizes
            double x = positions[index * blockLength];
            double y = positions[index * blockLength + 1];
            double pixelCenterSizeX = sizeX - 1;
            double pixelCenterSizeY = sizeY - 1;
            if (blockLength >= 4) {
                pixelCenterSizeX = Math.max(0, positions[index * blockLength + 2] - 1);
                pixelCenterSizeY = Math.max(0, positions[index * blockLength + 3] - 1);
            } else if (sizeX <= 0 || sizeY <= 0) {
                return Collections.emptyList();
            }
            x1 = Math.round(x - 0.5 * pixelCenterSizeX);
            y1 = Math.round(y - 0.5 * pixelCenterSizeY);
            x2 = Math.round(x + 0.5 * pixelCenterSizeX);
            y2 = Math.round(y + 0.5 * pixelCenterSizeY);
            // - note: Math.round performs rounding of half-integers always to up, like floor((a + 1/2) * 2)
            sizeX = x2 - x1 + 1;
            sizeY = y2 - y1 + 1;
        }
        if (sizeX <= 0 || sizeY <= 0) {
            return Collections.emptyList();
        }
        Queue<IRectangularArea> rectangles = new LinkedList<>();
        rectangles.add(IRectangularArea.of(x1, y1, x2, y2));
        if (thickness > 0 && 2L * thickness < sizeX && 2L * thickness < sizeY) {
            IRectangularArea.subtractCollection(rectangles,
                    IRectangularArea.of(
                            x1 + thickness, y1 + thickness,
                            x2 - thickness, y2 - thickness));
        }
        return rectangles;
    }
}
