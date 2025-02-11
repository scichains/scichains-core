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
import net.algart.executors.api.multichains.MultiChain;
import net.algart.executors.api.multichains.UseMultiChain;
import net.algart.executors.api.system.InstantiationMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleMultiChain {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.printf("Usage: " +
                            "%s some_multi_chain.mchain input_image output_image sub_chain_variant [A B]%n" +
                            "some_multi_chain.mchain should be a multi-chain, which process 2 scalars X and Y " +
                            "and have 2 parameters named A and B;%n" +
                            "it should calculate some formula like AX+BY and return the result in the output.",
                    CallSimpleMultiChain.class.getName());
            return;
        }
        final Path multiChainPath = Paths.get(args[0]);
        final String x = args[1];
        final String y = args[2];
        final String variant = args[3];
        final String parameterA = args.length > 4 ? args[4] : null;
        final String parameterB = args.length > 5 ? args[5] : null;

        ExecutionBlock.initializeExecutionSystem();

        System.out.printf("Loading %s...%n", multiChainPath.toAbsolutePath());
        try (var executor = UseMultiChain.newSharedExecutor(multiChainPath, InstantiationMode.REQUEST_ALL)) {
            CallSimpleChain.printSubChainExecutors();
            CallSimpleChain.printExecutorInterface(executor);
            executor.putStringScalar("x", x);
            executor.putStringScalar("y", y);
            executor.selectChainVariant(variant);
            if (parameterA != null) {
                executor.setStringParameter("a", parameterA);
            }
            if (parameterB != null) {
                executor.setStringParameter("b", parameterB);
            }
            executor.execute();
            System.out.printf("%s%nDone: result is %s%n", executor, executor.getData());
        }
    }
}
