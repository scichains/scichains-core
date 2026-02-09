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

package net.algart.executors.api.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.ExecutorSpecificationSet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;

public class ExecutorSpecificationSetTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int startArgIndex = 0;
        boolean resolve = false;
        boolean create = false;
        boolean writeDebugFiles = false;
        if (args.length > startArgIndex && args[startArgIndex].equals("--resolve")) {
            resolve = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("--create")) {
            create = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("--write_debug_files")) {
            writeDebugFiles = true;
            startArgIndex++;
        }

        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: %s executors_folder%n", ExecutorSpecificationSet.class.getName());
            return;
        }

        final Path executorsFolder = Paths.get(args[startArgIndex]);
        final Runtime rt = Runtime.getRuntime();
        System.out.printf("Used memory %.2f/%.2f GB%n",
                (rt.totalMemory() - rt.freeMemory()) * 1e-9, rt.maxMemory() * 1e-9);

        ExecutorSpecificationSet executorSpecificationSet = null;
        for (int test = 1; test <= 5; test++) {
            System.gc();
            System.out.printf("Test #%d...%n", test);
            System.out.printf("  Reading %s...%n", executorsFolder);
            long t1 = System.nanoTime();
            executorSpecificationSet = ExecutorSpecificationSet.newInstance();
            executorSpecificationSet.addFolder(executorsFolder, false);
            long t2 = System.nanoTime();
            final Collection<ExecutorSpecification> specifications = executorSpecificationSet.all();
            System.out.printf(Locale.US,
                    "  %d specifications loaded in %.3f ms (%.5f mcs/executor); used memory %.2f/%.2f MB%n",
                    specifications.size(), (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / specifications.size(),
                    (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
            if (resolve) {
                t1 = System.nanoTime();
                for (ExecutorSpecification specification : specifications) {
                    if (specification.isJavaExecutor()) {
                        specification.getJava().resolveSupportedExecutor();
                    }
                }
                t2 = System.nanoTime();
                System.out.printf(Locale.US,
                        "  Java classes of executors resolved in %.3f ms "
                                + "(%.5f mcs/executor); used memory %.2f/%.2f MB%n",
                        (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / specifications.size(),
                        (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
            }
            System.out.println();
        }

        if (create) {
            for (int test = 1; test <= 10; test++) {
                System.out.printf("Creation test #%d...%n", test);
                final Collection<ExecutorSpecification> specifications = executorSpecificationSet.all();
                long t1 = System.nanoTime();
                for (ExecutorSpecification specification : specifications) {
                    if (specification.isJavaExecutor()) {
//                    try {
//                        specification.getJavaConf().getClass().newInstance();
//                    } catch (Exception e) {
//                    }
                        final String javaConfiguration = specification.minimalSpecification();
                        //noinspection resource
                        ExecutionBlock.newExecutor(null, javaConfiguration);
                    }
                }
                long t2 = System.nanoTime();
                System.out.printf(Locale.US,
                        "  Executors created in %.3f ms "
                                + "(%.5f mcs/model); used memory %.2f/%.2f MB%n",
                        (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / specifications.size(),
                        (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
                System.out.println();
            }
            for (ExecutorSpecification specification : executorSpecificationSet.all()) {
                if (specification.isJavaExecutor()) {
                    System.out.printf("Creating executor %s.%s...%n",
                            specification.getCategory(), specification.getName());
                    final ExecutionBlock executionBlock = ExecutionBlock.newExecutor(
                            null, specification.minimalSpecification());
                    System.out.printf("  %s [id=%s]%n", executionBlock, executionBlock.getExecutorId());
                }
            }
        }

        if (writeDebugFiles) {
            System.out.printf("  Writing xxx__specification.json.files...%n");
            for (ExecutorSpecification specification : executorSpecificationSet.all()) {
                final String jsonString = specification.jsonString();
                final Path resultFile = Paths.get(
                        specification.getSpecificationFile() + "__specification.json");
                java.nio.file.Files.writeString(resultFile, jsonString);
            }
        }
        System.out.println("Done");
    }
}
