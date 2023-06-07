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

package net.algart.contours;

import net.algart.arrays.*;
import net.algart.contexts.InterruptionException;
import net.algart.math.IRectangularArea;
import net.algart.additions.math.IRectangleFinder;
import net.algart.additions.math.IntArrayTranslatingAppender;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/**
 * <p>Joiner of {@link Contours contours}.</p>
 * <p>Note that this class guarantees full 100% joining of only 4-connected contours (external and internal).
 * Contours of 8-connected objects, in some situations, are not joined completely.
 * It allows to use more simple and efficient algorithm with a guarantee that
 * it will not require a lot of memory (more than necessary for storing results
 * and the largest matrix, necessary for "drawing" any source contour).</p>
 */
public final class ContourJoiner {
    public enum JoiningOrder {
        UNORDERED("unordered") {
            @Override
            void sortIndexes(ContourJoiner joiner, int[] indexes, int count) {
            }
        },
        NATURAL("natural") {
            @Override
            void sortIndexes(ContourJoiner joiner, int[] indexes, int count) {
                java.util.Arrays.parallelSort(indexes, 0, count);
            }
        },
        SMALL_FIRST("small-first") {
            @Override
            void sortIndexes(ContourJoiner joiner, int[] indexes, int count) {
                ArraySorter.getQuickSorter().sortIndexes(indexes, 0, count,
                        (firstIndex, secondIndex) -> {
                            final int diff1 = joiner.allMaxY[firstIndex] - joiner.allMinY[firstIndex];
                            final int diff2 = joiner.allMaxY[secondIndex] - joiner.allMinY[secondIndex];
                            return diff1 < diff2 || (diff1 == diff2 && firstIndex < secondIndex);
                        });
            }
        },
        LARGE_FIRST("large-first") {
            @Override
            void sortIndexes(ContourJoiner joiner, int[] indexes, int count) {
                ArraySorter.getQuickSorter().sortIndexes(indexes, 0, count,
                        (firstIndex, secondIndex) -> {
                            final int diff1 = joiner.allMaxY[firstIndex] - joiner.allMinY[firstIndex];
                            final int diff2 = joiner.allMaxY[secondIndex] - joiner.allMinY[secondIndex];
                            return diff1 > diff2 || (diff1 == diff2 && firstIndex > secondIndex);
                        });
            }
        };

        private final String prettyName;

        JoiningOrder(String prettyName) {
            this.prettyName = prettyName;
        }

        abstract void sortIndexes(ContourJoiner joiner, int[] indexes, int count);
    }

    public static final int MAX_GRID_STEP_LOG = 30;
    // - helps to avoid problems with overflow

    private static final int MIN_RECOMMENDED_GRID_STEP_LOG = 4;
    // - usually even faster than 3: processing squares 2x2 provides almost the same precision,
    // but number of service operations is less
    private static final int MAX_RECOMMENDED_GRID_MATRIX_SIZE = 32 * 1024 * 1024;
    // - matrix of long[] + int[], so ~384 MB

    private static final int EMPTY_POSITION = -1;
    // - must be -1! it is used while operations with bits

    private static final int REPACKING_DEFERRED_QUEUE_STEP = 16;
    private static final int REVIVING_VISITED_GRID_STEP = 64;
    private static final int CHECK_INTERRUPTION_STEP = 256;

    private final int DETAILED_DEBUG_LEVEL = 0;
    // - must be 0 for normal work; other values leads to a lot of debug printing and extremely slows down work
    private JoiningOrder joiningOrder = JoiningOrder.UNORDERED;
    // - affects only to the order of resulting contours; non-trivial order usually decrease performance
    private boolean packResultContours = true;
    private int measureTimingLevel = 0;
    private BooleanSupplier interrupter = null;

    private final Contours contours;
    private final Contours result;
    private final boolean useGrid;
    private final int gridStepLog;
    private final int numberOfContours;
    private final int[] reindexedLabels;
    private final JoinedLabelsLists joinedLists;
    private final int maxNumberOfJoinedContours;
    private final boolean[] alreadyProcessed;
    final int[] allMinX;
    final int[] allMaxX;
    final int[] allMinY;
    final int[] allMaxY;
    private final boolean[] allInternal;
    private volatile IRectangularArea containingRectangle = null;

    private int[] clusterContours = new int[256];
    private final int[] clusterContoursOffsets;
    private final int[] indexesOfClusterContours;
    // - simplified equivalent of Contours class for storing "cluster":
    // set of contours having the same reindexed label (candidates to join);
    // clusterContours contains points (x,y) of unpacked cluster contours
    private final int[] reverseIndexesOfContoursInCluster;
    private final int[] sortedIndexesOfClusterContours;
    private final IntArrayTranslatingAppender sortedIndexesOfClusterContoursAppender;
    private int numberOfClusterContours = 0;

    private final boolean[] possibleNeighboursInClusterSet;
    private final int[] possibleNeighboursIndexesInCluster;
    private int numberOfPossibleNeighbours = 0;
    private final long[] visitedGrid;
    // visitedGridCells[y,x] is a bitmap 8x8, where every element is 1 if one of the contours, forming
    // the current contour as result of joining, intersects corresponding rectangle m * m, m = 2^(gridStepLog-3)/
    // In particular, visitedGridCells[y,x]!=0L when this contours intersects rectangle 8m * 8m
    // at position x * 2^gridStepLog, y * 2^gridStepLog
    private final int[] indexesOfNonZeroVisitedGridElements;
    private int nonZeroVisitedGridElementsCount = 0;

    private int[] compressedContoursPositions = new int[256];
    // - unlike clusterContours, consists of not points, but positions (offset in matrix): y * gridDimX + x
    private long[] compressedContoursBitMaps8x8 = new long[256];
    private final int[] compressedContoursPositionsOffsets;
    private int compressedContoursPositionsLength = 0;
    // - should be equal to compressedClusterPositionsOffsets[numberOfClusterContours-1]
    private int visitedGridMinX = -1;
    private int visitedGridMaxX = -1;
    private int visitedGridMinY = -1;
    private int visitedGridMaxY = -1;
    private int visitedGridDimX = -1;
    private int visitedGridDimY = -1;
    private int visitedGridSize = -1;

    private final Contours deferredContours = Contours.newInstance();
    private int deferredContoursStart = 0;
    private final ContourHeader currentHeader = new ContourHeader();
    private boolean currentInternal = false;
    private int currentLabel = -1;
    private int reindexedCurrentLabel = -1;
    private int currentIndex = -1;
    private int[] current = JArrays.EMPTY_INTS;
    // - used also in as a work memory in buildClusterWithSameReindexedLabel
    private boolean[] currentUsage = JArrays.EMPTY_BOOLEANS;
    private int currentNumberOfPoints = 0;
    private int currentLength = 0;
    int currentMinX = -1;
    int currentMaxX = -1;
    int currentMinY = -1;
    int currentMaxY = -1;
    private int[] currentPositionsForXPlusSegments;
    private int[] currentPositionsForYPlusSegments;
    // - in particular, currentPositionsForXPlusSegments is a matrix, element (x,y) of which contains position p,
    // such that the segment p->p+1 of the current contour is the segment between points (x,y)-(x+1,y),
    // or EMPTY_POSITION if the current contour does not contain this segment
    private final MutableIntArray currentIndexesOfJoinedContours = Arrays.SMM.newEmptyIntArray();
    // - for debugging needs
    private final IRectangleFinder rectangleFinder;

    private boolean joinedInternal = false;
    private int joinedIndex = -1;
    private int[] joined = null;
    private int joinedOffset = 0;
    private boolean[] joinedUsage = JArrays.EMPTY_BOOLEANS;
    private int joinedNumberOfPoints = 0;
    private int joinedLength = 0;
    private int joinedMinX = -1;
    private int joinedMaxX = -1;
    private int joinedMinY = -1;
    private int joinedMaxY = -1;
    private int intersectionMinX = -1;
    private int intersectionMinY = -1;
    private int intersectionDimX = -1;
    private int intersectionDimY = -1;
    private int intersectionDiffX = -1;
    private int intersectionDiffY = -1;
    private int intersectionMatrixSize = -1;
    private int[] joinedPositionsForXPlusSegments;
    private int[] joinedPositionsForYPlusSegments;

    private int[] joinResult = JArrays.EMPTY_INTS;
    private boolean joinResultInternal = false;
    private int joinResultNumberOfPoints = 0;
    private int[] joinAdditionalResult = JArrays.EMPTY_INTS;
    private boolean joinAdditionalResultInternal = false;
    private int joinAdditionalResultNumberOfPoints = 0;

    private int[] workPackedPoints = JArrays.EMPTY_INTS;

    private long tCreatingInitialization = 0;
    private long tCreatingJoinedLists = 0;
    private long tCreatingAllocating = 0;
    private long tCreatingAllocatingMatrices = 0;
    private long tCreatingAnalysingRectangles = 0;
    private long tCreatingClusterRectangles = 0;
    private long tTotalWork = 0;
    private long tSimple = 0;
    private long tBuildingCluster = 0;
    private long tPreprocessingGrid = 0;
    private long tInitializeNewCurrent = 0;
    private long tFindInitialNeighbours = 0;
    private long tReadingCurrent = 0;
    private long tAnalysing = 0;
    private long tRevivingGrid = 0;
    private long tJoiningCheckingGridSuccess = 0;
    private long tJoiningCheckingGridFail = 0;
    private long tJoiningReadingNewJoined = 0;
    private long tJoiningScanning1Success = 0;
    private long tJoiningScanning1Fail = 0;
    private long tJoiningScanning2Success = 0;
    private long tJoiningScanning2Fail = 0;
    private long tJoiningSwitching = 0;
    private long tJoiningAddingToGridAndNeighbours = 0;
    private long tWritingContours = 0;
    private long tOffsetOfMinX = 0;
    private long tFindFreeSegment = 0;
    private long tFindFreeUnusedSegment = 0;
    private int sMinCheckedContoursCount = Integer.MAX_VALUE;
    private int sMaxCheckedContoursCount = 0;
    private long sSumCheckedContoursCount = 0;
    private int sNumberOfCheckedContoursLoops = 0;
    private int sMinJoinedContoursCount = Integer.MAX_VALUE;
    private int sMaxJoinedContoursCount = 0;
    private long sSumJoinedContoursCount = 0;
    private int sNumberOfJoinedContours = 0;
    private int sMinDeferredContoursCount = Integer.MAX_VALUE;
    private int sMaxDeferredContoursCount = 0;
    private long sSumDeferredContoursCount = 0;
    private long sNumberOfDeferredContoursChecks = 0;
    private long sTotalNumberOfDeferredContours = 0;
    private long sNumberOfScanningCurrent = 0;
    private long sNumberOfSuccessfulScanningCurrent = 0;
    private long sNumberOfScanningJoined = 0;
    private long sNumberOfSuccessfulScanningJoined = 0;

