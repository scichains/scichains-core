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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.Executor;
import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.JepType;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleJep extends Executor {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private boolean subInterpreter = true;

    private final JepPerformerContainer normal = JepPerformerContainer.newContainer(JepType.NORMAL);
    private final JepPerformerContainer sub = JepPerformerContainer.newContainer(JepType.SUB_INTERPRETER);
    private final int instanceId = COUNTER.incrementAndGet();

    public boolean isSubInterpreter() {
        return subInterpreter;
    }

    public ExampleJep setSubInterpreter(boolean subInterpreter) {
        this.subInterpreter = subInterpreter;
        return this;
    }

    public ExampleJep() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        getScalar().setTo(testJep(getInputScalar(true).getValue()));
    }

    public Object testJep(String value) {
        long t1 = System.nanoTime();
        final JepPerformer performer = (subInterpreter ? sub : normal).noConfiguration().performer();
        long t2 = System.nanoTime();
        final String script = "from java.lang import System\n"
                + (!subInterpreter ? "import numpy\n" : "")
                + "import sys\n\n"
                + "def test():\n"
                + "    System.out.println('Hello from JEP!' + str(sys.path))\n"
                + "    return 'Hello from JEP, executor #" + instanceId + "'\n";
        performer.perform(script);
        long t3 = System.nanoTime();
        final Object result = performer.invokeFunction("test");
        logInfo(String.format(Locale.US,
                "Executing Python example: %.6f ms getting/creating context + %.6f ms processing",
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        return value == null ? result : value;
    }

    @Override
    public void close() {
        super.close();
        normal.close();
        sub.close();
    }

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        System.out.println(new ExampleJep().setSubInterpreter(false).testJep("shared"));
        System.out.println(new ExampleJep().setSubInterpreter(true).testJep("sub-interpreter"));
    }
}
