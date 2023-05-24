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

import net.algart.math.IRectangularArea;

import java.util.Objects;
import java.util.function.*;
import java.util.stream.IntStream;

// Not too fast version: optimization only by horizontal or vertical
public abstract class IRectangleFinder {
    IntUnaryOperator minX = null;
    IntUnaryOperator maxX = null;
    IntUnaryOperator minY = null;
    IntUnaryOperator maxY = null;
    IntUnaryOperator left2 = null;
    IntUnaryOperator right2 = null;
    int n = 0;

    final IRangeFinder rangeFinder = IRangeFinder.getEmptyInstance();

    public static IRectangleFinder getEmptyInstance() {
        return getEmptyInstance(false);
        // - vertical by default: better for many applications
    }

    public static IRectangleFinder getEmptyInstance(boolean horizontal) {
        return horizontal ? new Horizontal() : new Vertical();
    }

    public static IRectangleFinder getEmptyUnoptimizedInstance() {
        return new IRectangleFinderWithoutOptimization();
    }

    public static IRectangleFinder getInstance(
            IntUnaryOperator minX,
            IntUnaryOperator maxX,
            IntUnaryOperator minY,
            IntUnaryOperator maxY,
            int numberOfRectangles) {
        return getEmptyInstance().setRectangles(minX, maxX, minY, maxY, numberOfRectangles);
    }

    public static IRectangleFinder getInstance(int[] minX, int[] maxX, int[] minY, int[] maxY) {
        Objects.requireNonNull(minX, "Null minX");
        Objects.requireNonNull(maxX, "Null maxX");
        Objects.requireNonNull(minY, "Null minY");
        Objects.requireNonNull(maxY, "Null maxY");
        if (maxX.length != minX.length || minY.length != minX.length || maxY.length != minX.length) {
            throw new IllegalArgumentException("Different lengths of arrays of minimal/maximal coordinates");
        }
        return getInstance(i -> minX[i], i -> maxX[i], i -> minY[i], i -> maxY[i], minX.length);
    }

    public IntUnaryOperator minX() {
        return minX;
    }

    public IntUnaryOperator maxX() {
        return maxX;
    }

    public IntUnaryOperator minY() {
        return minY;
    }

    public IntUnaryOperator maxY() {
        return maxY;
    }

    public final int minX(int k) {
        return minX.applyAsInt(k);
    }

    public final int maxX(int k) {
        return maxX.applyAsInt(k);
    }

    public final int minY(int k) {
        return minY.applyAsInt(k);
    }

    public final int maxY(int k) {
        return maxY.applyAsInt(k);
    }

    public final int numberOfRectangles() {
        return n;
    }

    public final IRectangleFinder setRectangles(
            IntUnaryOperator minX,
            IntUnaryOperator maxX,
            IntUnaryOperator minY,
            IntUnaryOperator maxY,
            int numberOfRectangles) {
        if (numberOfRectangles < 0) {
            throw new IllegalArgumentException("Negative number of rectangles " + numberOfRectangles);
        }
        checkRectangles(minX, maxX, minY, maxY, numberOfRectangles);
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.n = numberOfRectangles;
        setRanges();
        return this;
    }

    public final IRectangleFinder setRectangles(int[] minX, int[] maxX, int[] minY, int[] maxY) {
        Objects.requireNonNull(minX, "Null minX");
        Objects.requireNonNull(maxX, "Null maxX");
        Objects.requireNonNull(minY, "Null minY");
        Objects.requireNonNull(maxY, "Null maxY");
        if (maxX.length != minX.length || minY.length != minX.length || maxY.length != minX.length) {
            throw new IllegalArgumentException("Different lengths of arrays of minimal/maximal coordinates");
        }
        return setRectangles(i -> minX[i], i -> maxX[i], i -> minY[i], i -> maxY[i], minX.length);
    }

