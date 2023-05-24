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
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.*;

public final class CopyMoveFiles extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_TARGET_ABSOLUTE_PATH = "target_absolute_path";

    public enum Action {
        COPY("Copying", "copy") {
            @Override
            void action(Path file, Path target) throws IOException {
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        },
        MOVE("Moving", "move") {
            @Override
            void action(Path file, Path target) throws IOException {
                Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        };

        private final String title;
        private final String verb;

        Action(String title, String verb) {
            this.title = title;
            this.verb = verb;
        }

        abstract void action(Path file, Path target) throws IOException;
    }

    private boolean doAction = true;
    private Action action = Action.COPY;
    private String target = "";
    private String globPattern = "*.(dat,tmp)";

    public CopyMoveFiles() {
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(OUTPUT_TARGET_ABSOLUTE_PATH);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public CopyMoveFiles setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public CopyMoveFiles setAction(Action action) {
        this.action = nonNull(action);
        return this;
    }

    public String getTarget() {
        return target;
    }

    public CopyMoveFiles setTarget(String target) {
        this.target = nonNull(target);
        return this;
    }

    public String getGlobPattern() {
        return globPattern;
    }

    public CopyMoveFiles setGlobPattern(String globPattern) {
        this.globPattern = nonEmpty(globPattern);
        return this;
    }

    @Override
    public void process() {
        final Path fileOrFolder = completeFilePath().toAbsolutePath();
        final Path target = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(this.target, this);
        getScalar().setTo(target);
        if (!doAction) {
            return;
        }
        try {
            if (Files.isRegularFile(fileOrFolder)) {
                logDebug(() -> action.title + " file " + fileOrFolder + " to " + target);
                action.action(fileOrFolder, target);
            } else {
                if (!Files.exists(fileOrFolder)) {
                    throw new FileNotFoundException(fileOrFolder + " does not exist: nothing to " + action.verb);
                }
                if (!Files.isDirectory(target)) {
                    throw new IllegalArgumentException("If the source file/folder is a folder, "
                            + "the target must also be an existing folder, but it is not so: " + target);
                }
                logDebug(() -> action.title + " files " + globPattern + " from folder "
                        + fileOrFolder + " to " + target);
                try (DirectoryStream<Path> files = Files.newDirectoryStream(fileOrFolder, globPattern)) {
                    for (Path f : files) {
                        if (Files.isRegularFile(f)) {
                            action.action(f, target.resolve(fileOrFolder.relativize(f)));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
