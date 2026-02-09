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

package net.algart.executors.modules.core.scalars.io;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.FileOperation;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ReadScalar extends FileOperation {
    private String charset = "UTF-8";
    private String defaultValue = "";

    public ReadScalar() {
        addFileOperationPorts();
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static ReadScalar getInstance() {
        return new ReadScalar();
    }

    public static ReadScalar getSecureInstance() {
        final ReadScalar result = new ReadScalar();
        result.setSecure(true);
        return result;
    }

    public String getCharset() {
        return charset;
    }

    public ReadScalar setCharset(String charset) {
        this.charset = nonEmpty(charset).trim();
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ReadScalar setDefaultValue(String defaultValue) {
        this.defaultValue = nonNull(defaultValue);
        return this;
    }

    @Override
    public ReadScalar setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public void process() {
        SScalar input = getInputScalar(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Reading scalar");
            getScalar().setTo(checkResult(input.getValue()));
        } else {
            getScalar().setTo(checkResult(readString()));
        }
    }

    public final String readString() {
        final Path path = completeFilePath();
        try {
            if (skipIfMissingFileOrThrow(path)) {
                logDebug(() -> "Creating null scalar for non-existing " + path.toAbsolutePath());
                return defaultValue.isEmpty() ? null : defaultValue;
            } else {
                logDebug(() -> "Reading UTF-8 scalar from " + path.toAbsolutePath());
                return readString(path, charset);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    String checkResult(String result) {
        return result;
    }

    public static String readString(Path file, String charset) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(charset, "Null charset");
        return Files.readString(file, Charset.forName(charset));
    }
}