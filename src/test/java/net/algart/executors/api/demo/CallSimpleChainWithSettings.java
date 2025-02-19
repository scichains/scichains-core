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
import net.algart.executors.api.chains.ChainExecutor;
import net.algart.executors.api.chains.UseSubChain;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.settings.Settings;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleChainWithSettings {
    private static void customizeViaParameters(ChainExecutor executor, String a, String b) {
        executor.setStringParameter("a", a);
        executor.setStringParameter("b", b);
    }

    private static void customizeViaSettings(ChainExecutor executor, String a, String b) {
        final Settings settings = executor.settings();
        final Parameters parameters = new Parameters();
        parameters.setString("a", a);
        parameters.setString("b", b);
        parameters.setDouble("delta", 0.003);
        // - adding "delta" parameter for a case when the sub-chain "understands" it
        final JsonObject settingsJson = settings.build(parameters);
        System.out.printf("%nSettings JSON: %s%n%n", Jsons.toPrettyString(settingsJson));
        executor.putSettingsJson(settingsJson);
    }

    private static void customizeViaCombiner(ChainExecutor executor, String a, String b) {
        final var combiner = executor.newCombine();
        combiner.setStringParameter("a", a);
        combiner.setStringParameter("b", b);
        combiner.setDoubleParameter("delta", 0.003);
        // - adding "delta" parameter for a case when the sub-chain "understands" it
        final var settingsScalar = combiner.combine();
        System.out.printf("%nCombined JSON: %s%n%n", settingsScalar);
        executor.putSettings(settingsScalar);
    }

    public static void main(String[] args) throws IOException {
        boolean settings = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-settings")) {
            settings = true;
            startArgIndex++;
        }
        boolean combine = false;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-combine")) {
            combine = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: " +
                            "%s [-json] some_chain.chain x y [a b]%n" +
                            "some_chain.chain should be a chain with settings, which process 2 scalars x and y " +
                            "and have 2 parameters named a and b.",
                    CallSimpleChainWithSettings.class.getName());
            return;
        }
        final Path chainPath = Paths.get(args[startArgIndex]);
        final String x = args[startArgIndex + 1];
        final String y = args[startArgIndex + 2];
        final String parameterA = args.length > startArgIndex + 3 ? args[startArgIndex + 3] : null;
        final String parameterB = args.length > startArgIndex + 4 ? args[startArgIndex + 4] : null;

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", chainPath.toAbsolutePath());
        try (var executor = UseSubChain.newSharedExecutor(chainPath)) {
            CallSimpleChain.printSubChainExecutors();
            CallSimpleChain.printExecutorInterface(executor);
            executor.putStringScalar("x", x);
            executor.putStringScalar("y", y);
            if (combine) {
                customizeViaCombiner(executor, parameterA, parameterB);
            } else if (settings) {
                customizeViaSettings(executor, parameterA, parameterB);
            } else {
                customizeViaParameters(executor, parameterA, parameterB);
            }
            executor.execute();
            System.out.printf("%s%nDone: result is %s%n", executor, executor.getData());
        }
    }
}
