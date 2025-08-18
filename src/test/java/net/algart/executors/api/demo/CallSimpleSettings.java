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

import jakarta.json.JsonObject;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.core.UseSettings;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleSettings {
    public static void main(String[] args) throws IOException {
        boolean builder = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-builder")) {
            builder = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 4) {
            System.out.printf("Usage: " +
                            "%s [-builder] some_settings.ss [a b str]%n" +
                            "some_settings.ss should be some settings specification, which has" +
                            " 3 parameters named a, b and str.",
                    CallSimpleSettings.class.getName());
            return;
        }
        final Path settingsPath = Paths.get(args[startArgIndex]);
        final String parameterA = args[startArgIndex + 1];
        final String parameterB = args[startArgIndex + 2];
        final String parameterStr = args[startArgIndex + 3];

        System.out.printf("Loading %s...%n", settingsPath.toAbsolutePath());
        if (builder) {
            final SettingsBuilder settingsBuilder = SettingsBuilder.read(settingsPath);
            final Parameters parameters = new Parameters()
                    .setString("a", parameterA)
                    .setString("b", parameterB)
                    .setString("str", parameterStr);
            final JsonObject resultJson = settingsBuilder.build(parameters);
            System.out.printf("Done: result JSON is%n%s%n", Jsons.toPrettyString(resultJson));
        } else {
            ExecutionBlock.initializeExecutionSystem();
            final ExecutorFactory factory = ExecutorFactory.newSharedFactory();
            try (var executor = UseSettings.newSharedExecutor(factory, settingsPath)) {
                executor.setStringParameter("a", parameterA);
                executor.setStringParameter("b", parameterB);
                executor.setStringParameter("str", parameterStr);
                final String result = executor.combine();
                System.out.printf("%s%nDone: result is%n%s%n", executor, result);
                CallSimpleChainForImage.printExecutorInterface(executor);
            }
        }
    }
}
