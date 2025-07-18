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

package net.algart.bridges.jep.tests;

import jep.Interpreter;
import jep.MainInterpreter;
import jep.PyConfig;
import jep.SharedInterpreter;

public class CustomPythonTest {
    private static final String TEST_SCRIPT =
            """
                    import sys

                    def test():
                        result = str(sys.path)
                        result = result + "\\nHello from Python: " + str(sys.version)
                        return result
                    """;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: %s path_to_Python%n", CustomPythonTest.class.getName());
            return;
        }

        final String pythonHome = args[0];
        PyConfig pyConfig = new PyConfig();
//        pyConfig.setIgnoreEnvironmentFlag(1);
        pyConfig.setPythonHome(pythonHome);
        MainInterpreter.setInitParams(pyConfig);

//        pyConfig.setPythonHome(null);
//        MainInterpreter.setInitParams(pyConfig);
        // - Note: after this operator, the environment variable PYTHONHOME will be used, if it is not disabled

        try (Interpreter interpreter = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interpreter);
            System.out.println();
            interpreter.exec(TEST_SCRIPT);
            Object result = interpreter.invoke("test");
            System.out.printf("From Python:%n%s%n", result);
        }


    }
}
