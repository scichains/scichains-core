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

package net.algart.executors.modules.core.system;

import net.algart.executors.api.Executor;

public final class Gc extends Executor {
    private boolean doAction = true;

    public Gc() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public Gc setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    @Override
    public void process() {
        if (!doAction) {
            return;
        }
        for (int k = 0; k < 3; k++) {
            System.gc();
            System.runFinalization();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        final Runtime rt = Runtime.getRuntime();
        final String message = String.format(
                "Used memory: %.5f MB / %.5f MB%n"
                        + "Number of active threads: %d%n",
                (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                rt.maxMemory() / 1048576.0,
                Thread.activeCount());
        logInfo(() -> "Performing System.gc(): " + message);
        getScalar().setTo(message);
    }
}
