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

package net.algart.executors.api.demo;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.mappings.UseMapping;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleMapping {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: " +
                            "%s some_mapping.map name1 value1 name2 value2 ...",
                    CallSimpleMapping.class.getName());
            return;
        }
        final Path mappingPath = Paths.get(args[0]);

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", mappingPath.toAbsolutePath());
        try (var executor = UseMapping.newSharedExecutor(mappingPath)) {
            for (int i = 1; i + 1 < args.length; i += 2) {
            final String name = args[i];
            final String value = args[i + 1];
            executor.setStringParameter(name, value);
            }
            final String result = executor.build();
            System.out.printf("%s%nDone: result is%n%s%n", executor, result);
            CallSimpleChain.printExecutorInterface(executor);
        }
    }
}
