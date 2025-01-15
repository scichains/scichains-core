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

package net.algart.executors.api.system;

import net.algart.executors.api.Executor;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledExtensions;

import java.io.IOError;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

public final class ExecutorSpecificationSet {
    private static final Logger LOG = System.getLogger(Executor.class.getName());

    private final Map<String, ExecutorSpecification> specifications = new LinkedHashMap<>();
    private boolean immutable = false;

    private ExecutorSpecificationSet() {
    }

    public static ExecutorSpecificationSet newInstance() {
        return new ExecutorSpecificationSet();
    }

    public static ExecutorSpecificationSet allBuiltIn() {
        try {
            return findAllBuiltIn();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static ExecutorSpecificationSet findAllBuiltIn() throws IOException {
        return InstalledSetHolder.builtInSet();
    }

    public void add(ExecutorSpecification specification) {
        Objects.requireNonNull(specification, "Null specification");
        add(specification.getId(), specification);
    }

    public void add(String executorId, ExecutorSpecification specification) {
        add(executorId, specification, null);
    }

    public ExecutorSpecificationSet addFolder(Path folder, boolean onlyBuiltIn) throws IOException {
        return addFolder(folder, null, onlyBuiltIn);
    }

    public ExecutorSpecificationSet addFolder(
            Path folder,
            ExtensionSpecification.Platform platform,
            boolean onlyBuiltIn)
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
                if (Files.isRegularFile(file) && ExecutorSpecification.isExecutorSpecificationFile(file)) {
                    ExecutorSpecification specification = ExecutorSpecification.readIfValid(file);
                    if (specification != null) {
                        if (onlyBuiltIn && !specification.isJavaExecutor()) {
                            continue;
                        }
                        specification.addSystemExecutorIdPort();
                        if (platform != null) {
                            specification.updateCategoryPrefix(platform.getCategory());
                            specification.addTags(platform.getTags());
                            specification.setPlatformId(platform.getId());
                            specification.addSystemPlatformIdPort();
                            // - but not resource folder: for Java executors it is usually not helpful
                            // (PathPropertyReplacement works better)
                        }
                        add(specification.getId(), specification, file);
                        LOG.log(System.Logger.Level.TRACE,
                                () -> "Executor " + specification.getId() + " loaded from " + file);
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
        for (ExtensionSpecification.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
            if (onlyBuiltIn && !platform.isBuiltIn()) {
                continue;
            }
            if (!platform.hasSpecifications()) {
                continue;
            }
            final Path folder = platform.specificationsFolder();
            final long t1 = System.nanoTime();
            addFolder(folder, platform, onlyBuiltIn);
            final long t2 = System.nanoTime();
            LOG.log(System.Logger.Level.INFO, () -> String.format(Locale.US,
                    "Loading installed built-in executor specifications from %s: %.3f ms",
                    folder, (t2 - t1) * 1e-6));
        }
    }

    public Collection<ExecutorSpecification> all() {
        return Collections.unmodifiableCollection(specifications.values());
    }

    public boolean contains(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        return specifications.containsKey(executorId);
    }

    public ExecutorSpecification get(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        return specifications.get(executorId);
    }

    public ExecutorSpecification remove(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        checkImmutable();
        return specifications.remove(executorId);
    }

    private void add(String executorId, ExecutorSpecification executorSpecification, Path file) {
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(executorSpecification, "Null executorSpecification");
        // - No sense to store null values in the map
        if (immutable) {
            throw new UnsupportedOperationException("This executors json set is immutable");
        }
        if (specifications.putIfAbsent(executorId, executorSpecification) != null) {
            throw new IllegalArgumentException("Duplicate executor ID: " + executorId
                    + (file == null ? "" : " in " + file));
        }
    }

    private void checkImmutable() {
        if (immutable) {
            throw new UnsupportedOperationException("This executors' json set is immutable");
        }
    }

    private static class InstalledSetHolder {
        private static ExecutorSpecificationSet installedSet = null;

        static synchronized ExecutorSpecificationSet builtInSet() throws IOException {
            // It is better than static initialization: this solution allows seeing possible exceptions
            // (static initialization will lead to very "strange" exceptions like NoClassDefFound error,
            // because this class will stay not initialized)
            if (installedSet == null) {
                final ExecutorSpecificationSet newSet = ExecutorSpecificationSet.newInstance();
                newSet.addInstalledModelFolders(true);
                // - I/O exceptions possible
                final SpecialSpecificationsBuilder builder = new SpecialSpecificationsBuilder(newSet);
                builder.addSpecifications();
                // - adding special specifications, which have no explicitly specified JSONs,
                // like executors, describing each platform - "clone" of CommonPlatformInformation
                newSet.immutable = true;
                installedSet = newSet;
            }
            return installedSet;
        }
    }
}
