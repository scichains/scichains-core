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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.multimatrix.MultiMatrix;

public final class Intensity extends MultiMatrixFilter {
    private boolean rgbResult = false;
    private boolean inputRequired = false;

    public boolean isRgbResult() {
        return rgbResult;
    }

    public Intensity setRgbResult(boolean rgbResult) {
        this.rgbResult = rgbResult;
        return this;
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public Intensity setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        return source == null ? null : source.toMonoIfNot().asOtherNumberOfChannels(rgbResult ? 3 : 1);
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
