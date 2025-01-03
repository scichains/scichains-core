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

package net.algart.executors.modules.core.scalars.io;

import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.Port;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WriteScalar extends WriteFileOperation {
    private boolean deleteFileIfEmpty = false;
    private String charset = "UTF-8";
    private String scalarContent = "";

    public WriteScalar() {
        addFileOperationPorts();
        addInputScalar(DEFAULT_INPUT_PORT);
    }

    public static WriteScalar getInstance() {
        return new WriteScalar();
    }

    public boolean isDeleteFileIfEmpty() {
        return deleteFileIfEmpty;
    }

    public WriteScalar setDeleteFileIfEmpty(boolean deleteFileIfEmpty) {
        this.deleteFileIfEmpty = deleteFileIfEmpty;
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public WriteScalar setCharset(String charset) {
        this.charset = nonEmpty(charset).trim();
        return this;
    }

    public String getScalarContent() {
        return scalarContent;
    }

    public WriteScalar setScalarContent(String scalarContent) {
        this.scalarContent = nonNull(scalarContent);
        return this;
    }

    @Override
    public WriteScalar setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public void process() {
        writeString(getInputScalar(true).getValue());
    }

    public void writeString(String scalar) {
        final Path path = completeFilePath();
        final String s = scalar != null ? scalar : scalarContent;
        try {
            if (s.isEmpty()) {
                if (deleteFileIfEmpty) {
                    logDebug(() -> "Removing file " + path.toAbsolutePath());
                    Files.deleteIfExists(path);
                }
            } else {
                logDebug(() -> "Writing UTF-8 scalar (" + s.length() + " characters) to file "
                        + path.toAbsolutePath());
                writeString(path, s, charset);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.INPUT, DEFAULT_INPUT_PORT);
    }

    public static void writeString(Path file, String s, String charset) throws IOException {
        Files.writeString(file, s, Charset.forName(charset));
    }
}
