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

package net.algart.executors.modules.core.files;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.common.io.WriteFileOperation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CreateFolder extends WriteFileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_ABSOLUTE_FOLDER = "absolute_folder";

    private RemoveFiles.Stage stage = RemoveFiles.Stage.EXECUTE;
    private boolean doAction = true;
    private boolean createParents = true;

    public CreateFolder() {
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(OUTPUT_ABSOLUTE_FOLDER);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public RemoveFiles.Stage getStage() {
        return stage;
    }

    public CreateFolder setStage(RemoveFiles.Stage stage) {
        this.stage = nonNull(stage);
        return this;
    }

    public boolean isDoAction() {
        return doAction;
    }

    public CreateFolder setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public boolean isCreateParents() {
        return createParents;
    }

    public CreateFolder setCreateParents(boolean createParents) {
        this.createParents = createParents;
        return this;
    }

    @Override
    public void initialize() {
        final Path folder = completeFilePathAndResultFolder().toAbsolutePath();
        // - this function also fills the result ports
        if (stage == RemoveFiles.Stage.RESET) {
            createFolder(folder);
        }
    }


    @Override
    public void process() {
        final Path folder = completeFilePathAndResultFolder().toAbsolutePath();
        // - this function also fills the result ports
        if (stage == RemoveFiles.Stage.EXECUTE) {
            createFolder(folder);
        }
    }

    public void createFolder(Path folder) {
        if (!doAction) {
            return;
        }
        try {
            logDebug(() -> "Creating " + folder);
            if (!Files.isDirectory(folder)) {
                if (createParents) {
                    Files.createDirectories(folder);
                } else {
                    Files.createDirectory(folder);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private Path completeFilePathAndResultFolder() {
        final Path path = completeFilePath();
        String absolute = path.toAbsolutePath().toString();
        if (!(absolute.endsWith("/") || absolute.endsWith(File.separator))) {
            // - to be on the safe side
            absolute += File.separator;
        }
        getScalar(OUTPUT_ABSOLUTE_FOLDER).setTo(absolute);
        return path;
    }
}