    private ContourJoiner(Contours contours, Integer gridStepLog, int[] joinedLabelsMap, int defaultJoinedLabel) {
        this.contours = Objects.requireNonNull(contours, "Null contours");
        if (defaultJoinedLabel < 0) {
            throw new IllegalArgumentException("Negative defaultJoinedLabel = " + defaultJoinedLabel);
        }
        if (gridStepLog != null) {
            if (gridStepLog < 0) {
                throw new IllegalArgumentException("Negative gridStepLog");
            }
            if (gridStepLog != 0 && (gridStepLog < 3 || gridStepLog > MAX_GRID_STEP_LOG)) {
                // - why 3? 2^3 * 2^3 = 64: number of bits in 1 long value
                throw new IllegalArgumentException("gridStepLog (grid step logarithm) = " + gridStepLog
                        + " is non-zero and is out of required range 3.." + MAX_GRID_STEP_LOG);
            }
            useGrid = gridStepLog != 0;
        } else {
            useGrid = true;
        }
        long t1 = System.nanoTime();
        this.result = Contours.newInstance();
        this.numberOfContours = contours.numberOfContours();
        assert numberOfContours <= Contours.MAX_NUMBER_OF_CONTOURS;
        this.reindexedLabels = IntStream.range(0, contours.numberOfContours()).parallel().map(
                contourIndex -> reindex(contours.getObjectLabel(contourIndex), joinedLabelsMap, defaultJoinedLabel))
                .toArray();
        long t2 = System.nanoTime();
        tCreatingInitialization = t2 - t1;

        this.joinedLists = new JoinedLabelsLists(reindexedLabels);
        // - must be built BEFORE filling allMin/Max: we need correct hasNeighboursToJoin() method
        this.maxNumberOfJoinedContours = joinedLists.maxListLength; // maxJoinedListLength(joinedLabelsLists);
        assert maxNumberOfJoinedContours <= numberOfContours;
        long t3 = System.nanoTime();
        tCreatingJoinedLists = t3 - t2;

        this.alreadyProcessed = new boolean[numberOfContours];
        // - zero-filled by Java
        this.clusterContoursOffsets = new int[maxNumberOfJoinedContours + 1];
        this.compressedContoursPositionsOffsets = new int[maxNumberOfJoinedContours + 1];
        this.indexesOfClusterContours = new int[maxNumberOfJoinedContours];
        this.sortedIndexesOfClusterContours = new int[maxNumberOfJoinedContours];
        this.reverseIndexesOfContoursInCluster = new int[numberOfContours];
        this.possibleNeighboursIndexesInCluster = new int[maxNumberOfJoinedContours];
        this.possibleNeighboursInClusterSet = new boolean[numberOfContours];
        // - zero-filled by Java
        this.allMinX = new int[numberOfContours];
        this.allMaxX = new int[numberOfContours];
        this.allMinY = new int[numberOfContours];
        this.allMaxY = new int[numberOfContours];
        this.allInternal = new boolean[numberOfContours];
        final int[] allMatrixSize = new int[numberOfContours];
        // - zero-filled by Java
        long t4 = System.nanoTime();
        tCreatingAllocating += t4 - t3;

        IntStream.range(0, (numberOfContours + 255) >>> 8).parallel().forEach(block -> {
            final ContourHeader header = new ContourHeader();
            for (int i = block << 8, to = (int) Math.min((long) i + 256, numberOfContours); i < to; i++) {
                contours.getHeader(header, i);
                allInternal[i] = header.isInternalContour();
                if (!hasNeighboursToJoin(i)) {
                    continue;
                    // - skip checking this contour at all: we will not process it;
                    // it must be AFTER initializing this.joinedLists
                }
                final int minX = header.minX();
                final int maxX = header.maxX();
                final int minY = header.minY();
                final int maxY = header.maxY();
                allMinX[i] = minX;
                allMaxX[i] = maxX;
                allMinY[i] = minY;
                allMaxY[i] = maxY;
                final int diffX = maxX - minX;
                final int diffY = maxY - minY;
                if (diffX < 0 || diffY < 0) {
                    throw new AssertionError("Overflow in sizes of containing rectangle for contour #"
                            + i + "; it must be impossible due to check of Contour2DArray.MAX_ABSOLUTE_COORDINATE");
                }
                final long matrixSize = (1 + (long) diffX) * (1 + (long) diffY);
                if (matrixSize > Integer.MAX_VALUE) {
                    throw new TooLargeArrayException("Containing rectangles of contours must have area "
                            + "<= Integer.MAX_VALUE pixels, but contour #" + i + " is "
                            + (1 + (long) diffX) + "x" + (1 + (long) diffY));
                }
                allMatrixSize[i] = (int) matrixSize;
            }
        });
        int maxContainingMatrixSize = 0;
        for (int k = 0; k < numberOfContours; k++) {
            int matrixSize = allMatrixSize[k];
            if (matrixSize > maxContainingMatrixSize) {
                maxContainingMatrixSize = matrixSize;
            }
            // - initial estimation for positions matrices; in normal situation,
            // there should not be necessary to reallocate them in ensureCapacityForPositionsMatrices
        }
        long t5 = System.nanoTime();
        tCreatingAnalysingRectangles += t5 - t4;

        final int gridMatrixSize;
        if (useGrid) {
            joinedLists.initializeNonEmptyClusterRectangles(allMinX, allMaxX, allMinY, allMaxY);
            int chosenGridStepLog;
            if (gridStepLog != null) {
                chosenGridStepLog = gridStepLog;
                gridMatrixSize = (int) joinedLists.maxClusterGridMatrixSize(chosenGridStepLog, true);
            } else {
                chosenGridStepLog = MIN_RECOMMENDED_GRID_STEP_LOG;
                long size = joinedLists.maxClusterGridMatrixSize(chosenGridStepLog, false);
                while (chosenGridStepLog < MAX_GRID_STEP_LOG && size > MAX_RECOMMENDED_GRID_MATRIX_SIZE) {
                    chosenGridStepLog++;
                    size = joinedLists.maxClusterGridMatrixSize(chosenGridStepLog, false);
                }
                assert size == (int) size : "impossible: after division by 2^" + MAX_GRID_STEP_LOG
                        + ", the cluster size is still > " + MAX_RECOMMENDED_GRID_MATRIX_SIZE;
                gridMatrixSize = (int) size;
            }
            this.gridStepLog = chosenGridStepLog;
            // - we need a separate check: there is no guarantee that b/16-a/16+1 <= (b-a)/16+1
        } else {
            this.gridStepLog = -1;
            gridMatrixSize = -1;
        }

        long t6 = System.nanoTime();
        tCreatingClusterRectangles += t6 - t5;

        this.currentPositionsForXPlusSegments = new int[maxContainingMatrixSize];
        this.currentPositionsForYPlusSegments = new int[maxContainingMatrixSize];
        this.joinedPositionsForXPlusSegments = new int[maxContainingMatrixSize];
        this.joinedPositionsForYPlusSegments = new int[maxContainingMatrixSize];
        this.sortedIndexesOfClusterContoursAppender = new IntArrayTranslatingAppender(
                sortedIndexesOfClusterContours, i -> indexesOfClusterContours[i]);
        this.rectangleFinder = IRectangleFinder.getEmptyInstance();
        this.rectangleFinder.setIndexActual(i -> !alreadyProcessed[indexesOfClusterContours[i]]);
        this.visitedGrid = useGrid ? new long[gridMatrixSize] : null;
        this.indexesOfNonZeroVisitedGridElements = useGrid ? new int[gridMatrixSize] : null;
        long t7 = System.nanoTime();
        // - in constructor we cannot use nanoTime() method of this object
        tCreatingAllocatingMatrices = t7 - t6;
    }

    public static ContourJoiner newInstance(Contours contours, Integer gridStepLog, int[] joinedLabelsMap) {
        Objects.requireNonNull(joinedLabelsMap, "Null joinedLabelsMap");
        return new ContourJoiner(contours, gridStepLog, joinedLabelsMap, 157);
    }

    public static ContourJoiner newInstance(
            Contours contours,
            Integer gridStepLog,
            int[] joinedLabelsMap,
            int defaultJoinedLabel) {
        return new ContourJoiner(contours, gridStepLog, joinedLabelsMap, defaultJoinedLabel);
    }

    public JoiningOrder getJoiningOrder() {
        return joiningOrder;
    }

    public ContourJoiner setJoiningOrder(JoiningOrder joiningOrder) {
        this.joiningOrder = Objects.requireNonNull(joiningOrder, "Null joining order");
        return this;
    }

    public boolean isPackResultContours() {
        return packResultContours;
    }

    public ContourJoiner setPackResultContours(boolean packResultContours) {
        this.packResultContours = packResultContours;
        return this;
    }

    public int getMeasureTimingLevel() {
        return measureTimingLevel;
    }

    public ContourJoiner setMeasureTimingLevel(int measureTimingLevel) {
        this.measureTimingLevel = measureTimingLevel;
        return this;
    }

    public BooleanSupplier getInterrupter() {
        return interrupter;
    }

    public ContourJoiner setInterrupter(BooleanSupplier interrupter) {
        this.interrupter = interrupter;
        return this;
    }

    public IRectangularArea containingRectangle() {
        IRectangularArea containingRectangle = this.containingRectangle;
        if (containingRectangle == null && numberOfContours > 0) {
            int containingMinX = Integer.MAX_VALUE;
            int containingMaxX = Integer.MIN_VALUE;
            int containingMinY = Integer.MAX_VALUE;
            int containingMaxY = Integer.MIN_VALUE;
            final ContourHeader header = new ContourHeader();
            for (int k = 0; k < numberOfContours; k++) {
                final int minX, maxX, minY, maxY;
                if (hasNeighboursToJoin(k)) {
                    minX = allMinX[k];
                    maxX = allMaxX[k];
                    minY = allMinY[k];
                    maxY = allMaxY[k];
                } else {
                    contours.getHeader(header, k);
                    minX = header.minX();
                    maxX = header.maxX();
                    minY = header.minY();
                    maxY = header.maxY();
                }
                if (minX < containingMinX) {
                    containingMinX = minX;
                }
                if (maxX > containingMaxX) {
                    containingMaxX = maxX;
                }
                if (minY < containingMinY) {
                    containingMinY = minY;
                }
                if (maxY > containingMaxY) {
                    containingMaxY = maxY;
                }

            }
            this.containingRectangle = containingRectangle = IRectangularArea.valueOf(
                    containingMinX, containingMinY, containingMaxX, containingMaxY);
        }
        return containingRectangle;
    }

    public Contours joinContours() {
        long t1 = System.nanoTime();
        JArrays.fillBooleanArray(alreadyProcessed, false);
        // - to be on the safe side (for multiple calls)
        result.clear();
        for (int k = 0; k < numberOfContours; k++) {
            addContourAndItsContinuations(k);
        }
        long t2 = System.nanoTime();
        tTotalWork = t2 - t1;
        return result;
    }

    public boolean needToJoin(int objectIndex1, int objectIndex2) {
        return reindexedLabels[objectIndex1] == reindexedLabels[objectIndex2];
    }

    public boolean hasNeighboursToJoin(int objectIndex) {
        return joinedLists.hasNeighboursToJoin(reindexedLabels[objectIndex]);
    }

