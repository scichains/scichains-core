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
import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.GlobalPythonConfiguration;
import net.algart.jep.additions.JepInterpreterKind;

// Note: this test does not use JepAPI and does not automatically verify integration with numpy
public class SimpleJepPerformerTest {

    public static void main(String[] args) {
        System.out.printf("Python information before initialization: %s%n",
                GlobalPythonConfiguration.INSTANCE.pythonHomeInformation());
        GlobalPythonConfiguration.INSTANCE
                .loadFromSystemProperties()
                // - allows specifying python.path in the system properties
                .setIgnoreEnvironmentFlag(0)
//                .setPythonHome("\\tmp")
                .useForJep();

        // - THE PREVIOUS CALL IS IMPORTANT: useForJep() calls the global static method
        // MainInterpreter.setInitParams(PyConfig config)
        // for GlobalPythonConfiguration.INSTANCE

        final JepPerformerContainer container = JepPerformerContainer.getContainer(JepInterpreterKind.SHARED);
        System.out.printf("Python information: %s%n", GlobalPythonConfiguration.INSTANCE.pythonHomeInformation());
        final JepPerformer performer = container.performer();
        final Interpreter context = performer.context();
        context.exec("def test():\n    return 'Hello from JEP'\n");
        Object result = context.invoke("test");
        System.out.printf("From Python function: %s%n", result);
        container.close();
    }
}
