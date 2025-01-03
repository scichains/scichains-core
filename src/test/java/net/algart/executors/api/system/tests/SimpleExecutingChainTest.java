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

package net.algart.executors.api.system.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.Chain;
import net.algart.executors.api.system.ChainSpecification;
import net.algart.executors.api.system.ExecutorLoaderSet;
import net.algart.executors.modules.core.logic.compiler.subchains.UseSubChain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimpleExecutingChainTest {
    private static void executeChainAsExecutor(Path chainPath) throws IOException {
        try (ExecutionBlock executor = UseSubChain.getSessionInstance("MySession").toExecutor(chainPath)) {
            executor.setAllOutputsNecessary(true);
            executor.execute();
            System.out.println("Executor finished: " + executor);
        }
    }

    private static void executeChainDirectly(Path chainPath) throws IOException {
        ChainSpecification chainSpecification = ChainSpecification.read(chainPath);
        final ExecutorFactory executorFactory = ExecutorLoaderSet.globalExecutorLoaders().newFactory("MySession");
        try (Chain chain = Chain.valueOf(null, executorFactory, chainSpecification)) {
            chain.reinitializeAll();
            chain.execute();
            System.out.println("Chain finished: " + chain);
        }
    }

    public static void main(String[] args) throws IOException {
        boolean lowLevel = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-lowLevel")) {
            lowLevel = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: %s [-lowLevel] some_chain.json%n" +
                            "The chain should not require any input data: " +
                            "this test does not set inputs and does not analyse outputs.",
                    SimpleExecutingChainTest.class.getName());
            return;
        }
        final Path chainPath = Paths.get(args[startArgIndex]);
        ExecutionBlock.initializeExecutionSystem();
        if (lowLevel) {
            executeChainDirectly(chainPath);
        } else {
            executeChainAsExecutor(chainPath);
        }

    }
}
