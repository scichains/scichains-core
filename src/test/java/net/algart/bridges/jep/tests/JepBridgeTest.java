/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.bridges.jep.tests;

import net.algart.bridges.jep.JepPerformer;
import net.algart.bridges.jep.JepPerformerContainer;
import net.algart.bridges.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.additions.JepInterpreterKind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public class JepBridgeTest {
    static boolean gc = false;
    static boolean free = false;
    private static final String SHARED_SCRIPT =
            """
                    import numpy as np

                    constArray = np.array([2, 3, 4])
                    class TestClass:
                        def __init__(self):
                            self.a = 2

                        def test(self):
                            result = np.array([3, 4])
                            # result = np.zeros(3) # works almost same time
                            return result
                    """;
    private static final String LOCAL_SCRIPT =
            """
                    import time;
                    class TestClass:
                        def __init__(self):
                            pass

                        def test(self):
                            # time.sleep(0.5)
                            return 'Hello from JEP';
                    """;
    final JepPerformerContainer sharedContainer = JepPerformerContainer.getContainer(JepInterpreterKind.SHARED);
    final JepPerformerContainer localContainer = JepPerformerContainer.getContainer(JepInterpreterKind.LOCAL);

    private static void showMemory(String message) {
        final Runtime rt = Runtime.getRuntime();
        System.out.printf("%s: %.2f/%.2f MB%n",
                message,
                (rt.totalMemory() - rt.freeMemory()) * 1e-6,
                rt.maxMemory() * 1e-6);
    }

    private static void gc() {
        System.gc();
        System.runFinalization();
        for (int k = 0; k < 2; k++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void test() throws InterruptedException {
        long t1, t2, t3, t4;
        showMemory("Starting memory");
        for (int test = 1; test <= 4; test++) {
            final boolean shared = test % 2 == 1;
            System.out.printf(Locale.US, "%nTest #%d%n", test);
            System.out.printf(Locale.US, "Number of active threads: %d%n", Thread.activeCount());
            t1 = System.nanoTime();
            final JepPerformer performer = (shared ? sharedContainer : localContainer).performer();
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Getting interpreter %s: %.3f mcs; number of active threads: %d%n",
                    performer.context(), (t2 - t1) * 1e-3, Thread.activeCount());
            showMemory("Memory");

            t1 = System.nanoTime();
            performer.perform(shared ? SHARED_SCRIPT : LOCAL_SCRIPT);
            Object result;
            t2 = System.nanoTime();
            try (AtomicPyObject testClass = performer.newObject("TestClass")) {
                t3 = System.nanoTime();
                result = testClass.invoke("test");
            }
            t4 = System.nanoTime();
            System.out.printf(Locale.US, "Executing Python: %s (%s), %.3f + %.3f + %.3f mcs; " +
                                         "number of active threads: %d%n",
                    result instanceof int[] ? Arrays.toString((int[]) result) : result,
                    result.getClass().getCanonicalName(),
                    (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, Thread.activeCount());
            showMemory("Memory");
        }
        System.out.printf("%n%n");
        System.out.printf(Locale.US, "Number of active threads before freeResources: %d%n",
                Thread.activeCount());
        if (free) {
            t1 = System.nanoTime();
            sharedContainer.close();
            localContainer.close();
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "freeResources(): %.3f ms; number of active threads: %d%n",
                    (t2 - t1) * 1e-6, Thread.activeCount());
        }
        Thread.sleep(1000);
        System.out.printf("Number of active threads: %d%n", Thread.activeCount());
        System.out.printf("Done%n");
    }

    public static void callTest(String[] args) throws InterruptedException {
        final JepBridgeTest test = new JepBridgeTest();
        configure(test.localContainer);
        configure(test.sharedContainer);
        // - WARNING! attempt to do this directly in the declaration will lead to error in maven test stage
        test.test();
    }

    public static JepPerformerContainer configure(JepPerformerContainer performerContainer) {
        final String jepApiClassName = "net.algart.bridges.jep.api.JepAPI";
        try {
            final Class<?> jepApiClass = Class.forName(jepApiClassName);
            final Method configureMethod = jepApiClass.getMethod("initialize", JepPerformerContainer.class);
            try {
                configureMethod.invoke(null, performerContainer);
                // - adds standard paths and, for shared kind, "import numpy\n" startup code
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            System.out.println(performerContainer + " configured by JepAPI");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            System.out.println(jepApiClassName + " class is not available or incorrect: " + e);
        }
        return performerContainer;
    }

    public static void main(String[] args) throws InterruptedException {
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-gc")) {
            gc = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-free")) {
            free = true;
        }

        System.out.printf("Number of active threads at the beginning: %d%n", Thread.activeCount());
        for (int m = 1; m < 10; m++) {
            System.out.printf("%n--------%nTest block #%d; number of active threads: %d%n", m, Thread.activeCount());
            callTest(args);
            if (gc) {
                gc();
                gc();
                System.out.printf("GC #%d: Number of active threads: %d%n%n%n", m, Thread.activeCount());
            }
        }
    }
}
