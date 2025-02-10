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

package net.algart.executors.api.python;

import net.algart.bridges.jep.additions.JepGlobalConfig;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;

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
    public static final String SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME = "_py_supplied_python_specification";
    public static final String SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_CAPTION = "supplied python specification";
    public static final String SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_HINT =
            "List of Python executor specification folders, " +
                    "supplied by this application and used to find Python-based executors";

    private static final DefaultExecutorLoader<PythonCaller> PYTHON_CALLER_LOADER =
            new DefaultExecutorLoader<>("Python loader");

    static {
        ExecutionBlock.globalLoaders().register(PYTHON_CALLER_LOADER);
    }

    private UsingPython() {
    }

    public static DefaultExecutorLoader<PythonCaller> pythonCallerLoader() {
        return PYTHON_CALLER_LOADER;
    }

    public static int usePath(
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
        final int n = pythonCallerSpecifications.size();
        for (int i = 0; i < n; i++) {
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
        return n;
    }

    // Note: corrects the argument
    public static void use(String sessionId, PythonCallerSpecification pythonCallerSpecification) throws IOException {
        correctPythonExecutorSpecification(pythonCallerSpecification);
        final PythonCaller pythonCaller = PythonCaller.of(pythonCallerSpecification);
        PYTHON_CALLER_LOADER.registerWorker(sessionId, pythonCallerSpecification, pythonCaller);
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        for (ExtensionSpecification.Platform platform : JepPlatforms.pythonPlatforms().installedPlatforms()) {
            if (platform.hasSpecifications()) {
                final long t1 = System.nanoTime();
                final int n = UsingPython.usePath(
                        ExecutionBlock.GLOBAL_SHARED_SESSION_ID, platform.specificationsFolder(), platform);
                final long t2 = System.nanoTime();
                Executor.LOG.log(System.Logger.Level.INFO, () -> String.format(Locale.US,
                        "Loading %d installed Python specifications from %s: %.3f ms",
                        n, platform.specificationsFolder(), (t2 - t1) * 1e-6));
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
        pythonCallerSpecification.setSourceInfoForSpecification().setLanguageName(PYTHON_LANGUAGE_NAME);
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        if (!result.getOutputPorts().containsKey(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutputPort(new ExecutorSpecification.PortConf()
                    .setName(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_ROOTS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_ROOTS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
        if (!result.getOutputPorts().containsKey(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutputPort(new ExecutorSpecification.PortConf()
                    .setName(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

}
