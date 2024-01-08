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

package net.algart.executors.modules.core.matrices.io;

import net.algart.external.ExternalAlgorithmCaller;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.Port;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

public final class WriteAlgARTMatrices extends WriteFileOperation implements ReadOnlyExecutionInput {
    private boolean requireInput = false;

    public WriteAlgARTMatrices() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
    }

    @Override
    public WriteAlgARTMatrices setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean requireInput() {
        return requireInput;
    }

    public WriteAlgARTMatrices setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix m = getInputMat(!requireInput).toMultiMatrix();
        if (m != null) {
            writeMat(m);
        }
    }

    public void writeMat(MultiMatrix m) {
        final File file = completeFilePath().toAbsolutePath().toFile();
        logDebug(() -> "Writing AlgART " + m + " to file " + file);
        try {
            ExternalAlgorithmCaller.writeAlgARTImage(file, m.allChannels());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT);
    }
}
