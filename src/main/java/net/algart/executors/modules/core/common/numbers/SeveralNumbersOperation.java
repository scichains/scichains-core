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

package net.algart.executors.modules.core.common.numbers;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;

import java.util.*;

// At least 1 input array required
public abstract class SeveralNumbersOperation extends Executor {
    public static final String INPUT_PORT_PREFIX = "input_";
    public static final String INDEX_IN_BLOCK_ARGUMENT_PREFIX = "indexInBlock";

    private final String[] predefinedInputPortNames;

    private final Map<Integer, Integer> indexInBlock = new HashMap<>();
    private int lengthInBlock = 0;
    private boolean replaceColumnRangeInInput = false;

    public final int getIndexInBlock(int index) {
        return indexInBlock.getOrDefault(index, 0);
    }

    public final SeveralNumbersOperation setIndexInBlock(int index, int indexInBlock) {
        this.indexInBlock.put(index, indexInBlock);
        return this;
    }

    public final int getLengthInBlock() {
        return lengthInBlock;
    }

    public final SeveralNumbersOperation setLengthInBlock(int lengthInBlock) {
        this.lengthInBlock = nonNegative(lengthInBlock);
        return this;
    }

    public final boolean isReplaceColumnRangeInInput() {
        return replaceColumnRangeInInput;
    }

    public final SeveralNumbersOperation setReplaceColumnRangeInInput(boolean replaceColumnRangeInInput) {
        this.replaceColumnRangeInInput = replaceColumnRangeInInput;
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        final Integer index = indexInBlockArgumentNameToInputIndex(name);
        if (index != null) {
            //noinspection resource
            setIndexInBlock(index, parameters().getInteger(name));
        } else {
            super.onChangeParameter(name);
        }
    }

    protected SeveralNumbersOperation(String... predefinedInputPortNames) {
        Objects.requireNonNull(predefinedInputPortNames, "Null predefinedInputPortNames");
        this.predefinedInputPortNames = predefinedInputPortNames.clone();
        for (String port : predefinedInputPortNames) {
            addInputNumbers(port);
        }
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        final List<SNumbers> sourceList = new ArrayList<>();
        for (int k = 0; requiredNumberOfInputs == null || k < requiredNumberOfInputs; k++) {
            final String portName = inputPortName(k);
            if (requiredNumberOfInputs == null && !hasInputPort(portName)) {
                break;
            }
            sourceList.add(getInputNumbers(portName, allowUninitializedInput(k)));
        }
        setStartProcessingTimeStamp();
        final SNumbers result = process(sourceList.toArray(new SNumbers[0]));
        setEndProcessingTimeStamp();
        getNumbers().exchange(result);
    }

    public final SNumbers process(SNumbers... inputs) {
        Objects.requireNonNull(inputs, "Null inputs array");
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        final int n = Math.max(inputs.length, requiredNumberOfInputs == null ? 0 : requiredNumberOfInputs);
        // - if requiredNumberOfInputs() is specified, allow to skip passing last null arguments
        final Integer requiredBlockLength = requiredBlockLength();
        final boolean blockLengthEqualityRequired = blockLengthEqualityRequired() || requiredBlockLength != null;
        final List<SNumbers> inputList = new ArrayList<>();
        int firstBlockLength = -1;
        int firstNumberOfBlocks = -1;
        SNumbers firstInputWithRange = null;
        IRange firstInputRange = null;
        for (int k = 0; k < n; k++) {
            SNumbers input = k < inputs.length ? inputs[k] : null;
            if (input == null || !input.isInitialized()) {
                inputList.add(null);
            } else {
                final IRange columnRange = selectedColumnRange(k);
                if (columnRange != null) {
                    if (firstInputWithRange == null) {
                        firstInputWithRange = input;
                        firstInputRange = columnRange;
                    }
                    input = input.columnRange((int) columnRange.min(), (int) columnRange.size());
                }
                if (firstBlockLength == -1) {
                    firstBlockLength = input.getBlockLength();
                    if (requiredBlockLength != null && firstBlockLength != requiredBlockLength) {
                        throw new IllegalArgumentException("Illegal block length in source number arrays: "
                                + firstBlockLength + " != " + requiredBlockLength + " (required block length)");
                    }
                    firstNumberOfBlocks = input.n();
                } else {
                    if (firstBlockLength != input.getBlockLength() && blockLengthEqualityRequired) {
                        throw new IllegalArgumentException("Different block length in source number arrays: "
                                + input.getBlockLength() + " != " + firstBlockLength);
                    }
                    if (firstNumberOfBlocks != input.n() && numberOfBlocksEqualityRequired()) {
                        throw new IllegalArgumentException("Different lengths of source number arrays: "
                                + input.n() + " != " + firstNumberOfBlocks);
                    }
                }
                inputList.add(input);
            }
        }
        if (firstBlockLength == -1 && !allowAllUninitializedInputs()) {
            throw new IllegalArgumentException("No initialized input arrays");
        }
        SNumbers result = processNumbers(inputList);
        if (replaceColumnRangeInInput() && firstInputWithRange != null) {
            assert firstBlockLength != -1;
            SNumbers corrected = firstInputWithRange.toPrecision(result.elementType());
            corrected.replaceColumnRange((int) firstInputRange.min(), result, 0, firstBlockLength);
            result = corrected;
        }
        return result;
    }

