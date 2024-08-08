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

import net.algart.arrays.*;
import net.algart.bridges.standard.JavaScriptPerformer;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.numbers.SeveralNumberArraysOperation;
import net.algart.executors.modules.core.logic.SimpleResultElementType;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;

import java.util.List;

public final class ElementwiseFormula extends SeveralNumberArraysOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_A = "a";
    public static final String INPUT_B = "b";
    public static final String INPUT_C = "c";
    public static final String INPUT_D = "d";
    public static final String INPUT_E = "e";
    public static final String INPUT_F = "f";

    private String formula = "a * p + b";
    private SimpleResultElementType resultElementType = SimpleResultElementType.FLOAT;
    private double defaultA = 0.0;
    private double defaultB = 0.0;
    private double defaultC = 0.0;
    private double defaultD = 0.0;
    private double defaultE = 0.0;
    private double defaultF = 0.0;
    private double p = 0.0;
    private double q = 0.0;
    private double r = 0.0;
    private double s = 0.0;
    private double t = 0.0;
    private double u = 0.0;

    private volatile JavaScriptPerformer javaScript = null;

    public ElementwiseFormula() {
        super(INPUT_A, INPUT_B, INPUT_C, INPUT_D, INPUT_E, INPUT_F);
    }

    public String getFormula() {
        return formula;
    }

    public ElementwiseFormula setFormula(String formula) {
        this.formula = nonNull(formula);
        return this;
    }

    public SimpleResultElementType getResultElementType() {
        return resultElementType;
    }

    public ElementwiseFormula setResultElementType(SimpleResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
        return this;
    }

    public double getDefaultA() {
        return defaultA;
    }

    public ElementwiseFormula setDefaultA(double defaultA) {
        this.defaultA = defaultA;
        return this;
    }

    public double getDefaultB() {
        return defaultB;
    }

    public ElementwiseFormula setDefaultB(double defaultB) {
        this.defaultB = defaultB;
        return this;
    }

    public double getDefaultC() {
        return defaultC;
    }

    public ElementwiseFormula setDefaultC(double defaultC) {
        this.defaultC = defaultC;
        return this;
    }

    public double getDefaultD() {
        return defaultD;
    }

    public ElementwiseFormula setDefaultD(double defaultD) {
        this.defaultD = defaultD;
        return this;
    }

    public double getDefaultE() {
        return defaultE;
    }

    public ElementwiseFormula setDefaultE(double defaultE) {
        this.defaultE = defaultE;
        return this;
    }

    public double getDefaultF() {
        return defaultF;
    }

    public ElementwiseFormula setDefaultF(double defaultF) {
        this.defaultF = defaultF;
        return this;
    }

    public double getP() {
        return p;
    }

    public ElementwiseFormula setP(double p) {
        this.p = p;
        return this;
    }

    public double getQ() {
        return q;
    }

    public ElementwiseFormula setQ(double q) {
        this.q = q;
        return this;
    }

    public double getR() {
        return r;
    }

    public ElementwiseFormula setR(double r) {
        this.r = r;
        return this;
    }

    public double getS() {
        return s;
    }

    public ElementwiseFormula setS(double s) {
        this.s = s;
        return this;
    }

    public double getT() {
        return t;
    }

    public ElementwiseFormula setT(double t) {
        this.t = t;
        return this;
    }

    public double getU() {
        return u;
    }

    public ElementwiseFormula setU(double u) {
        this.u = u;
        return this;
    }

    @Override
    public PArray process(List<PNumberArray> sources, int... blockLengths) {
        final double[] defaults = {defaultA, defaultB, defaultC, defaultD, defaultE, defaultF};
        long length = -1;
        for (PNumberArray source : sources) {
            if (source != null) {
                length = source.length();
            }
        }
        if (length == -1) {
            throw new IllegalArgumentException("At least one array must be initialized");
            // - check it for a case of calling from Java application
        }
        while (sources.size() < defaults.length) {
            sources.add(null);
        }
        for (int k = 0, n = sources.size(); k < n; k++) {
            if (sources.get(k) == null) {
                final ConstantFunc constant = ConstantFunc.getInstance(k < defaults.length ? defaults[k] : 0.0);
                // - check k for a case of calling from Java application
                sources.set(k, Arrays.asIndexFuncArray(constant, DoubleArray.class, length));
            }
        }
        //noinspection NonAtomicOperationOnVolatileField
        javaScript = JavaScriptPerformer.newInstanceIfChanged(formula, javaScript);
        javaScript.putVariable("p", p);
        javaScript.putVariable("q", q);
        javaScript.putVariable("r", r);
        javaScript.putVariable("s", s);
        javaScript.putVariable("t", t);
        javaScript.putVariable("u", u);
        Func f;
        if (javaScript.isInterfaceSupported()) {
            final String javaScriptFunction = "function get(a, b, c, d, e, f) { return (" + formula + ");}";
            final JavaScriptInterface bridge = javaScript.getInterface(
                    JavaScriptInterface.class,
                    javaScriptFunction);
            logDebug(() -> "Elementwise JavaScript function: " + javaScriptFunction);
            f = new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return bridge.get(x[0], x[1], x[2], x[3], x[4], x[5]);
                }
            };
        } else {
            f = new AbstractFunc() {
                @Override
                public double get(double... x) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (javaScript) {
                        javaScript.putVariable("a", x[0]);
                        javaScript.putVariable("b", x[1]);
                        javaScript.putVariable("c", x[2]);
                        javaScript.putVariable("d", x[3]);
                        javaScript.putVariable("e", x[4]);
                        javaScript.putVariable("f", x[5]);
                        return javaScript.calculateDouble();
                    }
                }
            };
        }
        final PArray array = Arrays.asFuncArray(
                f,
                Arrays.type(PArray.class, resultElementType.elementType()),
                sources.toArray(new PArray[0]));
        final UpdatablePArray result = (UpdatablePArray) Arrays.SMM.newUnresizableArray(array);
        Arrays.copy(null, result, array, 1);
        // - multithreading disabled: it requires a more complex solution with Invocable.getInterface
        return result;
    }

    public interface JavaScriptInterface {
        double get(double a, double b, double c, double d, double e, double f);
    }
}
