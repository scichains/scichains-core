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

import net.algart.math.IRectangularArea;

public final class ContourHeader {
    /**
     * If <code>(x &amp; 0xFF000000) == {@link #RESERVED}</code>, it means that it is a special element, not a point.
     */
    @SuppressWarnings("JavadocDeclaration")
    public static final int RESERVED = 0x7F000000;

    static final int MAX_ALLOWED_HEADER_LENGTH = 512;
    static final int HEADER_LENGTH_WITHOUT_FRAME_ID = 8;
    static final int HEADER_LENGTH_WITH_FRAME_ID = HEADER_LENGTH_WITHOUT_FRAME_ID + 2;
    static final int MIN_HEADER_LENGTH = HEADER_LENGTH_WITHOUT_FRAME_ID;
    static final int CONTAINING_RECTANGLE_OFFSET = 2;
    static final int FLAGS_OFFSET = 6;
    static final int LABEL_OFFSET = 7;
    static final int FRAME_ID_OFFSET = 9;
    static final int HEADER_LENGTH_MASK = 0x000000FF;
    static final int MAGIC_WORD_MASK = 0xFFFFFF00;
    static final int MAGIC_WORD = RESERVED | 0x00433100;
    // - means "C1": contour, version 1
    static final int MAGIC_WORD_1 = MAGIC_WORD | HEADER_LENGTH_WITHOUT_FRAME_ID;
    static final int MAGIC_WORD_2 = MAGIC_WORD | HEADER_LENGTH_WITH_FRAME_ID;
    static final int STANDARD_RESERVED_INDICATOR = 0x7FFF0000;
    static final int INTERNAL_FLAG = 0x00000001;
    static final int TOUCHES_MIN_X_MATRIX_BOUNDARY_FLAG = 0x00000010;
    static final int TOUCHES_MAX_X_MATRIX_BOUNDARY_FLAG = 0x00000020;
    static final int TOUCHES_MIN_Y_MATRIX_BOUNDARY_FLAG = 0x00000040;
    static final int TOUCHES_MAX_Y_MATRIX_BOUNDARY_FLAG = 0x00000080;
    // - note: we have no special flag for containing rectangle presence, because we never store contours without it
    static final int HAS_FRAME_ID = 0x00000100;

    private int objectLabel = 0;
    // - background by default
    private boolean hasContainingRectangle = false;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;

    {
        removeContainingRectangle();
        // - initializing minX, maxX, minY, maxY
    }

    private Integer frameId = null;

    private boolean internalContour = false;
    private boolean contourTouchingMinXMatrixBoundary = false;
    private boolean contourTouchingMaxXMatrixBoundary = false;
    private boolean contourTouchingMinYMatrixBoundary = false;
    private boolean contourTouchingMaxYMatrixBoundary = false;

    public ContourHeader() {
    }

    public ContourHeader(int objectLabel) {
        this.objectLabel = objectLabel;
    }

    public ContourHeader(int objectLabel, boolean internalContour) {
        this.objectLabel = objectLabel;
        this.internalContour = internalContour;
    }

    public ContourHeader(int objectLabel, boolean internalContour, int frameId) {
        this.objectLabel = objectLabel;
        this.internalContour = internalContour;
        this.frameId = frameId;
    }

    public int getObjectLabel() {
        return objectLabel;
    }

    public ContourHeader setObjectLabel(int objectLabel) {
        this.objectLabel = objectLabel;
        return this;
    }

    public Integer getFrameIdOrNull() {
        return frameId;
    }

    public boolean hasFrameId() {
        return frameId != null;
    }

    public int getFrameId() {
        if (frameId == null) {
            throw new IllegalStateException("No frame ID");
        }
        return frameId;
    }

    public ContourHeader setFrameId(int frameId) {
        this.frameId = frameId;
        return this;
    }

    public ContourHeader removeFrameId() {
        this.frameId = null;
        return this;
    }

    public boolean isInternalContour() {
        return internalContour;
    }

    public ContourHeader setInternalContour(boolean internalContour) {
        this.internalContour = internalContour;
        return this;
    }

    public boolean isContourTouchingMinXMatrixBoundary() {
        return contourTouchingMinXMatrixBoundary;
    }

    public ContourHeader setContourTouchingMinXMatrixBoundary(boolean contourTouchingMinXMatrixBoundary) {
        this.contourTouchingMinXMatrixBoundary = contourTouchingMinXMatrixBoundary;
        return this;
    }

