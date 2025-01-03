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

import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.multimatrix.MultiMatrix;

public final class ExampleResultIsSometimesNull extends MultiMatrixFilter {

    private boolean noResult = false;
    private boolean notClearOutputPort = false;

    public boolean isNoResult() {
        return noResult;
    }

    public ExampleResultIsSometimesNull setNoResult(boolean noResult) {
        this.noResult = noResult;
        return this;
    }

    public boolean isNotClearOutputPort() {
        return notClearOutputPort;
    }

    public ExampleResultIsSometimesNull setNotClearOutputPort(boolean notClearOutputPort) {
        this.notClearOutputPort = notClearOutputPort;
        return this;
    }

    @Override
    public void process() {
        if (notClearOutputPort) {
            if (!noResult) {
                final SMat inputMat = getInputMat();
                getMat().setTo(inputMat);
            } // Possible BUG! No important operation: clearing output port
        } else {
            super.process();
        }
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        return noResult ? null : source;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    @Override
    public void freeAllOutputPortData() {
        super.freeAllOutputPortData();
    }
}
