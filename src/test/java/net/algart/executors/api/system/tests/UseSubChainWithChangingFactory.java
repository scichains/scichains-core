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

import net.algart.executors.api.chains.ChainExecutor;
import net.algart.executors.api.chains.UseSubChain;
import net.algart.executors.api.extensions.InstalledExtensions;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.InstantiationMode;

import java.io.IOException;
import java.nio.file.Path;

public class UseSubChainWithChangingFactory {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        System.setProperty(InstalledExtensions.EXTENSIONS_ROOT_PROPERTY, "build");
        final String path1 = "src/test/resources/chains/chain_from_java/simplest_scalar/scalar_product.chain";
        final String path2 = "src/test/resources/chains/chain_from_java/simplest_scalar/scalar_sum.chain";

        UseSubChain useSubChain1 = new UseSubChain();
        useSubChain1.setSessionId("Session_1");
        ChainExecutor e1 = useSubChain1.newExecutor(Path.of(path1), InstantiationMode.NORMAL);
        UseSubChain useSubChain2 = new UseSubChain();
        useSubChain2.setSessionId("Session_2");
        ChainExecutor e2 = useSubChain2.newExecutor(Path.of(path2), InstantiationMode.NORMAL);
        ExecutorSpecification s2 = e1.executorFactory().getSpecification(e2.getExecutorId());
        if (s2 != null) {
            throw new AssertionError("Must be null");
        }
        s2 = e2.executorFactory().getSpecification(e2.getExecutorId());
        System.out.println(s2);
    }
}
