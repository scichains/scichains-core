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

package net.algart.executors.api.js;

import net.algart.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalPlatforms;
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

    public void useSeveralPaths(List<Path> jsCallerSpecificationsPaths) throws IOException {
        Objects.requireNonNull(jsCallerSpecificationsPaths, "Null paths to JS specifications files");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : jsCallerSpecificationsPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path jsCallerSpecificationsPaths) throws IOException {
        usePath(jsCallerSpecificationsPaths, null, null);
    }

    public int usePath(
            Path jsCallerSpecificationsPaths,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(jsCallerSpecificationsPaths, "Null path to JS specification files");
        final List<JSCallerSpecification> jsCallerSpecifications;
        if (Files.isDirectory(jsCallerSpecificationsPaths)) {
            jsCallerSpecifications = JSCallerSpecification.readAllIfValid(jsCallerSpecificationsPaths);
        } else {
            jsCallerSpecifications = Collections.singletonList(JSCallerSpecification.read(jsCallerSpecificationsPaths));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorSpecification.checkIdDifference(jsCallerSpecifications);
        final int n = jsCallerSpecifications.size();
        for (int i = 0; i < n; i++) {
            final JSCallerSpecification jsCallerSpecification = jsCallerSpecifications.get(i);
            logDebug("Loading JS caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + jsCallerSpecification.getSpecificationFile() + "...");
            if (platform != null) {
                jsCallerSpecification.updateCategoryPrefix(platform.getCategory());
                jsCallerSpecification.addTags(platform.getTags());
                jsCallerSpecification.setPlatformId(platform.getId());
            }
            use(jsCallerSpecification);
            if (report != null) {
                report.append(jsCallerSpecification.getSpecificationFile()).append("\n");
            }
        }
        return n;
    }

    // Note: corrects the argument
    public void use(JSCallerSpecification jsCallerSpecification) throws IOException {
        final String sessionId = getSessionId();
        final Path workingDirectory = translateWorkingDirectory();
        correctJSExecutorSpecification(jsCallerSpecification, workingDirectory);
        final JSCaller jsCaller = JSCaller.of(jsCallerSpecification, workingDirectory);
        JS_CALLER_LOADER.registerWorker(sessionId, jsCallerSpecification, jsCaller);
    }

    private Path translateWorkingDirectory() {
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(workingDirectory, this);
    }

    private void correctJSExecutorSpecification(JSCallerSpecification jsCallerSpecification, Path workingDirectory) {
        Objects.requireNonNull(jsCallerSpecification, "Null jsCallerSpecification");
        Objects.requireNonNull(workingDirectory, "Null workingDirectory");
        jsCallerSpecification.setTo(new InterpretJS());
        // - adds the Java object, (maybe) parameters and some ports
        jsCallerSpecification.addSystemExecutorIdPort();
        if (jsCallerSpecification.hasPlatformId()) {
            jsCallerSpecification.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(jsCallerSpecification);
        jsCallerSpecification.setSourceInfoForSpecification()
                .setLanguageName(JS_LANGUAGE_NAME)
                .setAbsoluteModulePath(workingDirectory.resolve(jsCallerSpecification.getJS().getModule()));
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
