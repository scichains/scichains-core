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
import net.algart.executors.api.settings.SettingsTree;
import net.algart.executors.api.system.*;

import java.io.IOException;
import java.util.Set;

public class ExecutorLoaderSetTest {
    private static ExecutorSpecificationFactory factory() {
        ExecutorLoaderSet global = ExecutionBlock.globalLoaders();
        return global.newFactory(ExecutionBlock.GLOBAL_SHARED_SESSION_ID);
        // return executorId -> global.getSpecification(null, executorId, true);
        // - this is non-optimized simple alternative
    }

    public static void main(String[] args) throws IOException {
        long t1 = System.nanoTime();
        ExecutionBlock.initializeExecutionSystem();
        long t2 = System.nanoTime();
        final ExecutorLoaderSet global = ExecutionBlock.globalLoaders();
        final Set<String> allIds = global.allExecutorIds(null, true);
        System.out.printf("Found %d standard executors in %.3f ms%n", allIds.size(), (t2 - t1) * 1e-6);

        long n = 0;
        for (int test = 1; test <= 16; test++) {
            final ExecutorSpecificationFactory factory = factory();
            t1 = System.nanoTime();
            n = allIds.stream().filter(id -> factory.getSettingsSpecification(id) != null).count();
            t2 = System.nanoTime();
            System.out.printf("Test #%d: found %d settings among all %d in %.3f ms%n",
                    test, n, allIds.size(), (t2 - t1) * 1e-6);
        }
        System.out.println();
        for (int test = 1; test <= 16; test++) {
            final ExecutorSpecificationFactory factory = factory();
            // The following optimization is not too efficient, unless there are a lot of dynamic executors:
            // usually most specifications in the standard factory are built-in and preloaded
            t1 = System.nanoTime();
            Set<String> probableIds = SettingsTree.SmartSearch.probableSettingsIds(global, null);
            t2 = System.nanoTime();
            long m = probableIds.stream().filter(id -> factory.getSettingsSpecification(id) != null).count();
            long t3 = System.nanoTime();
            if (n != m) {
                throw new AssertionError(m + " != " + n);
            }
            System.out.printf("Test #%d: found %d settings among %d probables in %.3f ms + %.3f ms%n",
                    test, n, probableIds.size(), (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);

        }
    }
}
