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
import jep.SubInterpreter;
import jep.python.PyCallable;

public class SimpleSubInterpreterTest {
    public static void main(String[] args) {
        String pyCode = """
                import sys
                sys.modules['numpy'] = None
                
                class Parameters:
                    def __init__(self):
                        pass
                """;
        Interpreter context = new SubInterpreter();
        String className = "Parameters";
        context.exec(pyCode);
        final PyCallable callable = (PyCallable) context.getValue(className);
        // - Without
        // sys.modules['numpy'] = None
        // the previous command leads to warnings in the console (when numpy+jep are correctly installed)
        // (the code above is the minimal example necessary for correct usage of Python in SciChains)
        // With this trick, we see only
        //  ModuleNotFoundError: No module named 'numpy.core'; 'numpy' is not a package
        // which is much safer.
        System.out.println("Callable: " + callable.getClass());
        Object parameters = callable.call();
        System.out.println("parameters: " + parameters.getClass());
        context.close();
    }
}
