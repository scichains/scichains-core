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
import net.algart.executors.api.data.SNumbers;
import net.algart.multimatrix.MultiMatrix;

import java.util.Map;

public abstract class MultiMatrixToSeveralNumbers extends Executor implements ReadOnlyExecutionInput {
    protected MultiMatrixToSeveralNumbers() {
        addInputMat(DEFAULT_INPUT_PORT);
    }

    @Override
    public void process() {
        SMat input = getInputMat();
        final MultiMatrix sourceMultiMatrix = input.toMultiMatrix(allowInputNonAlgartDepth());
        final Map<String, SNumbers> result = allOutputContainers(SNumbers.class, analyseOnlyRequested());
        setStartProcessingTimeStamp();
        analyse(result, sourceMultiMatrix);
        setEndProcessingTimeStamp();
    }

    public abstract void analyse(Map<String, SNumbers> results, MultiMatrix source);

    // May be overridden for quick operations
    protected boolean analyseOnlyRequested() {
        return true;
    }

    protected boolean allowInputNonAlgartDepth() {
        return true;
    }

}