    public String timingInfo() {
        final long tActualJoining = tRevivingGrid
                + tJoiningReadingNewJoined
                + tJoiningScanning1Success + tJoiningScanning1Fail
                + tJoiningScanning2Success + +tJoiningScanning2Fail
                + tJoiningSwitching + tJoiningAddingToGridAndNeighbours;
        final long tCreating = tCreatingInitialization + tCreatingJoinedLists
                + tCreatingAnalysingRectangles + tCreatingAllocating
                + tCreatingClusterRectangles + tCreatingAllocatingMatrices;
        final String common = String.format(Locale.US,
                "%d contours joined to %d ones (%s, %s) in %.3f ms (%.6f ms/contour, "
                        + "maximal number of joined %d): "
                        + "\n  %.3f creating (%.3f initialization + %.3f joined lists "
                        + "+ %.3f rectangles + %.3f clusters + %.3f and %.3f allocation);"
                        + "\n  %.3f processing",
                contours.numberOfContours(), result.numberOfContours(),
                joiningOrder.prettyName,
                useGrid ? "grid cells " + (1 << gridStepLog) + "x" + (1 << gridStepLog) : "no grid",
                (tCreating + tTotalWork) * 1e-6,
                ((tCreating + tTotalWork) * 1e-6) / contours.numberOfContours(),
                maxNumberOfJoinedContours,
                tCreating * 1e-6,
                tCreatingInitialization * 1e-6,
                tCreatingJoinedLists * 1e-6,
                tCreatingAnalysingRectangles * 1e-6,
                tCreatingClusterRectangles * 1e-6,
                tCreatingAllocating * 1e-6,
                tCreatingAllocatingMatrices * 1e-6,
                tTotalWork * 1e-6);
        if (measureTimingLevel == 0) {
            return common;
        }
        final String newCurrent = String.format(Locale.US, "%s:"
                        + "\n    %.3f simple adding (no other contours with same label),"
                        + "\n    %.3f building cluster: unpacking contours with the same reindexed label,"
                        + "\n    %.3f preprocessing grid (reallocating, compressing contours),"
                        + "\n    %.3f initializing new current contour,"
                        + "\n    %.3f finding initial possible neighbours,"
                        + "\n    %.3f reading current contour,",
                common,
                tSimple * 1e-6,
                tBuildingCluster * 1e-6,
                tPreprocessingGrid * 1e-6,
                tInitializeNewCurrent * 1e-6,
                tFindInitialNeighbours * 1e-6,
                tReadingCurrent * 1e-6);
        final String statistics = String.format(Locale.US,
                "\n    number of checks of contours before joining 1 new contour: "
                        + "min %d, max %d, mean %.3f, total %d"
                        + "\n    number of contours, joined to single one: "
                        + "min %d, max %d, mean %.3f, total %d"
                        + "\n    number of deferred contours: "
                        + "min %d, max %d, mean %.3f, total %d"
                        + "\n    number of successful/total scannings current contour: %d/%d"
                        + "\n    number of successful/total scannings joined contour: %d/%d",
                sMinCheckedContoursCount, sMaxCheckedContoursCount,
                sSumCheckedContoursCount / (double) sNumberOfCheckedContoursLoops,
                sSumCheckedContoursCount,
                sMinJoinedContoursCount, sMaxJoinedContoursCount,
                sSumJoinedContoursCount / (double) sNumberOfJoinedContours,
                sSumJoinedContoursCount,
                sMinDeferredContoursCount, sMaxDeferredContoursCount,
                sSumDeferredContoursCount / (double) sNumberOfDeferredContoursChecks,
                sTotalNumberOfDeferredContours,
                sNumberOfSuccessfulScanningCurrent, sNumberOfScanningCurrent,
                sNumberOfSuccessfulScanningJoined, sNumberOfScanningJoined);
        if (measureTimingLevel == 1) {
            return String.format(Locale.US, "%s"
                            + "\n    %.3f analysis,"
                            + "\n    %.3f writing result contours%s",
                    newCurrent,
                    tAnalysing * 1e-6,
                    tWritingContours * 1e-6,
                    statistics);
        }
        return String.format(Locale.US, "%s"
                        + "\n    %.3f analysis, including:"
                        + "\n      %.6f quick checks (including rectangles)%s,"
                        + "\n      %.6f actual joining, including:"
                        + "\n        %.6f reviving visited grid,"
                        + "\n        %.6f initializing/reading 2nd (joined) contour,"
                        + "\n        %.6f/%.6f scanning current contour (success/fail),"
                        + "\n        %.6f/%.6f scanning joined contour (success/fail),"
                        + "\n        %.6f switching algorithm,"
                        + "\n        %.6f adding information to neighbours lists/grid,%s"
                        + "\n    %.3f writing result contours%s",
                newCurrent,
                tAnalysing * 1e-6,
                (tAnalysing - tActualJoining) * 1e-6,
                measureTimingLevel < 3 ? "" :
                        String.format(Locale.US, ", including %.6f/%.6f checking grid (success/fail) ",
                                tJoiningCheckingGridSuccess * 1e-6, tJoiningCheckingGridFail * 1e-6),
                tActualJoining * 1e-6,
                tRevivingGrid * 1e-6,
                tJoiningReadingNewJoined * 1e-6,
                tJoiningScanning1Success * 1e-6, tJoiningScanning1Fail * 1e-6,
                tJoiningScanning2Success * 1e-6, tJoiningScanning2Fail * 1e-6,
                tJoiningSwitching * 1e-6,
                tJoiningAddingToGridAndNeighbours * 1e-6,
                measureTimingLevel < 3 ? "" :
                        String.format(Locale.US, " including: "
                                        + "\n          %.6f searching the leftmost point,"
                                        + "\n          %.6f finding free segment (1st iteration),"
                                        + "\n          %.6f finding free unused segment (further iterations),",
                                tOffsetOfMinX * 1e-6,
                                tFindFreeSegment * 1e-6,
                                tFindFreeUnusedSegment * 1e-6),
                tWritingContours * 1e-6,
                statistics);
    }

    private void addContourAndItsContinuations(final int contourIndex) {
        if (alreadyProcessed[contourIndex]) {
            return;
        }
        long t1 = nanoTime1();
        if (!hasNeighboursToJoin(contourIndex)) {
            contours.getHeader(currentHeader, contourIndex);
            currentHeader.clearContourTouchingMatrixBoundary();
            // - not relevant for joined contours
            currentHeader.setObjectLabel(reindexedLabels[contourIndex]);
            if (packResultContours) {
                final ContourLength contourLength = new ContourLength();
                final long lengthAndOffset = contours.getContourLengthAndOffset(contourIndex);
                workPackedPoints = Contours.packContourAndReallocateResult(
                        workPackedPoints, contourLength,
                        contours.points,
                        Contours.extractOffset(lengthAndOffset),
                        Contours.extractLength(lengthAndOffset));
                result.addContour(currentHeader, workPackedPoints, 0, contourLength.getArrayLength());
            } else {
                result.addContour(contours, contourIndex);
            }
            tSimple += nanoTime1() - t1;
            return;
            // - this contour has no stitched neighbours
        }
        buildClusterWithSameReindexedLabel(contourIndex);
        long t2 = nanoTime1();
        tBuildingCluster += t2 - t1;
        preprocessCompressedContours();
        long t3 = nanoTime1();
        tPreprocessingGrid += t3 - t2;
        joinCluster();
    }

    private void buildClusterWithSameReindexedLabel(int contourIndex) {
        int count = 0;
        int pointsLength = 0;
        final ContourLength contourLength = new ContourLength();
        final int reindexed = reindexedLabels[contourIndex];
        for (int i = joinedLists.offsets[reindexed], to = joinedLists.offsets[reindexed + 1]; i < to; i++) {
            final int index = joinedLists.indexes[i];
            assert index >= contourIndex : "invalid order in lists, built by buildJoinedLists()";
            indexesOfClusterContours[count] = index;
            reverseIndexesOfContoursInCluster[index] = count;
            current = contours.unpackContourAndReallocateResult(current, contourLength, index);
            final int length = contourLength.getArrayLength();
            ensureCapacityForUnpackedClusterAndReallocate((long) pointsLength + (long) length);
            System.arraycopy(current, 0, clusterContours, pointsLength, length);
            clusterContoursOffsets[count] = pointsLength;
            pointsLength += length;
            count++;
        }
        clusterContoursOffsets[count] = pointsLength;
        // - single extra element
        assert count == joinedLists.length(reindexed);
        if (count <= 1) {
            throw new AssertionError("Empty cluster due to incorrect hasNeighboursToJoin: " + count);
        }
        this.numberOfClusterContours = count;
        if (useGrid) {
            int clusterMinX = joinedLists.clusterMinX[reindexed];
            int clusterMaxX = joinedLists.clusterMaxX[reindexed];
            int clusterMinY = joinedLists.clusterMinY[reindexed];
            int clusterMaxY = joinedLists.clusterMaxY[reindexed];
            initializeVisitedGrid(clusterMinX, clusterMaxX, clusterMinY, clusterMaxY);
        }
    }

    private void initializeVisitedGrid(int clusterMinX, int clusterMaxX, int clusterMinY, int clusterMaxY) {
        assert useGrid;
        this.visitedGridMinX = clusterMinX >> gridStepLog;
        this.visitedGridMaxX = clusterMaxX >> gridStepLog;
        this.visitedGridMinY = clusterMinY >> gridStepLog;
        this.visitedGridMaxY = clusterMaxY >> gridStepLog;
        // - note: it is floor(xxx / 2^gridStepLog) even for negative numbers
        this.visitedGridDimX = this.visitedGridMaxX - this.visitedGridMinX + 1;
        this.visitedGridDimY = this.visitedGridMaxY - this.visitedGridMinY + 1;
        final long gridMatrixSize = (long) visitedGridDimX * (long) visitedGridDimY;
        if (gridMatrixSize > visitedGrid.length) {
            throw new AssertionError("Too large cluster: " + gridMatrixSize
                    + " > maximal length " + visitedGrid.length + ", found in the constructor");
        }
        this.visitedGridSize = (int) gridMatrixSize;
    }

    private void preprocessCompressedContours() {
        if (useGrid) {
            compressAllClusterContours();
            nonZeroVisitedGridElementsCount = 0;
            java.util.Arrays.fill(visitedGrid, 0, visitedGridSize, 0L);
        }
    }

    private void cleanupVisitedGrid() {
        if (!useGrid) {
            return;
        }
//        System.out.println(nonZeroVisitedGridElementsCount + "/" + visitedGridSize + ", " + numberOfDeferredContours());
        if (nonZeroVisitedGridElementsCount >= visitedGridSize >> 1) {
            java.util.Arrays.fill(visitedGrid, 0, visitedGridSize, 0L);
            // - the following loop will be slower than the simplest filling
        } else {
            for (int k = 0; k < nonZeroVisitedGridElementsCount; k++) {
                visitedGrid[indexesOfNonZeroVisitedGridElements[k]] = 0L;
            }
        }
        nonZeroVisitedGridElementsCount = 0;
        if (DETAILED_DEBUG_LEVEL >= 1) {
            for (int k = 0; k < visitedGridSize; k++) {
                if (visitedGrid[k] != 0L) {
                    throw new AssertionError("visitedGrid was not cleaned properly");
                }
            }
        }
    }

    private void compressAllClusterContours() {
        assert useGrid : "this function must not be used when grid is not used: gridStepLog = " + gridStepLog;
        compressedContoursPositionsLength = 0;
        compressedContoursPositionsOffsets[0] = 0;
        for (int k = 0; k < numberOfClusterContours; k++) {
            compressClusterContour(k);
            compressedContoursPositionsOffsets[k + 1] = compressedContoursPositionsLength;
        }
    }

    private void reviveVisitedGridForCurrentContour() {
        if (useGrid) {
            long t1 = nanoTime2();
            cleanupVisitedGrid();
            markPointsAtVisitedGridAndStoreNonZeroIndexes(current, 0, currentLength);
            for (int k = 0, m = numberOfDeferredContours(); k < m; k++) {
                final int pointsFrom = deferredContourOffset(k);
                final int pointsTo = pointsFrom + deferredContourLength(k);
                markPointsAtVisitedGridAndStoreNonZeroIndexes(deferredContours.points, pointsFrom, pointsTo);
            }
            long t2 = nanoTime2();
            tRevivingGrid += t2 - t1;
        }
    }

    private void compressClusterContour(final int indexInCluster) {
        final int index = indexesOfClusterContours[indexInCluster];
        final int minX = allMinX[index] >> gridStepLog;
        final int minY = allMinY[index] >> gridStepLog;
        final int dimX = (allMaxX[index] >> gridStepLog) - minX + 1;
        final int dimY = (allMaxY[index] >> gridStepLog) - minY + 1;

        final int from = clusterContoursOffsets[indexInCluster];
        final int to = clusterContoursOffsets[indexInCluster + 1];
        compressContour(clusterContours, from, to, minX, minY, dimX, dimY);
    }

    private void compressContour(int[] points, int pointsFrom, int pointsTo, int minX, int minY, int dimX, int dimY) {
        final int workSpaceSize = dimX * dimY;
        java.util.Arrays.fill(visitedGrid, 0, workSpaceSize, 0L);
        // - we use the beginning of visitedGrid as a work space
        markPointsAtGrid(points, pointsFrom, pointsTo, minX, minY, dimX, dimY);
        retrieveAllCompressedPointsFromGrid(minX, minY, dimX, dimY);
    }

    // Works with a local starting fragment of visitedGrid in coordinates of a joined contour minX/Y..maxX/Y
    private void markPointsAtGrid(int[] points, int pointsFrom, int pointsTo, int minX, int minY, int dimX, int dimY) {
        final long[] workGrid = visitedGrid;
        int lastXBit = Integer.MAX_VALUE;
        int lastYBit = Integer.MAX_VALUE;
        final int gridStepLogForBits = gridStepLog - 3;
        assert gridStepLogForBits >= 0;
        for (int i = pointsFrom; i < pointsTo; i += 2) {
            final int xBit = points[i] >> gridStepLogForBits;
            final int yBit = points[i + 1] >> gridStepLogForBits;
            if (xBit == lastXBit && yBit == lastYBit) {
                continue;
            }
            lastXBit = xBit;
            lastYBit = yBit;
            final int x = xBit >> 3;
            final int y = yBit >> 3;
            final int bitInLong = (yBit & 7) << 3 | (xBit & 7);
            final int gridX = x - minX;
            final int gridY = y - minY;
            assert 0 <= gridX && gridX < dimX
                    : "compressed gridX=" + gridX + " is out of range 0.." + dimX + "-1";
            assert 0 <= gridY && gridY < dimY
                    : "compressed gridY=" + gridY + " is out of range 0.." + dimY + "-1";
            final int position = gridY * dimX + gridX;
            workGrid[position] |= 1L << bitInLong;
        }
    }

