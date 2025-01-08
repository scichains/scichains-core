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
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.system.InstantiationMode;
import net.algart.executors.modules.core.logic.compiler.subchains.UseSubChain;
import net.algart.io.MatrixIO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallSimpleChain {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: " +
                            "%s some_chain.json input_image output_image [A [B]]%n" +
                            "some_chain.json should be some chain, which process single image and have" +
                            " 2 parameters named A and B;%n" +
                            "input_image should be some image file, for example, JPEG or BMP;%n" +
                            "output_image should be result image file;%n" +
                            "A and B should be parameters of the chain.",
                    CallSimpleChain.class.getName());
            return;
        }
        final Path chainPath = Paths.get(args[0]);
        final Path inputImagePath = Paths.get(args[1]);
        final Path outputImagePath = Paths.get(args[2]);
        final String parameterA = args.length > 3 ? args[3] : null;
        final String parameterB = args.length > 4 ? args[4] : null;
        System.out.println("Reading " + inputImagePath.toAbsolutePath() + "...");
        final SMat inputMat = SMat.valueOf(ImageIO.read(inputImagePath.toFile()));

        ExecutionBlock.initializeExecutionSystem();
        System.out.printf("Loading %s...%n", chainPath.toAbsolutePath());
        try (ExecutionBlock executor = UseSubChain.newExecutor(chainPath, InstantiationMode.REQUEST_ALL)) {
//            System.out.println(executor.getExecutorSpecification().jsonString());
            printExecutorInterface(executor);
            executor.putMat(inputMat);
            if (parameterA != null) {
                // - if null, default value should be used
                executor.setStringParameter("a", parameterA);
            }
            if (parameterB != null) {
                // - if null, default value should be used
                executor.setStringParameter("b", parameterB);
            }
            executor.execute();
            final BufferedImage result = executor.getMat().toBufferedImage();
            if (result == null) {
                throw new IllegalArgumentException("Chain " + chainPath + " did not create output image");
            }
            MatrixIO.writeBufferedImage(outputImagePath, result);
            System.out.println("O'k: results saved in " + outputImagePath);
        }
    }

    static void printExecutorInterface(ExecutionBlock executor) {
        System.out.println();
        System.out.println("Initial executor parameters:");
        System.out.printf("    ID = %s%n    session ID = %s%n", executor.getExecutorId(), executor.getSessionId());
        for (var e : executor.parameters().entrySet()) {
            System.out.printf("    %s = %s%n", e.getKey(), e.getValue());
        }
        System.out.println("Initial input ports:");
        for (var p : executor.allInputPorts()) {
            System.out.printf("    %s%n", p);
        }
        System.out.println("Initial output ports:");
        for (var p : executor.allOutputPorts()) {
            System.out.printf("    %s%n", p);
        }
        System.out.println();
    }
}
