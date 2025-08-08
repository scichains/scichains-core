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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;

public class GraalVMSimpleDemo {
    public void testUsual() {
        System.out.println("testUsual");
    }

    public static Context callJs() {
        Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();
        String source = """
                let a = 5;
                let b = 10;
                java.lang.System.out.println(a + b);
                a * b""";
        Value result = context.eval("js", source);
        System.out.println("result: " + result);
        return context;
    }

    public static void callPython() {
        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {
            String source = """
                    a = 5;
                    b = 10;
                    print(a + b);
                    a * b""";
            Value result = context.eval("python", source);
            System.out.println("result: " + result);
        }
    }

    public static void callPythonNumpy() {
        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {

            context.eval("python", "import numpy as np");
            context.eval("python", "arr = np.array([1, 2, 3, 4, 5])");
            context.eval("python", "sum_arr = np.sum(arr)");

            Value sumValue = context.eval("python", "sum_arr");
            int sum = sumValue.asInt();
            System.out.println("Sum from Python (NumPy): " + sum); // Вывод: 15

            Value pythonFunction = context.eval("python",
                    "def process_data(data):\n" +
                            "    import numpy as np\n" +
                            "    np_data = np.array(data)\n" +
                            "    result = np_data * 2\n" +
                            "    return result.tolist()");

            Value javaList = context.asValue(new int[]{10, 20, 30});
            Value pythonResult = pythonFunction.execute(javaList);
            java.util.List<Long> resultList = pythonResult.as(java.util.List.class);
            System.out.println("Result Python: " + resultList); // Вывод: [20, 40, 60]
        }
    }

    public static void main(String[] args) {
        Runtime rt = Runtime.getRuntime();
        List<Context> contexts = new java.util.ArrayList<>();
        for (int k = 1; k < 100; k++) {
            System.out.printf("Iteration %d...%n", k);
            long t1 = System.nanoTime();
            Context context = callJs();
            contexts.add(context);
//        callPython();
            // - uncomment if you added the necessary dependence in pom.xml
//        callPythonNumpy();
            // - does not work without installed GraalVM with installed numpy
            long t2 = System.nanoTime();

            System.out.printf("Creating context: %.3f ms, used memory: %.5f MB / %.5f MB%n",
                    (t2 - t1) * 1e-6,
                    (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                    rt.maxMemory() / 1048576.0);
        }
    }
}
