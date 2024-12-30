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

package net.algart.executors.modules.core.logic.compiler.python;

import net.algart.bridges.jep.additions.JepGlobalConfig;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.SimpleExecutorLoader;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.model.ExecutorSpecification;
import net.algart.executors.api.model.ExtensionSpecification;
import net.algart.executors.modules.core.logic.compiler.python.interpreters.InterpretPython;
import net.algart.executors.modules.core.logic.compiler.python.model.PythonCaller;
import net.algart.executors.modules.core.logic.compiler.python.model.PythonCallerSpecification;

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

    private static final SimpleExecutorLoader<PythonCaller> PYTHON_CALLER_LOADER =
            new SimpleExecutorLoader<>("Python loader");

    static {
        ExecutionBlock.registerExecutorLoader(PYTHON_CALLER_LOADER);
    }

    private UsingPython() {
    }

    public static SimpleExecutorLoader<PythonCaller> pythonCallerLoader() {
        return PYTHON_CALLER_LOADER;
    }

    public static void usePath(
            String sessionId,
            Path pythonCallerSpecificationPath,
            ExtensionSpecification.Platform platform)
            throws IOException {
        Objects.requireNonNull(pythonCallerSpecificationPath, "Null path to Python specification files");
        final List<PythonCallerSpecification> pythonCallerSpecifications;
        if (Files.isDirectory(pythonCallerSpecificationPath)) {
            pythonCallerSpecifications = PythonCallerSpecification.readAllIfValid(pythonCallerSpecificationPath);
        } else {
            pythonCallerSpecifications = Collections.singletonList(
                    PythonCallerSpecification.read(pythonCallerSpecificationPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorSpecification.checkIdDifference(pythonCallerSpecifications);
        for (int i = 0, n = pythonCallerSpecifications.size(); i < n; i++) {
            final PythonCallerSpecification pythonCallerSpecification = pythonCallerSpecifications.get(i);
            Executor.LOG.log(System.Logger.Level.DEBUG,
                    "Loading Python caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                            + "from " + pythonCallerSpecification.getExecutorSpecificationFile() + "...");
            if (platform != null) {
                pythonCallerSpecification.updateCategoryPrefix(platform.getCategory());
                pythonCallerSpecification.addTags(platform.getTags());
                pythonCallerSpecification.setPlatformId(platform.getId());
            }
            use(sessionId, pythonCallerSpecification);
        }
    }

    // Note: corrects the argument
    public static void use(String sessionId, PythonCallerSpecification pythonCallerSpecification) throws IOException {
        correctPythonExecutorSpecification(pythonCallerSpecification);
        final PythonCaller pythonCaller = PythonCaller.valueOf(pythonCallerSpecification);
        PYTHON_CALLER_LOADER.registerWorker(
                sessionId, pythonCaller.executorId(), pythonCaller, pythonCallerSpecification);
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        for (ExtensionSpecification.Platform platform : JepPlatforms.pythonPlatforms().installedPlatforms()) {
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

    private static void correctPythonExecutorSpecification(PythonCallerSpecification pythonCallerSpecification) {
        Objects.requireNonNull(pythonCallerSpecification, "Null pythonCallerSpecification");
        pythonCallerSpecification.setTo(new InterpretPython());
        // - adds JavaConf, (maybe) parameters and some ports
        pythonCallerSpecification.addSystemExecutorIdPort();
        if (pythonCallerSpecification.hasPlatformId()) {
            pythonCallerSpecification.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(pythonCallerSpecification);
        pythonCallerSpecification.setSourceInfoForModel().setLanguageName(PYTHON_LANGUAGE_NAME);
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        if (!result.getOutPorts().containsKey(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutPort(new ExecutorSpecification.PortConf()
                    .setName(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_ROOTS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_ROOTS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
        if (!result.getOutPorts().containsKey(SUPPLIED_PYTHON_MODELS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutPort(new ExecutorSpecification.PortConf()
                    .setName(SUPPLIED_PYTHON_MODELS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_MODELS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_MODELS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

}
