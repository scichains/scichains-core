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

package net.algart.executors.api.jep;

import jep.NDArray;
import net.algart.executors.api.data.SNumbers;

import java.util.Arrays;
import java.util.Objects;

public class Jep2SNumbers {
    private Jep2SNumbers() {
    }

    public static NDArray<Object> toNDArray(SNumbers numbers) {
        Objects.requireNonNull(numbers, "Null numbers");
        if (!numbers.isInitialized()) {
            throw new IllegalArgumentException("Not initialized numbers");
        }
        final int[] dimensions = {numbers.n(), numbers.getBlockLength()};
        return new NDArray<>(numbers.getArray(), numbers.isUnsigned(), dimensions);
    }

    public static SNumbers toSNumbers(SNumbers numbers) {
        return new SNumbers().setTo(numbers);
    }

    public static SNumbers setToArray(SNumbers result, Object array) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(array, "Null array");
        final NDArray<?> ndArray = wrapUsualArray(array);
        final int[] dimensions = ndArray.getDimensions();
        if (dimensions.length > 2) {
            throw new IllegalArgumentException("Cannot convert array to SNumbers: " +
                    "number of dimensions is greater than 2 " + Arrays.toString(dimensions));
        }
        result.setToArray(ndArray.getData(), dimensions.length == 1 ? 1 : dimensions[1]);
        return result;
    }

    private static NDArray<?> wrapUsualArray(Object array) {
        if (array instanceof NDArray<?>) {
            return (NDArray<?>) array;
        }
        final Class<?> c = array.getClass();
        if (c.isArray()) {
            return new NDArray<>(array, net.algart.arrays.Arrays.isUnsignedElementType(c.getComponentType()));
        }
        throw new UnsupportedOperationException("Unsupported type of array: "
                + array.getClass().getCanonicalName() + " (Java array or NDArray/DirectNDArray expected)");
    }
}
