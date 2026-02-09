/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jep.Interpreter;
import jep.JepConfig;
import jep.SharedInterpreter;
import jep.python.PyCallable;
import jep.python.PyObject;

public class SimpleJepForInterpretersAndNumpy {
    public static void main(String[] args) throws InterruptedException {
        SharedInterpreter.setConfig(new JepConfig().redirectStdout(System.out));

        Thread t = new Thread(() -> {
            try (Interpreter interp = new SharedInterpreter()) {
                // SubInterpreter will not work properly with numpy!
                Object result = null;
                System.out.printf("%nInterpreter: %s%n", interp);
// Numpy 1
                interp.exec("import numpy\n");
                interp.exec("class myClass():\n    pass\n");
                interp.exec("def createMyClass():\n    return myClass()\n");
                interp.exec("def myTestString():\n    return '123'\n");
                interp.exec("def myTestArray():\n    return [1,2,3]\n");
                interp.exec("def myTestNumber():\n    return 123\n");
                interp.exec("print(myTestNumber())");

// Numpy 2
                System.out.println("Getting PyCallable");
                final PyCallable callable = interp.getValue("myClass", PyCallable.class);
                System.out.println("Calling PyCallable");
                result = callable.call();
                System.out.printf("call result: %s%n", result);

// Numpy 3
                System.out.println("Calling constructor");
                final PyObject myClass = (PyObject) interp.invoke("myClass");
                System.out.printf("invoke result: %s%n", myClass);

// Numpy 4
                interp.exec("_myClass = myClass()");
                result = interp.getValue("_myClass");
                System.out.printf("getValue result: %s%n", result);

                System.out.println("Calling function");
                result = interp.invoke("myTestString");
                System.out.printf("invoke result: %s%n", result);
            }
        });
        t.start();
        t.join();

        System.out.println();
        System.err.println();

        t = new Thread(() -> {
            try (Interpreter interp = new SharedInterpreter()) {
                System.out.printf("%nInterpreter: %s%n", interp);
                interp.exec("import numpy as np\n");
                interp.exec("def myTest():\n    return np.array([2, 3, 4])\n");
                interp.exec("print(myTest())");
                Object result = interp.invoke("myTest");
                System.out.printf("invoke result: %s (%s)%n", result, result.getClass().getCanonicalName());
            }
        });
        t.start();
        t.join();
    }
}
