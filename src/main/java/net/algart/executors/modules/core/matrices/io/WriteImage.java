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

package net.algart.executors.modules.core.matrices.io;

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.io.WriteFileOperation;
import net.algart.io.MatrixIO;

import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class WriteImage extends WriteFileOperation implements ReadOnlyExecutionInput {
    private boolean inputRequired = false;
    private boolean autoContrastBeforeWriting = false;
    private boolean convertAllElementTypesToByte = true;
    private Double quality = null;
    private String compressionType = "";

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

    public boolean isInputRequired() {
        return inputRequired;
    }

    public WriteImage setInputRequired(boolean inputRequired) {
        this.inputRequired = inputRequired;
        return this;
    }

    public boolean isAutoContrastBeforeWriting() {
        return autoContrastBeforeWriting;
    }

    public WriteImage setAutoContrastBeforeWriting(boolean autoContrastBeforeWriting) {
        this.autoContrastBeforeWriting = autoContrastBeforeWriting;
        return this;
    }

    public boolean isConvertAllElementTypesToByte() {
        return convertAllElementTypesToByte;
    }

    public WriteImage setConvertAllElementTypesToByte(boolean convertAllElementTypesToByte) {
        this.convertAllElementTypesToByte = convertAllElementTypesToByte;
        return this;
    }

    public Double getQuality() {
        return quality;
    }

    public WriteImage setQuality(Double quality) {
        this.quality = quality;
        return this;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public WriteImage setCompressionType(String compressionType) {
        this.compressionType = nonNull(compressionType).trim();
        return this;
    }

    @Override
    public void process() {
        process(getInputMat(!inputRequired));
    }

    public void process(SMat inputMat) {
        if (inputMat.isInitialized()) {
            if (autoContrastBeforeWriting) {
                inputMat = inputMat.autoContrast();
            }
            try {
                writeImage(inputMat.toBufferedImage(convertAllElementTypesToByte));
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
    }

    public void writeImage(BufferedImage bufferedImage) throws IOException {
        final Path file = completeFilePath();
        logDebug(() -> "Writing image " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight()
                + " to file " + file.toAbsolutePath());
        MatrixIO.writeBufferedImage(file, bufferedImage, param -> setQuality(param, file));
    }

    private void setQuality(ImageWriteParam param, Path file) {
        final String compressionType = this.compressionType;
        final boolean hasCompression = !compressionType.isEmpty();
        final String[] legalTypes;
        if (quality != null || hasCompression) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            legalTypes = param.getCompressionTypes();
        } else {
            legalTypes = null;
        }
        if (hasCompression) {
            if (legalTypes == null) {
                throw new UnsupportedOperationException("Can't set compression type \"" + compressionType +
                        "\": there is no allowed compression for the extension of the file " + file);
            }
            if (Arrays.stream(legalTypes).noneMatch(compressionType::equals)) {
                throw new UnsupportedOperationException("Unknown compression type \"" + compressionType +
                        "\"; you should select one of the following legal compressions: " +
                        Arrays.toString(legalTypes));
            }
            param.setCompressionType(compressionType);
        }
        if (quality != null) {
            if (legalTypes != null && param.getCompressionType() == null) {
                if (legalTypes.length == 1) {
                    // - we can help the user a little:
                    // no need to manually set the compression type, if there is only 1 case
                    param.setCompressionType(legalTypes[0]);
                } else {
                    logDebug(() -> "Can't set compression quality to " + quality +
                            ": there is no default compression type, " +
                            "you should manually select one of the following legal compressions: " +
                            Arrays.toString(legalTypes) + ", allowed for the extension of the file " + file);
                    return;
                }
            }
            param.setCompressionQuality(quality.floatValue());
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
