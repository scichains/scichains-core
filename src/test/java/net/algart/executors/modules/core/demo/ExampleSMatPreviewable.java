/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.executors.api.Previewable;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SScalar;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Daniel on 29/06/2017.
 */
public final class ExampleSMatPreviewable extends Executor implements Previewable {
    private int width = 100;
    private int height = 100;

    private final AtomicLong executeCount = new AtomicLong();
    private final AtomicLong previewCount = new AtomicLong();

    public ExampleSMatPreviewable() {
        setDefaultOutputMat("output");
    }

    @Override
    public void process() {
        assert width > 10; // - checking processing "assert" statement by external system
        System.out.println("execute properties.color = " + parameters().getString("color"));

        Port output = getOutputPort("output");

        SMat m = (SMat) output.getData();
        System.out.println("ExampleSMatPreviewable: execute #" + executeCount.incrementAndGet());
        createResult(m);
    }

    @Override
    public Data createPreview() {
        System.out.println("preview properties.color = " + parameters().getString("color"));
        System.out.println("ExampleSMatPreviewable: preview #" + previewCount.incrementAndGet());
        return width <= 100 && height <= 100 ?
                SScalar.of("Small matrix") :
                getOutputPort("output").getData();
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
        }
    }

    private void createResult(SMat m) {
        final Color color = Color.decode(parameters().getString("color"));
        System.out.println("Filling by " + color);
        int channels = 3;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(height * width * channels);
        byteBuffer.position(0);

        m.setAll(new long[]{width, height}, SMat.Depth.U8, channels, byteBuffer, false);

        for (int i = 0; i < byteBuffer.capacity() / 3; i++) {
            int r = (byte) i * color.getRed() / 255;
            int g = (byte) i * color.getGreen() / 255;
            int b = (byte) i * color.getBlue() / 255;
            byteBuffer.put((byte) b);
            byteBuffer.put((byte) g);
            byteBuffer.put((byte) r);
        }
    }
}
