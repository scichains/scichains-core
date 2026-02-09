/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.loops;

import net.algart.executors.api.Executor;

public final class IterationCount extends Executor {
    public static final String OUTPUT_COUNT = "count";
    public static final String OUTPUT_COUNT_1 = "count_1";
    public static final String OUTPUT_IS_FIRST = "is_first";
    public static final String S = "s";
    public static final String X = "x";
    public static final String M = "m";

    private boolean throwExceptionIfOverflow = false;

    private long counter = 0L;

    public IterationCount() {
        setDefaultOutputScalar(OUTPUT_COUNT);
        addOutputScalar(OUTPUT_COUNT_1);
        addOutputScalar(OUTPUT_IS_FIRST);
        addInputScalar(S);
        addInputNumbers(X);
        addInputMat(M);
        addOutputScalar(S);
        addOutputNumbers(X);
        addOutputMat(M);
    }

    public boolean isThrowExceptionIfOverflow() {
        return throwExceptionIfOverflow;
    }

    public IterationCount setThrowExceptionIfOverflow(boolean throwExceptionIfOverflow) {
        this.throwExceptionIfOverflow = throwExceptionIfOverflow;
        return this;
    }

    @Override
    public void initialize() {
        counter = 0;
    }

    @Override
    public void process() {
        getScalar(S).exchange(getInputScalar(S, true));
        getNumbers(X).exchange(getInputNumbers(X, true));
        getMat(M).exchange(getInputMat(M, true));
        getScalar(OUTPUT_COUNT).setTo(counter);
        getScalar(OUTPUT_COUNT_1).setTo(counter + 1);
        getScalar(OUTPUT_IS_FIRST).setTo(counter == 0);
        if (counter + 1 == Long.MAX_VALUE) {
            // - very, very improbable
            if (throwExceptionIfOverflow) {
                throw new IllegalStateException("Counter overflow: " + counter);
            } else {
                return;
            }
        }
        counter++;
    }
}
