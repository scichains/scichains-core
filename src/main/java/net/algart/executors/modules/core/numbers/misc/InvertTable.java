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
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

import java.util.Arrays;
import java.util.Objects;

public final class InvertTable extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_TABLE = "table";
    public static final String OUTPUT_TABLE = "table";

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public InvertTable() {
        setDefaultInputNumbers(INPUT_TABLE);
        setDefaultOutputNumbers(OUTPUT_TABLE);
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public InvertTable setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers table) {
        Objects.requireNonNull(table, "Null table");
        return SNumbers.valueOfArray(invert(table.toIntArray(), indexingBase.start), 1);
    }

    public static int[] invert(int[] table, int indexingBase) {
        Objects.requireNonNull(table, "Null table");
        if (indexingBase < 0) {
            throw new IllegalArgumentException("Indexing base cannot be negative: " + indexingBase);
        }
        int max = 0;
        for (int v : table) {
            if (v > max) {
                max = v;
            }
        }
        int[] result = new int[max - indexingBase + 1];
        Arrays.fill(result, -1);
        for (int k = 0; k < table.length; k++) {
            final int index = table[k] - indexingBase;
            if (index >= 0 && result[index] == -1) {
                result[index] = k + indexingBase;
            }
        }
        return result;
    }
}
