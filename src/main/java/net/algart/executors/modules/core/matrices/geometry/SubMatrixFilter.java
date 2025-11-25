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

package net.algart.executors.modules.core.matrices.geometry;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelFilter;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;

public abstract class SubMatrixFilter extends MultiMatrixChannelFilter {
    public static final String RECTANGULAR_AREA = "rectangular_area";

    private boolean percents = false;
    private double left = 0.0;
    private double top = 0.0;
    private double front = 0.0;
    private double right = 0.0;
    private double bottom = 0.0;
    private double rear = 0.0;
    // right/bottom/rear are used if width/height/depth <=0
    private double width = 100.0;
    private double height = 100.0;
    private double depth = 100.0;

    public SubMatrixFilter() {
        addInputNumbers(RECTANGULAR_AREA);
        // - but output port is not necessary in all subclasses
    }

    public boolean isPercents() {
        return percents;
    }

    public SubMatrixFilter setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getLeft() {
        return left;
    }

    public SubMatrixFilter setLeft(double left) {
        this.left = left;
        return this;
    }

    public double getTop() {
        return top;
    }

    public SubMatrixFilter setTop(double top) {
        this.top = top;
        return this;
    }

    public double getFront() {
        return front;
    }

    public SubMatrixFilter setFront(double front) {
        this.front = front;
        return this;
    }

    public double getRight() {
        return right;
    }

    public SubMatrixFilter setRight(double right) {
        this.right = right;
        return this;
    }

    public double getBottom() {
        return bottom;
    }

    public SubMatrixFilter setBottom(double bottom) {
        this.bottom = bottom;
        return this;
    }

    public double getRear() {
        return rear;
    }

    public SubMatrixFilter setRear(double rear) {
        this.rear = rear;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public SubMatrixFilter setWidth(double width) {
        this.width = nonNegative(width);
        return this;
    }

    public double getHeight() {
        return height;
    }

    public SubMatrixFilter setHeight(double height) {
        this.height = nonNegative(height);
        return this;
    }

    public double getDepth() {
        return depth;
    }

    public SubMatrixFilter setDepth(double depth) {
        this.depth = depth;
        return this;
    }

    public Matrix<? extends PArray> extractSubMatrix(
            Matrix<? extends PArray> m,
            Matrix.ContinuationMode continuationMode) {
        final long dimX = m.dimX();
        final long dimY = m.dimY();
        final long dimZ = m.dimZ();
        final IRectangularArea area = getInputNumbers(RECTANGULAR_AREA, true).toIRectangularArea();
        if (area != null) {
            if (area.coordCount() != m.dimCount()) {
                throw new IllegalArgumentException("Dimensions mismatch: rectangular area is " + area.coordCount()
                        + "-dimensional, matrix is " + m.dimCount() + "-dimensional");
            }
            int coordCount = area.coordCount();
            IPoint to = area.max().add(IPoint.ofEqualCoordinates(coordCount, 1));
            if (isOutputNecessary(RECTANGULAR_AREA)) {
                // - here we also check, whether this port exists
                getNumbers(RECTANGULAR_AREA).setTo(area);
            }
            if (area.min().isOrigin() && to.equals(IPoint.of(m.dimensions()))) {
                logProcessing("whole area " + m);
                return m;
            } else {
                logProcessing("submatrix in area " + area + " of " + m);
                return m.subMatrix(area, continuationMode);
            }
        } else {
            long x1 = Math.round(percents ? left / 100.0 * Math.max(0, dimX - 1) : left);
            long x2 = Math.round(percents ? right / 100.0 * Math.max(0, dimX - 1) : right);
            long y1 = Math.round(percents ? top / 100.0 * Math.max(0, dimY - 1) : top);
            long y2 = Math.round(percents ? bottom / 100.0 * Math.max(0, dimY - 1) : bottom);
            long z1 = Math.round(percents ? front / 100.0 * Math.max(0, dimZ - 1) : front);
            long z2 = Math.round(percents ? rear / 100.0 * Math.max(0, dimZ - 1) : rear);
            long sizeX = Math.round(percents ? width / 100.0 * dimX : width);
            long sizeY = Math.round(percents ? height / 100.0 * dimY : height);
            long sizeZ = Math.round(percents ? depth / 100.0 * dimZ : depth);
            if (x2 < 0) {
                x2 += dimX;
            }
            if (y2 < 0) {
                y2 += dimY;
            }
            if (z2 < 0) {
                z2 += dimZ;
            }
            if (sizeX > 0) {
                x2 = Math.addExact(x1, sizeX) - 1;
            }
            if (sizeY > 0) {
                y2 = Math.addExact(y1, sizeY) - 1;
            }
            if (sizeZ > 0) {
                z2 = Math.addExact(z1, sizeZ) - 1;
            }
            sizeX = x2 - x1 + 1;
            sizeY = y2 - y1 + 1;
            sizeZ = z2 - z1 + 1;
            final long[] sizes = m.dimensions();
            final long[] from = new long[sizes.length];
            if (x2 < x1) {
                throw new IllegalArgumentException("Zero or negative sub-matrix X-size: " + x1 + ".." + x2);
            }
            from[0] = x1;
            sizes[0] = sizeX;
            if (from.length >= 2) {
                if (y2 < y1) {
                    throw new IllegalArgumentException("Zero or negative sub-matrix Y-size: " + y1 + ".." + y2);
                }
                from[1] = y1;
                sizes[1] = sizeY;
            }
            if (from.length >= 3) {
                if (z2 < z1) {
                    throw new IllegalArgumentException("Zero or negative sub-matrix Z-size: " + z1 + ".." + z2);
                }
                from[2] = z1;
                sizes[2] = sizeZ;
            }
            if (isOutputNecessary(RECTANGULAR_AREA)) {
                // - here we also check whether this port exists
                final IPoint minPoint = IPoint.of(from);
                final IPoint maxPoint = minPoint.add(IPoint.of(sizes)).addToAllCoordinates(-1);
                getNumbers(RECTANGULAR_AREA).setTo(IRectangularArea.of(minPoint, maxPoint));
            }
            if (x1 == 0 && y1 == 0 && z1 == 0 && sizeX == dimX && sizeY == dimY && sizeZ == dimZ) {
                logProcessing("whole " + m);
                return m;
            } else {
                logProcessing(m.dimCount() >= 3 ?
                        "submatrix " + sizeX + "x" + sizeY + "x" + sizeZ
                                + " (" + x1 + ".." + x2 + " x " + y1 + ".." + y2 + " x " + z1 + ".." + z2
                                + ") of " + m :
                        "submatrix " + sizeX + "x" + sizeY
                                + " (" + x1 + ".." + x2 + " x " + y1 + ".." + y2
                                + ") of " + m);
                return m.subMatr(from, sizes, continuationMode);
            }
        }
    }

    protected abstract void logProcessing(String submatrixDescription);
}
