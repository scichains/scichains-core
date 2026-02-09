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
import net.algart.executors.modules.core.common.numbers.IndexingBase;

import java.awt.*;

public final class DrawLine extends AWTDrawer {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_PAIRS_OF_INDEXES_OF_POINTS = "pairs_of_indexes_of_points";
    public static final String INPUT_COLORS = "colors";

    private boolean percents = false;
    private double x1 = 0.0;
    private double y1 = 0.0;
    private double x2 = 0.0;
    private double y2 = 0.0;
    private double thickness = 1;
    private double lengthIncrement = 0.0;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public DrawLine() {
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_PAIRS_OF_INDEXES_OF_POINTS);
        addInputNumbers(INPUT_COLORS);
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawLine setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX1() {
        return x1;
    }

    public DrawLine setX1(double x1) {
        this.x1 = x1;
        return this;
    }

    public double getY1() {
        return y1;
    }

    public DrawLine setY1(double y1) {
        this.y1 = y1;
        return this;
    }

    public double getX2() {
        return x2;
    }

    public DrawLine setX2(double x2) {
        this.x2 = x2;
        return this;
    }

    public double getY2() {
        return y2;
    }

    public DrawLine setY2(double y2) {
        this.y2 = y2;
        return this;
    }

    public double getThickness() {
        return thickness;
    }

    public DrawLine setThickness(double thickness) {
        this.thickness = nonNegative(thickness);
        return this;
    }

    public double getLengthIncrement() {
        return lengthIncrement;
    }

    public DrawLine setLengthIncrement(double lengthIncrement) {
        this.lengthIncrement = lengthIncrement;
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public DrawLine setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    public void process(Graphics2D g, int dimX, int dimY) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers pairsOfIndexes = getInputNumbers(INPUT_PAIRS_OF_INDEXES_OF_POINTS, true);
        if (pairsOfIndexes.isInitialized()) {
            translatePairsOfIndexes(positions, pairsOfIndexes, indexingBase);
        }
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double defaultX1 = percents ? this.x1 / 100.0 * (dimX - 1) : this.x1;
        final double defaultY1 = percents ? this.y1 / 100.0 * (dimY - 1) : this.y1;
        final double defaultX2 = percents ? this.x2 / 100.0 * (dimX - 1) : this.x2;
        final double defaultY2 = percents ? this.y2 / 100.0 * (dimY - 1) : this.y2;

        final PositionsAndColors positionsAndColors = new PositionsAndColors(
                positions, colors,
                new double[]{defaultX1, defaultY1, defaultX2, defaultY2},
                Double.NaN, 4);
        g.setStroke(new BasicStroke((float) thickness));
        for (int k = 0; k < positionsAndColors.n; k++) {
            final double[] x1y1x2y2 = positionsAndColors.xyAndOthers(k);
            increaseLength(x1y1x2y2, lengthIncrement);
            final float[] rgb = positionsAndColors.colorRGB(k);
            g.setColor(rgb == null ?
                    getColor() :
                    new Color(truncateColor01(rgb[0]), truncateColor01(rgb[1]), truncateColor01(rgb[2])));
            g.drawLine((int) x1y1x2y2[0], (int) x1y1x2y2[1], (int) x1y1x2y2[2], (int) x1y1x2y2[3]);
        }
    }

    public static void translatePairsOfIndexes(
            SNumbers positions,
            SNumbers pairsOfIndexes,
            IndexingBase indexingBase) {
        if (pairsOfIndexes.getBlockLength() != 2) {
            throw new IllegalArgumentException("Pairs of indexes must contain 2 elements per block");
        }
        final int positionsBlockLength = positions.getBlockLength();
        final int numberOfPositions = positions.n();
        if (positionsBlockLength < 2) {
            throw new IllegalArgumentException("Positions of points must contain "
                    + "at least 2 elements per block (x, y)");
        }
        final int n = pairsOfIndexes.n();
        final int[] pairsOfIndexesArray = pairsOfIndexes.toIntArray();
        final double[] positionsArray = positions.toDoubleArray();
        SNumbers.checkDimensions(n, 4);
        final double[] result = new double[4 * n];
        final int minIndex = indexingBase.start;
        final int maxIndex = numberOfPositions - 1 + indexingBase.start;
        for (int k = 0, disp = 0; k < n; k++) {
            final int i1 = pairsOfIndexesArray[2 * k];
            final int i2 = pairsOfIndexesArray[2 * k + 1];
            if (i1 < minIndex || i1 > maxIndex) {
                throw new IndexOutOfBoundsException("Invalid point index " + i1
                        + ": not in range " + minIndex + ".." + maxIndex);
            }
            if (i2 < minIndex || i2 > maxIndex) {
                throw new IndexOutOfBoundsException("Invalid point index " + i2
                        + ": not in range " + minIndex + ".." + maxIndex);
            }
            final int disp1 = (i1 - minIndex) * positionsBlockLength;
            final int disp2 = (i2 - minIndex) * positionsBlockLength;
            result[disp++] = positionsArray[disp1];
            result[disp++] = positionsArray[disp1 + 1];
            result[disp++] = positionsArray[disp2];
            result[disp++] = positionsArray[disp2 + 1];
        }
        positions.setTo(result, 4);
    }

    public static void increaseLength(double[] x1y1x2y2, double lengthIncrement) {
        if (lengthIncrement == 0.0) {
            return;
        }
        final double x1 = x1y1x2y2[0];
        final double x2 = x1y1x2y2[2];
        final double y1 = x1y1x2y2[1];
        final double y2 = x1y1x2y2[3];
        final double centerX = 0.5 * (x1 + x2);
        final double centerY = 0.5 * (y1 + y2);
        final double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        if (length == 0.0) {
            // special case: do not increase
            return;
        }
        final double scale = (length + lengthIncrement) / length;
        x1y1x2y2[0] = centerX + scale * (x1 - centerX);
        x1y1x2y2[2] = centerX + scale * (x2 - centerX);
        x1y1x2y2[1] = centerY + scale * (y1 - centerY);
        x1y1x2y2[3] = centerY + scale * (y2 - centerY);
    }
}
