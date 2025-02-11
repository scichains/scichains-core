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

import net.algart.executors.api.multichains.MultiChainSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class SimpleMultiChainTest {
    public static void main(String[] args) throws IOException {
        boolean rewrite = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-rewrite")) {
            rewrite = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: %s [-rewrite] multi_chain.mchain | folder_with_multi_chains%n",
                    SimpleMultiChainTest.class.getName());
            return;
        }
        final Path multiChainFile = Paths.get(args[startArgIndex]);
        List<Path> files;
        if (Files.isDirectory(multiChainFile)) {
            try (Stream<Path> list = Files.list(multiChainFile)) {
                files = list.filter(MultiChainSpecification::isMultiChainSpecificationFile).toList();
            }
        } else {
            files = List.of(multiChainFile);
        }
        for (Path file : files) {
            System.out.printf("Reading %s...%n", file);
            MultiChainSpecification specification = MultiChainSpecification.readIfValid(file);
            if (specification == null) {
                System.out.printf("Skipping %s (not a multi-chain)...%n", file);
                continue;
            }
            if (rewrite) {
                System.out.printf("Writing %s...%n", file);
                specification.write(file);
            }
            if (files.size() == 1) {
                System.out.println("Specification:");
                System.out.println(specification);
                System.out.println();
                System.out.println("JSON:");
                System.out.println(specification.jsonString());
                System.out.println();
            }
        }
    }
}
