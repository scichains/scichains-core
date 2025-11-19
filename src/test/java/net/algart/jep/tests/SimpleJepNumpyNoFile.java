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

import jep.Interpreter;
import jep.SharedInterpreter;
import jep.SubInterpreter;

import java.util.function.Supplier;

public class SimpleJepNumpyNoFile {
    public static void main(String[] args) {
        testNoNumpy((SubInterpreter::new));
        // testNumpy(SubInterpreter::new); // - an attempt to call SubInterpreter FIRST (bad idea: see below)
        testNumpy(SharedInterpreter::new);
        // - Correct usage!

        testNumpy(SubInterpreter::new);
        // - Note: numpy is INCOMPATIBLE with sub-interpreters: https://github.com/numpy/numpy/issues/27192
/*
        SubInterpreter shows a warning like the following:
        ----------
        <string>:1: UserWarning: NumPy was imported from a Python sub-interpreter but NumPy does not properly
        support sub-interpreters. This will likely work for most users but might cause hard to track down
        issues or subtle bugs. A common user of the rare sub-interpreter feature is wsgi which also allows
        single-interpreter mode.
        Improvements in the case of bugs are welcome, but is not on the NumPy roadmap,
        and full support may require significant effort to achieve.
        ----------

        An attempt to call SubInterpreter FIRST will lead to an exception like the following:

        Exception in thread "main" jep.JepException: <class 'RuntimeError'>: CPU dispatcher tracer already initlized
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\_core\multiarray.<module>(multiarray.py:11)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\_core\__init__.<module>(__init__.py:22)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\__config__.<module>(__config__.py:4)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\__init__.<module>(__init__.py:125)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\_core\multiarray.<module>(multiarray.py:11)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\_core\__init__.<module>(__init__.py:22)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\__config__.<module>(__config__.py:4)
            at C:\Users\Daniel\AppData\Local\Programs\Python\Python313\Lib\site-packages\numpy\__init__.<module>(__init__.py:125)
        */
    }

    private static void testNoNumpy(Supplier<Interpreter> supplier) {
        try (Interpreter interp = supplier.get()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("import sys");
            interp.exec("from java.lang import System");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(\"(java:) \" + s)");
            interp.exec("print(sys.path)");
            interp.exec("print(s)");
        }
    }

    private static void testNumpy(Supplier<Interpreter> supplier) {
        try (Interpreter interp = supplier.get()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("import numpy as np");
            interp.exec("import sys");
            interp.exec("from java.lang import System");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(\"(java:) \" + s)");
            interp.exec("print(sys.path)");
            interp.exec("print(s)");
            // - note: native "print" operator is visible only in the command line, not from IDE
            interp.exec("a = np.array([2, 3, 4])");
            interp.exec("System.out.println(\"(java:) \" + np.array_str(a))");
        }
    }
}
