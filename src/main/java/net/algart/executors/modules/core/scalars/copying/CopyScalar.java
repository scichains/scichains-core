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

package net.algart.executors.modules.core.scalars.copying;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SScalar;

public class CopyScalar extends Executor {
    private boolean inputRequired = false;

    public CopyScalar() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public CopyScalar setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    @Override
    public void process() {
        final SScalar input = getInputScalar(!inputRequired);
        if (input.isInitialized()) {
            logDebug(() -> "Copying scalar: \"" + input + "\"");
            // Note: input.toString() returns reduced string for very large scalars
            getScalar().setTo(checkResult(input.getValue()));
        } else {
            getScalar().setTo(input);
            // - actually copying null value
        }
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        if (name.equals("requireInput")) {
            return "inputRequired";
        }
        return name;
    }

    String checkResult(String result) {
        return result;
    }
}
