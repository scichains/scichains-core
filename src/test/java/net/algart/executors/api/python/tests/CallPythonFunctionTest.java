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

package net.algart.executors.api.python.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.python.core.CallPythonFunction;
import net.algart.jep.additions.JepType;

public class CallPythonFunctionTest {
    private static final boolean DO_CLOSE = true;
    // - set to false, and gc() should lead to cleaning Pythong threads

    private static void testPython() {
        System.out.printf("CallPythonFunctionTest test%n");
        CallPythonFunction e = new CallPythonFunction();
        e.setJepType(JepType.NORMAL);
//        e.setJepType(JepType.SUB_INTERPRETER);
        // - in the current version, leads to warnings in the console
        e.execute();
        if (DO_CLOSE) {
            e.close();
        }
    }

    public static void callGc() {
        System.out.printf("CallGC%n");
        for (int k = 0; k < 3; k++) {
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        final Runtime rt = Runtime.getRuntime();
        System.out.printf(
                "Used memory: %.5f MB / %.5f MB%n"
                        + "Number of active threads: %d%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                rt.maxMemory() / 1048576.0,
                Thread.activeCount());
    }

    public static void main(String[] args) {
        ExecutionBlock.initializeExecutionSystem();
        for (int test = 1; test <= 10; test++) {
            System.out.printf("%nTest #%d%n", test);
            testPython();
            callGc();
        }
    }
}
