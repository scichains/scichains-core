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

package net.algart.executors.api.js.core;

import net.algart.executors.api.js.JSCaller;
import net.algart.executors.api.js.JSSpecification;
import net.algart.graalvm.GraalSourceContainer;
import net.algart.executors.api.graalvm.GraalPlatforms;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UseJS extends FileOperation {
    public static final String JS_LANGUAGE_NAME = "JavaScript";

    private static final DefaultExecutorLoader<JSCaller> JS_CALLER_LOADER =
            new DefaultExecutorLoader<>("JS loader");

    static {
        globalLoaders().register(JS_CALLER_LOADER);
    }

    private String workingDirectory = ".";

    public UseJS() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseJS getInstance() {
        return new UseJS();
    }

    public static DefaultExecutorLoader<JSCaller> jsCallerLoader() {
        return JS_CALLER_LOADER;
    }

    @Override
    public UseJS setFile(String file) {
        super.setFile(file);
        return this;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public UseJS setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = nonEmptyTrimmed(workingDirectory);
        return this;
    }

    @Override
    public void process() {
        try {
            useSeveralPaths(completeSeveralFilePaths());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void useSeveralPaths(List<Path> jsSpecificationsPaths) throws IOException {
        Objects.requireNonNull(jsSpecificationsPaths, "Null paths to JS specifications files");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : jsSpecificationsPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path jsSpecificationsPaths) throws IOException {
        usePath(jsSpecificationsPaths, null, null);
    }

    public int usePath(
            Path jsSpecificationsPaths,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(jsSpecificationsPaths, "Null path to JS specification files");
        final List<JSSpecification> jsSpecifications;
        if (Files.isDirectory(jsSpecificationsPaths)) {
            jsSpecifications = JSSpecification.readAllIfValid(jsSpecificationsPaths);
        } else {
            jsSpecifications = Collections.singletonList(JSSpecification.read(jsSpecificationsPaths));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorSpecification.checkIdDifference(jsSpecifications);
        final int n = jsSpecifications.size();
        for (int i = 0; i < n; i++) {
            final JSSpecification jsSpecification = jsSpecifications.get(i);
            logDebug("Loading JS caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + jsSpecification.getSpecificationFile() + "...");
            if (platform != null) {
                jsSpecification.updateCategoryPrefix(platform.getCategory());
                jsSpecification.addTags(platform.getTags());
                jsSpecification.setPlatformId(platform.getId());
            }
            use(jsSpecification);
            if (report != null) {
                report.append(jsSpecification.getSpecificationFile()).append("\n");
            }
        }
        return n;
    }

    // Note: corrects the argument
    public void use(JSSpecification jsSpecification) throws IOException {
        final String sessionId = getSessionId();
        final Path workingDirectory = translateWorkingDirectory();
        correctJSExecutorSpecification(jsSpecification, workingDirectory);
        final JSCaller jsCaller = JSCaller.of(jsSpecification, workingDirectory);
        JS_CALLER_LOADER.registerWorker(sessionId, jsSpecification, jsCaller);
    }

    private Path translateWorkingDirectory() {
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(workingDirectory, this);
    }

    private void correctJSExecutorSpecification(JSSpecification jsSpecification, Path workingDirectory) {
        Objects.requireNonNull(jsSpecification, "Null jsSpecification");
        Objects.requireNonNull(workingDirectory, "Null workingDirectory");
        jsSpecification.setTo(new InterpretJS());
        // - adds the Java object, (maybe) parameters and some ports
        jsSpecification.addSystemExecutorIdPort();
        if (jsSpecification.hasPlatformId()) {
            jsSpecification.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(jsSpecification);
        jsSpecification.setSourceInfoForSpecification()
                .setLanguageName(JS_LANGUAGE_NAME)
                .setAbsoluteModulePath(workingDirectory.resolve(jsSpecification.getJS().getModule()));
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseJS useJS = UseJS.getInstance();
        useJS.setSessionId(GLOBAL_SHARED_SESSION_ID);
        for (ExtensionSpecification.Platform platform : GraalPlatforms.graalPlatforms().installedPlatforms()) {
            if (GraalSourceContainer.JAVASCRIPT_LANGUAGE.equals(platform.getLanguage())) {
                if (platform.hasSpecifications() && platform.hasModules()) {
                    final long t1 = System.nanoTime();
                    useJS.setWorkingDirectory(platform.modulesFolder().toString());
                    final int n = useJS.usePath(platform.specificationsFolder(), platform, null);
                    final long t2 = System.nanoTime();
                    logInfo(() -> String.format(Locale.US,
                            "Loading %d installed JS specifications from %s: %.3f ms",
                            n, platform.specificationsFolder(), (t2 - t1) * 1e-6));
                }
            }
        }
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        // nothing in this version
    }
}
