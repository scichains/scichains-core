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
import net.algart.executors.api.multichains.MultiChainExecutor;
import net.algart.executors.api.multichains.UseMultiChain;
import net.algart.executors.api.settings.CombineSettings;
import net.algart.executors.api.system.InstantiationMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleMultiChain {
    private static void customizeViaParameters(MultiChainExecutor executor, String variant, String a, String b) {
        executor.selectChainVariant(variant);
        executor.setStringParameter("a", a);
        executor.setStringParameter("b", b);
    }

    private static void customizeViaJson(MultiChainExecutor executor, String variant, String a, String b) {
        final var combiner = executor.newCombine();
        combiner.selectChainVariant(variant);
        combiner.setStringParameter("a", a);
        combiner.setStringParameter("b", b);
        final var settings = combiner.combineJson();
        executor.putSettingsJson(settings);
    }

    public static void main(String[] args) throws IOException {
        boolean json = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-json")) {
            json = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex+ 4) {
            System.out.printf("Usage: " +
                            "%s [-json] some_multi_chain.mchain input_image output_image sub_chain_variant [A B]%n" +
                            "some_multi_chain.mchain should be a multi-chain, which process 2 scalars X and Y " +
                            "and have 2 parameters named A and B;%n" +
                            "it should calculate some formula like AX+BY and return the result in the output.",
                    CallSimpleMultiChain.class.getName());
            return;
        }
        final Path multiChainPath = Paths.get(args[startArgIndex]);
        final String x = args[startArgIndex + 1];
        final String y = args[startArgIndex + 2];
        final String variant = args[startArgIndex + 3];
        final String parameterA = args.length > startArgIndex + 4 ? args[startArgIndex + 4] : null;
        final String parameterB = args.length > startArgIndex + 5 ? args[startArgIndex + 5] : null;

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", multiChainPath.toAbsolutePath());
        try (var executor = UseMultiChain.newSharedExecutor(multiChainPath)) {
            CallSimpleChain.printSubChainExecutors();
            CallSimpleChain.printExecutorInterface(executor);
            executor.putStringScalar("x", x);
            executor.putStringScalar("y", y);
            if (json) {
                customizeViaJson(executor, variant, parameterA, parameterB);
            } else {
                customizeViaParameters(executor, variant, parameterA, parameterB);
            }
            executor.execute();
            System.out.printf("%s%nDone: result is %s%n", executor, executor.getData());
        }
    }
}
