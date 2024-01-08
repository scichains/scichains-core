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

public final class ContourLength {
    private int numberOfPoints = 1;
    private int arrayLength = 2;

    long doubledArea = 0;

    public ContourLength setNumberOfPoints(int numberOfPoints) {
        if (numberOfPoints <= 0) {
            throw new IllegalArgumentException("Invalid cnntour length: zero or negative number of points "
                    + numberOfPoints);
        }
        if (numberOfPoints >= Contours.MAX_CONTOUR_NUMBER_OF_POINTS) {
            throw new IllegalArgumentException("Too large number of points in a contour: it is > "
                    + Contours.MAX_CONTOUR_NUMBER_OF_POINTS);
        }
        this.numberOfPoints = numberOfPoints;
        this.arrayLength = numberOfPoints << 1;
        return this;
    }

    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    public boolean isDegenerated() {
        return numberOfPoints == 1;
    }

    @Override
    public String toString() {
        return "length " + numberOfPoints + " point"
                + (numberOfPoints > 1 ? "s" : "") + " (int[" + arrayLength + "])";
    }
}
