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

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumbersToScalar;

import java.util.Arrays;
import java.util.Locale;

public final class NumbersInfo extends NumbersToScalar implements ReadOnlyExecutionInput {
    public static final String OUTPUT_N = "n";
    public static final String OUTPUT_BLOCK_LENGTH = "block_length";
    public static final String OUTPUT_ARRAY_LENGTH = "array_length";
    public static final String OUTPUT_ELEMENT_TYPE = "element_type";
    public static final String OUTPUT_MAX_POSSIBLE = "max_possible";

    private int indexInBlock = 0;
    private int lengthInBlock = 0;

    public NumbersInfo() {
        useVisibleResultParameter();
        setDefaultOutputScalar(OUTPUT_N);
        addOutputScalar(OUTPUT_BLOCK_LENGTH);
        addOutputScalar(OUTPUT_ARRAY_LENGTH);
        addOutputScalar(OUTPUT_ELEMENT_TYPE);
        addOutputScalar(OUTPUT_MAX_POSSIBLE);
        for (SimpleArrayStatistics statistics : SimpleArrayStatistics.values()) {
            addOutputScalar(statistics.statisticsName());
        }
    }

    public int getIndexInBlock() {
        return indexInBlock;
    }

    public NumbersInfo setIndexInBlock(int indexInBlock) {
        this.indexInBlock = nonNegative(indexInBlock);
        return this;
    }

    public int getLengthInBlock() {
        return lengthInBlock;
    }

    public NumbersInfo setLengthInBlock(int lengthInBlock) {
        this.lengthInBlock = nonNegative(lengthInBlock);
        return this;
    }

    @Override
    public SScalar analyse(SNumbers source) {
        if (!source.isInitialized()) {
            return new SScalar();
        }
        long t1 = debugTime();
        int indexInBlock = this.lengthInBlock > 0 ? this.indexInBlock : 0;
        int lengthInBlock = this.lengthInBlock > 0 ? this.lengthInBlock : source.getBlockLength();
        final boolean allQuick = Arrays.stream(SimpleArrayStatistics.values()).filter(
                statistics -> isOutputNecessary(statistics.statisticsName()))
                .allMatch(SimpleArrayStatistics::isQuickRangeInBlockProcessing);
        if (!allQuick && (indexInBlock != 0 || lengthInBlock != source.getBlockLength())) {
            source = source.columnRange(indexInBlock, lengthInBlock);
            indexInBlock = 0;
        }
        getScalar(OUTPUT_BLOCK_LENGTH).setTo(source.getBlockLength());
        getScalar(OUTPUT_ARRAY_LENGTH).setTo(source.getArrayLength());
        getScalar(OUTPUT_ELEMENT_TYPE).setTo(source.elementType());
        getScalar(OUTPUT_MAX_POSSIBLE).setTo(source.asNumberArray().maxPossibleValue(1.0));
        long t2 = debugTime();
        for (SimpleArrayStatistics statistics : SimpleArrayStatistics.values()) {
            if (isOutputNecessary(statistics.statisticsName())) {
                getScalar(statistics.statisticsName()).setTo(
                        statistics.statistics(source, indexInBlock, lengthInBlock));
            }
        }
        long t3 = debugTime();
        logDebug(String.format(Locale.US,
                "Calculating simple numbers statistics: "
                        + "%.3f ms = %.3f ms %s + %.3f ms processing",
                (t3 - t1) * 1e-6,
                (t2 - t1) * 1e-6, allQuick ? "initializing" : "extracting columns",
                (t3 - t2) * 1e-6));
        return SScalar.valueOf(source.n());
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

}
