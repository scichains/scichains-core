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

package net.algart.executors.modules.core.matrices.copying;

import net.algart.arrays.*;
import net.algart.executors.modules.core.matrices.geometry.SubMatrixFilter;

public final class FillMatrix extends SubMatrixFilter {
    private String color = "#FFFFFF";
    private double grayscaleValue = 0.0; // - used if color is an empty string
    private boolean rawGrayscaleValue = false;

    public String getColor() {
        return color;
    }

    public FillMatrix setColor(String color) {
        this.color = nonNull(color);
        return this;
    }

    public double getGrayscaleValue() {
        return grayscaleValue;
    }

    public FillMatrix setGrayscaleValue(double grayscaleValue) {
        this.grayscaleValue = grayscaleValue;
        return this;
    }

    public boolean isRawGrayscaleValue() {
        return rawGrayscaleValue;
    }

    public FillMatrix setRawGrayscaleValue(boolean rawGrayscaleValue) {
        this.rawGrayscaleValue = rawGrayscaleValue;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        Class<? extends PArray> destType = Arrays.type(PArray.class, m.elementType());
        final double maxPossibleValue = Arrays.maxPossibleValue(destType, 1.0);
        final double value;
        if (color.trim().isEmpty()) {
            value = rawGrayscaleValue ? grayscaleValue : grayscaleValue * maxPossibleValue;
        } else {
            value = colorChannel(color, maxPossibleValue);
        }
        if (extractSubMatrix(m, Matrix.ContinuationMode.NAN_CONSTANT) == m) {
            return Matrices.constantMatrix(value, destType, m.dimensions());
        } else {
            final Matrix<UpdatablePArray> clone = m.matrix(m.array().updatableClone(Arrays.SMM));
            final Matrix<? extends PArray> subMatrix = extractSubMatrix(clone, Matrix.ContinuationMode.NAN_CONSTANT);
            ((UpdatablePArray) subMatrix.array()).fill(value);
            return clone;
        }
    }

    @Override
    protected void logProcessing(String submatrixDescription) {
        if (currentChannel() == 0) {
            logDebug(() -> "Filling " + submatrixDescription);
        }

    }
}
