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

import java.util.function.DoublePredicate;

public enum InsideContourStatus {
    /**
     * Indicates that some point or object is a subset of the figure, lying strictly inside some contour,
     * and does not contain points of this contour itself (i.e. at the boundary of this figure).
     */
    INSIDE(information -> information > 0.0),
    /**
     * Indicates that some point or set of points a subset of the contour (all points lies at some segments
     * of this contour).
     */
    BOUNDARY(information -> information < 0.0),
    /**
     * Indicates that some point or object has no common points with the figure, lying inside some contour,
     * and also with the contour  itself (i.e. with the boundary of this figure).
     */
    OUTSIDE(Double::isNaN);

    private final static double MAX_COORDINATE = 0x100000000L;
    private final static double MIN_COORDINATE = -MAX_COORDINATE;
    // - actually it would be enough to use Integer.MIN_VALUE/MAX_VALUE+1, but we prefer larger value 2^32:
    // it may be processed faster and it is surely enough even while rounding errors
    private final static double COORDINATE_SHIFT_1 = MAX_COORDINATE;
    private final static double COORDINATE_THRESHOLD_1 = -2.0 * MAX_COORDINATE;
    // x - COORDINATE_SHIFT_1 surely > COORDINATE_THRESHOLD_1, etc.
    private final static double COORDINATE_SHIFT_2 = 3.0 * COORDINATE_SHIFT_1;
    private final static double COORDINATE_THRESHOLD_2 = -4.0 * MAX_COORDINATE;
    private final static double COORDINATE_SHIFT_3 = 5.0 * COORDINATE_SHIFT_1;
    private final static double COORDINATE_THRESHOLD_3 = -6.0 * MAX_COORDINATE;
    private final static double FURTHER_PROCESSING_NECESSARY_CODE = -8.0 * COORDINATE_SHIFT_1;

    final DoublePredicate statusChecker;

    InsideContourStatus(DoublePredicate statusChecker) {
        this.statusChecker = statusChecker;
    }

    public boolean isStrictlyInside() {
        return this == INSIDE;
    }

    public boolean isBoundary() {
        return this == BOUNDARY;
    }

    public boolean isStrictlyOutside() {
        return this == OUTSIDE;
    }

    public boolean matchesStatus(double information) {
        return statusChecker.test(information);
    }

    public static boolean isStrictlyInside(double information) {
        return INSIDE.matchesStatus(information);
    }

    public static boolean isBoundary(double information) {
        return BOUNDARY.matchesStatus(information);
    }

    public static boolean isStrictlyOutside(double information) {
        return OUTSIDE.matchesStatus(information);
    }

    public static double getInsideSectionWidth(double insideStatus) {
        if (!INSIDE.matchesStatus(insideStatus)) {
            throw new IllegalArgumentException("Information does not correspond to inside status: " + insideStatus);
        }
        return insideStatus;
    }

    public static boolean isHorizontalBoundary(double boundaryStatus) {
        return boundaryStatus == Double.NEGATIVE_INFINITY;
    }

    public static boolean isLeftBoundary(double boundaryStatus) {
        return isBoundary(boundaryStatus) && boundaryStatus > COORDINATE_THRESHOLD_1;
    }

    public static boolean isRightBoundary(double boundaryStatus) {
        return boundaryStatus <= COORDINATE_THRESHOLD_1 && boundaryStatus > COORDINATE_THRESHOLD_2;
    }

    public static boolean isNormalLeftRightBoundary(double boundaryStatus) {
        return isBoundary(boundaryStatus) && boundaryStatus > COORDINATE_THRESHOLD_2;
    }

    public static boolean isDegeneratedLeftRightBoundary(double boundaryStatus) {
        return boundaryStatus <= COORDINATE_THRESHOLD_2 && boundaryStatus > COORDINATE_THRESHOLD_3;
    }

    public static double getBoundarySectionSecondEnd(double boundaryStatus) {
        checkBoundary(boundaryStatus);
        if (boundaryStatus > COORDINATE_THRESHOLD_1) {
            return boundaryStatus + COORDINATE_SHIFT_1;
        }
        if (boundaryStatus > COORDINATE_THRESHOLD_2) {
            return boundaryStatus + COORDINATE_SHIFT_2;
        }
        if (boundaryStatus > COORDINATE_THRESHOLD_3) {
            return boundaryStatus + COORDINATE_SHIFT_3;
        }
        if (!isHorizontalBoundary(boundaryStatus)) {
            throw new IllegalArgumentException("Information corresponds to a horizontal section of the boundary; "
                + "coordinate of the second section end is unavailable in this case");
        }
        throw new IllegalArgumentException("Unallowable boundary information " + boundaryStatus
                + " - it is not a correct point status");
    }

    public static InsideContourStatus valueOfInformation(double information) {
        for (InsideContourStatus status : values()) {
            if (status.matchesStatus(information)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Illegal packed information " + information
                + ": it does not match to any status");
    }

    static double makeStrictlyInsideStatus(double minX, double maxX) {
        final double result = maxX - minX;
        assert result > 0.0;
        return result;
    }

    static double makeHorizontalBoundaryStatus() {
        return Double.NEGATIVE_INFINITY;
    }

    static double makeLeftBoundaryStatus(double rightX) {
        assert permittedCoordinate(rightX);
        return rightX - COORDINATE_SHIFT_1;
    }

    static double makeRightBoundaryStatus(double leftX) {
        assert permittedCoordinate(leftX);
        return leftX - COORDINATE_SHIFT_2;
    }

    static double makeDegeneratedLeftRightBoundaryStatus(double x) {
        assert permittedCoordinate(x);
        return x - COORDINATE_SHIFT_3;
    }

    static double makeStrictlyOutsideStatus() {
        return Double.NaN;
    }

    // - for internal usage only
    static boolean isFurtherProcessingNecessaryStatus(double information) {
        return information == FURTHER_PROCESSING_NECESSARY_CODE;
    }

    static double makeFurtherProcessingNecessaryStatus() {
        return FURTHER_PROCESSING_NECESSARY_CODE;
    }

    static boolean permittedCoordinate(double x) {
        return x > MIN_COORDINATE && x < MAX_COORDINATE;
    }

    private static void checkBoundary(double boundaryStatus) {
        if (!BOUNDARY.matchesStatus(boundaryStatus)) {
            throw new IllegalArgumentException("Information does not correspond to boundary: " + boundaryStatus);
        }
    }
}
