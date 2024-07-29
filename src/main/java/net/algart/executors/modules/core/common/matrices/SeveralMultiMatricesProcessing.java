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

package net.algart.executors.modules.core.common.matrices;

import net.algart.arrays.SizeMismatchException;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class SeveralMultiMatricesProcessing extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_PORT_PREFIX = "input_";

    private final String[] predefinedInputPortNames;
    private List<MultiMatrix> sourceMultiMatrices = null;
    // - elements may be null

    protected SeveralMultiMatricesProcessing(String[] predefinedInputPortNames) {
        Objects.requireNonNull(predefinedInputPortNames, "Null predefinedInputPortNames");
        this.predefinedInputPortNames = predefinedInputPortNames.clone();
        for (String port : predefinedInputPortNames) {
            addInputMat(port);
        }
    }

    protected final List<MultiMatrix> sourceMultiMatrices() {
        return sourceMultiMatrices;
    }

    @Override
    public void process() {
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        this.sourceMultiMatrices = new ArrayList<>();
        try {
            for (int k = 0; requiredNumberOfInputs == null || k < requiredNumberOfInputs; k++) {
                final String portName = inputPortName(k);
                if (requiredNumberOfInputs == null && !hasInputPort(portName)) {
                    break;
                }
                final SMat input = getInputMat(portName, allowUninitializedInput(k));
                this.sourceMultiMatrices.add(input.isInitialized() ?
                        input.toMultiMatrix(allowInputNonAlgartDepth(k)) :
                        null);
            }
            if (dimensionsEqualityRequired()) {
                checkDimensionOfNonNullEquality(sourceMultiMatrices);
            }
            final boolean resultRequired = resultRequired();
            setStartProcessingTimeStamp();
            final Object result = process(sourceMultiMatrices, resultRequired);
            setEndProcessingTimeStamp();
            if (result != null) {
                setNonNullResult(result);
            } else {
                if (resultRequired) {
                    throw new AssertionError("Invalid process implementation: "
                            + "it returned null, though resultRequired=true");
                }
                if (hasDefaultOutputPort()) {
                    getMat().remove();
                    // - important to clear result port, if it was not cleared from the previous call
                }
            }
        } finally {
            this.sourceMultiMatrices = null;
            // - allow garbage collector to free this memory
        }
    }

    protected Integer requiredNumberOfInputs() {
        return predefinedInputPortNames.length == 0 ? null : predefinedInputPortNames.length;
    }

    // May be overridden
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex >= predefinedInputPortNames.length;
    }

    // May be overridden
    protected boolean allowInputNonAlgartDepth(int inputIndex) {
        return true;
    }

    // May be overridden
    protected String inputPortName(int inputIndex) {
        return inputIndex < predefinedInputPortNames.length ?
                predefinedInputPortNames[inputIndex] :
                INPUT_PORT_PREFIX + (inputIndex + 1);
    }

    // Should be overridden to process arguments of different sizes
    protected boolean dimensionsEqualityRequired() {
        return true;
    }

    // Result type depends on this method; may be null
    abstract Object process(List<MultiMatrix> sources, boolean resultRequired);

    abstract boolean resultRequired();

    abstract void setNonNullResult(Object result);

    private static void checkDimensionOfNonNullEquality(List<? extends MultiMatrix> matrices) {
        Objects.requireNonNull(matrices);
        MultiMatrix first = null;
        int firstIndex = -1;
        int index = -1;
        for (MultiMatrix m : matrices) {
            index++;
            if (m == null) {
                continue;
            }
            if (first == null) {
                first = m;
                firstIndex = index;
            } else if (!m.dimEquals(first)) {
                throw new SizeMismatchException("The multi-matrix #" + index + " and #" + firstIndex
                        + " dimensions mismatch: #" + index + " is " + m + ", #" + firstIndex + " is " + first);
            }
        }
    }
}
