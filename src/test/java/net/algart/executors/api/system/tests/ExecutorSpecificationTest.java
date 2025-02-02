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

package net.algart.executors.api.system.tests;

import jakarta.json.JsonObject;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.system.ExecutorSpecification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExecutorSpecificationTest {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 4) {
            System.out.printf("Usage: %s executor_specification.json " +
                            "result_1.json result_2.json result_3.json%n",
                    ExecutorSpecification.class.getName());
            return;
        }
        final Path specificationFile = Paths.get(args[0]);
        final Path resultFile1 = Paths.get(args[1]);
        final Path resultFile2 = Paths.get(args[2]);
        final Path resultFile3 = Paths.get(args[3]);
        ExecutorSpecification specification = ExecutorSpecification.read(specificationFile);
//        ExecutorSpecification specification = ExecutorSpecification.valueOf(Jsons.readJson(specificationFile));
// - for testing null file
        specification.write(resultFile1);
        System.out.printf("Java configuration:%n");
        System.out.println(specification.minimalSpecification());
        System.out.printf("%nFull specification:%n");
        System.out.println(specification);
        System.out.printf("%nExecutor object:%n");
        Thread.sleep(100);
        ExecutionBlock executionBlock;
        try {
            executionBlock = ExecutionBlock.newExecutor(null, specification);
            Thread.sleep(100);
            System.out.println(executionBlock);
            if (executionBlock instanceof Executor) {
                specification.setTo((Executor) executionBlock);
                specification.write(resultFile2);
                System.out.printf("%nReloaded full specification:%n");
                System.out.println(specification);

                specification = ExecutorSpecification.of((Executor) executionBlock, "12345678");
                specification.write(resultFile3);
                System.out.printf("%nSpecification, created from executor:%n");
                System.out.println(specification);

                System.out.printf("%nExecutor specification:%n");
                System.out.println(executionBlock.getExecutorSpecification());
            }
            ExecutionBlock block1 = null;
            ExecutionBlock block2 = null;
            for (int test = 1; test <= 16; test++) {
                System.out.printf("Timing test %d...%n", test);
                final int n = 1000;
                JsonObject json = null;
                String minimal = null;
                long t1 = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    json = specification.toJson();
                }
                long t2 = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    minimal = specification.minimalSpecification();
                }
                long t3 = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    block1 = ExecutionBlock.newExecutor(null, specification);
                }
                long t4 = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    block2 = ExecutionBlock.newExecutor(null, minimal);
                }
                long t5 = System.nanoTime();
                System.out.printf("ExecutorSpecification.toJson(): %.3f mcs%n", (t2 - t1) * 1e-3 / n);
                System.out.printf("ExecutorSpecification.minimalSpecification(): %.3f mcs%n", (t3 - t2) * 1e-3 / n);
                System.out.printf("Creating execution block from specification: %.3f mcs%n", (t4 - t3) * 1e-3 / n);
                System.out.printf("Creating execution block from minimal string:  %.3f mcs%n", (t5 - t4) * 1e-3 / n);
            }
            System.out.printf("Full execution block:%n%s%n%n", block1.getExecutorSpecification().jsonString());
            System.out.printf("Minimal execution block:%n%s%n%n", block2.getExecutorSpecification().jsonString());
        } catch (ClassNotFoundException e) {
            System.out.printf("Cannot load required class: %s%n", e);
        }
    }
}
