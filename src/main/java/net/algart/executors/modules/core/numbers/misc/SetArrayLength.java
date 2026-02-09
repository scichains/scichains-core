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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.arrays.Arrays;
import net.algart.arrays.PNumberArray;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.functions.ConstantFunc;

import java.util.ArrayList;
import java.util.List;

public final class SetArrayLength extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_PORT_PREFIX = "input_";
    public static final String OUTPUT_PORT_PREFIX = "output_";

    public enum FillingMode {
        ZEROS(0.0),
        NAN(Double.NaN),
        CUSTOM(null);

        private final Double filler;

        FillingMode(Double filler) {
            this.filler = filler;
        }

        public double filler(double defaultValue) {
            return filler != null ? filler : defaultValue;
        }
    }

    private int minNumberOfBlocks = 0;
    private int maxNumberOfBlocks = Integer.MAX_VALUE;
    private FillingMode fillingMode = FillingMode.NAN;
    private double filler = 0.0;

    public int getMinNumberOfBlocks() {
        return minNumberOfBlocks;
    }

    public SetArrayLength setMinNumberOfBlocks(int minNumberOfBlocks) {
        this.minNumberOfBlocks = nonNegative(minNumberOfBlocks);
        return this;
    }

    public SetArrayLength setMinNumberOfBlocks(String minNumberOfBlocks) {
        this.minNumberOfBlocks = nonNegative(intOrDefault(minNumberOfBlocks, 0));
        return this;
    }

    public int getMaxNumberOfBlocks() {
        return maxNumberOfBlocks;
    }

    public SetArrayLength setMaxNumberOfBlocks(int maxNumberOfBlocks) {
        this.maxNumberOfBlocks = nonNegative(maxNumberOfBlocks);
        return this;
    }

    public SetArrayLength setMaxNumberOfBlocks(String maxNumberOfBlocks) {
        this.maxNumberOfBlocks = nonNegative(intOrDefault(maxNumberOfBlocks, Integer.MAX_VALUE));
        return this;
    }

    public FillingMode getFillingMode() {
        return fillingMode;
    }

    public SetArrayLength setFillingMode(FillingMode fillingMode) {
        this.fillingMode = nonNull(fillingMode);
        return this;
    }

    public double getFiller() {
        return filler;
    }

    public SetArrayLength setFiller(double filler) {
        this.filler = filler;
        return this;
    }

    @Override
    public void process() {
        final List<SNumbers> sourceList = new ArrayList<>();
        int requiredNumberOfBlocks = 0;
        for (int k = 0; ; k++) {
            final String portName = inputPortName(k);
            if (!hasInputPort(portName)) {
                break;
            }
            SNumbers numbers = getInputNumbers(portName, true);
            if (numbers.isInitialized()) {
                requiredNumberOfBlocks = Math.max(requiredNumberOfBlocks, numbers.n());
            }
            sourceList.add(numbers);
        }
        requiredNumberOfBlocks = Math.max(requiredNumberOfBlocks, minNumberOfBlocks);
        requiredNumberOfBlocks = Math.min(requiredNumberOfBlocks, maxNumberOfBlocks);
        for (int k = 0, n = sourceList.size(); k < n; k++) {
            final String portName = outputPortName(k);
            if (!hasOutputPort(portName)) {
                continue;
            }
            SNumbers source = sourceList.get(k);
            if (source.isInitialized()) {
                final SNumbers result = SNumbers.zeros(
                        source.elementType(), requiredNumberOfBlocks, source.getBlockLength());
                if (fillingMode != FillingMode.ZEROS) {
                    result.setTo(Arrays.asIndexFuncArray(
                            ConstantFunc.getInstance(fillingMode.filler(this.filler)),
                            Arrays.type(PNumberArray.class, result.elementType()),
                            result.getArrayLength()), result.getBlockLength());
                }
                result.replaceBlockRange(0, source, 0,
                        Math.min(source.n(), requiredNumberOfBlocks));
                getNumbers(portName).exchange(result);
            }
        }
    }

    @Override
    public String visibleOutputPortName() {
        return firstInitializedPortName();
    }

    public String inputPortName(int inputIndex) {
        return INPUT_PORT_PREFIX + (inputIndex + 1);
    }

    public String outputPortName(int outputIndex) {
        return OUTPUT_PORT_PREFIX + (outputIndex + 1);
    }

    private String firstInitializedPortName() {
        for (int k = 0; ; k++) {
            final String portName = inputPortName(k);
            if (!hasInputPort(portName)) {
                break;
            }
            if (getInputNumbers(portName, true).isInitialized()) {
                return outputPortName(k);
            }
        }
        return null;
    }
}
