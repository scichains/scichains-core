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

package net.algart.executors.api.demo;

import jakarta.json.JsonObject;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.mappings.MappingBuilder;
import net.algart.executors.api.mappings.core.UseMapping;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleMapping {
    public static void main(String[] args) throws IOException {
        boolean builder = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-builder")) {
            builder = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: " +
                            "%s [-builder] some_mapping.map name1 value1 name2 value2 ...",
                    CallSimpleMapping.class.getName());
            return;
        }
        final Path mappingPath = Paths.get(args[startArgIndex]);

        System.out.printf("Loading %s...%n", mappingPath.toAbsolutePath());
        if (builder) {
            final MappingBuilder mappingBuilder = MappingBuilder.read(mappingPath);
            final Parameters parameters = new Parameters();
            for (int i = startArgIndex + 1; i + 1 < args.length; i += 2) {
                final String name = args[i];
                final String value = args[i + 1];
                parameters.setString(name, value);
            }
            final JsonObject resultJson = mappingBuilder.build(parameters);
            System.out.printf("Done: result JSON is%n%s%n", Jsons.toPrettyString(resultJson));
        } else {
            ExecutionBlock.initializeExecutionSystem();
            final ExecutorFactory factory = ExecutorFactory.newSharedFactory();
            try (var executor = UseMapping.newSharedExecutor(factory, mappingPath)) {
                for (int i = startArgIndex + 1; i + 1 < args.length; i += 2) {
                    final String name = args[i];
                    final String value = args[i + 1];
                    executor.setStringParameter(name, value);
                }
                final String result = executor.build();
                System.out.printf("%s%nDone: result is%n%s%n", executor, result);
                CallSimpleChainForImage.printExecutorInterface(executor);
            }
        }
    }
}
