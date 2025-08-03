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

import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.JepInterpretation;
import net.algart.executors.api.Executor;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleJep extends Executor {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private boolean shared = false;

    private final JepPerformerContainer sharedContainer = JepPerformerContainer.getContainer(JepInterpretation.Kind.SHARED);
    private final JepPerformerContainer localContainer = JepPerformerContainer.getContainer(JepInterpretation.Kind.SUB_INTERPRETER);
    private final int instanceId = COUNTER.incrementAndGet();

    public boolean isShared() {
        return shared;
    }

    public ExampleJep setShared(boolean shared) {
        this.shared = shared;
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
//        Context context = Context.newBuilder("js")
//                .allowAllAccess(true)
//                .build();
//        System.out.println("SNumbers: " +
//                context.eval("js","Java.type('net.algart.executors.api.data.SNumbers')"));
//

        long t1 = System.nanoTime();
        final JepPerformer performer = (shared ? sharedContainer : localContainer).performer();
        long t2 = System.nanoTime();
        final String script = "from java.lang import System\n"
                + (shared ? "import numpy\n" : "")
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
        sharedContainer.close();
        localContainer.close();
    }

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        System.out.println(new ExampleJep().setShared(true).testJep("shared"));
        System.out.println(new ExampleJep().setShared(false).testJep("local"));
    }
}
