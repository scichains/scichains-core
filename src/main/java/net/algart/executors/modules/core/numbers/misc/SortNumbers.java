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

import net.algart.arrays.ArraySorter;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

public final class SortNumbers extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SORTED_INDEXES = "sorted_indexes";
    public static final String OUTPUT_REVERSE_INDEXES = "reverse_indexes";

    private int sortedIndexInBlock = 0;
    private boolean descending = false;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public SortNumbers() {
        addOutputNumbers(OUTPUT_SORTED_INDEXES);
        addOutputNumbers(OUTPUT_REVERSE_INDEXES);
    }

    public int getSortedIndexInBlock() {
        return sortedIndexInBlock;
    }

    public SortNumbers setSortedIndexInBlock(int sortedIndexInBlock) {
        this.sortedIndexInBlock = nonNegative(sortedIndexInBlock);
        return this;
    }

    public boolean isDescending() {
        return descending;
    }

    public SortNumbers setDescending(boolean descending) {
        this.descending = descending;
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public SortNumbers setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final int blockLength = source.getBlockLength();
        if (sortedIndexInBlock < 0 || sortedIndexInBlock >= blockLength) {
            throw new IllegalArgumentException("Sorted index " + sortedIndexInBlock
                    + " is out of range 0.." + (blockLength - 1));
        }
        final SNumbers r = source.clone();
        final int[] indexes = new int[r.n()];
        for (int k = 0; k < indexes.length; k++) {
            indexes[k] = k + indexingBase.start;
        }
        // sorting also indexes to provide stable order of unsorted values
        ArraySorter.getQuickSorter().sort(0, r.n(),
                (firstIndex, secondIndex) -> {
                    final double v1 = r.getValue(firstIndex, sortedIndexInBlock);
                    final double v2 = r.getValue(secondIndex, sortedIndexInBlock);
                    return descending ?
                            v1 > v2 || (v1 == v2 && indexes[firstIndex] > indexes[secondIndex]):
                            v1 < v2 || (v1 == v2 && indexes[firstIndex] < indexes[secondIndex]);
                },
                (firstIndex, secondIndex) -> {
                    final Object block1 = r.getBlockValues(firstIndex, null);
                    final Object block2 = r.getBlockValues(secondIndex, null);
                    r.setBlockValues(firstIndex, block2);
                    r.setBlockValues(secondIndex, block1);
                    int temp = indexes[firstIndex];
                    indexes[firstIndex] = indexes[secondIndex];
                    indexes[secondIndex] = temp;
                });
        getNumbers(OUTPUT_SORTED_INDEXES).setTo(indexes, 1);
        final int[] reverse = InvertTable.invert(indexes, indexingBase.start);
        getNumbers(OUTPUT_REVERSE_INDEXES).setTo(reverse, 1);
        return r;
    }

}
