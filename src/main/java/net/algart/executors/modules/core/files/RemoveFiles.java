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

package net.algart.executors.modules.core.files;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RemoveFiles extends WriteFileOperation implements ReadOnlyExecutionInput {
    public enum Stage {
        RESET,
        EXECUTE
    }

    private Stage stage = Stage.EXECUTE;
    private boolean doAction = true;
    private String globPattern = "*.(dat,tmp)";
    private boolean fileExistenceRequired = true;

    public RemoveFiles() {
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public Stage getStage() {
        return stage;
    }

    public RemoveFiles setStage(Stage stage) {
        this.stage = nonNull(stage);
        return this;
    }

    public boolean isDoAction() {
        return doAction;
    }

    public RemoveFiles setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public String getGlobPattern() {
        return globPattern;
    }

    public RemoveFiles setGlobPattern(String globPattern) {
        this.globPattern = nonEmpty(globPattern);
        return this;
    }

    public boolean isFileExistenceRequired() {
        return fileExistenceRequired;
    }

    public RemoveFiles setFileExistenceRequired(boolean fileExistenceRequired) {
        this.fileExistenceRequired = fileExistenceRequired;
        return this;
    }

    @Override
    public void initialize() {
        final Path fileOrFolder = completeFilePath().toAbsolutePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH
        if (stage == Stage.RESET) {
            removeFiles(fileOrFolder);
        }
    }


    @Override
    public void process() {
        final Path fileOrFolder = completeFilePath().toAbsolutePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH
        if (stage == Stage.EXECUTE) {
            removeFiles(fileOrFolder);
        }
    }

    public void removeFiles(Path fileOrFolder) {
        if (!doAction) {
            return;
        }
        try {
            if (Files.isRegularFile(fileOrFolder)) {
                // - so, it does exist
                logDebug(() -> "Removing file " + fileOrFolder);
                Files.delete(fileOrFolder);
            } else {
                if (!Files.exists(fileOrFolder)) {
                    if (fileExistenceRequired) {
                        throw new FileNotFoundException(fileOrFolder + " does not exist: nothing to remove");
                    } else {
                        return;
                    }
                }
                logDebug(() -> "Removing files " + globPattern + " from folder " + fileOrFolder);
                try (DirectoryStream<Path> files = Files.newDirectoryStream(fileOrFolder, globPattern)) {
                    for (Path f : files) {
                        if (Files.isRegularFile(f)) {
                            Files.delete(f);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
