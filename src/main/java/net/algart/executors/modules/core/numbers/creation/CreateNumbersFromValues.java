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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;

import java.util.HashMap;
import java.util.Map;

public final class CreateNumbersFromValues extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_PORT_PREFIX = "v";
    // Note: other port names MUST NOT starts with this prefix!

    private int blockLength = 1;
    private boolean singleBlock = false;
    private int numberOfBlocks = 100;
    private Class<?> elementType = double.class;
    private final Map<Integer, Double> values = new HashMap<>();

    public CreateNumbersFromValues() {
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public CreateNumbersFromValues setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public boolean isSingleBlock() {
        return singleBlock;
    }

    public CreateNumbersFromValues setSingleBlock(boolean singleBlock) {
        this.singleBlock = singleBlock;
        return this;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public CreateNumbersFromValues setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = nonNegative(numberOfBlocks);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public CreateNumbersFromValues setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public CreateNumbersFromValues setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public double getValue(int index) {
        return values.getOrDefault(index, 0.0);
    }

    public CreateNumbersFromValues setValue(int index, double value) {
        this.values.put(index, value);
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.startsWith(INPUT_PORT_PREFIX)) {
            final int index;
            try {
                index = Integer.parseInt(name.substring(INPUT_PORT_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                return;
            }
            setValue(index, parameters().getDouble(name));
            return;
        }
        super.onChangeParameter(name);
    }

    @Override
    public void process() {
        if ((long) blockLength * (long) numberOfBlocks > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("numberOfBlocks * blockLength = "
                    + numberOfBlocks + " * " + blockLength + " >= 2^31");
        }
        final int length = blockLength * numberOfBlocks;
        final double[] values = new double[length];
        for (int k = 0; k < values.length; k++) {
            values[k] = getValue(k);
        }
        getNumbers().setToArray(values, singleBlock ? values.length : blockLength).setPrecision(elementType);
    }
}
