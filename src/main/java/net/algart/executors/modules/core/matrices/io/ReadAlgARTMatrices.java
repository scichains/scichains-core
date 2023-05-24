/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.matrices.io;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.external.ExternalAlgorithmCaller;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.io.FileOperation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.List;

public final class ReadAlgARTMatrices extends FileOperation implements ReadOnlyExecutionInput {
    public ReadAlgARTMatrices() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public static ReadAlgARTMatrices getSecureInstance() {
        final ReadAlgARTMatrices result = new ReadAlgARTMatrices();
        result.setSecure(true);
        return result;
    }

    @Override
    public ReadAlgARTMatrices setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public void process() {
        SMat input = getInputMat(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying " + input);
            getMat().setTo(input);
        } else {
            getMat().setTo(readMultiMatrix());
        }
    }

    public MultiMatrix readMultiMatrix() {
        final File file = completeFilePath().toAbsolutePath().toFile();
        logDebug(() -> "Reading AlgART multi-matrix from " + file);
        try {
            final List<Matrix<? extends PArray>> matrices = ExternalAlgorithmCaller.readAlgARTImage(file);
            final MultiMatrix multiMatrix = MultiMatrix.valueOfRGBA(matrices);
            final MultiMatrix result = multiMatrix.clone();
            multiMatrix.freeResources();
            // - close files
            return result;
        } catch (IOException e) {
            throw new IOError(e);
        }

    }
}
