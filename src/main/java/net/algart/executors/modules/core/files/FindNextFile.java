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
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.matrices.io.ReadImage;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FindNextFile extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_INDEX = "file_index";
    public static final String OUTPUT_NUMBER_OF_FILES = "number_of_files";
    public static final String OUTPUT_LIST_OF_FILES = "list_of_files";
    public static final String OUTPUT_LAST = "last";

    private String globPattern = "*.{jpeg,jpg,png,gif,bmp}";
    private String regularExpression = "";
    private FileSortOrder sortOrder = FileSortOrder.SUBDIRECTORIES_FIRST;
    private boolean singlePath = false;
    private boolean recursiveScanning = true;
    private boolean clearFileIndexOnReset = true;

    private final List<Path> sortedFiles = new ArrayList<>();
    private String sortedFilesString = "";
    private int currentFileIndex = 0;

    public FindNextFile() {
        //noinspection resource
        setFileExistenceRequired(false);
        // - but we override nonEmptyPathRequired!
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
        addOutputScalar(OUTPUT_PARENT_FOLDER);
        addOutputScalar(OUTPUT_FILE_NAME);
    }

    public String getGlobPattern() {
        return globPattern;
    }

    public FindNextFile setGlobPattern(String globPattern) {
        this.globPattern = nonEmpty(globPattern);
        return this;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public FindNextFile setRegularExpression(String regularExpression) {
        this.regularExpression = nonNull(regularExpression);
        return this;
    }

    public FileSortOrder getSortOrder() {
        return sortOrder;
    }

    public FindNextFile setSortOrder(FileSortOrder sortOrder) {
        this.sortOrder = nonNull(sortOrder);
        return this;
    }

    public boolean isSinglePath() {
        return singlePath;
    }

    public FindNextFile setSinglePath(boolean singlePath) {
        this.singlePath = singlePath;
        return this;
    }

    public boolean isRecursiveScanning() {
        return recursiveScanning;
    }

    public FindNextFile setRecursiveScanning(boolean recursiveScanning) {
        this.recursiveScanning = recursiveScanning;
        return this;
    }

    public boolean isClearFileIndexOnReset() {
        return clearFileIndexOnReset;
    }

    public FindNextFile setClearFileIndexOnReset(boolean clearFileIndexOnReset) {
        this.clearFileIndexOnReset = clearFileIndexOnReset;
        return this;
    }

    public int currentFileIndex() {
        return currentFileIndex;
    }

    public int numberOfFiles() {
        return sortedFiles.size();
    }

    @Override
    public void initialize() {
        try {
            sortedFiles.clear();
            final Path fileOrFolder = completeFilePath();
            // - this function also fills some result ports by fillOutputFileInformation() method,
            // but they should be rewritten later in the process for every file
            if (singlePath || Files.isRegularFile(fileOrFolder)) {
                sortedFiles.add(fileOrFolder);
            } else {
                final String regularExpression = this.regularExpression.trim();
                final Pattern pattern = regularExpression.isEmpty() ? null : Pattern.compile(regularExpression);
                ListOfFiles.findFiles(sortedFiles, fileOrFolder, globPattern, pattern, recursiveScanning);
                if (isFileExistenceRequired() && sortedFiles.isEmpty()) {
                    throw new FileNotFoundException("No files in " + fileOrFolder
                            + ", corresponding to pattern " + globPattern);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
        Collections.sort(sortedFiles);
        sortedFilesString = sortedFiles.stream().map(String::valueOf).collect(Collectors.joining("\n"));
        if (clearFileIndexOnReset) {
            currentFileIndex = 0;
        }
    }


    @Override
    public void process() {
        final int numberOfFiles = sortedFiles.size();
        if (numberOfFiles == 0 && isFileExistenceRequired()) {
            throw new IllegalStateException("Illegal usage of process() method: "
                    + "initialize() was not successfully executed");
        }
        final int fileIndex = currentFileIndex;
        currentFileIndex++;
        if (currentFileIndex >= numberOfFiles) {
            currentFileIndex = 0;
        }
        final boolean last = currentFileIndex == 0;
        getScalar(OUTPUT_INDEX).setTo(fileIndex + 1);
        getScalar(OUTPUT_NUMBER_OF_FILES).setTo(numberOfFiles);
        getScalar(OUTPUT_LIST_OF_FILES).setTo(sortedFilesString);
        getScalar(OUTPUT_LAST).setTo(last);
        if (fileIndex >= numberOfFiles) {
            // - possible when !isFileExistenceRequired();
            // not necessary to clear the main output port for a case of a loop,
            // because this condition cannot be "false" and then become "true" during the loop
            return;
        }
        final Path fileToRead = sortedFiles.get(fileIndex).toAbsolutePath();
        final Path absolutePath = fileToRead.toAbsolutePath();
        final String result = absolutePath.toString();
        getScalar(OUTPUT_ABSOLUTE_PATH).setTo(result);
        getScalar(OUTPUT_PARENT_FOLDER).setTo(absolutePath.getParent().toString());
        getScalar(OUTPUT_FILE_NAME).setTo(absolutePath.getFileName().toString());
        final ReadImage readImage = ReadImage.getInstance();
        readImage.setFile(fileToRead.toString());
        getScalar().setTo(result);
    }

    @Override
    protected boolean nonEmptyPathRequired() {
        return true;
    }
}
