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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;

import java.util.Arrays;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class CreateRandomNumbers extends Executor implements ReadOnlyExecutionInput {
    private int blockLength = 1;
    private int numberOfBlocks = 100;
    private Class<?> elementType = float.class;
    private double min = 0.0;
    private double max = 1.0;
    private long randSeed = 0;
    // 0 means really random: new sequence for each call

    public CreateRandomNumbers() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public int getBlockLength() {
        return blockLength;
    }

    public CreateRandomNumbers setBlockLength(int blockLength) {
        this.blockLength = positive(blockLength);
        return this;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public CreateRandomNumbers setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = nonNegative(numberOfBlocks);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public CreateRandomNumbers setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
        return this;
    }

    public CreateRandomNumbers setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    public double getMin() {
        return min;
    }

    public CreateRandomNumbers setMin(double min) {
        this.min = min;
        return this;
    }

    public double getMax() {
        return max;
    }

    public CreateRandomNumbers setMax(double max) {
        this.max = max;
        return this;
    }

    public long getRandSeed() {
        return randSeed;
    }

    public CreateRandomNumbers setRandSeed(long randSeed) {
        this.randSeed = randSeed;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            if ((long) blockLength * (long) numberOfBlocks > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("numberOfBlocks * blockLength = "
                        + numberOfBlocks + " * " + blockLength + " >= 2^31");
            }
            long t1 = debugTime();
            final int length = blockLength * numberOfBlocks;
            final float[] floats = new float[length];
            long t2 = debugTime();
            if (min == max) {
                Arrays.fill(floats, (float) min);
            } else if (min > max) {
                throw new IllegalArgumentException("min value > max value");
            } else {
                final RandomGenerator random = randSeed == 0 ?
                        new SplittableRandom() :
                        new SplittableRandom(randSeed);
                // - we don't use ThreadLocalRandom: SonarQube considers that it is not secure
                for (int k = 0; k < floats.length; k++) {
                    floats[k] = (float) random.nextDouble(min, max);
                }
            }
            long t3 = debugTime();
            setEndProcessingTimeStamp();
            getNumbers().setToArray(floats, blockLength).setPrecision(elementType);
            long t4 = debugTime();
            logDebug(() -> String.format(Locale.US,
                    "Generating %d random numbers (%d x %d), %s: " +
                            "%.3f ms = %.3f ms allocating + %.3f ms generating (%.2f ns/element) + %.3f ms returning",
                    length, blockLength, numberOfBlocks,
                    randSeed == 0 ? "random initial value" : "stable initial value [" + randSeed + "]",
                    (t4 - t1) * 1e-6,
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (double) (t3 - t2) / length, (t4 - t3) * 1e-6));
        }
    }
}
