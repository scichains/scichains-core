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

package net.algart.executors.api.tests;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.extensions.InstalledExtensions;
import net.algart.executors.api.system.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsTreeTest {
    public static final String MY_SESSION_ID = "~~DUMMY_SESSION";
    public static final String DEMO_CHAIN_SETTINGS_ID = "0526290c-5160-4509-82d7-9687e594ab53";
    // - the ID of build/settings/specifications/algart_executors_examples/combined_settings.ss

    public static void main(String[] args) throws IOException {
        System.setProperty(InstalledExtensions.EXTENSIONS_ROOT_PROPERTY, "build");
        ExecutionBlock.initializeExecutionSystem();
        final ExecutorLoaderSet global = ExecutionBlock.globalLoaders();
        final Set<String> allIds = global.allExecutorIds(MY_SESSION_ID, true);
        System.out.printf("%nFound %d standard executors%n%n", allIds.size());

        SettingsTree treeQuick = null, treeSmart = null;
        String sQuick = null, sSmart = null;
        for (int test = 1; test <= 16; test++) {
            final ExecutorSpecificationFactory factory = global.newFactory(MY_SESSION_ID);
            final SmartSearchSettings smartSearch = SmartSearchSettings.of(factory, global, MY_SESSION_ID);
            long t1 = System.nanoTime();
            final ExecutorSpecification specification = factory.getSpecification(DEMO_CHAIN_SETTINGS_ID);
            long t2 = System.nanoTime();
            treeQuick = SettingsTree.of(factory, specification);
            long t3 = System.nanoTime();
            sQuick = treeQuick.jsonString(ExecutorSpecification.JsonMode.MEDIUM);
            long t4 = System.nanoTime();
            treeSmart = SettingsTree.of(smartSearch, specification);
            long t5 = System.nanoTime();
            sSmart = treeSmart.jsonString(ExecutorSpecification.JsonMode.MEDIUM);
            long t6 = System.nanoTime();

            System.out.printf("Test #%d: get specification %.3f ms, " +
                            "quick tree %.3f + %.3f ms JSON string, " +
                            "smart tree %.3f ms + %.3f ms JSON string%n",
                    test, (t2 - t1) * 1e-6,
                    (t3 - t2) * 1e-6, (t4 - t3) * 1e-6,
                    (t5 - t4) * 1e-6, (t6 - t5) * 1e-6);
        }
        System.out.println();
        System.out.printf("**** %s **** %n", treeQuick);
        System.out.println(sQuick);
        System.out.println();
        System.out.printf("**** %s ****%n", treeSmart);
        System.out.println(sSmart);
        System.out.printf("**** Smart trees: ****%n%s%n%n", listToString(treeSmart.treePaths()));
        System.out.printf("**** Smart controls: ****%n%s%n", listToString(treeSmart.controlPaths()));
    }

    private static String listToString(List<?> paths) {
        return paths.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }
}