    // Works with main visitedGrid in coordinates of the cluster
    private void markPointsAtVisitedGridAndStoreNonZeroIndexes(int[] points, int pointsFrom, int pointsTo) {
        final int minX = visitedGridMinX;
        final int minY = visitedGridMinY;
        final int dimX = visitedGridDimX;
        final int dimY = visitedGridDimY;
        int lastXBit = Integer.MAX_VALUE;
        int lastYBit = Integer.MAX_VALUE;
        final int gridStepLogForBits = gridStepLog - 3;
        assert gridStepLogForBits >= 0;
        for (int i = pointsFrom; i < pointsTo; i += 2) {
            final int xBit = points[i] >> gridStepLogForBits;
            final int yBit = points[i + 1] >> gridStepLogForBits;
            if (xBit == lastXBit && yBit == lastYBit) {
                continue;
            }
            lastXBit = xBit;
            lastYBit = yBit;
            final int x = xBit >> 3;
            final int y = yBit >> 3;
            final int bitInLong = (yBit & 7) << 3 | (xBit & 7);
            final int gridX = x - minX;
            final int gridY = y - minY;
            assert 0 <= gridX && gridX < dimX
                    : "compressed gridX=" + gridX + " is out of range 0.." + dimX + "-1";
            assert 0 <= gridY && gridY < dimY
                    : "compressed gridY=" + gridY + " is out of range 0.." + dimY + "-1";
            final int position = gridY * dimX + gridX;
            final long previous = visitedGrid[position];
            visitedGrid[position] = previous | (1L << bitInLong);
            if (previous == 0L) {
                indexesOfNonZeroVisitedGridElements[nonZeroVisitedGridElementsCount++] = position;
            }
        }
    }

    private void retrieveAllCompressedPointsFromGrid(int minX, int minY, int dimX, int dimY) {
        final long[] workGrid = visitedGrid;
        final int fullGridXFrom = minX - visitedGridMinX;
        final int fullGridXTo = fullGridXFrom + dimX;
        assert fullGridXFrom >= 0 && fullGridXTo <= visitedGridDimX
                : "x-range " + fullGridXFrom + ".." + fullGridXTo + " is out of range 0.." + visitedGridDimX;
        final int fullGridYFrom = minY - visitedGridMinY;
        final int fullGridYTo = fullGridYFrom + dimY;
        assert fullGridYFrom >= 0 && fullGridYTo <= visitedGridDimY
                : " y-range " + fullGridYFrom + ".." + fullGridYTo + " is out of range 0.." + visitedGridDimY;
        final int increment = visitedGridDimX - dimX;
        int fullGridOffset = fullGridYFrom * visitedGridDimX + fullGridXFrom;
        int count = compressedContoursPositionsLength;
        for (int fullGridY = fullGridYFrom, offset = 0; fullGridY < fullGridYTo; fullGridY++) {
            assert fullGridOffset == fullGridY * visitedGridDimX + fullGridXFrom;
            for (int fullGridX = fullGridXFrom; fullGridX < fullGridXTo; fullGridX++, offset++) {
                final long bitMap8x8 = workGrid[offset];
                if (bitMap8x8 != 0L) {
                    if (count >= compressedContoursPositions.length) {
                        ensureCapacityForCompressedClusterAndReallocate((long) count + 1);
                    }
                    compressedContoursPositions[count] = fullGridOffset;
                    compressedContoursBitMaps8x8[count] = bitMap8x8;
                    count++;
                }
                fullGridOffset++;
            }
            fullGridOffset += increment;
        }
        compressedContoursPositionsLength = count;
    }

    private boolean isCompressedContourProbablyVisited(final int indexInCluster) {
        long t1 = nanoTime3();
        assert useGrid;
        // - it is better to check this outside
        final int from = compressedContoursPositionsOffsets[indexInCluster];
        final int to = compressedContoursPositionsOffsets[indexInCluster + 1];
        for (int i = from; i < to; i++) {
            if ((visitedGrid[compressedContoursPositions[i]] & compressedContoursBitMaps8x8[i]) != 0) {
                tJoiningCheckingGridSuccess += nanoTime3() - t1;
                return true;
            }
        }
        tJoiningCheckingGridFail += nanoTime3() - t1;
        return false;
    }

    private void addCompressedContourToVisitedGrid(final int indexInCluster) {
        if (!useGrid) {
            return;
        }
        final int from = compressedContoursPositionsOffsets[indexInCluster];
        final int to = compressedContoursPositionsOffsets[indexInCluster + 1];
        for (int i = from; i < to; i++) {
            final int position = compressedContoursPositions[i];
            assert position >= 0 && position < visitedGridSize :
                    "position[" + i + "]=" + position + " is out of grid "
                            + visitedGridDimX + "x" + visitedGridDimY + "=" + visitedGridSize;
            final long bitMap8x8 = compressedContoursBitMaps8x8[i];
            assert bitMap8x8 != 0 : "Zero bitmap 8x8 at position " + position;
            final long previous = visitedGrid[position];
            visitedGrid[position] = bitMap8x8 | previous;
            if (previous == 0) {
                indexesOfNonZeroVisitedGridElements[nonZeroVisitedGridElementsCount++] = position;
            }
        }
    }

    private void joinCluster() {
        assert numberOfClusterContours > 1;
        reindexedCurrentLabel = reindexedLabels[indexesOfClusterContours[0]];
        rectangleFinder.setIndexedRectangles(
                allMinX, allMaxX, allMinY, allMaxY,
                indexesOfClusterContours, numberOfClusterContours);
        // - so, this finder will search only among numberOfClusterContours objects (usually little amount)
        long joiningCount = 0;
        for (int k = 0; k < numberOfClusterContours; k++) {
            final int currentIndex = indexesOfClusterContours[k];
            if (alreadyProcessed[currentIndex]) {
                continue;
            }
//            System.out.printf("%d/%d...%n", k, n);
            long t1 = nanoTime1();
            initializeCurrentContour(k, currentIndex);
            removeAllPreviouslyAddedPossibleNeighbours();
            long t2 = nanoTime1();
            tInitializeNewCurrent += t2 - t1;
            addPossibleNeighbours(currentIndex);
            long t3 = nanoTime1();
            tFindInitialNeighbours += t3 - t2;
            readCurrentContour();
            long t4 = nanoTime1();
            tReadingCurrent += t4 - t3;
            do {
                t4 = nanoTime1();
                boolean atLeastOneSuccessfullyJoined;
                do {
                    joiningCount++;
//                    if ((joiningCount & 0xFFF) == 0) System.out.printf("\r%d...    ", joiningCount);
                    if (interrupter != null
                            && (joiningCount % CHECK_INTERRUPTION_STEP) == 0
                            && interrupter.getAsBoolean()) {
                        throw new InterruptionException("Contours joiner was interrupted while processing contour #"
                                + indexesOfClusterContours[0] + "/" + numberOfContours + " after "
                                + joiningCount + " joining actions");
                    }
                    if (joiningCount % REVIVING_VISITED_GRID_STEP == 0) {
                        reviveVisitedGridForCurrentContour();
                    }
                    atLeastOneSuccessfullyJoined = growByContoursFromCluster();
                } while (atLeastOneSuccessfullyJoined && currentNumberOfPoints > 0);
                // - no sense to continue, if currentNumberOfPoints = 0: the contour was actually removed
                long t5 = nanoTime1();
                tAnalysing += t5 - t4;
                correctJoinedContoursStatistics();
                writeContour(current, currentNumberOfPoints, currentInternal);
                long t6 = nanoTime1();
                tWritingContours += t6 - t5;
            } while (loadCurrentContourFromDeferred());
            alreadyProcessed[currentIndex] = true;
        }
    }

    private boolean growByContoursFromCluster() {
        final int m = numberOfPossibleNeighbours;
        // - note: findIntersected() DOES NOT returns indexes with alreadyProcessed[k]=true
        boolean atLeastOneSuccessfullyJoined = false;
        final boolean noGrid = !useGrid;
        for (int i = 0; i < m; i++) {
            final int joinedIndex = possibleNeighboursIndexesInCluster[i];
            assert joinedIndex > currentIndex;
            if (!alreadyProcessed[joinedIndex] && (noGrid
                    || isCompressedContourProbablyVisited(reverseIndexesOfContoursInCluster[joinedIndex]))) {
                if (joinContour(joinedIndex)) {
                    alreadyProcessed[joinedIndex] = true;
                    atLeastOneSuccessfullyJoined = true;
                    break;
                }
            }
        }
        correctQuickChecksStatistics(m);
        return atLeastOneSuccessfullyJoined;
    }

    private void removeAllPreviouslyAddedPossibleNeighbours() {
        for (int k = 0; k < numberOfPossibleNeighbours; k++) {
            possibleNeighboursInClusterSet[possibleNeighboursIndexesInCluster[k]] = false;
        }
        numberOfPossibleNeighbours = 0;
    }

    private void addPossibleNeighbours(int contourIndex) {
        sortedIndexesOfClusterContoursAppender.reset();
        rectangleFinder.findIntersecting(
                allMinX[contourIndex], allMaxX[contourIndex], allMinY[contourIndex], allMaxY[contourIndex],
                sortedIndexesOfClusterContoursAppender);
        final int m = sortedIndexesOfClusterContoursAppender.offset();
        joiningOrder.sortIndexes(this, sortedIndexesOfClusterContours, m);
        for (int k = 0; k < m; k++) {
            final int possibleNeighbour = sortedIndexesOfClusterContours[k];
            // - index in original contours array
            if (possibleNeighbour > currentIndex
                    // - it is better not to add contours <= currentIndex into lists,
                    // than to ignore them in growing loop
                    && !possibleNeighboursInClusterSet[possibleNeighbour]
                    && !alreadyProcessed[possibleNeighbour]) {
                possibleNeighboursInClusterSet[possibleNeighbour] = true;
                possibleNeighboursIndexesInCluster[numberOfPossibleNeighbours++] = possibleNeighbour;
            }
        }
    }

