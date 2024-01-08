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

import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.api.GraalSafety;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.*;
import java.util.function.Predicate;

public class GraalVMScriptEngineTest {

    private static void gc() throws InterruptedException {
        System.out.println("gc");
        System.gc();
        Thread.sleep(200);
        System.gc();
    }

    private static void test() throws ScriptException {
        final String moduleFile = "./src/test/java/net/algart/bridges/graalvm/tests/js/sometest.mjs";

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("a", 2);
        final String simpleScript = "10 * a";
        final Object result = engine.eval(simpleScript);
        System.out.println(result);
        System.out.println();
        System.out.println();

        engine = new ScriptEngineManager().getEngineByName("graal.js");
        bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
        final String script = "//import {toJson} from '" + moduleFile + "';\n" +
                // - will not work without .mjs
                "var s1 = new java.lang.String(\"123\");\n"
                + "var ObjectClass = Java.type('java.lang.Object');\n"
                + "var StringClass = Java.type('java.lang.String');\n"
                + "var s = new StringClass(\"123\");\n"
                + "var File = Java.type(\"java.io.File\")\n"
                + "var a = new File(\"/tmp\").listFiles();\n"
                + "var Arrays = Java.type(\"java.util.Arrays\");\n"
                + "var thread = new java.lang.Thread(); thread.start(); print(thread);\n"
                + "function testFunc(s) { print('Hello'); return s + '_' };\n"
                + "print(Arrays.toString(a));\n"
                + "print(s);\n"
                + "testFunc";

        engine.eval(script);

        System.out.println();
        System.out.println();

        String language = "js";
        GraalPerformerContainer container = GraalPerformerContainer.getShared(GraalSafety.ALL_ACCESS);
        Context context = container.performer("some-id").context();
        final Source source = Source.newBuilder(language, script, "Unnamed.mjs").buildLiteral();
        Value v = context.eval(source);
        System.out.println("Result of script: " + v);

        final Engine e = context.getEngine();
        System.out.println("Languages: " + e.getLanguages());
        System.out.println("Instruments: " + e.getInstruments());

        System.out.println("Number of shared contexts: " + GraalPerformerContainer.numberOfStoredPerformers());
        System.out.println();
        System.out.println("Again:");
        v = context.eval(source);
        System.out.println(v);
        System.out.println();
        final Value member = context.getBindings("js").getMember("testFunc");
        System.out.println("testFunc: " + member);
        Value executeResult;
        if (member != null) {
            // - null for .mjs
            executeResult = member.execute("Java-String");
            System.out.println("Result: " + executeResult);
        }
        executeResult = v.execute("Some string");
        System.out.println("Stored function result: " + executeResult);
        System.out.println("Number of shared contexts (inside) " + GraalPerformerContainer.numberOfStoredPerformers());
        container.freeResources(false);
        // - helps for local container, but not for shared
    }

    public static void main(String[] args) throws ScriptException, InterruptedException {
        test();
        gc();
        System.out.println("Number of shared contexts (outside) " + GraalPerformerContainer.numberOfStoredPerformers());
        gc();
        gc();
        // - sometimes necessary to call several times to allow finalizer/cleaner to perform closing
    }
}
