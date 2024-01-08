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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public enum FileSortOrder {
    STANDARD(Collections::sort),
    SUBDIRECTORIES_FIRST(paths -> paths.sort(((o1, o2) -> {
        final boolean file1 = Files.isRegularFile(o1);
        final boolean file2 = Files.isRegularFile(o2);
        return file1 != file2 ? (file2 ? -1 : 1) : o1.compareTo(o2);
    }))),
    SUBDIRECTORIES_LAST(paths -> paths.sort(((o1, o2) -> {
        final boolean file1 = Files.isRegularFile(o1);
        final boolean file2 = Files.isRegularFile(o2);
        return file1 != file2 ? (file1 ? -1 : 1) : o1.compareTo(o2);
    })));

    private final Consumer<List<Path>> sorter;

    FileSortOrder(Consumer<List<Path>> sorter) {
        this.sorter = sorter;
    }

    public void sort(List<Path> files) {
        Objects.requireNonNull(files, "Null files");
        sorter.accept(files);
    }
}
