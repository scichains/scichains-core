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

package net.algart.executors.modules.core.system;

import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.model.ExecutorSpecification;
import net.algart.executors.api.model.ExecutorSpecificationSet;
import net.algart.executors.api.model.ExtensionSpecification;
import net.algart.executors.api.model.InstalledExtensions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public final class SystemInformation extends Executor implements ReadOnlyExecutionInput {
    @Override
    public void process() {
        getScalar().setTo(information());
    }

    public String information() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Session ID: %s%n", getSessionId()));
        sb.append(String.format("Context ID: %s%n", getContextId()));
        sb.append(String.format("Context name: %s%n", getContextName()));
        sb.append(String.format("Context path: %s%n", getContextPath()));
        sb.append(String.format("This executor ID: %s%n", getExecutorId()));
        sb.append(String.format("Number of active threads: %d%n", Thread.activeCount()));
        sb.append(String.format("Number of CPU units: %d%n", Runtime.getRuntime().availableProcessors()));
        final Path currentRelativePath = Paths.get("").toAbsolutePath();
        sb.append(String.format("Current OS directory: %s%n", currentRelativePath));
        sb.append(String.format("Current directory: %s%n", getCurrentDirectory()));
        sb.append(String.format("Current context class loader: %s%n", Thread.currentThread().getContextClassLoader()));
        sb.append(String.format("Installed extensions root:%n    %s%n",
                InstalledExtensions.EXTENSIONS_ROOT));
        sb.append(String.format("Installed extensions path list:%n    %s%n",
                InstalledExtensions.EXTENSIONS_PATH));
        try {
            sb.append(String.format("%nInstalled executors path:%n    %s%n",
                    InstalledExtensions.installedExtensionsPaths().stream()
                            .map(Path::toString).collect(Collectors.joining(String.format("%n    ")))));
        } catch (Exception e) {
            sb.append("%nCANNOT find installed executors paths: ").append(e);
        }

        final String classPath = System.getProperty("java.class.path");
        sb.append(String.format("%nJava class path:%n    %s%n",
                classPath == null ? "n/a" : classPath.replace(File.pathSeparator, String.format("%n    "))));

        sb.append(String.format("%nSupplied Python root folders:%n    %s%n",
                String.join(String.format("%n    "), JepPlatforms.pythonRootFolders())));

        sb.append(String.format("%nInstalled platforms:%n"));
        final StringJoiner joiner = new StringJoiner(String.format(",%n"));
        for (ExtensionSpecification.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
            joiner.add(String.format("%s%n[[%s%n" +
                            "    models folder: %s%n" +
                            "    modules folder: %s%n" +
                            "    libraries folder: %s%n" +
                            "    resources folder: %s%n" +
                            "    resolved valid paths in classpath: [%s]%n" +
                            "]]",
                    platform.jsonString(),
                    platform.isBuiltIn() ? "  built-in," : "",
                    folderToString(platform.modelsFolderOrNull()),
                    folderToString(platform.modulesFolderOrNull()),
                    folderToString(platform.librariesFolderOrNull()),
                    folderToString(platform.resourcesFolderOrNull()),
                    pathsToString(platform.validClassPaths())));
        }
        sb.append(joiner);

        sb.append(String.format("%n%nSystem properties:%n"));
        System.getProperties().forEach((key, value) -> sb.append(String.format("    %s: \"%s\"%n", key, value)));

        final Collection<ExecutorSpecification> allExecutors = ExecutorSpecificationSet.allBuiltIn().all();
        sb.append(String.format("%n%nAll %d executor models:%n", allExecutors.size()));
        allExecutors.forEach(
                executor -> sb.append(String.format("    %s    %s%n",
                        executor.getCanonicalName(),
                        executor.hasExecutorSpecificationFile() ? "--    " + executor.getExecutorSpecificationFile() : "(no file)")));
        return sb.toString();
    }

    private static String folderToString(Path folder) {
        return folder == null ? "N/A" :
                Files.exists(folder) ? folder.toString() : folder + " - NOT exists!";
    }

    private static String pathsToString(Collection<Path> paths) {
        if (paths.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Path path : paths) {
            sb.append(String.format("%n        %s", path));
        }
        sb.append(String.format("%n    "));
        return sb.toString();
    }

    public static void main(String[] args) {
        try (SystemInformation e = new SystemInformation()) {
            System.out.println(e.information());
        }
    }
}
