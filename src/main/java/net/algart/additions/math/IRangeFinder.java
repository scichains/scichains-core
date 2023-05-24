/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.additions.math;

import net.algart.arrays.ArraySelector;
import net.algart.arrays.JArrays;
import net.algart.arrays.TooLargeArrayException;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public class IRangeFinder {
    private static final int MAX_NUMBER_OF_RANGES = Integer.MAX_VALUE / 3;
    // - our tree requires ~3*n longs, and it is better to check n at the very beginning

    private static final boolean MEDIAN_OF_BOTH_ENDS = true;
    // - both values leads to comparable speed, but "true" value seems to be more stable in difficult cases
    private static final int THRESHOLD_FOR_STORING_SHORT_LISTS = 16;
    // - zero value disabled this optimization
    static final boolean OPTIMIZE_ADDITIONAL_SEARCH = true;
    // - should be true for little better performance

    private static final ArraySelector ARRAY_SELECTOR = ArraySelector.getQuickSelector();

    private IntUnaryOperator left = null;
    private IntUnaryOperator right = null;
    int n = 0;
    private IntPredicate indexActual = value -> true;

    private long[] tree = { 1L };
    // (see also clear() method)
    // This array contains:
    //      n elements: packed index+left (n = number of checked intervals)
    //      interval tree structure
    // Interval tree structure:
    //      1 element: (encoded) length of this structure
    //         (could be calculated on the base of ICC, but it require recursion)
    //      1 element: center point C
    //      ICC structure (intervals containing C)
    //      LT (left subtree)
    //      RT (right subtree)
    // or, for a short list (number of intervals n <= THRESHOLD_FOR_STORING_SHORT_LISTS):
    //      1 element: n
    //      n elements: packed index+left for intervals
    // ICC structure:
    //      1 element: m = length of ICC (number of intervals, containing the point C)
    //      m elements: packed index+left for intervals, containing the point C
    //      m elements: packed index+right for intervals, containing the point C

    private int[] work = JArrays.EMPTY_INTS;
    int treeOffset = 0;

    IRangeFinder() {
    }

    public static IRangeFinder getEmptyInstance() {
        return new IRangeFinder();
    }

    public static IRangeFinder getEmptyUnoptimizedInstance() {
        return new IRangeFinderWithoutOptimization();
    }

    public static IRangeFinder getInstance(IntUnaryOperator left, IntUnaryOperator right, int numberOfRanges) {
        return new IRangeFinder().setRanges(left, right, numberOfRanges);
    }

    public static IRangeFinder getInstance(int[] left, int[] right) {
        return new IRangeFinder().setRanges(left, right);
    }

    public final IntUnaryOperator left() {
        return left;
    }

    public final IntUnaryOperator right() {
        return right;
    }

    public final int left(int k) {
        return left.applyAsInt(k);
    }

    public final int right(int k) {
        return right.applyAsInt(k);
    }

    public final int numberOfRanges() {
        return n;
    }

    public final IRangeFinder setRanges(IntUnaryOperator left, IntUnaryOperator right, int numberOfRanges) {
        if (numberOfRanges < 0) {
            throw new IllegalArgumentException("Negative number of ranges " + numberOfRanges);
        }
        if (numberOfRanges > 0) {
            Objects.requireNonNull(left, "Null operator for getting left bounds");
            Objects.requireNonNull(right, "Null operator for getting right bounds");
        }
        if (numberOfRanges > MAX_NUMBER_OF_RANGES) {
            throw new TooLargeArrayException("Too large number of ranges " + numberOfRanges
                    + ": cannot be >" + MAX_NUMBER_OF_RANGES);
        }
        this.left = left;
        this.right = right;
        this.n = numberOfRanges;
        build();
        return this;
    }

    public final IRangeFinder setRanges(int[] left, int[] right) {
        Objects.requireNonNull(left, "Null left");
        Objects.requireNonNull(right, "Null right");
        if (left.length != right.length) {
            throw new IllegalArgumentException("Different lengths of arrays of left/right coordinates");
        }
        return setRanges(i -> left[i], i -> right[i], left.length);
    }

    public final IRangeFinder setIndexedRanges(int[] allLeft, int[] allRight, int[] indexes, int numberOfRanges) {
        Objects.requireNonNull(allLeft, "Null allLeft");
        Objects.requireNonNull(allRight, "Null allRight");
        Objects.requireNonNull(indexes, "Null indexes");
        if (allLeft.length != allRight.length) {
            throw new IllegalArgumentException("Different lengths of arrays of left/right coordinates");
        }
        if (numberOfRanges < 0) {
            throw new IllegalArgumentException("Negative number of ranges");
        }
        if (numberOfRanges > indexes.length) {
            throw new IllegalArgumentException("Number of ranges " + numberOfRanges
                    + " > indexes array length = " + indexes.length);
        }
        return setRanges(i -> allLeft[indexes[i]], i -> allRight[indexes[i]], numberOfRanges);
    }

    /**
     * Allows to remove some ranges from finding, for example, if they were processed by some way
     * and already not necessary. For such indexes, <tt>indexActual.test(index)</tt> should return false.
     * By default, all indexes are actual (built-in <tt>indexActual</tt> always returns <tt>true</tt>).
     *
     * <p>Note that this method should be very quick/ If it requires essential time, it is better
     * to check actuality in the high-level processing.
     *
     * @param indexActual whether a range with the given index is necessary to be returned
     * @return a reference to this object.
     */
    public final IRangeFinder setIndexActual(IntPredicate indexActual) {
        this.indexActual = Objects.requireNonNull(indexActual, "Null predicate, is an index necessary");
        return this;
    }

    public final IRangeFinder setAllIndexesActual() {
        return setIndexActual(value -> true);
    }

    public final boolean indexActual(int k) {
        return indexActual.test(k);
    }

    // Useful if this instance will be used for a long time with another accesses to the heap
    public IRangeFinder compact() {
        final int treeLength = treeOffset + treeLength(treeOffset);
        if (treeLength < tree.length) {
            tree = Arrays.copyOf(tree, treeLength);
        }
        final long workLength = workLength();
        if (workLength < work.length) {
            work = Arrays.copyOf(work, (int) workLength);
        }
        return this;
    }

    public void findContaining(int point, IRangeConsumer rangeConsumer) {
        findContainingPoint(this.treeOffset, point, rangeConsumer);
    }

    public void findContaining(int point, IntConsumer indexConsumer) {
        findContainingPoint(this.treeOffset, point, indexConsumer);
    }

    public int findContaining(int point, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findContaining(point, appender);
        return appender.offset();
    }

    /*Repeat() IRangeConsumer ==> IntConsumer;;
               rangeConsumer ==> indexConsumer;;
               (accept\(index),\s*left,\s*right ==> $1
     */
    public void findContaining(double point, IRangeConsumer rangeConsumer) {
        if (point < Integer.MIN_VALUE || point > Integer.MAX_VALUE) {
            return;
        }
        final int roundedPoint = (int) Math.round(point);
        // - if an integer interval contains non-integer x, it always contains both floor(x) amd ceil(x)
        if (roundedPoint == point) {
            findContaining(roundedPoint, rangeConsumer);
        } else {
            findContaining(roundedPoint, (index, left, right) -> {
                if (left <= point && point <= right) {
                    rangeConsumer.accept(index, left, right);
                }
            });
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public void findContaining(double point, IntConsumer indexConsumer) {
        if (point < Integer.MIN_VALUE || point > Integer.MAX_VALUE) {
            return;
        }
        final int roundedPoint = (int) Math.round(point);
        // - if an integer interval contains non-integer x, it always contains both floor(x) amd ceil(x)
        if (roundedPoint == point) {
            findContaining(roundedPoint, indexConsumer);
        } else {
            findContaining(roundedPoint, (index, left, right) -> {
                if (left <= point && point <= right) {
                    indexConsumer.accept(index);
                }
            });
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    public int findContaining(double point, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findContaining(point, appender);
        return appender.offset();
    }

    /*Repeat() IRangeConsumer ==> IntConsumer;;
               rangeConsumer ==> indexConsumer
     */
    // Note: if min > max, returns all intervals containing min
    public void findIntersecting(int min, int max, IRangeConsumer rangeConsumer) {
        if (OPTIMIZE_ADDITIONAL_SEARCH) {
            final int p = findContainingPointAndMinGreater(this.treeOffset, min, rangeConsumer);
            findIntervalsLeftInRangeFromKnown(max, rangeConsumer, p);
        } else {
            findContainingPoint(this.treeOffset, min, rangeConsumer);
            findIntervalsLeftInRange(min, max, rangeConsumer);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    // Note: if min > max, returns all intervals containing min
    public void findIntersecting(int min, int max, IntConsumer indexConsumer) {
        if (OPTIMIZE_ADDITIONAL_SEARCH) {
            final int p = findContainingPointAndMinGreater(this.treeOffset, min, indexConsumer);
            findIntervalsLeftInRangeFromKnown(max, indexConsumer, p);
        } else {
            findContainingPoint(this.treeOffset, min, indexConsumer);
            findIntervalsLeftInRange(min, max, indexConsumer);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    public int findIntersecting(int min, int max, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findIntersecting(min, max, appender);
        return appender.offset();
    }

    public void clear() {
        this.left = null;
        this.right = null;
        this.n = 0;
        this.tree = new long[] { 1 };
        this.work = JArrays.EMPTY_INTS;
        this.treeOffset = 0;
    }

    @Override
    public String toString() {
        return "integer ranges finder (interval tree) for " + n + " ranges"
                + (n == 0 ? " (empty)" : "")
                + " (" + treeOffset + "+" + treeLength(treeOffset)
                        + " elements, capacity " + tree.length + ")";
    }

    IRangeFinder build() {
        ensureWorkCapacity(workLength());
        ensureTreeCapacity(2 * (long) n + 1);
        packInitialLeftIndexes();
        sortLeftIndexes();
        treeOffset = n;
        buildTreePart(treeOffset, 0, n);
        findFirstPositionInSortedArrayWithSameLeft();
        return this;
    }

    private void packInitialLeftIndexes() {
        IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
            for (int k = block << 8, to = (int) Math.min((long) k + 256, n); k < to; k++) {
                final int left = left(k);
                final int right = right(k);
                if (left > right) {
                    throw new IllegalStateException("Illegal range #" + k + ": left > right");
                }
                tree[k] = packLowAndHigh(k, left);
            }
        });
    }

    private void sortLeftIndexes() {
        Arrays.parallelSort(tree, 0, n);
    }

    private void findFirstPositionInSortedArrayWithSameLeft() {
        int firstPositionWithThisLeft = -1;
        int lastLeft = 157;
        for (int k = 0; k < n; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (firstPositionWithThisLeft == -1 || left != lastLeft) {
                firstPositionWithThisLeft = k;
                lastLeft = left;
            }
            final int index = unpackLow(packed);
            work[index] = firstPositionWithThisLeft;
        }
    }

    private int firstPositionInSortedArrayWithSameLeft(int index) {
        return work[index];
    }

    // Tree must contain indexes of processed intervals from tree[indexesFrom] until tree[indexesTo-1];
    // these indexes must be sorted by left boundary and contain this boundary in the high word
    private int buildTreePart(final int resultTreeOffset, final int indexesFrom, final int indexesTo) {
        final int count = indexesTo - indexesFrom;
        assert count >= 0;
        if (count <= THRESHOLD_FOR_STORING_SHORT_LISTS) {
            return buildShortList(resultTreeOffset, indexesFrom, count);
        }
        ensureTreeCapacity((long) resultTreeOffset + 2);
        final int c = medianPoint(indexesFrom, indexesTo);
        tree[resultTreeOffset] = -1; // - some incorrect value (to be on the safe side)
        tree[resultTreeOffset + 1] = c;
        // - not packed
        final int iccOffset = iccOffset(resultTreeOffset);
        final int lTreeOffset = buildICC(iccOffset, c, indexesFrom, indexesTo);
        // tree[resultTreeOffset...]:  -1 c ICC
        final int afterLIndexes = extractIndexesOfStrictlyLeft(lTreeOffset, c, indexesFrom, indexesTo);
        // tree[resultTreeOffset...]:  -1 c ICC LI (LI is left indexes); their sorted order is PRESERVED
        final int lTreeLength = buildTreePart(afterLIndexes, lTreeOffset, afterLIndexes);
        // tree[resultTreeOffset...]:  -1 c ICC LI LT (LT is left sub-tree)
        System.arraycopy(tree, afterLIndexes, tree, lTreeOffset, lTreeLength);
        // tree[resultTreeOffset...]:  -1 c ICC LT
        final int rTreeOffset = lTreeOffset + lTreeLength;
        final int afterRIndexes = extractIndexesOfStrictlyRight(rTreeOffset, c, indexesFrom, indexesTo);
        // tree[resultTreeOffset...]:  -1 c LI LT RI (RI is left indexes); their sorted order is PRESERVED
        final int rTreeLength = buildTreePart(afterRIndexes, rTreeOffset, afterRIndexes);
        // tree[resultTreeOffset...]:  -1 c LI LT RI RT (RT is right sub-tree)
        System.arraycopy(tree, afterRIndexes, tree, rTreeOffset, rTreeLength);
        // tree[resultTreeOffset...]:  -1 c ICC LT RT
        final int treeLength = rTreeOffset + rTreeLength - resultTreeOffset;
        tree[resultTreeOffset] = addTreeLengthSignature(treeLength);
        // tree[resultTreeOffset...]:  treeLength c ICC LT RT
        assert lTreeOffset == leftTreeOffset(resultTreeOffset, iccLength(resultTreeOffset));
        assert rTreeOffset == leftToRightTreeOffset(lTreeOffset);
        treeLength(lTreeOffset);
        treeLength(rTreeOffset);
        // - built-in checking correctness (assertions inside treeLength method)
        return treeLength;
    }

    // Tree must contain indexes of processed intervals from tree[indexesOffset] until tree[indexesOffset+count-1];
    // these indexes must be sorted by left boundary and contain this boundary in the high word
    private int buildShortList(final int resultListOffset, final int indexesOffset, int count) {
        final int result = count + 1;
        ensureTreeCapacity((long) resultListOffset + (long) result);
        tree[resultListOffset] = result;
        // - not packed
        System.arraycopy(tree, indexesOffset, tree, resultListOffset + 1, count);
        // Sorting is not necessary: indexes were already sorted by left boundary
        return result;
    }

    private int medianPoint(int indexesFrom, int indexesTo) {
        if (MEDIAN_OF_BOTH_ENDS) {
            int m = 0;
            for (int k = indexesFrom; k < indexesTo; k++) {
                final long packed = tree[k];
                int index = unpackLow(packed);
                int left = unpackHigh(packed);
                work[m++] = left;
                work[m++] = right(index);
            }
            return ARRAY_SELECTOR.select(0, m, m >> 1, work);
        } else {
            int centerIndex = (indexesFrom + indexesTo) >>> 1;
            return unpackHigh(tree[centerIndex]);
            // - returning left boundary: saves ~25% of building time, but little reduces requests

//            int m = indexesTo - indexesFrom;
//            for (int i = 0, k = indexesFrom; i < m; i++, k++) {
//                int index = unpackLow(tree[k]);
//                work[i] = (int) ((long) left(index) + (long) right(index)) >> 1;
//                 - note: here >> , not >>> (coordinates can be negative)
//            }
//            return ARRAY_SELECTOR.select(0, m, m >> 1, work);
        }
    }

    // Tree must contain indexes of processed intervals from tree[indexesFrom] until tree[indexesTo-1];
    // these indexes must be sorted by left boundary and contain this boundary in the high word
    private int buildICC(final int resultICCOffset, final int center, final int indexesFrom, final int indexesTo) {
        ensureTreeCapacity((long) resultICCOffset + 1 + (long) (indexesTo - indexesFrom));
        // - usually little more than actually necessary, but it is not a problem:
        // we will reuse this space while building subtrees
        final int leftBoundariesOffset = resultICCOffset + 1;
        int offset = leftBoundariesOffset;
        int minGreaterLeft = -1;
        int minGreaterLeftIndex = -1;
        for (int k = indexesFrom; k < indexesTo; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            final int index = unpackLow(packed);
            assert left == left(index);
            if (left > center) {
                if (minGreaterLeftIndex == -1 || left < minGreaterLeft) {
                    minGreaterLeft = left;
                    minGreaterLeftIndex = index;
                }
            } else if (right(index) >= center) {
                // left <= center
                tree[offset++] = packed;
            }
        }
        final int rightBoundariesOffset = offset;
        final int numberOfIntervalsContainingCenter = offset - leftBoundariesOffset;
        ensureTreeCapacity((long) offset + (long) numberOfIntervalsContainingCenter);
        for (int k = leftBoundariesOffset; k < rightBoundariesOffset; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            final int right = right(index);
            tree[offset++] = packLowAndHigh(index, right);
        }
        tree[resultICCOffset] = packICCCount(numberOfIntervalsContainingCenter, minGreaterLeftIndex);
        // Sorting left part is not necessary: indexes were already sorted by left boundary
        Arrays.parallelSort(tree, rightBoundariesOffset, offset);
        return offset;
    }

    private int extractIndexesOfStrictlyLeft(int resultOffset, int center, int indexesFrom, int indexesTo) {
        int offset = resultOffset;
        for (int k = indexesFrom; k < indexesTo; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            final int right = right(index);
            if (right < center) {
                if (offset >= tree.length) {
                    ensureTreeCapacity((long) offset + 1);
                }
                tree[offset++] = packed;
            }
        }
        return offset;
    }

    private int extractIndexesOfStrictlyRight(int resultOffset, int center, int indexesFrom, int indexesTo) {
        int offset = resultOffset;
        for (int k = indexesFrom; k < indexesTo; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);;
            if (left > center) {
                if (offset >= tree.length) {
                    ensureTreeCapacity((long) offset + 1);
                }
                tree[offset++] = packed;
            }
        }
        return offset;
    }

    /*Repeat() IRangeConsumer ==> IntConsumer;;
               rangeConsumer ==> indexConsumer
     */

    // Returns 1st position p in the sorted array L[] of left bounds, that L[p] > point
    int findContainingPointAndMinGreater(int subtreeOffset, int point, IRangeConsumer rangeConsumer) {
        int minGreater = n;
        for (; ; ) {
            final long packedTreeLength = tree[subtreeOffset];
            final int treeLength = (int) packedTreeLength;
            if (treeLength == packedTreeLength) {
                assert treeLength >= 1;
                // - short list
                int p = findShortListAndMinGreater(
                        point, rangeConsumer, subtreeOffset + 1, subtreeOffset + treeLength);
                if (p < minGreater) {
                    minGreater = p;
                }
                break;
            }
            final int center = unpackTreeCenterAtOffset(subtreeOffset + 1);
            final int iccOffset = iccOffset(subtreeOffset);
            final long iccHeader = tree[iccOffset];
            final int iccCount = unpackICCCount(iccHeader);
            final int iccLeftOffset = iccOffset + 1;
            final int iccRightOffset = iccLeftOffset + iccCount;
            final int leftTreeOffset = iccRightOffset + iccCount;
            if (point <= center) {
                final int minGreaterCenter = unpackIndexOfIntervalWithMinimalLeftGreaterCenter(iccHeader);
                int p = n;
                if (iccCount > 0) {
                    p = findICCLeftAndMinGreater(point, rangeConsumer, iccLeftOffset, iccRightOffset);
                }
                if (p == n && minGreaterCenter >= 0) {
                    // - if p==n (not found), we need to use the leftmost from all intervals with L > center
                    p = firstPositionInSortedArrayWithSameLeft(minGreaterCenter);
                }
                if (p < minGreater) {
                    minGreater = p;
                }
                if (point == center) {
                    break;
                }
                subtreeOffset = leftTreeOffset;
            } else {
                if (iccCount > 0) {
                    findICCRight(point, rangeConsumer, iccRightOffset, leftTreeOffset);
                    // - all these intervals have left <= point and not affect the result of this function
                }
                subtreeOffset = leftToRightTreeOffset(leftTreeOffset);
            }
        }
        return minGreater;
    }

    void findContainingPoint(int subtreeOffset, int point, IRangeConsumer rangeConsumer) {
        for (; ; ) {
            final long packedTreeLength = tree[subtreeOffset];
            final int treeLength = (int) packedTreeLength;
            if (treeLength == packedTreeLength) {
                assert treeLength >= 1;
                // - short list
                findShortList(point, rangeConsumer, subtreeOffset + 1, subtreeOffset + treeLength);
                break;
            }
            final int center = unpackTreeCenterAtOffset(subtreeOffset + 1);
            final int iccOffset = iccOffset(subtreeOffset);
            final long iccHeader = tree[iccOffset];
            final int iccCount = unpackICCCount(iccHeader);
            final int iccLeftOffset = iccOffset + 1;
            final int iccRightOffset = iccLeftOffset + iccCount;
            final int leftTreeOffset = iccRightOffset + iccCount;
            if (point <= center) {
                if (iccCount > 0) {
                    findICCLeft(point, rangeConsumer, iccLeftOffset, iccRightOffset);
                }
                if (point == center) {
                    return;
                }
                subtreeOffset = leftTreeOffset;

            } else {
                if (iccCount > 0) {
                    findICCRight(point, rangeConsumer, iccRightOffset, leftTreeOffset);
                    // - all these intervals have left <= point and not affect the result of this function
                }
                subtreeOffset = leftToRightTreeOffset(leftTreeOffset);
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    // Returns 1st position p in the sorted array L[] of left bounds, that L[p] > point
    int findContainingPointAndMinGreater(int subtreeOffset, int point, IntConsumer indexConsumer) {
        int minGreater = n;
        for (; ; ) {
            final long packedTreeLength = tree[subtreeOffset];
            final int treeLength = (int) packedTreeLength;
            if (treeLength == packedTreeLength) {
                assert treeLength >= 1;
                // - short list
                int p = findShortListAndMinGreater(
                        point, indexConsumer, subtreeOffset + 1, subtreeOffset + treeLength);
                if (p < minGreater) {
                    minGreater = p;
                }
                break;
            }
            final int center = unpackTreeCenterAtOffset(subtreeOffset + 1);
            final int iccOffset = iccOffset(subtreeOffset);
            final long iccHeader = tree[iccOffset];
            final int iccCount = unpackICCCount(iccHeader);
            final int iccLeftOffset = iccOffset + 1;
            final int iccRightOffset = iccLeftOffset + iccCount;
            final int leftTreeOffset = iccRightOffset + iccCount;
            if (point <= center) {
                final int minGreaterCenter = unpackIndexOfIntervalWithMinimalLeftGreaterCenter(iccHeader);
                int p = n;
                if (iccCount > 0) {
                    p = findICCLeftAndMinGreater(point, indexConsumer, iccLeftOffset, iccRightOffset);
                }
                if (p == n && minGreaterCenter >= 0) {
                    // - if p==n (not found), we need to use the leftmost from all intervals with L > center
                    p = firstPositionInSortedArrayWithSameLeft(minGreaterCenter);
                }
                if (p < minGreater) {
                    minGreater = p;
                }
                if (point == center) {
                    break;
                }
                subtreeOffset = leftTreeOffset;
            } else {
                if (iccCount > 0) {
                    findICCRight(point, indexConsumer, iccRightOffset, leftTreeOffset);
                    // - all these intervals have left <= point and not affect the result of this function
                }
                subtreeOffset = leftToRightTreeOffset(leftTreeOffset);
            }
        }
        return minGreater;
    }

    void findContainingPoint(int subtreeOffset, int point, IntConsumer indexConsumer) {
        for (; ; ) {
            final long packedTreeLength = tree[subtreeOffset];
            final int treeLength = (int) packedTreeLength;
            if (treeLength == packedTreeLength) {
                assert treeLength >= 1;
                // - short list
                findShortList(point, indexConsumer, subtreeOffset + 1, subtreeOffset + treeLength);
                break;
            }
            final int center = unpackTreeCenterAtOffset(subtreeOffset + 1);
            final int iccOffset = iccOffset(subtreeOffset);
            final long iccHeader = tree[iccOffset];
            final int iccCount = unpackICCCount(iccHeader);
            final int iccLeftOffset = iccOffset + 1;
            final int iccRightOffset = iccLeftOffset + iccCount;
            final int leftTreeOffset = iccRightOffset + iccCount;
            if (point <= center) {
                if (iccCount > 0) {
                    findICCLeft(point, indexConsumer, iccLeftOffset, iccRightOffset);
                }
                if (point == center) {
                    return;
                }
                subtreeOffset = leftTreeOffset;

            } else {
                if (iccCount > 0) {
                    findICCRight(point, indexConsumer, iccRightOffset, leftTreeOffset);
                    // - all these intervals have left <= point and not affect the result of this function
                }
                subtreeOffset = leftToRightTreeOffset(leftTreeOffset);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private int findShortListAndMinGreater(int point, IRangeConsumer rangeConsumer, int from, int to) {
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            final int index = unpackLow(packed);
            if (left > point) {
                return firstPositionInSortedArrayWithSameLeft(index);
            }
            final int right = right(index);
            if (point <= right && indexActual.test(index)) {
                rangeConsumer.accept(index, left, right);
            }
        }
        return n;
    }

    private int findShortListAndMinGreater(int point, IntConsumer indexConsumer, int from, int to) {
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            final int index = unpackLow(packed);
            if (left > point) {
                return firstPositionInSortedArrayWithSameLeft(index);
            }
            final int right = right(index);
            if (point <= right && indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
        return n;
    }

    private void findShortList(int point, IRangeConsumer rangeConsumer, int from, int to) {
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > point) {
                return;
            }
            final int index = unpackLow(packed);
            final int right = right(index);
            if (point <= right && indexActual.test(index)) {
                rangeConsumer.accept(index, left, right);
            }
        }
    }

    private void findShortList(int point, IntConsumer indexConsumer, int from, int to) {
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > point) {
                return;
            }
            final int index = unpackLow(packed);
            final int right = right(index);
            if (point <= right && indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
    }

    // Note: here and below findFirstGreaterHigh / findFirstGreaterOrEqualHigh may be excluded:
    // we can perform full loop from..to-1 (or to-1..from) and break it when the boundary becomes
    // > point or < point. However, timing shows that the current solution is little faster:
    // main loop contains less operation, and binary search is a VERY quick loop.
    private int findICCLeftAndMinGreater(int point, IRangeConsumer rangeConsumer, int from, int to) {
        final int firstGreaterHigh = findFirstGreaterHigh(tree, from, to, point);
        for (int k = from; k < firstGreaterHigh; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                rangeConsumer.accept(index, unpackHigh(packed), right(index));
            }
        }
        return firstGreaterHigh == to ? n : firstPositionInSortedArrayWithSameLeft(unpackLow(tree[firstGreaterHigh]));
    }

    private int findICCLeftAndMinGreater(int point, IntConsumer indexConsumer, int from, int to) {
        final int firstGreaterHigh = findFirstGreaterHigh(tree, from, to, point);
        for (int k = from; k < firstGreaterHigh; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
        return firstGreaterHigh == to ? n : firstPositionInSortedArrayWithSameLeft(unpackLow(tree[firstGreaterHigh]));
    }

    private void findICCLeft(int point, IRangeConsumer rangeConsumer, int from, int to) {
        final int firstGreaterHigh = findFirstGreaterHigh(tree, from, to, point);
        for (int k = from; k < firstGreaterHigh; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                rangeConsumer.accept(index, unpackHigh(packed), right(index));
            }
        }
    }

    private void findICCLeft(int point, IntConsumer indexConsumer, int from, int to) {
        final int firstGreaterHigh = findFirstGreaterHigh(tree, from, to, point);
        for (int k = from; k < firstGreaterHigh; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
    }

    private void findICCRight(int point, IRangeConsumer rangeConsumer, int from, int to) {
        from = findFirstGreaterOrEqualHigh(tree, from, to, point);
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                rangeConsumer.accept(index, left(index), unpackHigh(packed));
            }
        }
    }

    private void findICCRight(int point, IntConsumer indexConsumer, int from, int to) {
        from = findFirstGreaterOrEqualHigh(tree, from, to, point);
        for (int k = from; k < to; k++) {
            final long packed = tree[k];
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
    }

    /*Repeat() IRangeConsumer ==> IntConsumer;;
               rangeConsumer ==> indexConsumer;;
               (accept\(index),\s*left,\s*right\(index\) ==> $1
     */
    void findIntervalsLeftInRangeFromKnown(int max, IRangeConsumer rangeConsumer, int from) {
//        if (DEBUG_MODE) {
//            final int firstGreaterThenMin = findFirstGreaterHigh(tree, 0, n, minExcluding);
//            if (from != firstGreaterThenMin) {
//                throw new AssertionError("Invalid first position > min: "
//                        + from + " != " + firstGreaterThenMin);
//            }
//        }
        for (int k = from; k < n; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > max) {
                break;
            }
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                rangeConsumer.accept(index, left, right(index));
            }
        }
    }

    void findIntervalsLeftInRange(int minExcluding, int max, IRangeConsumer rangeConsumer) {
        final int from = findFirstGreaterHigh(tree, 0, n, minExcluding);
        for (int k = from; k < n; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > max) {
                break;
            }
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                rangeConsumer.accept(index, left, right(index));
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    void findIntervalsLeftInRangeFromKnown(int max, IntConsumer indexConsumer, int from) {
//        if (DEBUG_MODE) {
//            final int firstGreaterThenMin = findFirstGreaterHigh(tree, 0, n, minExcluding);
//            if (from != firstGreaterThenMin) {
//                throw new AssertionError("Invalid first position > min: "
//                        + from + " != " + firstGreaterThenMin);
//            }
//        }
        for (int k = from; k < n; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > max) {
                break;
            }
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
    }

    void findIntervalsLeftInRange(int minExcluding, int max, IntConsumer indexConsumer) {
        final int from = findFirstGreaterHigh(tree, 0, n, minExcluding);
        for (int k = from; k < n; k++) {
            final long packed = tree[k];
            final int left = unpackHigh(packed);
            if (left > max) {
                break;
            }
            final int index = unpackLow(packed);
            if (indexActual.test(index)) {
                indexConsumer.accept(index);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private long workLength() {
        return MEDIAN_OF_BOTH_ENDS ? 2 * (long) n : n;
    }

    private void ensureTreeCapacity(long newTreeLength) {
        if (newTreeLength > tree.length) {
            if (newTreeLength > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large array is necessary for interval tree: >=2^31 elements");
            }
            final int newLength = Math.max(16, Math.max((int) newTreeLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * tree.length))));
//            System.out.println("increasing " + tree.length + " -> " + newLength);
            tree = Arrays.copyOf(tree, newLength);
        }
    }

    private void ensureWorkCapacity(long newWorkLength) {
        if (newWorkLength > work.length) {
            if (newWorkLength > Integer.MAX_VALUE) {
                // - actually should not occur due to MAX_NUMBER_OF_INTERVALS
                throw new TooLargeArrayException("Too large array is necessary for interval tree: >=2^31 elements");
            }
            final int newLength = Math.max(16, Math.max((int) newWorkLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * work.length))));
            work = Arrays.copyOf(work, newLength);
        }
    }

    private int treeLength(int treeOffset) {
        return unpackTreeLengthAtOffset(treeOffset);
    }

    private static int iccOffset(int treeOffset) {
        return treeOffset + 2;
    }

    private int iccCount(int treeOffset) {
        return unpackICCCountAtOffset(iccOffset(treeOffset));
    }

    private int iccLength(int treeOffset) {
        return 2 * iccCount(treeOffset) + 1;
    }

    private static int leftTreeOffset(int treeOffset, int iccLength) {
        return iccOffset(treeOffset) + iccLength;
    }

    private int leftToRightTreeOffset(int leftTreeOffset) {
        return leftTreeOffset + unpackTreeLengthAtOffset(leftTreeOffset);
    }

    private static long addTreeLengthSignature(int length) {
        return packLowAndHigh(length, 0x74726565);
        // - letters 't','r','e','e'
    }

    private int unpackTreeLengthAtOffset(int offset) {
        return unpackTreeLength(tree[offset]);
    }

    private boolean isShortList(long packedTreeLength) {
        return packedTreeLength == (int) packedTreeLength;
    }

    private int unpackTreeLength(long packedTreeLength) {
        int result = unpackLow(packedTreeLength);
        if (isShortList(packedTreeLength)) {
            // - short list: packed without signature
            if (result <= 0) {
                throw new AssertionError("Damaged interval tree: "
                        + packedTreeLength + " is not a correct subtree length");
            }
            return result;
        } else {
            if (unpackHigh(packedTreeLength) != 0x74726565 || result <= 0) {
                throw new AssertionError("Damaged interval tree: 0x"
                        + Long.toHexString(packedTreeLength) + " is not a correct subtree length");
            }
            return result;
        }
    }

    private int unpackTreeCenterAtOffset(int offset) {
        long packed = tree[offset];
        final int result = (int) packed;
        if (packed != result) {
            throw new AssertionError("Damaged interval tree: tree[" + offset + "] = 0x"
                    + Long.toHexString(packed) + " is not a correct center (it is not 32-bit integer)");
        }
        return result;
    }

    private static long packICCCount(int numberOfIntervals, int indexOfIntervalWithMinimalLeftGreaterCenter) {
        return packLowAndHigh(numberOfIntervals, indexOfIntervalWithMinimalLeftGreaterCenter);
    }

    private int unpackICCCountAtOffset(int offset) {
        return unpackICCCount(tree[offset]);
    }

    private int unpackICCCount(long iccHeader) {
        return unpackLow(iccHeader);
    }

    private int unpackIndexOfIntervalWithMinimalLeftGreaterCenter(long iccHeader) {
        final int result = unpackHigh(iccHeader);
        if (result < -1) {
            throw new AssertionError("Damaged interval tree: ICC header contains incorrect index");
        }
        return result;
    }

    private static int findFirstGreaterHigh(long[] sortedArray, final int from, final int to, final int high) {
        // int index = Arrays.binarySearch(sortedArray, from, to, packHigh(high));
        // if (index < 0) {
        //     index = -(index + 1);
        // }
        // - The code above is replaced with implementation of binarySearch:
        final long key = packHigh(high);
        int index = -1;
        int left = from;
        int right = to - 1;
        while (left <= right) {
            final int mid = (left + right) >>> 1;
            final long midVal = sortedArray[mid];
            if (midVal < key) {
                left = mid + 1;
            } else if (midVal > key) {
                right = mid - 1;
            } else {
                index = mid;
                break;
            }
        }
        if (index == -1) { // key not found.
            index = left;
            // So, sortedArray[index] > packHigh(high), unpackHigh(sortedArray[index]) >= high (not > !)
            // It does not mean that there are NO necessary elements:
            // we searched for packHigh, that usually differs from array elements
        }
        while (index < to && unpackHigh(sortedArray[index]) == high) {
            index++;
        }
        return index;
    }

    private static int findFirstGreaterOrEqualHigh(long[] sortedArray, final int from, final int to, final int high) {
        // int index = Arrays.binarySearch(sortedArray, from, to, packHigh(high));
        // if (index < 0) {
        //     index = -(index + 1);
        // }
        // - The code above is replaced with implementation of binarySearch:
        final long key = packHigh(high);
        int index = -1;
        int left = from;
        int right = to - 1;
        while (left <= right) {
            final int mid = (left + right) >>> 1;
            final long midVal = sortedArray[mid];
            if (midVal < key) {
                left = mid + 1;
            } else if (midVal > key) {
                right = mid - 1;
            } else {
                index = mid;
                break;
            }
        }
        if (index < 0) {
            index = left;
            // So, sortedArray[index] > packHigh(high), unpackHigh(sortedArray[index]) >= high (not > !)
            // It does not mean that there are NO necessary elements:
            // we searched for packHigh, that usually differs from array elements
        }
        while (index > 0 && unpackHigh(sortedArray[index - 1]) == high) {
            index--;
        }
        return index;
    }

    private static long packLowAndHigh(int low, int high) {
        return ((long) high << 32) | ((long) low & 0xFFFFFFFFL);
    }

    private static long packHigh(int high) {
        return (long) high << 32;
    }

    public static int unpackHigh(long packed) {
        return (int) (packed >>> 32);
    }

    public static int unpackLow(long packed) {
        return (int) packed;
    }
}