    /*
    // Slower old alternative for joinCluster(); not used now
    // Here rectanglesIntersectionFinder is an instance of SimpleRectanglesIntersectionFinder,
    // stored in SimpleRectanglesIntersectionFinderLegacy.java.txt in extensions (or in GIT history)
    private void joinClusterBasedOnGrowingRectangle() {
        assert numberOfClusterContours > 1;
        reindexedCurrentLabel = reindexedLabels[indexesOfClusterContours[0]];
        rectanglesIntersectionFinder.setCheckedIndexes(indexesOfClusterContours, numberOfClusterContours);
        // - so, this finder will search only among numberOfClusterContours objects (usually little number)
        long joiningCount = 0;
        for (int k = 0; k < numberOfClusterContours; k++) {
            final int currentIndex = indexesOfClusterContours[k];
            if (alreadyProcessed[currentIndex]) {
                continue;
            }
//            System.out.printf("%d/%d...%n", k, n);
            long t1 = nanoTime1();
            initializeCurrentContour(k, currentIndex);
            readCurrentContour();
            long t2 = nanoTime1();
            tReadingCurrent += t2 - t1;
            do {
                t2 = nanoTime1();
                boolean atLeastOneSuccessfullyJoined;
                do {
//                    long tQuick1 = nanoTime();
                    joiningCount++;
//                    if ((joiningCount & 0xFFF) == 0) System.out.printf("\r%d...    ", joiningCount);
                    if (interrupter != null && (joiningCount & 0xFF) == 0 && interrupter.getAsBoolean()) {
                        throw new InterruptionException("Contours joiner was interrupted while processing contour #"
                                + indexesOfClusterContours[0] + "/" + numberOfContours + " after "
                                + joiningCount + " joining actions");
                    }
                    atLeastOneSuccessfullyJoined = growByContoursFromClusterBasedOnGrowingRectangle();
                } while (atLeastOneSuccessfullyJoined && currentNumberOfPoints > 0);
                // - no sense to continue, if currentNumberOfPoints = 0: the contour was actually removed
                long t3 = nanoTime1();
                tAnalysing += t3 - t2;
                correctJoinedContoursStatistics();
                writeContour(current, currentNumberOfPoints, currentInternal);
                long t4 = nanoTime1();
                tWritingContours += t4 - t3;
            } while (loadCurrentContourFromDeferred());
            alreadyProcessed[currentIndex] = true;
        }
    }

    // Slower old alternative for growByContoursFromCluster(); not used now
    private boolean growByContoursFromClusterBasedOnGrowingRectangle() {
        rectanglesIntersectionFinder.findIntersectedWithCurrent(this);
        // - must be recalculated: currentMin/MaxX/Y were recalculated by joinContour()
        joiningOrder.sortIndexes(rectanglesIntersectionFinder);
        final int[] indexesOfIntersected = rectanglesIntersectionFinder.resultIndexes();
        final int m = rectanglesIntersectionFinder.numberOfResultIndexes();
        // - note: findIntersected() DOES NOT returns indexes with alreadyProcessed[k]=true
        boolean atLeastOneSuccessfullyJoined = false;
//                    long tQuick2 = nanoTime();
//                    tQuickCheckRectangles += tQuick2 - tQuick1;
        for (int i = 0; i < m; i++) {
            final int joinedIndex = indexesOfIntersected[i];
            if (joinedIndex > currentIndex) {
                if (joinContour(joinedIndex)) {
                    alreadyProcessed[joinedIndex] = true;
                    atLeastOneSuccessfullyJoined = true;
                    break;
                }
            }
        }
        correctQuickChecksStatistics(m);
        return atLeastOneSuccessfullyJoined;
    }
    */

    private boolean joinContour(int joinedIndex) {
        long t1 = nanoTime2();
        initializeNewJoinedAndReallocatePositionsMatrices(joinedIndex);
        readNewJoined();
        long t2 = nanoTime2();
        tJoiningReadingNewJoined += t2 - t1;

        boolean canBeJoined = findPositionsOfCurrentContour();
        long t3 = nanoTime2();
        sNumberOfScanningCurrent++;
        if (!canBeJoined) {
            tJoiningScanning1Fail += t3 - t2;
            return false;
        }
        tJoiningScanning1Success += t3 - t2;
        sNumberOfSuccessfulScanningCurrent++;

        canBeJoined = findPositionsOfJoinedContourAndCheckCodirectionalSegmentsWithCurrentContour();
        long t4 = nanoTime2();
        sNumberOfScanningJoined++;
        if (!canBeJoined) {
            tJoiningScanning2Fail += t4 - t3;
            return false;
        }
        tJoiningScanning2Success += t4 - t3;
        sNumberOfSuccessfulScanningJoined++;

        clearUsage();
        addInformationAboutJoined();
        int iteration = 0;
        if (joinCurrentAndJoined(iteration++)) {
            exchangeMainAndAdditionalJoinResult();
            // - exchanging pointer is faster than alternate solution - saving in deferred results;
            // so, we prefer to use deferred results only if there are actual newly created "holes"
            while (joinCurrentAndJoined(iteration++)) {
                saveJoinResultContourInDeferred();
            }
            exchangeMainAndAdditionalJoinResult();
        }
        exchangeCurrentAndJoinResult();
        long t5 = nanoTime2();
        tJoiningSwitching += t5 - t4;

        addPossibleNeighbours(joinedIndex);
        addCompressedContourToVisitedGrid(reverseIndexesOfContoursInCluster[joinedIndex]);
        long t6 = nanoTime2();
        tJoiningAddingToGridAndNeighbours += t6 - t5;
        return true;
    }

    private void exchangeCurrentAndJoinResult() {
        final int[] tempArray = current;
        current = joinResult;
        joinResult = tempArray;
        final int tempInt = currentNumberOfPoints;
        currentNumberOfPoints = joinResultNumberOfPoints;
        currentLength = 2 * currentNumberOfPoints;
        joinResultNumberOfPoints = tempInt;
        final boolean tempBoolean = currentInternal;
        currentInternal = joinResultInternal;
        joinResultInternal = tempBoolean;
    }

    private void exchangeMainAndAdditionalJoinResult() {
        final int[] tempArray = joinAdditionalResult;
        joinAdditionalResult = joinResult;
        joinResult = tempArray;
        final int tempInt = joinAdditionalResultNumberOfPoints;
        joinAdditionalResultNumberOfPoints = joinResultNumberOfPoints;
        joinResultNumberOfPoints = tempInt;
        final boolean tempBoolean = joinAdditionalResultInternal;
        joinAdditionalResultInternal = joinResultInternal;
        joinResultInternal = tempBoolean;
    }

    private int numberOfDeferredContours() {
        return deferredContours.numberOfContours() - deferredContoursStart;
    }

    private int deferredContourOffset(int indexOfDeferred) {
        return deferredContours.getContourOffset(deferredContoursStart + indexOfDeferred);
    }

    private int deferredContourLength(int indexOfDeferred) {
        return deferredContours.getContourLength(deferredContoursStart + indexOfDeferred);
    }

    private boolean loadCurrentContourFromDeferred() {
        final int m = deferredContours.numberOfContours();
        assert m >= deferredContoursStart;
        assert deferredContoursStart < REPACKING_DEFERRED_QUEUE_STEP;
        if (m == deferredContoursStart) {
            return false;
        }
        // Remove the first contour: so, deferred contours works like a queue, not a stack.
        // It is little slower, but more logical, because exchangeMainAndAdditionalJoinResult()
        // method suppose that the 1st processed contour will be also returned as 1st.
        currentInternal = deferredContours.isInternalContour(deferredContoursStart);
        final ContourLength contourLength = new ContourLength();
        current = deferredContours.getContourPointsAndReallocateResult(current, contourLength, deferredContoursStart);
        // - no need to unpack these contours: they are already unpacked!
        currentNumberOfPoints = contourLength.getNumberOfPoints();
        currentLength = 2 * currentNumberOfPoints;
        deferredContoursStart++;
        if (deferredContoursStart >= REPACKING_DEFERRED_QUEUE_STEP) {
            deferredContours.removeContoursRange(0, deferredContoursStart);
            deferredContoursStart = 0;
        }
        return true;
    }

    private void saveJoinResultContourInDeferred() {
        if (joinResultNumberOfPoints <= 0) {
            return;
        }
        sTotalNumberOfDeferredContours++;
        currentHeader.clear();
        // label is not important on this stage
        currentHeader.setInternalContour(joinResultInternal);
        deferredContours.addContour(currentHeader, joinResult, 0, 2 * joinResultNumberOfPoints);
    }

    private void writeContour(int[] contour, int numberOfPoints, boolean internalContour) {
        if (numberOfPoints <= 0) {
            return;
        }
        currentHeader.clear();
        currentHeader.setObjectLabel(reindexedCurrentLabel);
        currentHeader.setInternalContour(internalContour);
        final int nonOptimizedLength = 2 * numberOfPoints;
        if (packResultContours) {
            final ContourLength contourLength = new ContourLength();
            workPackedPoints = Contours.packContourAndReallocateResultUnchecked(
                    workPackedPoints, contourLength, contour, 0, nonOptimizedLength);
            result.addContour(currentHeader, workPackedPoints, 0, contourLength.getArrayLength());
        } else {
            result.addContour(currentHeader, contour, 0, nonOptimizedLength);
        }
    }

    private void initializeCurrentContour(int indexInCluster, int currentIndex) {
        this.currentIndex = currentIndex;
        assert hasNeighboursToJoin(currentIndex);
        currentMinX = allMinX[currentIndex];
        currentMaxX = allMaxX[currentIndex];
        currentMinY = allMinY[currentIndex];
        currentMaxY = allMaxY[currentIndex];
        cleanupVisitedGrid();
        addCompressedContourToVisitedGrid(indexInCluster);
    }

    private void readCurrentContour() {
        currentLabel = contours.getObjectLabel(currentIndex);
        currentInternal = allInternal[currentIndex];
        final int indexInCluster = reverseIndexesOfContoursInCluster[currentIndex];
        final int contourOffset = clusterContoursOffsets[indexInCluster];
        currentLength = clusterContoursOffsets[indexInCluster + 1] - contourOffset;
        currentNumberOfPoints = currentLength >> 1;
        ensureCapacityForCurrent(currentLength);
        // NOTE: we must reallocate it when necessary, because "current"
        // can be exchanged with "joinResult" by exchangeCurrentAndJoinResult()
        System.arraycopy(clusterContours, contourOffset, current, 0, currentLength);
        currentIndexesOfJoinedContours.clear();
        currentIndexesOfJoinedContours.pushInt(currentIndex);
    }

    private void initializeNewJoinedAndReallocatePositionsMatrices(int joinedIndex) {
        this.joinedIndex = joinedIndex;
        joinedMinX = allMinX[joinedIndex];
        joinedMaxX = allMaxX[joinedIndex];
        joinedMinY = allMinY[joinedIndex];
        joinedMaxY = allMaxY[joinedIndex];
        intersectionMinX = Math.max(currentMinX, joinedMinX);
        final int intersectionMaxX = Math.min(currentMaxX, joinedMaxX);
        intersectionMinY = Math.max(currentMinY, joinedMinY);
        final int intersectionMaxY = Math.min(currentMaxY, joinedMaxY);
        if (intersectionMinX > intersectionMaxX || intersectionMinY > intersectionMaxY) {
            throw new AssertionError("Containing rectangles of current and joined contours does not "
                    + "intersect: " + currentMinX + ".." + currentMaxX + "x" + currentMinY + ".." + currentMaxY
                    + " and " + joinedMinX + ".." + joinedMaxX + "x" + joinedMinY + ".." + joinedMaxY
                    + "; it must be checked before this moment");
        }
        intersectionDiffX = intersectionMaxX - intersectionMinX;
        intersectionDiffY = intersectionMaxY - intersectionMinY;
        intersectionDimX = intersectionDiffX + 1;
        intersectionDimY = intersectionDiffY + 1;
        final long matrixSize = (long) intersectionDimX * (long) intersectionDimY;
        ensureCapacityForPositionsMatrices(matrixSize);
        assert intersectionDimX > 0 && intersectionDimY > 0;
        // - see checks in the constructor
        this.intersectionMatrixSize = (int) matrixSize;
    }

    private void readNewJoined() {
        if (DETAILED_DEBUG_LEVEL >= 1 && reindexedLabels[joinedIndex] != reindexedCurrentLabel) {
            throw new AssertionError("Attempt to join contours with different reindexed labels: "
                    + "for current contour reindex(" + currentLabel + ")=" + reindexedCurrentLabel
                    + ", but for joined contour reindex(" + joinedLabel() + ")=" + reindexedLabels[joinedIndex]);
        }
        joinedInternal = allInternal[joinedIndex];
        final int indexInCluster = reverseIndexesOfContoursInCluster[joinedIndex];
        joined = clusterContours;
        joinedOffset = clusterContoursOffsets[indexInCluster];
        joinedLength = clusterContoursOffsets[indexInCluster + 1] - joinedOffset;
        joinedNumberOfPoints = joinedLength >> 1;
    }

    private int joinedLabel() {
        return contours.getObjectLabel(joinedIndex);
    }

