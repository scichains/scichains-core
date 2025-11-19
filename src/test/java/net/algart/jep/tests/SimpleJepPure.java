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

public class SimpleJepPure {
    private static final boolean FORCE_DISABLE_NUMPY = false;
    // - not necessary since JEP 4.3.1
    private static final boolean USE_INVOKE_TO_CREATE_OBJECT = true;
    private static final boolean USE_GET_VALUE = true;

    public static void main(String[] args) {
        try (Interpreter interpreter = new SubInterpreter()) {
            System.out.println("Interpreter: " + interpreter);
            System.out.println();
            if (FORCE_DISABLE_NUMPY) {
                interpreter.exec("""
                        import sys
                        sys.modules['numpy'] = None
                        """);
                // - useful for SubInterpreter
            }
            interpreter.exec("""
                class Parameters:
                    def __init__(self):
                        pass
                """);
            interpreter.exec("def test():\n    return '123'\n");
            interpreter.exec("print(test())");
            Object result = interpreter.invoke("test");
            System.out.printf("Python function: %s%n", result);
            // - before this, no problem with Numpy (jep 4.2.2)

            if (USE_INVOKE_TO_CREATE_OBJECT) {
                result = interpreter.invoke("Parameters");
                // - in the current version, leads to warnings in the console (when numpy+jep are correctly installed)
                System.out.printf("New Python object: %s (%s)%n", result, result == null ? null : result.getClass());
            }

            if (USE_GET_VALUE) {
                Object member = interpreter.getValue("Parameters");
                // - in the current version, leads to warnings in the console (when numpy+jep are correctly installed)
                System.out.printf("member: %s (%s)%n", member, member.getClass());
                Object instance = ((PyCallable) member).call();
                System.out.printf("Parameters instance: %s (%s)%n", instance, instance.getClass());
            }
        }
    }
}
