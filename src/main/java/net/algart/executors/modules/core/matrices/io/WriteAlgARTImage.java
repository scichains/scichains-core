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

package net.algart.executors.modules.core.matrices.io;

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.modules.core.common.io.WriteFileOperation;
import net.algart.io.MatrixIO;
import net.algart.multimatrix.MultiMatrix;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;

public final class WriteAlgARTImage extends WriteFileOperation implements ReadOnlyExecutionInput {
    private boolean inputRequired = false;

    public WriteAlgARTImage() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
    }

    @Override
    public WriteAlgARTImage setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean isInputRequired() {
        return inputRequired;
    }

    public WriteAlgARTImage setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix m = getInputMat(!inputRequired).toMultiMatrix();
        if (m != null) {
            writeMat(m);
        }
    }

    public void writeMat(MultiMatrix m) {
        final Path file = completeFilePath().toAbsolutePath();
        logDebug(() -> "Writing AlgART " + m + " to file " + file);
        try {
            MatrixIO.writeImageFolder(file, m.allChannels());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT);
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireInput") ? "inputRequired" : name;
    }
}
