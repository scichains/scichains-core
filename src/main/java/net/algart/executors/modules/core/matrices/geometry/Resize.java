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

package net.algart.executors.modules.core.matrices.geometry;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannel2DFilter;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix2D;

public final class Resize extends MultiMatrixChannel2DFilter {
    public static final String OUTPUT_DIM_X = "dim_x";
    public static final String OUTPUT_DIM_Y = "dim_y";

    public enum ResizingMode {
        NEAREST(Matrices.ResizingMethod.SIMPLE),
        AVERAGING(Matrices.ResizingMethod.AVERAGING),
        BILINEAR(Matrices.ResizingMethod.POLYLINEAR_INTERPOLATION),
        AVERAGING_BILINEAR(Matrices.ResizingMethod.POLYLINEAR_AVERAGING),
        AVERAGING_MIN(new Matrices.ResizingMethod.Averaging(Matrices.InterpolationMethod.STEP_FUNCTION) {
            @Override
            protected Func getAveragingFunc(long[] apertureDim) {
                return Func.MIN;
            }
        }),
        AVERAGING_MAX(new Matrices.ResizingMethod.Averaging(Matrices.InterpolationMethod.STEP_FUNCTION) {
            @Override
            protected Func getAveragingFunc(long[] apertureDim) {
                return Func.MAX;
            }
        });

        final Matrices.ResizingMethod resizingMethod;

        ResizingMode(Matrices.ResizingMethod resizingMethod) {
            this.resizingMethod = resizingMethod;
        }
    }

    private double dimX = 0.0;
    private double dimY = 0.0;
    private boolean percents = false;
    private ResizingMode resizingMode = ResizingMode.AVERAGING_BILINEAR;
    private boolean convertBitToByte = false;
    private boolean inputRequired = false;

    public Resize() {
        addOutputScalar(OUTPUT_DIM_X);
        addOutputScalar(OUTPUT_DIM_Y);
    }

    public double getDimX() {
        return dimX;
    }

    public Resize setDimX(double dimX) {
        this.dimX = nonNegative(dimX);
        return this;
    }

    public double getDimY() {
        return dimY;
    }

    public Resize setDimY(double dimY) {
        this.dimY = nonNegative(dimY);
        return this;
    }

    public boolean isPercents() {
        return percents;
    }

    public Resize setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public ResizingMode getResizingMode() {
        return resizingMode;
    }

    public Resize setResizingMode(ResizingMode resizingMode) {
        this.resizingMode = nonNull(resizingMode);
        return this;
    }

    public boolean isConvertBitToByte() {
        return convertBitToByte;
    }

    public Resize setConvertBitToByte(boolean convertBitToByte) {
        this.convertBitToByte = convertBitToByte;
        return this;
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public Resize setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        if (source == null) {
            return null;
        }
        MultiMatrix2D result = super.process(source);
        getScalar(OUTPUT_DIM_X).setTo(result.dimX());
        getScalar(OUTPUT_DIM_Y).setTo(result.dimY());
        return result;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        long newDimX = m.dimX();
        long newDimY = m.dimY();
        if (dimX != 0.0 || dimY != 0.0) {
            if (dimX != 0.0) {
                newDimX = percents ? Math.round((float) (dimX / 100.0 * m.dimX())) : Math.round((float) dimX);
            }
            if (dimY != 0.0) {
                newDimY = percents ? Math.round((float) (dimY / 100.0 * m.dimY())) : Math.round((float) dimY);
            }
            if (dimX == 0.0) {
                newDimX = Math.round(newDimX * (float) newDimY / (float) m.dimY());
            }
            if (dimY == 0.0) {
                newDimY = Math.round(newDimY * (float) newDimX / (float) m.dimX());
            }
        }
        if (currentChannel() == 0) {
            if (LOGGABLE_DEBUG) {
                logDebug("Resizing to " + newDimX + "x" + newDimY + " of" + m);
            }
        }
        if (m.elementType() == boolean.class && convertBitToByte) {
            m = Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, 255.0), ByteArray.class, m);
        }
        Matrix<UpdatablePArray> result = Arrays.SMM.newMatrix(
                UpdatablePArray.class, m.elementType(), newDimX, newDimY);
        Matrices.resize(null, resizingMode.resizingMethod, result, m);
        return result;
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return !inputRequired;
    }

    @Override
    protected boolean resultRequired() {
        return inputRequired;
    }
}
