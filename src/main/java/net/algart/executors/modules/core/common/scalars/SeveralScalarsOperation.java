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

package net.algart.executors.modules.core.common.scalars;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class SeveralScalarsOperation extends Executor {
    public static final String INPUT_PORT_PREFIX = "s";

    private final String[] predefinedInputPortNames;

    protected SeveralScalarsOperation(String... predefinedInputPortNames) {
        Objects.requireNonNull(predefinedInputPortNames, "Null predefinedInputPortNames");
        this.predefinedInputPortNames = predefinedInputPortNames.clone();
        for (String port : predefinedInputPortNames) {
            addInputScalar(port);
        }
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        final boolean replaceWithNull = replaceUninitializedInputWithNull();
        final List<SScalar> sourceList = new ArrayList<>();
        for (int k = 0; requiredNumberOfInputs == null || k < requiredNumberOfInputs; k++) {
            final String portName = inputPortName(k);
            if (requiredNumberOfInputs == null && !hasInputPort(portName)) {
                break;
            }
            final SScalar input = getInputScalar(portName, allowUninitializedInput(k));
            sourceList.add(replaceWithNull && !input.isInitialized() ? null : input);
        }
        setStartProcessingTimeStamp();
        final SScalar result = process(sourceList);
        setEndProcessingTimeStamp();
        getScalar().setTo(result);
    }

    public abstract SScalar process(List<SScalar> sources);

    protected Integer requiredNumberOfInputs() {
        return predefinedInputPortNames.length == 0 ? null : predefinedInputPortNames.length;
    }

    // May be overridden
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }

    protected boolean replaceUninitializedInputWithNull() {
        return false;
    }

    // May be overridden
    protected String inputPortName(int inputIndex) {
        return inputIndex < predefinedInputPortNames.length ?
                predefinedInputPortNames[inputIndex] :
                INPUT_PORT_PREFIX + (inputIndex + 1);
    }
}
