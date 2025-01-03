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

package net.algart.executors.modules.core.common.matrices;

import net.algart.multimatrix.MultiMatrix;

import java.util.Arrays;
import java.util.List;

public abstract class SeveralMultiMatricesOperation extends SeveralMultiMatricesProcessing {
    protected SeveralMultiMatricesOperation(String... predefinedInputPortNames) {
        super(predefinedInputPortNames);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public final MultiMatrix process(MultiMatrix... sources) {
        return process(Arrays.asList(sources));
    }

    public abstract MultiMatrix process(List<MultiMatrix> sources);

    @Override
    protected boolean resultRequired() {
        return true;
    }

    @Override
    Object process(List<MultiMatrix> sources, boolean resultRequired) {
        return process(sources);
    }

    @Override
    final void setNonNullResult(Object result) {
        assert result instanceof MultiMatrix;
        getMat().setTo((MultiMatrix) result);
    }
}