    // Unlike process method above, here we have only initialized arrays or nulls
    // with identical (when required) blockLength and arrayLength
    protected abstract SNumbers processNumbers(List<SNumbers> sources);

    protected Integer requiredNumberOfInputs() {
        return predefinedInputPortNames.length == 0 ? null : predefinedInputPortNames.length;
    }

    // May be overridden
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }

    // May be overridden
    protected boolean allowAllUninitializedInputs() {
        return false;
    }

    // May be overridden
    protected String inputPortName(int inputIndex) {
        return inputIndex < predefinedInputPortNames.length ?
                predefinedInputPortNames[inputIndex] :
                INPUT_PORT_PREFIX + (inputIndex + 1);
    }

    // May be overridden TOGETHER with indexInBlockArgumentNameToInputIndex
    protected String indexInBlockArgumentName(int inputIndex) {
        if (inputIndex < predefinedInputPortNames.length) {
            String port = predefinedInputPortNames[inputIndex];
            if (port != null) {
                if (port.length() > 0) {
                    // capitalize first letter
                    port = port.substring(0, 1).toUpperCase() + port.substring(1);
                }
                return INDEX_IN_BLOCK_ARGUMENT_PREFIX + port;
            }
        }
        return INDEX_IN_BLOCK_ARGUMENT_PREFIX + (inputIndex + 1);
    }

    protected Integer indexInBlockArgumentNameToInputIndex(String argumentName) {
        if (argumentName == null) {
            return null;
        }
        for (int k = 0; k < predefinedInputPortNames.length; k++) {
            if (argumentName.equals(indexInBlockArgumentName(k))) {
                return k;
            }
        }
        if (argumentName.startsWith(INDEX_IN_BLOCK_ARGUMENT_PREFIX)) {
            try {
                return Integer.parseInt(argumentName.substring(INDEX_IN_BLOCK_ARGUMENT_PREFIX.length())) - 1;
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    // Should be overridden to process arguments with different sizes
    protected boolean numberOfBlocksEqualityRequired() {
        return true;
    }

    // Should be overridden to process arguments with different block lengths
    protected boolean blockLengthEqualityRequired() {
        return true;
    }

    // Should be overridden to require some given block length for all sources.
    protected Integer requiredBlockLength() {
        return null;
    }

    // If returns some range, the input array #inputIndex will be replaced by its columns from this range.
    // In this case, the corresponding blockLength in process() method will be the size of this range.
    protected IRange selectedColumnRange(int inputIndex) {
        if (lengthInBlock <= 0) {
            return null;
        }
        final int start = getIndexInBlock(inputIndex);
        return IRange.of(start, start + lengthInBlock - 1);
    }

    // If true, the result is produced by replacing selectedColumnRange(k)
    // in the first array input #k, where is it not null
    protected boolean replaceColumnRangeInInput() {
        return replaceColumnRangeInInput;
    }
}
