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

import net.algart.arrays.ByteArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.api.data.SMat;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;


public final class MergeColorChannels extends ColorSpaceConversion {
    public static final long DEFAULT_DIM_X_WHEN_NO_SOURCE = 512;
    public static final long DEFAULT_DIM_Y_WHEN_NO_SOURCE = 512;

    private final double[] defaultFillerForChannels = new double[]{1.0, 1.0, 1.0};

    public MergeColorChannels() {
        addOutputMat(DEFAULT_OUTPUT_PORT);
        for (String port : SplitColorChannels.CHANNEL_PORTS) {
            addInputMat(port);
        }
    }

    public double getDefaultFillerForChannel1() {
        return defaultFillerForChannels[0];
    }

    public void setDefaultFillerForChannel1(double defaultFillerForChannel1) {
        this.defaultFillerForChannels[0] = defaultFillerForChannel1;
    }

    public double getDefaultFillerForChannel2() {
        return defaultFillerForChannels[1];
    }

    public void setDefaultFillerForChannel2(double defaultFillerForChannel2) {
        this.defaultFillerForChannels[1] = defaultFillerForChannel2;
    }

    public double getDefaultFillerForChannel3() {
        return defaultFillerForChannels[2];
    }

    public void setDefaultFillerForChannel3(double defaultFillerForChannel3) {
        this.defaultFillerForChannels[2] = defaultFillerForChannel3;
    }

    @Override
    public void process() {
        final List<Matrix<? extends PArray>> channels = new ArrayList<>();
        final List<Matrix<? extends PArray>> initialized = new ArrayList<>();
        for (String port : SplitColorChannels.CHANNEL_PORTS) {
            final int channelIndex = channels.size();
            final SMat channel = getInputMat(port, true);
            if (channel.isInitialized()) {
                final Matrix<? extends PArray> m = channel.toMultiMatrix2D().intensityChannel();
                logDebug(() -> "Merging channel #" + (channelIndex + 1) + " -> "
                        + "color matrix " + m.dimX() + "x" + m.dimY());
                channels.add(m);
                initialized.add(m);
            } else {
                if (channelIndex == 3) {
                    break;
                    // - alpha is optional
                }
                logDebug(() -> "Merged channel #" + (channelIndex + 1)
                        + " is empty: it will be filled by default value " + defaultFillerForChannels[channelIndex]);
                channels.add(null);
            }
        }
        Matrices.checkDimensionEquality(initialized);
        // - not necessary: the size equality will be checked later in any case
        final Matrix<? extends PArray> some = initialized.isEmpty() ? null : initialized.get(0);
        final long dimX = some == null ? DEFAULT_DIM_X_WHEN_NO_SOURCE : some.dimX();
        final long dimY = some == null ? DEFAULT_DIM_Y_WHEN_NO_SOURCE : some.dimY();
        final Class<? extends PArray> type = some == null ? ByteArray.class : some.type(PArray.class);
        final double scale = some == null ? 255.0 : some.array().maxPossibleValue(1.0);
        if (some == null) {
            logDebug(() -> "All merged channels are empty; creating constant byte matrix " + dimX + "x" + dimY);
        }
        for (int k = 0; k < channels.size(); k++) {
            if (channels.get(k) == null) {
                final double value = scale * defaultFillerForChannels[k];
                channels.set(k, Matrices.constantMatrix(value, type, dimX, dimY));
            }
        }
        final List<Matrix<? extends PArray>> rgba = getChannelsColorSpace().merge(channels);
        getMat().setTo(MultiMatrix.valueOf2DRGBA(rgba).clone());
    }
}
