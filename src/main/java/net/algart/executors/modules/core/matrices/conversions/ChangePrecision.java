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
    public static final String OUTPUT_ELEMENT_TYPE = "element_type";
    private boolean rawCast = false;
    private Class<?> elementType = byte.class;
    private boolean inputRequired = false;

    public ChangePrecision() {
        addOutputScalar(OUTPUT_ELEMENT_TYPE);
    }

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

    /**
     * Note: <code>null</code> value is allowed, it means "unchanged".
     */
    public ChangePrecision setElementType(Class<?> elementType) {
        this.elementType = elementType;
        return this;
    }

    public ChangePrecision setElementType(String elementType) {
        return setElementType(MultiMatrixGenerator.elementType(elementType, true));
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public ChangePrecision setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (source == null) {
            return null;
        }
        if (elementType == null || elementType == source.elementType()) {
            setOutputScalar(OUTPUT_ELEMENT_TYPE, source.elementType().getSimpleName());
            return source;
        }
        setOutputScalar(OUTPUT_ELEMENT_TYPE, elementType.getSimpleName());
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
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return !inputRequired;
    }

    @Override
    protected boolean resultRequired() {
        return inputRequired;
    }
}
