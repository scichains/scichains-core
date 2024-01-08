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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.Executor;

/**
 * Simple java test number arrays ports
 * Created by semen on 12.10.2017.
 */
public final class SNumbersTest extends Executor {
    @Override
    public void process() {
        Data in = getInputPort("in").getData();
        SNumbers data;
        if (in == null) {
            data = new SNumbers();
            data.setTo(new float[]{1,2,3,4,5}, 1);
            in = data;
        } else if (!in.isInitialized()) {
            data = (SNumbers) in;
            data.setTo(new int[]{1,2,3,4,5}, 1);
        } else {
            data = (SNumbers)in;
            if (data.isIntArray()) {
                int []ints = (int[]) data.getArray();
                for (int i = 0; i < ints.length; ++i) {
                    ints[i] *= 2;
                }
            } else if (data.isFloatArray()) {
                float []floats = (float[]) data.getArray();
                for (int i = 0; i < floats.length; ++i) {
                    floats[i] *= 2;
                }

            } else if (data.isLongArray()) {
                long []longs = (long[]) data.getArray();
                for (int i = 0; i < longs.length; ++i) {
                    longs[i] *= 2;
                }
            } else if (data.isDoubleArray()) {
                double []doubles = (double[]) data.getArray();
                for (int i = 0; i < doubles.length; ++i) {
                    doubles[i] *= 2;
                }
            }
        }

        (getOutputPort("out").getData(SNumbers.class, true)).setTo(data);
    }

    @Override
    public String visibleOutputPortName() {
        return "out";
    }
}
