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

package net.algart.executors.api.model;

import net.algart.executors.api.Executor;

import java.io.IOError;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

public final class ExecutorJsonSet {
    private static final Logger LOG = System.getLogger(Executor.class.getName());

    private final Map<String, ExecutorJson> executorJsons = new LinkedHashMap<>();
    private boolean immutable = false;

    private ExecutorJsonSet() {
    }

    public static ExecutorJsonSet newInstance() {
        return new ExecutorJsonSet();
    }

    public static ExecutorJsonSet allBuiltIn() {
        try {
            return findAllBuiltIn();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static ExecutorJsonSet findAllBuiltIn() throws IOException {
        return InstalledSetHolder.builtInSet();
    }

    public void add(ExecutorJson executorJson) {
        Objects.requireNonNull(executorJson, "Null executorJson");
        add(executorJson.getExecutorId(), executorJson);
    }

    public void add(String executorId, ExecutorJson executorJson) {
        add(executorId, executorJson, null);
    }

    public ExecutorJsonSet addFolder(Path folder, boolean onlyBuiltIn) throws IOException {
        return addFolder(folder, null, onlyBuiltIn);
    }

    public ExecutorJsonSet addFolder(Path folder, ExtensionJson.Platform platform, boolean onlyBuiltIn)
            throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        LOG.log(System.Logger.Level.TRACE, () -> "Adding executors folder " + folder);
        checkImmutable();
        if (!Files.exists(folder)) {
            throw new NoSuchFileException(folder.toString());
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (file.getFileName().toString().startsWith(".")) {
                    continue;
                }
                if (Files.isDirectory(file)) {
                    addFolder(file, platform, onlyBuiltIn);
                    continue;
                }
                if (Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(".json")) {
                    ExecutorJson executorJson = ExecutorJson.readIfValid(file);
                    if (executorJson != null) {
                        if (onlyBuiltIn && !executorJson.isJavaExecutor()) {
                            continue;
                        }
                        executorJson.addSystemExecutorIdPort();
                        if (platform != null) {
                            executorJson.updateCategoryPrefix(platform.getCategory());
                            executorJson.addTags(platform.getTags());
                            executorJson.setPlatformId(platform.getId());
                            executorJson.addSystemPlatformIdPort();
                            // - but not resource folder: for Java executors it is usually not helpful
                            // (PathPropertyReplacement works better)
                        }
                        add(executorJson.getExecutorId(), executorJson, file);
                        LOG.log(System.Logger.Level.TRACE,
                                () -> "Executor " + executorJson.getExecutorId() + " loaded from " + file);
                    } else {
                        LOG.log(System.Logger.Level.TRACE,
                                () -> "File " + file + " skipped: it is not an executor's JSON");
                    }
                }
            }
        }
        return this;
    }

    public void addInstalledModelFolders(boolean onlyBuiltIn) throws IOException {
        for (ExtensionJson.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
            if (onlyBuiltIn && !platform.isBuiltIn()) {
                continue;
            }
            if (!platform.hasModels()) {
                continue;
            }
            final Path folder = platform.modelsFolder();
            final long t1 = System.nanoTime();
            addFolder(folder, platform, onlyBuiltIn);
            final long t2 = System.nanoTime();
            LOG.log(System.Logger.Level.INFO, () -> String.format(Locale.US,
                    "Loading installed built-in executor models from %s: %.3f ms",
                    folder, (t2 - t1) * 1e-6));
        }
    }

    public Collection<ExecutorJson> all() {
        return Collections.unmodifiableCollection(executorJsons.values());
    }

    public boolean contains(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        return executorJsons.containsKey(executorId);
    }

    public ExecutorJson get(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        return executorJsons.get(executorId);
    }

    public ExecutorJson remove(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        checkImmutable();
        return executorJsons.remove(executorId);
    }

    private void add(String executorId, ExecutorJson executorJson, Path file) {
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(executorJson, "Null executorJson");
        // - No sense to store null values in the map
        if (immutable) {
            throw new UnsupportedOperationException("This executors json set is immutable");
        }
        if (executorJsons.putIfAbsent(executorId, executorJson) != null) {
            throw new IllegalArgumentException("Duplicate executor model: " + executorId
                    + (file == null ? "" : " in " + file));
        }
    }

    private void checkImmutable() {
        if (immutable) {
            throw new UnsupportedOperationException("This executors' json set is immutable");
        }
    }

    private static class InstalledSetHolder {
        private static ExecutorJsonSet installedSet = null;

        static synchronized ExecutorJsonSet builtInSet() throws IOException {
            // It is better than static initialization: this solution allows to see possible exceptions
            // (static initialization will lead to very "strange" exceptions like NoClassDefFound error,
            // because this class will stay not initialized)
            if (installedSet == null) {
                final ExecutorJsonSet newSet = ExecutorJsonSet.newInstance();
                newSet.addInstalledModelFolders(true);
                // - I/O exceptions possible
                final SpecialModelsBuilder builder = new SpecialModelsBuilder(newSet);
                builder.addSpecialModels();
                // - adding special models, which have no explicitly specified JSONs,
                // like executors, describing each platform - "clones" of CommonPlatformInformation
                newSet.immutable = true;
                installedSet = newSet;
            }
            return installedSet;
        }
    }
}