    private boolean joinCurrentAndJoined(int iteration) {
        joinResultNumberOfPoints = 0;
        // - initialize empty result
        final CurrentOrJoinedContour oneFromTwo = new CurrentOrJoinedContour(iteration);
        int p = oneFromTwo.initializeSwitchingAlgorithm();
        if (p == -1) {
            return false;
        }
        ensureCapacityForJoinResultContour((long) currentLength + (long) joinedLength);
        final int maxPossibleResultPosition = currentLength + joinedLength;
        final int startPosition = p;
        final CurrentOrJoinedSwitcher startSwitcher = oneFromTwo.switcher;
        final int[] twoStartPositions = {-1, -1};
        twoStartPositions[oneFromTwo.switcher.index] = startPosition;
        final int[] twoLastPositions = twoStartPositions.clone();
        boolean switchesOccured = false;
        int resultPosition = 0;
        do {
            checkInfiniteLoop(resultPosition, maxPossibleResultPosition);
            int x = oneFromTwo.x(p);
            int y = oneFromTwo.y(p);
            assert !oneFromTwo.used(p) :
                    "Already used point " + x + "," + y + " appeared; it is impossible while correct scanning";
            final int dx = x - intersectionMinX;
            final int dy = y - intersectionMinY;
            int distance = 0;
            if (dx < 0 || dx > intersectionDiffX) {
                distance = dx < 0 ? -dx : dx - intersectionDiffX;
                final int distanceY = dy < 0 ? -dy : dy - intersectionDiffY;
                if (distanceY > 0) {
                    distance += distanceY;
                }
            } else if (dy < 0 || dy > intersectionDiffY) {
                distance = dy < 0 ? -dy : dy - intersectionDiffY;
            }
            // - we need at least "distance" steps to appear inside intersectionMinX/Y..joinedMaxX/Y,
            // because every step has length 1 (horizontal or vertical)
            final boolean skipTrivialSteps = distance > 6;
            if (skipTrivialSteps) {
                // - try to make large step; no sense to optimize for very little distance
                distance--;
                // - we need to make last step in a usual manner, to be accurate with REMOVED_POINT
                distance <<= 1;
                final int limit = oneFromTwo.switcher == startSwitcher && startPosition > p ?
                        startPosition :
                        oneFromTwo.length;
                if (distance > limit - p) {
                    distance = limit - p;
                }
                oneFromTwo.copyTo(p, joinResult, resultPosition, distance);
                java.util.Arrays.fill(oneFromTwo.usage, p >> 1, (p + distance) >> 1, true);
                resultPosition += distance;
                p += distance;
                if (p == oneFromTwo.length) {
                    p = 0;
                }
                if (DETAILED_DEBUG_LEVEL >= 3) {
                    oneFromTwo.debugPrintSkipping(p, x, y, distance);
                }
            } else {
                joinResult[resultPosition++] = x;
                joinResult[resultPosition++] = y;
                oneFromTwo.use(p);
                p = oneFromTwo.cyclicNextEven(p);
            }
            //            return positionAtContour(positionAtThis, points, offset, length, switcher.other());
            int positionAtOther = oneFromTwo.other.positionAtContour(p, this);
            if (DETAILED_DEBUG_LEVEL >= 3) {
                oneFromTwo.debugPrintPoint(p);
            }
            final boolean nonTrivialSituation = positionAtOther != EMPTY_POSITION;
            if (nonTrivialSituation) {
                if (skipTrivialSteps) {
                    throw new AssertionError("After skipping trivial steps we must "
                            + "not find anything yet (because of distance-- above)");
                }
                assert (positionAtOther & 0x1) == 0;
                final int checkedAtThis = oneFromTwo.switcher.positionAtContour(positionAtOther, this);
                if (checkedAtThis != p) {
                    throw new AssertionError("Mutual positions of current and joined contours "
                            + "do not match: current position = " + p + ", mutual = " + checkedAtThis);
                }
                positionAtOther = oneFromTwo.cyclicNextEvenAtOther(positionAtOther);
                // - Because direction of the other segment is opposite, its END coincides with
                // the BEGINNING of this segment. Note that we can be sure that it is really opposite:
                // codirectional segments are disabled to join (see findPositionsOfJoinedContour()).
                x = oneFromTwo.x(p);
                y = oneFromTwo.y(p);
                if (oneFromTwo.otherX(positionAtOther) != x || oneFromTwo.otherY(positionAtOther) != y) {
                    throw new AssertionError("Different point at the current and joined contours");
                }
                final int positionAtThis = oneFromTwo.switcher.positionAtContour(positionAtOther, this);
                twoLastPositions[oneFromTwo.switcher.index] = p;
                // IMPORTANT! We must do this in BOTH following cases, to provide correct
                // last position != -1 in checkReturningBack
                if (positionAtThis != EMPTY_POSITION) {
                    // - Next position at other is also occupied by this contour:
                    // here we should not switch to it, but just must JUMP to the next position.
                    p = oneFromTwo.cyclicNextEven(positionAtThis);
                    // - Because other segment is opposite, its END coincides with the BEGINNING of this segment
                    if (oneFromTwo.x(p) != x || oneFromTwo.y(p) != y) {
                        throw new AssertionError("Invalid jump to #" + p + ": " + x + "," + y + " -> "
                                + oneFromTwo.x(p) + "," + oneFromTwo.y(p) + " at "
                                + oneFromTwo.switcher.name + " contour. " + contoursInfo());
                    }
                    if (DETAILED_DEBUG_LEVEL >= 2) {
                        oneFromTwo.debugPrintJump(p);
                    }
                } else {
                    oneFromTwo.switchToOther();
                    p = positionAtOther;
                    if (DETAILED_DEBUG_LEVEL >= 3) {
                        oneFromTwo.debugPrintSwitching(p);
                    }

                }
                switchesOccured = true;
                if (p != startPosition || oneFromTwo.switcher != startSwitcher) {
                    // Note: we skip this checking in trivial situation (for better performance).
                    // Here "if" is important: if we are going to finish the loop (returned back to start position),
                    // we should not check cyclicLess, because, of course, start position is LESS than the last.
                    oneFromTwo.checkReturningBackAfterSwitchOrJump(p, twoStartPositions, twoLastPositions);
                }
            }
        } while (p != startPosition || oneFromTwo.switcher != startSwitcher);
        // - note: the check above is NOT a duplicate of the previous same check
        if (!switchesOccured) {
            throw new AssertionError("No switches, though we must have counter-directional segments!");
        }
        joinResultNumberOfPoints = resultPosition >> 1;
        return true;
    }

    private void clearUsage() {
        ensureCapacityForUsage();
        java.util.Arrays.fill(currentUsage, 0, currentNumberOfPoints, false);
        java.util.Arrays.fill(joinedUsage, 0, joinedNumberOfPoints, false);
    }

    private void checkInfiniteLoop(int resultPosition, int maxPossibleResultPosition) {
        if (resultPosition >= maxPossibleResultPosition) {
            throw new AssertionError("Infinite loop whilee joining to the contour #"
                    + currentIndex + " (0-based numbering, label " + currentLabel
                    + ") a new contour #" + joinedIndex + " (label " + joinedLabel()
                    + "): " + contoursInfo());
        }
    }

    private void addInformationAboutJoined() {
        currentMinX = Math.min(currentMinX, joinedMinX);
        currentMaxX = Math.max(currentMaxX, joinedMaxX);
        currentMinY = Math.min(currentMinY, joinedMinY);
        currentMaxY = Math.max(currentMaxY, joinedMaxY);
        currentIndexesOfJoinedContours.pushInt(joinedIndex);
    }

    private boolean findPositionsOfCurrentContour() {
        JArrays.fillIntArray(currentPositionsForXPlusSegments, 0, intersectionMatrixSize, EMPTY_POSITION);
        JArrays.fillIntArray(currentPositionsForYPlusSegments, 0, intersectionMatrixSize, EMPTY_POSITION);
        final int n = currentLength;
        assert n > 0;
        final int intersectionMinX = this.intersectionMinX;
        final int intersectionMinY = this.intersectionMinY;
        final int intersectionDiffX = this.intersectionDiffX;
        final int intersectionDiffY = this.intersectionDiffY;
        final int intersectionDimX = this.intersectionDimX;
        // - JVM works better with local variables, not fields of an object
        boolean hasPointsInsideJoinedRectangle = false;
        for (int i = 0; i < n; ) {
            final int x = current[i] - intersectionMinX;
            final int y = current[i + 1] - intersectionMinY;
            if (x < 0 || x > intersectionDiffX) {
                int distance = x < 0 ? -x : x - intersectionDiffX;
                final int distanceY = y < 0 ? -y : y - intersectionDiffY;
                if (distanceY > 0) {
                    distance += distanceY;
                }
                i += distance << 1;
                // - we need at least such number of steps to appear inside intersectionMinX/Y..joinedMaxX/Y,
                // because every step has length 1 (horizontal or vertical)
                continue;
            }
            if (y < 0 || y > intersectionDiffY) {
                int distance = y < 0 ? -y : y - intersectionDiffY;
                i += distance << 1;
                // - we need at least such number of steps to appear inside intersectionMinX/Y..joinedMaxX/Y,
                // because every step has length 1 (horizontal or vertical)
                continue;
            }

            final int lastI = i == 0 ? n - 2 : i - 2;
            final int lastX = current[lastI] - intersectionMinX;
            final int lastY = current[lastI + 1] - intersectionMinY;
            i += 2;
            if (lastX < 0 || lastY < 0 || lastX > intersectionDiffX || lastY > intersectionDiffY) {
                continue;
            }

            hasPointsInsideJoinedRectangle = true;
            final int dx = x - lastX;
            final int dy = y - lastY;
            assert dy == 0 ? dx == -1 || dx == 1 : dx == 0 && (dy == -1 || dy == 1) :
                    "invalid segment in currently joined contour: " + lastX + "," + lastY + " -> " + x + "," + y
                            + "; it was NOT CHECKED yet??";
            final int disp = (dy < 0 ? y : lastY) * intersectionDimX + (dx < 0 ? x : lastX);
            final int[] currentPositions = dy == 0 ?
                    currentPositionsForXPlusSegments :
                    currentPositionsForYPlusSegments;
            if (currentPositions[disp] != EMPTY_POSITION) {
                // - its possible, because we didn't check ALL segments of the joined contour
                throw new IllegalArgumentException("One of the contours [#"
                        + Arrays.toString(currentIndexesOfJoinedContours, ", #", 500)
                        + "] intersects itself, i.e. twice contains the segment " +
                        +(intersectionMinX + lastX) + "," + (intersectionMinY + lastY) + " - "
                        + (intersectionMinX + x) + "," + (intersectionMinY + y)
                        + "; such contours cannot be joined");
            }
            currentPositions[disp] = lastI;
        }
        if (DETAILED_DEBUG_LEVEL >= 2) {
            System.out.printf("Testing ability to join %d (%d, %d points) to %d (%d, %d points): "
                            + "current %s joined rectangle (%s)%n",
                    joinedIndex, joinedLabel(), joinedNumberOfPoints,
                    currentIndex, currentLabel, currentNumberOfPoints,
                    hasPointsInsideJoinedRectangle ? "INTERSECTS" : "DOESN'T intersect",
                    Arrays.toString(currentIndexesOfJoinedContours, ",", 200));
        }
        return hasPointsInsideJoinedRectangle;
    }

