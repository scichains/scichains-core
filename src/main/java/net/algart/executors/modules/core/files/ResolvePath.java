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
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.io.MatrixIO;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResolvePath extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_PATH = "path";
    public static final String OUTPUT_ABSOLUTE_PATH = "absolute_path";
    public static final String OUTPUT_PARENT_FOLDER = "parent_folder";
    public static final String OUTPUT_FILE_NAME = "file_name";

    public enum SecurityLevel {
        NONE() {
            @Override
            public void testChild(String childPath) {
            }
        },
        MEDIUM() {
            @Override
            public void testChild(String childPath) {
                checkParent(childPath);
                for (int k = 0, length = childPath.length(); k < length; k++) {
                    final char c = childPath.charAt(k);
                    if (!(c == '_' || c == '-' || c == '.' || c == ' ' || Character.isLetterOrDigit(c))) {
                        throw new IllegalArgumentException("Non-allowed child path \"" + childPath
                                + "\": only letters, digits, spaces and characters '_', '-', '.' are permitted");
                    }
                }
            }
        },
        HIGH() {
            @Override
            public void testChild(String childPath) {
                checkParent(childPath);
                if (!childPath.matches("^[A-Za-z0-9_\\-. ]*$")) {
                    throw new IllegalArgumentException("Non-allowed child path \"" + childPath
                            + "\": only latin letters A-Z, a-z, digits 0-9, spaces "
                            + "and characters '_', '-', '.' are permitted");
                }
            }
        };

        public abstract void testChild(String childPath);

        private static void checkParent(String childPath) {
            if (childPath.contains("..")) {
                throw new IllegalArgumentException("Non-allowed child path \"" + childPath
                        + "\": sequence of 2 or more dots \"..\" is prohibited.");
            }
        }
    }

    public enum Operation {
        NONE(true) {
            @Override
            public void checkOrCreate(Path path) {
            }
        },
        CHECK_EXISTENCE(true) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.exists(path)) {
                    throw new FileNotFoundException("\"" + path + "\" does not exist");
                }
            }
        },
        CHECK_FOLDER_EXISTENCE(true) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isDirectory(path)) {
                    throw new FileNotFoundException("\"" + path + "\" does not exist or is not a directory");
                }
            }
        },
        CHECK_FILE_EXISTENCE(true) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isRegularFile(path)) {
                    throw new FileNotFoundException("\"" + path + "\" does not exist or is not a regular file");
                }
            }
        },
        CHECK_PARENT_EXISTENCE(true) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                final Path parent = path.getParent();
                if (parent != null && !Files.isDirectory(parent)) {
                    throw new FileNotFoundException("\"" + parent + "\" (parent folder of \""
                            + path + "\") does not exist or is not a directory");
                }
            }
        },
        CREATE_FOLDER(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isDirectory(path)) {
                    Files.createDirectory(path);
                }
            }
        },
        CREATE_FILE(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isRegularFile(path)) {
                    Files.createFile(path);
                }
            }
        },
        CREATE_PARENT(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                final Path parent = path.getParent();
                if (parent != null && !Files.isDirectory(parent)) {
                    Files.createDirectory(parent);
                }
            }
        },
        CREATE_FOLDER_WITH_PARENTS(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isDirectory(path)) {
                    Files.createDirectories(path);
                }
            }
        },
        CREATE_FILE_WITH_PARENTS(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                if (!Files.isRegularFile(path)) {
                    final Path parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.createFile(path);
                }
            }
        },
        CREATE_PARENTS(false) {
            @Override
            public void checkOrCreate(Path path) throws IOException {
                final Path parent = path.getParent();
                if (parent != null && !Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
            }
        };

        private final boolean readOnlyOperation;

        Operation(boolean readOnlyOperation) {
            this.readOnlyOperation = readOnlyOperation;
        }

        public boolean isReadOnlyOperation() {
            return readOnlyOperation;
        }

        public abstract void checkOrCreate(Path path) throws IOException;
    }

    private String path = "";
    private String childPath = "";
    private SecurityLevel securityLevel = SecurityLevel.HIGH;
    private Operation operation = Operation.NONE;
    private boolean removeExtension = false;

    public ResolvePath() {
        setDefaultInputScalar(INPUT_PATH);
        setDefaultOutputScalar(OUTPUT_ABSOLUTE_PATH);
        addOutputScalar(OUTPUT_PARENT_FOLDER);
        addOutputScalar(OUTPUT_FILE_NAME);
    }

    public String getPath() {
        return path;
    }

    public ResolvePath setPath(String path) {
        this.path = nonNull(path);
        return this;
    }

    public String getChildPath() {
        return childPath;
    }

    public ResolvePath setChildPath(String childPath) {
        this.childPath = nonNull(childPath);
        return this;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public ResolvePath setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = nonNull(securityLevel);
        return this;
    }

    public Operation getOperation() {
        return operation;
    }

    public ResolvePath setOperation(Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public boolean isRemoveExtension() {
        return removeExtension;
    }

    public ResolvePath setRemoveExtension(boolean removeExtension) {
        this.removeExtension = removeExtension;
        return this;
    }

    @Override
    public void process() {
        final String filePath = PathPropertyReplacement.translateTmpDir(filePath());
        PathPropertyReplacement.checkProbableProperties(filePath);
        Path path = Paths.get(filePath);
        PathPropertyReplacement.checkAbsolute(path);
        path = resolveResultPath(path);
        // - note: path is already absolute
        PathPropertyReplacement.checkProbableProperties(path.toString());
        // - must be AFTER resolveResultPath
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
        final String inputPath = getInputScalar(INPUT_PATH, true).getValue();
        final String result = inputPath != null ? inputPath : this.path;
        return nonEmpty(result.trim(), "path");
    }

    private Path resolveResultPath(Path path) {
        String childPath = this.childPath.trim();
        if (!childPath.isEmpty()) {
            securityLevel.testChild(childPath);
            if (removeExtension) {
                childPath = MatrixIO.removeExtension(childPath);
            }
            path = path.resolve(childPath);
        }
        return path;
    }
}
