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
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;

public final class ExampleLoadingStage extends Executor {
    public static class TestExecutor extends Executor {
        public enum Mode {
            MODE_1,
            MODE_2
        }

        private int parameter1;
        private String parameter2;
        private Mode mode;

        public TestExecutor() {
            addOutputScalar(DEFAULT_OUTPUT_PORT);
        }

        public int getParameter1() {
            return parameter1;
        }

        public TestExecutor setParameter1(int parameter1) {
            this.parameter1 = parameter1;
            return this;
        }

        public String getParameter2() {
            return parameter2;
        }

        public TestExecutor setParameter2(String parameter2) {
            this.parameter2 = parameter2;
            return this;
        }

        public Mode getMode() {
            return mode;
        }

        public TestExecutor setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public void process() {
            getScalar().setTo("Called from " + this +
                    "\nexecutorId=" + getExecutorId()
                    + "\nsessionId=" + getSessionId());
        }

        @Override
        public String toString() {
            return "TestExecutor{" +
                    "parameter1=" + parameter1 +
                    ", parameter2='" + parameter2 + '\'' +
                    ", mode=" + mode +
                    '}';
        }
    }

    private static final ExecutorLoader MY_LOADER = new DefaultExecutorLoader<>("test loader");

    static {
        globalLoaders().register(MY_LOADER);
    }

    public ExampleLoadingStage() {
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        final String id = "7ec64582-de9e-4607-85ad-adfa97a3b0e5";
        final var executorSpecification = ExecutorSpecification.of(new TestExecutor(), id);
        final String sessionId = getSessionId();
        MY_LOADER.setSpecification(sessionId, executorSpecification);
        getScalar(DEFAULT_OUTPUT_PORT).setTo(executorSpecification.jsonString());
        System.out.println("Loading-stage test for session " + sessionId);
        System.out.println("Current folder: " + getCurrentDirectory());
        System.out.println(globalLoaders().allSerializedSpecifications(sessionId, true));
    }
}