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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.Port;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.Executor;

import java.nio.ByteBuffer;

/**
 * @author mnogono
 *         Created on 20.06.2017.
 */
public final class Example1 extends Executor {
    private int width = 10;
    private int height = 10;
    private byte channels = 1;

    public Example1() {
//        throw new AssertionError("Hmm...");
    }

    @Override
    public void process() {
        Port output = getRequiredOutputPort("image");

        SMat outMat = (SMat) output.getData();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(height * width * channels);
        byteBuffer.position(0);

        outMat.setAll(new long[] {width, height}, SMat.Depth.U8, channels, byteBuffer, false);
        for (int i = 0; i < byteBuffer.capacity(); ++i) {
            byteBuffer.put((byte)i);
        }
        outMat.setInitialized(true);
    }

    @Override
    public String visibleOutputPortName() {
        return "image";
    }

    @Override
    public void onChangeParameter(String name) {
        switch (name) {
            case "width": {
                width = parameters().getInteger(name);
                break;
            }
            case "height": {
                height = parameters().getInteger(name);
                break;
            }
        }
    }
}
