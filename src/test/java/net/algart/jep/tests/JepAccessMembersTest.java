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
import jep.python.PyCallable;
import jep.python.PyObject;

public class JepAccessMembersTest {
    private static final String TEST_SCRIPT =
            """
                    import sys
                    
                    class TestClass:
                        def __init__(self):
                            self.a = 12

                    def test():
                        result = str(sys.path)
                        result = result + "\\nHello from Python: " + str(sys.version)
                        a = TestClass()
                        return result
                    """;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: %s path_to_Python%n", JepAccessMembersTest.class.getName());
            return;
        }

        final String pythonHome = args[0];
        PyConfig pyConfig = PyConfig.python();
        pyConfig.setUseEnvironment(false);
        // - not necessary: ignored when pythonHome is set
        pyConfig.setHome(pythonHome);
        MainInterpreter.setInitParams(pyConfig);

//        pyConfig.setPythonHome(null);
//        MainInterpreter.setInitParams(pyConfig);
        // - Note: after this operator, the environment variable PYTHONHOME will be used, if it is not disabled

        Interpreter interpreter = new SharedInterpreter();
        System.out.printf("Interpreter 1: %s%n", interpreter);
        interpreter.exec(TEST_SCRIPT);
        Object result = interpreter.invoke("test");
        System.out.printf("invoke():%n%s%n", result);
        interpreter.close();
        // - The second SharedInterpreter will not work in the same thread before closing the previous

        interpreter = new SharedInterpreter();
        System.out.printf("%nInterpreter 2: %s%n", interpreter);
        interpreter.exec(TEST_SCRIPT);
        PyCallable testFunction = interpreter.getValue("test", PyCallable.class);
        result = testFunction.callAs(Object.class);
        System.out.printf("callAs:%n%s%n", result);
        PyCallable testClass = interpreter.getValue("TestClass", PyCallable.class);
        PyObject testInstance = testClass.callAs(PyObject.class);
        result = testInstance.getAttr("a");
        System.out.printf("getAttr:%n%s%n", result);
        interpreter.close();

        interpreter = new SubInterpreter();
        System.out.printf("%nInterpreter 3: %s%n", interpreter);
        interpreter.exec(TEST_SCRIPT);
        testFunction = (PyCallable) interpreter.getValue("test");
        result = testFunction.call();
        // - old style (without type cast): necessary when numpy is integrated!
        System.out.printf("call:%n%s%n", result);
        testInstance = (PyObject) interpreter.invoke("TestClass");
        result = testInstance.getAttr("a");
        System.out.printf("getAttr:%n%s%n", result);
        interpreter.close();
    }
}
