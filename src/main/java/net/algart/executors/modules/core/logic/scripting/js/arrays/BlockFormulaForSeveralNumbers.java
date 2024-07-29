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

package net.algart.executors.modules.core.logic.scripting.js.arrays;

import net.algart.bridges.standard.JavaScriptPerformer;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;
import net.algart.executors.modules.core.logic.SimpleResultElementType;

import java.util.List;

// Note: it does not implement ReadOnlyExecutionInput!
// "Crazy" JavaScript CAN modify source SNumbers.
public final class BlockFormulaForSeveralNumbers extends SeveralNumbersOperation {
    public static final String INPUT_A = "a";
    public static final String INPUT_B = "b";
    public static final String INPUT_C = "c";
    public static final String INPUT_D = "d";
    public static final String INPUT_E = "e";
    public static final String INPUT_F = "f";

    private String formula = "a[0] + p * aInfo.i";
    private SimpleResultElementType resultElementType = SimpleResultElementType.FLOAT;
    private double p = 0.0;
    private double q = 0.0;
    private double r = 0.0;
    private double s = 0.0;
    private double t = 0.0;
    private double u = 0.0;

    private volatile JavaScriptPerformer javaScript = null;

    public BlockFormulaForSeveralNumbers() {
        super(INPUT_A, INPUT_B, INPUT_C, INPUT_D, INPUT_E, INPUT_F);
    }

    public String getFormula() {
        return formula;
    }

    public BlockFormulaForSeveralNumbers setFormula(String formula) {
        this.formula = nonNull(formula);
        return this;
    }

    public SimpleResultElementType getResultElementType() {
        return resultElementType;
    }

    public BlockFormulaForSeveralNumbers setResultElementType(SimpleResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
        return this;
    }

    public double getP() {
        return p;
    }

    public BlockFormulaForSeveralNumbers setP(double p) {
        this.p = p;
        return this;
    }

    public double getQ() {
        return q;
    }

    public BlockFormulaForSeveralNumbers setQ(double q) {
        this.q = q;
        return this;
    }

    public double getR() {
        return r;
    }

    public BlockFormulaForSeveralNumbers setR(double r) {
        this.r = r;
        return this;
    }

    public double getS() {
        return s;
    }

    public BlockFormulaForSeveralNumbers setS(double s) {
        this.s = s;
        return this;
    }

    public double getT() {
        return t;
    }

    public BlockFormulaForSeveralNumbers setT(double t) {
        this.t = t;
        return this;
    }

    public double getU() {
        return u;
    }

    public BlockFormulaForSeveralNumbers setU(double u) {
        this.u = u;
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        int n = -1;
        for (SNumbers source : sources) {
            if (source != null && source.isInitialized()) {
                n = source.n();
            }
        }
        if (n == -1) {
            throw new IllegalArgumentException("At least one array must be initialized");
            // - check it for a case of calling from Java application
        }
        javaScript = JavaScriptPerformer.newInstanceIfChanged(formula, javaScript);
        final SNumbers result = SNumbers.zeros(resultElementType.elementType(), n, 1);
        javaScript.putVariable("p", p);
        javaScript.putVariable("q", q);
        javaScript.putVariable("r", r);
        javaScript.putVariable("s", s);
        javaScript.putVariable("t", t);
        javaScript.putVariable("u", u);
        JSNumbersBlockInformation aInfo = new JSNumbersBlockInformation(sources.get(0));
        JSNumbersBlockInformation bInfo = new JSNumbersBlockInformation(sources.get(1));
        JSNumbersBlockInformation cInfo = new JSNumbersBlockInformation(sources.get(2));
        JSNumbersBlockInformation dInfo = new JSNumbersBlockInformation(sources.get(3));
        JSNumbersBlockInformation eInfo = new JSNumbersBlockInformation(sources.get(4));
        JSNumbersBlockInformation fInfo = new JSNumbersBlockInformation(sources.get(5));
        double[] a = aInfo.x;
        double[] b = bInfo.x;
        double[] c = cInfo.x;
        double[] d = dInfo.x;
        double[] e = eInfo.x;
        double[] f = fInfo.x;
        javaScript.putVariable("aInfo", aInfo);
        javaScript.putVariable("bInfo", bInfo);
        javaScript.putVariable("cInfo", cInfo);
        javaScript.putVariable("dInfo", dInfo);
        javaScript.putVariable("eInfo", eInfo);
        javaScript.putVariable("fInfo", fInfo);
        javaScript.putVariable("a", a);
        javaScript.putVariable("b", b);
        javaScript.putVariable("c", c);
        javaScript.putVariable("d", d);
        javaScript.putVariable("e", e);
        javaScript.putVariable("f", f);
        for (int k = 0; k < n; k++) {
            aInfo.readBlock(k);
            bInfo.readBlock(k);
            cInfo.readBlock(k);
            dInfo.readBlock(k);
            eInfo.readBlock(k);
            fInfo.readBlock(k);
            result.setValue(k, javaScript.calculateDouble());
        }
        return result;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }
}
