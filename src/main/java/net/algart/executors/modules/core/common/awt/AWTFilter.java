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

package net.algart.executors.modules.core.common.awt;

import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.Executor;

import java.awt.image.BufferedImage;

public abstract class AWTFilter extends Executor {
    private boolean convertMonoToColor = false;

    protected AWTFilter() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public boolean isConvertMonoToColor() {
        return convertMonoToColor;
    }

    public AWTFilter setConvertMonoToColor(boolean convertMonoToColor) {
        this.convertMonoToColor = convertMonoToColor;
        return this;
    }


    @Override
    public final void process() {
        SMat input = getInputMat();
        if (convertMonoToColor && input.getNumberOfChannels() == 1) {
            MultiMatrix2D source = input.toMultiMatrix2D().asOtherNumberOfChannels(3);
            if (source.bitsPerElement() == 1) {
                source = source.asPrecision(byte.class);
            }
            input = SMat.valueOf(source);
        }
        final BufferedImage source = input.toBufferedImage();
        final BufferedImage target = process(source);
        getMat().setTo(target);
    }

    public abstract BufferedImage process(BufferedImage source);
}