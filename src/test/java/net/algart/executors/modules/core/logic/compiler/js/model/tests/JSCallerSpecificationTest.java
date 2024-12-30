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

package net.algart.executors.modules.core.logic.compiler.js.model.tests;

import net.algart.executors.modules.core.logic.compiler.js.model.JSCallerSpecification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSCallerSpecificationTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2) {
            System.out.printf("Usage: %s js_executor_model.json result.json%n",
                    JSCallerSpecificationTest.class.getName());
            return;
        }
        final Path modelFile = Paths.get(args[0]);
        final Path resultFile = Paths.get(args[1]);
        JSCallerSpecification specification = JSCallerSpecification.read(modelFile);
        specification.write(resultFile);
        System.out.printf("JS configuration:%n");
        System.out.println(specification.getJS());
    }
}