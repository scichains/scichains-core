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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.external.ImageConversions;

import java.util.List;

public enum ChannelsColorSpace {
    RGBA {
        @Override
        List<Matrix<? extends PArray>> split(List<Matrix<? extends PArray>> rgba) {
            return rgba;
        }

        @Override
        List<Matrix<? extends PArray>> merge(List<Matrix<? extends PArray>> channels) {
            return channels;
        }
    },
    HSV {
        @Override
        List<Matrix<? extends PArray>> split(List<Matrix<? extends PArray>> rgba) {
            Class<? extends PArray> resultType = rgba.get(0).type(PArray.class);
            //Note: in future we will maybe return another element type.
            return Matrices.several(
                PArray.class,
                ImageConversions.asHue(resultType, rgba.get(0), rgba.get(1), rgba.get(2)),
                ImageConversions.asHSVSaturation(resultType, rgba.get(0), rgba.get(1), rgba.get(2)),
                ImageConversions.asHSVValue(resultType, rgba.get(0), rgba.get(1), rgba.get(2)));
        }

        @Override
        List<Matrix<? extends PArray>> merge(List<Matrix<? extends PArray>> channels) {
            return ImageConversions.asRGBFromHSV(
                channels.get(0).type(PArray.class), channels.get(0), channels.get(1), channels.get(2));
        }
    },
    HLS {
        @Override
        List<Matrix<? extends PArray>> split(List<Matrix<? extends PArray>> rgba) {
            Class<? extends PArray> resultType = rgba.get(0).type(PArray.class);
            return Matrices.several(
                PArray.class,
                ImageConversions.asHue(resultType, rgba.get(0), rgba.get(1), rgba.get(2)),
                ImageConversions.asHSLLightness(resultType, rgba.get(0), rgba.get(1), rgba.get(2)),
                ImageConversions.asHSLSaturation(resultType, rgba.get(0), rgba.get(1), rgba.get(2)));
        }

        @Override
        List<Matrix<? extends PArray>> merge(List<Matrix<? extends PArray>> channels) {
            return ImageConversions.asRGBFromHSL(
                channels.get(0).type(PArray.class),
                    channels.get(0),    // Hue
                    channels.get(2),    // Saturation at index 2 in HLS
                    channels.get(1));   // Lightness at index 1 in HLS
        }
    };

    abstract List<Matrix<? extends PArray>> split(List<Matrix<? extends PArray>> rgba);

    abstract List<Matrix<? extends PArray>> merge(List<Matrix<? extends PArray>> channels);
}
