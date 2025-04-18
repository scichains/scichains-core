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

package net.algart.executors.modules.core.logic.ifelse.scalars;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SScalar;

public final class SwitchScalar extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_SELECTOR = "selector_input";
    public static final String INPUT_PORT_PREFIX = "x";
    private int selector = 0;
    private boolean requireInput = true;

    public SwitchScalar() {
        addInputScalar(INPUT_SELECTOR);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public int getSelector() {
        return selector;
    }

    public SwitchScalar setSelector(int selector) {
        this.selector = nonNegative(selector);
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public SwitchScalar setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public void process() {
        final int selector = nonNegative(selector(), "selector index");
        getScalar().setTo(selector == 0 ?
                firstInitialized() :
                getInputScalar(portName(selector), !requireInput));
    }

    @Override
    public Boolean checkInputNecessary(Port inputPort) {
        if (inputPort == null) {
            return null;
        }
        final String inputPortName = inputPort.getName();
        if (INPUT_SELECTOR.equals(inputPortName)) {
            return null;
        }
        final int selector = selector();
        return selector == 0 || portName(selector).equals(inputPortName);
    }

    public int selector() {
        final String inputSelector = getInputScalar(INPUT_SELECTOR, true).getValue();
        return inputSelector != null && !inputSelector.isEmpty() ?
                Integer.parseInt(inputSelector) :
                this.selector;
    }

    private SScalar firstInitialized() {
        for (int k = 1; ; k++) {
            final String portName = portName(k);
            if (!hasInputPort(portName)) {
                throw new IllegalArgumentException("No initialized input scalar found");
            }
            final SScalar data = getInputScalar(portName, true);
            if (data.isInitialized()) {
                return data;
            }
        }
    }

    private String portName(int index) {
        return INPUT_PORT_PREFIX + index;
    }

}
