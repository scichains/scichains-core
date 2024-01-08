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
import net.algart.math.functions.AbstractFunc;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelGenerator;

public final class CreateBilinearFunction extends MultiMatrixChannelGenerator {
    private String color = "#FFFFFF";
    private double cx2 = 0.0;
    private double cxy = 0.0;
    private double cy2 = 0.0;
    private double cx = 1.0;
    private double cy = 1.0;
    private double c = 0.0;
    private boolean sqrtOfResult = false;
    private double originX = 50;
    private double originY = 50;
    private boolean originInPercent = true;
    private boolean remainedOfDivisionByMaxValue = false;
    private boolean rawValues = false;

    public String getColor() {
        return color;
    }

    public CreateBilinearFunction setColor(String color) {
        this.color = nonNull(color);
        return this;
    }

    public double getCx2() {
        return cx2;
    }

    public CreateBilinearFunction setCx2(double cx2) {
        this.cx2 = cx2;
        return this;
    }

    public double getCxy() {
        return cxy;
    }

    public CreateBilinearFunction setCxy(double cxy) {
        this.cxy = cxy;
        return this;
    }

    public double getCy2() {
        return cy2;
    }

    public CreateBilinearFunction setCy2(double cy2) {
        this.cy2 = cy2;
        return this;
    }

    public double getCx() {
        return cx;
    }

    public CreateBilinearFunction setCx(double cx) {
        this.cx = cx;
        return this;
    }

    public double getCy() {
        return cy;
    }

    public CreateBilinearFunction setCy(double cy) {
        this.cy = cy;
        return this;
    }

    public double getC() {
        return c;
    }

    public CreateBilinearFunction setC(double c) {
        this.c = c;
        return this;
    }

    public boolean isSqrtOfResult() {
        return sqrtOfResult;
    }

    public CreateBilinearFunction setSqrtOfResult(boolean sqrtOfResult) {
        this.sqrtOfResult = sqrtOfResult;
        return this;
    }

    public double getOriginX() {
        return originX;
    }

    public CreateBilinearFunction setOriginX(double originX) {
        this.originX = originX;
        return this;
    }

    public double getOriginY() {
        return originY;
    }

    public CreateBilinearFunction setOriginY(double originY) {
        this.originY = originY;
        return this;
    }

    public boolean isOriginInPercent() {
        return originInPercent;
    }

    public CreateBilinearFunction setOriginInPercent(boolean originInPercent) {
        this.originInPercent = originInPercent;
        return this;
    }

    public boolean isRemainedOfDivisionByMaxValue() {
        return remainedOfDivisionByMaxValue;
    }

    public CreateBilinearFunction setRemainedOfDivisionByMaxValue(boolean remainedOfDivisionByMaxValue) {
        this.remainedOfDivisionByMaxValue = remainedOfDivisionByMaxValue;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public CreateBilinearFunction setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> createChannel() {
        Class<? extends PArray> destType = Arrays.type(PArray.class, getElementType());
        final double maxPossibleValue = Arrays.maxPossibleValue(destType, 1.0);
        final double channelValue = colorChannel(color, rawValues ? 1.0 : maxPossibleValue);
        final double x0 = originInPercent ?  originX * getDimX() / 100.0 : originX;
        final double y0 = originInPercent ?  originY * getDimY() / 100.0 : originY;
        logDebug(() -> "Creating bilinear " + getElementType() + "["
            + getNumberOfChannels() + "x" + getDimX() + "x" + getDimY()
            + "], channel " + currentChannel() + ": channelValue " + channelValue);
        return Matrices.asCoordFuncMatrix(new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0], x[1]);
            }

            @Override
            public double get(double x, double y) {
                x -= x0;
                y -= y0;
                final double v = (cx2 * x + 2.0 * cxy * y + cx) * x + (cy2 * y + cy) * y + c;
                final double result = channelValue * (sqrtOfResult ? StrictMath.sqrt(v) : v);
                return remainedOfDivisionByMaxValue ? result % maxPossibleValue : result;
            }
        }, destType, getDimX(), getDimY());
    }

}
