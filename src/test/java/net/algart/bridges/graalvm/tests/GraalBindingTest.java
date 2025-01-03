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

package net.algart.bridges.graalvm.tests;

import net.algart.bridges.graalvm.api.GraalSafety;
import net.algart.executors.api.data.SMat;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;

public class GraalBindingTest {

    Context safe = GraalSafety.SAFE.newBuilder()
            .build();
    Context pure = GraalSafety.PURE.newBuilder()
//             .allowHostAccess(org.graalvm.polyglot.HostAccess.newBuilder().allowArrayAccess(true).build())
            // - without this, we have no even access to Java arrays
            .build();
    Source creation = Source.newBuilder("js",
                    "function createObject() { return new Object() }\ncreateObject",
                    "test.mjs")
            .buildLiteral();
    Source code = Source.newBuilder("js",
                    "function exec(obj) {" +
                            "   obj.d = obj.a[0] + obj.a[1] + obj.b;\n" +
                            "   print('Object: ' + obj, typeof(obj));\n" +
                            "   print('a: ' + obj.a, typeof(obj.a));\n" +
                            "   print('b: ' + obj.b, typeof(obj.b));\n" +
                            "   print('c1: ' + obj.c1, typeof(obj.c1));\n" +
//                            "   print('c1.getDimCount(): ' + obj.c1.getDimCount());\n" +
                            // - will not work in pure
                            "   print('c2: ' + obj.c2, typeof(obj.c2));\n" +
                            "   print('c2.length: ' + obj.c2.length);\n" +
                            "   print('d: ' + obj.d, typeof(obj.d));\n" +
                            "   return obj.d\n" +
                            "}\n\n" +
                            "exec",
                    "test2.mjs")
            .buildLiteral();

    public void testJSObject(Context context) {
        Value func = context.eval(code);
        Value creator = context.eval(creation);
        Value obj = creator.execute();
        obj.putMember("a", new double[]{1.2, 10.0});
        obj.putMember("b", 100.0);
        obj.putMember("c1", new SMat());
        obj.putMember("c2", "JavaString");
        Value result = result = func.execute(obj);
        System.out.printf("Found %s = %s%n",
                result, obj.getMember("d"));
    }

    public static void main(String[] args) throws ScriptException, InterruptedException {
        final GraalBindingTest test = new GraalBindingTest();
        System.out.println("Testing safe:");
        test.testJSObject(test.safe);
        System.out.println();
        System.out.println("Testing pure:");
        test.testJSObject(test.pure);
    }
}