    private boolean findPositionsOfJoinedContourAndCheckCodirectionalSegmentsWithCurrentContour() {
        JArrays.fillIntArray(joinedPositionsForXPlusSegments, 0, intersectionMatrixSize, EMPTY_POSITION);
        JArrays.fillIntArray(joinedPositionsForYPlusSegments, 0, intersectionMatrixSize, EMPTY_POSITION);
        boolean hasCommonPointsWithCurrentContour = false;
        final int n = joinedLength;
        assert n > 0;
        final int intersectionMinX = this.intersectionMinX;
        final int intersectionMinY = this.intersectionMinY;
        final int intersectionDiffX = this.intersectionDiffX;
        final int intersectionDiffY = this.intersectionDiffY;
        final int intersectionDimX = this.intersectionDimX;
        // - JVM works better with local variables, not fields of an object
        for (int i = 0; i < n; ) {
            final int x = joined[joinedOffset + i] - intersectionMinX;
            final int y = joined[joinedOffset + i + 1] - intersectionMinY;
            if (x < 0 || x > intersectionDiffX) {
                int distance = x < 0 ? -x : x - intersectionDiffX;
                final int distanceY = y < 0 ? -y : y - intersectionDiffY;
                if (distanceY > 0) {
                    distance += distanceY;
                }
                i += distance << 1;
                // - we need at least such number of steps to appear inside intersectionMinX/Y..joinedMaxX/Y,
                // because every step has length 1 (horizontal or vertical)
                continue;
            }
            if (y < 0 || y > intersectionDiffY) {
                int distance = y < 0 ? -y : y - intersectionDiffY;
                i += distance << 1;
                // - we need at least such number of steps to appear inside intersectionMinX/Y..joinedMaxX/Y,
                // because every step has length 1 (horizontal or vertical)
                continue;
            }

            final int lastI = i == 0 ? n - 2 : i - 2;
            final int lastX = joined[joinedOffset + lastI] - intersectionMinX;
            final int lastY = joined[joinedOffset + lastI + 1] - intersectionMinY;
            i += 2;
            if (lastX < 0 || lastY < 0 || lastX > intersectionDiffX || lastY > intersectionDiffY) {
                continue;
            }

            final int dx = x - lastX;
            final int dy = y - lastY;
            assert dy == 0 ? dx == -1 || dx == 1 : dx == 0 && (dy == -1 || dy == 1) :
                    "invalid segment in unpacked contour: " + lastX + "," + lastY + " -> " + x + "," + y;
            final int disp = (dy < 0 ? y : lastY) * intersectionDimX + (dx < 0 ? x : lastX);
            final int[] joinedPositions = dy == 0 ?
                    joinedPositionsForXPlusSegments :
                    joinedPositionsForYPlusSegments;
            if (joinedPositions[disp] != EMPTY_POSITION) {
                throw new IllegalArgumentException("The contour #" + joinedIndex
                        + " intersects itself, i.e. twice contains the segment " +
                        +(intersectionMinX + lastX) + "," + (intersectionMinY + lastY) + " - "
                        + (intersectionMinX + x) + "," + (intersectionMinY + y)
                        + "; such contours cannot be joined");
            }
            joinedPositions[disp] = lastI;
            int q = dy == 0 ? currentPositionsForXPlusSegments[disp] : currentPositionsForYPlusSegments[disp];
            if (q != EMPTY_POSITION) {
                final int currentX = current[q] - intersectionMinX;
                final int currentY = current[q + 1] - intersectionMinY;
                if (currentX == lastX && currentY == lastY) {
                    // If we have CODIRECTIONAL segments, we must reject this contour at all:
                    // in other case we can lead to current contour, containing some segments TWICE.
                    if (DETAILED_DEBUG_LEVEL >= 2) {
                        System.out.printf("  Joining failed%n");
                    }
                    return false;
                }
                assert currentX == x && currentY == y :
                        "segments in current and joined contours do not match: " + lastX + "," + lastY + " -> "
                                + x + "," + y + ", but " + currentX + "," + currentY;
                final int nextQ = cyclicNextEven(q, currentLength);
                final int currentNextX = current[nextQ] - intersectionMinX;
                final int currentNextY = current[nextQ + 1] - intersectionMinY;
                assert currentNextX == lastX && currentNextY == lastY :
                        "segments in current and joined contours do not match: " + lastX + "," + lastY + " -> "
                                + x + "," + y + ", but " + currentNextX + "," + currentNextY;
                hasCommonPointsWithCurrentContour = true;
            }
        }
        if (DETAILED_DEBUG_LEVEL >= 2) {
            System.out.printf("Joining %d (%d, %d points) to %d (%d, %d points): joined %s current rectangle (%s)%n",
                    joinedIndex, joinedLabel(), joinedNumberOfPoints,
                    currentIndex, currentLabel, currentNumberOfPoints,
                    hasCommonPointsWithCurrentContour ? "INTERSECTS" : "DOESN'T intersect",
                    Arrays.toString(currentIndexesOfJoinedContours, ",", 200));
        }
        return hasCommonPointsWithCurrentContour;
    }

    private int positionOfMinX(int[] points, int offset, int numberOfPoints) {
        long t1 = nanoTime3();
        assert numberOfPoints >= 1;
        int offsetOfMinX = offset;
        int minX = points[offset];
        for (int i = offset + 2, to = offset + 2 * numberOfPoints; i < to; i += 2) {
            final int x = points[i];
            if (x < minX) {
                minX = x;
                offsetOfMinX = i;
            }
        }
        long t2 = nanoTime3();
        tOffsetOfMinX += t2 - t1;
        return offsetOfMinX - offset;
    }

    // This method finds position q at the OTHER contour, so that its segment q->q+1
    // is equal to positionAtCurrent->positionAtCurrent+1 segment in the current "contour"
    private int positionAtJoined(int positionAtCurrent) {
        final int x = current[positionAtCurrent] - intersectionMinX;
        final int y = current[positionAtCurrent + 1] - intersectionMinY;
        if (x < 0 || y < 0 || x >= intersectionDimX || y >= intersectionDimY) {
            return EMPTY_POSITION;
        }
        final int nextPosition = cyclicNextEven(positionAtCurrent, currentLength);
        final int nextX = current[nextPosition] - intersectionMinX;
        final int nextY = current[nextPosition + 1] - intersectionMinY;
        if (nextX < 0 || nextY < 0 || nextX >= intersectionDimX || nextY >= intersectionDimY) {
            return EMPTY_POSITION;
        }
        final int dx = nextX - x;
        final int dy = nextY - y;
        assert dy == 0 ? dx == -1 || dx == 1 : dx == 0 && (dy == -1 || dy == 1) :
                "invalid unpacked contour: dx = " + dx + ", dy = " + dy
                        + " after the point #" + positionAtCurrent / 2 + "/" + currentLength / 2;
        final int disp = (dy < 0 ? nextY : y) * intersectionDimX + (dx < 0 ? nextX : x);
        final int q;
        q = dy == 0 ? joinedPositionsForXPlusSegments[disp] : joinedPositionsForYPlusSegments[disp];
        assert q == EMPTY_POSITION || (q & 1) == 0 :
                "odd value " + q + " is impossible in positions matrix";
        return q;
    }

    private int positionAtCurrent(int positionAtJoined) {
        final int x = joined[joinedOffset + positionAtJoined] - intersectionMinX;
        final int y = joined[joinedOffset + positionAtJoined + 1] - intersectionMinY;
        if (x < 0 || y < 0 || x >= intersectionDimX || y >= intersectionDimY) {
            return EMPTY_POSITION;
        }
        final int nextPosition = cyclicNextEven(positionAtJoined, joinedLength);
        final int nextX = joined[joinedOffset + nextPosition] - intersectionMinX;
        final int nextY = joined[joinedOffset + nextPosition + 1] - intersectionMinY;
        if (nextX < 0 || nextY < 0 || nextX >= intersectionDimX || nextY >= intersectionDimY) {
            return EMPTY_POSITION;
        }
        final int dx = nextX - x;
        final int dy = nextY - y;
        assert dy == 0 ? dx == -1 || dx == 1 : dx == 0 && (dy == -1 || dy == 1) :
                "invalid unpacked contour: dx = " + dx + ", dy = " + dy
                        + " after the point #" + positionAtJoined / 2 + "/" + joinedLength / 2;
        final int disp = (dy < 0 ? nextY : y) * intersectionDimX + (dx < 0 ? nextX : x);
        final int q;
        q = dy == 0 ? currentPositionsForXPlusSegments[disp] : currentPositionsForYPlusSegments[disp];
        assert q == EMPTY_POSITION || (q & 1) == 0 :
                "odd value " + q + " is impossible in positions matrix";
        return q;
    }

    private long nanoTime1() {
        return measureTimingLevel >= 1 ? System.nanoTime() : 0;
    }

    private long nanoTime2() {
        return measureTimingLevel >= 2 ? System.nanoTime() : 0;
    }

    private long nanoTime3() {
        return measureTimingLevel >= 3 ? System.nanoTime() : 0;
    }

    private void correctQuickChecksStatistics(int checkedContoursCount) {
        sMinCheckedContoursCount = Math.min(sMinCheckedContoursCount, checkedContoursCount);
        sMaxCheckedContoursCount = Math.max(sMaxCheckedContoursCount, checkedContoursCount);
        sSumCheckedContoursCount += checkedContoursCount;
        sNumberOfCheckedContoursLoops++;
    }

    private void correctJoinedContoursStatistics() {
        int m = (int) currentIndexesOfJoinedContours.length();
        sMinJoinedContoursCount = Math.min(sMinJoinedContoursCount, m);
        sMaxJoinedContoursCount = Math.max(sMaxJoinedContoursCount, m);
        sSumJoinedContoursCount += m;
        sNumberOfJoinedContours++;
        m = numberOfDeferredContours();
        sMinDeferredContoursCount = Math.min(sMinDeferredContoursCount, m);
        sMaxDeferredContoursCount = Math.max(sMaxDeferredContoursCount, m);
        sSumDeferredContoursCount += m;
        sNumberOfDeferredContoursChecks++;
    }

    private String contoursInfo() {
        return "(The current contour is now a union of the following: [#" + Arrays.toString(
                currentIndexesOfJoinedContours, ", #", 500)
                + "]; its points: " + JArrays.toString(
                JArrays.copyOfRange(current, 0, currentLength),
                ",", 2500) + "; points of joined: "
                + JArrays.toString(
                JArrays.copyOfRange(joined, 0, joinedLength),
                ",", 2500) + ".)";
    }

    private static int reindex(int objectLabel, int[] joinedLabelsMap, int defaultJoinedLabel) {
        if (objectLabel < 0) {
            throw new IllegalArgumentException("Objects in contours must be represented by zero "
                    + "or negative integers, but we have " + objectLabel);
        }
        if (joinedLabelsMap == null) {
            return defaultJoinedLabel;
        }
        if (objectLabel >= joinedLabelsMap.length) {
            // - correct situation: internal objects, not intersecting frame boundaries,
            // are not added into this disjoint set
            return objectLabel;
        } else {
            final int result = joinedLabelsMap[objectLabel];
            if (result < 0) {
                throw new IllegalArgumentException("Joined labels map must contain only non-negative elements, "
                        + "but it contains " + result);
            }
            return result;
        }
    }

