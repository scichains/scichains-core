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

package net.algart.executors.modules.core.common;

import net.algart.multimatrix.MultiMatrix2D;

import java.awt.*;

public interface ChannelOperation {
    int currentChannel();

    int numberOfChannels();

    default double colorChannel(double[] rgb) {
        return colorChannel(rgb, currentChannel(), numberOfChannels());
    }

    static double colorChannel(double[] rgb, int channel, int numberOfChannels) {
        return colorChannel(rgb, channel, numberOfChannels, 0.0);
    }

    static double colorChannel(double[] rgb, int channel, int numberOfChannels, double defaultForExtraChannels) {
        return correctLittleRoundingError(numberOfChannels == 1 ? intensity(rgb[0], rgb[1], rgb[2])
                : channel == MultiMatrix2D.DEFAULT_R_CHANNEL ? rgb[0]
                : channel == MultiMatrix2D.DEFAULT_G_CHANNEL ? rgb[1]
                : channel == MultiMatrix2D.DEFAULT_B_CHANNEL ? rgb[2]
                : channel == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL ? rgb[3]
                : defaultForExtraChannels);
    }

    default double colorChannel(String s, double maxPossibleValue) {
        return colorChannel(s, maxPossibleValue, currentChannel(), numberOfChannels());
    }

    static double colorChannel(String s, double maxPossibleValue, int currentChannel, int numberOfChannels) {
        return colorChannel(decodeRGBA(s, maxPossibleValue), currentChannel, numberOfChannels);
    }

    default double colorChannel(Color color, double maxPossibleValue) {
        return colorChannel(color, maxPossibleValue, currentChannel(), numberOfChannels());
    }

    static double colorChannel(Color color, int channel, int numberOfChannels) {
        return numberOfChannels == 1 ?
                intensity(color.getRed(), color.getGreen(), color.getBlue()) / 255.0
                : channel == MultiMatrix2D.DEFAULT_R_CHANNEL ? color.getRed() / 255.0
                : channel == MultiMatrix2D.DEFAULT_G_CHANNEL ? color.getGreen() / 255.0
                : channel == MultiMatrix2D.DEFAULT_B_CHANNEL ? color.getBlue() / 255.0
                : channel == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL ? color.getAlpha() / 255.0
                : 0.0;
    }

    static double colorChannel(Color color, double maxPossibleValue, int currentChannel, int numberOfChannels) {
        return correctLittleRoundingError(colorChannel(color, currentChannel, numberOfChannels)
                * maxPossibleValue);
    }

    static double[] decodeRGBA(String s) {
        return decodeRGBA(s, 1.0);
    }

    /**
     * Returns 4 channel values.
     * <p>Note: if 4th value (alpha) is not specified in the string, it will be equal to <tt>scale</tt>.
     *
     * @param s     some color.
     * @param scale the decoded values 0.0..1.0., if they represent color, are multiplied by this scale
     *              (for example, 255.0 or 65535.0)
     * @return decoded channels (the length of returned array is always 4).
     */
    static double[] decodeRGBA(String s, double scale) {
        s = s.trim();
        final boolean rawChannels = s.startsWith("[") && s.endsWith("]");
        final boolean normalizedChannels = s.startsWith("(") && s.endsWith(")");
        if (rawChannels || normalizedChannels) {
            // (0.5, 1.0, 1.0) or [128, 255, 128]
            final double defaultAlpha = scale;
            if (rawChannels) {
                // [128, 255, 128]
                scale = 1.0;
            }
            final String[] values = s.substring(1, s.length() - 1).split("[, ]+");
            if (values.length == 0) {
                // - to be on the safe side
                return new double[]{0.0, 0.0, 0.0, scale};
            }
            final double[] result = new double[4];
            for (int k = 0; k < result.length; k++) {
                if (k >= values.length) {
                    result[k] = result[values.length - 1];
                } else {
                    final String v = values[k].trim();
                    result[k] = v.isEmpty() ? 0.0 : scale * Double.parseDouble(v);
                }
            }
            if (values.length < 4) {
                result[3] = defaultAlpha;
                // alpha: fully opacity
            }
            return result;
        }
        final boolean numberSign = s.startsWith("#");
        final boolean hexNumber = s.startsWith("0x") || s.startsWith("0X");
        if ((numberSign || hexNumber)) {
            final boolean isRgba = numberSign && s.length() == 9;
            long i = Long.decode(s);
            final int alpha;
            if (isRgba) {
                alpha = (int) i & 0xFF;
                i >>>= 8;
            } else {
                alpha = 0xFF;
            }
            final int r = (int) (i >>> 16) & 0xFF;
            final int g = (int) (i >>> 8) & 0xFF;
            final int b = (int) i & 0xFF;
            // - Note: we allow to specify alpha in this format ONLY with help of 8 hexadecimal digits after #,
            // and it is RRGGBBAA, NOT AARRGGBB!
            // In other words:
            //     #FF0000 means RED with alpha=255,
            //     #00FF0000 means GREEN with alpha=0,
            //     0x00FF0000 = 0xFF0000 means RED with alpha=255.
            // It helps to avoid possible mistake: alpha = 0, probable in AARRGGBB format
            // (mathematically, 00FF0000 = FF0000 in hexadecimal system).
            // It is important, because in other case we will produce zero-alpha by mistake.
            // Zero-alpha pixels work fine in AlgART and OpenCV functions, but may lead to problems
            // while using Java AWT: attempt to draw such image on Graphics2D will not modify R, G, B
            // components of the result, if alpha=0 in the source. As a result, in particular,
            // SMat.bufferedImageToPackedBGRA method will work incorrectly with such BufferedImage.
            return new double[]{
                    scale * (double) r / 255d,
                    scale * (double) g / 255d,
                    scale * (double) b / 255d,
                    scale * (double) alpha / 255d};
        }
        // scaled single value
        return decodeRGBAForSingleNumber(Double.parseDouble(s), scale);
    }

    static double[] decodeRGBAForSingleNumber(double value) {
        return new double[]{value, value, value, 1.0};
    }

    static double[] decodeRGBAForSingleNumber(double value, double scale) {
        value *= scale;
        return new double[]{value, value, value, scale};
    }

    static double intensity(double r, double g, double b) {
        if (r == g && r == b) {
            // important: the formula below may lead to little error here
            return r;
        } else {
            return 0.299 * r + 0.587 * g + 0.114 * b;
        }
    }

    static double correctLittleRoundingError(double probablyInteger) {
        final long round = StrictMath.round(probablyInteger);
        return Math.abs(round) >= 1 && Math.abs(probablyInteger - round) < 1e-7 ? round : probablyInteger;
    }
}
