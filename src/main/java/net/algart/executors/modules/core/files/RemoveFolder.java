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

package net.algart.executors.modules.core.files;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class RemoveFolder extends WriteFileOperation implements ReadOnlyExecutionInput {
    private RemoveFiles.Stage stage = RemoveFiles.Stage.EXECUTE;
    private boolean doAction = true;
    private boolean folderExistenceRequired = true;

    public RemoveFolder() {
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public RemoveFiles.Stage getStage() {
        return stage;
    }

    public RemoveFolder setStage(RemoveFiles.Stage stage) {
        this.stage = nonNull(stage);
        return this;
    }

    public boolean isDoAction() {
        return doAction;
    }

    public RemoveFolder setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public boolean isFolderExistenceRequired() {
        return folderExistenceRequired;
    }

    public RemoveFolder setFolderExistenceRequired(boolean folderExistenceRequired) {
        this.folderExistenceRequired = folderExistenceRequired;
        return this;
    }

    @Override
    public void initialize() {
        final Path fileOrFolder = completeFilePath().toAbsolutePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH
        if (stage == RemoveFiles.Stage.RESET) {
            removeFolder(fileOrFolder);
        }
    }


    @Override
    public void process() {
        final Path fileOrFolder = completeFilePath().toAbsolutePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH
        if (stage == RemoveFiles.Stage.EXECUTE) {
            removeFolder(fileOrFolder);
        }
    }

    public void removeFolder(Path fileOrFolder) {
        if (!doAction) {
            return;
        }
        try {
            logDebug(() -> "Removing " + fileOrFolder);
            if (Files.isRegularFile(fileOrFolder)) {
                Files.delete(fileOrFolder);
            } else {
                if (!Files.exists(fileOrFolder)) {
                    if (folderExistenceRequired) {
                        throw new FileNotFoundException(fileOrFolder + " does not exist: nothing to remove");
                    } else {
                        return;
                    }
                }
                try (Stream<Path> walk = Files.walk(fileOrFolder)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(RemoveFolder::removeWithoutIOException);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static void removeWithoutIOException(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
