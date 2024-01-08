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

import net.algart.executors.modules.core.common.Multithreading;
import net.algart.additions.math.IRectangleFinder;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class ContourNestingAnalyser {
    public static class NestingInformation {
        private int nestingLevel = -1;
        private int nestingParent = -1;

        public int getNestingLevel() {
            return nestingLevel;
        }

        public int getNestingParent() {
            return nestingParent;
        }
    }

    private final Contours contours;
    private final boolean allContoursSurelyUnpacked;

    private boolean nestingLevelNecessary = true;

    private final int[] allMinX;
    private final int[] allMaxX;
    private final int[] allMinY;
    private final int[] allMaxY;
    private final long[] allOrientedAreas;

    private final IRectangleFinder rectanglesFinder;

    private final int[] contourNestingLevels;
    private final int[] contourNestingParents;

    private int totalNumberOfNestingContours = 0;
    private int totalNumberOfCheckedContours = 0;
    private long totalSummaryContoursLength = 0;

    private ContourNestingAnalyser(Contours contours, boolean allContoursSurelyUnpacked, long[] areasToCompare) {
        this.contours = Objects.requireNonNull(contours, "Null contours");
        this.allContoursSurelyUnpacked = allContoursSurelyUnpacked;
        final int numberOfContours = contours.numberOfContours();
        if (areasToCompare != null && areasToCompare.length < numberOfContours) {
            throw new IllegalArgumentException("Too short areasToCompare array: its length " + areasToCompare.length
                    + " < number of contours = " + numberOfContours);
        }
//        long t1 = System.nanoTime();
        this.allMinX = new int[numberOfContours];
        this.allMaxX = new int[numberOfContours];
        this.allMinY = new int[numberOfContours];
        this.allMaxY = new int[numberOfContours];
        this.allOrientedAreas = areasToCompare != null ? areasToCompare : new long[numberOfContours];
        this.contourNestingLevels = new int[numberOfContours];
        this.contourNestingParents = new int[numberOfContours];
//        long t2 = System.nanoTime();
        IntStream.range(0, (numberOfContours + 15) >>> 4).parallel().forEach(areasToCompare == null ?
                block -> {
                    for (int i = block << 4, to = (int) Math.min((long) i + 16, numberOfContours); i < to; i++) {
                        final int headerOffset = contours.getSerializedHeaderOffset(i);
                        ContourNestingAnalyser.this.setMinMax(contours, i, headerOffset);
                        allOrientedAreas[i] = contours.preciseDoubledArea(i);
                    }
                } :
                block -> {
                    for (int i = block << 4, to = (int) Math.min((long) i + 16, numberOfContours); i < to; i++) {
                        final int headerOffset = contours.getSerializedHeaderOffset(i);
                        ContourNestingAnalyser.this.setMinMax(contours, i, headerOffset);
                    }
                });
//        long t3 = System.nanoTime();
        this.rectanglesFinder = IRectangleFinder.getInstance(allMinX, allMaxX, allMinY, allMaxY);
//        long t4 = System.nanoTime();
//        System.out.printf("!!! %d contours: %.3f + %.3f (%s) + %.3f ms%n",
//                numberOfContours, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
//                areasToCompare == null, (t4 - t3) * 1e-6);
    }

    public static ContourNestingAnalyser newInstance(
            Contours contours,
            boolean allContoursSurelyUnpacked,
            long[] areasToCompare) {
        return new ContourNestingAnalyser(contours, allContoursSurelyUnpacked, areasToCompare);
    }

    public static ContourNestingAnalyser newInstanceForUnpackedContours(Contours contours, long[] areasToCompare) {
        return new ContourNestingAnalyser(contours, true, areasToCompare);
    }

    public static ContourNestingAnalyser newInstance(Contours contours, long[] areasToCompare) {
        return new ContourNestingAnalyser(contours, false, areasToCompare);
    }

    public static ContourNestingAnalyser newInstance(Contours contours) {
        return new ContourNestingAnalyser(contours, false, null);
    }

    public boolean isNestingLevelNecessary() {
        return nestingLevelNecessary;
    }

    public ContourNestingAnalyser setNestingLevelNecessary(boolean nestingLevelNecessary) {
        this.nestingLevelNecessary = nestingLevelNecessary;
        return this;
    }

    public Contours contours() {
        return contours;
    }

    public boolean allContoursSurelyUnpacked() {
        return allContoursSurelyUnpacked;
    }

    // Can be used while multithreading
    public void findRectanglesContainingPoint(double x, double y, IntConsumer indexConsumer) {
        Objects.requireNonNull(indexConsumer, "Null consumer");
        if (pointOutOfRange(x, y)) {
            return;
        }
        final int roundX = (int) Math.round(x);
        final int roundY = (int) Math.round(y);
        // - if an integer rectangle contains non-integer (x,y), it always contains both floor(x/y) amd ceil(x/y)
        rectanglesFinder.findContaining(roundX, roundY, indexConsumer);
    }

    // Can be used while multithreading
    public void findContoursContainingInside(double x, double y, IntConsumer indexConsumer) {
        Objects.requireNonNull(indexConsumer, "Null consumer");
        findRectanglesContainingPoint(x, y,
                index -> {
                    if (contours.isPointStrictlyInside(index, x, y)) {
                        indexConsumer.accept(index);
                    }
                });
    }

    // Can be used while multithreading
    public NestingInformation analysePoint(double x, double y) {
        return analysePoint(null, x, y);
    }

    // Can be used while multithreading
    public NestingInformation analysePoint(NestingInformation result, double x, double y) {
        if (result == null) {
            result = new NestingInformation();
        }
        final MinimalContourFinder finder = getFinder().initializeCheckedPoint(x, y);
        findContainingRectangles(finder);
        result.nestingLevel = finder.getNestingLevel();
        result.nestingParent = finder.getNestingParent();
        return result;
    }

    // Cannot be used while multithreading
    public ContourNestingAnalyser analyseAllContours() {
        final int n = contours.numberOfContours();
        final int numberOfRanges = Multithreading.recommendedNumberOfParallelRanges(n);
        final int[] splitters = Multithreading.splitToRanges(n, numberOfRanges);
        final MinimalContourFinder[] finders = new MinimalContourFinder[numberOfRanges];
        java.util.Arrays.setAll(finders, value -> getFinder());
        Multithreading.loopStream(numberOfRanges).forEach(rangeIndex -> {
            MinimalContourFinder finder = finders[rangeIndex];
            final int from = splitters[rangeIndex];
            final int to = splitters[rangeIndex + 1];
            for (int k = from; k < to; k++) {
                finder.initializeCheckedContour(k);
                findContainingRectangles(finder);
                contourNestingLevels[k] = finder.getNestingLevel();
                contourNestingParents[k] = finder.getNestingParent();
            }
        });
        for (MinimalContourFinder finder : finders) {
            totalNumberOfNestingContours += finder.numberOfNestingContours;
            totalNumberOfCheckedContours += finder.numberOfCheckedContours;
            totalSummaryContoursLength += finder.summaryContoursLength;
        }
        return this;
    }

    public int[] getContourNestingLevels() {
        return contourNestingLevels;
    }

    public int[] getContourNestingParents() {
        return contourNestingParents;
    }

    public int numberOfNestingContours() {
        return totalNumberOfNestingContours;
    }

    public int numberOfCheckedContours() {
        return totalNumberOfCheckedContours;
    }

    public long summaryContoursLength() {
        return totalSummaryContoursLength;
    }

    @Override
    public String toString() {
        return "nesting analyser for "+ contours + ", using " + rectanglesFinder;
    }

    private void setMinMax(Contours contours, int i, int headerOffset) {
        allMinX[i] = contours.minX(headerOffset);
        allMaxX[i] = contours.maxX(headerOffset);
        allMinY[i] = contours.minY(headerOffset);
        allMaxY[i] = contours.maxY(headerOffset);
    }

    private void findContainingRectangles(MinimalContourFinder finder) {
        Objects.requireNonNull(finder, "Null finder");
        if (finder.isOutOfRange()) {
            return;
        }
        rectanglesFinder.findContaining(finder.roundX(), finder.roundY(), finder);
    }

    private static boolean pointOutOfRange(double x, double y) {
        return x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE;
    }

    private MinimalContourFinder getFinder() {
        return nestingLevelNecessary ? new MinimalContourWithLevelFinder() : new MinimalContourFinder();
    }

    private class MinimalContourFinder implements IntConsumer {
        long minimalCheckedArea;
        long minimalCheckedAreaAbs;
        double x;
        double y;
        private boolean outOfRange;
        private int roundX;
        private int roundY;
        int nestingLevel;
        long minArea;
        int nestingParent;
        int numberOfNestingContours = 0;
        int numberOfCheckedContours = 0;
        long summaryContoursLength = 0;
        private final Point2D representative = new Point2D.Double();

        MinimalContourFinder initializeCheckedPoint(double x, double y) {
            initializeCommon();
            this.minimalCheckedArea = this.minimalCheckedAreaAbs = Long.MAX_VALUE;
            this.x = x;
            this.y = y;
            this.outOfRange = pointOutOfRange(x, y);
            // - now we are sure, that if x and y are integer, so, they are represented absolutely exactly
            // (it is important for following rounding)
            roundPoint();
            return this;
        }

        MinimalContourFinder initializeCheckedContour(int checkedContourIndex) {
            initializeCommon();
            this.minimalCheckedArea = allOrientedAreas[checkedContourIndex];
            this.minimalCheckedAreaAbs = Math.abs(minimalCheckedArea);
            contours.findSomePointInside(representative, checkedContourIndex, allContoursSurelyUnpacked);
            this.x = representative.getX();
            this.y = representative.getY();
            this.outOfRange = false;
            roundPoint();
            return this;
        }

        private void initializeCommon() {
            this.nestingLevel = 0;
            this.minArea = Long.MAX_VALUE;
            this.nestingParent = -1;
        }

        private void roundPoint() {
            this.roundX = (int) Math.round(x);
            this.roundY = (int) Math.round(y);
            // - if an integer rectangle contains non-integer (x,y), it always contains both floor(x/y) amd ceil(x/y)
        }

        int roundX() {
            return roundX;
        }

        int roundY() {
            return roundY;
        }

        boolean isOutOfRange() {
            return outOfRange;
        }

        @Override
        public void accept(int index) {
            final long area = allOrientedAreas[index];
            final long areaAbs = Math.abs(area);
            if (areaAbs < minimalCheckedAreaAbs || (areaAbs == minimalCheckedAreaAbs && area >= minimalCheckedArea)) {
                // - 2nd case: area < minimalCheckedArea if
                // area < 0 (pore), but checked area > 0 (particle inside this pore)
                return;
            }
            if (areaAbs >= minArea) {
                return;
            }
            final long lengthAndOffset = contours.getContourLengthAndOffset(index);
            final int offset = Contours.extractOffset(lengthAndOffset);
            final int length = Contours.extractLength(lengthAndOffset);
            if (InsideContourStatus.isStrictlyInside(Contours.pointInsideContourInformation(
                    contours.points, offset, length, x, y, allContoursSurelyUnpacked, -1))) {
                // - would be little faster to accumulate contours and sort them before this check
                minArea = areaAbs;
                nestingParent = index;
                numberOfNestingContours++;
            }
            numberOfCheckedContours++;
            summaryContoursLength += length;
        }

        public int getNestingLevel() {
            return nestingLevel;
        }

        public int getNestingParent() {
            return nestingParent;
        }
    }

    private class MinimalContourWithLevelFinder extends MinimalContourFinder {
        @Override
        public void accept(int index) {
            final long area = allOrientedAreas[index];
            final long areaAbs = Math.abs(area);
            if (areaAbs < minimalCheckedAreaAbs || (areaAbs == minimalCheckedAreaAbs && area >= minimalCheckedArea)) {
                // - 2nd case: area < minimalCheckedArea,
                // if area < 0 (pore), but checked area > 0 (particle inside this pore)
                return;
            }
            final long lengthAndOffset = contours.getContourLengthAndOffset(index);
            final int offset = Contours.extractOffset(lengthAndOffset);
            final int length = Contours.extractLength(lengthAndOffset);
            if (InsideContourStatus.isStrictlyInside(Contours.pointInsideContourInformation(
                    contours.points, offset, length, x, y, allContoursSurelyUnpacked, -1))) {
                if (areaAbs < minArea) {
                    minArea = areaAbs;
                    nestingParent = index;
                }
                nestingLevel++;
                numberOfNestingContours++;
            }
            numberOfCheckedContours++;
            summaryContoursLength += length;
        }
    }
}