    public final IRectangleFinder setIndexedRectangles(
            int[] minX,
            int[] maxX,
            int[] minY,
            int[] maxY,
            int[] indexes,
            int numberOfRectangles) {
        Objects.requireNonNull(minX, "Null minX");
        Objects.requireNonNull(maxX, "Null maxX");
        Objects.requireNonNull(minY, "Null minY");
        Objects.requireNonNull(maxY, "Null maxY");
        Objects.requireNonNull(indexes, "Null indexes");
        if (maxX.length != minX.length || minY.length != minX.length || maxY.length != minX.length) {
            throw new IllegalArgumentException("Different lengths of arrays of minimal/maximal coordinates");
        }
        if (numberOfRectangles < 0) {
            throw new IllegalArgumentException("Negative number of rectangles");
        }
        if (numberOfRectangles > indexes.length) {
            throw new IllegalArgumentException("Number of rectangles " + numberOfRectangles
                    + " > indexes array length = " + indexes.length);
        }
        return setRectangles(
                i -> minX[indexes[i]],
                i -> maxX[indexes[i]],
                i -> minY[indexes[i]],
                i -> maxY[indexes[i]],
                numberOfRectangles);
    }

    public final IRectangleFinder setIndexActual(IntPredicate indexActual) {
        rangeFinder.setIndexActual(indexActual);
        return this;
    }

    public final IRectangleFinder setAllIndexesActual() {
        rangeFinder.setAllIndexesActual();
        return this;
    }

    public final boolean indexActual(int k) {
        return rangeFinder.indexActual(k);
    }

    // Useful if this instance will be used for a long time with another accesses to the heap
    public IRectangleFinder compact() {
        rangeFinder.compact();
        return this;
    }

    public abstract void findContaining(int x, int y, IntConsumer indexConsumer);

    public int findContaining(int x, int y, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findContaining(x, y, appender);
        return appender.offset();
    }

    public abstract void findContaining(double x, double y, IntConsumer indexConsumer);

    public int findContaining(double x, double y, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findContaining(x, y, appender);
        return appender.offset();
    }

    public abstract void findIntersecting(int minX, int maxX, int minY, int maxY, IntConsumer indexConsumer);

    public int findIntersecting(int minX, int maxX, int minY, int maxY, int[] resultIndexes) {
        final IntArrayAppender appender = new IntArrayAppender(resultIndexes);
        findIntersecting(minX, maxX, minY, maxY, appender);
        return appender.offset();
    }

    public void clear() {
        this.rangeFinder.clear();
        this.minX = null;
        this.maxX = null;
        this.minY = null;
        this.maxY = null;
        this.left2 = null;
        this.right2 = null;
        this.n = 0;
    }

    @Override
    public String toString() {
        return algorithmName() + " for " + n + " rectangles"
                + (n == 0 ? " (empty)" : "")
                + ", based on " + rangeFinder;
    }

    protected abstract void setRanges();

    protected String algorithmName() {
        return "finder";
    };

    // Not used now
    private static IRectangularArea checkAndFindContainingRectangle(
            IntUnaryOperator minX,
            IntUnaryOperator maxX,
            IntUnaryOperator minY,
            IntUnaryOperator maxY,
            int numberOfRectangles) {
        if (numberOfRectangles <= 0) {
            return null;
        }
        Objects.requireNonNull(minX, "Null operator for getting minX bounds");
        Objects.requireNonNull(maxX, "Null operator for getting maxX bounds");
        Objects.requireNonNull(minY, "Null operator for getting minY bounds");
        Objects.requireNonNull(maxY, "Null operator for getting maxY bounds");
        return IntStream.range(0, (numberOfRectangles + 1023) >>> 10).parallel().mapToObj(block -> {
            int containingMinX = Integer.MAX_VALUE;
            int containingMaxX = Integer.MIN_VALUE;
            int containingMinY = Integer.MAX_VALUE;
            int containingMaxY = Integer.MIN_VALUE;
            for (int i = block << 10, to = (int) Math.min((long) i + 1024, numberOfRectangles); i < to; i++) {
                final int rMinX = minX.applyAsInt(i);
                final int rMaxX = maxX.applyAsInt(i);
                final int rMinY = minY.applyAsInt(i);
                final int rMaxY = maxY.applyAsInt(i);
                if (rMinX > rMaxX) {
                    throw new IllegalArgumentException("Illegal rectangle #" + i + ": minX > maxX");
                }
                if (rMinY > rMaxY) {
                    throw new IllegalArgumentException("Illegal rectangle #" + i + ": minY > maxY");
                }
                if (rMinX < containingMinX) {
                    containingMinX = rMinX;
                }
                if (rMaxX > containingMaxX) {
                    containingMaxX = rMaxX;
                }
                if (rMinY < containingMinY) {
                    containingMinY = rMinY;
                }
                if (rMaxY > containingMaxY) {
                    containingMaxY = rMaxY;
                }
            }
            return IRectangularArea.valueOf(containingMinX, containingMinY, containingMaxX, containingMaxY);
        }).reduce(IRectangularArea::expand).orElse(null);
    }

