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

package net.algart.bridges.jep.api;

import jep.*;
import jep.python.PyCallable;
import jep.python.PyObject;
import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.*;

import java.nio.Buffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class JepAPI {
    public static final boolean REQUIRE_NUMPY_INTEGRATION = net.algart.arrays.Arrays.SystemSettings.getBooleanProperty(
            "net.algart.jep.numpyIntegrationRequired", true);

    private static final AtomicBoolean NUMPY_INTEGRATION_PROBLEM_LOGGED = new AtomicBoolean(false);
    private static final System.Logger LOG = System.getLogger(JepAPI.class.getName());

    public static final String STANDARD_API_PACKAGE = "pyalgart";
    // Note: simple "algart" name leads to error, probably due to the existing "net.algart" Java package
    public static final String STANDARD_API_MODULE = STANDARD_API_PACKAGE + ".api";
    public static final String STANDARD_API_MODULE_ALIAS = "pya";
    // Note: there is no a constant for "Environment" class: we do not create this class outside Python code!
    public static final String STANDARD_API_PARAMETERS_CLASS_NAME = STANDARD_API_MODULE + ".Parameters";
    public static final String STANDARD_API_INPUTS_CLASS_NAME = STANDARD_API_MODULE + ".Inputs";
    public static final String STANDARD_API_OUTPUTS_CLASS_NAME = STANDARD_API_MODULE + ".Outputs";
    public static final String STANDARD_API_ENVIRONMENT_VARIABLE = "_env";
    public static final String STANDARD_API_PARAMETER_EXECUTOR = "executor";
    public static final String STANDARD_API_PARAMETER_PLATFORM = "platform";
    public static final String STANDARD_API_PARAMETER_WORKING_DIRECTORY = "working_dir";
    public static final String STANDARD_API_JEP_VERIFIER = STANDARD_API_PACKAGE + ".jep_verifier";
    public static final String STANDARD_API_JEP_VERIFIER_FUNCTION = STANDARD_API_JEP_VERIFIER + ".returnTestNdArray";
    public static final List<String> STANDARD_STARTUP = List.of(
            "import " + STANDARD_API_MODULE);
    public static final List<String> STANDARD_STARTUP_SHARED = List.of(
            "import numpy",
            "import " + STANDARD_API_MODULE + " as " + STANDARD_API_MODULE_ALIAS,
            "import " + STANDARD_API_JEP_VERIFIER);

    // In the current version, this class has not any settings, but maybe they will be added in the future
    private JepAPI() {
    }

    public static JepAPI getInstance() {
        return new JepAPI();
    }

    public static JepPerformerContainer getContainer(JepInterpretation.Kind kind) {
        return initialize(JepPerformerContainer.getContainer(kind));
    }

    public void loadParameters(Executor executor, AtomicPyObject parameters) {
        loadParameters(executor.parameters(), parameters);
    }

    public void loadParameters(Map<String, Object> parametersMap, AtomicPyObject parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        Objects.requireNonNull(parametersMap, "Null parametersMap");
        for (Map.Entry<String, Object> entry : parametersMap.entrySet()) {
            parameters.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public void initializedGlobalEnvironment(JepPerformer performer, Executor executor, Path workingDirectory) {
        Objects.requireNonNull(performer, "Null performer");
        if (performer.kind().isPure()) {
            // in "pure" Python (sub-interpreters) this is dangerous even to use SubInterpreter.getValue();
            // but really this is not enough: other operations also do not work well
            return;
        }
        try (AtomicPyObject environment = performer.getObject(
                STANDARD_API_MODULE + "." + STANDARD_API_ENVIRONMENT_VARIABLE)) {
            environment.setAttribute(STANDARD_API_PARAMETER_EXECUTOR, executor);
            environment.setAttribute(STANDARD_API_PARAMETER_PLATFORM, executor.executorPlatform());
            Path currentDirectory = workingDirectory != null ? workingDirectory : executor.getCurrentDirectory();
            environment.setAttribute(STANDARD_API_PARAMETER_WORKING_DIRECTORY,
                    currentDirectory == null ? null : currentDirectory.toString());
        }
    }

    public void readInputPorts(JepPerformer performer, Collection<Port> inputPorts, AtomicPyObject inputs) {
        Objects.requireNonNull(inputs, "Null inputs");
        Objects.requireNonNull(inputPorts, "Null inputPorts");
        for (Port port : inputPorts) {
            inputs.setAttribute(port.getName(), readInputPort(performer, port));
        }
    }

    public void writeOutputPorts(JepPerformer performer, Collection<Port> outputPorts, AtomicPyObject outputs) {
        Objects.requireNonNull(outputs, "Null outputs");
        Objects.requireNonNull(outputPorts, "Null outputPorts");
        for (Port port : outputPorts) {
            writeOutputPort(performer, port, outputs.getAttributeOrNull(port.getName()));
        }
    }

    public Object readInputPort(JepPerformer performer, Port port) {
        Objects.requireNonNull(port, "Null port");
        if (!port.isInput()) {
            throw new IllegalArgumentException("Non-input port: " + port);
        }
        final Data data = port.getData();
        Object value = null;
        // - if there is no data in port, we still create a correct Python field
        if (data != null && data.isInitialized()) {
            // - normally data == null is impossible
            value = switch (data.type()) {
                case SCALAR -> loadScalarToJep(performer, port);
                case NUMBERS -> loadNumbersToJep(performer, port);
                case MAT -> loadMatToJep(performer, port);
            };
        }
        return value;
    }

    public void writeOutputPort(JepPerformer performer, Port port, Object value) {
        writeOutputPort(performer, port, value, false);
    }

    public void writeOutputPort(JepPerformer performer, Port port, Object value, boolean preserveExisting) {
        Objects.requireNonNull(port, "Null port");
        if (!port.isOutput()) {
            throw new IllegalArgumentException("Non-output port: " + port);
        }
        if (preserveExisting && port.hasData()) {
            return;
        }
        final DataType dataType = port.getDataType();
        if (value == null) {
            port.removeData();
        } else if (dataType != null) {
            // - normally port.getDataType() == null is impossible
            switch (dataType) {
                case SCALAR -> storeScalarFromJep(performer, port, value);
                case NUMBERS -> storeNumbersFromJep(performer, port, value);
                case MAT -> storeMatFromJep(performer, port, value);
            }
        }
    }

    public AtomicPyObject newAPIObject(JepPerformer performer, String className) {
        try {
            return performer.newObject(className);
        } catch (JepException e) {
            throw new JepException("Cannot create an empty instance of Python class \"" + className
                    + "\"; maybe, the Python code does not import/declare this class. "
                    + "Note that standard classes from \"" + STANDARD_API_MODULE + "\" "
                    + "module are imported automatically", e);
        }
    }

    public String loadScalarToJep(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        return port.getData(SScalar.class, false).getValue();
    }

    public void storeScalarFromJep(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        value = closePyObject(performer, value);
        port.getData(SScalar.class, true).setTo(value);
    }

    public Object loadNumbersToJep(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SNumbers numbers = port.getData(SNumbers.class, false);
        if (isNumpyIntegration(performer.configuration())) {
            return Jep2SNumbers.toNDArray(numbers);
        } else {
            // - we can try to pass into Python, at least, 1-column simple array
            checkNumbers(numbers);
            return numbers.getArray();
        }
    }

    public void storeNumbersFromJep(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SNumbers data = port.getData(SNumbers.class, true);
        if (value instanceof SNumbers) {
            data.setTo((SNumbers) value);
        } else {
            checkJepNDArray(port, value, false, true);
            Jep2SNumbers.setToArray(data, value);
        }
    }

    public DirectNDArray<Buffer> loadMatToJep(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SMat matrix = port.getData(SMat.class, false);
        if (isNumpyIntegration(performer.configuration())) {
            return Jep2SMat.toNDArray(matrix);
        } else {
            // Note: unlike loadNumberToJep, we cannot do anything without the normal jep+numpy integration:
            // we MUST pass matrix dimensions, and the only way to do this is NDArray class
            throw new IllegalArgumentException("Cannot pass the matrix\n    " + matrix +
                    "\nto Python inputs: numpy.ndarray should be used in this case,\n" +
                    " but there is an integration problem between Python packages \"jep\" and \"numpy\".\n" +
                    GlobalPythonConfiguration.JEP_INSTALLATION_HINTS);
        }
    }

    public void storeMatFromJep(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SMat data = port.getData(SMat.class, true);
        if (isNumpyIntegration(performer.configuration())) {
            if (value instanceof SMat) {
                data.setTo((SMat) value);
            } else {
                checkJepNDArray(port, value, true, false);
                Jep2SMat.setToArray(data, value);
            }
        } else {
            throw new IllegalStateException("Cannot load a matrix from Python outputs: " +
                    "numpy.ndarray should be used in this case,\n" +
                    " but there is an integration problem between Python packages \"jep\" and \"numpy\".\n" +
                    GlobalPythonConfiguration.JEP_INSTALLATION_HINTS);
        }
    }

    public static JepPerformerContainer initialize(JepPerformerContainer performerContainer) {
        Objects.requireNonNull(performerContainer, "Null performerContainer");
        return performerContainer.setConfigurationSupplier(() -> initializeConfiguration(performerContainer));
    }

    public static JepExtendedConfiguration initializeConfiguration(JepPerformerContainer performerContainer) {
        final JepExtendedConfiguration configuration = new JepExtendedConfiguration();
        final JepInterpretation.Kind kind = performerContainer.getKind();
        configuration.addIncludePaths(JepPlatforms.pythonRootFolders().toArray(new String[0]));
        configuration.redirectStdout(System.out);
        configuration.redirectStdErr(System.err);
        // - this helps to correctly use "print" Python function:
        // Python print will normally go to stdout, but some IDE redirect the java output elsewhere.
        configuration.setStartupCode(initializingJepStartupCode(kind));
        configuration.setVerifier(standardJepVerifier(kind));
        LOG.log(System.Logger.Level.TRACE, "Configuring " + performerContainer + ": " + configuration);
        return configuration;
    }

    public static List<String> initializingJepStartupCode(JepInterpretation.Kind kind) {
        Objects.requireNonNull(kind, "Null JEP interpretation kind");
        return kind.isPure() ?
                STANDARD_STARTUP :
                STANDARD_STARTUP_SHARED;
    }

    public static VerificationStatus verifyPure(Interpreter jepInterpreter, JepConfig configuration) {
        // does nothing in the current version
        return null;
    }

    public static VerificationStatus verifyShared(Interpreter jepInterpreter, JepConfig configuration) {
        final Object array;
        try {
            try (PyCallable creator = jepInterpreter.getValue(STANDARD_API_JEP_VERIFIER_FUNCTION, PyCallable.class)) {
                array = creator.call();
            }
        } catch (JepException e) {
            throw new JepException("Cannot execute Python verification function \"" +
                    STANDARD_API_JEP_VERIFIER_FUNCTION +
                    "\"; maybe, the Python module " + STANDARD_API_JEP_VERIFIER +
                    " was not installed correctly", e);
        }
        final boolean ok = array instanceof NDArray<?> || array instanceof DirectNDArray<?>;
        if (!ok) {
            final Supplier<String> message = () ->
                    "Integration problem between Python packages \"jep\" and \"numpy\":\n" +
                            "the function that creates numpy.ndarray for integers " +
                            "does not return the correct Java type NDArray/DirectNDArray " +
                            "(it returns " +
                            (array == null ? null : "\"" + array.getClass().getCanonicalName() + "\"") +
                            ").\nThe \"jep\" package was probably not installed correctly in Python.\n" +
                            GlobalPythonConfiguration.JEP_INSTALLATION_HINTS;
            if (REQUIRE_NUMPY_INTEGRATION) {
                throw new JepException(message.get());
            } else {
                System.Logger.Level level = NUMPY_INTEGRATION_PROBLEM_LOGGED.getAndSet(true) ?
                        System.Logger.Level.DEBUG :
                        System.Logger.Level.WARNING;
                LOG.log(level, message);
            }
        }
        return new VerificationStatus(ok);
    }

    public static boolean isNumpyIntegrationVerified(JepConfig configuration) {
        return configuration instanceof JepExtendedConfiguration extendedConfiguration
                && extendedConfiguration.getVerificationStatus() instanceof VerificationStatus;
    }

    public static boolean isNumpyIntegration(JepConfig configuration) {
        return configuration instanceof JepExtendedConfiguration extendedConfiguration
                && extendedConfiguration.getVerificationStatus()
                instanceof VerificationStatus(boolean numpyIntegration)
                && numpyIntegration;
    }

    public record VerificationStatus(boolean numpyIntegration) {
    }

    private static JepExtendedConfiguration.Verifier standardJepVerifier(JepInterpretation.Kind kind) {
        return kind.isPure() ? JepAPI::verifyPure : JepAPI::verifyShared;
    }

    private static Object closePyObject(JepPerformer performer, Object value) {
        if (value instanceof final PyObject pyObject) {
            // - If a Python function returned PyObject/PyCallable, we should close it immediately - it cannot be used
            // outside the single thread (any attempt will lead to JepException "Invalid thread access").
            // In the case of scalar, we should also to convert it into String.
            try (AtomicPyObject atomicObject = performer.wrapObject(pyObject)) {
                value = atomicObject.toString();
                // - avoid further calling "toString" for pyObject (will lead to "Invalid thread access")
            } // correctly closes PyObject (inside its thread)
        }
        return value;
    }

    private static void checkJepNDArray(Port port, Object value, boolean allowDirectArray, boolean allowJavaArrays) {
        Objects.requireNonNull(value, "Null value for storing in port " + port.getName());
        if (allowJavaArrays && value.getClass().isArray()) {
            return;
        }
        if (!(value instanceof NDArray<?> || (allowDirectArray && value instanceof DirectNDArray<?>))) {
            throw new JepException("Invalid type of property \"" + port.getName()
                    + "\" in Python outputs: numpy.ndarray expected, but actual Java type is \""
                    + value.getClass().getCanonicalName() + "\"");
            // - this is possible not only as a result of the numpy+jep integration problem:
            // the user just returned something wrong from Python code
        }
    }

    private static void checkNumbers(SNumbers numbers) {
        Objects.requireNonNull(numbers, "Null numbers");
        if (!numbers.isInitialized()) {
            throw new IllegalArgumentException("Not initialized numbers");
        }
        if (numbers.getBlockLength() != 1) {
            throw new IllegalArgumentException("Cannot pass numbers with " + numbers.getBlockLength() +
                    " > 1 columns\nto Python inputs: numpy.ndarray should be used in this case,\n" +
                    " but there is an integration problem between Python packages \"jep\" and \"numpy\".\n" +
                    GlobalPythonConfiguration.JEP_INSTALLATION_HINTS);
        }
    }


}
