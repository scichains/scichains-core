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
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExecutorJsonSet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;

public class ExecutorJsonSetTest {
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
            System.out.printf("Usage: %s executors_folder%n", ExecutorJsonSet.class.getName());
            return;
        }

        final Path modelFolder = Paths.get(args[startArgIndex]);
        final Runtime rt = Runtime.getRuntime();
        System.out.printf("Used memory %.2f/%.2f GB%n",
                (rt.totalMemory() - rt.freeMemory()) * 1e-9, rt.maxMemory() * 1e-9);

        ExecutorJsonSet executorJsonSet = null;
        for (int test = 1; test <= 5; test++) {
            System.gc();
            System.out.printf("Test #%d...%n", test);
            System.out.printf("  Reading %s...%n", modelFolder);
            long t1 = System.nanoTime();
            executorJsonSet = ExecutorJsonSet.newInstance();
            executorJsonSet.addFolder(modelFolder, false);
            long t2 = System.nanoTime();
            final Collection<ExecutorJson> models = executorJsonSet.all();
            System.out.printf(Locale.US,
                    "  %d models loaded in %.3f ms (%.5f mcs/model); used memory %.2f/%.2f MB%n",
                    models.size(), (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / models.size(),
                    (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
            if (resolve) {
                t1 = System.nanoTime();
                for (ExecutorJson model : models) {
                    if (model.isJavaExecutor()) {
                        model.getJava().resolveSupportedExecutor();
                    }
                }
                t2 = System.nanoTime();
                System.out.printf(Locale.US,
                        "  Java classes of executors resolved in %.3f ms "
                                + "(%.5f mcs/model); used memory %.2f/%.2f MB%n",
                        (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / models.size(),
                        (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
            }
            System.out.println();
        }

        if (create) {
            for (int test = 1; test <= 10; test++) {
                System.out.printf("Creation test #%d...%n", test);
                final Collection<ExecutorJson> models = executorJsonSet.all();
                long t1 = System.nanoTime();
                for (ExecutorJson model : models) {
                    if (model.isJavaExecutor()) {
//                    try {
//                        model.getJavaConf().getClass().newInstance();
//                    } catch (Exception e) {
//                    }
                        final String id = model.getExecutorId();
                        final String javaConfiguration = model.minimalConfigurationJsonString();
                        ExecutionBlock.newExecutionBlock(null, id, javaConfiguration);
                    }
                }
                long t2 = System.nanoTime();
                System.out.printf(Locale.US,
                        "  Executors created in %.3f ms "
                                + "(%.5f mcs/model); used memory %.2f/%.2f MB%n",
                        (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / models.size(),
                        (rt.totalMemory() - rt.freeMemory()) * 1e-6, rt.maxMemory() * 1e-6);
                System.out.println();
            }
            for (ExecutorJson model : executorJsonSet.all()) {
                if (model.isJavaExecutor()) {
                    System.out.printf("Creating executor %s.%s...%n", model.getCategory(), model.getName());
                    final ExecutionBlock executionBlock = ExecutionBlock.newExecutionBlock(
                            null, model.getExecutorId(), model.minimalConfigurationJsonString());
                    System.out.printf("  %s [id=%s]%n", executionBlock, executionBlock.getExecutorId());
                }
            }
        }

        if (writeDebugFiles) {
            System.out.printf("  Writing xxx__model.json.files...%n");
            for (ExecutorJson model : executorJsonSet.all()) {
                final String jsonString = model.jsonString();
                final Path resultFile = Paths.get(model.getExecutorJsonFile() + "__model.json");
                java.nio.file.Files.writeString(resultFile, jsonString);
            }
        }
        System.out.println("Done");
    }
}
