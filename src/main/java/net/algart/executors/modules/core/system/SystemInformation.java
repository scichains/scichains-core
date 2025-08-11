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

package net.algart.executors.modules.core.system;

import net.algart.jep.additions.GlobalPythonConfiguration;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledExtensions;
import net.algart.executors.api.system.ExecutorLoaderSet;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.ExecutorSpecificationSet;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Language;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class SystemInformation extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_CURRENT_DIRECTORY = "current_directory";
    public static final String OUTPUT_SESSION_ID = "session_id";
    public static final String OUTPUT_CONTEXT_ID = "context_id";
    public static final String OUTPUT_CONTEXT_NAME = "context_name";
    public static final String OUTPUT_CONTEXT_PATH = "context_path";

    @Override
    public void process() {
        getScalar(OUTPUT_CURRENT_DIRECTORY).setTo(getCurrentDirectory());
        getScalar(OUTPUT_SESSION_ID).setTo(getSessionId());
        getScalar(OUTPUT_CONTEXT_ID).setTo(getContextId());
        getScalar(OUTPUT_CONTEXT_NAME).setTo(getContextName());
        getScalar(OUTPUT_CONTEXT_PATH).setTo(getContextPath());
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
        sb.append(String.format("Java home: %s%n", System.getProperty("java.home")));
        sb.append(String.format("Current OS directory: %s%n", currentRelativePath));
        sb.append(String.format("Current chain directory: %s%n", getCurrentDirectory()));
        sb.append(String.format("Current context class loader: %s%n", Thread.currentThread().getContextClassLoader()));

        sb.append("Python (JEP):%n".formatted());
        final String pythonHome = GlobalPythonConfiguration.INSTANCE.pythonHomeInformation().pythonHome();
        sb.append("    Python home directory: %s%n".formatted(pythonHome == null ? "n/a" : pythonHome));
        try (Context context = Context.newBuilder().build()) {
            final Engine engine = context.getEngine();
            sb.append("GraalVM:%n".formatted());
            sb.append("    Graal version: %s%n".formatted(engine.getVersion()));
            sb.append("    Graal implementation: %s%n".formatted(engine.getImplementationName()));
            final Map<String, Language> languages = engine.getLanguages();
            sb.append("    Graal supported %d language%s: %n".formatted(
                    languages.size(), languages.size() == 1 ? "" : "s"));
            for (var e : languages.entrySet()) {
                final Language l = e.getValue();
                sb.append("        %s: id \"%s\", name \"%s\", implementation \"%s\", version \"%s\"%n".formatted(
                        e.getKey(), l.getId(), l.getName(), l.getImplementationName(), l.getVersion()));
            }
        }

        final ExecutorLoaderSet global = ExecutionBlock.globalLoaders();
        final Set<String> allSessions = global.allSessionIds();
        sb.append(String.format("%n%d sessions with installed executors:%n", allSessions.size()));
        for (String sessionId : allSessions) {
            sb.append(String.format("    %s: %d executors%n", sessionId,
                    global.allExecutorIds(sessionId, false).size()));
        }
        sb.append(String.format("%nInstalled extensions root:%n    %s%n",
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
                            "    specifications folder: %s%n" +
                            "    modules folder: %s%n" +
                            "    libraries folder: %s%n" +
                            "    resources folder: %s%n" +
                            "    resolved valid paths in classpath: [%s]%n" +
                            "]]",
                    platform.jsonString(),
                    platform.isBuiltIn() ? "  built-in," : "",
                    folderToString(platform.specificationsFolderOrNull()),
                    folderToString(platform.modulesFolderOrNull()),
                    folderToString(platform.librariesFolderOrNull()),
                    folderToString(platform.resourcesFolderOrNull()),
                    pathsToString(platform.validClassPaths())));
        }
        sb.append(joiner);

        sb.append(String.format("%n%nSystem properties:%n"));
        new TreeMap<>(System.getProperties()).forEach(
                (key, value) -> sb.append(String.format("    %s: \"%s\"%n", key, value)));
        // - TreeMap sorts properties alphabetically

        final Collection<ExecutorSpecification> allExecutors = ExecutorSpecificationSet.allBuiltIn().all();
        sb.append(String.format("%n%nAll %d executor specifications:%n", allExecutors.size()));
        allExecutors.forEach(
                executor -> sb.append(String.format("    %s    %s%n",
                        executor.canonicalName(),
                        executor.hasSpecificationFile() ? "--    " + executor.getSpecificationFile() : "(no file)")));
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
