/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jakarta.json.JsonException;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorSpecificationSet;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoadingChainTest {
    boolean detailed = false;
    boolean initialize = true;
    boolean stopOnError = false;

    private void processChain(final Path chainFile, ExecutorFactory executorFactory) throws IOException {
        try {
            System.out.printf("Reading %s... ", chainFile);
            ChainSpecification chainSpecification = ChainSpecification.readIfValid(chainFile);
            if (chainSpecification == null) {
                System.out.printf("%n%s is not a chain%n", chainFile);
                return;
            }

            if (detailed) {
                System.out.printf("%nFull chain json:%n");
                System.out.println(chainSpecification);
            }

            try (Chain chain = Chain.of(null, executorFactory, chainSpecification)) {
                if (detailed) {
                    System.out.printf("%nFull chain:%n");
                    System.out.println(chain.toString(true));
                    System.out.println();
                }

                if (initialize) {
                    chain.reinitializeAll();
                }
            }
            System.out.println("O'k");
        } catch (JsonException e) {
            System.out.printf("%nCannot load chain:%n%s%n", e.getMessage());
            if (stopOnError) {
                throw e;
            }
        }
    }

    private void processChainsFolder(final Path folder, ExecutorFactory executorFactory) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    processChainsFolder(file, executorFactory);
                } else if (ChainSpecification.isChainSpecificationFile(file)) {
                    processChain(file, executorFactory);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
//        System.out.println(ChainSpecification.isChainSpecificationFile(Paths.get("a/compile.chain")));
        final LoadingChainTest test = new LoadingChainTest();
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-detailed")) {
            test.detailed = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-onlyLoad")) {
            test.initialize = false;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-stopOnError")) {
            test.stopOnError = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.printf("Usage: %s [-detailed] [-onlyLoad] [-stopOnError] "
                            + "executors_folder chain.json/folder%n",
                    LoadingChainTest.class.getName());
            return;
        }
        final Path specificationFolder = Paths.get(args[startArgIndex]);
        final Path chainFile = Paths.get(args[startArgIndex + 1]);

        System.out.printf("Reading %s...%n", specificationFolder);
        final ExecutorSpecificationSet executorSpecificationSet = ExecutorSpecificationSet.newInstance();
        executorSpecificationSet.addFolder(specificationFolder, true);
        final ExecutorFactory executorFactory = ExecutionBlock.globalLoaders().newFactory(
                "~~DUMMY", executorSpecificationSet);

        if (Files.isDirectory(chainFile)) {
            test.processChainsFolder(chainFile, executorFactory);
        } else {
            test.processChain(chainFile, executorFactory);
        }
    }
}
