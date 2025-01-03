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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.multimatrix.MultiMatrix;

import java.util.Objects;

public final class SubMatrix extends SubMatrixFilter {
    public static final String OUTPUT_DIM_X = "dim_x";
    public static final String OUTPUT_DIM_Y = "dim_y";

    private boolean doAction = true;
    private Matrix.ContinuationMode continuationMode = Matrix.ContinuationMode.CYCLIC;

    public SubMatrix() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
        addOutputNumbers(RECTANGULAR_AREA);
        addOutputScalar(OUTPUT_DIM_X);
        addOutputScalar(OUTPUT_DIM_Y);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public SubMatrix setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public Matrix.ContinuationMode getContinuationMode() {
        return continuationMode;
    }

    public SubMatrix setContinuationMode(Matrix.ContinuationMode continuationMode) {
        this.continuationMode = continuationMode;
        return this;
    }

    public SubMatrix setContinuationMode(String continuationMode) {
        Objects.requireNonNull(continuationMode, "Null continuation mode");
        switch (continuationMode) {
            case "CYCLIC":
                this.continuationMode = Matrix.ContinuationMode.CYCLIC;
                break;
            case "PSEUDO_CYCLIC":
                this.continuationMode = Matrix.ContinuationMode.PSEUDO_CYCLIC;
                break;
            case "MIRROR_CYCLIC":
                this.continuationMode = Matrix.ContinuationMode.MIRROR_CYCLIC;
                break;
            case "ZERO_CONSTANT":
                this.continuationMode = Matrix.ContinuationMode.ZERO_CONSTANT;
                break;
            case "POSITIVE_INFINITY":
                this.continuationMode = Matrix.ContinuationMode.getConstantMode(Double.POSITIVE_INFINITY);
                break;
            case "NEGATIVE_INFINITY":
                this.continuationMode = Matrix.ContinuationMode.getConstantMode(Double.NEGATIVE_INFINITY);
                break;
            case "NAN_CONSTANT":
                this.continuationMode = Matrix.ContinuationMode.NAN_CONSTANT;
                break;
            default:
                this.continuationMode = Matrix.ContinuationMode.NONE;
                break;
        }
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        getScalar(OUTPUT_DIM_X).setTo(source.dim(0));
        getScalar(OUTPUT_DIM_Y).setTo(source.dim(1));
        return doAction ? super.process(source) : source;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        return extractSubMatrix(m, continuationMode);
    }

    @Override
    protected void logProcessing(String submatrixDescription) {
        if (currentChannel() == 0) {
            logDebug(() -> "Extracting " + submatrixDescription);
        }
    }
}
