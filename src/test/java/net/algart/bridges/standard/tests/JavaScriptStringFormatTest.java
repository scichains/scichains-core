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

package net.algart.bridges.standard.tests;

import net.algart.bridges.standard.JavaScriptPerformer;

public class JavaScriptStringFormatTest {
    public static void main(String[] args) {
        final JavaScriptPerformer formula = JavaScriptPerformer.newInstance(
                "var StringC = Java.type(\"java.lang.String\");\n"
                        + "var DoubleC = Java.type(\"java.lang.Double\");\n"
                        + "var index = parseInt(3);\n"
                        + "var n = java.lang.Math.round(3.0);\n"
                        + "var ix = java.lang.Math.round(parseInt('3'));\n"
                        + "var iy = parseInt('4');\n"
                        + "var perc = DoubleC.valueOf(index * 100.0 / n);\n"
                        + "print(StringC.format(java.util.Locale.US, \"%s/%s (%.1f%%) [x:%s, y:%s]\","
                        + "index, n, perc, ix, iy));\n"
                        + "n");
        Object result = formula.perform();
        System.out.printf("%s:%n%s%n", result.getClass(), result);
    }
}
