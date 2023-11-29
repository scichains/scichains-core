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

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.io.FileOperation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class ReadImage extends FileOperation implements ReadOnlyExecutionInput, MatReader {
    public static final String DEFAULT_HELPER_CLASS =
            ReadImage.class.getName().replace(
                    "." + ReadImage.class.getSimpleName(),
                    ".helpers.DefaultMatReaderHelper");
    // - must be another package, to avoid problems with adding helper in another project

    public static final String OUTPUT_DIM_X = "dim_x";
    public static final String OUTPUT_DIM_Y = "dim_y";

    private boolean fileExistenceRequired = true;
    private int numberOfChannels = 0;
    private boolean useHelperClass = true;

    public ReadImage() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_DIM_X);
        addOutputScalar(OUTPUT_DIM_Y);
    }

    public static ReadImage getInstance() {
        return new ReadImage();
    }

    public static ReadImage getSecureInstance() {
        final ReadImage result = new ReadImage();
        result.setSecure(true);
        return result;
    }

    @Override
    public ReadImage setFile(String file) {
        super.setFile(file);
        return this;
    }

    public boolean isFileExistenceRequired() {
        return fileExistenceRequired;
    }

    public ReadImage setFileExistenceRequired(boolean fileExistenceRequired) {
        this.fileExistenceRequired = fileExistenceRequired;
        return this;
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public ReadImage setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = nonNegative(numberOfChannels);
        return this;
    }

    public boolean isUseHelperClass() {
        return useHelperClass;
    }

    public ReadImage setUseHelperClass(boolean useHelperClass) {
        this.useHelperClass = useHelperClass;
        return this;
    }

    @Override
    public void process() {
        SMat input = getInputMat(defaultInputPortName(), true);
        final SMat result = getMat();
        if (input.isInitialized()) {
            logDebug(() -> "Copying " + input);
            result.setTo(input);
        } else {
            readMat(result);
        }
        getScalar(OUTPUT_DIM_X).setTo(result.getDimX());
        getScalar(OUTPUT_DIM_Y).setTo(result.getDimY());
    }

    public SMat readImage() throws UnsupportedImageFormatException {
        return readMat(new SMat());
    }

    public SMat readMat(SMat result) throws UnsupportedImageFormatException {
        readMat(result, completeFilePath());
        return result;
    }

    public void readMat(SMat result, Path path) throws UnsupportedImageFormatException {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(path, "Null path");
        try {
            final BufferedImage bufferedImage = readBufferedImage(path);
            if (bufferedImage == null) {
                assert !fileExistenceRequired : "Impossible when fileExistenceRequired";
                return;
            }
            result.setTo(bufferedImage);
        } catch (UnsupportedImageFormatException e) {
            // If Java platform imageio failed, try the helper
            if (!useHelperClass) {
                throw e;
            }
            final MatReader helper;
            try {
                helper = (MatReader) Class.forName(DEFAULT_HELPER_CLASS).getConstructor().newInstance();
            } catch (Exception helperException) {
                Executor.LOG.log(System.Logger.Level.DEBUG,
                        () -> "Java image I/O failed; attempt to use the helper \""
                                + DEFAULT_HELPER_CLASS + "\" also failed: " + helperException,
                        helperException);
                throw e;
            }
            try {
                helper.readMat(result, path);
            } catch (UnsupportedImageFormatException ignored) {
                throw e;
                // - prefer to show previous exception
            }
        }
        if (numberOfChannels != 0) {
            result.setTo(result.toMultiMatrix().asOtherNumberOfChannels(numberOfChannels));
        }
    }

    public BufferedImage readBufferedImage() throws UnsupportedImageFormatException {
        return readBufferedImage(completeFilePath());
    }

    public BufferedImage readBufferedImage(Path path) throws UnsupportedImageFormatException {
        Objects.requireNonNull(path, "Null path");
        final File file = path.toFile();
        try {
            if (!file.isFile()) {
                if (fileExistenceRequired) {
                    throw new FileNotFoundException("File not found: " + file);
                } else {
                    return null;
                }
            }
            final BufferedImage bufferedImage = javax.imageio.ImageIO.read(file);
            logDebug(() -> "Reading image from " + file.getAbsolutePath() + " by Java API (ImageIO)");
            if (bufferedImage == null) {
                throw new UnsupportedImageFormatException("Cannot read " + file + ": no suitable reader");
            }
            return bufferedImage;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
