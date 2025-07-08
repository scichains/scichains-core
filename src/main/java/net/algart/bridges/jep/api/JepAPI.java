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
import net.algart.bridges.jep.JepPerformer;
import net.algart.bridges.jep.JepPerformerContainer;
import net.algart.bridges.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.additions.JepExtendedConfiguration;
import net.algart.bridges.jep.additions.GlobalPythonConfiguration;
import net.algart.bridges.jep.additions.JepInterpreterKind;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.*;

import java.nio.Buffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class JepAPI {
    public static final boolean REQUIRE_NUMPY_INTEGRATION = net.algart.arrays.Arrays.SystemSettings.getBooleanProperty(
             "net.algart.bridges.jep.numpyIntegrationRequired", true);

    private static final AtomicBoolean NUMPY_INTEGRATION_PROBLEM_LOGGED = new AtomicBoolean(false);
    private static volatile boolean numpyIntegration = false;

    public static final String STANDARD_API_PACKAGE = "algart_api";
    public static final String STANDARD_API_IN_OUT = STANDARD_API_PACKAGE + ".SInOut";
    public static final String STANDARD_API_PARAMETERS_CLASS_NAME = STANDARD_API_IN_OUT + ".SParameters";
    public static final String STANDARD_API_INPUTS_CLASS_NAME = STANDARD_API_IN_OUT + ".SInputs";
    public static final String STANDARD_API_OUTPUTS_CLASS_NAME = STANDARD_API_IN_OUT + ".SOutputs";
    public static final String STANDARD_API_PARAMETER = "_sys";
    public static final String STANDARD_API_PARAMETER_EXECUTOR = "executor";
    public static final String STANDARD_API_PARAMETER_PLATFORM = "platform";
    public static final String STANDARD_API_JEP_VERIFIER = STANDARD_API_PACKAGE + ".SJepVerifier";
    public static final String STANDARD_API_JEP_VERIFIER_FUNCTION = STANDARD_API_JEP_VERIFIER + ".returnTestNdArray";
    public static final List<String> STANDARD_STARTUP = List.of(
            "import " + STANDARD_API_IN_OUT + "\n\n");
    public static final List<String> STANDARD_STARTUP_SHARED = List.of(
            "import numpy\n",
            "import " + STANDARD_API_IN_OUT + "\n",
            "import " + STANDARD_API_JEP_VERIFIER + "\n\n");

    static final System.Logger LOG = System.getLogger(JepAPI.class.getName());

    // In the current version, this class has not any settings, but maybe they will be added in future
    private JepAPI() {
    }

    public static JepAPI getInstance() {
        return new JepAPI();
    }

    public static JepPerformerContainer getContainer() {
        return initialize(JepPerformerContainer.getContainer());
    }

    public static JepPerformerContainer getContainer(JepInterpreterKind kind) {
        return initialize(JepPerformerContainer.getContainer(kind));
    }

    public void loadParameters(Executor executor, AtomicPyObject parameters) {
        loadSystemParameters(executor, parameters);
        loadParameters(executor.parameters(), parameters);
    }

    public void loadSystemParameters(Executor executor, AtomicPyObject parameters) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(parameters, "Null parameters");
        try (AtomicPyObject parameter = parameters.getObject(STANDARD_API_PARAMETER)) {
            parameter.setAttribute(STANDARD_API_PARAMETER_EXECUTOR, executor);
            parameter.setAttribute(STANDARD_API_PARAMETER_PLATFORM, executor.executorPlatform());
        }
    }

    public void loadParameters(Map<String, Object> parametersMap, AtomicPyObject parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        Objects.requireNonNull(parametersMap, "Null parametersMap");
        for (Map.Entry<String, Object> entry : parametersMap.entrySet()) {
            parameters.setAttribute(entry.getKey(), entry.getValue());
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

    // performer is not used in the current version
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
                case SCALAR -> loadScalar(performer, port);
                case NUMBERS -> loadNumbers(performer, port);
                case MAT -> loadMat(performer, port);
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
                case SCALAR -> storeScalar(performer, port, value);
                case NUMBERS -> storeNumbers(performer, port, value);
                case MAT -> storeMat(performer, port, value);
            }
        }
    }

    public AtomicPyObject newAPIObject(JepPerformer performer, String className) {
        try {
            return performer.newObject(className);
        } catch (JepException e) {
            throw new JepException("Cannot create an empty instance of Python class \"" + className
                    + "\"; maybe, the Python code does not import/declare this class. "
                    + "Note that standard classes from \"" + STANDARD_API_IN_OUT + "\" "
                    + "module are imported automatically", e);
        }
    }

    public String loadScalar(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        return port.getData(SScalar.class, false).getValue();
    }

    public void storeScalar(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        value = closePyObject(performer, value);
        port.getData(SScalar.class, true).setTo(value);
    }

    public NDArray<Object> loadNumbers(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        return Jep2SNumbers.toNDArray(port.getData(SNumbers.class, false));
    }

    public void storeNumbers(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SNumbers data = port.getData(SNumbers.class, true);
        if (value instanceof SNumbers) {
            data.setTo((SNumbers) value);
        } else {
            checkNDArray(port, value, false, true);
            Jep2SNumbers.setToArray(data, value);
        }
    }

    public DirectNDArray<Buffer> loadMat(JepPerformer performer, Port port) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        return Jep2SMat.toNDArray(port.getData(SMat.class, false));
    }

    public void storeMat(JepPerformer performer, Port port, Object value) {
        Objects.requireNonNull(performer, "Null performer");
        Objects.requireNonNull(port, "Null port");
        final SMat data = port.getData(SMat.class, true);
        if (value instanceof SMat) {
            data.setTo((SMat) value);
        } else {
            checkNDArray(port, value, true, false);
            Jep2SMat.setToArray(data, value);
        }
    }

    public static JepPerformerContainer initialize(JepPerformerContainer performerContainer) {
        Objects.requireNonNull(performerContainer, "Null performerContainer");
        return performerContainer.setConfigurationSupplier(() -> initializeConfig(performerContainer));
    }

    public static JepExtendedConfiguration initializeConfig(JepPerformerContainer performerContainer) {
        JepExtendedConfiguration configuration = new JepExtendedConfiguration();
        JepInterpreterKind jepInterpreterKind = performerContainer.getKind();
        configuration.addIncludePaths(JepPlatforms.pythonRootFolders().toArray(new String[0]));
        configuration.setStartupCode(initializingJepStartupCode(jepInterpreterKind));
        configuration.setVerifier(standardJepVerifier(jepInterpreterKind));
        return configuration;
    }

    public static List<String> initializingJepStartupCode(JepInterpreterKind jepInterpreterKind) {
        Objects.requireNonNull(jepInterpreterKind, "Null jepInterpreterKind");
        return jepInterpreterKind == JepInterpreterKind.SHARED ?
                STANDARD_STARTUP_SHARED :
                STANDARD_STARTUP;
    }

    public static void verifyLocal(Interpreter jepInterpreter, JepConfig configuration) {
        // does nothing in the current version
    }

    public static void verifyShared(Interpreter jepInterpreter, JepConfig configuration) {
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
        numpyIntegration = ok;
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
                if (!NUMPY_INTEGRATION_PROBLEM_LOGGED.getAndSet(true)) {
                    LOG.log(System.Logger.Level.INFO, message);
                }
            }
        }
    }

    public static boolean isNumpyIntegration() {
        return numpyIntegration;
    }

    private static JepExtendedConfiguration.Verifier standardJepVerifier(JepInterpreterKind jepInterpreterKind) {
        return jepInterpreterKind == JepInterpreterKind.SHARED ?
                JepAPI::verifyShared :
                JepAPI::verifyLocal;
    }


    private static Object closePyObject(JepPerformer performer, Object value) {
        if (value instanceof final PyObject pyObject) {
            // - if Python function returned PyObject/PyCallable, we should close it immediately - it cannot be used
            // outside the single thread (any attempt will lead to JepException "Invalid thread access").
            // In the case of scalar, we should also to convert it into String.
            try (AtomicPyObject atomicObject = performer.wrapObject(pyObject)) {
                value = atomicObject.toString();
                // - avoid further calling "toString" for pyObject (will lead to "Invalid thread access")
            } // correctly closes PyObject (inside its thread)
        }
        return value;
    }

    private static void checkNDArray(Port port, Object value, boolean allowDirectArray, boolean allowJavaArrays) {
        Objects.requireNonNull(value, "Null value for storing in port " + port.getName());
        if (allowJavaArrays && value.getClass().isArray()) {
            return;
        }
        if (!(value instanceof NDArray<?> || (allowDirectArray && value instanceof DirectNDArray<?>))) {
            throw new JepException("Invalid type of property \"" + port.getName()
                    + "\" in Python outputs: numpy.ndarray expected, but actual Java type is \""
                    + value.getClass().getCanonicalName() + "\"");
        }
    }
}
