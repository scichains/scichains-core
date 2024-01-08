/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;

public final class SplitNumbersColumns extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_PORT_PREFIX = "output_";

    private boolean requireInput = true;

    public SplitNumbersColumns() {
        useVisibleResultParameter();
        addInputNumbers(DEFAULT_INPUT_PORT);
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public SplitNumbersColumns setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public void process() {
        final SNumbers numbers = getInputNumbers(!requireInput);
        if (!numbers.isInitialized()) {
            return;
        }
        for (int k = 0, m = numbers.getBlockLength(); k < m; k++) {
            final String portName = outputPortName(k);
            if (isOutputNecessary(portName)) {
                getNumbers(portName).setTo(numbers.column(k));
            }
        }
    }

    private String outputPortName(int outputIndex) {
        return OUTPUT_PORT_PREFIX + (outputIndex + 1);
    }
}
