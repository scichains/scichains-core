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

package net.algart.executors.api.model.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.model.ExecutorJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExecutorJsonTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 4) {
            System.out.printf("Usage: %s executor_model.json result_1.json result_2.json result_3.json%n",
                    ExecutorJson.class.getName());
            return;
        }
        final Path modelFile = Paths.get(args[0]);
        final Path resultFile1 = Paths.get(args[1]);
        final Path resultFile2 = Paths.get(args[2]);
        final Path resultFile3 = Paths.get(args[3]);
        ExecutorJson model = ExecutorJson.read(modelFile);
//        ExecutorJson model = ExecutorJson.valueOf(Jsons.readJson(modelFile)); // - for testing null file
        model.write(resultFile1);
        System.out.printf("Java configuration:%n");
        System.out.println(model.minimalConfigurationJsonString());
        System.out.printf("%nFull model:%n");
        System.out.println(model);
        System.out.printf("%nExecutor object:%n");
        Thread.sleep(100);
        final ExecutionBlock executionBlock;
        try {
            executionBlock = ExecutionBlock.newExecutionBlock(
                    null, model.getExecutorId(), model.minimalConfigurationJsonString());
            Thread.sleep(100);
            System.out.println(executionBlock);
            if (executionBlock instanceof Executor) {
                model.setTo((Executor) executionBlock);
                model.write(resultFile2);
                System.out.printf("%nReloaded full model:%n");
                System.out.println(model);

                model = ExecutorJson.valueOf((Executor) executionBlock, "12345678");
                model.write(resultFile3);
                System.out.printf("%nModel, created from executor:%n");
                System.out.println(model);
            }
        } catch (ClassNotFoundException e) {
            System.out.printf("Cannot load required class: %s%n", e);
        }
    }
}
