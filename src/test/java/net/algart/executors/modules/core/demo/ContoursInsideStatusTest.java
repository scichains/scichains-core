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

package net.algart.executors.modules.core.demo;

import net.algart.arrays.Matrices;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.contexts.InterruptionException;
import net.algart.contours.ContourHeader;
import net.algart.contours.ContourNestingAnalyser;
import net.algart.contours.Contours;
import net.algart.contours.InsideContourStatus;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;

import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public final class ContoursInsideStatusTest extends Executor {
    public static final String INPUT_BACKGROUND = "background";
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_STATUS = "status";
    public static final String OUTPUT_POINT_INFORMATION = "point_information";
    public static final String OUTPUT_ROUNDED_POINT_INFORMATION = "rounded_point_information";

    private boolean processAllPixels = false;
    private boolean unpackContours = false;
    private boolean simpleCheckOfAllContours = false;
    private double x = 0.0;
    private double y = 0.0;
    private int startX = 0;
    private int startY = 0;
    private int sizeX = 256;
    private int sizeY = 256;
    private double scale = 1.0;
    private boolean multiplySizesByScale = false;
    private float horizontalBoundaryCode = 1000.5f;
    private float boundaryIncrement = 500.5f;
    private boolean findRepresentatives = true;
    private float insideRepresentativeCode = 800.0f;
    private float boundaryRepresentativeCode = 900.0f;

    public ContoursInsideStatusTest() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        addInputMat(INPUT_BACKGROUND);
        setDefaultOutputMat(OUTPUT_STATUS);
        addOutputScalar(OUTPUT_POINT_INFORMATION);
        addOutputScalar(OUTPUT_ROUNDED_POINT_INFORMATION);
    }

    public boolean isProcessAllPixels() {
        return processAllPixels;
    }

    public ContoursInsideStatusTest setProcessAllPixels(boolean processAllPixels) {
        this.processAllPixels = processAllPixels;
        return this;
    }

    public boolean isUnpackContours() {
        return unpackContours;
    }

    public ContoursInsideStatusTest setUnpackContours(boolean unpackContours) {
        this.unpackContours = unpackContours;
        return this;
    }

    public boolean isSimpleCheckOfAllContours() {
        return simpleCheckOfAllContours;
    }

    public ContoursInsideStatusTest setSimpleCheckOfAllContours(boolean simpleCheckOfAllContours) {
        this.simpleCheckOfAllContours = simpleCheckOfAllContours;
        return this;
    }

    public double getX() {
        return x;
    }

    public ContoursInsideStatusTest setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public ContoursInsideStatusTest setY(double y) {
        this.y = y;
        return this;
    }

    public int getStartX() {
        return startX;
    }

    public ContoursInsideStatusTest setStartX(int startX) {
        this.startX = startX;
        return this;
    }

    public int getStartY() {
        return startY;
    }

    public ContoursInsideStatusTest setStartY(int startY) {
        this.startY = startY;
        return this;
    }

    public int getSizeX() {
        return sizeX;
    }

    public ContoursInsideStatusTest setSizeX(int sizeX) {
        this.sizeX = positive(sizeX);
        return this;
    }

    public int getSizeY() {
        return sizeY;
    }

    public ContoursInsideStatusTest setSizeY(int sizeY) {
        this.sizeY = positive(sizeY);
        return this;
    }

    public double getScale() {
        return scale;
    }

    public ContoursInsideStatusTest setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public boolean isMultiplySizesByScale() {
        return multiplySizesByScale;
    }

    public ContoursInsideStatusTest setMultiplySizesByScale(boolean multiplySizesByScale) {
        this.multiplySizesByScale = multiplySizesByScale;
        return this;
    }

    public float getHorizontalBoundaryCode() {
        return horizontalBoundaryCode;
    }

    public ContoursInsideStatusTest setHorizontalBoundaryCode(float horizontalBoundaryCode) {
        this.horizontalBoundaryCode = horizontalBoundaryCode;
        return this;
    }

    public float getBoundaryIncrement() {
        return boundaryIncrement;
    }

    public ContoursInsideStatusTest setBoundaryIncrement(float boundaryIncrement) {
        this.boundaryIncrement = boundaryIncrement;
        return this;
    }

    public boolean isFindRepresentatives() {
        return findRepresentatives;
    }

    public ContoursInsideStatusTest setFindRepresentatives(boolean findRepresentatives) {
        this.findRepresentatives = findRepresentatives;
        return this;
    }

    public float getInsideRepresentativeCode() {
        return insideRepresentativeCode;
    }

    public ContoursInsideStatusTest setInsideRepresentativeCode(float insideRepresentativeCode) {
        this.insideRepresentativeCode = insideRepresentativeCode;
        return this;
    }

    public float getBoundaryRepresentativeCode() {
        return boundaryRepresentativeCode;
    }

    public ContoursInsideStatusTest setBoundaryRepresentativeCode(float boundaryRepresentativeCode) {
        this.boundaryRepresentativeCode = boundaryRepresentativeCode;
        return this;
    }

    @Override
    public void process() {
        final SNumbers inputContours = getInputNumbers(INPUT_CONTOURS);
        final Contours sourceContours = Contours.deserialize(inputContours.toIntArrayOrReference());
        final Contours contours = unpackContours ? sourceContours.unpackContours(true) : sourceContours;
        final double scaleInv = 1.0 / scale;
        final ContourNestingAnalyser analyser = ContourNestingAnalyser.newInstance(
                contours, unpackContours, null);
        getScalar(OUTPUT_POINT_INFORMATION).setTo(testPointDetailed(
                analyser, (x + startX) * scaleInv, (y + startY) * scaleInv));
        getScalar(OUTPUT_ROUNDED_POINT_INFORMATION).setTo(testPointDetailed(
                analyser, (Math.rint(x) + startX) * scaleInv, (Math.rint(y) + startY) * scaleInv));
        if (processAllPixels) {
            long t1 = debugTime();
            final int sizeX = multiplySizesByScale ? (int) Math.round(this.sizeX * scale) : this.sizeX;
            final int sizeY = multiplySizesByScale ? (int) Math.round(this.sizeY * scale) : this.sizeY;
            final float[] infoMap = new float[sizeX * sizeY];
            IntStream.range(0, sizeY).parallel().forEach(i -> {
                final PointTester tester = new PointTester(contours);
                for (int j = 0, disp = i * sizeX; j < sizeX; j++, disp++) {
                    double x = (j + startX) * scaleInv;
                    double y = (i + startY) * scaleInv;
                    if (simpleCheckOfAllContours) {
                        infoMap[disp] = testPoint(contours, x, y);
                    } else {
                        tester.initialize(x, y);
                        analyser.findRectanglesContainingPoint(x, y, tester);
                        infoMap[disp] = tester.result();
                    }
                }
                if (isInterrupted()) {
                    throw new InterruptionException("Processing pixels was interrupted");
                }
            });
            long t2 = debugTime();
            if (findRepresentatives) {
                Point2D point = new Point2D.Double();
                for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
                    final boolean found = contours.findSomePointInside(point, k, unpackContours);
                    float code = found ? insideRepresentativeCode : boundaryRepresentativeCode;
                    final int j = (int) (point.getX() * scale - startX);
                    final int i = (int) (point.getY() * scale - startY);
                    if (j >= 0 && j < sizeX && i >= 0 && i < sizeY) {
                        infoMap[i * sizeX + j] = code;
                    }
                }
            }
            long t3 = debugTime();
            final MultiMatrix2D result = MultiMatrix.valueOf2DMono(Matrices.matrix(
                    SimpleMemoryModel.asUpdatableFloatArray(infoMap), sizeX, sizeY));
            logDebug(() -> String.format(Locale.US,
                    "Checking %d contours in %.3f ms, %.5f mcs/pixel; finding representatives %.3f ms",
                    contours.numberOfContours(),
                    (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / infoMap.length, (t3 - t2) * 1e-6));
            getMat().setTo(result);
            if (findRepresentatives) {
                Point2D point = new Point2D.Double();
                for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
                    final boolean found = contours.findSomePointInside(point, k, unpackContours);
                    final double information = contours.pointInsideContourInformation(
                            k, point.getX(), point.getY(), unpackContours);
                    final int j = (int) (point.getX() * scale - startX);
                    final int i = (int) (point.getY() * scale - startY);
                    if (found) {
                        if (!InsideContourStatus.isStrictlyInside(information))
                            throw new AssertionError("Failure of finding inside point in the contour #"
                                    + k + ": " + point + " (" + j + ", " + i
                                    + " at the image) is not inside, its status is "
                                    + InsideContourStatus.valueOfInformation(information)
                                    + " (information: " + information + ")");
                    } else {
                        if (point.getX() != contours.getContourPointX(k, 0)
                                || point.getY() != contours.getContourPointY(k, 0)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }
    }

    private float testPoint(Contours contours, double x, double y) {
        double minWidth = Double.POSITIVE_INFINITY;
        boolean boundary = false;
        final ContourHeader header = new ContourHeader();
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            contours.getHeader(header, k);
            if (header.minX() > x || x > header.maxX() || header.minY() > y || y > header.maxY()) {
                continue;
            }
            final double info = contours.pointInsideContourInformation(k, x, y, unpackContours);
            final InsideContourStatus status = InsideContourStatus.valueOfInformation(info);
            if (status.isBoundary()) {
                boundary = true;
                if (InsideContourStatus.isHorizontalBoundary(info)) {
                    return horizontalBoundaryCode;
                } else {
                    final double width = Math.abs(InsideContourStatus.getBoundarySectionSecondEnd(info) - x);
                    if (width < minWidth) {
                        minWidth = width;
                    }
                }
            } else if (status.isStrictlyInside()) {
                final double width = InsideContourStatus.getInsideSectionWidth(info);
                if (width < minWidth) {
                    minWidth = width;
                }
            }
        }
        return minWidth == Double.POSITIVE_INFINITY ? -1.0f :
                boundary ? (float) minWidth + boundaryIncrement : (float) minWidth;
    }

    private static String testPointDetailed(ContourNestingAnalyser analyser, double x, double y) {
        final StringBuilder sb = new StringBuilder();
        final Contours contours = analyser.contours();
        sb.append("Contours, containing point (").append(x).append(", ").append(y).append("):");
        analyser.findContoursContainingInside(x, y, index -> sb.append(" ").append(index));
        sb.append("\nMinimal containing contour: ");
        final ContourNestingAnalyser.NestingInformation nesting = analyser.analysePoint(x, y);
        sb.append(nesting.getNestingParent());
        sb.append(", nesting level: ");
        sb.append(nesting.getNestingLevel());
        sb.append("\n");
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            final double info = contours.pointInsideContourInformation(k, x, y, false);
            final InsideContourStatus status = InsideContourStatus.valueOfInformation(info);
            if (status.isBoundary()) {
                sb.append("boundary of contour #").append(k);
                if (InsideContourStatus.isHorizontalBoundary(info)) {
                    sb.append(", horizontal part");
                } else if (InsideContourStatus.isLeftBoundary(info)) {
                    sb.append(", left point, the right end is ")
                            .append(InsideContourStatus.getBoundarySectionSecondEnd(info));
                } else if (InsideContourStatus.isRightBoundary(info)) {
                    sb.append(", degenerated (both left and right)");
                    final double storedX = InsideContourStatus.getBoundarySectionSecondEnd(info);
                    if (storedX != x) {
                        throw new AssertionError("Invalid stored X = " + storedX);
                    }
                } else {
                    throw new AssertionError("Unknown boundary code " + info);
                }
                sb.append("\n");
                contours.pointInsideContourInformation(k, x, y, false);
                // - for debugging
            } else if (status.isStrictlyInside()) {
                sb.append("inside contour #").append(k).append(", section width ")
                        .append(InsideContourStatus.getInsideSectionWidth(info)).append("\n");
            }
        }
        return sb.length() == 0 ? "outside all" : sb.toString();
    }

    private class PointTester implements IntConsumer {
        final Contours contours;
        private double x;
        private double y;
        private double minWidth;
        boolean boundary;
        boolean horizontalBoundary;

        PointTester(Contours contours) {
            this.contours = contours;
        }

        void initialize(double x, double y) {
            this.x = x;
            this.y = y;
            minWidth = Double.POSITIVE_INFINITY;
            boundary = false;
            horizontalBoundary = false;
        }

        @Override
        public void accept(int index) {
            final double info = contours.pointInsideContourInformation(index, x, y, unpackContours);
            final InsideContourStatus status = InsideContourStatus.valueOfInformation(info);
            if (status.isBoundary()) {
                boundary = true;
                if (InsideContourStatus.isHorizontalBoundary(info)) {
                    horizontalBoundary = true;
                } else {
                    final double width = Math.abs(InsideContourStatus.getBoundarySectionSecondEnd(info) - x);
                    if (width < minWidth) {
                        minWidth = width;
                    }
                }
            } else if (status.isStrictlyInside()) {
                final double width = InsideContourStatus.getInsideSectionWidth(info);
                if (width < minWidth) {
                    minWidth = width;
                }
            }
        }

        public float result() {
            return horizontalBoundary ? horizontalBoundaryCode :
                    minWidth == Double.POSITIVE_INFINITY ? -1.0f :
                            boundary ? (float) minWidth + boundaryIncrement : (float) minWidth;
        }
    }
}
