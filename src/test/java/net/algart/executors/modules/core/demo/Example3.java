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

import net.algart.arrays.ByteArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;

import java.util.AbstractList;
import java.util.List;

public final class Example3 extends Executor {
    private int width = 1000; // must be >100!
    private int height = 1000;
    private Class<?> elementType = byte.class;

    @Override
    public void process() {
        System.out.printf("Creating beautiful test %dx%d%n", width, height);
        final List<Matrix<? extends PArray>> m = new AbstractList<>() {
            @Override
            public Matrix<ByteArray> get(int index) {
                return Matrices.asCoordFuncMatrix(false, new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        if (x[1] < height / 4.0) {
                            return x[0];
                        } else if (x[1] < height / 2.0) {
                            return index > 0 ? 0 : x[0] / 2;
                        } else {
                            return x[0] * x[1] * (index + 1) / 10.0;
                        }
                    }
                }, ByteArray.class, width, height);
            }

            @Override
            public int size() {
                return 3;
            }
        };

        getMat("image").setTo(MultiMatrix.valueOf2DRGBA(m).asPrecision(elementType));
//        throw new AssertionError("some error");
    }

    @Override
    public String visibleOutputPortName() {
        return "image";
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
            case "elementType" -> {
                elementType = MultiMatrixGenerator.elementType(parameters().getString(name));
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: %s target_image%n", Example3.class);
            return;
        }
        @SuppressWarnings("resource")
        Example3 e = new Example3();
        e.addPort(Port.newOutput("image", DataType.MAT));
        e.execute();
    }
}
