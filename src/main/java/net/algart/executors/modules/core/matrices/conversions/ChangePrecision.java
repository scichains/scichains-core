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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelFilter;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;

public final class ChangePrecision extends MultiMatrixChannelFilter {
    private boolean rawCast = false;
    private Class<?> elementType = byte.class;
    private boolean requireInput = false;

    public boolean isRawCast() {
        return rawCast;
    }

    public ChangePrecision setRawCast(boolean rawCast) {
        this.rawCast = rawCast;
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public ChangePrecision setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public ChangePrecision setElementType(String elementType) {
        return setElementType(MultiMatrixGenerator.elementType(elementType));
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public ChangePrecision setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (source == null) {
            return null;
        }
        if (elementType == source.elementType()) {
            return source;
        }
        if (rawCast) {
            return super.process(source);
            // i.e. using processChannel below
        } else {
            logDebug(
                    () -> "Changing precision " + source.elementType().getSimpleName() + " -> "
                            + elementType.getSimpleName() + " for matrix " + source);
            return source.toPrecisionIfNot(elementType);
        }
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        if (!rawCast) {
            throw new UnsupportedOperationException("Must be called ONLY in rawCast mode");
        }
        if (currentChannel() == 0) {
            logDebug(
                    () -> "Change precision (raw cast) " + m.elementType().getSimpleName() + " -> "
                            + elementType.getSimpleName() + " for matrix "
                            + numberOfChannels() + "x" + m.dimX() + "x" + m.dimY());
        }
        final Class<PArray> newType = Arrays.type(PArray.class, elementType);
        return Matrices.clone(Matrices.asFuncMatrix(Func.IDENTITY, newType, m));
    }

    @Override
    protected boolean allowUninitializedInput() {
        return !requireInput;
    }

    @Override
    protected boolean resultRequired() {
        return requireInput;
    }
}
