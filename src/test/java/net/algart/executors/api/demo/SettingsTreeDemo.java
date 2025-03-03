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
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.chains.UseSubChain;
import net.algart.executors.api.multichains.MultiChainSpecification;
import net.algart.executors.api.multichains.UseMultiChain;
import net.algart.executors.api.settings.CombineSettings;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.settings.UseSettings;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.SettingsTree;
import net.algart.executors.api.system.SmartSearchSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsTreeDemo {
    public static final String MY_SESSION_ID = "~~DUMMY_SESSION";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        boolean smart = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equalsIgnoreCase("-smart")) {
            smart = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: " +
                            "%s some.chain/some.mchain/some.ss",
                    SettingsTreeDemo.class.getName());
            return;
        }
        ExecutionBlock.initializeExecutionSystem();
        final ExecutorFactory factory = ExecutorFactory.newSharedFactory();
        final Path executorPath = Paths.get(args[startArgIndex]);
        final CombineSettings executor;
        if (SettingsSpecification.isSettingsSpecificationFile(executorPath)) {
            executor = UseSettings.newSharedExecutor(factory, executorPath);
        } else if (ChainSpecification.isChainSpecificationFile(executorPath)) {
            executor = UseSubChain.newSharedExecutor(executorPath).newCombine();
            // - exception if there are no settings
        } else if (MultiChainSpecification.isMultiChainSpecificationFile(executorPath)) {
            executor = UseMultiChain.newSharedExecutor(executorPath).newCombine();
        } else {
            throw new IllegalArgumentException("This file is not settings, chain or multi-chain: " + executorPath);
        }

        final ExecutorSpecification specification = executor.getSpecification();

        SettingsTree tree;
        if (smart) {
            tree = SettingsTree.of(SmartSearchSettings.newInstance(MY_SESSION_ID), specification);
        } else {
            tree = SettingsTree.of(factory, specification);
        }

        System.out.println();
        System.out.printf("**** %s **** %n%s%n%n", tree, tree.jsonString(ExecutorSpecification.JsonMode.CONTROLS_ONLY));
        System.out.printf("**** Default values: **** %n%s%n%n", tree.defaultSettingsJsonString());
        System.out.println("**** Trees: ****");
        for (SettingsTree.Path path : tree.treePaths()) {
            System.out.printf("%s:%n    node: %s%n    root: %s%n", path, path.reqTree(), path.root());
        }
        System.out.println();
        System.out.println("**** Controls: ****");
        for (SettingsTree.Path path : tree.controlPaths()) {
            System.out.printf("%s:%n    %s%n", path, path.reqControl().toJson());
        }
    }
}
