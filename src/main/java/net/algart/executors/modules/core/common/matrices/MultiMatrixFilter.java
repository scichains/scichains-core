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

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.multimatrix.MultiMatrix;

public abstract class MultiMatrixFilter extends Executor implements ReadOnlyExecutionInput {
    private MultiMatrix sourceMultiMatrix = null;

    protected MultiMatrixFilter() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    protected final MultiMatrix sourceMultiMatrix() {
        return sourceMultiMatrix;
    }

    @Override
    public void process() {
        if (resultRequired()) {
            process(getInputMat(allowUninitializedInput()), getMat());
        } else {
            final SMat result = process(getInputMat(allowUninitializedInput()), null);
            if (result != null) {
                getMat().setTo(result);
            } else if (hasDefaultOutputPort()) {
                getMat().remove();
                // - important to clear result port, if it was not cleared from the previous call
            }
        }
    }

    public final SMat process(SMat source) {
        return process(source, null);
    }

    public abstract MultiMatrix process(MultiMatrix source);

    // May be overridden
    protected boolean allowUninitializedInput() {
        return false;
    }

    // May be overridden
    protected boolean allowInputNonAlgartDepth() {
        return true;
    }

    // May be overridden
    protected boolean resultRequired() {
        return true;
    }

    private SMat process(SMat source, SMat result) {
        this.sourceMultiMatrix = source.toMultiMatrix(allowInputNonAlgartDepth());
        try {
            setStartProcessingTimeStamp();
            final MultiMatrix resultMultiMatrix = process(sourceMultiMatrix);
            setEndProcessingTimeStamp();
            if (resultMultiMatrix == null) {
                if (resultRequired()) {
                    throw new AssertionError("Invalid process implementation: "
                            + "it returned null, though resultRequired=true");
                }
                return null;
            }
            if (result == null) {
                result = new SMat();
            }
            result.setTo(resultMultiMatrix);
            return result;
        } finally {
            this.sourceMultiMatrix = null;
            // - allow garbage collector to free this memory
        }
    }
}
