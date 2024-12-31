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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.SMat;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public final class SplitColorChannels extends ColorSpaceConversion {
    public static final String CHANNEL_1 = "channel_1";
    public static final String CHANNEL_2 = "channel_2";
    public static final String CHANNEL_3 = "channel_3";
    public static final String CHANNEL_4 = "channel_4";

    public static final String VISIBLE_RESULT_IS_FIRST_CONNECTED = "first_connected";

    public static final List<String> CHANNEL_PORTS = List.of(CHANNEL_1, CHANNEL_2, CHANNEL_3, CHANNEL_4);

    public SplitColorChannels() {
        useVisibleResultParameter();
        addInputMat(DEFAULT_INPUT_PORT);
        for (String port : CHANNEL_PORTS) {
            addOutputMat(port);
        }
    }

    @Override
    public void process() {
        boolean somethingNecesary = false;
        for (String port : CHANNEL_PORTS) {
            somethingNecesary |= isOutputNecessary(port);
        }
        if (!somethingNecesary) {
            return;
        }
        SMat input = getInputMat();
        final MultiMatrix2D source = input.toMultiMatrix2D();
        List<Matrix<? extends PArray>> rgba = source.allChannelsInRGBAOrder();
        if (rgba.size() == 1) {
            rgba = Matrices.several(PArray.class, rgba.get(0), rgba.get(0), rgba.get(0));
            // - make R=G=B
        }
        final List<Matrix<? extends PArray>> channels = getChannelsColorSpace().split(rgba);
        int channelIndex = 0;
        for (String port : CHANNEL_PORTS) {
            if (channelIndex >= channels.size()) {
                break;
            }
            final int finalIndex = channelIndex;
            if (isOutputNecessary(port)) {
                final SMat outputPortMat = getMat(port);
                logDebug(() -> "Extracting channel #" + (finalIndex + 1) + "/" + channels.size() + " -> "
                        + "monochrome matrix " + source.dimX() + "x" + source.dimY());
                outputPortMat.setTo(MultiMatrix.valueOf2DMono(channels.get(channelIndex)).clone());
            } else {
                logDebug(() -> "(extracting channel #" + (finalIndex + 1) + "/" + channels.size()
                        + " skipped: it is not visible and not connected to any other processing)");
            }
            channelIndex++;
        }
    }

    @Override
    public String visibleOutputPortName() {
        final String visibleResult = super.visibleOutputPortName();
        if (VISIBLE_RESULT_IS_FIRST_CONNECTED.equals(visibleResult)) {
            for (String port : CHANNEL_PORTS) {
                final Port outputPort = getOutputPort(port);
                if (outputPort != null && outputPort.isConnected()) {
                    return port;
                }
            }
            return null;
        } else {
            return visibleResult;
        }
    }
}
