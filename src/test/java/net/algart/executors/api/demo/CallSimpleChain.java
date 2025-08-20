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
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.core.ChainExecutor;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.chains.core.UseChain;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.system.CreateMode;
import net.algart.executors.api.system.ExecutorFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CallSimpleChain {
    private static final boolean CUSTOMIZING_USE = true;
    boolean executeAll = false;
    double x;
    double y;
    Double a;
    Double b;

    private ChainExecutor createExecutor(Path chainPath) throws IOException {
        if (!executeAll) {
            return UseChain.newSharedExecutor(chainPath);
            // - this is maximally simple
        } else if (CUSTOMIZING_USE) {
            final ChainSpecification specification = ChainSpecification.read(chainPath);
            final UseChain use = UseChain.getSharedInstance().setOverrideBehaviour(true).setExecuteAll(true);
            // - Note: setOverrideBehaviour is necessary!
            // By default, the chain behavior depends on the specification:
            // the flags in the Executor / Options/ Execution section.
            // setOverrideBehaviour allows overriding these settings.
            // Also, the necessity of this call prevents the user from unintentionally changing behavior:
            // usually you should not use "executeAll" mode, it is intended for debugging needs.
            return use.newExecutor(specification, CreateMode.REQUEST_ALL);
            // - also not too difficult
        } else {
            final ChainSpecification specification = ChainSpecification.read(chainPath);
            specification.getExecutor().getOptions().getExecution().setAll(true);
            return UseChain.newSharedExecutor(specification, CreateMode.REQUEST_ALL);
            // - you can do it like this too
        }
    }

    private void executeChainAsExecutor(Path chainPath)
            throws IOException {
        try (ChainExecutor executor = createExecutor(chainPath)) {
            executor.putDoubleScalar("x", x);
            executor.putDoubleScalar("y", y);
            executor.setParameter("a", a);
            executor.setParameter("b", b);
            // setParameters works well also with null values
            executor.execute();
            System.out.println("Executor finished: " + executor);
        }
    }

    private void executeChainDirectly(Path chainPath)
            throws IOException {
        ChainSpecification chainSpecification = ChainSpecification.read(chainPath);
        final ExecutorFactory executorFactory = ExecutorFactory.newFactory("MySession");
        try (Chain chain = Chain.of(null, executorFactory, chainSpecification)) {
            chain.setExecuteAll(executeAll);
            chain.reinitializeAll();
            chain.setParameters(new Parameters().set("a", a).set("b", b));
            final Map<String, Data> inputs = new HashMap<>();
            inputs.put("x", SScalar.of(x));
            inputs.put("y", SScalar.of(y));
            chain.setInputData(inputs);
            chain.execute();
            System.out.println("Chain finished: " + chain);
        }
    }

    public static void main(String[] args) throws IOException {
        final CallSimpleChain demo = new CallSimpleChain();
        boolean lowLevel = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-lowLevel")) {
            lowLevel = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-all")) {
            demo.executeAll = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.printf("Usage: %s [-lowLevel] [-all] some_chain.chain%n" +
                            "some_chain.chain should be a chain with settings, which process 2 scalars x and y " +
                            "and have 2 parameters named a and b. However, this is not necessary.%n" +
                            "Note: in the low-level mode, the chain must not use any chains, settings " +
                            "or other dynamic executors.",
                    CallSimpleChain.class.getName());
            return;
        }
        demo.x = Double.parseDouble(args[startArgIndex + 1]);
        demo.y = Double.parseDouble(args[startArgIndex + 2]);
        demo.a = args.length > startArgIndex + 3 ? Double.valueOf(args[startArgIndex + 3]) : null;
        demo.b = args.length > startArgIndex + 4 ? Double.valueOf(args[startArgIndex + 4]) : null;
        final Path chainPath = Paths.get(args[startArgIndex]);
        ExecutionBlock.initializeExecutionSystem();
        if (lowLevel) {
            demo.executeChainDirectly(chainPath);
        } else {
            demo.executeChainAsExecutor(chainPath);
        }

    }
}
