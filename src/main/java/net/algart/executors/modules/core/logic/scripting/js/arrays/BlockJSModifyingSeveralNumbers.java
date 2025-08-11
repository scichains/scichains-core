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

package net.algart.executors.modules.core.logic.scripting.js.arrays;

import net.algart.arrays.SizeMismatchException;
import net.algart.executors.api.js.engine.JavaScriptPerformer;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;

// Note: it does not implement ReadOnlyExecutionInput!
public final class BlockJSModifyingSeveralNumbers extends Executor {
    public static final String IN_OUT_A = "a";
    public static final String IN_OUT_B = "b";
    public static final String IN_OUT_C = "c";
    public static final String IN_OUT_D = "d";

    private String javaScriptCode = "a[0] = a[0] * p + k;";
    private double p = 0.0;
    private double q = 0.0;
    private double r = 0.0;
    private double s = 0.0;
    private double t = 0.0;
    private double u = 0.0;

    private volatile JavaScriptPerformer javaScript = null;

    public BlockJSModifyingSeveralNumbers() {
        useVisibleResultParameter();
        addInputNumbers(IN_OUT_A);
        addInputNumbers(IN_OUT_B);
        addInputNumbers(IN_OUT_C);
        addInputNumbers(IN_OUT_D);
        addOutputNumbers(IN_OUT_A);
        addOutputNumbers(IN_OUT_B);
        addOutputNumbers(IN_OUT_C);
        addOutputNumbers(IN_OUT_D);
    }

    public String getJavaScriptCode() {
        return javaScriptCode;
    }

    public BlockJSModifyingSeveralNumbers setJavaScriptCode(String javaScriptCode) {
        this.javaScriptCode = nonNull(javaScriptCode);
        return this;
    }

    public double getP() {
        return p;
    }

    public BlockJSModifyingSeveralNumbers setP(double p) {
        this.p = p;
        return this;
    }

    public double getQ() {
        return q;
    }

    public BlockJSModifyingSeveralNumbers setQ(double q) {
        this.q = q;
        return this;
    }

    public double getR() {
        return r;
    }

    public BlockJSModifyingSeveralNumbers setR(double r) {
        this.r = r;
        return this;
    }

    public double getS() {
        return s;
    }

    public BlockJSModifyingSeveralNumbers setS(double s) {
        this.s = s;
        return this;
    }

    public double getT() {
        return t;
    }

    public BlockJSModifyingSeveralNumbers setT(double t) {
        this.t = t;
        return this;
    }

    public double getU() {
        return u;
    }

    public BlockJSModifyingSeveralNumbers setU(double u) {
        this.u = u;
        return this;
    }

    @Override
    public void process() {
        final SNumbers aArray = getInputNumbers(IN_OUT_A);
        final SNumbers bArray = getInputNumbers(IN_OUT_B, true);
        final SNumbers cArray = getInputNumbers(IN_OUT_C, true);
        final SNumbers dArray = getInputNumbers(IN_OUT_D, true);
        checkNEquality(aArray, bArray, "a", "b");
        checkNEquality(aArray, cArray, "a", "c");
        checkNEquality(aArray, dArray, "a", "d");
        //noinspection NonAtomicOperationOnVolatileField
        javaScript = JavaScriptPerformer.newInstanceIfChanged(javaScriptCode, javaScript);
        javaScript.putVariable("p", p);
        javaScript.putVariable("q", q);
        javaScript.putVariable("r", r);
        javaScript.putVariable("s", s);
        javaScript.putVariable("t", t);
        javaScript.putVariable("u", u);
        final double[] a = createBlock(aArray);
        final double[] b = createBlock(bArray);
        final double[] c = createBlock(cArray);
        final double[] d = createBlock(dArray);
        javaScript.putVariable("a", a);
        javaScript.putVariable("b", b);
        javaScript.putVariable("c", c);
        javaScript.putVariable("d", c);
        for (int k = 0, n = aArray.n(); k < n; k++) {
            javaScript.putVariable("k", k);
            readBlock(a, aArray, k);
            readBlock(b, bArray, k);
            readBlock(c, cArray, k);
            readBlock(d, dArray, k);
            javaScript.perform();
            writeBlock(a, aArray, k);
            writeBlock(b, bArray, k);
            writeBlock(c, cArray, k);
            writeBlock(d, dArray, k);
        }
        getNumbers(IN_OUT_A).setTo(aArray);
        getNumbers(IN_OUT_B).setTo(bArray);
        getNumbers(IN_OUT_C).setTo(cArray);
        getNumbers(IN_OUT_D).setTo(dArray);
    }

    private static double[] createBlock(SNumbers numbers) {
        return new double[numbers != null && numbers.isInitialized() ? numbers.getBlockLength() : 0];
    }

    private static void readBlock(double[] block, SNumbers numbers, int k) {
        if (block.length > 0) {
            numbers.getBlockDoubleValues(k, block);
        }
    }

    private static void writeBlock(double[] block, SNumbers numbers, int k) {
        if (block.length > 0) {
            numbers.setBlockDoubleValues(k, block);
        }
    }

    private static void checkNEquality(SNumbers first, SNumbers second, String firstName, String secondName) {
        if (first != null && first.isInitialized() && second != null && second.isInitialized()) {
            if (second.n() != first.n()) {
                throw new SizeMismatchException("The number arrays' \"" + firstName + "\" and \"" + secondName
                        + "\" numbers of columns mismatch: " + first.n() + " and " + second.n());
            }
        }
    }
}
