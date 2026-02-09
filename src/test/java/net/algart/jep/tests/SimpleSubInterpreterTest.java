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
import jep.SubInterpreter;
import jep.python.PyCallable;

// See also: https://github.com/ninia/jep/issues/612
public class SimpleSubInterpreterTest {
    public static void main(String[] args) {
        String pyCode = """
                # import sys
                # sys.modules['numpy'] = None
                # - deprecated trick in JEP 4.3.1 and later
                class Parameters:
                    def __init__(self):
                        pass
                """;
        Interpreter context = new SubInterpreter();
        String className = "Parameters";
        context.exec(pyCode);
        System.err.println("Python code executed");
        // - We use System.err instead of System.out for the correct printing order in the IDE console
        final PyCallable callable = (PyCallable) context.getValue(className);

        // - Note:
        // sys.modules['numpy'] = None
        // was actual in JEP 4.2.2 and earlier versions.
        // Without it, the previous command leads to warnings in the console (when numpy+jep are correctly installed)
        // (the code above is the minimal example necessary for correct usage of Python in SciChains)
        // With this trick we see only
        //  ModuleNotFoundError: No module named 'numpy.core'; 'numpy' is not a package
        // that is much safer.
        // In the modern JEP 4.3.1 and later this is not actual

        System.err.println("Callable: " + callable.getClass());
        Object parameters = callable.call();
        System.err.println("parameters: " + parameters.getClass());
        context.close();
    }
}
