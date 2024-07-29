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

package net.algart.bridges.graalvm.tests;

import net.algart.bridges.graalvm.GraalJSType;
import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalAPI;
import net.algart.bridges.graalvm.api.GraalSafety;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;

public class GraalPerformerContainerTest {
    static GraalPerformerContainerTest test = new GraalPerformerContainerTest();

    GraalPerformerContainer performerContainer = GraalAPI.initializeJS(GraalPerformerContainer
//            .getSharedSafe()
            .getSharedPure()
            .setCustomizer(GraalSafety.SAFE)
            .setActionOnChangeContextId(GraalPerformerContainer.ActionOnChangeContextId.NONE));

    private static void gc() throws InterruptedException {
        System.out.println("gc");
        test = new GraalPerformerContainerTest();
        System.gc();
        Thread.sleep(200);
        System.gc();
    }

    private void test(String id, boolean doClose) {
        System.out.println("Using " + performerContainer);
        final String script = !performerContainer.getCustomizer().isSupportedJavaAccess() ?
                "const a = 5; let b = 5; a+b" :
                "var a = 5; var b = 5; var StringClass = Java.type('java.lang.String');\n"
                        + "var s = new StringClass(\"asd\");\n"
                        + "print('SNumbers: ' + SNumbersClass.zeros('int',5,2));\n"
                        + (performerContainer.getCustomizer().isAllAccess() ?
                        "java.lang.System.out.println('Java printing: ' + s);\n" :
                        "")
                        + "print('JS printing: ' + s);\n"
                        + "s.length + a+b";
        GraalPerformer performer = performerContainer.performer(id);
        GraalSourceContainer sourceContainer = GraalSourceContainer.newLiteral()
                .setJS(GraalJSType.COMMON, script, "test");
        Value emptyObjectFunction = GraalAPI.storedCreateEmptyObjectJSFunction(performer);
        System.out.println("Empty object function: \"" + emptyObjectFunction
                + "\"; its result: " + emptyObjectFunction.execute());

        // - if we use ".js" here, we will not be able to use "const" / "let";
        // if we use ".mjs", it will be a module, so the static code will be executed only once
        Value v = performer.perform(sourceContainer);
        System.out.println(v);
        System.out.println("Number of shared contexts inside: " + GraalPerformerContainer.numberOfStoredPerformers());
        if (doClose) {
            performerContainer.freeResources(true);
            System.out.println("Number of shared contexts (closed): "
                    + GraalPerformerContainer.numberOfStoredPerformers());
            System.out.println("Probably closed performer: " + performer);
            // performer.autoBindings(); // - will throw an exception
        }
    }

    public static void main(String[] args) throws ScriptException, InterruptedException {
        test.test("some-id", false);
        gc();
        System.out.println("Number of shared contexts (outside) " + GraalPerformerContainer.numberOfStoredPerformers());
        System.out.println();
        gc();
        gc();

        test.test("some-id", false);
        // - same context; static operator (print) will not be performed again
//        gc();
        System.out.println("Number of shared contexts (outside) " + GraalPerformerContainer.numberOfStoredPerformers());
        System.out.println();

        test.test("some-id2", false);
        gc();
        System.out.println("Number of shared contexts (outside) " + GraalPerformerContainer.numberOfStoredPerformers());
        System.out.println();

        test.test("some-id3", true);
        gc();
        gc();
        gc();
        System.out.println("Number of shared contexts (outside): " + GraalPerformerContainer.numberOfStoredPerformers());
    }
}
