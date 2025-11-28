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
import net.algart.executors.api.chains.core.ChainExecutor;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.chains.core.UseChain;
import net.algart.executors.api.multichains.core.MultiChainExecutor;
import net.algart.executors.api.multichains.MultiChainSpecification;
import net.algart.executors.api.multichains.core.UseMultiChain;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.settings.core.CombineSettings;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.settings.core.UseSettings;
import net.algart.executors.api.system.*;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsTreeDemo {
    private static JsonObject buildCustomSettings(SettingsTree tree) {
        return tree.settingsJson(path -> {
            final ControlSpecification control = path.reqControl();
            if (control.getValueType() == ParameterValueType.STRING) {
                return Jsons.stringValue(path.toString());
            } else {
                return Jsons.doubleValue(111); // or: control.getValueType().toJsonValue("111");
            }
            // - some example: JSON containing the string representation of the path or "111";
            // in the real application, for example, we could extract here some value from some visual editor
        });
    }

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
            final ChainExecutor chainExecutor = UseChain.newSharedExecutor(executorPath);
            executor = chainExecutor.newCombine();
            // - exception if there are no settings
        } else if (MultiChainSpecification.isMultiChainSpecificationFile(executorPath)) {
            final MultiChainExecutor multiChainExecutor = UseMultiChain.newSharedExecutor(executorPath);
            executor = multiChainExecutor.newCombine();
        } else {
            throw new IllegalArgumentException("This file is not settings, chain or multi-chain: " + executorPath);
        }

        final ExecutorSpecification specification = executor.getSpecification();

        final SettingsTree tree;
        if (smart) {
            tree = SettingsTree.of(SmartSearchSettings.newSharedInstance(), specification);
        } else {
            tree = SettingsTree.of(factory, specification);
        }

        System.out.println();
        System.out.printf("**** %s **** %n%s%n%n",
                tree,
                tree.specificationJsonString(ExecutorSpecification.JsonMode.CONTROLS_ONLY));

        System.out.printf("**** Default values: **** %n%s%n%n", tree.defaultSettingsJsonString());

        final JsonObject customSettings = buildCustomSettings(tree);
        System.out.printf("**** Some custom values: **** %n%s%n%n", Jsons.toPrettyString(customSettings));

        System.out.println("**** Trees: ****");
        for (SettingsTree.Path path : tree.treePaths()) {
            System.out.printf("%s:%n    node: %s%n    root: %s%n", path, path.reqTree(), path.rootTree());
        }
        System.out.println();

        System.out.println("**** Controls: ****");
        for (SettingsTree.Path path : tree.controlPaths()) {
            System.out.printf("%s:%n    %s%n", path, path.reqControl().toJson());
        }
    }
}
