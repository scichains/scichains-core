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
import net.algart.executors.api.chains.ChainExecutor;
import net.algart.executors.api.chains.UseSubChain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleChainWithSettings {
    private static void customizeViaJson(ChainExecutor executor,String a, String b) {
        final var combiner = executor.newCombine();
        combiner.setStringParameter("a", a);
        combiner.setStringParameter("b", b);
        combiner.setDoubleParameter("delta", 0.003);
        // - adding "delta" sub-parameter for a case when the sub-chain "understands" it
        final var settings = combiner.combine();
        System.out.printf("%nSettings JSON: %s%n%n", settings);
        executor.putSettings(settings);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: " +
                            "%s some_chain.chain x y [a b]%n" +
                            "some_chain.chain should be a chain with settings, which process 2 scalars x and y " +
                            "and have 2 parameters named a and b.",
                    CallSimpleChainWithSettings.class.getName());
            return;
        }
        final Path chainPath = Paths.get(args[0]);
        final String x = args[1];
        final String y = args[2];
        final String parameterA = args.length > 3 ? args[3] : null;
        final String parameterB = args.length > 4 ? args[4] : null;

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", chainPath.toAbsolutePath());
        try (var executor = UseSubChain.newSharedExecutor(chainPath)) {
            CallSimpleChain.printSubChainExecutors();
            CallSimpleChain.printExecutorInterface(executor);
            executor.putStringScalar("x", x);
            executor.putStringScalar("y", y);
            customizeViaJson(executor, parameterA, parameterB);
            executor.execute();
            System.out.printf("%s%nDone: result is %s%n", executor, executor.getData());
        }
    }
}
