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

import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.concurrent.atomic.AtomicInteger;

public class ExampleGraalJS extends Executor {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public ExampleGraalJS() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        getScalar().setTo(testGraal(getInputScalar(true).getValue()));
    }

    public Object testGraal(String value) {
        Value result;
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .hostClassLoader(ExampleGraalJS.class.getClassLoader())
                // - necessary to work via JNI
                .allowHostClassLookup(className -> {
                    System.out.println("cccc Checking " + className);
                    return true;
                })
                .build()) {

            result = context.eval("js",
                    "print('Hello');\n" +
                            "Java.type('" + SNumbers.class.getName() + "').zeros(Java.type('int'), 50, 2) + \n" +
                            "'  aa'");
            return value == null ?
                    result + "\n" +
                            "Number of stored performers: " + GraalPerformerContainer.numberOfStoredPerformers():
                    value;
        }
    }

    public static void main(String[] args) {
        try (ExampleGraalJS test = new ExampleGraalJS()) {
            final Object  result = test.testGraal(null);
            System.out.println();
            System.out.println(result);
        }
    }
}
