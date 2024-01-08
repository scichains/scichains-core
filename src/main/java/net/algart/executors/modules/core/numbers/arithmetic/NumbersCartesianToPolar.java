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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.math.IRange;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class NumbersCartesianToPolar extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_X = "x";
    public static final String INPUT_Y = "y";
    public static final String INPUT_X_Y = "x_y";
    public static final String OUTPUT_R = "r";
    public static final String OUTPUT_FI = "fi";
    public static final String OUTPUT_R_FI = "r_fi";

    public enum AngleRange {
        ZERO_2PI {
            @Override
            double correctAtan2(double atan2) {
                return atan2 < 0.0 ? atan2 + PI_2 : atan2;
            }
        },
        ZERO_2PI_AS_0_1 {
            @Override
            double correctAtan2(double atan2) {
                double angle = atan2 * MULTIPLIER;
                return angle < 0.0 ? angle + 1.0 : angle;
            }
        },
        MINUS_PI_PLUS_PI {
            @Override
            double correctAtan2(double atan2) {
                return atan2;
            }
        },
        MINUS_PI_PLUS_PI_AS_MINUS_HALF_PLUS_HALF {
            @Override
            double correctAtan2(double atan2) {
                return atan2 * MULTIPLIER;
            }
        },
        MINUS_PI_PLUS_PI_AS_MINUS_1_PLUS_1 {
            @Override
            double correctAtan2(double atan2) {
                return atan2 * MULTIPLIER_2;
            }
        },
        MINUS_PI_PLUS_PI_AS_0_1 {
            @Override
            double correctAtan2(double atan2) {
                return atan2 * MULTIPLIER + 0.5;
            }
        };

        private static final double MULTIPLIER = 1.0 / (2.0 * StrictMath.PI);
        private static final double MULTIPLIER_2 = 1.0 / StrictMath.PI;
        private static final double PI_2 = 2.0 * StrictMath.PI;

        abstract double correctAtan2(double atan2);
    }

    private AngleRange angleRange = AngleRange.MINUS_PI_PLUS_PI;
    private double angleMultiplier = 1.0;
    private double epsilonForLittleSquare = -1.0;
    private double angleForLittleSquare = 0.0;

    public NumbersCartesianToPolar() {
        super(INPUT_X, INPUT_Y, INPUT_X_Y);
        useVisibleResultParameter();
        setDefaultOutputNumbers(OUTPUT_R_FI);
        addOutputNumbers(OUTPUT_R);
        addOutputNumbers(OUTPUT_FI);
    }

    public AngleRange getAngleRange() {
        return angleRange;
    }

    public NumbersCartesianToPolar setAngleRange(AngleRange angleRange) {
        this.angleRange = nonNull(angleRange);
        return this;
    }

    public double getAngleMultiplier() {
        return angleMultiplier;
    }

    public NumbersCartesianToPolar setAngleMultiplier(double angleMultiplier) {
        this.angleMultiplier = angleMultiplier;
        return this;
    }

    public double getEpsilonForLittleSquare() {
        return epsilonForLittleSquare;
    }

    public NumbersCartesianToPolar setEpsilonForLittleSquare(double epsilonForLittleSquare) {
        this.epsilonForLittleSquare = epsilonForLittleSquare;
        return this;
    }

    public double getAngleForLittleSquare() {
        return angleForLittleSquare;
    }

    public NumbersCartesianToPolar setAngleForLittleSquare(double angleForLittleSquare) {
        this.angleForLittleSquare = angleForLittleSquare;
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        SNumbers x = sources.get(0);
        SNumbers y = sources.get(1);
        SNumbers xy = sources.get(2);
        if (xy != null) {
            xy.requireBlockLength(2, "x/y");
        }
        if (x != null) {
            x.requireInitialized("x").requireBlockLength(1, "x");
        } else {
            x = checkXY(xy, "x").columnRange(0, 1);
        }
        if (y != null) {
            y.requireInitialized("y").requireBlockLength(1, "y");
        } else {
            y = checkXY(xy, "y").columnRange(1, 1);
        }
        final float[] xArray = x.toFloatArray();
        final float[] yArray = y.toFloatArray();
        final SNumbers result = new SNumbers();
        if (isOutputNecessary(OUTPUT_R_FI)) {
            final float[] rFi = magnitudeAndArctangent(xArray, yArray);
            result.setTo(rFi, 2);
            if (isOutputNecessary(OUTPUT_R)) {
                getNumbers(OUTPUT_R).replaceColumnRange(0, result, 0, 1);
            }
            if (isOutputNecessary(OUTPUT_FI)) {
                getNumbers(OUTPUT_FI).replaceColumnRange(0, result, 1, 1);
            }
        } else {
            if (isOutputNecessary(OUTPUT_R)) {
                getNumbers(OUTPUT_R).setTo(magnitude(xArray, yArray), 1);
            }
            if (isOutputNecessary(OUTPUT_FI)) {
                getNumbers(OUTPUT_FI).setTo(arctangent(xArray, yArray), 1);
            }
        }
        return result;
    }

    public float[] magnitude(float[] x, float[] y) {
        Objects.requireNonNull(x, "Null x");
        Objects.requireNonNull(y, "Null y");
        if (x.length != y.length) {
            throw new IllegalArgumentException("Different lengths of x and y");
        }
        float[] result = new float[x.length];
        IntStream.range(0, (x.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, to = (int) Math.min((long) i + 256, x.length); i < to; i++) {
                final double xValue = x[i];
                final double yValue = y[i];
                result[i] = (float) (Math.sqrt(xValue * xValue + yValue * yValue));
            }
        });
        return result;
    }

    public float[] arctangent(float[] x, float[] y) {
        Objects.requireNonNull(x, "Null x");
        Objects.requireNonNull(y, "Null y");
        if (x.length != y.length) {
            throw new IllegalArgumentException("Different lengths of x and y");
        }
        float[] result = new float[x.length];
        IntStream.range(0, (x.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, to = (int) Math.min((long) i + 256, x.length); i < to; i++) {
                final double xValue = x[i];
                final double yValue = y[i];
                final double arctangent =
                        xValue * xValue + yValue * yValue <= epsilonForLittleSquare ?
                                angleForLittleSquare :
                                angleRange.correctAtan2(Math.atan2(yValue, xValue)) * angleMultiplier;
                result[i] = (float) arctangent;
            }
        });
        return result;
    }

    public float[] magnitudeAndArctangent(float[] x, float[] y) {
        Objects.requireNonNull(x, "Null x");
        Objects.requireNonNull(y, "Null y");
        if (x.length != y.length) {
            throw new IllegalArgumentException("Different lengths of x and y");
        }
        float[] result = new float[2 * x.length];
        IntStream.range(0, (x.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, disp = 2 * i, to = (int) Math.min((long) i + 256, x.length); i < to; i++) {
                final double xValue = x[i];
                final double yValue = y[i];
                final double rSqr = xValue * xValue + yValue * yValue;
                final double arctangent =
                        rSqr <= epsilonForLittleSquare ?
                                angleForLittleSquare :
                                angleRange.correctAtan2(Math.atan2(yValue, xValue)) * angleMultiplier;
                result[disp++] = (float) Math.sqrt(rSqr);
                result[disp++] = (float) arctangent;
            }
        });
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

    @Override
    protected IRange selectedColumnRange(int inputIndex) {
        final int start = getIndexInBlock(inputIndex);
        return IRange.valueOf(start, inputIndex == 2 ? start + 1 : start);
    }

    private static SNumbers checkXY(SNumbers xy, String xOrYName) {
        if (xy == null || !xy.isInitialized()) {
            throw new IllegalArgumentException("The \"x/y\" port has no initialized data, but they are required, "
                    + "because \"" + xOrYName + "\" is also empty");
        }
        return xy;
    }

}
