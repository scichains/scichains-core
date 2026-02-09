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

package net.algart.executors.modules.core.logic.ifelse.scalars;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;

public final class RedirectScalar extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_PORT_PREFIX = "x";
    private int redirector = 0;

    public RedirectScalar() {
        addInputScalar(DEFAULT_INPUT_PORT);
    }

    public int getRedirector() {
        return redirector;
    }

    public RedirectScalar setRedirector(int redirector) {
        this.redirector = positive(redirector);
        return this;
    }

    @Override
    public void process() {
        positive(redirector, "redirector"); // - for a case of the 1st call, when it is not initialized
        getScalar(portName(redirector)).setTo(getInputScalar());
    }

    @Override
    public String visibleOutputPortName() {
        return portName(redirector);
    }

    private String portName(int index) {
        return OUTPUT_PORT_PREFIX + index;
    }
}
