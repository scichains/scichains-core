/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.scripting.js.tests;

import net.algart.executors.api.data.SNumbers;
import net.algart.bridges.standard.JavaScriptPerformer;
import net.algart.executors.modules.core.logic.scripting.js.arrays.BlockJSModifyingNamedNumbers;
import net.algart.executors.modules.core.numbers.creation.CreateRandomNumbers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Locale;

public class ArraysJavaScriptSpeed {
    private static final int N = 1000_000;

    final ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
    {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
//        bindings.put("polyglot.js.allowHostClassLookup", (Predicate<double[]>) s -> true);
    }

    final CreateRandomNumbers execCreate = new CreateRandomNumbers();
    final BlockJSModifyingNamedNumbers execScript = new BlockJSModifyingNamedNumbers();
    final SNumbers numbers;
    final JavaScriptPerformer formula = JavaScriptPerformer.newInstance(
            "var result=index++/1000.0; a[0]=b[1]=c[2]=result", engine);
    final Context graalContext = Context.newBuilder("js")
            .allowExperimentalOptions(true)
            .allowHostAccess(HostAccess.ALL)
            .allowPolyglotAccess(PolyglotAccess.ALL)
            .build();

    public ArraysJavaScriptSpeed() {
        execCreate.setElementType(float.class).setBlockLength(5).setNumberOfBlocks(N)
                .setMin(-2600000.0).setMax(2500000.0).setRandSeed(2);
        execCreate.process();
        numbers = execCreate.getNumbers();
        execScript.setMainOperator("x1 = x0 + 2.0");
        JavaScriptPerformer.newInstance("var index=0", engine).perform();
        graalContext.eval("js", "var index=0");
    }

    private void testExecutor() {
        long t1 = System.nanoTime();
        execScript.processNumbers(numbers);
        long t2 = System.nanoTime();
        System.out.printf("Numbers processed: %s; %.3f ms, %.2f ns / element%n",
                numbers.getValue(1),
                (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);
    }

    private void testDirectScript() {
        double[] a = new double[10];
        double[] b = new double[10];
        double[] c = new double[10];
        formula.putVariable("a", a);
        formula.putVariable("b", b);
        formula.putVariable("c", c);
        long t1 = System.nanoTime();
        double result = 0.0;
        for (int k = 0; k <= N; k++) {
            result = formula.calculateDouble();
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Script executed: %s; %.3f ms, %.4f ns/iteration%n",
                result + ", " + a[0], (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);
    }

    private void testDirectGraal() {
        final Value bindings = graalContext.getBindings("js");
        double[] a = new double[10];
        double[] b = new double[10];
        double[] c = new double[10];
        bindings.putMember("a", a);
        bindings.putMember("b", b);
        bindings.putMember("c", c);
        long t1 = System.nanoTime();
        double result = 0.0;
        for (int k = 0; k <= N; k++) {
            result = graalContext.eval("js", formula.script()).asDouble();
        }
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "GraalVM script executed: %s; %.3f ms, %.4f ns/iteration%n",
                result + ", " + a[0], (t2 - t1) * 1e-6, (double) (t2 - t1) / (double) N);
    }

    public static void main(String[] args) {
        final ArraysJavaScriptSpeed test = new ArraysJavaScriptSpeed();
        for (int testIndex = 1; testIndex <= 10; testIndex++) {
            System.out.printf("%nTest #%d...%n", testIndex);
            test.testExecutor();
            test.testDirectScript();
            test.testDirectGraal();
        }
    }
}
