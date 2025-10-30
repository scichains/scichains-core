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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.OptionalArguments;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MergeChannelsGroups extends SeveralMultiMatricesOperation {
    public enum ResultElementType {
        REQUIRE_IDENTICAL(null),
        FIRST_INPUT(null),
        INT(int.class),
        FLOAT(float.class),
        DOUBLE(double.class);

        final Class<?> elementType;

        ResultElementType(Class<?> elementType) {
            this.elementType = elementType;
        }
    }

    private boolean inputRequired = true;
    private ResultElementType resultElementType = ResultElementType.FIRST_INPUT;

    public boolean isInputRequired() {
        return inputRequired;
    }

    public MergeChannelsGroups setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    public ResultElementType getResultElementType() {
        return resultElementType;
    }

    public void setResultElementType(ResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        sources = new OptionalArguments<>(sources).extract();
        if (sources.isEmpty()) {
            if (inputRequired) {
                throw new IllegalArgumentException("No source matrices");
            } else {
                return null;
            }
        }
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        Class<?> firstElementType = null;
        for (MultiMatrix source : sources) {
            if (firstElementType == null) {
                firstElementType = source.elementType();
            }
            switch (resultElementType) {
                case REQUIRE_IDENTICAL: {
                    // nothing to do: MultiMatrix.valueOf will check types
                    break;
                }
                case FIRST_INPUT: {
                    source = source.asPrecision(firstElementType);
                    break;
                }
                default: {
                    source = source.asPrecision(resultElementType.elementType);
                    break;
                }
            }
            result.addAll(source.allChannels());
        }
        return MultiMatrix.of(result);
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }
}
