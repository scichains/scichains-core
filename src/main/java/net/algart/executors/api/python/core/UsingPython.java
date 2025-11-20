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

package net.algart.executors.api.python.core;

import net.algart.executors.api.python.JepCaller;
import net.algart.executors.api.python.PythonSpecification;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.GlobalPythonConfiguration;
import net.algart.executors.api.jep.JepPlatforms;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.PortSpecification;

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

    private static final DefaultExecutorLoader<JepCaller> JEP_CALLER_LOADER =
            new DefaultExecutorLoader<>("Python JEP loader");

    static {
        ExecutionBlock.globalLoaders().register(JEP_CALLER_LOADER);
    }

    private UsingPython() {
    }

    public static DefaultExecutorLoader<JepCaller> jepCallerLoader() {
        return JEP_CALLER_LOADER;
    }

    public static int usePath(
            String sessionId,
            Path pythonSpecificationPath,
            ExtensionSpecification.Platform platform)
            throws IOException {
        Objects.requireNonNull(pythonSpecificationPath, "Null path to Python specification files");
        final List<PythonSpecification> pythonSpecifications;
        if (Files.isDirectory(pythonSpecificationPath)) {
            pythonSpecifications = PythonSpecification.readAllIfValid(pythonSpecificationPath);
        } else {
            pythonSpecifications = Collections.singletonList(
                    PythonSpecification.read(pythonSpecificationPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        ExecutorSpecification.checkIdDifference(pythonSpecifications);
        final int n = pythonSpecifications.size();
        for (int i = 0; i < n; i++) {
            final PythonSpecification pythonSpecification = pythonSpecifications.get(i);
            Executor.LOG.log(System.Logger.Level.DEBUG,
                    "Loading Python caller " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                            + "from " + pythonSpecification.getSpecificationFile() + "...");
            if (platform != null) {
                pythonSpecification.updateCategoryPrefix(platform.getCategory());
                pythonSpecification.addTags(platform.getTags());
                pythonSpecification.setPlatformId(platform.getId());
            }
            use(sessionId, pythonSpecification);
        }
        return n;
    }

    // Note: corrects the argument
    public static void use(String sessionId, PythonSpecification pythonSpecification) throws IOException {
        correctPythonExecutorSpecification(pythonSpecification);
        final JepCaller jepCaller = JepCaller.of(pythonSpecification);
        JEP_CALLER_LOADER.registerWorker(sessionId, pythonSpecification, jepCaller);
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
        JepPerformerContainer.disableNoConfiguration();
        GlobalPythonConfiguration.INSTANCE.loadFromSystemProperties().useForJep();
        final String pythonHome = GlobalPythonConfiguration.INSTANCE.pythonHome().home();
        Executor.LOG.log(System.Logger.Level.INFO, () -> "Python home: " + (pythonHome == null ? "n/a" : pythonHome));
    }

    private static void correctPythonExecutorSpecification(PythonSpecification pythonSpecification) {
        Objects.requireNonNull(pythonSpecification, "Null pythonSpecification");
        pythonSpecification.setTo(new InterpretPython());
        // - adds JavaConf, (maybe) parameters and some ports
        pythonSpecification.addSystemExecutorIdPort();
        if (pythonSpecification.hasPlatformId()) {
            pythonSpecification.addSystemPlatformIdPort();
        }
        addSpecialOutputPorts(pythonSpecification);
        pythonSpecification.setSourceInfoForSpecification().setLanguageName(PYTHON_LANGUAGE_NAME);
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        if (!result.getOutputPorts().containsKey(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutputPort(new PortSpecification()
                    .setName(SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_ROOTS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_ROOTS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
        if (!result.getOutputPorts().containsKey(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME)) {
            // - to be on the safe side (maybe, the user defined the output port with the same name)
            result.addOutputPort(new PortSpecification()
                    .setName(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME)
                    .setCaption(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_CAPTION)
                    .setHint(SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

}
