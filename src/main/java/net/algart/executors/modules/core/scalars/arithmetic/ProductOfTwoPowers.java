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

package net.algart.executors.modules.core.scalars.arithmetic;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.SeveralScalarsOperation;

import java.util.List;

public final class ProductOfTwoPowers extends SeveralScalarsOperation {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";

    private double a = 1.0;
    private double b = 1.0;
    private double m = 1.0;

    public ProductOfTwoPowers() {
        super(INPUT_X, INPUT_Y);
    }

    public double getA() {
        return a;
    }

    public ProductOfTwoPowers setA(double a) {
        this.a = a;
        return this;
    }

    public ProductOfTwoPowers setA(String a) {
        return setA(smartParseDouble(a));
    }

    public double getB() {
        return b;
    }

    public ProductOfTwoPowers setB(double b) {
        this.b = b;
        return this;
    }

    public ProductOfTwoPowers setB(String b) {
        return setB(smartParseDouble(b));
    }

    public double getM() {
        return m;
    }

    public ProductOfTwoPowers setM(double m) {
        this.m = m;
        return this;
    }

    @Override
    public SScalar process(List<SScalar> sources) {
        double x = sources.get(0).toDouble();
        Double y = sources.get(1).toDoubleOrNull();
        double r = y == null ?
                StrictMath.pow(x, a) * m :
                StrictMath.pow(x, a) * StrictMath.pow(y, b) * m;
        return SScalar.of(r);
    }

    public static double smartParseDouble(String s) {
        s = s.trim();
        final int p = s.indexOf("/");
        if (p == -1) {
            return Double.parseDouble(s);
        }
        final String left = s.substring(0, p).trim();
        final String right = s.substring(p + 1).trim();
        return Double.parseDouble(left) / Double.parseDouble(right);
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex > 0;
    }

}
