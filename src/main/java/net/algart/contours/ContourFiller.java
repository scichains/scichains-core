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

package net.algart.contours;

import net.algart.arrays.*;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public final class ContourFiller {
    private static final int START_INTERSECTIONS_CAPACITY = 512;

    private final Contours contours;
    private final UpdatablePArray labels;
    private final int[][] intersectionsWithHorizontal;
    private final int[][] initialIntersectionsWithHorizontal;
    private final int[] intersectionsCount;
    private final int startX;
    private final int startY;
    private final int dimX;
    private final int dimY;

    private final int[] contourIndexes;
    private int numberOfNecessaryContours = 0;

    private int minContourY;
    private int maxContourY;
    private final ContourLength contourLength = new ContourLength();
    private int[] unpackedContour = null;

    private IntPredicate needToFill = null;
    private boolean needToUnpack = true;
    private boolean needToUnpackDiagonals = true;
    private int[] labelsMap = null;
    private int indexingBase = 0;
    private IntUnaryOperator labelToFillerDefault = value -> value;

    private ContourFiller(Contours contours, Class<?> elementType, long startX, long startY, long dimX, long dimY) {
        this.contours = Objects.requireNonNull(contours, "Null contours");
        if (startX < Contours.MIN_ABSOLUTE_COORDINATE || startX > Contours.MAX_ABSOLUTE_COORDINATE
                || startY < Contours.MIN_ABSOLUTE_COORDINATE || startY > Contours.MAX_ABSOLUTE_COORDINATE) {
            throw new IllegalArgumentException("Start coordinates (" + startX + ", " + startY
                    + ") are out of allowed range "
                    + Contours.MIN_ABSOLUTE_COORDINATE + ".." + Contours.MAX_ABSOLUTE_COORDINATE);
        }
        if (dimX < 0 || dimY < 0) {
            throw new IllegalArgumentException("Negative dimX or dimY");
        }
        if (dimX > Integer.MAX_VALUE || dimY > Integer.MAX_VALUE || dimX * dimY > Integer.MAX_VALUE
                || dimY * (long) START_INTERSECTIONS_CAPACITY > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot fill contours for matrix "
                    + dimX + "x" + dimY + ": it is too large");
        }
        if (Arrays.sizeOf(elementType) < 0) {
            throw new IllegalArgumentException("Unsupported element type " + elementType + ": it must be primitive");
        }
        this.startX = (int) startX;
        this.startY = (int) startY;
        this.dimX = (int) dimX;
        this.dimY = (int) dimY;
        this.labels = (UpdatablePArray) Arrays.SMM.newUnresizableArray(elementType, dimX * dimY);
        this.intersectionsWithHorizontal = new int[this.dimY][START_INTERSECTIONS_CAPACITY];
        this.initialIntersectionsWithHorizontal = this.intersectionsWithHorizontal.clone();
        // - shallow copy: saving references only
        this.intersectionsCount = new int[this.dimY];
        // - zero-filled by Java
        this.contourIndexes = new int[contours.numberOfContours()];
    }

    public static ContourFiller newInstance(
            Contours contours,
            long startX,
            long startY,
            long dimX,
            long dimY) {
        return new ContourFiller(contours, int.class, startX, startY, dimX, dimY);
    }

    public static ContourFiller newInstance(
            Contours contours,
            Class<?> elementType,
            long startX,
            long startY,
            long dimX,
            long dimY) {
        return new ContourFiller(contours, elementType, startX, startY, dimX, dimY);
    }

    public IntPredicate getNeedToFill() {
        return needToFill;
    }

    public ContourFiller setNeedToFill(IntPredicate needToFill) {
        this.needToFill = needToFill;
        return this;
    }

    public boolean isNeedToUnpack() {
        return needToUnpack;
    }

    public ContourFiller setNeedToUnpack(boolean needToUnpack) {
        this.needToUnpack = needToUnpack;
        return this;
    }

    public boolean isNeedToUnpackDiagonals() {
        return needToUnpackDiagonals;
    }

    public ContourFiller setNeedToUnpackDiagonals(boolean needToUnpackDiagonals) {
        this.needToUnpackDiagonals = needToUnpackDiagonals;
        return this;
    }

    public int[] getLabelsMap() {
        return labelsMap;
    }

    public ContourFiller setLabelsMap(int[] labelsMap) {
        this.labelsMap = labelsMap;
        return this;
    }

    public int getIndexingBase() {
        return indexingBase;
    }

    public ContourFiller setIndexingBase(int indexingBase) {
        if (indexingBase < 0) {
            throw new IllegalArgumentException("Indexing base cannot be negative: " + indexingBase);
        }
        this.indexingBase = indexingBase;
        return this;
    }

    public IntUnaryOperator getLabelToFillerDefault() {
        return Objects.requireNonNull(labelToFillerDefault, "Null default value function");
    }

    public ContourFiller setLabelToFillerDefault(IntUnaryOperator labelToFillerDefault) {
        this.labelToFillerDefault = labelToFillerDefault;
        return this;
    }

    public int dimX() {
        return dimX;
    }

    public int dimY() {
        return dimY;
    }

    public void findAndSortNecessaryContours() {
        final int n = contours.numberOfContours();
        final int[] contourSize = new int[n];
        final MutableIntArray maxSizeForLabel = net.algart.arrays.Arrays.SMM.newEmptyIntArray();
        final ContourHeader header = new ContourHeader();
        int count = 0;
        for (int k = 0; k < n; k++) {
            if (needToFill != null && !needToFill.test(k)) {
                continue;
            }
            contours.getHeader(header, k);
            final int minX = header.minX();
            final int maxX = header.maxX();
            final int minY = header.minY();
            final int maxY = header.maxY();
            if (maxX < startX || minX >= startX + dimX || maxY < startY || minY >= startY + dimY) {
                continue;
            }
            final int diffY = maxY - minY;
            if (diffY < 0) {
                throw new AssertionError("Overflow in sizes of containing rectangle for contour #" + k
                        + "; it must be impossible due to check of Contour2DArray.MAX_ABSOLUTE_COORDINATE");
            }
            final int label = contours.getObjectLabel(k);
            if (label >= 0) {
                // - ignore order for strange negative labels
                if (label >= maxSizeForLabel.length()) {
                    maxSizeForLabel.length((long) label + 1);
                }
                if (diffY > maxSizeForLabel.getInt(label)) {
                    maxSizeForLabel.setInt(label, diffY);
                }
            }
            contourSize[count] = diffY;
            contourIndexes[count] = k;
            count++;
        }
        this.numberOfNecessaryContours = count;
        ArraySorter.getQuickSorter().sortIndexes(contourIndexes, 0, count, (firstIndex, secondIndex) -> {
            final int firstLabel = contours.getObjectLabel(firstIndex);
            final int secondLabel = contours.getObjectLabel(secondIndex);
            if (firstLabel != secondLabel) {
                if (firstLabel < 0 || secondLabel < 0) {
                    return firstLabel < secondLabel;
                    // - don't try to fill strange negative labels in correct order
                }
                final int firstSize = maxSizeForLabel.getInt(firstLabel);
                final int secondSize = maxSizeForLabel.getInt(secondLabel);
                if (firstSize != secondSize) {
                    return firstSize > secondSize;
                }
                // - greater first: small particle will be drawn over larger
                return firstLabel < secondLabel;
                // - for equal sizes we MUST provide correct order: each label must be continuous
            }
            final boolean firstInternal = contours.isInternalContour(firstIndex);
            final boolean secondInternal = contours.isInternalContour(secondIndex);
            if (firstInternal != secondInternal) {
                // external first
                return secondInternal;
            }
            return contourSize[firstIndex] < contourSize[secondIndex];
            // greater first
        });
    }

    public int numberOfNecessaryContours() {
        return numberOfNecessaryContours;
    }

    public void fillNecessaryContours() {
        final int maxTranslatedLabelPlus1 = labelsMap == null ? Integer.MIN_VALUE : labelsMap.length + indexingBase;
        for (int from = 0; from < numberOfNecessaryContours; ) {
            final int label = contours.getObjectLabel(contourIndexes[from]);
            int to = from + 1;
            while (to < numberOfNecessaryContours && contours.getObjectLabel(contourIndexes[to]) == label) {
                to++;
            }
            final int translatedLabel;
            if (label < maxTranslatedLabelPlus1 && label >= indexingBase) {
                assert labelsMap != null;
                // - now maxTranslatedLabelPlus1 != Integer.MIN_VALUE, so, labelsMap != null
                translatedLabel = labelsMap[label - indexingBase];
            } else {
                translatedLabel = labelToFillerDefault.applyAsInt(label);
            }
//            System.out.printf("Filling %d..%d contours for %d -> %d...%n", from, to, label, translatedLabel);
            fillContours(contourIndexes, from, to, translatedLabel);
            from = to;
        }
    }

    public void fillContours(int[] contourIndexes, int from, int to, int label) {
        openContour();
        for (int k = from; k < to; k++) {
            final int contourIndex = contourIndexes[k];
            unpackedContour = needToUnpack ?
                    contours.unpackContourAndReallocateResult(
                            unpackedContour, contourLength, contourIndex, needToUnpackDiagonals) :
                    contours.getContourPointsAndReallocateResult(
                            unpackedContour, contourLength, contourIndex);
            // - guarantees that the result contour points is a connected line, consisting from segments with length 1
            if (!contourLength.isDegenerated()) {
                addIntersectionsWithContour(unpackedContour, contourLength.getArrayLength());
            }
        }
        fillAndCloseContour(label);
    }

    public Matrix<? extends PArray> getLabels() {
        return Matrices.matrix(labels, dimX, dimY);
    }

    private void openContour() {
        maxContourY = Integer.MIN_VALUE;
        minContourY = Integer.MAX_VALUE;
    }

    private void addIntersectionsWithContour(int[] contour, int arrayLength) {
        final int dimY = this.dimY;
        final int startX = this.startX;
        final int startY = this.startY;
        // - JVM works better with local variables, not fields of an object
        for (int i = 0; i < arrayLength; ) {
            final int x = contour[i] - startX;
            final int y = contour[i + 1] - startY;
            if (y < 0 || y > dimY) {
                // - note: if y==dimY, it MAY be a suitable edge y-1..y
                int distance = y < 0 ? -y : y - dimY;
                i += distance << 1;
                // - we need at least such number of steps to appear inside minY..maxY+1,
                // because every step has length 1 (horizontal or vertical)
                continue;
            }
            final int lastI = i == 0 ? arrayLength - 2 : i - 2;
            final int lastX = contour[lastI] - startX;
            final int lastY = contour[lastI + 1] - startY;
            i += 2;

            final int dx = x - lastX;
            final int dy = y - lastY;
            if (dy == 0 ? dx != -1 && dx != 1 : dx != 0 || (dy != -1 && dy != 1))
                if (needToUnpack) {
                    throw new AssertionError("Invalid segment in unpacked contour: "
                            + lastX + "," + lastY + " -> " + x + "," + y + "; cannot appear in unpacked contours");
                } else {
                    throw new IllegalArgumentException("Invalid segment in the contour: "
                            + lastX + "," + lastY + " -> " + x + "," + y
                            + "; you must use setNeedToUnpack(true) (default mode) to process such contours");
                }
            if (dx == 0) {
                final int lessFromTwoY = dy < 0 ? y : lastY;
                if (lessFromTwoY >= 0 && lessFromTwoY < dimY) {
                    addIntersection(x, lessFromTwoY);
                }
            }
        }
    }

    private void fillAndCloseContour(int label) {
        final int dimX = this.dimX;
        for (int y = minContourY, disp = y * dimX; y <= maxContourY; y++, disp += dimX) {
            final int count = intersectionsCount[y];
            assert (count & 1) == 0 : "odd number of intersection with y=" + y + " (impossible for connected curve)";
            final int[] intersections = intersectionsWithHorizontal[y];
            java.util.Arrays.sort(intersections, 0, count);
            for (int k = 0; k < count; k += 2) {
                int fromX = intersections[k];
                int toX = intersections[k + 1];
                if (toX <= 0) {
                    continue;
                }
                if (fromX >= dimX) {
                    break;
                }
                if (fromX < 0) {
                    fromX = 0;
                }
                if (toX > dimX) {
                    toX = dimX;
                }
                assert fromX <= toX;
                labels.fill((long) disp + fromX, (long) toX - fromX, label);
            }
            removeAllIntersections(y);
        }
    }

    private void addIntersection(int x, int y) {
        if (y < minContourY) {
            minContourY = y;
        }
        if (y > maxContourY) {
            maxContourY = y;
        }
        final int count = intersectionsCount[y]++;
        int[] intersections = intersectionsWithHorizontal[y];
        if (count >= intersections.length) {
            intersections = ensureCapacity(y, (long) count + 1L);
        }
        intersections[count] = x;
    }

    private void removeAllIntersections(int y) {
        intersectionsCount[y] = 0;
        if (intersectionsWithHorizontal[y].length > START_INTERSECTIONS_CAPACITY) {
            intersectionsWithHorizontal[y] = initialIntersectionsWithHorizontal[y];
        }
    }

    private int[] ensureCapacity(int y, long newCount) {
        if (newCount > Integer.MAX_VALUE) {
            // - should not occur while filling current contours (with 31-bit indexing)
            throw new TooLargeArrayException("Too large array required");
        }

        final int length = intersectionsWithHorizontal[y].length;
        if (newCount > length) {
            final int newLength = Math.max(START_INTERSECTIONS_CAPACITY, Math.max((int) newCount,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * length))));
            return intersectionsWithHorizontal[y] = java.util.Arrays.copyOf(
                    intersectionsWithHorizontal[y], newLength);
        } else {
            return intersectionsWithHorizontal[y];
        }
    }
}