    public boolean isContourTouchingMaxXMatrixBoundary() {
        return contourTouchingMaxXMatrixBoundary;
    }

    public ContourHeader setContourTouchingMaxXMatrixBoundary(boolean contourTouchingMaxXMatrixBoundary) {
        this.contourTouchingMaxXMatrixBoundary = contourTouchingMaxXMatrixBoundary;
        return this;
    }

    public boolean isContourTouchingMinYMatrixBoundary() {
        return contourTouchingMinYMatrixBoundary;
    }

    public ContourHeader setContourTouchingMinYMatrixBoundary(boolean contourTouchingMinYMatrixBoundary) {
        this.contourTouchingMinYMatrixBoundary = contourTouchingMinYMatrixBoundary;
        return this;
    }

    public boolean isContourTouchingMaxYMatrixBoundary() {
        return contourTouchingMaxYMatrixBoundary;
    }

    public ContourHeader setContourTouchingMaxYMatrixBoundary(boolean contourTouchingMaxYMatrixBoundary) {
        this.contourTouchingMaxYMatrixBoundary = contourTouchingMaxYMatrixBoundary;
        return this;
    }

    /**
     * Returns <tt>true</tt> if the header contains information about the rectangle, containing the contour.
     * Note that headers, read from {@link Contours}, always contains this information,
     * but headers, created via constructors, never contain it.
     *
     * @return whether the header has containing rectangle.
     */
    public boolean hasContainingRectangle() {
        return hasContainingRectangle;
    }

    public int minX() {
        checkContainingRectangle();
        return minX;
    }

    public int maxX() {
        checkContainingRectangle();
        return maxX;
    }

    public int minY() {
        checkContainingRectangle();
        return minY;
    }

    public int maxY() {
        checkContainingRectangle();
        return maxY;
    }

    public IRectangularArea containingRectangle() {
        checkContainingRectangle();
        return IRectangularArea.valueOf(minX, minY, maxX, maxY);
    }

    public void removeContainingRectangle() {
        this.hasContainingRectangle = false;
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
    }

    public ContourHeader clear() {
        objectLabel = 0;
        removeContainingRectangle();
        frameId = null;
        internalContour = false;
        clearContourTouchingMatrixBoundary();
        return this;
    }

    public ContourHeader clearContourTouchingMatrixBoundary() {
        this.contourTouchingMinXMatrixBoundary = false;
        this.contourTouchingMaxXMatrixBoundary = false;
        this.contourTouchingMinYMatrixBoundary = false;
        this.contourTouchingMaxYMatrixBoundary = false;
        return this;
    }

    @Override
    public String toString() {
        return "contour header: "
                + "label " + objectLabel
                + (hasFrameId() ? ", frame " + frameId : "")
                + (internalContour ? ", internal" : ", external")
                + (hasContainingRectangle ?
                ", containing rectangle " + minX + ".." + maxX + "x" + minY + ".." + maxY :
                "");
    }

    int headerLength() {
        return frameId == null ? HEADER_LENGTH_WITHOUT_FRAME_ID : HEADER_LENGTH_WITH_FRAME_ID;
    }

