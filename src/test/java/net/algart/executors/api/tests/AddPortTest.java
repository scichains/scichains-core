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

package net.algart.executors.api.tests;

import net.algart.executors.api.Executor;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.SScalar;

import java.util.UUID;

public class AddPortTest {
    public static void main(String[] args) {
        final Executor executor = new Executor() {
            @Override
            public void process() {
            }
        };
        boolean a;
        a = executor.addPort(Port.newInput("input", DataType.SCALAR)
                .setData(new SScalar().setTo("Some")));
        System.out.printf("Adding input: %s%n", a);
        a = executor.addPort(Port.newOutput("output", DataType.MAT));
        System.out.printf("Adding output: %s%n", a);
        System.out.printf("Ports:%n   %s%n   %s%n", executor.allInputPorts(), executor.allOutputPorts());
        a = executor.addPort(Port.newInput("input", DataType.SCALAR));
        System.out.printf("Adding input: %s%n", a);
        a = executor.addPort(Port.newOutput("output", DataType.SCALAR));
        System.out.printf("Adding output: %s%n", a);
        a = executor.addPort(Port.newOutput("output2", DataType.MAT).setUuid(UUID.randomUUID()));
        System.out.printf("Adding output2: %s%n", a);
        System.out.printf("Ports:%n   %s%n   %s%n", executor.allInputPorts(), executor.allOutputPorts());
    }
}
