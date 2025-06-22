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
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;

public final class PositionsAndColors {
    final int xyBlockLength;
    final double[] xy;
    private final int colorBlockLength;
    private final int numberOfColors;
    private final float[] colorValues;
    private final double defaultColorValue;
    final int n;

    public PositionsAndColors(
            SNumbers positions,
            SNumbers colors,
            double defaultColorValue) {
        this(positions, colors, null, defaultColorValue, 2);
    }

    public PositionsAndColors(
            SNumbers positions,
            SNumbers colors,
            double[] defaultXYAndOthers,
            double defaultColorValue,
            int minBlockLength) {
        if (positions.isInitialized()) {
            this.xyBlockLength = positions.getBlockLength();
            if (this.xyBlockLength < minBlockLength) {
                throw new IllegalArgumentException("Positions must contain at least "
                        + minBlockLength + " elements per block");
            }
            this.xy = positions.toDoubleArray();
            this.n = xy.length / xyBlockLength;
        } else {
            this.xyBlockLength = minBlockLength;
            this.xy = defaultXYAndOthers != null ? defaultXYAndOthers.clone() : new double[0];
            this.n = 1;
        }
        if (colors.isInitialized()) {
            this.colorBlockLength = colors.getBlockLength();
            this.colorValues = colors.toFloatArray();
            this.numberOfColors = colorValues.length / colorBlockLength;
        } else {
            this.colorBlockLength = 1;
            this.colorValues = new float[0];
            this.numberOfColors = 0;
        }
        this.defaultColorValue = defaultColorValue;
    }

    public int n() {
        return n;
    }

    public double[] xyAndOthers(int index) {
        final int disp = index * xyBlockLength;
        return Arrays.copyOfRange(xy, disp, disp + Math.min(4, xyBlockLength));
    }

    public float[] colorRGB(int index) {
        if (colorBlockLength < 3 || index >= numberOfColors) {
            return null;
        } else {
            final int disp = index * colorBlockLength;
            return Arrays.copyOfRange(colorValues, disp, disp + 3);
        }
    }

    public double x(int index) {
        return xy[index * xyBlockLength];
    }

    public double y(int index) {
        return xy[index * xyBlockLength + 1];
    }

    public double colorValue(int index, int channelIndex, double maxPossibleValue) {
        if (index >= numberOfColors) {
            return defaultColorValue;
        }
        if (channelIndex == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL && colorBlockLength <= channelIndex) {
            return maxPossibleValue;
        }
        channelIndex = Math.min(channelIndex, colorBlockLength - 1);
        final int disp = index * colorBlockLength + channelIndex;
        assert disp < colorValues.length :
                index + " * " + colorBlockLength + " + " + channelIndex + " >= " + colorValues.length;
        return colorValues[disp] * maxPossibleValue;
    }
}
