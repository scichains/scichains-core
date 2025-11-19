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

package net.algart.jep.tests;

import jep.*;
import net.algart.jep.additions.JepInterpretation;
import net.algart.jep.additions.JepNumpyIntegrationException;

public class JepNumpyIntegrationTest {
    private static final String NUMPY_SCRIPT =
            """
                    import sys
                    import numpy
                    
                    def info():
                        return str(sys.version) + "\\n" + str(sys.prefix)
        
                    def array():
                        return numpy.array([1, 2])
                    """;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: %s path_to_Python%n", JepNumpyIntegrationTest.class.getName());
            return;
        }

        final String pythonHome = args[0];
        PyConfig pyConfig = PyConfig.python();
        pyConfig.setHome(pythonHome);
        System.out.printf("Using Python home: %s%n", pythonHome);
        MainInterpreter.setInitParams(pyConfig);

        try (Interpreter interpreter = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interpreter);
            System.out.println();
            interpreter.exec(NUMPY_SCRIPT);
            System.out.println("From Python:");
            final Object info = interpreter.invoke("info");
            System.out.printf("%ninfo() = %s%n", info);
            final Object array;
            try {
                array = JepInterpretation.checkNumpyIntegration(interpreter, "array");
            } catch (JepNumpyIntegrationException e) {
                System.out.printf("Numpy IS NOT WELL-CONFIGURED TOGETHER with JEP: returned array is %s%n",
                        e.isReturnedNull() ? null : e.getActualResultClass().getSimpleName());
                System.out.printf("%nException:%n%s%n", e.getMessage());
                return;
            }
            System.out.printf("%narray() = %s%n", array);
            assert array instanceof NDArray<?> || array instanceof DirectNDArray<?>;
            System.out.printf("Numpy is normally installed: array = %s%n",
                    array.getClass().getSimpleName());
        }
    }
}
