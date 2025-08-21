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

package net.algart.executors.api.js.scriptengine.tests;

import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptContextContainer;
import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptPerformer;

import javax.script.ScriptEngine;
import java.util.Locale;

public class JavaScriptSpeed {
    private static final int N = 1_000_000;

    public static void main(String[] args) {
        final ScriptEngine context = JavaScriptContextContainer.getInstance().getLocalContext();
        JavaScriptPerformer scriptWithLoopOnArray = JavaScriptPerformer.newInstance(
                "var n = " + N + "\n"
                        + """
                            var sum = 0
                            for (let k = 0; k < n; k++) {
                                sum += a[k]
                            }
                        """, context);


        JavaScriptPerformer scriptWithLoopOnInternalArray = JavaScriptPerformer.newInstance(
                "var n = " + N + "\n"
                        + "var a = new Float64Array(n)\n"
                        + """
                            for (let k = 0; k < n; k++) {
                                a[k] = k * 0.001;
                            }
                        """, context);
        JavaScriptPerformer.newInstance("var index=0", context).perform();
        JavaScriptPerformer shortFormula = JavaScriptPerformer.newInstance(
                "var result=index++ * 0.001; a[0]=result", context);
        double[] a = new double[10];
        double[] b = new double[10];
        double[] c = new double[10];
        shortFormula.putVariable("a", a);
        shortFormula.putVariable("b", b);
        shortFormula.putVariable("c", c);
        for (int test = 1; test <= 10; test++) {
            System.out.printf("Test #%d...%n", test);
            long t1 = System.nanoTime();
            scriptWithLoopOnArray.perform();
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "eval long script with loop (%s): time %.3f ms, %.4f ns/iteration%n",
                    a[1], (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);

            t1 = System.nanoTime();
            scriptWithLoopOnInternalArray.perform();
            t2 = System.nanoTime();
            System.out.printf(Locale.US,
                    "eval long script with loop on Float64Array (%s): time %.3f ms, %.4f ns/iteration%n",
                    a[1], (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);

            t1 = System.nanoTime();
            double result = 0.0;
            for (int k = 1; k <= N; k++) {
                result = shortFormula.calculateDouble();
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "loop of evalDouble (%s): time %.3f ms, %.4f ns/iteration%n%n",
                    result, (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);
        }
    }
}
