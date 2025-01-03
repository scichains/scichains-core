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

import net.algart.arrays.Arrays;
import net.algart.arrays.JArrays;
import net.algart.arrays.PArray;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.SystemEnvironment;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.system.*;
import net.algart.executors.modules.core.common.TimingStatistics;
import net.algart.io.MatrixIO;
import net.algart.multimatrix.MultiMatrix2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ExecutingChain {
    public static final String SESSION_ID = "~~DUMMY_SESSION";

    static {
        String property = System.getProperty("java.util.logging.config.file");
        if (property != null) {
            property = SystemEnvironment.replaceHomeEnvironmentVariable(property);
            System.setProperty("java.util.logging.config.file", property);
        }
    }

    public static void main(String[] args) throws IOException {
        ExecutionBlock.initializeExecutionSystem();
        boolean detailed = false;
        boolean monochrome = false;
        boolean cleanCopy = false;
        boolean executeAll = false;
        boolean multithreading = false;
        boolean ignoreExceptions = false;
        boolean gc = false;
        boolean checkStability = false;
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-detailed")) {
            detailed = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-mono")) {
            monochrome = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-clean")) {
            cleanCopy = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-all")) {
            executeAll = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-multithreading")) {
            multithreading = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-ignoreExceptions")) {
            ignoreExceptions = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-gc")) {
            gc = true;
            startArgIndex++;
        }
        if (args.length > startArgIndex && args[startArgIndex].equals("-checkStability")) {
            checkStability = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 1) {
            System.out.printf("Usage: "
                            + "%s [-mono] ]-clean] [-all] [-multithreading] [-ignoreExceptions] [-gc] "
                            + "chain.json [some_image_file result_folder [number_of_tests]]%n",
                    ExecutingChain.class.getName());
            System.out.println("Also please specify the following system variables:");
            System.out.println("    -Dnet.algart.executors.path=folder_with_all_executor_JSONs");
            System.out.println("    -Dnet.algart.executors.logic.compiler.subchains.path="
                    + "folder_standard_subchains (optional)");
            System.out.println("    -Dnet.algart.executors.modules.opencv.useGPU=true/false (optional)");
            return;
        }
        final Path chainFile = Paths.get(args[startArgIndex]);
        final Path sourceFile = startArgIndex + 1 < args.length ? Path.of(args[startArgIndex + 1]) : null;
        final Path resultFolder = startArgIndex + 2 < args.length ? Path.of(args[startArgIndex + 2]) : null;
        final int numberOfTests = startArgIndex + 3 < args.length ? Integer.parseInt(args[startArgIndex + 3]) : 1;

        System.out.printf("Reading%n    %s%n...", JArrays.toString(
                InstalledExtensions.installedExtensionsPaths().toArray(),
                String.format(";%n    "), 16384));
        long t1 = System.nanoTime();
        final ExecutorSpecificationSet executorSpecificationSet = ExecutorSpecificationSet.allBuiltIn();
        long t2 = System.nanoTime();
        System.out.printf(" done (%d executors, %.3f ms)%n", executorSpecificationSet.all().size(), (t2 - t1) * 1e-6);
        final ExecutorFactory executorFactory = ExecutorLoaderSet.globalExecutorLoaders().newFactory(SESSION_ID);

        System.out.printf("Reading %s...", chainFile);
        t1 = System.nanoTime();
        ChainSpecification chainSpecification = ChainSpecification.read(chainFile);
        Chain originalChain = Chain.valueOf(null, executorFactory, chainSpecification);
        originalChain.setMultithreading(multithreading);
        originalChain.setExecuteAll(executeAll);
        originalChain.setIgnoreExceptions(ignoreExceptions);
        originalChain.setTimingByExecutorsEnabled(true);
        originalChain.setTimingSettings(
                1000,
                new TimingStatistics.Settings().setUniformPercentileLevels(5));
        originalChain.reinitializeAll();
        t2 = System.nanoTime();
        System.out.printf(" done (%.3f ms)%n", (t2 - t1) * 1e-6);
        System.out.printf("Chain to execute: %s%n", originalChain);
        if (detailed) {
            System.out.printf("Detailed chain:%n");
            System.out.println(originalChain.toString(true));
        }

        SMat sourceMat = sourceFile == null ? null : SMat.valueOf(ImageIO.read(sourceFile.toFile()));
        final Map<String, Data> inputs = new HashMap<>();
        if (sourceMat != null) {
            if (monochrome) {
                sourceMat = SMat.valueOf(sourceMat.toMultiMatrix2D().asMono().clone());
            }
            System.out.printf("Reading source image %s%s: %s%n",
                    monochrome ? "(monochrome) " : "", sourceFile, sourceMat);
            inputs.put(Executor.DEFAULT_INPUT_PORT, sourceMat);
        }
        originalChain.setInputData(inputs);

        System.out.printf("%nExecuting %s blocks, %s mode...%n",
                executeAll ? "all" : "output and dependent",
                multithreading ? "multithreading" : "single-thread");

        if (resultFolder != null) {
            if (!Files.exists(resultFolder)) {
                Files.createDirectory(resultFolder);
            }
        }
        for (int test = 1; test <= numberOfTests; test++) {
            System.out.printf("%nTest #%d/%d...%n", test, numberOfTests);
            System.gc(); // - at lease one gc is important for stable results
            if (gc) {
                for (int k = 0; k < 5; k++) {
                    System.gc();
                }
            }
            System.out.println(Executor.Timing.getInstance().startingInfo());
            t1 = System.nanoTime();
            Executor.Timing.getInstance().start();
            Chain chain = originalChain;
            if (cleanCopy) {
                chain = chain.cleanCopy();
                chain.reinitializeAll();
                chain.setInputData(inputs);
            }
            t2 = System.nanoTime();
            try {
                chain.execute();
            } finally {
                Executor.Timing.getInstance().finish();
            }
            long t3 = System.nanoTime();
            boolean unstable = false;
            for (ChainBlock block : chain.getAllOutputs()) {
                final ChainOutputPort outputPort = block.reqStandardOutputPort();
                final Data data = outputPort.getData();
                final String name = block.getStandardInputOutputName();
                final String nameForFile = name.replaceAll("[\\/\\\\]", "_");
                System.out.printf("Output block \"%s\" result: %s%n", name, data);
                if (data.isInitialized() && resultFolder != null) {
                    final Path textFile = resultFolder.resolve(nameForFile + ".txt");
                    final String text;
                    if (checkStability && data instanceof SMat) {
                        final MultiMatrix2D multiMatrix2D = ((SMat) data).toMultiMatrix2D();
                        final PArray halftone = multiMatrix2D.intensityChannel().array();
                        text = multiMatrix2D + ": data hash=" + halftone.hashCode()
                                + ", mean=" + Arrays.sumOf(halftone) / halftone.length()
                                / halftone.maxPossibleValue(1.0);
                    } else {
                        text = data instanceof SNumbers ?
                                ((SNumbers) data).toString(true) :
                                data.toString();
                    }
                    boolean changed = false;
                    if (checkStability && test > 1) {
                        final String previous = Files.readString(textFile);
                        if (text.equalsIgnoreCase(previous)) {
                            System.out.printf("Checking stability of \"%s\": stable result%n", name);
                        } else {
                            unstable = changed = true;
                            System.err.printf("Output block \"%s\" CHANGED!%n[[[%s]]]%n instead of %n[[[%s]]]%n%n",
                                    name, text, previous);
                        }
                    }
                    Files.writeString(textFile, text);
                    if (data instanceof SMat && ((SMat) data).getDimCount() == 2) {
                        final Path imageFile = resultFolder.resolve(nameForFile
                                + (changed ? ".changed" : "") + ".bmp");
                        final BufferedImage bufferedImage = ((SMat) data).toBufferedImage();
                        assert bufferedImage != null;
                        System.out.printf("Saving result in %s%n", imageFile);
                        MatrixIO.writeBufferedImage(imageFile, bufferedImage);
                    }
                } else {
                    System.out.printf("WARNING: output block \"%s\" has no initialized data%n", name);
                }
            }
            System.out.println();
            System.out.printf(Locale.US, "Executed %d/%d blocks%n",
                    chain.numberOfReadyBlocks(), chain.numberOfBlocks());
            if (unstable) {
                return;
            }
            System.out.printf(Locale.US, "Execution time: %.3f ms%s%n",
                    (t3 - t1) * 1e-6,
                    cleanCopy ?
                            String.format(Locale.US,
                                    ", including %.3f ms initializing", (t2 - t1) * 1e-6) :
                            "");
            System.out.println(Executor.Timing.getInstance().finishingInfo());
            System.out.println(chain.timingInfo());
            chain.freeData();
            if (gc) {
                for (int k = 0; k < 5; k++) {
                    System.gc();
                }
            }
        }
        originalChain.freeResources();
        if (gc) {
            for (int k = 0; k < 5; k++) {
                System.gc();
            }
        }
        System.out.print(Executor.Timing.memoryInfo());
        originalChain = null;
        if (gc) {
            for (int k = 0; k < 10; k++) {
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                System.out.print(Executor.Timing.memoryInfo());
            }
        }
    }
}
