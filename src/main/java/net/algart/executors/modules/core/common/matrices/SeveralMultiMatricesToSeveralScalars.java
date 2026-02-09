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

package net.algart.executors.modules.core.common.matrices;

import net.algart.executors.api.data.SScalar;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;
import java.util.Map;

public abstract class SeveralMultiMatricesToSeveralScalars extends SeveralMultiMatricesProcessing {
    protected SeveralMultiMatricesToSeveralScalars(String... predefinedInputPortNames) {
        super(predefinedInputPortNames);
    }

    public abstract void analyse(Map<String, SScalar> results, List<MultiMatrix> sources);

    @Override
    Object process(List<MultiMatrix> sources, boolean resultRequired) {
        final Map<String, SScalar> result = allOutputContainers(SScalar.class, analyseOnlyRequested());
        analyse(result, sources);
        return null;
    }

    // May be overridden for quick operations
    protected boolean analyseOnlyRequested() {
        return true;
    }

    @Override
    final boolean resultRequired() {
        return false;
    }

    @Override
    final void setNonNullResult(Object result) {
        throw new AssertionError("Must not be called");
    }
}
