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

package net.algart.bridges.graalvm.tests;

import net.algart.executors.api.data.SNumbers;
import org.graalvm.polyglot.Context;

public class GraalVMAccessToJava {
    public void testUsual() {
        System.out.println("testUsual");
    }

    public static void testStatic() {
        System.out.println("testStatic");
    }

    public static void main(String[] args) {
        Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();

        context.eval("js", "java.lang.System.out.println('Hello')");
        context.getBindings("js").putMember("test", new GraalVMAccessToJava());
        context.eval("js", "test.testUsual()");
        System.out.println("SNumbers: " +
                context.eval("js",
                        "var SNumbersClass = Java.type('" + SNumbers.class.getName() + "');\n" +
                                "print(typeof(SNumbersClass));\n" +
                                "print(new SNumbersClass());\n" +
                                "SNumbersClass.zeros(Java.type('int'), 100, 2)"));
        context.getBindings("js").putMember("SNUMBERS_CLASS", SNumbers.class);
        System.out.println("SNumbers: " +
                context.eval("js",
                        "print(typeof(SNUMBERS_CLASS));\n" +
                                "print(new SNUMBERS_CLASS());\n" +
                                "var SNUMBERS_C = Java.type(SNUMBERS_CLASS.getName());\n" +
                                "SNUMBERS_C.zeros(Java.type('int'), 50, 2)"));
        System.out.println("SNumbers: " +
                context.eval("js", "Java.type('" + SNumbers.class.getName() +
                        "').zeros(Java.type('int'), 50, 2)"));
//        System.out.println("SNumbers: " +
//                context.eval("js", "SNUMBERS_CLASS.zeros('int', 50, 2)"));
        // - does not work

        String thisClass = GraalVMAccessToJava.class.getName();
        context.eval("js", "Java.type('" + thisClass + "').testStatic()");
//        context.eval("js","'" + thisClass + "'.testStatic()"); // - does not work
    }
}
