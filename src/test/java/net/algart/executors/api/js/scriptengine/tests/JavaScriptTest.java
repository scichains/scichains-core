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

import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptPerformer;

public class JavaScriptTest {
    public static void main(String[] args) {
        JavaScriptPerformer formula = JavaScriptPerformer.newInstance("print(1);\n"
                + "//java.lang.System.out.println('2');\n"
                + "print(new java.lang.String());\n"
                + "var IntsC = Java.type('int[]');\n"
                + "print(IntsC);\n"
                + "print(new IntsC(2).length);\n"
                + "5 < 10");
        Object result = formula.perform();
        System.out.printf("Engine: %s%n", formula.context());
        System.out.printf("%s: %s%n", result.getClass(), result);
        System.out.printf("evalBoolean: %s%n", formula.calculateBoolean());
    }
}