    void read(Contours contours, int headerOffset) {
        // Note: attempt to use DirectAccessible here does not increase performance, even slows down!
        this.objectLabel = contours.points[headerOffset + LABEL_OFFSET];
        this.minX = contours.minX(headerOffset);
        this.maxX = contours.maxX(headerOffset);
        this.minY = contours.minY(headerOffset);
        this.maxY = contours.maxY(headerOffset);
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("Illegal header in the contour array "
                    + "at position " + (headerOffset + CONTAINING_RECTANGLE_OFFSET)
                    + ": negative sizes of containing rectangle " + minX + ".." + maxX + "x" + minY + ".." + maxY);
        }
        if (minX < Contours.MIN_ABSOLUTE_COORDINATE || maxX > Contours.MAX_ABSOLUTE_COORDINATE
                || minY < Contours.MIN_ABSOLUTE_COORDINATE || maxY > Contours.MAX_ABSOLUTE_COORDINATE) {
            throw new IllegalArgumentException("Illegal header in the contour array "
                    + "at position " + (headerOffset + CONTAINING_RECTANGLE_OFFSET)
                    + ": containing rectangle " + minX + ".." + maxX + "x" + minY + ".." + maxY
                    + " is outside allowed range"
                    + Contours.MIN_ABSOLUTE_COORDINATE + ".." + Contours.MAX_ABSOLUTE_COORDINATE);
        }
        this.hasContainingRectangle = true;
        final int flags = contours.points[headerOffset + FLAGS_OFFSET];
        setFlags(flags);
        if ((flags & HAS_FRAME_ID) != 0) {
            this.frameId = contours.points[headerOffset + FRAME_ID_OFFSET];
        } else {
            this.frameId = null;
        }
    }

    int write(Contours contours, final int headerOffset, final int contourNumberOfPoints) {
        final int headerLength = headerLength();
        int offset = headerOffset;
        contours.points[offset++] = MAGIC_WORD | headerLength;
        contours.points[offset++] = 2 * contourNumberOfPoints + headerLength;
        // - number of words in contour
        contours.points[offset++] = minX;
        contours.points[offset++] = maxX;
        contours.points[offset++] = minY;
        contours.points[offset++] = maxY;
        // - we store these fields always, even they are incorrect (!hasContainingRectangle);
        // in this case we MUST re-fill these fields after calling this method
        contours.points[offset++] = STANDARD_RESERVED_INDICATOR | getFlags();
        contours.points[offset++] = objectLabel;
        if (frameId != null) {
            contours.points[offset++] = STANDARD_RESERVED_INDICATOR;
            contours.points[offset++] = frameId;
        }
        assert offset - headerOffset == headerLength;
        return offset;
    }

    void appendToEnd(Contours contours, int contourNumberOfPoints) {
        final int position = contours.pointsLength;
        final long newPointsLength = (long) position + (long) headerLength();
        contours.ensureCapacityForPoints(newPointsLength);
        contours.pointsLength = (int) newPointsLength;
        write(contours, position, contourNumberOfPoints);
    }

    void appendToEndForAllocatingSpace(Contours contours) {
        appendToEnd(contours, 0);
    }

    // See Contours.transformContour
    void transformContainingRectangle(double scaleX, double scaleY, double shiftX, double shiftY) {
        if (hasContainingRectangle) {
            final long x1 = Math.round(scaleX * minX + shiftX);
            final long y1 = Math.round(scaleY * minY + shiftY);
            Contours.checkPoint(x1, y1);
            // - to be on the safe side
            final long x2 = Math.round(scaleX * maxX + shiftX);
            final long y2 = Math.round(scaleY * maxY + shiftY);
            Contours.checkPoint(x2, y2);
            // - to be on the safe side
            minX = (int) Math.min(x1, x2);
            minY = (int) Math.min(y1, y2);
            maxX = (int) Math.max(x1, x2);
            maxY = (int) Math.max(y1, y2);
        }
    }

    int getFlags() {
        return getFlagsWithoutContourTouching()
                | (contourTouchingMinXMatrixBoundary ? TOUCHES_MIN_X_MATRIX_BOUNDARY_FLAG : 0)
                | (contourTouchingMaxXMatrixBoundary ? TOUCHES_MAX_X_MATRIX_BOUNDARY_FLAG : 0)
                | (contourTouchingMinYMatrixBoundary ? TOUCHES_MIN_Y_MATRIX_BOUNDARY_FLAG : 0)
                | (contourTouchingMaxYMatrixBoundary ? TOUCHES_MAX_Y_MATRIX_BOUNDARY_FLAG : 0);
    }

    int getFlagsWithoutContourTouching() {
        return (internalContour ? INTERNAL_FLAG : 0)
                | (frameId != null ? HAS_FRAME_ID : 0);
    }

    void setFlags(int flags) {
        this.internalContour = (flags & INTERNAL_FLAG) != 0;
        this.contourTouchingMinXMatrixBoundary = (flags & TOUCHES_MIN_X_MATRIX_BOUNDARY_FLAG) != 0;
        this.contourTouchingMaxXMatrixBoundary = (flags & TOUCHES_MAX_X_MATRIX_BOUNDARY_FLAG) != 0;
        this.contourTouchingMinYMatrixBoundary = (flags & TOUCHES_MIN_Y_MATRIX_BOUNDARY_FLAG) != 0;
        this.contourTouchingMaxYMatrixBoundary = (flags & TOUCHES_MAX_Y_MATRIX_BOUNDARY_FLAG) != 0;
    }

    private void checkContainingRectangle() {
        if (!hasContainingRectangle) {
            throw new IllegalStateException("This header does not contain information about containing rectangle");
        }
    }
}
