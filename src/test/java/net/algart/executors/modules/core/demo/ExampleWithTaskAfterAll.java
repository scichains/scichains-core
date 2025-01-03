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

public final class ExampleWithTaskAfterAll extends Executor {
    private class FinishHook implements Runnable {
        @Override
        public void run() {
            System.out.printf("*** Clearing %s%n", ExampleWithTaskAfterAll.this);
        }

        @Override
        public String toString() {
            return "FinishHook";
        }
    }

    private static class FinishHookPreprocess implements Runnable {
        @Override
        public void run() {
            System.out.printf("%n%n*** Previous hook: one-time tasks after executing all:%n  %s%n",
                    allOneTimeTasksAfterExecutingAll());
        }

        @Override
        public String toString() {
            return "FinishHookPreprocess";
        }
    }

    private static class FinishHookPostprocess implements Runnable {
        @Override
        public void run() {
            System.out.printf("*** Next hook: one-time tasks after executing all:%n  %s%n%n",
                    allOneTimeTasksAfterExecutingAll());
        }

        @Override
        public String toString() {
            return "FinishHookPostprocess";
        }
    }

    private final FinishHook finishHook = new FinishHook();
    private final FinishHookPreprocess finishHookPreprocess = new FinishHookPreprocess();
    private final FinishHookPostprocess finishHookPostprocess = new FinishHookPostprocess();

    @Override
    public void process() {
        addOneTimeTaskAfterExecutingAll(finishHookPreprocess);
        addOneTimeTaskAfterExecutingAll(finishHook);
        addOneTimeTaskAfterExecutingAll(new FinishHookPostprocess());
        // - not too good in a case of a loop in the chain
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tasks before executing all:%n  %s%n", allTasksBeforeExecutingAll()));
        sb.append(String.format("Tasks after executing all:%n  %s%n", allTasksAfterExecutingAll()));
        sb.append(String.format("One-time tasks after executing all:%n  %s%n", allOneTimeTasksAfterExecutingAll()));
        getScalar().setTo(sb.toString());
    }

    public static void main(String[] args) {
        beforeExecutingAll();
        addOneTimeTaskAfterExecutingAll(() -> System.out.println("Hook 1"));
        addOneTimeTaskAfterExecutingAll(() -> System.out.println("Hook 2"));
        addOneTimeTaskAfterExecutingAll(() -> System.out.println("Hook 3"));
        afterExecutingAll();
        afterExecutingAll();
    }
}
