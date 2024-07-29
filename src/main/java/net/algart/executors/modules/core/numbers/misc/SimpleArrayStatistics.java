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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.arrays.Arrays;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;

import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

public enum SimpleArrayStatistics {
    MEAN("mean", n -> Arrays.sumOf(n.asNumberArray()) / n.getArrayLength()),
    SUM("sum", n -> Arrays.sumOf(n.asNumberArray())),
    MIN("min", (n, r) ->
            n.minInColumnRange((int) r.min(), (int) r.size(), false)),
    MAX("max", (n, r) ->
            n.maxInColumnRange((int) r.min(), (int) r.size(), false)),
    MAX_ABS("max_abs", (n, r) ->
            n.maxAbsInColumnRange((int) r.min(), (int) r.size(), false)),
    MIN_OF_ORDINARY("min_of_ordinary", (n, r) ->
            n.minInColumnRange((int) r.min(), (int) r.size(), true)),
    MAX_OF_ORDINARY("max_of_ordinary", (n, r) ->
            n.maxInColumnRange((int) r.min(), (int) r.size(), true)),
    MAX_ABS_OF_ORDINARY("max_abs_of_ordinary", (n, r) ->
            n.maxAbsInColumnRange((int) r.min(), (int) r.size(), true)),
    HASH("hash", SNumbers::hashCode);

    private final boolean quickRangeInBlockProcessing;
    private final String statisticsName;
    private final ToDoubleFunction<SNumbers> fullStatistics;
    private final ToDoubleBiFunction<SNumbers, IRange> rangeStatistics;

    SimpleArrayStatistics(String statisticsName, ToDoubleFunction<SNumbers> fullStatistics) {
        this.statisticsName = statisticsName;
        this.quickRangeInBlockProcessing = false;
        this.fullStatistics = fullStatistics;
        this.rangeStatistics = null;
    }

    SimpleArrayStatistics(String statisticsName, ToDoubleBiFunction<SNumbers, IRange> rangeStatistics) {
        this.statisticsName = statisticsName;
        this.quickRangeInBlockProcessing = true;
        this.fullStatistics = null;
        this.rangeStatistics = rangeStatistics;
    }

    public String statisticsName() {
        return statisticsName;
    }

    public boolean isQuickRangeInBlockProcessing() {
        return quickRangeInBlockProcessing;
    }

    public double statistics(SNumbers numbers, int indexInBlock, int lengthInBlock) {
        numbers.checkStartIndexAndLenthInBlock(indexInBlock, lengthInBlock, true);
        if (quickRangeInBlockProcessing) {
            final IRange range = IRange.valueOf(indexInBlock, indexInBlock + lengthInBlock - 1);
            return rangeStatistics.applyAsDouble(numbers, range);
        } else {
            if (indexInBlock != 0 || lengthInBlock != numbers.getBlockLength()) {
                numbers = numbers.columnRange(indexInBlock, lengthInBlock);
            }
            return fullStatistics.applyAsDouble(numbers);
        }
    }
}
