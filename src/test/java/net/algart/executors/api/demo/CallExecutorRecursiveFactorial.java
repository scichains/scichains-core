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
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorNotFoundException;

import java.io.IOException;

public class CallExecutorRecursiveFactorial {
    public static final String SESSION_ID = "~~DUMMY_SESSION";
    public static final String RECURSIVE_FACTORIAL_CHAIN_ID = "8585f3b5-decf-45e5-be50-e91b7a1a693c";
    // - ID of the sub-chain build/chain/models/stare_examples/recursive_factorial.chain

    public static void main(String[] args) throws IOException, ExecutorNotFoundException, ClassNotFoundException {
        if (args.length < 1) {
            System.out.printf("Usage: %s number_to_calculate_factorial%n",
                    CallExecutorRecursiveFactorial.class.getName());
            return;
        }
        final int value = Integer.parseInt(args[0]);

        ExecutionBlock.initializeExecutionSystem();
        // - automatically registers RECURSIVE_FACTORIAL_CHAIN_ID, because
        // it is a part of the platform folder for sub-chains
        final ExecutorFactory executorFactory = ExecutorFactory.newDefaultInstance(SESSION_ID);
        try (ExecutionBlock executor = executorFactory.newExecutor(RECURSIVE_FACTORIAL_CHAIN_ID)) {
            CallSimpleChain.printExecutorInterface(executor);
            executor.setIntParameter("n", value);
            executor.requestDefaultOutput();
            executor.execute();
            final double result = executor.getScalar().toDouble();
            System.out.println("Factorial of " + value + " is " + result);
        }
    }
}
