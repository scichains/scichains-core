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

package net.algart.executors.modules.core.scalars.creation;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.executors.modules.core.files.ResolvePath;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CreatePath extends Executor {
    public static final String OUTPUT_ABSOLUTE_PATH = "absolute_path";
    public static final String OUTPUT_PARENT_FOLDER = "parent_folder";
    public static final String OUTPUT_FILE_NAME = "file_name";

    private final boolean readOnly;

    private String path = "";
    private ResolvePath.Operation operation = ResolvePath.Operation.NONE;
    private boolean secure = false;

    private CreatePath(boolean readOnly) {
        this.readOnly = readOnly;
        setDefaultInputScalar(DEFAULT_INPUT_PORT);
        // - Note: it MUST be DEFAULT_INPUT_PORT to allow using this function as input blocks in the chain:
        // input block must have a standard name DEFAULT_INPUT_PORT: see ExecutorSpecification.setTo(Chain)
        setDefaultOutputScalar(OUTPUT_ABSOLUTE_PATH);
        addOutputScalar(OUTPUT_PARENT_FOLDER);
        addOutputScalar(OUTPUT_FILE_NAME);
    }

    public static CreatePath getInstanceForSource() {
        return new CreatePath(true);
    }

    public static CreatePath getInstanceForResult() {
        return new CreatePath(false);
    }

    public static CreatePath getSecureInstanceForSource() {
        return getInstanceForSource().setSecure(true);
    }

    public static CreatePath getSecureInstanceForResult() {
        return getInstanceForResult().setSecure(true);
    }

    public String getPath() {
        return path;
    }

    public CreatePath setPath(String path) {
        this.path = nonNull(path);
        return this;
    }

    public ResolvePath.Operation getOperation() {
        return operation;
    }

    public CreatePath setOperation(ResolvePath.Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public boolean isSecure() {
        return secure;
    }

    public CreatePath setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    @Override
    public void process() {
        if (readOnly && !operation.isReadOnlyOperation()) {
            throw new UnsupportedOperationException("Non-read-only operation "
                    + operation + " is not allowed in " + getClass());
        }
        String filePath = filePath();
        if (readOnly || !secure) {
            filePath = PathPropertyReplacement.translatePathProperties(filePath, this);
        }
        if (!secure) {
            filePath = PathPropertyReplacement.translateSystemProperties(filePath);
        }
        filePath = PathPropertyReplacement.translateTmpDir(filePath);
        Path path = Paths.get(filePath);
        if (readOnly || !secure) {
            path = translateCurrentDirectory(path);
        } else {
            PathPropertyReplacement.checkAbsolute(path);
        }
        if (secure) {
            PathPropertyReplacement.checkProbableProperties(filePath);
        }
        getScalar(OUTPUT_ABSOLUTE_PATH).setTo(path.toString());
        final Path parent = path.getParent();
        if (parent != null) {
            getScalar(OUTPUT_PARENT_FOLDER).setTo(parent.toString());
        }
        final Path fileName = path.getFileName();
        if (fileName != null) {
            getScalar(OUTPUT_FILE_NAME).setTo(fileName.toString());
        }
        try {
            operation.checkOrCreate(path);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private String filePath() {
        final String inputPath = getInputScalar(DEFAULT_INPUT_PORT, true).getValue();
        final String result = inputPath != null ? inputPath : this.path;
        return nonEmpty(result.trim(), "path");
    }

}
