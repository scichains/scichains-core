/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.matrices.creation;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelGenerator;

public final class CreateConstantMatrix extends MultiMatrixChannelGenerator {
    private String color = "#FFFFFF";

    public String getColor() {
        return color;
    }

    public CreateConstantMatrix setColor(String color) {
        this.color = nonNull(color);
        return this;
    }

    @Override
    protected Matrix<? extends PArray> createChannel() {
        Class<? extends PArray> destType = Arrays.type(PArray.class, getElementType());
        double maxPossibleValue = Arrays.maxPossibleValue(destType, 1.0);
        double value = colorChannel(color, maxPossibleValue);
        logDebug(() -> "Creating constant " + getElementType() + "["
                + getNumberOfChannels() + "x" + getDimX() + "x" + getDimY()
                + "], channel " + currentChannel() + ": filler " + value);
        final Matrix<? extends PArray> result =
                getDimZ() > 0 ?
                        Matrices.constantMatrix(value, destType, getDimX(), getDimY(), getDimZ()) :
                        Matrices.constantMatrix(value, destType, getDimX(), getDimY());
        return result;
    }
}
