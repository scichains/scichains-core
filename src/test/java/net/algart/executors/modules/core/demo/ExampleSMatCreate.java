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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SMat;

public final class ExampleSMatCreate extends Executor {
    private int width = 10;
    private int height = 10;
    private int channels = 1;

    public ExampleSMatCreate() {
    }

    @Override
    public void process() {
        Port output = getRequiredOutputPort("output");
        SMat outMat = output.getData(SMat.class, true);

        // outMat = getMat();
        // - more brief equivalent of 2 previous "low-level" operators

        byte[] byteArray = new byte[height * width * channels];
        for (int i = 0; i < byteArray.length; i++) {
            int c = i % channels;
            byteArray[i] = (byte) (i * (c + 1));
        }
        outMat.setAll(
                new long[]{width, height},
                SMat.Depth.U8, channels, byteArray);
        outMat.setInitialized(true);
    }

    @Override
    public void onChangeParameter(String name) {
        switch (name) {
            case "width" -> {
                width = parameters().getInteger(name);
            }
            case "height" -> {
                height = parameters().getInteger(name);
            }
            case "channels" -> {
                channels = parameters().getInteger(name);
            }
        }
    }
}
