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

package net.algart.executors.modules.core.common.numbers;

import net.algart.arrays.*;
import net.algart.executors.api.data.SNumbers;

public abstract class NumberArrayFilter extends NumbersFilter {
    protected NumberArrayFilter() {
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final int blockLength = source.getBlockLength();
        final Object result = processJavaArray(source.getArray(), blockLength, source.n());
        if (result == null) {
            return null;
        }
        final Integer resultBlockLength = resultBlockLength();
        return SNumbers.valueOfArray(result, resultBlockLength == null ? blockLength : resultBlockLength);
    }

    public final Object processUnstructuredJavaArray(Object javaArray) {
        return processJavaArray(javaArray, 1, java.lang.reflect.Array.getLength(javaArray));
    }

    public final Object processJavaArray(Object javaArray, int blockLength, int numberOfBlocks) {
        final UpdatablePNumberArray array = javaArray == null ? null : PNumberArray.as(javaArray);
        final PArray result = process(array, blockLength, numberOfBlocks);
        return result == null ? null : result.ja();
    }

    // Really result must be one of types, supported by SNumbers
    public abstract PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks);

    // Override this to specify custom resulting block length (in other case, block length of the
    // first argument will be used).
    protected Integer resultBlockLength() {
        return null;
    }

}
