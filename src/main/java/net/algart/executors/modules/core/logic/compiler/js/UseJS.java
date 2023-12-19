/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.compiler.js;

import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalPlatforms;
import net.algart.executors.api.SimpleExecutionBlockLoader;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.executors.modules.core.logic.compiler.js.interpreters.InterpretJS;
import net.algart.executors.modules.core.logic.compiler.js.model.JSCaller;
import net.algart.executors.modules.core.logic.compiler.js.model.JSCallerJson;

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

    private static final SimpleExecutionBlockLoader<JSCaller> JS_CALLER_LOADER =
            new SimpleExecutionBlockLoader<>("JS loader");

    static {
        registerExecutionBlockLoader(JS_CALLER_LOADER);
    }

    private String workingDirectory = ".";

    public UseJS() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseJS getInstance() {
        return new UseJS();
    }

    public static SimpleExecutionBlockLoader<JSCaller> jsCallerLoader() {
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

    public void useSeveralPaths(List<Path> jsCallerJsonPaths) throws IOException {
        Objects.requireNonNull(jsCallerJsonPaths, "Null paths to JS model JSON files");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : jsCallerJsonPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path jsCallerJsonPath) throws IOException {
        usePath(jsCallerJsonPath, null, null);
    }

    public void usePath(Path jsCallerJsonPath, ExtensionJson.Platform platform, StringBuilder report)
            throws IOException {
        Objects.requireNonNull(jsCallerJsonPath, "Null path to JS model JSON files");
        final List<JSCallerJson> jsCallerJsons;
        if (Files.isDirectory(jsCallerJsonPath)) {
            jsCallerJsons = JSCallerJson.readAllIfValid(jsCallerJsonPath);
        } else {
            jsCallerJsons = Collections.singletonList(JSCallerJson.read(jsCallerJsonPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorJson.checkIdDifference(jsCallerJsons);
        for (int i = 0, n = jsCallerJsons.size(); i < n; i++) {
            final JSCallerJson jsCallerJson = jsCallerJsons.get(i);
            logDebug("Loading JS caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + jsCallerJson.getExecutorJsonFile() + "...");
            if (platform != null) {
                jsCallerJson.updateCategoryPrefix(platform.getCategory());
                jsCallerJson.addTags(platform.getTags());
                jsCallerJson.setPlatformId(platform.getId());
            }
            use(jsCallerJson);
            if (report != null) {
                report.append(jsCallerJson.getExecutorJsonFile()).append("\n");
            }
        }
    }

    // Note: corrects the argument
    public void use(JSCallerJson jsCallerJson) throws IOException {
        final String sessionId = getSessionId();
        final Path workingDirectory = translateWorkingDirectory();
        correctJSExecutorModel(jsCallerJson, workingDirectory);
        final JSCaller jsCaller = JSCaller.valueOf(jsCallerJson, workingDirectory);
        JS_CALLER_LOADER.registerWorker(sessionId, jsCaller.executorId(), jsCaller, jsCallerJson);
    }

    private Path translateWorkingDirectory() {
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(workingDirectory, this);
    }

    private void correctJSExecutorModel(JSCallerJson jsCallerJson, Path workingDirectory) {
        Objects.requireNonNull(jsCallerJson, "Null jsCallerJson");
        Objects.requireNonNull(workingDirectory, "Null workingDirectory");
        jsCallerJson.setTo(new InterpretJS());
        // - adds JavaConf, (maybe) parameters and some ports
        jsCallerJson.addSystemExecutorIdPort();
        if (jsCallerJson.hasPlatformId()) {
            jsCallerJson.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(jsCallerJson);
        jsCallerJson.setSourceInfoForModel()
                .setLanguageName(JS_LANGUAGE_NAME)
                .setAbsoluteModulePath(workingDirectory.resolve(jsCallerJson.getJS().getModule()));
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseJS useJS = UseJS.getInstance();
        useJS.setSessionId(GLOBAL_SHARED_SESSION_ID);
        for (ExtensionJson.Platform platform : GraalPlatforms.graalPlatforms().installedPlatforms()) {
            if (GraalSourceContainer.JAVASCRIPT_LANGUAGE.equals(platform.getLanguage())) {
                if (platform.hasModels() && platform.hasModules()) {
                    final long t1 = System.nanoTime();
                    useJS.setWorkingDirectory(platform.modulesFolder().toString());
                    useJS.usePath(platform.modelsFolder(), platform, null);
                    final long t2 = System.nanoTime();
                    logInfo(() -> String.format(Locale.US,
                            "Loading installed JS models from %s: %.3f ms",
                            platform.modelsFolder(), (t2 - t1) * 1e-6));
                }
            }
        }
    }

    private static void addSpecialOutputPorts(ExecutorJson result) {
        // nothing in this version
    }
}
