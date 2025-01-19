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

package net.algart.executors.modules.core.common.io;

import net.algart.executors.api.Executor;
import net.algart.io.MatrixIO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class FileOperation extends Executor {
    public static final String INPUT_FILE = "file";
    public static final String INPUT_FILE_NAME_ADDITION = "file_name_addition";
    public static final String OUTPUT_ABSOLUTE_PATH = "absolute_path";
    /**
     * The path, produced by {@link #currentOSPath()} method, if it was used.
     */
    public static final String OUTPUT_OS_PATH = "os_path";
    public static final String OUTPUT_PARENT_FOLDER = "parent_folder";
    public static final String OUTPUT_FILE_NAME = "file_name";

    public static final String DEFAULT_EMPTY_FILE = "";

    public static final String FILE_NAME_ADDITION_PATTERN = "$$$";

    public enum FileNameAdditionMode {
        NONE() {
            @Override
            public String completePath(String path, String fileNameAddition) {
                return path;
            }
        },
        AFTER_ALL_PATH() {
            @Override
            public String completePath(String path, String fileNameAddition) {
                return path + fileNameAddition.trim();
            }
        },
        REPLACE_IN_PATH() {
            @Override
            public String completePath(String path, String fileNameAddition) {
                return path.replace(FILE_NAME_ADDITION_PATTERN, fileNameAddition.trim());
            }
        },
        REPLACE_IN_PATH_REMOVING_EXTENSION() {
            @Override
            public String completePath(String path, String fileNameAddition) {
                return path.replace(FILE_NAME_ADDITION_PATTERN, MatrixIO.removeExtension(fileNameAddition.trim()));
            }
        };

        public abstract String completePath(String path, String fileNameAddition);
    }

    private final boolean readOnly;

    private String file = DEFAULT_EMPTY_FILE;
    private FileNameAdditionMode fileNameAdditionMode = FileNameAdditionMode.NONE;
    private boolean secure = false;

    protected FileOperation() {
        this(true);
    }

    public FileOperation(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public String getFile() {
        return file;
    }

    public FileOperation setFile(String file) {
        this.file = nonNull(file);
        return this;
    }

    public FileOperation setFile(Path file) {
        this.file = nonNull(file).toString();
        return this;
    }

    public FileNameAdditionMode getFileNameAdditionMode() {
        return fileNameAdditionMode;
    }

    public FileOperation setFileNameAdditionMode(FileNameAdditionMode fileNameAdditionMode) {
        this.fileNameAdditionMode = nonNull(fileNameAdditionMode);
        return this;
    }

    public boolean isSecure() {
        return secure;
    }

    public FileOperation setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public final String filePath() {
        String inputFile = hasInputPort(INPUT_FILE) ?
                // - note: here we have no guarantees that the subclass will add this port in its constructor
                getInputScalar(INPUT_FILE, true).getValue() :
                null;
        final String result = inputFile != null ? inputFile : this.file;
        return nonEmpty(result.trim(), "file");
    }

    public Path completeOSFilePath(boolean relativize) {
        return completeOSFilePath(filePath(), relativize, true);
    }

    public Path completeOSFilePath(String filePath, boolean relativize, boolean processPorts) {
        final Path completed = completeFilePath(filePath, processPorts);
        final Path result = simplifyOSPath(completed, relativize);
        if (processPorts) {
            setOutputScalar(OUTPUT_OS_PATH, () -> result);
        }
        return result;
    }

    public Path completeFilePath() {
        return completeFilePath(filePath());
    }

    // Note: this function does not work too well with OUTPUT_XXX ports:
    // it will fill out only the LAST from several paths
    public final List<Path> completeSeveralFilePaths() {
        return completeSeveralFilePaths(filePath());
    }

    public final Path completeFilePath(String filePath) {
        return completeFilePath(filePath, true);
    }

    public final Path completeFilePath(String filePath, boolean processPorts) {
        Objects.requireNonNull(filePath, "Null file path");
        filePath = filePath.trim();
        if (processPorts && fileNameAdditionMode != FileNameAdditionMode.NONE) {
            if (secure) {
                throw new SecurityException("File name additions must not be used in secure mode, "
                        + "but actually " + fileNameAdditionMode + " is selected");
            } else {
                final String addition = getInputScalar(INPUT_FILE_NAME_ADDITION).getValue();
                filePath = fileNameAdditionMode.completePath(filePath, addition);
            }
        }
        if (readOnly || !secure) {
            filePath = PathPropertyReplacement.translatePathProperties(filePath, this);
        }
        if (!secure) {
            filePath = PathPropertyReplacement.translateSystemProperties(filePath);
        }
        filePath = PathPropertyReplacement.translateTmpDir(filePath);
        if (secure) {
            PathPropertyReplacement.checkProbableProperties(filePath);
        }
        Path path = Paths.get(filePath);
        if (readOnly || !secure) {
            path = translateCurrentDirectory(path);
        } else {
            PathPropertyReplacement.checkAbsolute(path);
        }
        /* Obsolete ability (never used):
        path = modifyResultPath(path);
        if (processPorts && !secure && fileNameAdditionMode.needToRepeatAfterCorrection) {
            path = Paths.get(fileNameAdditionMode.completePath(path.toString(), addition));
            // - maybe we need to replace $$$ also after correction by modifyResultPath
        }
         */
        if (processPorts) {
            fillOutputFileInformation(path);
        }
        return path;
    }

    public final List<Path> completeSeveralFilePaths(String filePathsSeparatedBySemicolon) {
        Objects.requireNonNull(filePathsSeparatedBySemicolon, "Null file paths");
        final ArrayList<Path> result = new ArrayList<>();
        for (String filePath : filePathsSeparatedBySemicolon.split("[\\;]")) {
            result.add(completeFilePath(filePath));
        }
        return result;
    }

    public void fillOutputFileInformation(Path path) {
        final Path absolutePath = path.toAbsolutePath();
        setOutputScalar(OUTPUT_ABSOLUTE_PATH, absolutePath::toString);
        if (hasOutputPort(OUTPUT_PARENT_FOLDER)) {
            final Path parent = absolutePath.getParent();
            if (parent != null) {
                getScalar(OUTPUT_PARENT_FOLDER).setTo(parent.toString());
            }
        }
        if (hasOutputPort(OUTPUT_FILE_NAME)) {
            final Path fileName = absolutePath.getFileName();
            if (fileName != null) {
                getScalar(OUTPUT_FILE_NAME).setTo(fileName.toString());
            }
        }
    }

    protected final void addFileOperationPorts() {
        addInputScalar(INPUT_FILE);
        addInputScalar(INPUT_FILE_NAME_ADDITION);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
        addOutputScalar(OUTPUT_PARENT_FOLDER);
        addOutputScalar(OUTPUT_FILE_NAME);
    }

    /**
     * Returns absolute current OS path. Equivalent to <code>Paths.get("").toAbsolutePath()</code>.
     *
     * @return current OS path (absolute form of the empty path "").
     */
    public static Path currentOSPath() {
        return Paths.get("").toAbsolutePath();
    }

    /**
     * Returns the relative form of the specified absolute path if it is a sub-path of the current OS directory
     * <code>{@link #currentOSPath()}</code>
     * <i>or</i> its parent directory <code>{@link #currentOSPath()}.getParent()</code>.
     * In both cases, the result will be
     * <pre>
     *     {@link #currentOSPath()}.{@link Path#relativize(Path) relativize}(absolutePath)
     * </pre>
     * Otherwise, the <code>absolutePath</code> argument is returned unchanged.
     *
     * <p>Additional check of the parent folder helps to handle the situation
     * when the application executable file (like "java.exe") is located in a folder as "MySoftware/bin"
     * and the specified file is in its "sibling" as "MySoftware/demo/xxx.dat".
     * Then the current OS folder by default is "MySoftware/bin",
     * and the function returns "../demo/xxx.dat".</p>
     *
     * @param absolutePath the path to relativize.
     * @return a shortened form of the given path when it is located inside the current OS directory '
     * or its parent directory.
     */
    public static Path relativizePathInsideCurrentOrParent(Path absolutePath) {
        Objects.requireNonNull(absolutePath, "Null absolutePath");
        final Path osPath = currentOSPath();
        if (absolutePath.startsWith(osPath)) {
            return osPath.relativize(absolutePath);
        }
        final Path osParent = osPath.getParent();
        if (osParent != null && absolutePath.startsWith(osParent)) {
            return osPath.relativize(absolutePath);
        }
        return absolutePath;
    }

    /**
     * Equivalent to <pre>
     * relativize ? {@link #relativizePathInsideCurrentOrParent
     * relativizePathInsideCurrent}(path.toAbsolutePath()) : path.toAbsolutePath()
     * </pre>
     *
     * @param path       the path to simplify.
     * @param relativize whether we need to shorten the path when it is located inside the current OS directory.
     * @return a shortened form of the given path if the second argument is <code>true</code>,
     * or the absolute path if it is <code>false</code>.
     */
    public static Path simplifyOSPath(Path path, boolean relativize) {
        Objects.requireNonNull(path, "Null path");
        return relativize ? relativizePathInsideCurrentOrParent(path.toAbsolutePath()) : path.toAbsolutePath();
//        System.out.printf("Simplify OS path: %s%n", result);
//        return result;
    }

//    public static void main(String[] args) {
//        Path osPath = Paths.get("/SciChains/bin/").toAbsolutePath();
//        Path absolutePath = Paths.get("C:\\SciChains\\ext\\base-core\\extension.json").toAbsolutePath();
//        System.out.println(osPath);
//        System.out.println(absolutePath);
//        System.out.println(osPath.relativize(absolutePath));
//
//    }
}
