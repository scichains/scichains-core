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

package net.algart.executors.modules.core.matrices.geometry;

import net.algart.arrays.Matrix;

public enum ContinuationMode {
    DEFAULT(null),
    CYCLIC(Matrix.ContinuationMode.CYCLIC),
    MIRROR_CYCLIC(Matrix.ContinuationMode.MIRROR_CYCLIC),
    ZERO_CONSTANT(Matrix.ContinuationMode.ZERO_CONSTANT),
    POSITIVE_INFINITY(Matrix.ContinuationMode.getConstantMode(Double.POSITIVE_INFINITY)),
    NEGATIVE_INFINITY(Matrix.ContinuationMode.getConstantMode(Double.NEGATIVE_INFINITY)),
    NAN_CONSTANT(Matrix.ContinuationMode.NAN_CONSTANT);

    private final Matrix.ContinuationMode continuationMode;

    public Matrix.ContinuationMode continuationModeOrNull() {
        return continuationMode;
    }

    public Matrix.ContinuationMode continuationMode() {
        return continuationMode == null ? Matrix.ContinuationMode.PSEUDO_CYCLIC : continuationMode;
    }

    ContinuationMode(Matrix.ContinuationMode continuationMode) {
        this.continuationMode = continuationMode;
    }
}
