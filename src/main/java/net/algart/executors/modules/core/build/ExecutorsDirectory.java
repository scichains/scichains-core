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

package net.algart.executors.modules.core.build;

import jakarta.json.JsonObject;
import net.algart.executors.api.SystemEnvironment;
import net.algart.executors.api.model.ExecutorJson;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.StreamSupport;

public final class ExecutorsDirectory {
    private static final List<String> POSSIBLE_BLOCK_KINDS = Collections.unmodifiableList(Arrays.asList(
            "function", "input", "output", "data"));

    private final Set<String> ids = new HashSet<>();
    private final Set<String> instantiationNames = new HashSet<>();

    private boolean descriptions = false;

    private String lastCategory = null;

    private void show(Path f) throws IOException {
        final JsonObject json = ExecutorJsonVerifier.readExecutorJson(f, true);
        if (json == null) {
            return;
        }
        final ExecutorJson model = ExecutorJson.valueOf(json);
        final String category = model.getCategory();
        if (!category.equals(lastCategory)) {
            System.out.printf("%n%s%n", category);
        }
        lastCategory = category;
        System.out.printf("  %s%n", model.getName());
        if (descriptions) {
            final String description = model.getDescription();
            if (description != null) {
                System.out.printf("    %s%n", description);
            }
        }
    }

    private void showAll(Path folder) throws IOException {
        try (final DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            StreamSupport.stream(files.spliterator(), false)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(file -> {
                        try {
                            if (Files.isDirectory(file)) {
                                showAll(file);
                            } else if (file.getFileName().toString().endsWith(".json")) {
                                show(file);
                            }
                        } catch (IOException e) {
                            throw new IOError(e);
                        }
                    });
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorsDirectory lister = new ExecutorsDirectory();
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-descriptions")) {
            lister.descriptions = true;
            startArgIndex++;
        }
        if (args.length == startArgIndex) {
            System.out.printf("Usage: %s [-descriptions] folder1_with_json_files folder2_with_json_files...s%n",
                    ExecutorsDirectory.class.getName());
            return;
        }
        for (int k = startArgIndex; k < args.length; k++) {
            final String path = SystemEnvironment.replaceHomeEnvironmentVariable(args[k]);
            final Path folder = Paths.get(path);
            System.out.printf("%s...%n", folder);
            lister.showAll(folder);
            System.out.println();
        }
    }

}
