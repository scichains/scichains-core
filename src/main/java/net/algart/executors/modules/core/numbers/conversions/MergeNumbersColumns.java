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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.*;

public final class MergeNumbersColumns extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String NUMBER_OF_EMPTY_COLUMNS_FOR_NON_INITIALIZED_PREFIX = "numberOfEmptyColumns";

    private boolean inputRequired = true;
    private MergeNumbers.ResultElementType resultElementType = MergeNumbers.ResultElementType.FIRST_INPUT;
    private final Map<Integer, Integer> numberOfEmptyColumnsForNonInitialized = new HashMap<>();

    public boolean isInputRequired() {
        return inputRequired;
    }

    public MergeNumbersColumns setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    public MergeNumbers.ResultElementType getResultElementType() {
        return resultElementType;
    }

    public MergeNumbersColumns setResultElementType(MergeNumbers.ResultElementType resultElementType) {
        this.resultElementType = nonNull(resultElementType);
        return this;
    }

    public int getNumberOfEmptyColumnsForNonInitialized(int index) {
        return numberOfEmptyColumnsForNonInitialized.getOrDefault(index, 0);
    }

    public MergeNumbersColumns setNumberOfEmptyColumnsForNonInitialized(
            int index,
            int numberOfEmptyColumnsForNonInitialized) {
        this.numberOfEmptyColumnsForNonInitialized.put(index, numberOfEmptyColumnsForNonInitialized);
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.startsWith(NUMBER_OF_EMPTY_COLUMNS_FOR_NON_INITIALIZED_PREFIX)) {
            final int index;
            try {
                index = Integer.parseInt(
                        name.substring(NUMBER_OF_EMPTY_COLUMNS_FOR_NON_INITIALIZED_PREFIX.length())) - 1;
            } catch (NumberFormatException ignored) {
                return;
            }
            final int value = parameters().getInteger(name);
            //noinspection resource
            setNumberOfEmptyColumnsForNonInitialized(index, nonNegative(value, "number of empty columns"));
            return;
        }
        super.onChangeParameter(name);
    }

    @Override
    public SNumbers processNumbers(List<SNumbers> sources) {
        if (sources.stream().noneMatch(Objects::nonNull)) {
            if (inputRequired) {
                throw new IllegalArgumentException("There are no initialized input arrays");
            } else {
                return new SNumbers();
            }
        }
        final Class<?> resultElementType = MergeNumbers.findElementType(sources, this.resultElementType);
        assert resultElementType != null : "must not be null if there are non-null sources: " + sources;
        final int n = sources.stream().filter(Objects::nonNull).findFirst().map(SNumbers::n).orElse(0);
        final List<SNumbers> actualSources = new ArrayList<>();
        long resultBlockLength = 0;
        for (int i = 0, size = sources.size(); i < size; i++) {
            SNumbers source = sources.get(i);
            if (source != null) {
                actualSources.add(source);
                resultBlockLength += source.getBlockLength();
            } else {
                final int blockLength = numberOfEmptyColumnsForNonInitialized.getOrDefault(i, 0);
                if (blockLength > 0) {
                    actualSources.add(SNumbers.zeros(resultElementType, n, blockLength).fillValue(Double.NaN));
                    resultBlockLength += blockLength;
                }
            }
        }
        if (resultBlockLength > Integer.MAX_VALUE) {
            // - very improbable
            throw new IllegalArgumentException("Too large total number of channels: " + resultBlockLength + " >=2^31");
        }
        final SNumbers result = SNumbers.zeros(resultElementType, n, (int) resultBlockLength);
        int blockDisp = 0;
        for (SNumbers source : actualSources) {
            assert source != null;
            if (source.elementType() != resultElementType) {
                source = source.toPrecision(resultElementType);
            }
            final int blockLength = source.getBlockLength();
            result.replaceColumnRange(blockDisp, source, 0, blockLength);
            blockDisp += blockLength;
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
        return true;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

}
