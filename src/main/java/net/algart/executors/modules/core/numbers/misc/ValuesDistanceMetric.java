/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.numbers.misc;

public enum ValuesDistanceMetric {
    ELEMENTWISE_ABSOLUTE_DIFFERENCE {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            throw new UnsupportedOperationException(this + " does not allow return single number");
        }

        @Override
        public void distance(double[] result, double[] a, double[] b, double[] valuesWeights) {
            check(a, b, valuesWeights);
            for (int k = 0; k < a.length; k++) {
                result[k] = Math.abs(a[k] - b[k]);
            }
        }
    },
    EUCLIDEAN {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            return Math.sqrt(SUM_OF_SQUARES.distance(a, b, valuesWeights));
        }
    },
    NORMALIZED_EUCLIDEAN {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            return Math.sqrt(SUM_OF_SQUARES.distance(a, b, valuesWeights) / a.length);
        }
    },
    SUM_OF_SQUARES {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            check(a, b, valuesWeights);
            double sum = 0.0;
            for (int k = 0; k < a.length; k++) {
                final double d = (a[k] - b[k]) * valuesWeights[k];
                sum += d * d;
            }
            return sum;
        }
    },
    MEAN_ABSOLUTE_DIFFERENCE {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            return SUM_OF_ABSOLUTE_DIFFERENCES.distance(a, b, valuesWeights) / a.length;
        }
    },
    SUM_OF_ABSOLUTE_DIFFERENCES {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            check(a, b, valuesWeights);
            double sum = 0.0;
            for (int k = 0; k < a.length; k++) {
                final double d = (a[k] - b[k]) * valuesWeights[k];
                sum += Math.abs(d);
            }
            return sum;
        }
    },
    MAX_ABSOLUTE_DIFFERENCE {
        @Override
        public double distance(double[] a, double[] b, double[] valuesWeights) {
            check(a, b, valuesWeights);
            double max = 0.0;
            for (int k = 0; k < a.length; k++) {
                final double d = Math.abs(a[k] - b[k]) * valuesWeights[k];
                if (d > max) {
                    max = d;
                }
            }
            return max;
        }
    };

    public boolean isSingleNumber() {
        return this != ELEMENTWISE_ABSOLUTE_DIFFERENCE;
    }

    public int resultLength(int sourceLength) {
        return this == ELEMENTWISE_ABSOLUTE_DIFFERENCE ? sourceLength : 1;
    }

    public void distance(double[] result, double[] a, double[] b, double[] valuesWeights) {
        result[0] = distance(a, b, valuesWeights);
    }

    public abstract double distance(double[] a, double[] b, double[] valuesWeights);

    private static void check(double[] a, double[] b, double[] weights) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Different array lengths: a.length="
                    + a.length + ", b.length=" + b.length);
        }
        if (a.length != weights.length) {
            throw new IllegalArgumentException("Different array lengths: a.length=b.length="
                    + a.length + ", weights.length=" + weights.length);
        }
    }
}
