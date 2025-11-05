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

import java.nio.file.Files;
import java.nio.file.Path;

public final class CheckFileExistence extends FileOperation implements ReadOnlyExecutionInput {
    public static final String OUTPUT_IS_EXISTING_FILE = "is_existing_file";
    public static final String OUTPUT_IS_EXISTING_FOLDER = "is_existing_folder";

    private String whenNotExists = "0";
    private String whenExists = "1";

    public CheckFileExistence() {
        //noinspection resource
        setFileExistenceRequired(false);
        addInputScalar(INPUT_FILE);
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_IS_EXISTING_FILE);
        addOutputScalar(OUTPUT_IS_EXISTING_FOLDER);
        addOutputScalar(OUTPUT_ABSOLUTE_PATH);
    }

    public String getWhenNotExists() {
        return whenNotExists;
    }

    public CheckFileExistence setWhenNotExists(String whenNotExists) {
        this.whenNotExists = whenNotExists;
        return this;
    }

    public String getWhenExists() {
        return whenExists;
    }

    public CheckFileExistence setWhenExists(String whenExists) {
        this.whenExists = whenExists;
        return this;
    }

    @Override
    public void process() {
        final Path f = completeFilePath();
        // - this function also fills the result port OUTPUT_ABSOLUTE_PATH

        getScalar().setTo(f != null && Files.exists(f) ? whenExists : whenNotExists);
        getScalar(OUTPUT_IS_EXISTING_FILE).setTo(f != null && Files.isRegularFile(f) ? whenExists : whenNotExists);
        getScalar(OUTPUT_IS_EXISTING_FOLDER).setTo(f != null && Files.isDirectory(f) ? whenExists : whenNotExists);
    }
}