    private void ensureCapacityForUsage() {
        if (currentNumberOfPoints > currentUsage.length) {
            currentUsage = new boolean[Math.max(16, Math.max(currentNumberOfPoints,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * currentUsage.length))))];
        }
        if (joinedNumberOfPoints > joinedUsage.length) {
            joinedUsage = new boolean[Math.max(16, Math.max(joinedNumberOfPoints,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * joinedUsage.length))))];
        }
    }

    private void ensureCapacityForUnpackedClusterAndReallocate(long requiredLength) {
        if (requiredLength > clusterContours.length) {
            if (requiredLength > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large contour array: > Integer.MAX_VALUE elements");
            }
            clusterContours = java.util.Arrays.copyOf(clusterContours, Math.max(16, Math.max((int) requiredLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * clusterContours.length)))));
        }
    }

    private void ensureCapacityForCompressedClusterAndReallocate(long requiredLength) {
        assert compressedContoursBitMaps8x8.length == compressedContoursPositions.length;
        if (requiredLength > compressedContoursPositions.length) {
            if (requiredLength > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large compressed positions array: > Integer.MAX_VALUE elements");
            }
            final int newLength = Math.max(16, Math.max((int) requiredLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * compressedContoursPositions.length))));
            compressedContoursPositions = java.util.Arrays.copyOf(compressedContoursPositions, newLength);
            compressedContoursBitMaps8x8 = java.util.Arrays.copyOf(compressedContoursBitMaps8x8, newLength);
        }
    }

    private void ensureCapacityForCurrent(int requiredLength) {
        if (requiredLength > current.length) {
            current = new int[Math.max(16, Math.max(requiredLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * current.length))))];
        }
    }

    private void ensureCapacityForJoinResultContour(long requiredLength) {
        if (requiredLength > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large possible result of joining contours: 2 * "
                    + requiredLength / 2 + " points >= 2^31");
        }
        if (requiredLength > joinResult.length) {
            joinResult = new int[Math.max(16, Math.max((int) requiredLength,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * joinResult.length))))];
        }
    }

    private void ensureCapacityForPositionsMatrices(long requiredMatrixSize) {
        if (requiredMatrixSize > currentPositionsForXPlusSegments.length) {
            // - usually should not occur: we allocated enough memory in the constructor
            if (requiredMatrixSize > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("Too large intersection area: "
                        + intersectionDimX + " x " + intersectionDimY + " >= 2^31 pixels, "
                        + "such contours cannot be joined (it occurred while attempt to join contour #"
                        + joinedIndex + " with containing rectangle "
                        + joinedMinX + ".." + joinedMaxX + " x " + joinedMinY + ".." + joinedMaxY
                        + " to current contour, growing from #" + currentIndex
                        + " and having now containing rectangle "
                        + currentMinX + ".." + currentMaxX + " x " + currentMinY + ".." + currentMaxY + ")");
            }
            final int newMatrixSize = Math.max(16, Math.max((int) requiredMatrixSize,
                    (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * currentPositionsForXPlusSegments.length))));
//            System.err.println("Enlarged to " + newMatrixSize);
            currentPositionsForXPlusSegments = new int[newMatrixSize];
            currentPositionsForYPlusSegments = new int[newMatrixSize];
            joinedPositionsForXPlusSegments = new int[newMatrixSize];
            joinedPositionsForYPlusSegments = new int[newMatrixSize];
        }
    }

    private static int cyclicNextEven(int p, int length) {
        p += 2;
        return p == length ? 0 : p;
    }

    private static boolean cyclicLess(int start, int length, int a, int b) {
        assert 0 <= a && a < length : "must be 0 <= " + a + " < " + length;
        assert 0 <= b && b < length : "must be 0 <= " + b + " < " + length;
        assert 0 <= start && start < length : "must be 0 <= " + start + " < " + length;
        if (a < start) {
            return b < start && a < b;
        } else {
            return b < start || a < b;
        }
    }

    private class CurrentOrJoinedContour {
        private final int iteration;

        CurrentOrJoinedSwitcher switcher = null;
        CurrentOrJoinedSwitcher other = null;
        int[] points = null;
        int offset = 0;
        int[] otherPoints = null;
        int otherOffset = 0;
        boolean[] usage = null;
        int length;
        int otherLength;

        private CurrentOrJoinedContour(int iteration) {
            assert iteration >= 0;
            this.iteration = iteration;
        }

        void switchTo(CurrentOrJoinedSwitcher switcher) {
            this.switcher = switcher;
            if (switcher.isJoined()) {
                this.other = CurrentOrJoinedSwitcher.CURRENT;
                this.points = joined;
                this.offset = joinedOffset;
                this.otherPoints = current;
                this.otherOffset = 0;
                this.length = joinedLength;
                this.otherLength = currentLength;
                this.usage = joinedUsage;
            } else {
                this.other = CurrentOrJoinedSwitcher.JOINED;
                this.points = current;
                this.offset = 0;
                this.otherPoints = joined;
                this.otherOffset = joinedOffset;
                this.length = currentLength;
                this.otherLength = joinedLength;
                this.usage = currentUsage;
            }
        }

        int x(int p) {
            return points[offset + p];
        }

        int y(int p) {
            return points[offset + p + 1];
        }

        int otherX(int p) {
            return otherPoints[otherOffset + p];
        }

        int otherY(int p) {
            return otherPoints[otherOffset + p + 1];
        }

        boolean used(int p) {
            return usage[p >> 1];
        }

        void use(int p) {
            usage[p >> 1] = true;
        }

        void copyTo(int p, int[] result, int resultOffset, int length) {
            System.arraycopy(points, offset + p, result, resultOffset, length);
        }

        void switchToOther() {
            switchTo(other);
        }

        int initializeSwitchingAlgorithm() {
            if ((long) currentNumberOfPoints + (long) joinedNumberOfPoints > Contours.MAX_CONTOUR_NUMBER_OF_POINTS) {
                throw new TooLargeArrayException("Too large contours: summary number of points in the joining result"
                        + " will be > " + Contours.MAX_CONTOUR_NUMBER_OF_POINTS);
            }
            int p;
            if (iteration == 0) {
                switchTo(CurrentOrJoinedSwitcher.CURRENT);
                p = positionOfMinX(current, 0, currentNumberOfPoints);
                final int joinedMinXPosition = positionOfMinX(joined, joinedOffset, joinedNumberOfPoints);
                if (otherX(joinedMinXPosition) < x(p)) {
                    p = joinedMinXPosition;
                    switchToOther();
                }
                // - We start from the most left point of 2 contours: we can be sure that it is at
                // their external boundary (the following loop skips the possible pores between 2 contours).
                // (Here "external" means "the most outside", regardless whether these contours are external or internal.)
                p = findSegmentBelongingToOnlyThisOneFromTwo(p);
                joinResultInternal = switcher.isJoined() ? joinedInternal : currentInternal;
                if (p == -1) {
                    assert switcher.isCurrent() : "Joined contour cannot be a subset of current, "
                            + "because its minX was < minX of the current contour";
                    // Joined contour contains all points of the current one and (maybe) also some
                    // additional branches; at the same time, it has no codirectional segments with this one
//                if (JOIN_INTERNAL/* && joinedInternal == currentInternal*/) {
                    switchToOther();
                    p = findSegmentBelongingToOnlyThisOneFromTwo(0);
                }
            } else {
                switchTo(CurrentOrJoinedSwitcher.JOINED);
                // - Start from the joined contour: it should be shorter
                p = findUnusedSegmentBelongingToOnlyThisOneFromTwo();
                if (p == -1) {
                    switchToOther();
                    // - Then investigate the current contour. It is necessary for a case of possible loops
                    // in the current contour:
                    //        CCCCjjjjj
                    //        CCCCjjjjj
                    //        C...CCC
                    //        C...CCC
                    //        C...CCC
                    //        CCCCCCC
                    p = findUnusedSegmentBelongingToOnlyThisOneFromTwo();
                }
                joinResultInternal = !joinedInternal;
                // - it is hole between current/joined contours: its direction is reverse
            }
            if (DETAILED_DEBUG_LEVEL >= 2) {
                debugPrintStarting(p);
            }
            return p;
        }

        private int findSegmentBelongingToOnlyThisOneFromTwo(int startPosition) {
            long t1 = nanoTime3();
            int p = startPosition;
            int result = -1;
            for (int count = 0, n = length; count < n; count += 2) {
                if (other.positionAtContour(p, ContourJoiner.this) == EMPTY_POSITION) {
                    result = p;
                    break;
                }
                p += 2;
                if (p == n) {
                    p = 0;
                }
            }
            long t2 = nanoTime3();
            tFindFreeSegment += t2 - t1;
            return result;
        }

        private int findUnusedSegmentBelongingToOnlyThisOneFromTwo() {
            long t1 = nanoTime3();
            int p = 0;
            int result = -1;
            for (int count = 0, n = length; count < n; count += 2) {
                final boolean used = usage[p >> 1];
                if (!used && other.positionAtContour(p, ContourJoiner.this) == EMPTY_POSITION) {
                    result = p;
                    break;
                }
                p += 2;
                if (p == n) {
                    p = 0;
                }
            }
            long t2 = nanoTime3();
            tFindFreeUnusedSegment += t2 - t1;
            return result;
        }

        private int cyclicNextEven(int p) {
            p += 2;
            return p == length ? 0 : p;
        }

        private int cyclicNextEvenAtOther(int p) {
            p += 2;
            return p == otherLength ? 0 : p;
        }

        private void checkReturningBackAfterSwitchOrJump(
                int p,
                int[] twoStartPositions,
                int[] twoLastPositions) {
            final int oneFromTwoStartPosition = twoStartPositions[switcher.index];
            if (oneFromTwoStartPosition == -1) {
                twoStartPositions[switcher.index] = p;
            } else {
                final int lastPosition = twoLastPositions[switcher.index];
                if (lastPosition == -1) {
                    throw new AssertionError("Last position was not initialize yet!");
                }
                if (cyclicLess(oneFromTwoStartPosition, length, p, lastPosition)) {
                    throw new AssertionError("Returning back: cannot join "
                            + "to the current contour #" + currentIndex + " (0-based numbering, label #"
                            + currentLabel + ", " + currentNumberOfPoints
                            + " segments) a new joined contour #" + joinedIndex
                            + " (label #" + joinedLabel() + ", " + joinedNumberOfPoints
                            + " segments), because " + other.name + " contour returned back to "
                            + "an earlier point #" + p / 2 + "/" + length / 2
                            + " [x=" + points[p] + ",y=" + points[p + 1] + "] at "
                            + switcher.name + " contour (it is before the last point at this contour #"
                            + lastPosition / 2 + "/" + length / 2
                            + " [x=" + points[lastPosition] + ",y=" + points[lastPosition + 1]
                            + "], and we started scanning it from point #"
                            + oneFromTwoStartPosition / 2 + "/" + length / 2
                            + " [x=" + points[oneFromTwoStartPosition] + ",y="
                            + points[oneFromTwoStartPosition + 1]
                            + "]). It is possible if some of contours are self-intersecting; "
                            + "such contours cannot be joined. "
                            + contoursInfo());
                }
            }

        }

        private void debugPrintStarting(int p) {
            System.out.printf("[%d, %d deferred] Starting joining %d (%d, internal=%s, %d points) "
                            + "to %d (%d, internal=%s, %d points): #%d in %s%n",
                    iteration, deferredContours.numberOfContours(),
                    joinedIndex, joinedLabel(), joinedInternal, joinedNumberOfPoints,
                    currentIndex, currentLabel, currentInternal, currentNumberOfPoints,
                    p / 2, switcher);
        }

        private void debugPrintSkipping(int p, int previousX, int previousY, int distance) {
            System.out.printf("[%d] Joining %d (%d, %d points) to %d (%d, %d points): #%d in %s, "
                            + "skipping %d: %d,%d -> %d,%d (%s)%n",
                    iteration, joinedIndex, joinedLabel(), joinedNumberOfPoints,
                    currentIndex, currentLabel, currentNumberOfPoints,
                    p / 2, switcher, distance, previousX, previousY,
                    points[p % length],
                    points[(p + 1) % length],
                    Arrays.toString(currentIndexesOfJoinedContours, ",", 200));

        }

        private void debugPrintPoint(int p) {
            System.out.printf("[%d] Joining %d (%d, %d points) to %d (%d, %d points): "
                            + "#%d in %s, %d,%d -> %d,%d (%s)%n",
                    iteration, joinedIndex, joinedLabel(), joinedNumberOfPoints,
                    currentIndex, currentLabel, currentNumberOfPoints,
                    p / 2, switcher, points[p], points[p + 1],
                    points[(p + 2) % length],
                    points[(p + 3) % length],
                    Arrays.toString(currentIndexesOfJoinedContours, ",", 200));
        }

        private void debugPrintJump(int p) {
            System.out.printf("  JUMPING in %s: point #%d%n", switcher, p / 2);
        }

        private void debugPrintSwitching(int p) {
            System.out.printf("  SWITCHING to %s: point #%d: %d,%d%n", switcher, p / 2, points[p], points[p + 1]);
        }
    }

    private enum CurrentOrJoinedSwitcher {
        CURRENT(0, "current") {
            @Override
            int positionAtContour(int p, ContourJoiner joiner) {
                return joiner.positionAtCurrent(p);
            }

            @Override
            boolean isCurrent() {
                return true;
            }

            @Override
            boolean isJoined() {
                return false;
            }
        },
        JOINED(1, "joined") {
            @Override
            int positionAtContour(int p, ContourJoiner joiner) {
                return joiner.positionAtJoined(p);
            }

            @Override
            boolean isCurrent() {
                return false;
            }

            @Override
            boolean isJoined() {
                return true;
            }
        };

        final int index;
        final String name;

        CurrentOrJoinedSwitcher(int index, String name) {
            this.index = index;
            this.name = name;
        }

        abstract boolean isCurrent();

        abstract boolean isJoined();

        abstract int positionAtContour(int p, ContourJoiner joiner);
    }
}
