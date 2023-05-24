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

package net.algart.executors.modules.core.matrices.arithmetic;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;

public enum AngleDistanceMetric {
    /**
     * sin dfi
     */
    SIN() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2sin = x2 * y1 - x1 * y2;
                return r1r2sin / r1r2;
            }
        }
    },
    /**
     * sin dfi * cos dfi (i.e. 0.5 sin 2*dfi)
     */
    SIN_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2Sqr = (x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2);
            if (r1r2Sqr < COMPUTER_EPSILON_SQR) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2sin = x2 * y1 - x1 * y2;
                double r1r2cos = x1 * y1 + x2 * y2;
                return r1r2sin * r1r2cos / r1r2Sqr;
            }
        }
    },
    /**
     * sin dfi * signum cos dfi
     */
    SIN_SIGNUM_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2cos = x1 * y1 + x2 * y2;
                return (r1r2cos >= 0.0 ? x2 * y1 - x1 * y2 : x1 * y2 - x2 * y1) / r1r2;
            }
        }
    },
    /**
     * sqrt(|r1| * |r2|) * sin dfi
     */
    R_SIN() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2sin = x2 * y1 - x1 * y2;
                return r1r2sin / Math.sqrt(r1r2);
            }
        }
    },
    /**
     * sqrt(|r1| * |r2|) * sin dfi * cos dfi
     */
    R_SIN_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2sin = x2 * y1 - x1 * y2;
                double r1r2cos = x1 * y1 + x2 * y2;
                return r1r2sin * r1r2cos / (r1r2 * Math.sqrt(r1r2));
            }
        }
    },
    /**
     * sqrt(|r1| * |r2|) * sin dfi * signum cos dfi
     */
    R_SIN_SIGNUM_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume 0.0 for very little vectors
                return 0.0;
            } else {
                double r1r2cos = x1 * y1 + x2 * y2;
                return (r1r2cos >= 0.0 ? x2 * y1 - x1 * y2 : x1 * y2 - x2 * y1) / Math.sqrt(r1r2);
            }
        }
    },
    /**
     * |r1| * |r2| * sin dfi
     */
    R_R_SIN() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            return x2 * y1 - x1 * y2;
        }
    },
    /**
     * |r1| * |r2| * sin dfi * cos dfi
     */
    R_R_SIN_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2sin = x2 * y1 - x1 * y2;
            double r1r2cos = x1 * y1 + x2 * y2;
            double r1r2 = Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (r1r2 < COMPUTER_EPSILON) {
                // assume cos = 1.0 for very little vectors
                return r1r2sin;
            } else {
                return r1r2sin * r1r2cos / r1r2;
            }
        }
    },
    /**
     * |r1| * |r2| * sin dfi * signum cos dfi
     */
    R_R_SIN_SIGNUM_COS() {
        @Override
        public double distance(double x1, double y1, double x2, double y2) {
            double r1r2cos = x1 * y1 + x2 * y2;
            return r1r2cos >= 0.0 ? x2 * y1 - x1 * y2 : x1 * y2 - x2 * y1;
        }
    };

    private static final double COMPUTER_EPSILON = 1e-10;
    private static final double COMPUTER_EPSILON_SQR = 1e-20;

    public abstract double distance(double x1, double y1, double x2, double y2);

    public final Matrix<? extends PArray> asAngleDifference(
            Matrix<? extends PArray> x1,
            Matrix<? extends PArray> y1,
            Matrix<? extends PArray> x2,
            Matrix<? extends PArray> y2,
            Class<? extends PArray> requiredType) {
        return Matrices.asFuncMatrix(new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0], x[1], x[2], x[3]);
            }

            @Override
            public double get(double x1, double y1, double x2, double y2) {
                return distance(x1, y1, x2, y2);
            }
        }, requiredType, x1, y1, x2, y2);
    }
}
