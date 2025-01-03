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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;

public final class CreateNumbersFromString extends Executor implements ReadOnlyExecutionInput {
    private int blockLength = 1;
    private boolean singleBlock = true;
    private Class<?> elementType = float.class;
    private String value = "";

    public CreateNumbersFromString() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public CreateNumbersFromString setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public boolean isSingleBlock() {
        return singleBlock;
    }

    public CreateNumbersFromString setSingleBlock(boolean singleBlock) {
        this.singleBlock = singleBlock;
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public CreateNumbersFromString setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public CreateNumbersFromString setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public String getValue() {
        return value;
    }

    public CreateNumbersFromString setValue(String value) {
        this.value = nonNull(value);
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
            return;
        }
        if (elementType == long.class) {
            // - the only type than cannot be precisely represented by double
            final long[] longs;
            try {
                longs = new SScalar(value).toLongs();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal numbers in string \"" + value
                        + "\": for \"long\" element type you must use correct integer values"
                        + " (in particular. automatic type cast is not supported)", e);
            }
            getNumbers().setTo(longs, singleBlock ? longs.length : blockLength);
        } else {
            final double[] doubles;
            try {
                doubles = new SScalar(value).toDoubles();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal numbers in string \"" + value + "\"", e);
            }
            getNumbers().setToArray(doubles, singleBlock ? doubles.length : blockLength).setPrecision(elementType);
        }
    }
}
