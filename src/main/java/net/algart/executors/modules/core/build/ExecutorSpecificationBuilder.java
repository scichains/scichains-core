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

package net.algart.executors.modules.core.build;

import net.algart.executors.api.Executor;
import net.algart.executors.api.system.ExecutorSpecification;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class ExecutorSpecificationBuilder {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.printf("Usage: %s ExecutorClassName result_executor_model.json%n",
                    ExecutorSpecificationBuilder.class.getName());
            return;
        }

        final String className = args[0];
        final Path resultFile = Paths.get(args[1]);
        final Executor executor = (Executor) Class.forName(className).getDeclaredConstructor().newInstance();
        final ExecutorSpecification executorSpecification = ExecutorSpecification.valueOf(executor, UUID.randomUUID().toString());
        executorSpecification.write(resultFile, StandardOpenOption.CREATE_NEW);
        System.out.printf("JSON model for %s successfully stored in %s%n", executor.getClass(), resultFile);
        executor.close();
    }
}
