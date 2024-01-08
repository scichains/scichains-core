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

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.Port;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

public final class WriteImage extends WriteFileOperation implements ReadOnlyExecutionInput {
    private boolean requireInput = false;
    private boolean autoContrastBeforeWriting = false;

    public WriteImage() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
    }

    public static WriteImage getInstance() {
        return new WriteImage();
    }

    @Override
    public WriteImage setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean requireInput() {
        return requireInput;
    }

    public WriteImage setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    public boolean isAutoContrastBeforeWriting() {
        return autoContrastBeforeWriting;
    }

    public WriteImage setAutoContrastBeforeWriting(boolean autoContrastBeforeWriting) {
        this.autoContrastBeforeWriting = autoContrastBeforeWriting;
        return this;
    }

    @Override
    public void process() {
        process(getInputMat(!requireInput));
    }

    public void process(SMat inputMat) {
        if (inputMat.isInitialized()) {
            if (autoContrastBeforeWriting) {
                inputMat = inputMat.autoContrast();
            }
            writeImage(inputMat.toBufferedImage());
        }
    }

    public void writeImage(BufferedImage bufferedImage) throws UnsupportedImageFormatException {
        final File file = completeFilePath().toFile();
        String formatName = extension(file.getName(), "BMP");
        logDebug(() -> "Writing image " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight()
                + " to file " + file.getAbsolutePath() + " (format " + formatName.toUpperCase() + ")");
        try {
            if (!javax.imageio.ImageIO.write(bufferedImage, formatName, file)) {
                // Currently leads to NullPointerException: https://bugs.openjdk.java.net/browse/JDK-8064859
                throw new UnsupportedImageFormatException("Cannot write " + file + ": no writer for " + formatName);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT);
    }
}
