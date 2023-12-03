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

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ListOfFiles extends FileOperation implements ReadOnlyExecutionInput {
    private String globPattern = "*.{jpeg,jpg,png,gif,bmp}";
    private String regularExpression = "";
    private FileSortOrder sortOrder = FileSortOrder.SUBDIRECTORIES_FIRST;
    private boolean singlePath = false;
    private boolean recursiveScanning = true;
    private boolean absolutePaths = true;
    private boolean removeExtension = false;
    private boolean folderExistenceRequired = false;

    public ListOfFiles() {
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public String getGlobPattern() {
        return globPattern;
    }

    public ListOfFiles setGlobPattern(String globPattern) {
        this.globPattern = nonEmpty(globPattern);
        return this;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public ListOfFiles setRegularExpression(String regularExpression) {
        this.regularExpression = nonNull(regularExpression);
        return this;
    }

    public FileSortOrder getSortOrder() {
        return sortOrder;
    }

    public ListOfFiles setSortOrder(FileSortOrder sortOrder) {
        this.sortOrder = nonNull(sortOrder);
        return this;
    }

    public boolean isSinglePath() {
        return singlePath;
    }

    public ListOfFiles setSinglePath(boolean singlePath) {
        this.singlePath = singlePath;
        return this;
    }

    public boolean isRecursiveScanning() {
        return recursiveScanning;
    }

    public ListOfFiles setRecursiveScanning(boolean recursiveScanning) {
        this.recursiveScanning = recursiveScanning;
        return this;
    }

    public boolean isAbsolutePaths() {
        return absolutePaths;
    }

    public ListOfFiles setAbsolutePaths(boolean absolutePaths) {
        this.absolutePaths = absolutePaths;
        return this;
    }

    public boolean isRemoveExtension() {
        return removeExtension;
    }

    public ListOfFiles setRemoveExtension(boolean removeExtension) {
        this.removeExtension = removeExtension;
        return this;
    }

    public boolean isFolderExistenceRequired() {
        return folderExistenceRequired;
    }

    public ListOfFiles setFolderExistenceRequired(boolean folderExistenceRequired) {
        this.folderExistenceRequired = folderExistenceRequired;
        return this;
    }

    @Override
    public void process() {
        getScalar().setTo(listOfFiles().stream().map(String::valueOf).collect(Collectors.joining("\n")));
    }

    public List<Path> listOfFiles() {
        final Path fileOrFolder = completeFilePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH
        try {
            logDebug(() -> "Reading list of files in " + fileOrFolder);
            if (singlePath || Files.isRegularFile(fileOrFolder)) {
                return List.of(correctPath(fileOrFolder, null));
            } else {
                if (!Files.exists(fileOrFolder)) {
                    if (folderExistenceRequired) {
                        throw new FileNotFoundException(fileOrFolder + " does not exist");
                    } else {
                        return List.of();
                    }
                }
                final String regularExpression = this.regularExpression.trim();
                final Pattern pattern = regularExpression.isEmpty() ? null : Pattern.compile(regularExpression);
                final List<Path> result = new ArrayList<>();
                findFiles(result, fileOrFolder, globPattern, pattern, recursiveScanning);
                sortOrder.sort(result);
                return result.stream().map(path -> correctPath(path, fileOrFolder)).collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static void findFiles(
            List<Path> result,
            Path path,
            String globPattern,
            Pattern regExp,
            boolean recursiveScanning) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(path, globPattern)) {
                for (Path file : files) {
                    if (regExp == null || regExp.matcher(file.toString().toLowerCase()).matches()) {
                        result.add(file);
                    }
                }
            }
            if (recursiveScanning) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
                    for (Path file : files) {
                        findFiles(result, file, globPattern, regExp, recursiveScanning);
                    }
                }
            }
        }
    }

    private Path correctPath(Path path, Path root) {
        if (absolutePaths) {
            path = path.toAbsolutePath();
        } else if (root != null) {
            path = root.relativize(path);
        }
        return removeExtension ? Paths.get(FileOperation.removeExtension(path.toString())) : path;
    }
}