    private static void checkRectangles(
            IntUnaryOperator minX,
            IntUnaryOperator maxX,
            IntUnaryOperator minY,
            IntUnaryOperator maxY,
            int numberOfRectangles) {
        if (numberOfRectangles <= 0) {
            return;
        }
        Objects.requireNonNull(minX, "Null operator for getting minX bounds");
        Objects.requireNonNull(maxX, "Null operator for getting maxX bounds");
        Objects.requireNonNull(minY, "Null operator for getting minY bounds");
        Objects.requireNonNull(maxY, "Null operator for getting maxY bounds");
        IntStream.range(0, (numberOfRectangles + 1023) >>> 10).parallel().forEach(block -> {
            for (int i = block << 10, to = (int) Math.min((long) i + 1024, numberOfRectangles); i < to; i++) {
                final int rMinX = minX.applyAsInt(i);
                final int rMaxX = maxX.applyAsInt(i);
                final int rMinY = minY.applyAsInt(i);
                final int rMaxY = maxY.applyAsInt(i);
                if (rMinX > rMaxX) {
                    throw new IllegalArgumentException("Illegal rectangle #" + i + ": minX > maxX");
                }
                if (rMinY > rMaxY) {
                    throw new IllegalArgumentException("Illegal rectangle #" + i + ": minY > maxY");
                }
            }
        });
    }

    private static class Horizontal extends IRectangleFinder {
        @Override
        public void findContaining(int x, int y, IntConsumer indexConsumer) {
            rangeFinder.findContaining(x, index -> {
                if (left2.applyAsInt(index) <= y && right2.applyAsInt(index) >= y) {
                    indexConsumer.accept(index);
                }
            });
        }

        @Override
        public void findContaining(double x, double y, IntConsumer indexConsumer) {
            if (y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
                return;
            }
            rangeFinder.findContaining(x, index -> {
                if (left2.applyAsInt(index) <= y && right2.applyAsInt(index) >= y) {
                    indexConsumer.accept(index);
                }
            });
        }

        @Override
        public void findIntersecting(int minX, int maxX, int minY, int maxY, IntConsumer indexConsumer) {
            if (minX > maxX || minY > maxY) {
                return;
            }
            rangeFinder.findIntersecting(minX, maxX, index -> {
                if (left2.applyAsInt(index) <= maxY && right2.applyAsInt(index) >= minY) {
                    indexConsumer.accept(index);
                }
            });
        }

        protected void setRanges() {
            this.rangeFinder.setRanges(minX, maxX, n);
            this.left2 = minY;
            this.right2 = maxY;
        }

        @Override
        protected String algorithmName() {
            return "horizontal-splitting integer rectangles finder";
        }
    }

    private static class Vertical extends IRectangleFinder {
        @Override
        public void findContaining(int x, int y, IntConsumer indexConsumer) {
            rangeFinder.findContaining(y, index -> {
                if (left2.applyAsInt(index) <= x && right2.applyAsInt(index) >= x) {
                    indexConsumer.accept(index);
                }
            });
        }

        @Override
        public void findContaining(double x, double y, IntConsumer indexConsumer) {
            if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE) {
                return;
            }
            rangeFinder.findContaining(y, index -> {
                if (left2.applyAsInt(index) <= x && right2.applyAsInt(index) >= x) {
                    indexConsumer.accept(index);
                }
            });
        }

        @Override
        public void findIntersecting(int minX, int maxX, int minY, int maxY, IntConsumer indexConsumer) {
            if (minX > maxX || minY > maxY) {
                return;
            }
            rangeFinder.findIntersecting(minY, maxY, index -> {
                if (left2.applyAsInt(index) <= maxX && right2.applyAsInt(index) >= minX) {
                    indexConsumer.accept(index);
                }
            });
        }

        protected void setRanges() {
            this.rangeFinder.setRanges(minY, maxY, n);
            this.left2 = minX;
            this.right2 = maxX;
        }

        @Override
        protected String algorithmName() {
            return "vertical-splitting integer rectangles finder";
        }
    }
}
