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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.List;
import java.util.Objects;

public final class MergeNumbers extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
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

    public MergeNumbers setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    public ResultElementType getResultElementType() {
        return resultElementType;
    }

    public MergeNumbers setResultElementType(ResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
        return this;
    }

    @Override
    public SNumbers processNumbers(List<SNumbers> sources) {
        if (sources.stream().noneMatch(Objects::nonNull)) {
            if (inputRequired) {
                throw new IllegalArgumentException("No non-null input arrays");
            } else {
                return new SNumbers();
            }
        }
        final Class<?> resultElementType = findElementType(sources, this.resultElementType);
        assert resultElementType != null : "must not be null if there are non-null sources: " + sources;
        final int blockLength = sources.stream().filter(Objects::nonNull).findFirst()
                .map(SNumbers::getBlockLength).orElse(1);
        final long n = sources.stream().filter(Objects::nonNull).mapToLong(SNumbers::n).sum();
        if (n > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large summary number of blocks: " + n + " >=2^31");
        }
        final SNumbers result = SNumbers.zeros(resultElementType, (int) n, blockLength);
        int disp = 0;
        for (SNumbers source : sources) {
            if (source != null) {
                if (source.elementType() != resultElementType) {
                    source = source.toPrecision(resultElementType);
                }
                final int numberOfBlocks = source.n();
                result.replaceBlockRange(disp, source, 0, numberOfBlocks);
                disp += numberOfBlocks;
            }
        }
        return result;
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }

    @Override
    protected boolean allowAllUninitializedInputs() {
        return !inputRequired;
    }

    @Override
    protected boolean numberOfBlocksEqualityRequired() {
        return false;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return true;
    }

    static Class<?> findElementType(List<SNumbers> sources, ResultElementType resultElementType) {
        switch (resultElementType) {
            case REQUIRE_IDENTICAL: {
                Class<?> result = null;
                for (SNumbers source : sources) {
                    if (source != null) {
                        if (result == null) {
                            result = source.elementType();
                        } else if (result != source.elementType()) {
                            throw new IllegalArgumentException("Different element type of source arrays: "
                                    + result + "[] and " + source.elementType() + "[]");
                        }
                    }
                }
                return result;
            }
            case FIRST_INPUT: {
                for (SNumbers source : sources) {
                    if (source != null) {
                        return source.elementType();
                    }
                }
                return null;
            }
            default: {
                assert resultElementType.elementType != null;
                return resultElementType.elementType;
            }
        }
    }
}
