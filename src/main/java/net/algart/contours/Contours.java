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
import net.algart.math.Point;
import net.algart.matrices.scanning.Boundary2DScanner;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * <p>Array of 2D contours.</p>
 * <p>Every contour is a sequence of 32-bit <tt>int</tt> values, describing N integer points (xk,yk).
 * Every contours starts from the header, containing K 32-bit special values.
 * In current version, K is 8 or 10,
 * Format:</p>
 *
 * <pre>
 *      MAGIC_WORD,        2*N+K,        (MAGIC_WORD = {@value
 *      ContourHeader#MAGIC_WORD_1} or {@value ContourHeader#MAGIC_WORD_2})
 *      minX,              maxX,
 *      minY,              maxY,
 *      RESERVED | FLAGS,  objectLabel,  (RESERVED = {@value ContourHeader#STANDARD_RESERVED_INDICATOR})
 *      RESERVED,          frameID       (optional pair)
 *      x1,                y1,
 *      . . .
 *      xN,                yN
 *
 * </pre>
 * <p>Here <tt>MAGIC_WORD</tt> contains (in low bits) the value K. Value 2*N+K is the total number of 32-bit
 * integers, occupied by this contour: the next contours starts at the position "offset of this contour"+2*N+K.</p>
 *
 * <p><tt>minX, maxX, minY, maxY</tt> are the mininal/maximal coordinates of all points in this contour,
 * i.e. the rectangle, containing all this contour.
 * (It is allowed to store here some greater rectangle, <i>containing</i> it, but if the contour
 * is created by standard methods, <tt>minX, maxX, minY, maxY</tt> will contain correct minimums and maximums.)</p>
 *
 * <p><tt>RESERVED | FLAGS</tt> contains in low bits some additional information (see {@link ContourHeader} class),
 * in particular, whether this contour is external or internal and whether this header contains
 * frameID (see below). <tt>objectLabel</tt> is an integer label of the object, the boundary of which
 * is represented by this contour.</p>
 *
 * <p>The last pair is optional. <tt>RESERVED</tt> is a special indicator 0x7FFF0000;
 * <tt>frameID</tt> is some integer ID, that can be stored together with contour
 * and should have some unique ID of the frame in a multi-frame map, from which this contour has been scanned.</p>
 */
public final class Contours {
    /**
     * Contours array consists of blocks (lines), where each block contains {@link #BLOCK_LENGTH} = {@value}
     * elements. Typical block contains two coordinates (x, y).
     */
    public static final int BLOCK_LENGTH = 2;

    /**
     * Coordinates of points, saved in the contour, cannot be out of range
     * <tt>-0x40000000</tt> &le; <i>x, y</i> &le;<tt>0x3FFFFFFF</tt>
     * It allows to guarantee that {@link ContourHeader#containingRectangle() containing rectangle}
     * of each contour has sizes, less than <tt>Integer.MAX_VALUE</tt>.
     * Also it allows to use positive values with bit #30 = 1 as reserved.
     */
    public static final int MIN_ABSOLUTE_COORDINATE = -0x40000000;
    public static final int MAX_ABSOLUTE_COORDINATE = 0x3FFFFFFF;
    // - Note: MIN_ABSOLUTE_COORDINATE and MAX_ABSOLUTE_COORDINATE must have these values!
    // For other values, checkPoint() functions will no work properly.

    private static final long MAX_ABSOLUTE_COORDINATE_HIGH_BITS = 0xFFFFFFFF80000000L;

    /**
     * Maximal allowed number of contours: {@value}.<br>
     * It is essentially less than <tt>Integer.MAX_VALUE</tt>, that allows to simplify
     * arithmetic operations with number of contours.
     * <p>More precisely, we guarantee that this value is less than <tt>Integer.MAX_VALUE</tt> / 4:
     * for example, you can freely multiply number of contours by 4.
     */
    public static final int MAX_NUMBER_OF_CONTOURS = 500_000_000;

    /**
     * Number of points, saved in the contour, cannot be greater than {@value} ~ 2<sup>30</sup>.
     * It allows to guarantee that each contour can be successfully stored in simple Java <tt>int[]</tt> array.
     */
    public static final int MAX_CONTOUR_NUMBER_OF_POINTS =
            (Integer.MAX_VALUE - ContourHeader.MAX_ALLOWED_HEADER_LENGTH) >> 1;
    // - Note: it is also useful (though not used) that it is essentially less than Integer.MAX_VALUE / 2;
    // so, we can freely add 1 or 2 to this value without overflow

    private static boolean DEBUG_MODE = false;

    int[] points = JArrays.EMPTY_INTS;
    int pointsLength = 0;
    private int[] contourHeaderOffsets = JArrays.EMPTY_INTS;
    // - note: we must store HEADER's offsets, not CONTOUR's offsets, because the header length may vary
    private int numberOfContours = 0;
    private int openedContourHeaderMarker = -1;
    private int openedContourPointsMarker = -1;
    private int touchingMatrixBoundaryFlags = 0;
    private int contourMinX;
    private int contourMaxX;
    private int contourMinY;
    private int contourMaxY;

    private boolean optimizeCollinearSteps = false;

    private Contours() {
    }

    public static Contours newInstance() {
        return new Contours();
    }

    public boolean isOptimizeCollinearSteps() {
        return optimizeCollinearSteps;
    }

    public Contours setOptimizeCollinearSteps(boolean optimizeCollinearSteps) {
        this.optimizeCollinearSteps = optimizeCollinearSteps;
        return this;
    }

    public static boolean isSerializedContours(int[] serialized) {
        return isSerializedContour(serialized, 0);
    }

    public static boolean isSerializedContour(int[] serialized, int offset) {
        return offset < serialized.length &&
                (serialized[offset] & ContourHeader.MAGIC_WORD_MASK) == ContourHeader.MAGIC_WORD;
    }

    public static Contours deserialize(int[] serialized) {
        Objects.requireNonNull(serialized, "Null serialized");
        final Contours r = new Contours();
        r.points = serialized.clone();
        r.pointsLength = serialized.length;
        // - since this moment, we will work with this copy
        ContourHeader checkedHeader = new ContourHeader();
        int p = 0;
        while (p < serialized.length) {
            if (r.numberOfContours >= MAX_NUMBER_OF_CONTOURS) {
                throw new TooLargeArrayException("Too large number of serialized contours: >"
                        + MAX_NUMBER_OF_CONTOURS);
                // - better message than while calling r.addContourHeaderOffset()
            }
            int value = r.points[p];
            if (!isSerializedContour(serialized, p)) {
                throw new IllegalArgumentException("Unsupported version of serialized contour array at position "
                        + p + ", contour #" + r.numberOfContours + ": start contour signature high bits (in value "
                        + value + "=0x" + Integer.toHexString(value) + ") are 0x"
                        + Integer.toHexString(value & ContourHeader.MAGIC_WORD_MASK)
                        + " instead of 0x"
                        + Integer.toHexString(ContourHeader.MAGIC_WORD));
            }
            final int headerLength = value & ContourHeader.HEADER_LENGTH_MASK;
            if (headerLength < ContourHeader.MIN_HEADER_LENGTH) {
                throw new IllegalArgumentException("Too short header: " + headerLength
                        + " elements < " + ContourHeader.MIN_HEADER_LENGTH);
            }
            final int fullLength = r.points[p + 1];
            if (fullLength <= headerLength || (fullLength - headerLength) % 2 != 0) {
                throw new IllegalArgumentException("Serialized contour length must be even and positive, "
                        + "but it = " + fullLength + " - " + headerLength
                        + " (full number of elements - number of header elements) at position " + p);
            }
            if ((long) p + (long) fullLength > serialized.length) {
                throw new IllegalArgumentException("Serialized contour has not enough elemets: current position " + p
                        + " + number of contour elements " + fullLength + " > array length" + serialized.length);
            }
            checkedHeader.read(r, p);
            // - check correctness of the header
            r.addContourHeaderOffset(p);
            p += fullLength;
        }
        assert p == r.pointsLength : "the check \"...>serialized.length\" didn't work properly";
        return r;
    }

    public int[] serialize() {
        return java.util.Arrays.copyOf(points, pointsLength);
    }

    public boolean isEmpty() {
        return numberOfContours == 0;
    }

    public int numberOfContours() {
        return numberOfContours;
    }

    public void clear() {
        clear(false);
    }

    public void clear(boolean freeResources) {
        checkClosed();
        pointsLength = 0;
        numberOfContours = 0;
        if (freeResources) {
            points = JArrays.EMPTY_INTS;
            contourHeaderOffsets = JArrays.EMPTY_INTS;
        }
    }


    /**
     * Starts adding points of new contour.
     *
     * <p>Note: most fields in the header may be not filled yet (they will be written in
     * {@link #closeContour(ContourHeader)} method).
     * But {@link ContourHeader#setFrameId(int) frame ID} it is an exception and must be filled <b>now</b>;
     * it cannot be added while closing contour.
     *
     * @param header header of new contour.
     */
    public void openContourForAdding(ContourHeader header) {
        if (numberOfContours >= MAX_NUMBER_OF_CONTOURS) {
            throw new TooLargeArrayException("Cannot add contour: current number of contours is maximally possible "
                    + MAX_NUMBER_OF_CONTOURS);
        }
        if (pointsLength > Integer.MAX_VALUE - ContourHeader.MAX_ALLOWED_HEADER_LENGTH) {
            throw new TooLargeArrayException("Too large contour array: > "
                    + (Integer.MAX_VALUE - ContourHeader.MAX_ALLOWED_HEADER_LENGTH) + " elements");
        }
        openedContourHeaderMarker = pointsLength;
        touchingMatrixBoundaryFlags = 0;
        contourMinX = Integer.MAX_VALUE;
        contourMaxX = Integer.MIN_VALUE;
        contourMinY = Integer.MAX_VALUE;
        contourMaxY = Integer.MIN_VALUE;
        header.appendToEndForAllocatingSpace(this);
        // - really, 0 points are already added
        openedContourPointsMarker = pointsLength;
    }

    public void addPoint(Boundary2DScanner scanner, long startX, long startY) {
        final long x = scanner.x() + scanner.lastStep().increasedPixelVertexX();
        final long y = scanner.y() + scanner.lastStep().increasedPixelVertexY();
        addPoint(
                startX + x,
                startY + y,
                x == 0,
                x == scanner.dimX(),
                y == 0,
                y == scanner.dimY());
    }

    public void addPoint(long x, long y) {
        addPoint(
                x,
                y,
                false,
                false,
                false,
                false);
    }

    public void addPoint(
            long x,
            long y,
            boolean xAtMinXMatrixBoundary,
            boolean xAtMaxXMatrixBoundary,
            boolean yAtMinYMatrixBoundary,
            boolean yAtMaxYMatrixBoundary) {
        checkPoint(x, y);
        if (!isContourOpened()) {
            throw new IllegalStateException("Cannot add point to closed contour");
        }
        final int intX = (int) x;
        final int intY = (int) y;
        if (intX < contourMinX) {
            contourMinX = intX;
        }
        if (intX > contourMaxX) {
            contourMaxX = intX;
        }
        if (intY < contourMinY) {
            contourMinY = intY;
        }
        if (intY > contourMaxY) {
            contourMaxY = intY;
        }
        if (xAtMinXMatrixBoundary) {
            touchingMatrixBoundaryFlags |= ContourHeader.TOUCHES_MIN_X_MATRIX_BOUNDARY_FLAG;
        }
        if (xAtMaxXMatrixBoundary) {
            touchingMatrixBoundaryFlags |= ContourHeader.TOUCHES_MAX_X_MATRIX_BOUNDARY_FLAG;
        }
        if (yAtMinYMatrixBoundary) {
            touchingMatrixBoundaryFlags |= ContourHeader.TOUCHES_MIN_Y_MATRIX_BOUNDARY_FLAG;
        }
        if (yAtMaxYMatrixBoundary) {
            touchingMatrixBoundaryFlags |= ContourHeader.TOUCHES_MAX_Y_MATRIX_BOUNDARY_FLAG;
        }
        final int length = pointsLength;
        if (length < ContourHeader.MIN_HEADER_LENGTH) {
            throw new IllegalStateException("Cannot add a point: header was not added yet");
        }
        if (optimizeCollinearSteps && length >= (long) openedContourPointsMarker + 4) {
            // - we need at least 2 previous points (4 elements) to check collinearity
            final int previousX = points[length - 2];
            final int previousY = points[length - 1];
            final int previousPreviousX = points[length - 4];
            final int previousPreviousY = points[length - 3];
            if (isReserved(previousX) || isReserved(previousPreviousX)) {
                throw new IllegalStateException("Damaged contour array: reserved value at position "
                        + (isReserved(previousX) ? length - 2 : length - 4));
            }
            if (x == previousX && y == previousY) {
                return;
            }
            final int previousDx = previousX - previousPreviousX;
            final int previousDy = previousY - previousPreviousY;
            final int dx = intX - previousX;
            final int dy = intY - previousY;
            // - overflow impossible: see MAX_ABSOLUTE_COORDINATE
            if (collinear32AndCodirectional(previousDx, previousDy, dx, dy) || (previousDx == 0 && previousDy == 0)) {
                points[length - 2] = intX;
                points[length - 1] = intY;
                return;
            }
        }
        if (pointsLength > points.length - 2) {
            ensureCapacityForPoints((long) pointsLength + 2L);
        }
        points[pointsLength++] = intX;
        points[pointsLength++] = intY;
    }

    /**
     * Finishes adding points and closes the contour.
     *
     * <p>Note: touching matrix boundary flags, stored in the contour array, are defined by previous
     * <tt>addPoint</tt> calls, <b>not</b> by these 4 flags from the header.
     * Corresponding 4 flags in the passed header are <b>ignored</b>.
     * {@link ContourHeader#containingRectangle() Containing rectangle} in the passed header is also <b>ignored</b>;
     * instead, this function stores in the contour array the rectangle, calculated while previous
     * <tt>addPoint</tt> calls.
     *
     * @param header additional information about contour.
     */
    public void closeContour(ContourHeader header) {
        Objects.requireNonNull(header, "Null header");
        if (!isContourOpened()) {
            throw new IllegalStateException("Cannot close already closed contour");
        }
        final int length = pointsLength;
        final int p = openedContourHeaderMarker;
        final int q = openedContourPointsMarker;
        openedContourHeaderMarker = -1;
        openedContourPointsMarker = -1;
        // Note: p >= 0 (see isContourOpened())
        if (q - p != header.headerLength()) {
            throw new IllegalStateException("Header length " + header.headerLength() + " does not match the length "
                    + "of header, previously written in the contour");
        }
        if (!isReserved(points[p])) {
            throw new IllegalStateException("Damaged contour array: no reserved value (signature) at position " + p);
        }
        if (length - q < 2) {
            throw new IllegalStateException("Cannot close empty contour: at least 1 point must be added "
                    + "(empty contours are not allowed");
        }
        final int numberOfPoints = (length - q) >> 1;
        if (numberOfPoints > MAX_CONTOUR_NUMBER_OF_POINTS) {
            throw new IllegalStateException("Too large number of points in a contour: it is > "
                    + MAX_CONTOUR_NUMBER_OF_POINTS);
        }
        points[p + 1] = length - p;
        points[p + ContourHeader.CONTAINING_RECTANGLE_OFFSET] = contourMinX;
        points[p + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 1] = contourMaxX;
        points[p + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 2] = contourMinY;
        points[p + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 3] = contourMaxY;
        points[p + ContourHeader.FLAGS_OFFSET] = ContourHeader.STANDARD_RESERVED_INDICATOR
                | touchingMatrixBoundaryFlags
                | (header.getFlagsWithoutContourTouching());
        points[p + ContourHeader.LABEL_OFFSET] = header.getObjectLabel();
        addContourHeaderOffset(p);
    }

    public boolean isContourOpened() {
        return openedContourHeaderMarker >= 0;
    }

    public void addContour(ContourHeader header, IntArray contour) {
        addContour(header, contour, false);
    }

    // Note: packAddedContour=true is not actually used in current library.
    public void addContour(ContourHeader header, IntArray contour, boolean packAddedContour) {
        Objects.requireNonNull(header, "Null header");
        Objects.requireNonNull(contour, "Null contour");
        if (contour instanceof DirectAccessible && ((DirectAccessible) contour).hasJavaArray()) {
            // - usually true, but can be false if contour was created manually in non-simple memory model
            final DirectAccessible da = (DirectAccessible) contour;
            final int[] points = (int[]) da.javaArray();
            final int offset = da.javaArrayOffset();
            final int length = da.javaArrayLength();
            addContour(header, points, offset, length);
            return;
        }
        checkClosed();
        if (numberOfContours >= MAX_NUMBER_OF_CONTOURS) {
            throw new TooLargeArrayException("Cannot add contour: current number of contours is maximally possible "
                    + MAX_NUMBER_OF_CONTOURS);
        }
        final int oldLength = pointsLength;
        if (packAddedContour) {
            final MutableIntArray resultContour = Arrays.SMM.newEmptyIntArray();
            packContour(resultContour, contour);
            contour = resultContour;
        }
        final long contourLength = contour.length();
        checkContourLength(contourLength);
        header.appendToEnd(this, (int) (contourLength >> 1));
        // - changes pointsLength
        if (!header.hasContainingRectangle()) {
            findContainingRectangle(points, oldLength + ContourHeader.CONTAINING_RECTANGLE_OFFSET, contour);
        }
        addContourHeaderOffset(oldLength);
        final long newPointsLength = (long) pointsLength + contourLength;
        ensureCapacityForPoints(newPointsLength);
        contour.getData(0, points, pointsLength, (int) contourLength);
        pointsLength = (int) newPointsLength;
    }

    public void addContour(ContourHeader header, int[] contour) {
        Objects.requireNonNull(contour, "Null contour");
        addContour(header, contour, 0, contour.length);
    }

    public void addContour(ContourHeader header, int[] contour, int contourOffset, int contourLength) {
        Objects.requireNonNull(contour, "Null contour");
        checkContourLengthAndOffset(contour, contourOffset, contourLength);
        checkClosed();
        if (numberOfContours >= MAX_NUMBER_OF_CONTOURS) {
            throw new TooLargeArrayException("Cannot add contour: current number of contours is maximally possible "
                    + MAX_NUMBER_OF_CONTOURS);
        }
        final int oldLength = pointsLength;
        final int headerLength = header.headerLength();
        final long occupiedContourLength = (long) headerLength + contourLength;
        final long newPointsLength = (long) pointsLength + occupiedContourLength;
        ensureCapacityForPoints(newPointsLength);
        pointsLength = (int) newPointsLength;
        final int newPosition = header.write(this, oldLength, contourLength >> 1);
        if (!header.hasContainingRectangle()) {
            findContainingRectangle(
                    points,
                    oldLength + ContourHeader.CONTAINING_RECTANGLE_OFFSET,
                    contour,
                    contourOffset,
                    contourLength);
        }
        System.arraycopy(contour, contourOffset, points, newPosition, contourLength);
        addContourHeaderOffset(oldLength);
    }

    public void addContoursRange(Contours contours, int from, int to) {
        Objects.requireNonNull(contours, "Null contours");
        if (from > to) {
            throw new IllegalArgumentException("from = " + from + " > to = " + to);
        }
        if (from < 0 || to > contours.numberOfContours) {
            throw new IndexOutOfBoundsException("Required contours range " + from + ".." + (to - 1)
                    + " is out of full range 0.." + (contours.numberOfContours - 1));
        }
        checkClosed();
        final int count = to - from;
        if (count == 0) {
            return;
        }
        // - now from < to, so, from < contours.numberOfContours
        final int oldNumberOfContours = numberOfContours;
        final int fromOffset = contours.contourHeaderOffsets[from];
        final int toOffset = to == contours.numberOfContours ?
                contours.pointsLength :
                contours.contourHeaderOffsets[to];
        final int pointsCount = toOffset - fromOffset;
        final int offsetIncrement = pointsLength - fromOffset;
        ensureCapacityForPoints((long) pointsLength + pointsCount);
        ensureCapacityForHeaderOffsets((long) numberOfContours + (long) count);
        System.arraycopy(contours.points, fromOffset, this.points, pointsLength, pointsCount);
        pointsLength += pointsCount;
        copyAndIncreaseArray(
                contours.contourHeaderOffsets,
                from,
                this.contourHeaderOffsets,
                numberOfContours,
                count,
                offsetIncrement);
        numberOfContours += count;
        for (int k = oldNumberOfContours; k < numberOfContours; k++) {
            testCorrectness(contourHeaderOffsets[k]);
            // - to be on the safe side
        }
    }

    public void addContour(Contours contours, int contourIndex) {
        Objects.requireNonNull(contours, "Null contours");
        contours.checkContourIndex(contourIndex);
        checkClosed();
        final int fromOffset = contours.contourHeaderOffsets[contourIndex];
        final int nextIndex = contourIndex + 1;
        final int toOffset = nextIndex == contours.numberOfContours ?
                contours.pointsLength :
                contours.contourHeaderOffsets[nextIndex];
        final int pointsCount = toOffset - fromOffset;
        final int oldLength = pointsLength;
        ensureCapacityForPoints((long) pointsLength + pointsCount);
        addContourHeaderOffset(oldLength);
        System.arraycopy(contours.points, fromOffset, this.points, pointsLength, pointsCount);
        pointsLength += pointsCount;
    }

    public Contours contoursRange(int from, int to) {
        final Contours result = Contours.newInstance();
        result.addContoursRange(this, from, to);
        return result;
    }

    public void addContours(Contours contours) {
        Objects.requireNonNull(contours, "Null contours");
        addContoursRange(contours, 0, contours.numberOfContours);
    }

    public void addTransformedContours(
            Contours contours,
            double scaleX,
            double scaleY,
            double shiftX,
            double shiftY,
            boolean removeDegeneratedContours) {
//        if (true) {
//            addTransformedContoursSimple(contours, scaleX, scaleY, shiftX, shiftY, removeDegeneratedContours);
//            return;
//        }
        Objects.requireNonNull(contours, "Null contours");
        ContourHeader header = new ContourHeader();
        ContourLength contourLength = new ContourLength();
        final boolean invertOrientation = (scaleX < 0.0) != (scaleY < 0.0);
        int[] transformed = null;
        int[] packed = null;
//        long tRead = 0, tTransform = 0, tPack = 0, tWrite = 0;
        for (int k = 0, n = contours.numberOfContours; k < n; k++) {
//            long t1 = System.nanoTime();
            contours.getHeader(header, k);
            transformed = contours.getContourPointsAndReallocateResult(transformed, contourLength, k);
//            long t2 = System.nanoTime();
//            tRead += t2 - t1;
            final int length = contourLength.getArrayLength();
            transformContour(transformed, 0, length, scaleX, scaleY, shiftX, shiftY);
//            long t3 = System.nanoTime();
//            tTransform += t3 - t2;
            packed = packContourAndReallocateResultUnchecked(
                    packed, contourLength, transformed, 0, length);
            // - points were already checked in transformContour
//            long t4 = System.nanoTime();
//            tPack += t4 - t3;
            final int packedLength = contourLength.getArrayLength();
            header.transformContainingRectangle(scaleX, scaleY, shiftX, shiftY);
            if (invertOrientation) {
                header.setInternalContour(!header.isInternalContour());
            }
            final int headerOffset = pointsLength;
            addContour(header, packed, 0, packedLength);
//            long t5 = System.nanoTime();
//            tWrite += t5 - t4;
            // unpackContour(numberOfContours - 1); // - uncomment this for simple built-in test
            if (removeDegeneratedContours) {
                // - reading containing rectangle, calculated inside addContour
                final boolean degeneratedX = minX(headerOffset) == maxX(headerOffset);
                final boolean degeneratedY = minY(headerOffset) == maxY(headerOffset);
                if (degeneratedX && degeneratedY && packedLength != 2) {
                    throw new AssertionError("packContour did not remove duplicated transformed");
                }
                if (degeneratedX || degeneratedY) {
                    removeLastContour();
                }
            }
        }
//        System.out.printf(Locale.US, "%.3f + %.3f + %.3f + %.3f%n",
//                tRead * 1e-6, tTransform * 1e-6, tPack * 1e-6, tWrite * 1e-6);
    }

    // Can be used for debugging addTransformedContours
    private void addTransformedContoursSimple(
            Contours contours,
            double scaleX,
            double scaleY,
            double shiftX,
            double shiftY,
            boolean removeDegeneratedContours) {
        Objects.requireNonNull(contours, "Null contours");
        ContourHeader header = new ContourHeader();
        final boolean invertOrientation = (scaleX < 0.0) != (scaleY < 0.0);
        final MutableIntArray transformed = Arrays.SMM.newEmptyIntArray();
        final MutableIntArray packed = Arrays.SMM.newEmptyIntArray();
        for (int k = 0, n = contours.numberOfContours; k < n; k++) {
            contours.getHeader(header, k);
            transformContour(transformed, contours.getContour(k), scaleX, scaleY, shiftX, shiftY);
            packContour(packed, transformed);
            header.removeContainingRectangle();
            if (invertOrientation) {
                header.setInternalContour(!header.isInternalContour());
            }
            final int headerOffset = pointsLength;
            addContour(header, packed);
            if (removeDegeneratedContours) {
                // - reading containing rectangle, calculated inside addContour
                final boolean degeneratedX = minX(headerOffset) == maxX(headerOffset);
                final boolean degeneratedY = minY(headerOffset) == maxY(headerOffset);
                if (degeneratedX && degeneratedY && packed.length() != 2) {
                    throw new AssertionError("packContour did not remove duplicated points");
                }
                if (degeneratedX || degeneratedY) {
                    removeLastContour();
                }
            }
        }
    }

    public Contours transformContours(
            double scaleX,
            double scaleY,
            double shiftX,
            double shiftY,
            boolean removeDegeneratedContours) {
        final Contours result = Contours.newInstance();
        result.addTransformedContours(this, scaleX, scaleY, shiftX, shiftY, removeDegeneratedContours);
        return result;
    }

    public void removeContoursRange(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("from = " + from + " > to = " + to);
        }
        if (from < 0 || to > numberOfContours) {
            throw new IndexOutOfBoundsException("Required contours range " + from + ".." + (to - 1)
                    + " is out of full range 0.." + (numberOfContours - 1));
        }
        checkClosed();
        if (from == to) {
            return;
        }
        // - now from < to, so, from < numberOfContours
        final int fromOffset = contourHeaderOffsets[from];
        final int toOffset = to == numberOfContours ? pointsLength : contourHeaderOffsets[to];
        assert fromOffset < toOffset : from + " < " + to + ", but offsets " + fromOffset + " >= " + toOffset;
        final int shiftedContoursCount = numberOfContours - to;
        final int shiftedPointsCounts = pointsLength - toOffset;
        final int offsetDecrement = toOffset - fromOffset;
        System.arraycopy(contourHeaderOffsets, to, contourHeaderOffsets, from, shiftedContoursCount);
        increaseArray(contourHeaderOffsets, from, shiftedContoursCount, -offsetDecrement);
        numberOfContours -= to - from;
        System.arraycopy(points, toOffset, points, fromOffset, shiftedPointsCounts);
        pointsLength -= offsetDecrement;
    }

    public void removeSingleContour(int contourIndex) {
        checkContourIndex(contourIndex);
        removeContoursRange(contourIndex, contourIndex + 1);
    }

    public void removeLastContour() {
        checkClosed();
        final int lastContourIndex = numberOfContours - 1;
        if (lastContourIndex < 0) {
            throw new IllegalStateException("No contours");
        }
        pointsLength = contourHeaderOffsets[lastContourIndex];
        numberOfContours = lastContourIndex;
    }

    private void checkClosed() {
        if (isContourOpened()) {
            throw new IllegalStateException("Cannot modify array with opened contour");
        }
    }

    public int getContourNumberOfPoints(int contourIndex) {
        return getContourLength(contourIndex) >> 1;
    }

    public int getContourLength(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        final int headerLength = points[p] & ContourHeader.HEADER_LENGTH_MASK;
        final int fullLength = points[p + 1];
        return fullLength - headerLength;
    }

    public int getContourOffset(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        testCorrectness(p);
        final int headerLength = points[p] & ContourHeader.HEADER_LENGTH_MASK;
        final int offset = p + headerLength;
        assert offset >= 0 : "Damaged contours" + this;
        return offset;
    }

    // Allows to little optimize access to both length and offset
    public long getContourLengthAndOffset(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        final int headerLength = points[p] & ContourHeader.HEADER_LENGTH_MASK;
        final int fullLength = points[p + 1];
        final int offset = p + headerLength;
        return packLowAndHigh(offset, fullLength - headerLength);
    }

    public static int extractLength(long lengthAndOffset) {
        return (int) (lengthAndOffset >>> 32);
    }

    public static int extractOffset(long lengthAndOffset) {
        return (int) lengthAndOffset;
    }

    // Note: can be used also for contourIndex = numberOfContours, then returns pointsLength
    public int getSerializedHeaderOffset(int contourIndex) {
        if (contourIndex == numberOfContours) {
            return pointsLength;
        }
        checkContourIndex(contourIndex);
        return contourHeaderOffsets[contourIndex];
    }

    public IntArray getContour(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return SimpleMemoryModel.asUpdatableIntArray(points).subArr(offset, length);
    }

    public int getContourPoints(int[] resultPoints, int contourIndex) {
        Objects.requireNonNull(resultPoints, "Null resultPoints");
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        System.arraycopy(points, offset, resultPoints, 0, length);
        return length;
    }

    public double getContourPointX(int contourIndex, int pointIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        final int m = length >> 1;
        if (pointIndex < 0 || pointIndex > m) {
            throw new IndexOutOfBoundsException("Index of contour point " + pointIndex
                    + " is out of range 0.." + (m - 1));
        }
        return points[offset + 2 * pointIndex];
    }

    public double getContourPointY(int contourIndex, int pointIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        final int m = length >> 1;
        if (pointIndex < 0 || pointIndex > m) {
            throw new IndexOutOfBoundsException("Index of contour point " + pointIndex
                    + " is out of range 0.." + (m - 1));
        }
        return points[offset + 2 * pointIndex + 1];
    }

    public int[] getContourPointsAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            int contourIndex) {
        Objects.requireNonNull(resultLength, "Null result length");
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        if (resultPoints == null) {
            resultPoints = new int[Math.max(length, 16)];
        }
        resultPoints = ensureCapacityForContour(resultPoints, length);
        System.arraycopy(points, offset, resultPoints, 0, length);
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;

    }

    public MutableIntArray unpackContour(int contourIndex) {
        return unpackContour(contourIndex, false);
    }

    public MutableIntArray unpackContour(int contourIndex, boolean processDiagonalSegments) {
        return unpackContour(getContour(contourIndex), processDiagonalSegments);
    }

    public int[] unpackContourAndReallocateResult(int[] resultPoints, ContourLength resultLength, int contourIndex) {
        return unpackContourAndReallocateResult(
                resultPoints, resultLength, contourIndex, false, false);
    }

    public int[] unpackContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            int contourIndex,
            boolean processDiagonalSegments) {
        return unpackContourAndReallocateResult(
                resultPoints, resultLength, contourIndex, processDiagonalSegments, false);
    }

    public Contours packContours() {
        final Contours packedContours = Contours.newInstance();
        final ContourHeader header = new ContourHeader();
        int[] packed = null;
        final ContourLength contourLength = new ContourLength();
        for (int k = 0; k < numberOfContours; k++) {
            getHeader(header, k);
            packed = Contours.packContourAndReallocateResult(packed, contourLength, getContour(k));
            packedContours.addContour(header, packed, 0, contourLength.getArrayLength());
        }
        return packedContours;
    }

    public Contours unpackContours() {
        return unpackContours(false, null);
    }

    public Contours unpackContours(boolean processDiagonalSegments) {
        return unpackContours(processDiagonalSegments, null);
    }

    public Contours unpackContours(boolean processDiagonalSegments, long[] doubledAreas) {
        final boolean withArea = doubledAreas != null;
        if (withArea && doubledAreas.length < numberOfContours) {
            throw new IllegalArgumentException("Too short doubledAreas array: its length " + doubledAreas.length
                    + " < number of contours = " + numberOfContours);
        }
        final Contours unpackedContours = Contours.newInstance();
        final ContourHeader header = new ContourHeader();
        int[] unpacked = null;
        final ContourLength contourLength = new ContourLength();
        for (int k = 0; k < numberOfContours; k++) {
            getHeader(header, k);
            unpacked = unpackContourAndReallocateResult(unpacked, contourLength, k, processDiagonalSegments, withArea);
            unpackedContours.addContour(header, unpacked, 0, contourLength.getArrayLength());
            if (withArea) {
                doubledAreas[k] = contourLength.doubledArea;
            }
        }
        return unpackedContours;
    }

    public ContourHeader getHeader(int contourIndex) {
        return getHeader(null, contourIndex);
    }

    public ContourHeader getHeader(ContourHeader resultHeader, int contourIndex) {
        checkContourIndex(contourIndex);
        if (resultHeader == null) {
            resultHeader = new ContourHeader();
        }
        resultHeader.read(this, contourHeaderOffsets[contourIndex]);
        return resultHeader;
    }

    public int getObjectLabel(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        return points[p + ContourHeader.LABEL_OFFSET];
    }

    // Note: it is the only setter that can have sense for header without full rebuilding the contour.
    public void setObjectLabel(int contourIndex, int label) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        points[p + ContourHeader.LABEL_OFFSET] = label;
    }

    public Integer getFrameIdOrNull(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        return (points[p + ContourHeader.FLAGS_OFFSET] & ContourHeader.HAS_FRAME_ID) != 0 ?
                points[p + ContourHeader.FRAME_ID_OFFSET] :
                null;
    }

    public boolean isInternalContour(int contourIndex) {
        checkContourIndex(contourIndex);
        final int p = contourHeaderOffsets[contourIndex];
        return (points[(p + ContourHeader.FLAGS_OFFSET)] & ContourHeader.INTERNAL_FLAG) != 0;
    }

    public int[] getAllObjectLabels() {
        final int[] result = new int[numberOfContours];
        for (int k = 0; k < result.length; k++) {
            result[k] = getObjectLabel(k);
        }
        return result;
    }

    public boolean[] getAllInternalContour() {
        final boolean[] result = new boolean[numberOfContours];
        for (int k = 0; k < result.length; k++) {
            result[k] = isInternalContour(k);
        }
        return result;
    }

    public int[] getAllFrameId(int absentId) {
        final int[] result = new int[numberOfContours];
        for (int k = 0; k < result.length; k++) {
            final Integer frameId = getFrameIdOrNull(k);
            result[k] = frameId == null ? absentId : frameId;
        }
        return result;
    }

    public boolean isPointStrictlyInside(int contourIndex, double x, double y) {
        return isPointStrictlyInside(contourIndex, x, y, false);
    }

    public boolean isPointStrictlyInside(int contourIndex, double x, double y, boolean surelyUnpacked) {
        return InsideContourStatus.INSIDE.matchesStatus(
                pointInsideContourInformation(contourIndex, x, y, surelyUnpacked));
    }

    public InsideContourStatus pointInsideContourStatus(int contourIndex, double x, double y, boolean surelyUnpacked) {
        return InsideContourStatus.valueOfInformation(
                pointInsideContourInformation(contourIndex, x, y, surelyUnpacked));
    }

    public double pointInsideContourInformation(int contourIndex, double x, double y, boolean surelyUnpacked) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return pointInsideContourInformation(points, offset, length, x, y, surelyUnpacked, -1);
    }

    public Point findSomePointInside(int contourIndex) {
        return findSomePointInside(contourIndex, false);
    }

    public Point findSomePointInside(int contourIndex, boolean surelyUnpacked) {
        final Point2D result = new Point2D.Double();
        if (findSomePointInside(result, contourIndex, surelyUnpacked)) {
            return Point.valueOf(result.getX(), result.getY());
        } else {
            return null;
        }
    }

    public boolean findSomePointInside(Point2D result, int contourIndex) {
        return findSomePointInside(result, contourIndex, false);
    }

    public boolean findSomePointInside(Point2D result, int contourIndex, boolean surelyUnpacked) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return findSomePointInside(result, points, offset, length, surelyUnpacked);
    }

    public double strictPerimeter(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return strictPerimeter(points, offset, length);
    }

    public double segmentCentersPerimeter(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return segmentCentersPerimeter(points, offset, length);
    }

    public double strictArea(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return strictArea(points, offset, length);
    }

    public double segmentCentersArea(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return segmentCentersArea(points, offset, length);
    }

    public long preciseDoubledArea(int contourIndex) {
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        final int length = extractLength(lengthAndOffset);
        return preciseDoubledArea(points, offset, length);
    }

    public void sortIndexesByLabels(int[] indexes) {
        sortIndexesByLabels(indexes, false);
    }

    // Can be useful to find all particle(s) and its pores for each label.
    public void sortIndexesByLabels(int[] indexes, boolean internalContoursFirst) {
        Objects.requireNonNull(indexes, "Null indexes");
        ArraySorter.getQuickSorter().sortIndexes(indexes, 0, indexes.length,
                internalContoursFirst ?
                        (firstIndex, secondIndex) -> {
                            final int label1 = getObjectLabel(firstIndex);
                            final int label2 = getObjectLabel(secondIndex);
                            return label1 < label2 || (label1 == label2
                                    && isInternalContour(firstIndex) && !isInternalContour(secondIndex));
                        } :
                        (firstIndex, secondIndex) -> {
                            final int label1 = getObjectLabel(firstIndex);
                            final int label2 = getObjectLabel(secondIndex);
                            return label1 < label2 || (label1 == label2
                                    && !isInternalContour(firstIndex) && isInternalContour(secondIndex));
                        }
        );
    }

    public void sortIndexesByPreciseArea(int[] indexes, boolean absoluteValueOfArea) {
        sortIndexesByPreciseArea(indexes, absoluteValueOfArea, false);
    }

    // Can be useful, for example, for drawing nested objects: greater objects should be drawn first.
    // Works correctly only if contour area is never >= 2^62 (impossible for usual contours).
    public void sortIndexesByPreciseArea(int[] indexes, boolean absoluteValueOfArea, boolean greaterAreasFirst) {
        Objects.requireNonNull(indexes, "Null indexes");
        final int n = indexes.length;
        final long[] areas = new long[n];
        IntStream.range(0, (n + 15) >>> 4).parallel().forEach(block -> {
            for (int i = block << 4, to = (int) Math.min((long) i + 16, n); i < to; i++) {
                final int index = indexes[i];
                if (index < 0 || index >= numberOfContours) {
                    throw new IndexOutOfBoundsException("Index of contour " + index + " (at ths position #"
                            + i + " in the index array) is out of range 0.." + (numberOfContours - 1));
                }
                long area = preciseDoubledArea(index);
                if (absoluteValueOfArea) {
                    area = Math.abs(area);
                }
                areas[i] = greaterAreasFirst ? -area : area;
            }
        });
        // Here we cannot use sortIndexes: it requires areas[] array for all contours with length "numberOfContours",
        // but we prefer to use shorter array with length "indexes.length"
        ArraySorter.getQuickSorter().sort(0, n,
                (i, j) -> areas[i] < areas[j],
                (firstIndex, secondIndex) -> {
                    final int tempIndex = indexes[firstIndex];
                    indexes[firstIndex] = indexes[secondIndex];
                    indexes[secondIndex] = tempIndex;
                    final long tempArea = areas[firstIndex];
                    areas[firstIndex] = areas[secondIndex];
                    areas[secondIndex] = tempArea;
                });
    }

    public boolean equalsToSerialized(int[] serialized) {
        if (serialized == null || serialized.length != pointsLength) {
            // - while comparison with serialized form, we require accurate length of the argument,
            // like in deserialize() method; but "points" array is usually longer
            return false;
        }
        return JArrays.arrayEquals(points, 0, serialized, 0, pointsLength);
    }

    @Override
    public String toString() {
        return "array of " + numberOfContours + " contours (int[" + pointsLength + "])";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Contours contours = (Contours) o;
        return pointsLength == contours.pointsLength
                && optimizeCollinearSteps == contours.optimizeCollinearSteps
                && JArrays.arrayEquals(points, 0, contours.points, 0, pointsLength);
        // - note that points.length is usually > pointsLength (after ensureCapacity)
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(pointsLength, optimizeCollinearSteps);
        for (int i = 0; i < pointsLength; i++) {
            result = 31 * result + points[i];
        }
        return result;
    }

    public static int getContourNumberOfPoints(IntArray contour) {
        checkContourLength(contour);
        return (int) contour.length() >> 1;
    }

    public static int getContourPoints(int[] resultPoints, IntArray contour) {
        Objects.requireNonNull(resultPoints, "Null resultPoints");
        checkContourLength(contour);
        final int length = (int) contour.length();
        contour.getData(0, resultPoints, 0, length);
        return length >> 1;
    }

    public static int[] getContourPointsAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            IntArray contour) {
        Objects.requireNonNull(resultLength, "Null result length");
        checkContourLength(contour);
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        final int length = (int) contour.length();
        resultPoints = ensureCapacityForContour(resultPoints, length);
        contour.getData(0, resultPoints, 0, length);
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }


    public static int packContour(MutableIntArray resultContour, IntArray nonOptimizedContour) {
        Objects.requireNonNull(resultContour, "Null result contour");
        Objects.requireNonNull(nonOptimizedContour, "Null non-optimized contour");
        long n = nonOptimizedContour.length();
        checkContourLength(n);
        n = removeLastIdenticalPoints(nonOptimizedContour, n);
        resultContour.clear();
        int previousPreviousX = nonOptimizedContour.getInt(0);
        int previousPreviousY = nonOptimizedContour.getInt(1);
        checkPoint(previousPreviousX, previousPreviousY);
        final int x0 = previousPreviousX;
        final int y0 = previousPreviousY;
        resultContour.pushInt(x0);
        resultContour.pushInt(y0);
        if (n == 2) {
            return 1;
            // - degenerated contour, containing only 1 point or several identical points
        }
        assert n >= 4;
        long i = findFirstNotIdenticalPoint(nonOptimizedContour, n, x0, y0);
        int previousX = nonOptimizedContour.getInt(i++);
        int previousY = nonOptimizedContour.getInt(i++);
        assert previousX != x0 || previousY != y0;
        checkPoint(previousX, previousY);
        final int dx0 = previousX - x0;
        final int dy0 = previousY - y0;
        resultContour.pushInt(previousX);
        resultContour.pushInt(previousY);
        assert i <= n;
        assert i >= 4;
        // - we need at least 2 previous points (4 elements) to check collinearity
        while (i < n) {
            final int x = nonOptimizedContour.getInt(i++);
            final int y = nonOptimizedContour.getInt(i++);
            if (x == previousX && y == previousY) {
                continue;
            }
            checkPoint(x, y);
            final int previousDx = previousX - previousPreviousX;
            final int previousDy = previousY - previousPreviousY;
            final int dx = x - previousX;
            final int dy = y - previousY;
            // - overflow impossible: see MAX_ABSOLUTE_COORDINATE
            if (collinear32AndCodirectional(previousDx, previousDy, dx, dy)) {
                // - note: previousDx=previousDy=0 is impossible
                final long length = resultContour.length();
                resultContour.setInt(length - 2, x);
                resultContour.setInt(length - 1, y);
            } else {
                resultContour.pushInt(x);
                resultContour.pushInt(y);
                previousPreviousX = previousX;
                previousPreviousY = previousY;
            }
            previousX = x;
            previousY = y;
        }
        assert resultContour.length() <= n;
        correctPackedContourWithCollinearFirstAndLastSegments(resultContour, x0, y0, dx0, dy0);
        return (int) (resultContour.length() >> 1);
    }

    public static int[] packContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            IntArray nonOptimizedContour) {
        Objects.requireNonNull(resultLength, "Null result length");
        Objects.requireNonNull(nonOptimizedContour, "Null non-optimized contour");
        long n = nonOptimizedContour.length();
        checkContourLength(n);
        if (nonOptimizedContour instanceof DirectAccessible
                && ((DirectAccessible) nonOptimizedContour).hasJavaArray()) {
            // - usually true, but can be false if nonOptimizedContour was created manually in non-simple memory model
            final DirectAccessible da = (DirectAccessible) nonOptimizedContour;
            final int[] points = (int[]) da.javaArray();
            final int offset = da.javaArrayOffset();
            final int length = da.javaArrayLength();
            return packContourAndReallocateResult(resultPoints, resultLength, points, offset, length);
        }
        n = removeLastIdenticalPoints(nonOptimizedContour, n);
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        int previousPreviousX = nonOptimizedContour.getInt(0);
        int previousPreviousY = nonOptimizedContour.getInt(1);
        checkPoint(previousPreviousX, previousPreviousY);
        final int x0 = previousPreviousX;
        final int y0 = previousPreviousY;
        resultPoints[0] = x0;
        resultPoints[1] = y0;
        resultLength.setNumberOfPoints(1);
        if (n == 2) {
            return resultPoints;
        }
        assert n >= 4;
        long i = findFirstNotIdenticalPoint(nonOptimizedContour, n, x0, y0);
        int previousX = nonOptimizedContour.getInt(i++);
        int previousY = nonOptimizedContour.getInt(i++);
        assert previousX != x0 || previousY != y0;
        checkPoint(previousX, previousY);
        final int dx0 = previousX - x0;
        final int dy0 = previousY - y0;
        resultPoints[2] = previousX;
        resultPoints[3] = previousY;
        resultLength.setNumberOfPoints(2);
        assert i <= n;
        assert i >= 4;
        // - we need at least 2 previous points (4 elements) to check collinearity
        int length = 4;
        while (i < n) {
            final int x = nonOptimizedContour.getInt(i++);
            final int y = nonOptimizedContour.getInt(i++);
            if (x == previousX && y == previousY) {
                continue;
            }
            checkPoint(x, y);
            final int previousDx = previousX - previousPreviousX;
            final int previousDy = previousY - previousPreviousY;
            final int dx = x - previousX;
            final int dy = y - previousY;
            // - overflow impossible: see MAX_ABSOLUTE_COORDINATE
            if (collinear32AndCodirectional(previousDx, previousDy, dx, dy)) {
                // - note: previousDx=previousDy=0 is impossible
                resultPoints[length - 2] = x;
                resultPoints[length - 1] = y;
            } else {
                if (length > resultPoints.length - 2) {
                    resultPoints = ensureCapacityForContour(resultPoints, (long) length + 2);
                }
                resultPoints[length++] = x;
                resultPoints[length++] = y;
                previousPreviousX = previousX;
                previousPreviousY = previousY;
            }
            previousX = x;
            previousY = y;
        }
        assert length <= n;
        length = correctPackedContourWithCollinearFirstAndLastSegments(resultPoints, x0, y0, dx0, dy0, length);
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }

    /*Repeat() checkPoint\(\w+,\s*\w+\);\s*(?:\r(?!\n)|\n|\r\n)\s* ==> ;;
               public static ==> static ;;
               (packContourAndReallocateResult) ==> $1Unchecked
    */
    public static int[] packContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            int[] nonOptimizedContour,
            int nonOptimizedOffset,
            int n) {
        Objects.requireNonNull(resultLength, "Null result length");
        Objects.requireNonNull(nonOptimizedContour, "Null non-optimized contour");
        checkContourLength(n);
        n = removeLastIdenticalPoints(nonOptimizedContour, nonOptimizedOffset, n);
        if (nonOptimizedOffset > nonOptimizedContour.length - n) {
            throw new IndexOutOfBoundsException("Offset in array = " + nonOptimizedOffset
                    + ", length since this offset = " + n
                    + ", but array length = " + nonOptimizedContour.length);
        }
        final int to = nonOptimizedOffset + n;
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        final int x0 = nonOptimizedContour[nonOptimizedOffset];
        final int y0 = nonOptimizedContour[nonOptimizedOffset + 1];
        checkPoint(x0, y0);
        resultPoints[0] = x0;
        resultPoints[1] = y0;
        resultLength.setNumberOfPoints(1);
        if (n == 2) {
            return resultPoints;
        }
        nonOptimizedOffset = findFirstNotIdenticalPoint(nonOptimizedContour, nonOptimizedOffset, n, x0, y0);
        int previousX = nonOptimizedContour[nonOptimizedOffset++];
        int previousY = nonOptimizedContour[nonOptimizedOffset++];
        assert previousX != x0 || previousY != y0;
        assert n >= 4;
        checkPoint(previousX, previousY);
        final int dx0 = previousX - x0;
        final int dy0 = previousY - y0;
        int previousDx = dx0;
        int previousDy = dy0;
        int length = 2;
        // - decreased by 1 point
        int resultPointsLengthMinus2 = resultPoints.length - 2;
        assert nonOptimizedOffset <= to;
        while (nonOptimizedOffset < to) {
            final int x = nonOptimizedContour[nonOptimizedOffset++];
            final int y = nonOptimizedContour[nonOptimizedOffset++];
            final int dx = x - previousX;
            final int dy = y - previousY;
            // - overflow impossible (when non-zero): see checkPoint(x, y)
            if ((dx | dy) == 0) {
                continue;
            }
            checkPoint(x, y);
            if (!collinear32AndCodirectional(previousDx, previousDy, dx, dy)) {
                if (length > resultPointsLengthMinus2) {
                    resultPoints = ensureCapacityForContour(resultPoints, (long) length + 2);
                    resultPointsLengthMinus2 = resultPoints.length - 2;
                }
                resultPoints[length++] = previousX;
                resultPoints[length++] = previousY;
                previousDx = dx;
                previousDy = dy;
            }
            previousX = x;
            previousY = y;
        }
        if (length > resultPointsLengthMinus2) {
            resultPoints = ensureCapacityForContour(resultPoints, (long) length + 2);
        }
        resultPoints[length++] = previousX;
        resultPoints[length++] = previousY;
        assert length <= n;
        length = correctPackedContourWithCollinearFirstAndLastSegments(resultPoints, x0, y0, dx0, dy0, length);
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    static int[] packContourAndReallocateResultUnchecked(
            int[] resultPoints,
            ContourLength resultLength,
            int[] nonOptimizedContour,
            int nonOptimizedOffset,
            int n) {
        Objects.requireNonNull(resultLength, "Null result length");
        Objects.requireNonNull(nonOptimizedContour, "Null non-optimized contour");
        checkContourLength(n);
        n = removeLastIdenticalPoints(nonOptimizedContour, nonOptimizedOffset, n);
        if (nonOptimizedOffset > nonOptimizedContour.length - n) {
            throw new IndexOutOfBoundsException("Offset in array = " + nonOptimizedOffset
                    + ", length since this offset = " + n
                    + ", but array length = " + nonOptimizedContour.length);
        }
        final int to = nonOptimizedOffset + n;
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        final int x0 = nonOptimizedContour[nonOptimizedOffset];
        final int y0 = nonOptimizedContour[nonOptimizedOffset + 1];
        resultPoints[0] = x0;
        resultPoints[1] = y0;
        resultLength.setNumberOfPoints(1);
        if (n == 2) {
            return resultPoints;
        }
        nonOptimizedOffset = findFirstNotIdenticalPoint(nonOptimizedContour, nonOptimizedOffset, n, x0, y0);
        int previousX = nonOptimizedContour[nonOptimizedOffset++];
        int previousY = nonOptimizedContour[nonOptimizedOffset++];
        assert previousX != x0 || previousY != y0;
        assert n >= 4;
        final int dx0 = previousX - x0;
        final int dy0 = previousY - y0;
        int previousDx = dx0;
        int previousDy = dy0;
        int length = 2;
        // - decreased by 1 point
        int resultPointsLengthMinus2 = resultPoints.length - 2;
        assert nonOptimizedOffset <= to;
        while (nonOptimizedOffset < to) {
            final int x = nonOptimizedContour[nonOptimizedOffset++];
            final int y = nonOptimizedContour[nonOptimizedOffset++];
            final int dx = x - previousX;
            final int dy = y - previousY;
            // - overflow impossible (when non-zero): see checkPoint(x, y)
            if ((dx | dy) == 0) {
                continue;
            }
            if (!collinear32AndCodirectional(previousDx, previousDy, dx, dy)) {
                if (length > resultPointsLengthMinus2) {
                    resultPoints = ensureCapacityForContour(resultPoints, (long) length + 2);
                    resultPointsLengthMinus2 = resultPoints.length - 2;
                }
                resultPoints[length++] = previousX;
                resultPoints[length++] = previousY;
                previousDx = dx;
                previousDy = dy;
            }
            previousX = x;
            previousY = y;
        }
        if (length > resultPointsLengthMinus2) {
            resultPoints = ensureCapacityForContour(resultPoints, (long) length + 2);
        }
        resultPoints[length++] = previousX;
        resultPoints[length++] = previousY;
        assert length <= n;
        length = correctPackedContourWithCollinearFirstAndLastSegments(resultPoints, x0, y0, dx0, dy0, length);
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }

    /*Repeat.AutoGeneratedEnd*/

    public static MutableIntArray unpackContour(IntArray optimizedContour, boolean processDiagonalSegments) {
        final MutableIntArray resultContour = Arrays.SMM.newEmptyIntArray();
        unpackContour(resultContour, optimizedContour, processDiagonalSegments);
        return resultContour;
    }

    public static int unpackContour(
            MutableIntArray resultContour,
            IntArray optimizedContour,
            boolean processDiagonalSegments) {
        Objects.requireNonNull(resultContour, "Null result contour");
        Objects.requireNonNull(optimizedContour, "Null optimized contour");
        long n = optimizedContour.length();
        checkContourLength(n);
        n = removeLastIdenticalPoints(optimizedContour, n);
        resultContour.clear();
        int lastX = optimizedContour.getInt(n - 2);
        int lastY = optimizedContour.getInt(n - 1);
        checkPoint(lastX, lastY);
        if (n == 2) {
            // - degenerated contour, containing only 1 point or several identical points,
            // should be processed separately: the common algorithm below will not find any segments in it
            resultContour.pushInt(lastX);
            resultContour.pushInt(lastY);
            return 1;
        }
        for (long i = 0; i < n; i += 2) {
            final int x = optimizedContour.getInt(i);
            final int y = optimizedContour.getInt(i + 1);
            checkPoint(x, y);
            // - below we add all points between (lastX, lastY) until (x, y), EXCLUDING (lastX, lastY)
            if (y == lastY) {
                if (lastX < x) {
                    while (lastX < x) {
                        checkAbilityToAddToContour(resultContour);
                        resultContour.pushInt(++lastX);
                        resultContour.pushInt(lastY);
                    }
                } else {
                    while (lastX > x) {
                        checkAbilityToAddToContour(resultContour);
                        resultContour.pushInt(--lastX);
                        resultContour.pushInt(lastY);
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (x == lastX) {
                if (lastY < y) {
                    while (lastY < y) {
                        checkAbilityToAddToContour(resultContour);
                        resultContour.pushInt(lastX);
                        resultContour.pushInt(++lastY);
                    }
                } else {
                    while (lastY > y) {
                        checkAbilityToAddToContour(resultContour);
                        resultContour.pushInt(lastX);
                        resultContour.pushInt(--lastY);
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (processDiagonalSegments) {
                addDiagonalExcludingFirst(resultContour, lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            } else {
                throw new IllegalArgumentException("Cannot unpack contour containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + ((i == 0 ? n : i) / 2 - 1) + " and #" + i / 2);
            }
            assert lastX == x;
            assert lastY == y;
        }
        final long resultNumberOfPoints = resultContour.length() >> 1;
        assert resultNumberOfPoints <= MAX_CONTOUR_NUMBER_OF_POINTS;
        return (int) resultNumberOfPoints;
    }


    public static int[] unpackContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            IntArray optimizedContour,
            boolean processDiagonalSegments) {
        Objects.requireNonNull(resultLength, "Null result length");
        Objects.requireNonNull(optimizedContour, "Null optimized contour");
        long n = optimizedContour.length();
        checkContourLength(n);
        n = removeLastIdenticalPoints(optimizedContour, n);
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        int lastX = optimizedContour.getInt(n - 2);
        int lastY = optimizedContour.getInt(n - 1);
        checkPoint(lastX, lastY);
        resultLength.doubledArea = 0;
        if (n == 2) {
            resultPoints[0] = lastX;
            resultPoints[1] = lastY;
            resultLength.setNumberOfPoints(1);
            return resultPoints;
        }
        if (optimizedContour instanceof DirectAccessible && ((DirectAccessible) optimizedContour).hasJavaArray()) {
            final DirectAccessible da = (DirectAccessible) optimizedContour;
            final int[] points = (int[]) da.javaArray();
            final int offset = da.javaArrayOffset();
            return unpackContourAndReallocateResult(
                    resultPoints, resultLength, points, offset, (int) n, lastX, lastY, processDiagonalSegments);
        } else {
            return unpackContourAndReallocateResult(
                    resultPoints, resultLength, optimizedContour, n, lastX, lastY, processDiagonalSegments);
        }
    }

    public static MutableIntArray reverseContour(MutableIntArray resultContour, IntArray contour) {
        Objects.requireNonNull(resultContour, "Null result contour");
        checkContourLength(contour);
        resultContour.clear();
        for (long i = contour.length() - 2; i >= 0; i -= 2) {
            resultContour.pushInt(contour.getInt(i));
            resultContour.pushInt(contour.getInt(i + 1));
        }
        return resultContour;
    }

    public static MutableIntArray transformContour(
            MutableIntArray resultContour,
            IntArray contour,
            double scaleX,
            double scaleY,
            double shiftX,
            double shiftY) {
        Objects.requireNonNull(resultContour, "Null result contour");
        checkContourLength(contour);
        resultContour.length(0);
        for (long i = 0, n = contour.length(); i < n; i += 2) {
            final long x = Math.round(scaleX * contour.getInt(i) + shiftX);
            final long y = Math.round(scaleY * contour.getInt(i + 1) + shiftY);
            checkPoint(x, y);
            resultContour.pushInt((int) x);
            resultContour.pushInt((int) y);
        }
        return resultContour;
    }

    public static void transformContour(
            int[] contour,
            int offset,
            int n,
            double scaleX,
            double scaleY,
            double shiftX,
            double shiftY) {
        checkContourLengthAndOffset(contour, offset, n);
        if (scaleX == 1.0 && scaleY == 1.0 && shiftX == 0.0 && shiftY == 0.0) {
            return;
        }
        IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
            int i = offset + (block << 8);
            final int to = (int) Math.min((long) i + 256, offset + n);
            for (; i < to; i += 2) {
                final long x = Math.round(scaleX * contour[i] + shiftX);
                final long y = Math.round(scaleY * contour[i + 1] + shiftY);
                checkPoint(x, y);
                contour[i] = (int) x;
                contour[i + 1] = (int) y;
            }
        });
    }

    public static double pointInsideContourInformation(
            int[] contour,
            final int offset,
            final int n,
            final double x,
            final double y,
            final boolean surelyUnpacked) {
        return pointInsideContourInformation(contour, offset, n, x, y, surelyUnpacked, -1);
    }

    public static boolean findSomePointInside(
            Point2D result,
            int[] contour,
            final int offset,
            final int n,
            final boolean surelyUnpacked) {
        checkContourLengthAndOffset(contour, offset, n);
        Objects.requireNonNull(result, "Null result point");
        int lastX = contour[offset];
        int lastY = contour[offset + 1];
        result.setLocation(lastX, lastY);
        if (n == 2) {
            return false;
        }
        final int offsetTo = offset + n;
        assert offset < offsetTo;
        for (int p = offset + 2; p < offsetTo; p += 2) {
            final int pointX = contour[p];
            final int pointY = contour[p + 1];
            if (pointY != lastY) {
                double x = 0.5 * (double) ((long) lastX + (long) pointX);
                double y = 0.5 * (double) ((long) lastY + (long) pointY);
                final double information = pointInsideContourInformation(contour, offset, n, x, y, surelyUnpacked, p);
                boolean ok = InsideContourStatus.isStrictlyInside(information);
                // Strictly inside is possible, if this segment is fully rounded by another parts of this contour
                if (!ok) {
                    // Note: horizontal boundary is also possible here, if the contour returns back, for example:
                    // 340, 100
                    // 340, 110
                    // 340, 105
                    // 350, 105
                    // 350, 100
                    ok = InsideContourStatus.isNormalLeftRightBoundary(information);
                    if (ok) {
                        double x2 = InsideContourStatus.getBoundarySectionSecondEnd(information);
                        x = 0.5 * (x + x2);
                        if (DEBUG_MODE) {
                            final double newInfo = pointInsideContourInformation(
                                    contour, offset, n, x, y, surelyUnpacked);
                            if (!InsideContourStatus.isStrictlyInside(newInfo)) {
                                throw new AssertionError("Failure of finding inside point: we find ("
                                        + x + ", " + y + "), starting from ("
                                        + 0.5 * (double) ((long) lastX + (long) pointX) + ", " + y
                                        + "), but its status is " + InsideContourStatus.valueOfInformation(newInfo)
                                        + " (information: " + newInfo + ")");
                            }
                        }
                    }
                }
                if (ok) {
                    result.setLocation(x, y);
                    return true;
                }
            }
            lastX = pointX;
            lastY = pointY;
        }
        return false;
    }

    public static double strictPerimeter(IntArray contour) {
        checkContourLength(contour);
        final long n = contour.length();
        int lastX = contour.getInt(n - 2);
        int lastY = contour.getInt(n - 1);
        double perimeter = 0.0;
        for (long i = 0; i < n; i += 2) {
            final int x = contour.getInt(i);
            final int y = contour.getInt(i + 1);
            final long dx = (long) x - (long) lastX;
            final long dy = (long) y - (long) lastY;
            final long dSqr = dx * dx + dy * dy;
            perimeter += dSqr <= 1 ? dSqr : Math.sqrt((double) dSqr);
            // - if dSqr = 0 or 1, sqrt(dSqr) is strictly equal to dSqr
            lastX = x;
            lastY = y;
        }
        return perimeter;
    }

    public static double strictPerimeter(int[] contour, int offset, int n) {
        checkContourLengthAndOffset(contour, offset, n);
        int lastX = contour[offset + n - 2];
        int lastY = contour[offset + n - 1];
        double perimeter = 0.0;
        for (int i = offset, to = i + n; i < to; i += 2) {
            final int x = contour[i];
            final int y = contour[i + 1];
            final long dx = (long) x - (long) lastX;
            final long dy = (long) y - (long) lastY;
            final long dSqr = dx * dx + dy * dy;
            perimeter += dSqr <= 1 ? dSqr : Math.sqrt((double) dSqr);
            // - if dSqr = 0 or 1, sqrt(dSqr) is strictly equal to dSqr
            lastX = x;
            lastY = y;
        }
        return perimeter;
    }

    public static double segmentCentersPerimeter(IntArray contour) {
        checkContourLength(contour);
        final long n = contour.length();
        long x1 = contour.getInt(n - 2);
        long y1 = contour.getInt(n - 1);
        long x2 = contour.getInt(0);
        long y2 = contour.getInt(1);
        double lastX = 0.5 * (x1 + x2);
        double lastY = 0.5 * (y1 + y2);
        double perimeter = 0.0;
        for (long i = 0; i < n; i += 2) {
            x1 = x2;
            y1 = y2;
            final long j = i + 2 < n ? i + 2 : 0;
            x2 = contour.getInt(j);
            y2 = contour.getInt(j + 1);
            final double x = 0.5 * (x1 + x2);
            final double y = 0.5 * (y1 + y2);
            final double dx = x - lastX;
            final double dy = y - lastY;
            final double dSqr = dx * dx + dy * dy;
            perimeter += Math.sqrt(dSqr);
            lastX = x;
            lastY = y;
        }
        return perimeter;
    }

    public static double segmentCentersPerimeter(int[] contour, int offset, int n) {
        checkContourLengthAndOffset(contour, offset, n);
        long x1 = contour[offset + n - 2];
        long y1 = contour[offset + n - 1];
        long x2 = contour[offset];
        long y2 = contour[offset + 1];
        double lastX = 0.5 * (x1 + x2);
        double lastY = 0.5 * (y1 + y2);
        double perimeter = 0.0;
        for (int i = offset, to = i + n; i < to; i += 2) {
            x1 = x2;
            y1 = y2;
            final int j = i + 2 < to ? i + 2 : offset;
            x2 = contour[j];
            y2 = contour[j + 1];
            final double x = 0.5 * (x1 + x2);
            final double y = 0.5 * (y1 + y2);
            final double dx = x - lastX;
            final double dy = y - lastY;
            final double dSqr = dx * dx + dy * dy;
            perimeter += Math.sqrt(dSqr);
            lastX = x;
            lastY = y;
        }
        return perimeter;
    }

    public static double strictArea(IntArray contour) {
        checkContourLength(contour);
        final long n = contour.length();
        int lastX = contour.getInt(n - 2);
        int lastY = contour.getInt(n - 1);
        double area = 0.0;
        for (long i = 0; i < n; i += 2) {
            final int x = contour.getInt(i);
            final int y = contour.getInt(i + 1);
            area += ((long) x + (long) lastX) * ((long) y - (long) lastY);
            lastX = x;
            lastY = y;
        }
        return 0.5 * area;
    }

    public static double strictArea(int[] contour, int offset, int n) {
        checkContourLengthAndOffset(contour, offset, n);
        int lastX = contour[offset + n - 2];
        int lastY = contour[offset + n - 1];
        double area = 0.0;
        for (int i = offset, to = i + n; i < to; i += 2) {
            final int x = contour[i];
            final int y = contour[i + 1];
            area += ((long) x + (long) lastX) * ((long) y - (long) lastY);
            lastX = x;
            lastY = y;
        }
        return 0.5 * area;
    }

    public static double segmentCentersArea(IntArray contour) {
        checkContourLength(contour);
        final long n = contour.length();
        long x1 = contour.getInt(n - 2);
        long y1 = contour.getInt(n - 1);
        long x2 = contour.getInt(0);
        long y2 = contour.getInt(1);
        double lastX = 0.5 * (x1 + x2);
        double lastY = 0.5 * (y1 + y2);
        double area = 0.0;
        for (long i = 0; i < n; i += 2) {
            x1 = x2;
            y1 = y2;
            final long j = i + 2 < n ? i + 2 : 0;
            x2 = contour.getInt(j);
            y2 = contour.getInt(j + 1);
            final double x = 0.5 * (x1 + x2);
            final double y = 0.5 * (y1 + y2);
            area += (x + lastX) * (y - lastY);
            lastX = x;
            lastY = y;
        }
        return 0.5 * area;
    }

    public static double segmentCentersArea(int[] contour, int offset, int n) {
        checkContourLengthAndOffset(contour, offset, n);
        long x1 = contour[offset + n - 2];
        long y1 = contour[offset + n - 1];
        long x2 = contour[offset];
        long y2 = contour[offset + 1];
        double lastX = 0.5 * (x1 + x2);
        double lastY = 0.5 * (y1 + y2);
        double area = 0.0;
        for (int i = offset, to = i + n; i < to; i += 2) {
            x1 = x2;
            y1 = y2;
            final int j = i + 2 < to ? i + 2 : offset;
            x2 = contour[j];
            y2 = contour[j + 1];
            final double x = 0.5 * (x1 + x2);
            final double y = 0.5 * (y1 + y2);
            area += (x + lastX) * (y - lastY);
            lastX = x;
            lastY = y;
        }
        return 0.5 * area;
    }

    public static long preciseDoubledArea(int[] contour, int offset, int n) {
        checkContourLengthAndOffset(contour, offset, n);
        int lastX = contour[offset + n - 2];
        int lastY = contour[offset + n - 1];
        long area = 0;
        for (int i = offset, to = i + n; i < to; i += 2) {
            final int x = contour[i];
            final int y = contour[i + 1];
            area += ((long) x + (long) lastX) * ((long) y - (long) lastY);
            // - note: the result will be even, unless there are diagonal segments
            lastX = x;
            lastY = y;
        }
        return area;
    }

    int minX(int headerOffset) {
        return points[headerOffset + ContourHeader.CONTAINING_RECTANGLE_OFFSET];
    }

    int maxX(int headerOffset) {
        return points[headerOffset + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 1];
    }

    int minY(int headerOffset) {
        return points[headerOffset + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 2];
    }

    int maxY(int headerOffset) {
        return points[headerOffset + ContourHeader.CONTAINING_RECTANGLE_OFFSET + 3];
    }

    void ensureCapacityForPoints(long newPointsLength) {
        if (newPointsLength > points.length) {
            if (newPointsLength > Integer.MAX_VALUE) {
                // - we can check this inside the previous check: points.length <= Integer.MAX_VALUE always
                throw new TooLargeArrayException("Too large requested contour array: >=2^31 elements");
            }
            points = java.util.Arrays.copyOf(points, Math.max(16,
                    Math.max((int) newPointsLength,
                            (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * points.length)))));
        }
    }

    void checkContourIndex(int k) {
        if (k < 0 || k >= numberOfContours) {
            throw new IndexOutOfBoundsException("Index of contour " + k
                    + " is out of range 0.." + (numberOfContours - 1));
        }
    }

    private void addContourHeaderOffset(int newHeaderOffset) {
        if (numberOfContours >= contourHeaderOffsets.length) {
            if (numberOfContours >= MAX_NUMBER_OF_CONTOURS) {
                throw new TooLargeArrayException("Cannot add contour: current number of contours "
                        + "is maximally possible " + MAX_NUMBER_OF_CONTOURS);
            }
            ensureCapacityForHeaderOffsets(numberOfContours + 1L);
        }
        contourHeaderOffsets[numberOfContours++] = newHeaderOffset;
    }

    private void ensureCapacityForHeaderOffsets(long newNumberOfContours) {
        if (newNumberOfContours > MAX_NUMBER_OF_CONTOURS) {
            // - we need to check this before the following: it is independent test
            throw new TooLargeArrayException("Too large requested number of contours: > "
                    + MAX_NUMBER_OF_CONTOURS);
        }
        if (newNumberOfContours > contourHeaderOffsets.length) {
            contourHeaderOffsets = java.util.Arrays.copyOf(contourHeaderOffsets, Math.max(16,
                    Math.max((int) newNumberOfContours,
                            (int) Math.min(MAX_NUMBER_OF_CONTOURS, (long) (2.0 * contourHeaderOffsets.length)))));
        }
    }

    private void testCorrectness(int headerOffset) {
        assert (points[headerOffset] & ContourHeader.MAGIC_WORD_MASK) == ContourHeader.MAGIC_WORD :
                "Contours array " + this + " damaged: invalid element 0x" + Integer.toHexString(points[headerOffset])
                        + " at offset " + headerOffset;
    }

    private static void checkContourLength(long length) {
        if (length < 0) {
            // - necessary while called for a length, passed directly
            throw new IllegalArgumentException("Negative contour length " + length);
        }
        if (length >> 1 > MAX_CONTOUR_NUMBER_OF_POINTS) {
            throw new IllegalArgumentException("Too large number of points in a contour: it is > "
                    + MAX_CONTOUR_NUMBER_OF_POINTS);
        }
        if (length % BLOCK_LENGTH != 0) {
            throw new IllegalArgumentException("Contour length must be even, but it is " + length);
        }
        if (length == 0) {
            throw new IllegalArgumentException("Empty contour is not allowed");
        }
    }

    private static void checkContourLengthAndOffset(int[] contour, int offset, int length) {
        Objects.requireNonNull(contour, "Null contour");
        if (offset < 0) {
            throw new IllegalArgumentException("Negative contour offset " + offset);
        }
        checkContourLength(length);
        if (offset > contour.length - length) {
            throw new IndexOutOfBoundsException("Contour offset + length >= contour length = "
                    + contour.length);
        }
        // so, offset + length is always 32-bit integer
    }

    private static void findContainingRectangle(
            int[] minXMaxXMinYMaxY,
            int position,
            IntArray contour) {
        Objects.requireNonNull(minXMaxXMinYMaxY, "Null array for results");
        if (position < 0) {
            throw new IllegalArgumentException("Negative position = " + position);
        }
        if (minXMaxXMinYMaxY.length < position + 4) {
            throw new IllegalArgumentException("Too short result array for minX, maxX, minY, maxY: its length "
                    + minXMaxXMinYMaxY.length + " < " + position + " + 4");
        }
        checkContourLength(contour);
        final long n = contour.length();
        int minX = contour.getInt(0);
        int maxX = minX;
        int minY = contour.getInt(1);
        int maxY = minY;
        for (long disp = 2; disp < n; ) {
            final int x = contour.getInt(disp++);
            final int y = contour.getInt(disp++);
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        minXMaxXMinYMaxY[position++] = minX;
        minXMaxXMinYMaxY[position++] = maxX;
        minXMaxXMinYMaxY[position++] = minY;
        minXMaxXMinYMaxY[position] = maxY;
    }

    private static void findContainingRectangle(
            int[] minXMaxXMinYMaxY,
            int position,
            int[] contour,
            int contourOffset,
            int n) {
        Objects.requireNonNull(minXMaxXMinYMaxY, "Null array for results");
        if (position < 0) {
            throw new IllegalArgumentException("Negative position = " + position);
        }
        if (minXMaxXMinYMaxY.length < position + 4) {
            throw new IllegalArgumentException("Too short result array for minX, maxX, minY, maxY: its length "
                    + minXMaxXMinYMaxY.length + " < " + position + " + 4");
        }
        checkContourLength(n);
        int disp = contourOffset;
        int minX = contour[disp++];
        int maxX = minX;
        int minY = contour[disp++];
        int maxY = minY;
        for (int to = contourOffset + n; disp < to; ) {
            final int x = contour[disp++];
            final int y = contour[disp++];
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        minXMaxXMinYMaxY[position++] = minX;
        minXMaxXMinYMaxY[position++] = maxX;
        minXMaxXMinYMaxY[position++] = minY;
        minXMaxXMinYMaxY[position] = maxY;
    }

    int[] unpackContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            int contourIndex,
            boolean processDiagonalSegments,
            boolean calculateArea) {
        Objects.requireNonNull(resultLength, "Null result length");
        if (resultPoints == null || resultPoints.length < 16) {
            resultPoints = new int[16];
        }
        final long lengthAndOffset = getContourLengthAndOffset(contourIndex);
        final int offset = extractOffset(lengthAndOffset);
        int n = extractLength(lengthAndOffset);
        n = removeLastIdenticalPoints(points, offset, n);
        int lastX = points[offset + n - 2];
        int lastY = points[offset + n - 1];
        resultPoints[0] = lastX;
        resultPoints[1] = lastY;
        resultLength.setNumberOfPoints(1);
        resultLength.doubledArea = 0;
        if (n == 2) {
            return resultPoints;
        }
        if (calculateArea) {
            return unpackContourAndReallocateResultWithArea(
                    resultPoints, resultLength, points, offset, n, lastX, lastY, processDiagonalSegments);
        } else {
            return unpackContourAndReallocateResult(
                    resultPoints, resultLength, points, offset, n, lastX, lastY, processDiagonalSegments);
        }
    }


    /*Repeat()
            (unpackContourAndReallocateResult)WithArea ==> $1,,$1WithArea,,$1;;
            ((?:long\s+|resultLength\.)?doubledArea[ \w\+\-\*\(\)\=;]*(?:\r(?!\n)|\n|\r\n)\s*) ==> ,,$1,, ;;
            (IntArray\s+optimizedContour) ==> $1,,int[] optimizedContour,
            final int optimizedOffset,,int[] optimizedContour,
            final int optimizedOffset;;
            (final long n\,) ==> $1,,final int n, ,,final int n,;;
            (for \(long disp = 0; disp < n; disp \+= 2\)) ==> $1,,
            for (int disp = optimizedOffset, to = disp + n; disp < to; disp += 2),,
            for (int disp = optimizedOffset, to = disp + n; disp < to; disp += 2);;
            optimizedContour.getInt\(([^\)]*)\) ==> optimizedContour.getInt($1),,optimizedContour[$1],,optimizedContour[$1]
    */
    private static int[] unpackContourAndReallocateResultWithArea(
            int[] resultPoints,
            ContourLength resultLength,
            IntArray optimizedContour,
            final long n,
            int lastX,
            int lastY,
            boolean processDiagonalSegments) {
        assert n >= 2;
        int length = 0;
        final MutableInt mutableLength = new MutableInt();
        long doubledArea = 0;
        for (long disp = 0; disp < n; disp += 2) {
            final int x = optimizedContour.getInt(disp);
            final int y = optimizedContour.getInt(disp + 1);
            checkPoint(x, y);
            if (y == lastY) {
                if (lastX < x) {
                    final long newLength = ((long) (x - lastX) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX < x) {
                        resultPoints[length++] = ++lastX;
                        resultPoints[length++] = lastY;
                    }
                } else {
                    final long newLength = ((long) (lastX - x) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX > x) {
                        resultPoints[length++] = --lastX;
                        resultPoints[length++] = lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (x == lastX) {
                final long doubledDifference = (long) (y - lastY) << 1;
                doubledArea += x * doubledDifference;
                if (lastY < y) {
                    final long newLength = doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY < y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = ++lastY;
                    }
                } else {
                    final long newLength = -doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY > y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = --lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (processDiagonalSegments) {
                mutableLength.value = length;
                doubledArea += ((long) x + (long) lastX) * ((long) y - (long) lastY);
                resultPoints = addDiagonalExcludingFirst(resultPoints, mutableLength, lastX, lastY, x, y);
                length = mutableLength.value;
                lastX = x;
                lastY = y;
            } else {
                throw new IllegalArgumentException("Cannot unpack contour containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + ((disp == 0 ? n : disp) / 2 - 1) + " and #" + disp / 2);
            }
            assert lastX == x;
            assert lastY == y;
        }
        resultLength.setNumberOfPoints(length >> 1);
        resultLength.doubledArea = doubledArea;
        return resultPoints;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static int[] unpackContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            IntArray optimizedContour,
            final long n,
            int lastX,
            int lastY,
            boolean processDiagonalSegments) {
        assert n >= 2;
        int length = 0;
        final MutableInt mutableLength = new MutableInt();
        for (long disp = 0; disp < n; disp += 2) {
            final int x = optimizedContour.getInt(disp);
            final int y = optimizedContour.getInt(disp + 1);
            checkPoint(x, y);
            if (y == lastY) {
                if (lastX < x) {
                    final long newLength = ((long) (x - lastX) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX < x) {
                        resultPoints[length++] = ++lastX;
                        resultPoints[length++] = lastY;
                    }
                } else {
                    final long newLength = ((long) (lastX - x) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX > x) {
                        resultPoints[length++] = --lastX;
                        resultPoints[length++] = lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (x == lastX) {
                final long doubledDifference = (long) (y - lastY) << 1;
                if (lastY < y) {
                    final long newLength = doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY < y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = ++lastY;
                    }
                } else {
                    final long newLength = -doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY > y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = --lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (processDiagonalSegments) {
                mutableLength.value = length;
                resultPoints = addDiagonalExcludingFirst(resultPoints, mutableLength, lastX, lastY, x, y);
                length = mutableLength.value;
                lastX = x;
                lastY = y;
            } else {
                throw new IllegalArgumentException("Cannot unpack contour containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + ((disp == 0 ? n : disp) / 2 - 1) + " and #" + disp / 2);
            }
            assert lastX == x;
            assert lastY == y;
        }
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }


    private static int[] unpackContourAndReallocateResultWithArea(
            int[] resultPoints,
            ContourLength resultLength,
            int[] optimizedContour,
            final int optimizedOffset,
            final int n,
            int lastX,
            int lastY,
            boolean processDiagonalSegments) {
        assert n >= 2;
        int length = 0;
        final MutableInt mutableLength = new MutableInt();
        long doubledArea = 0;
        for (int disp = optimizedOffset, to = disp + n; disp < to; disp += 2) {
            final int x = optimizedContour[disp];
            final int y = optimizedContour[disp + 1];
            checkPoint(x, y);
            if (y == lastY) {
                if (lastX < x) {
                    final long newLength = ((long) (x - lastX) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX < x) {
                        resultPoints[length++] = ++lastX;
                        resultPoints[length++] = lastY;
                    }
                } else {
                    final long newLength = ((long) (lastX - x) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX > x) {
                        resultPoints[length++] = --lastX;
                        resultPoints[length++] = lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (x == lastX) {
                final long doubledDifference = (long) (y - lastY) << 1;
                doubledArea += x * doubledDifference;
                if (lastY < y) {
                    final long newLength = doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY < y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = ++lastY;
                    }
                } else {
                    final long newLength = -doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY > y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = --lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (processDiagonalSegments) {
                mutableLength.value = length;
                doubledArea += ((long) x + (long) lastX) * ((long) y - (long) lastY);
                resultPoints = addDiagonalExcludingFirst(resultPoints, mutableLength, lastX, lastY, x, y);
                length = mutableLength.value;
                lastX = x;
                lastY = y;
            } else {
                throw new IllegalArgumentException("Cannot unpack contour containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + ((disp == 0 ? n : disp) / 2 - 1) + " and #" + disp / 2);
            }
            assert lastX == x;
            assert lastY == y;
        }
        resultLength.setNumberOfPoints(length >> 1);
        resultLength.doubledArea = doubledArea;
        return resultPoints;
    }


    private static int[] unpackContourAndReallocateResult(
            int[] resultPoints,
            ContourLength resultLength,
            int[] optimizedContour,
            final int optimizedOffset,
            final int n,
            int lastX,
            int lastY,
            boolean processDiagonalSegments) {
        assert n >= 2;
        int length = 0;
        final MutableInt mutableLength = new MutableInt();
        for (int disp = optimizedOffset, to = disp + n; disp < to; disp += 2) {
            final int x = optimizedContour[disp];
            final int y = optimizedContour[disp + 1];
            checkPoint(x, y);
            if (y == lastY) {
                if (lastX < x) {
                    final long newLength = ((long) (x - lastX) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX < x) {
                        resultPoints[length++] = ++lastX;
                        resultPoints[length++] = lastY;
                    }
                } else {
                    final long newLength = ((long) (lastX - x) << 1) + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastX > x) {
                        resultPoints[length++] = --lastX;
                        resultPoints[length++] = lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (x == lastX) {
                final long doubledDifference = (long) (y - lastY) << 1;
                if (lastY < y) {
                    final long newLength = doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY < y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = ++lastY;
                    }
                } else {
                    final long newLength = -doubledDifference + (long) length;
                    if (newLength > resultPoints.length) {
                        resultPoints = ensureCapacityForContour(resultPoints, newLength);
                    }
                    while (lastY > y) {
                        resultPoints[length++] = lastX;
                        resultPoints[length++] = --lastY;
                    }
                }
                // note: the loop above REMOVES duplicated points if exist
            } else if (processDiagonalSegments) {
                mutableLength.value = length;
                resultPoints = addDiagonalExcludingFirst(resultPoints, mutableLength, lastX, lastY, x, y);
                length = mutableLength.value;
                lastX = x;
                lastY = y;
            } else {
                throw new IllegalArgumentException("Cannot unpack contour containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + ((disp == 0 ? n : disp) / 2 - 1) + " and #" + disp / 2);
            }
            assert lastX == x;
            assert lastY == y;
        }
        resultLength.setNumberOfPoints(length >> 1);
        return resultPoints;
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat()
        void addDiagonalExcludingFirst\(MutableIntArray result ==>
        int[] addDiagonalExcludingFirst(int[] result, MutableInt len;;
        final long savedLength = result\.length\(\) ==> final int savedLength = len.value;
        int length = savedLength;;
        result.length\(\) ==> length;;
        checkAbilityToAddToContour\(result, numberOfSegmentsWithLength1\) ==>
        result = ensureCapacityForContour(result, newLength);
        len.value = (int) newLength;;
        result\.pushInt\(([^\)]*)\) ==> result[length++] = $1;;
        \/\/return; - for repeater ==> return result;
    */
    private static void addDiagonalExcludingFirst(MutableIntArray result, int x1, int y1, int x2, int y2) {
        assert x1 != x2 && y1 != y2 :
                "illegal usage for non-diagonal line ())" + x1 + "," + y1 + ") - (" + x2 + "," + y2 + ")";
        final int xDifference = x2 - x1;
        final int yDifference = y2 - y1;
        final int xLengthMinus1 = Math.abs(xDifference);
        final int yLengthMinus1 = Math.abs(yDifference);
        final int numberOfSegmentsWithLength1 = xLengthMinus1 + yLengthMinus1;
        // - overflow impossible due to checkPoint; by the same reason, overflows impossible in the loops below
        final long savedLength = result.length();
        final long newLength = savedLength + 2 * (long) numberOfSegmentsWithLength1;
        checkAbilityToAddToContour(result, numberOfSegmentsWithLength1);
        if (xLengthMinus1 < yLengthMinus1) {
            final double tangent = (double) xDifference / (double) yDifference;
            // (x2-x1)/(y2-y1) or, that is the same, (x1-x2)/(y1-y2) (it is more suitable for branch y1>=y2)
            if (y1 < y2) {
                double dy = 1.0;
                int lastX = x1;
                for (int y = y1 + 1; y < y2; y++) {
                    final int x = x1 + (int) Math.rint(dy * tangent);
                    dy += 1.0;
                    if (x != lastX) {
                        result.pushInt(lastX);
                        result.pushInt(y);
                        lastX = x;
                    }
                    result.pushInt(x);
                    result.pushInt(y);
                }
                if (x2 != lastX) {
                    result.pushInt(lastX);
                    result.pushInt(y2);
                }
            } else {
                double dy = yLengthMinus1 - 1;
                int nextX = x2 + (int) Math.rint(dy * tangent);
                if (x1 != nextX) {
                    result.pushInt(nextX);
                    result.pushInt(y1);
                }
                final int y2p1 = y2 + 1;
                for (int y = y1 - 1; y > y2; y--) {
                    final int x = nextX;
                    result.pushInt(x);
                    result.pushInt(y);
                    dy -= 1.0;
                    nextX = y == y2p1 ? x2 : x2 + (int) Math.rint(dy * tangent);
                    if (x != nextX) {
                        result.pushInt(nextX);
                        result.pushInt(y);
                    }
                }
            }
        } else {
            final double tangent = (double) yDifference / (double) xDifference;
            // (y2-y1)/(x2-x1) or, that is the same, (y1-y2)/(x1-x2) (it is more suitable for branch x1>=x2)
            if (x1 < x2) {
                double dx = 1.0;
                int lastY = y1;
                for (int x = x1 + 1; x < x2; x++) {
                    final int y = y1 + (int) Math.rint(dx * tangent);
                    dx += 1.0;
                    if (y != lastY) {
                        result.pushInt(x);
                        result.pushInt(lastY);
                        lastY = y;
                    }
                    result.pushInt(x);
                    result.pushInt(y);
                }
                if (y2 != lastY) {
                    result.pushInt(x2);
                    result.pushInt(lastY);
                }
            } else {
                double dx = xLengthMinus1 - 1;
                int nextY = y2 + (int) Math.rint(dx * tangent);
                if (y1 != nextY) {
                    result.pushInt(x1);
                    result.pushInt(nextY);
                }
                final int x2p1 = x2 + 1;
                for (int x = x1 - 1; x > x2; x--) {
                    final int y = nextY;
                    result.pushInt(x);
                    result.pushInt(y);
                    dx -= 1.0;
                    nextY = x == x2p1 ? y2 : y2 + (int) Math.rint(dx * tangent);
                    if (y != nextY) {
                        result.pushInt(x);
                        result.pushInt(nextY);
                    }
                }
            }
        }
        result.pushInt(x2);
        result.pushInt(y2);
        // - we prefer to add the last point without floating-point operations
        if (result.length() != newLength) {
            throw new AssertionError("Illegal result length: " + result.length()
                    + " instead of " + newLength + " = " + savedLength + " + 2 * " + numberOfSegmentsWithLength1);
        }
        //return; - for repeater
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static int[] addDiagonalExcludingFirst(int[] result, MutableInt len, int x1, int y1, int x2, int y2) {
        assert x1 != x2 && y1 != y2 :
                "illegal usage for non-diagonal line ())" + x1 + "," + y1 + ") - (" + x2 + "," + y2 + ")";
        final int xDifference = x2 - x1;
        final int yDifference = y2 - y1;
        final int xLengthMinus1 = Math.abs(xDifference);
        final int yLengthMinus1 = Math.abs(yDifference);
        final int numberOfSegmentsWithLength1 = xLengthMinus1 + yLengthMinus1;
        // - overflow impossible due to checkPoint; by the same reason, overflows impossible in the loops below
        final int savedLength = len.value;
        int length = savedLength;
        final long newLength = savedLength + 2 * (long) numberOfSegmentsWithLength1;
        result = ensureCapacityForContour(result, newLength);
        len.value = (int) newLength;
        if (xLengthMinus1 < yLengthMinus1) {
            final double tangent = (double) xDifference / (double) yDifference;
            // (x2-x1)/(y2-y1) or, that is the same, (x1-x2)/(y1-y2) (it is more suitable for branch y1>=y2)
            if (y1 < y2) {
                double dy = 1.0;
                int lastX = x1;
                for (int y = y1 + 1; y < y2; y++) {
                    final int x = x1 + (int) Math.rint(dy * tangent);
                    dy += 1.0;
                    if (x != lastX) {
                        result[length++] = lastX;
                        result[length++] = y;
                        lastX = x;
                    }
                    result[length++] = x;
                    result[length++] = y;
                }
                if (x2 != lastX) {
                    result[length++] = lastX;
                    result[length++] = y2;
                }
            } else {
                double dy = yLengthMinus1 - 1;
                int nextX = x2 + (int) Math.rint(dy * tangent);
                if (x1 != nextX) {
                    result[length++] = nextX;
                    result[length++] = y1;
                }
                final int y2p1 = y2 + 1;
                for (int y = y1 - 1; y > y2; y--) {
                    final int x = nextX;
                    result[length++] = x;
                    result[length++] = y;
                    dy -= 1.0;
                    nextX = y == y2p1 ? x2 : x2 + (int) Math.rint(dy * tangent);
                    if (x != nextX) {
                        result[length++] = nextX;
                        result[length++] = y;
                    }
                }
            }
        } else {
            final double tangent = (double) yDifference / (double) xDifference;
            // (y2-y1)/(x2-x1) or, that is the same, (y1-y2)/(x1-x2) (it is more suitable for branch x1>=x2)
            if (x1 < x2) {
                double dx = 1.0;
                int lastY = y1;
                for (int x = x1 + 1; x < x2; x++) {
                    final int y = y1 + (int) Math.rint(dx * tangent);
                    dx += 1.0;
                    if (y != lastY) {
                        result[length++] = x;
                        result[length++] = lastY;
                        lastY = y;
                    }
                    result[length++] = x;
                    result[length++] = y;
                }
                if (y2 != lastY) {
                    result[length++] = x2;
                    result[length++] = lastY;
                }
            } else {
                double dx = xLengthMinus1 - 1;
                int nextY = y2 + (int) Math.rint(dx * tangent);
                if (y1 != nextY) {
                    result[length++] = x1;
                    result[length++] = nextY;
                }
                final int x2p1 = x2 + 1;
                for (int x = x1 - 1; x > x2; x--) {
                    final int y = nextY;
                    result[length++] = x;
                    result[length++] = y;
                    dx -= 1.0;
                    nextY = x == x2p1 ? y2 : y2 + (int) Math.rint(dx * tangent);
                    if (y != nextY) {
                        result[length++] = x;
                        result[length++] = nextY;
                    }
                }
            }
        }
        result[length++] = x2;
        result[length++] = y2;
        // - we prefer to add the last point without floating-point operations
        if (length != newLength) {
            throw new AssertionError("Illegal result length: " + length
                    + " instead of " + newLength + " = " + savedLength + " + 2 * " + numberOfSegmentsWithLength1);
        }
        return result;
    }

    /*Repeat.AutoGeneratedEnd*/

    private static long removeLastIdenticalPoints(IntArray nonOptimizedContour, long n) {
        assert n >= 2;
        final int lastX = nonOptimizedContour.getInt(n - 2);
        final int lastY = nonOptimizedContour.getInt(n - 1);
        while (n > 2
                && nonOptimizedContour.getInt(n - 4) == lastX
                && nonOptimizedContour.getInt(n - 3) == lastY) {
            n -= 2;
        }
        return n;
    }

    private static int removeLastIdenticalPoints(int[] nonOptimizedContour, int nonOptimizedOffset, int n) {
        assert n >= 2;
        final int lastX = nonOptimizedContour[nonOptimizedOffset + n - 2];
        final int lastY = nonOptimizedContour[nonOptimizedOffset + n - 1];
        while (n > 2
                && nonOptimizedContour[nonOptimizedOffset + n - 4] == lastX
                && nonOptimizedContour[nonOptimizedOffset + n - 3] == lastY) {
            n -= 2;
        }
        return n;
    }

    private static long findFirstNotIdenticalPoint(IntArray nonOptimizedContour, long n, int x0, int y0) {
        assert n >= 4;
        long i = 2;
        while (i < n
                && nonOptimizedContour.getInt(i) == x0
                && nonOptimizedContour.getInt(i + 1) == y0) {
            i += 2;
        }
        if (i == n) {
            throw new AssertionError("removeLastIdenticalPoints didn't work properly");
        }
        return i;
    }

    private static int findFirstNotIdenticalPoint(
            int[] nonOptimizedContour,
            int nonOptimizedOffset,
            int n,
            int x0,
            int y0) {
        assert n >= 4;
        final int to = nonOptimizedOffset + n;
        int offset = nonOptimizedOffset + 2;
        while (offset < to
                && nonOptimizedContour[offset] == x0
                && nonOptimizedContour[offset + 1] == y0) {
            offset += 2;
        }
        if (offset == to) {
            throw new AssertionError("removeLastIdenticalPoints didn't work properly");
        }
        return offset;
    }

    private static void correctPackedContourWithCollinearFirstAndLastSegments(
            MutableIntArray resultContour,
            int x0,
            int y0,
            int dx0,
            int dy0) {
        assert dx0 != 0 || dy0 != 0 : "findFirstNotIdenticalPoint didn't work properly";
        final long length = resultContour.length();
        if (length > 4) {
            final int previousX = resultContour.getInt(length - 4);
            final int previousY = resultContour.getInt(length - 3);
            final int x = resultContour.getInt(length - 2);
            final int y = resultContour.getInt(length - 1);
            int dx = x0 - x;
            int dy = y0 - y;
            if (collinear32AndCodirectional(x - previousX, y - previousY, dx, dy)) {
                resultContour.length(length - 2);
                // - just removing LAST point (in particular, when dx=dy=0)
                dx = x - previousX;
                dy = y - previousY;
                assert dx != 0 || dy != 0 : "packContour main loop didn't work properly";
            }
            if (collinear32AndCodirectional(dx, dy, dx0, dy0)) {
                Arrays.removeRange(resultContour, 0, 2);
                // - removing FIRST point (it is little faster to remove last point,
                // but it will lead to unpacking to cyclic-shifted contour)
            }
        }
    }

    private static int correctPackedContourWithCollinearFirstAndLastSegments(
            int[] resultContour,
            int x0,
            int y0,
            int dx0,
            int dy0,
            int length) {
        assert dx0 != 0 || dy0 != 0 : "findFirstNotIdenticalPoint didn't work properly";
        if (length > 4) {
            final int previousX = resultContour[length - 4];
            final int previousY = resultContour[length - 3];
            final int x = resultContour[length - 2];
            final int y = resultContour[length - 1];
            int dx = x0 - x;
            int dy = y0 - y;
            if (collinear32AndCodirectional(x - previousX, y - previousY, dx, dy)) {
                length -= 2;
                // - just removing LAST point
                dx = x - previousX;
                dy = y - previousY;
                assert dx != 0 || dy != 0 : "packContour main loop didn't work properly";
            }
            if (collinear32AndCodirectional(dx, dy, dx0, dy0)) {
                length -= 2;
                System.arraycopy(resultContour, 2, resultContour, 0, length);
                // - removing FIRST point (it is little faster to remove last point,
                // but it will lead to unpacking to cyclic-shifted contour)
            }
        }
        return length;
    }

    private static void increaseArray(int[] array, int offset, int count, int increment) {
        assert count >= 0;
        if (count == 0) {
            return;
        }
        assert offset <= array.length - count;
        IntStream.range(0, (count + 255) >>> 8).parallel().forEach(block -> {
            for (int i = offset + (block << 8), to = (int) Math.min((long) i + 256, offset + count); i < to; i++) {
                array[i] += increment;
            }
        });
    }

    private static void copyAndIncreaseArray(
            int[] array,
            int offset,
            int[] resultArray,
            int resultOffset,
            int count,
            int increment) {
        assert count >= 0;
        if (count == 0) {
            return;
        }
        assert offset <= array.length - count;
        assert resultOffset <= resultArray.length - count;
        IntStream.range(0, (count + 255) >>> 8).parallel().forEach(block -> {
            final int blockOffset = block << 8;
            int j = resultOffset + blockOffset;
            for (int i = offset + blockOffset, to = (int) Math.min((long) i + 256, (long) offset + count);
                 i < to; i++, j++) {
                resultArray[j] = array[i] + increment;
            }
        });
    }

    static double pointInsideContourInformation(
            int[] contour,
            final int offset,
            final int n,
            final double x,
            final double y,
            final boolean surelyUnpacked,
            int pOfSegmentContainingXY) {
        final double degeneratedStatus = checkDegeneratedCases(contour, offset, n, x, y);
        if (!InsideContourStatus.isFurtherProcessingNecessaryStatus(degeneratedStatus)) {
            return degeneratedStatus;
        }
        assert n >= 4 : "too little n=" + n + " was not checked";
        // - important to simplify further code (we have at least 1 segment, at least 2 points)
        final int offsetTo = offset + n;
        final int intY = (int) StrictMath.ceil(y);
        // - ceil is important for comparison with integer pointY and nextY
        final boolean yIsInteger = intY == y;
        int p = skipStartingHorizontal(contour, offset, offsetTo, x, yIsInteger, intY);
        if (p < 0) {
            return p == -1 ?
                    InsideContourStatus.makeHorizontalBoundaryStatus() :
                    InsideContourStatus.makeStrictlyOutsideStatus();
        }
        final int pOrOffsetTo = p == offset ? offsetTo : p;
        int lastX = contour[pOrOffsetTo - 2];
        int lastY = contour[pOrOffsetTo - 1];
        // Now we are sure, that p-2 -- p is not a horizontal segment at the specified y,
        // moreover, point p is not at this horizontal
        int countLess = 0;
        int countGreater = 0;
        int countContaining = 0;
        double maxIntersectionLessX = Double.NEGATIVE_INFINITY;
        double minIntersectionGreaterX = Double.POSITIVE_INFINITY;
        final int start = p;
        int count = n >> 1;
        do {
            //noinspection AssertWithSideEffects
            assert --count >= 0 : "infinite loop at position " + start;
            // Now we will scan a half-open segment ]P', P] , where P' is (lastX,lastY), P is contour[p/p+1].
            assert p < offsetTo;
            if (surelyUnpacked) {
                // We need >= |lastY - intY| steps until lastY - every step has length 1 (horizontal or vertical)
                final int increment = incrementForLongJump(Math.abs(lastY - intY), p, offsetTo, start);
                if (increment > 0) {
                    p += increment;
                    lastX = contour[p - 2];
                    lastY = contour[p - 1];
                }
            }

            int pointY = contour[p + 1];
            int pointX = contour[p];
            if (yIsInteger && pointY == intY) {
                // - in other words, if pointY == y, but without floating-point operations
                assert surelyUnpacked || lastY != intY :
                        "lastY=intY=" + intY + " is possible only in the beginning, but here cannot be pointY=intY";
                // - but in surelyUnpacked mode we should not assert:
                // lastY=intY is possible if the contour is not actually unpacked
                int minX = pointX;
                int maxX = pointX;
                int q = p + 2;
                if (q == offsetTo) {
                    q = offset;
                }
                // - attention: we will return p - 2 (not INCREASED p)
                while (contour[q + 1] == intY) {
                    assert q != p : "infinite loop at position " + p;
                    // - skipStartingHorizontal provides a guarantee that the contour contains points with y != intY
                    int newX = contour[q];
                    if (newX < minX) {
                        minX = newX;
                    }
                    if (newX > maxX) {
                        maxX = newX;
                    }
                    q += 2;
                    if (q == offsetTo) {
                        q = offset;
                    }
                }
                final boolean checkedXYIsLeftFromHorizontalSegment = x < minX;
                final boolean intersectionContainsX = !checkedXYIsLeftFromHorizontalSegment && x <= maxX;
                p = (q == offset ? offsetTo : q) - 2;
                // - now we are sure that all horizontal ranges, scanned before, do not contain our x;
                // new "p" is the second end of this horizontal range
                pointX = contour[p];
                assert contour[p + 1] == pointY;
                // - correcting (pointX,pointY): shifting it to the second end of this horizontal range;
                // it will be important at the next iteration of this loop
                final int nextY = contour[p + 2 == offsetTo ? offset + 1 : p + 3];
                assert nextY != intY : "the loop above didn't skip position " + p;
                final boolean contourTouchesHorizontal = (lastY < pointY) != (pointY < nextY);
                if (intersectionContainsX) {
                    // Note: it is zero-width intersection (we have checked non-zero segment above)
                    if (minX != maxX) {
                        // If our point lies inside some NON-ZERO horizontal segment, we have not enough place
                        // to return all useful information about intersection
                        // (minX..maxX + nearest other intersection), and we just return a code "horizontal boundary"
                        return InsideContourStatus.makeHorizontalBoundaryStatus();
                    }
                    countContaining++;
                    if (contourTouchesHorizontal) {
                        countContaining++;
                        // - necessary to distinguish this situation from OUTSIDE status
                    }
                } else if (checkedXYIsLeftFromHorizontalSegment) {
                    if (!contourTouchesHorizontal) {
                        // - if touches, it means +=2, but we will check only lowest bit of this counter
                        countGreater++;
                    }
                    if (minX < minIntersectionGreaterX) {
                        minIntersectionGreaterX = minX;
                    }
                } else {
                    if (!contourTouchesHorizontal) {
                        // - if touches, it means +=2, but we will check only lowest bit of this counter
                        countLess++;
                    }
                    if (maxX > maxIntersectionLessX) {
                        maxIntersectionLessX = maxX;
                    }
                }
            } else if (pointY != lastY) {
                // - we don't need to do anything for horizontal segment, if it is not at our y
                if (p == pOfSegmentContainingXY) {
                    countContaining++;
                    if (DEBUG_MODE) {
                        assert lastY < pointY ? lastY < y && y < pointY : pointY < y && y < lastY;
                        assert x == intersectionX(y, lastX, lastY, pointX, pointY);
                    }
                } else {
                    if (lastY < pointY ? lastY < y && y < pointY : pointY < y && y < lastY) {
                        // - the segment intersects the horizontal y. Note:
                        // 1) y == pointY is impossible (it was checked above), so, we have replaced <= with simple <
                        // 2) y == lastY is POSSIBLE, but should not be checked here, so we must also use simple <
                        final double intersectionX = intersectionX(y, lastX, lastY, pointX, pointY);
                        assert InsideContourStatus.permittedCoordinate(intersectionX);
                        if (intersectionX == x) {
                            // - note: instead of this check, we could work with oriented area
                            // (by multiplying with segmentDy);
                            // but such optimization has no sense, because, in any case, we must find
                            // maxIntersectionLessX..minIntersectionGreaterX and must use intersectionX for comparison
                            // with x to provide a guarantee maxIntersectionLessX <= minIntersectionGreaterX
                            countContaining++;
                        } else if (x < intersectionX) {
                            countGreater++;
                            if (intersectionX < minIntersectionGreaterX) {
                                minIntersectionGreaterX = intersectionX;
                            }
                        } else {
                            countLess++;
                            if (intersectionX > maxIntersectionLessX) {
                                maxIntersectionLessX = intersectionX;
                            }
                        }
                    }
                }
            }
            lastX = pointX;
            lastY = pointY;
            p += 2;
            if (p == offsetTo) {
                p = offset;
            }
        } while (p != start);
        // - note that we cannot slip start position inside skipHorizontalContourCyclicPart,
        // because start point does not lie at our horizontal
        if (((countLess + countContaining + countGreater) & 1) != 0) {
            final String message = "Imbalance of left/right/containing counters ("
                    + countLess + ", " + countGreater + ", " + countContaining + ") at point (" + x + ", " + y + ")";
            if (surelyUnpacked) {
                throw new IllegalStateException(message
                        + "; maybe, the contour is not actually unpacked, though surelyUnpacked argument is true");
            } else {
                throw new AssertionError(message);
            }
        }
        if (maxIntersectionLessX >= minIntersectionGreaterX) {
            throw new AssertionError("Invalid maxIntersectionLessX/minIntersectionGreaterX: "
                    + maxIntersectionLessX + " >= " + minIntersectionGreaterX);
        }
        final boolean centerEven = (countContaining & 1) == 0;
        final boolean leftEven = (countLess & 1) != 0;
        if (centerEven) {
            assert leftEven == ((countGreater & 1) != 0);
            if (leftEven) {
                return InsideContourStatus.makeStrictlyInsideStatus(maxIntersectionLessX, minIntersectionGreaterX);
            } else {
                return countContaining == 0 ?
                        InsideContourStatus.makeStrictlyOutsideStatus() :
                        InsideContourStatus.makeDegeneratedLeftRightBoundaryStatus(x);
            }
        } else {
            if (leftEven) {
                return InsideContourStatus.makeRightBoundaryStatus(maxIntersectionLessX);
            } else {
                return InsideContourStatus.makeLeftBoundaryStatus(minIntersectionGreaterX);
            }
        }
    }

    private static double checkDegeneratedCases(int[] contour, int offset, int n, double x, double y) {
        checkContourLengthAndOffset(contour, offset, n);
        assert (n & 1) == 0;
        assert n != 0 : "zero length cannot be accepted by checkContourLength()";
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE
                || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
            return InsideContourStatus.makeStrictlyOutsideStatus();
        }
        if (n == 2) {
            return x == contour[offset] && y == contour[offset + 1] ?
                    InsideContourStatus.makeHorizontalBoundaryStatus() :
                    InsideContourStatus.makeStrictlyOutsideStatus();
        }
        return InsideContourStatus.makeFurtherProcessingNecessaryStatus();
    }

    private static int skipStartingHorizontal(
            int[] contour,
            int offset,
            int offsetTo,
            double x,
            boolean yIsInteger,
            int intY) {
        assert offset >= 0 && offsetTo > offset + 2;
        if (yIsInteger && contour[offset + 1] == intY) {
            int minX = contour[offset];
            int maxX = minX;
            for (int p = offset + 2; p < offsetTo; p += 2) {
                int pointY = contour[p + 1];
                if (pointY != intY) {
                    // - We may start from ANY point (our loop below is cyclic), and we prefer
                    // to start from a point, not belonging to the checked horizontal line
                    return p;
                }
                int pointX = contour[p];
                if (pointX < minX) {
                    minX = pointX;
                }
                if (pointX > maxX) {
                    maxX = pointX;
                }
            }
            final boolean horitontalBoundary = minX <= x && x <= maxX;
            // - All points are at the same line y=intY.
            // Important: such simple check is possible due to n >= 4 (it would be incorrect if n==2, from==to).
            return horitontalBoundary ? -1 : -2;
        }
        return offset;
    }

    private static int incrementForLongJump(int distance, int p, int offsetTo, int start) {
        if (distance > 5) {
            // - try to make long jump; no sense to optimize for very little distance;
            distance--;
            // - we must not cross the line y, that can little differ from intY
            final int limit = (start > p ? start : offsetTo) - (p + 2);
            assert limit >= 0;
            return Math.min(2 * distance, limit);
            // - reduce distance to not cross the values offsetTo / start, important for final checks in loop
        } else {
            return 0;
        }
    }

    private static double intersectionX(double y, int x1, int y1, int x2, int y2) {
        final double segmentDx = (double) ((long) x2 - (long) x1);
        final double segmentDy = (double) ((long) y2 - (long) y1);
        // (long) is necessary for a case of very large values, like Integer.MIN_VALUE/MAX_VALUE
        return (double) x1 + ((y - (double) y1) / segmentDy) * segmentDx;
        // - note: for integer, half-integer and many other points it is calculated exactly or almost exactly;
        // for example, if y = 0.5*(y1+y2) (exact value), then ((y - (double) y1) / segmentDy) will be exact 0.5
    }

    // Note: this function NOT ALWAYS returns true when one of 2 vectors is zero!
    // Please do not call it with zero dx1=dy1=0 or dx2=dy2=0
    private static boolean collinear32AndCodirectional(int dx1, int dy1, int dx2, int dy2) {
        // Usually one of dx/dy = 0, and other != 0, but it is faster to use multiplication to check this
        return (long) dx1 * (long) dy2 == (long) dy1 * (long) dx2
                && (dx1 ^ dx2) >= 0 && (dy1 ^ dy2) >= 0;
        // (a ^ b) >= 0 means (a < 0) == (b < 0)
    }

    static void checkPoint(long x, long y) {
        if ((((x - MIN_ABSOLUTE_COORDINATE) | (y - MIN_ABSOLUTE_COORDINATE))
                & MAX_ABSOLUTE_COORDINATE_HIGH_BITS) != 0) {
            // - unsigned difference x-MIN_ABSOLUTE_COORDINATE will be in range 0..0x000000007FFFFFFFL
            // if and only if MIN_ABSOLUTE_COORDINATE<=x<=MAX_ABSOLUTE_COORDINATE;
            // MAX_ABSOLUTE_COORDINATE_HIGH_BITS consists of all other bits
            throw new IllegalArgumentException("Point coordinates (" + x + ", " + y + ") are out of allowed range "
                    + (MIN_ABSOLUTE_COORDINATE) + ".." + MAX_ABSOLUTE_COORDINATE);
        }
    }

    // Note: it is better to have separate function for more typical case if int arguments (in addition to long)
    private static void checkPoint(int x, int y) throws IllegalArgumentException {
        if (((x - MIN_ABSOLUTE_COORDINATE) | (y - MIN_ABSOLUTE_COORDINATE)) < 0) {
            // - unsigned difference x-MIN_ABSOLUTE_COORDINATE will be in range 0..0x7FFFFFFF
            // if and only if MIN_ABSOLUTE_COORDINATE<=x<=MAX_ABSOLUTE_COORDINATE
            throw new IllegalArgumentException("Point coordinates (" + x + ", " + y + ") are out of allowed range "
                    + MIN_ABSOLUTE_COORDINATE + ".." + MAX_ABSOLUTE_COORDINATE);
        }
    }

    private static void checkContourLength(IntArray contour) {
        Objects.requireNonNull(contour, "Null contour");
        checkContourLength(contour.length());
    }

    private static void checkAbilityToAddToContour(MutableIntArray contour) {
        if (contour.length() >> 1 >= MAX_CONTOUR_NUMBER_OF_POINTS) {
            throw new IllegalArgumentException("Too large number of points in a contour: it is > "
                    + MAX_CONTOUR_NUMBER_OF_POINTS);
        }
    }

    private static void checkAbilityToAddToContour(MutableIntArray contour, int numberOfAddedPoints) {
        if (contour.length() >> 1 > MAX_CONTOUR_NUMBER_OF_POINTS - numberOfAddedPoints) {
            throw new IllegalArgumentException("Too large number of points in a contour: it is > "
                    + MAX_CONTOUR_NUMBER_OF_POINTS);
        }
    }

    private static int[] ensureCapacityForContour(int[] points, long newLength) {
        if (newLength <= points.length) {
            return points;
        }
        if (newLength >> 1 >= MAX_CONTOUR_NUMBER_OF_POINTS) {
            throw new IllegalArgumentException("Too large number of points in a contour: it is > "
                    + MAX_CONTOUR_NUMBER_OF_POINTS);
        }
        return java.util.Arrays.copyOf(points, Math.max((int) newLength,
                (int) Math.min(Integer.MAX_VALUE, (long) (2.0 * points.length))));
    }

    private static boolean isReserved(int x) {
        return (x & 0xFF000000) == ContourHeader.RESERVED;
    }

    private static long packLowAndHigh(int low, int high) {
        return ((long) high << 32) | ((long) low & 0xFFFFFFFFL);
    }

    private static class MutableInt {
        private int value;
    }

//    public static void main(String[] args) {
//        checkPoint(-1, -1);
//        long x = MAX_ABSOLUTE_COORDINATE - 1;
//        long y = MIN_ABSOLUTE_COORDINATE;
//        checkPoint(x, y);
//        System.out.println(x + " ok");
//        x++;
//        checkPoint(x, -1);
//        System.out.println(x + " ok");
//        x--;
//        checkPoint(x, 1000000000000L);
//        System.out.println(x + " ok");
//        x -= MAX_ABSOLUTE_COORDINATE;
//        checkPoint(x, 12);
//        System.out.println(x + " ok");
//    }
}
