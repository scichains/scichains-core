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
import net.algart.executors.api.settings.UseSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleSettings {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.printf("Usage: " +
                            "%s some_settings.ss [a b str]%n" +
                            "some_settings.ss should be some settings specification, which has" +
                            " 3 parameters named a, b and str.",
                    CallSimpleSettings.class.getName());
            return;
        }
        final Path settingsPath = Paths.get(args[0]);
        final String parameterA = args[1];
        final String parameterB = args[2];
        final String parameterStr = args[3];

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", settingsPath.toAbsolutePath());
        try (var executor = UseSettings.newSharedCombine(settingsPath)) {
            executor.setStringParameter("a", parameterA);
            executor.setStringParameter("b", parameterB);
            executor.setStringParameter("str", parameterStr);
            final String result = executor.combine();
            System.out.printf("%s%nDone: result is%n%s%n", executor, result);
            CallSimpleChain.printExecutorInterface(executor);
        }
    }
}
