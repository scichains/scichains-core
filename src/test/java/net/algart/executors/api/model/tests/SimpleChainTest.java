/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.api.model.tests;

import net.algart.executors.api.model.Chain;
import net.algart.executors.api.model.ChainJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimpleChainTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s chain.json result_1.json [result_2.json]%n",
                    ChainJson.class.getName());
            return;
        }
        final Path chainFile = Paths.get(args[0]);
        final Path resultFile1 = Paths.get(args[1]);
        final Path resultFile2 = args.length >= 3 ? Paths.get(args[2]) : null;
        System.out.printf("Reading %s...%n", chainFile);
        ChainJson chainJson = ChainJson.read(chainFile);
        System.out.printf("Writing %s...%n", resultFile1);
        chainJson.rewriteChainSection(resultFile1);
        System.out.printf("Full chain JSON:%n");
        System.out.println(chainJson);
        if (resultFile2 != null) {
            chainJson = ChainJson.read(resultFile1);
            chainJson.rewriteChainSection(resultFile2);
        }

        long t1 = System.nanoTime();
        Chain chain = Chain.valueOf(null, null, chainJson);
        long t2 = System.nanoTime();
        System.out.printf("%nFull chain created in %.3f ms:%n", (t2 - t1) * 1e-6);
        System.out.println(chain);
        t1 = System.nanoTime();
        chain = chain.cleanCopy();
        t2 = System.nanoTime();
        System.out.printf("%nClean copy created in %.3f ms:%n", (t2 - t1) * 1e-6);
        System.out.println(chain);
    }
}
