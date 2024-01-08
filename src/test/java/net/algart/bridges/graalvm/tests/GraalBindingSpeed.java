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

import net.algart.bridges.graalvm.api.GraalSafety;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GraalBindingSpeed {
    private static int N = 1000;

    Context safe = GraalSafety.SAFE.newBuilder().build();
    Context pure = GraalSafety.PURE.newBuilder().build();
    Source creation = Source.newBuilder("js",
                    "function createObject() { return new Object() }\ncreateObject",
                    "test.mjs")
            .buildLiteral();
    Source quickCode = Source.newBuilder("js",
                    "function exec(obj) {" +
                            "   return obj.c = obj.a + obj.b" +
                            "}\n\n" +
                            "exec",
                    "test1.mjs")
            .buildLiteral();
    // - module-like
    Source detailedCode = Source.newBuilder("js",
                    "function exec(obj) {" +
                            "   obj.c = obj.a + obj.b;\n" +
                            "   print(obj, typeof(obj));\n" +
                            "   print(obj.a, typeof(obj.a));\n" +
                            "   print(obj.b, typeof(obj.b));\n" +
                            "   print(obj.c, typeof(obj.c));\n" +
                            "   return obj.c\n" +
                            "}\n\n" +
                            "exec",
                    "test2.mjs")
            .buildLiteral();

    public void testMap(Context context, boolean detailed) {
        final Source code = detailed ? detailedCode : quickCode;
        final int n = detailed ? 1 : N;
        long t1 = System.nanoTime();
        Value func = null;
        for (int k = 0; k < n; k++) {
            func = context.eval(code);
        }
        assert func != null;
        long t2 = System.nanoTime();
        Map<String, Object> map = null;
        for (int k = 0; k < n; k++) {
            map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
        }
        long t3 = System.nanoTime();
        Value result = null;
        for (int k = 0; k < n; k++) {
            result = func.execute(map);
        }
        long t4 = System.nanoTime();
        System.out.printf(Locale.US,
                "Found %s = %s: %.3f mcs = script %.3f mcs + creating map %.3f mcs + performing %.3f mcs%n",
                result, map.get("c"),
                (t4 - t1) * 1e-3 / n, (t2 - t1) * 1e-3 / n, (t3 - t2) * 1e-3 / n, (t4 - t3) * 1e-3 / n);
    }

    public void testJSObject(Context context, boolean detailed) {
        final Source code = detailed ? detailedCode : quickCode;
        final int n = detailed ? 1 : N;
        long t1 = System.nanoTime();
        Value func = null;
        Value creator = null;
        for (int k = 0; k < n; k++) {
            func = context.eval(code);
            creator = context.eval(creation);
        }
        assert func != null;
        long t2 = System.nanoTime();
        Value obj = null;
        for (int k = 0; k < n; k++) {
            obj = creator.execute();
            obj.putMember("a", 10);
            obj.putMember("b", 20);
        }
        long t3 = System.nanoTime();
        Value result = null;
        for (int k = 0; k < n; k++) {
            result = func.execute(obj);
        }
        long t4 = System.nanoTime();
        System.out.printf(Locale.US,
                "Found %s = %s: %.3f mcs = script %.3f mcs + creating map %.3f mcs + performing %.3f mcs%n",
                result, obj.getMember("c"),
                (t4 - t1) * 1e-3 / n, (t2 - t1) * 1e-3 / n, (t3 - t2) * 1e-3 / n, (t4 - t3) * 1e-3 / n);
    }

    public static void main(String[] args) throws ScriptException, InterruptedException {
        final GraalBindingSpeed test = new GraalBindingSpeed();
        for (int k = 1; k <= 200; k++) {
            System.out.printf("Test #%d%n", k);
            System.out.print("Map, safe:    ");
            test.testMap(test.safe, false);
            System.out.print("Object, safe: ");
            test.testJSObject(test.safe, false);
            System.out.print("Object, pure: ");
            test.testJSObject(test.pure, false);
        }
        System.out.println();
        System.out.println("Safe, map:");
        test.testMap(test.safe, true);
        System.out.println();
        test.testMap(test.safe, true);
        System.out.println();

        System.out.println("Safe, JS object:");
        test.testJSObject(test.safe, true);
        System.out.println();
        test.testJSObject(test.safe, true);
        System.out.println();

        System.out.println("Pure, JS object:");
        test.testJSObject(test.pure, true);
        System.out.println();
        test.testJSObject(test.pure, true);
        System.out.println();
    }
}
