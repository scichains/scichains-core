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

package net.algart.executors.modules.core.matrices.conversions;

import net.algart.arrays.*;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.ChannelOperation;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;

import java.util.ArrayList;
import java.util.List;

public final class ApplyMask extends SeveralMultiMatricesOperation {
    public static final String INPUT_MASK = "mask";

    public enum MaskingMode {
        FILL_BACKGROUND,
        FILL_FOREGROUND
    }

    private MaskingMode maskingMode = MaskingMode.FILL_BACKGROUND;
    private String filler = "0";

    public ApplyMask() {
        super(DEFAULT_INPUT_PORT, INPUT_MASK);
    }

    public MaskingMode getMaskingMode() {
        return maskingMode;
    }

    public ApplyMask setMaskingMode(MaskingMode maskingMode) {
        this.maskingMode = nonNull(maskingMode);
        return this;
    }

    public String getFiller() {
        return filler;
    }

    public ApplyMask setFiller(String filler) {
        this.filler = nonEmpty(filler);
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        final MultiMatrix source = sources.get(0);
        MultiMatrix mask = sources.get(1);
        if (mask == null) {
            return source;
        }
        source.checkDimensionEquality(mask, "source", "mask");
        final BitArray maskArray = mask.nonZeroRGBMatrix().array();
        List<Matrix<? extends PArray>> result = new ArrayList<>();
        final double maxPossibleValue = source.maxPossibleValue();
        for (int k = 0, n = source.numberOfChannels(); k < n; k++) {
            Matrix<? extends PArray> m = source.channel(k);
            Matrix<? extends UpdatablePArray> clone = Matrices.clone(m);
            double filler = ChannelOperation.colorChannel(this.filler, maxPossibleValue, k, n);
            switch (maskingMode) {
                case FILL_BACKGROUND -> {
                    Arrays.unpackZeroBits(clone.array(), maskArray, filler);
                }
                case FILL_FOREGROUND -> {
                    Arrays.unpackUnitBits(clone.array(), maskArray, filler);
                }
                default -> {
                    throw new AssertionError("Unsupported mode " + maskingMode);
                }
            }
            result.add(clone);
        }
        return MultiMatrix.valueOf(result);
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex >= 1;
    }
}
