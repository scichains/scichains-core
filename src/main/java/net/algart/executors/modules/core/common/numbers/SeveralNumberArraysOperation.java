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

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.PNumberArray;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.executors.api.data.SNumbers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class SeveralNumberArraysOperation extends SeveralNumbersOperation {
    protected SeveralNumberArraysOperation(String... predefinedInputPortNames) {
        super(predefinedInputPortNames);
    }

    protected SNumbers processNumbers(List<SNumbers> sources) {
        Objects.requireNonNull(sources, "Null sources list");
        final List<PNumberArray> sourceList = new ArrayList<>();
        final int[] blockLengthsArray = new int[sources.size()];
        int firstBlockLength = -1;
        int k = 0;
        for (SNumbers source : sources) {
            if (source == null) {
                sourceList.add(null);
                blockLengthsArray[k] = 0;
            } else {
                if (firstBlockLength == -1) {
                    firstBlockLength = source.getBlockLength();
                }
                sourceList.add((PNumberArray) SimpleMemoryModel.asUpdatableArray(source.getArray()));
                blockLengthsArray[k] = source.getBlockLength();
            }
            k++;
        }
        if (firstBlockLength == -1) {
            throw new IllegalArgumentException("No initialized input arrays");
        }
        final PArray array = process(sourceList, blockLengthsArray);
        final Integer resultBlockLength = resultBlockLength();
        return SNumbers.valueOfArray(
                Arrays.toJavaArray(array),
                resultBlockLength == null ? firstBlockLength : resultBlockLength);
    }

    protected abstract PArray process(List<PNumberArray> sources, int... blockLengths);

    // Override this to specify custom resulting block length (in another case, block length of the
    // first argument will be used).
    protected Integer resultBlockLength() {
        return null;
    }
}
