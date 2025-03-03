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
import net.algart.executors.api.multichains.MultiChainExecutor;
import net.algart.executors.api.multichains.UseMultiChain;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleMultiChain {
    private static void customizeViaParameters(MultiChainExecutor executor, String variant, String a, String b) {
        executor.selectChainVariant(variant);
        executor.setStringParameter("a", a);
        executor.setStringParameter("b", b);
    }

    private static void customizeViaBuilder(MultiChainExecutor executor, String variant, String a, String b) {
        final SettingsBuilder settingsBuilder = executor.settingsBuilder();
        final Parameters parameters = new Parameters();
        parameters.setString(executor.multiChain().selectedChainParameter(), variant);
        parameters.setString("a", a);
        parameters.setString("b", b);
        parameters.setString(variant, "{\"delta\": 0.003}");
        // - adding "delta" parameter for a case when the sub-chain "understands" it
        final JsonObject settingsJson = settingsBuilder.build(parameters);
        System.out.printf("%nSettings JSON: %s%n%n", Jsons.toPrettyString(settingsJson));
        executor.putSettingsJson(settingsJson);
    }

    // Little more convenient than customizeViaSettings: combiner has "selectChainVariant" method
    private static void customizeViaCombiner(MultiChainExecutor executor, String variant, String a, String b) {
        final var combiner = executor.newCombine();
        combiner.selectChainVariant(variant);
        combiner.setStringParameter("a", a);
        combiner.setStringParameter("b", b);
        combiner.putStringScalar(variant, "{\"delta\": 0.003}");
        // - adding "delta" sub-parameter for a case when the sub-chain "understands" it
        final var settings = combiner.combine();
        System.out.printf("%nCombined JSON: %s%n%n", settings);
        executor.putSettings(settings);
    }

    public static void main(String[] args) throws IOException {
        boolean builder = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-builder")) {
            builder = true;
            startArgIndex++;
        }
        boolean combine = false;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-combine")) {
            combine = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 4) {
            System.out.printf("Usage: " +
                            "%s [-builder|-combine] some_multi_chain.mchain x y sub_chain_variant [a b]%n" +
                            "some_multi_chain.mchain should be a multi-chain, which process 2 scalars x and y " +
                            "and have 2 parameters named a and b;%n" +
                            "it should calculate some formula like ax+by and return the result in the output.",
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
            CallSimpleChainForImage.printSubChainExecutors();
            CallSimpleChainForImage.printExecutorInterface(executor);
            executor.putStringScalar("x", x);
            executor.putStringScalar("y", y);
            if (combine) {
                customizeViaCombiner(executor, variant, parameterA, parameterB);
            } else if (builder) {
                customizeViaBuilder(executor, variant, parameterA, parameterB);
            } else {
                customizeViaParameters(executor, variant, parameterA, parameterB);
            }
            executor.execute();
            System.out.printf("%s%nDone: result is %s%n", executor, executor.getData());
        }
    }
}
