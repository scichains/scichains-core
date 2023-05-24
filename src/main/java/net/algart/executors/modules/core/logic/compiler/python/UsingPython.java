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

package net.algart.executors.modules.core.logic.compiler.python;

import net.algart.bridges.jep.additions.JepGlobalConfig;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.SimpleExecutionBlockLoader;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;
import net.algart.executors.modules.core.logic.compiler.python.interpreters.InterpretPython;
import net.algart.executors.modules.core.logic.compiler.python.model.PythonCaller;
import net.algart.executors.modules.core.logic.compiler.python.model.PythonCallerJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UsingPython {
    public static final String PYTHON_LANGUAGE_NAME = "Python";
    public static final String SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME = "_py_supplied_python_roots";
    public static final String SUPPLIED_PYTHON_ROOTS_OUTPUT_CAPTION = "supplied python roots";
    public static final String SUPPLIED_PYTHON_ROOTS_OUTPUT_HINT =
            "List of Python root folders, supplied by this application and added to Python search paths";
    public static final String SUPPLIED_PYTHON_MODELS_OUTPUT_NAME = "_py_supplied_python_models";
    public static final String SUPPLIED_PYTHON_MODELS_OUTPUT_CAPTION = "supplied python models";
    public static final String SUPPLIED_PYTHON_MODELS_OUTPUT_HINT =
            "List of Python model folders, supplied by this application and used to find Python-based executors";

    private static final SimpleExecutionBlockLoader<PythonCaller> PYTHON_CALLER_LOADER =
            new SimpleExecutionBlockLoader<>("Python loader");

    static {
        ExecutionBlock.registerExecutionBlockLoader(PYTHON_CALLER_LOADER);
    }

    private UsingPython() {
    }

    public static SimpleExecutionBlockLoader<PythonCaller> pythonCallerLoader() {
        return PYTHON_CALLER_LOADER;
    }

    public static void usePath(String sessionId, Path pythonCallerJsonPath, ExtensionJson.Platform platform)
            throws IOException {
        Objects.requireNonNull(pythonCallerJsonPath, "Null path to Python model JSON files");
        final List<PythonCallerJson> pythonCallerJsons;
        if (Files.isDirectory(pythonCallerJsonPath)) {
            pythonCallerJsons = PythonCallerJson.readAllIfValid(pythonCallerJsonPath);
        } else {
            pythonCallerJsons = Collections.singletonList(PythonCallerJson.read(pythonCallerJsonPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorJson.checkIdDifference(pythonCallerJsons);
        for (int i = 0, n = pythonCallerJsons.size(); i < n; i++) {
            final PythonCallerJson pythonCallerJson = pythonCallerJsons.get(i);
            Executor.LOG.log(System.Logger.Level.DEBUG,
                    "Loading Python caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                            + "from " + pythonCallerJson.getExecutorJsonFile() + "...");
            if (platform != null) {
                pythonCallerJson.updateCategoryPrefix(platform.getCategory());
                pythonCallerJson.setPlatformId(platform.getId());
            }
            use(sessionId, pythonCallerJson);
        }
    }

    // Note: corrects the argument
    public static void use(String sessionId, PythonCallerJson pythonCallerJson) throws IOException {
        correctPythonExecutorModel(pythonCallerJson);
        final PythonCaller pythonCaller = PythonCaller.valueOf(pythonCallerJson);
        PYTHON_CALLER_LOADER.registerWorker(sessionId, pythonCaller.executorId(), pythonCaller, pythonCallerJson);
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        for (ExtensionJson.Platform platform : JepPlatforms.pythonPlatforms().installedPlatforms()) {
            if (platform.hasModels()) {
                final long t1 = System.nanoTime();
                UsingPython.usePath(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, platform.modelsFolder(), platform);
                final long t2 = System.nanoTime();
                Executor.LOG.log(System.Logger.Level.INFO, () -> String.format(Locale.US,
                        "Loading installed Python models from %s: %.3f ms",
                        platform.modelsFolder(), (t2 - t1) * 1e-6));
            }
        }
    }

    public static void initializePython() {
        JepGlobalConfig.INSTANCE.loadFromSystemProperties().useForJep();
    }

    private static void correctPythonExecutorModel(PythonCallerJson pythonCallerJson) {
        Objects.requireNonNull(pythonCallerJson, "Null pythonCallerJson");
        pythonCallerJson.setTo(new InterpretPython());
        // - adds JavaConf, (maybe) parameters and some ports
        pythonCallerJson.addSystemExecutorIdPort();
        if (pythonCallerJson.hasPlatformId()) {
            pythonCallerJson.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(pythonCallerJson);
        pythonCallerJson.setSourceInfoForModel().setLanguageName(PYTHON_LANGUAGE_NAME);
    }

    private static void addSpecialOutputPorts(ExecutorJson result) {
        if (!result.getOutPorts().containsKey(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutPort(new ExecutorJson.PortConf()
                    .setName(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_ROOTS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_ROOTS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
        if (!result.getOutPorts().containsKey(SUPPLIED_PYTHON_MODELS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutPort(new ExecutorJson.PortConf()
                    .setName(SUPPLIED_PYTHON_MODELS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_MODELS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_MODELS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

}
