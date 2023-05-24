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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.arrays.DoubleArray;
import net.algart.arrays.FloatArray;
import net.algart.arrays.Matrix;
import net.algart.math.Point;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.ContinuedMorphology;
import net.algart.matrices.morphology.Morphology;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;

public final class FindValue extends MultiMatrixFilter {
    public enum ValueKind {
        ZERO {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.zeroPixels(checkOnlyRGBChannels);
            }
        },
        NON_ZERO {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.nonZeroPixels(checkOnlyRGBChannels);
            }
        },
        NAN {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asFunc(IS_NAN, FloatArray.class).nonZeroPixels(checkOnlyRGBChannels);
            }
        },
        INFINITY {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asFunc(IS_INFINITY, FloatArray.class).nonZeroPixels(checkOnlyRGBChannels);
            }
        },
        EQUAL_TO_RAW_CUSTOM {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asFunc(LinearFunc.getInstance(customValue, -1.0), DoubleArray.class)
                        .zeroPixels(checkOnlyRGBChannels);
            }
        },
        NONEQUAL_TO_RAW_CUSTOM {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asFunc(LinearFunc.getInstance(customValue, -1.0), DoubleArray.class)
                        .nonZeroPixels(checkOnlyRGBChannels);
            }
        },
        EQUAL_TO_NORMALIZED_CUSTOM {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asPrecision(double.class)
                        .asFunc(LinearFunc.getInstance(customValue, -1.0), DoubleArray.class)
                        .zeroPixels(checkOnlyRGBChannels);
            }
        },
        NONEQUAL_TO_NORMALIZED_CUSTOM {
            @Override
            MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue) {
                return source.asPrecision(double.class)
                        .asFunc(LinearFunc.getInstance(customValue, -1.0), DoubleArray.class)
                        .nonZeroPixels(checkOnlyRGBChannels);
            }
        };


        abstract MultiMatrix find(MultiMatrix source, boolean checkOnlyRGBChannels, double customValue);
    }

    private static final Func IS_NAN = new AbstractFunc() {
        @Override
        public double get(double... x) {
            return Double.isNaN(x[0]) ? 1.0 : 0.0;
        }

        @Override
        public double get(double x0) {
            return Double.isNaN(x0) ? 1.0 : 0.0;
        }
    };

    private static final Func IS_INFINITY = new AbstractFunc() {
        @Override
        public double get(double... x) {
            return Double.isInfinite(x[0]) ? 1.0 : 0.0;
        }

        @Override
        public double get(double x0) {
            return Double.isInfinite(x0) ? 1.0 : 0.0;
        }
    };

    private ValueKind valueKind = ValueKind.NON_ZERO;
    private boolean checkOnlyRGBChannels = true;
    private double customValue = 0.0;
    private int dilationSize = 0;

    public ValueKind getValueKind() {
        return valueKind;
    }

    public FindValue setValueKind(ValueKind valueKind) {
        this.valueKind = nonNull(valueKind);
        return this;
    }

    public boolean isCheckOnlyRGBChannels() {
        return checkOnlyRGBChannels;
    }

    public FindValue setCheckOnlyRGBChannels(boolean checkOnlyRGBChannels) {
        this.checkOnlyRGBChannels = checkOnlyRGBChannels;
        return this;
    }

    public double getCustomValue() {
        return customValue;
    }

    public FindValue setCustomValue(double customValue) {
        this.customValue = customValue;
        return this;
    }

    public int getDilationSize() {
        return dilationSize;
    }

    public FindValue setDilationSize(int dilationSize) {
        this.dilationSize = nonNegative(dilationSize);
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        MultiMatrix binaryResult = valueKind.find(source, checkOnlyRGBChannels, customValue);
        if (dilationSize > 0) {
            final Morphology morphology = ContinuedMorphology.getInstance(
                    BasicMorphology.getInstance(null),
                    Matrix.ContinuationMode.ZERO_CONSTANT);
            final Pattern pattern = Patterns.newSphereIntegerPattern(
                    Point.origin(binaryResult.dimCount()),
                    Math.max(0.0, 0.5 * (dilationSize + 1) - 0.2));
            binaryResult = binaryResult.mapChannels(m -> morphology.dilation(m, pattern));
        }
        return binaryResult;
    }
}
