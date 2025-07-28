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

package net.algart.bridges.graalvm.api;

import net.algart.bridges.graalvm.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.*;
import org.graalvm.polyglot.Value;

import java.lang.System.Logger;
import java.util.*;

public class GraalAPI {
    public static final String STANDARD_API_ENVIRONMENT = "_env";
    public static final String STANDARD_API_PARAMETER_EXECUTOR = "executor";
    public static final String STANDARD_API_PARAMETER_PLATFORM = "platform";
    public static final String STANDARD_API_CREATE_OBJECT_PROPERTY_NAME = "__SYS_createEmptyObject";
    public static final String STANDARD_API_LOGGER_NAME = "LOGGER";
    public static final String STANDARD_API_SCALAR_CLASS = "SScalarClass";
    public static final String STANDARD_API_NUMBERS_CLASS = "SNumbersClass";
    public static final String STANDARD_API_MAT_CLASS = "SMatClass";
    public static final String JS_ARRAY_BLOCK_LENGTH_PROPERTY_NAME = "blockLength";

    private static final String STANDARD_API_JS_SERVICE_SOURCE_PURE = """
            function __SYS_createEmptyObjectImpl() {
                return new Object()
            }

            [__SYS_createEmptyObjectImpl]
            """;
    private static final GraalSourceContainer STANDARD_API_JS_SERVICE_SOURCE_CONTAINER_PURE = GraalSourceContainer
            .newLiteral()
            .setModuleJS(STANDARD_API_JS_SERVICE_SOURCE_PURE, "__S_service_pure");
    private static final String STANDARD_API_JS_SERVICE_SOURCE = """
            function __SYS_createEmptyObjectImpl() {
                return new Object()
            }

            [__SYS_createEmptyObjectImpl, Java.type("%s"), Java.type("%s"), Java.type("%s")]
            """.formatted(
            SScalar.class.getCanonicalName(),
            SNumbers.class.getCanonicalName(),
            SMat.class.getCanonicalName());
    private static final GraalSourceContainer STANDARD_API_JS_SERVICE_SOURCE_CONTAINER = GraalSourceContainer
            .newLiteral()
            .setModuleJS(STANDARD_API_JS_SERVICE_SOURCE, "__S_service");
    // - note: creating container is a very "light" operation, not leading to using any Graal code

    private static final Logger LOG = System.getLogger(GraalAPI.class.getName());
    private static final java.util.logging.Logger JAVA_LOG =
            java.util.logging.Logger.getLogger(GraalAPI.class.getName());

    private boolean convertInputScalarToNumber = false;
    private boolean convertInputNumbersToArray = false;
    private boolean convertInputArraysToDouble = false;
    private boolean convertOutputIntegersToBriefForm = false;

    private GraalAPI() {
    }

    public static GraalAPI getInstance() {
        return new GraalAPI();
    }

    public static GraalAPI getSmartScriptingInstance() {
        return getInstance()
                .setConvertInputScalarToNumber(true)
                .setConvertInputNumbersToArray(true)
                .setConvertInputArraysToDouble(true)
                .setConvertOutputIntegersToBriefForm(true);
    }

    public static GraalPerformerContainer getJSContainer(boolean shared) {
        return initializeJS(GraalPerformerContainer.getContainer(shared).setAutoBindingJS());
    }

    public static GraalPerformerContainer getJSContainer(boolean shared, GraalContextCustomizer customizer) {
        return initializeJS(GraalPerformerContainer.getContainer(shared, customizer).setAutoBindingJS());
    }

    public boolean isConvertInputScalarToNumber() {
        return convertInputScalarToNumber;
    }

    public GraalAPI setConvertInputScalarToNumber(boolean convertInputScalarToNumber) {
        this.convertInputScalarToNumber = convertInputScalarToNumber;
        return this;
    }

    public boolean isConvertInputNumbersToArray() {
        return convertInputNumbersToArray;
    }

    public GraalAPI setConvertInputNumbersToArray(boolean convertInputNumbersToArray) {
        this.convertInputNumbersToArray = convertInputNumbersToArray;
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return convertInputArraysToDouble;
    }

    public GraalAPI setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        this.convertInputArraysToDouble = convertInputArraysToDouble;
        return this;
    }

    public boolean isConvertOutputIntegerToBriefForm() {
        return convertOutputIntegersToBriefForm;
    }

    public GraalAPI setConvertOutputIntegersToBriefForm(boolean convertOutputIntegersToBriefForm) {
        this.convertOutputIntegersToBriefForm = convertOutputIntegersToBriefForm;
        return this;
    }

    // Unlike loadParameters, we use here usual Map (instead of Value):
    // it allows avoiding creating a special empty object with name
    // STANDARD_API_PARAMETER inside "parameters" object
    public void loadSystemParameters(Executor executor, Value parameters) {
        Objects.requireNonNull(executor, "Null executor");
        final Map<String, Object> parameter = new LinkedHashMap<>();
        // Not Map.of: executorPlatform may  be null
        parameter.put(STANDARD_API_PARAMETER_EXECUTOR, executor);
        parameter.put(STANDARD_API_PARAMETER_PLATFORM, executor.executorPlatform());
        parameters.putMember(STANDARD_API_ENVIRONMENT, Collections.unmodifiableMap(parameter));
    }

    public void loadParameters(Executor executor, Value parameters) {
        loadSystemParameters(executor, parameters);
        loadParameters(executor.parameters(), parameters);
    }

    // Note: Graal allows using Map directly inside JavaScript or other languages,
    // but we prefer to work with "native" language-dependent object Value.
    // In particular, it allows using very pure mode without any access permissions (for strings and numbers).
    public void loadParameters(Map<String, Object> parametersMap, Value parameters) {
        Objects.requireNonNull(parametersMap, "Null parametersMap");
        Objects.requireNonNull(parameters, "Null parameters");
        for (Map.Entry<String, Object> entry : parametersMap.entrySet()) {
            parameters.putMember(entry.getKey(), entry.getValue());
        }
    }

    public void readInputPorts(Collection<Port> inputPorts, Value inputs) {
        Objects.requireNonNull(inputPorts, "Null inputPorts");
        Objects.requireNonNull(inputs, "Null inputs");
        for (Port port : inputPorts) {
            inputs.putMember(port.getName(), readInputPort(port));
        }
    }

    public void writeOutputPorts(Collection<Port> outputPorts, Value outputs) {
        Objects.requireNonNull(outputPorts, "Null outputPorts");
        Objects.requireNonNull(outputs, "Null outputs");
        for (Port port : outputPorts) {
            writeOutputPort(port, outputs.getMember(port.getName()));
        }
    }

    public Object readInputPort(Port port) {
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
                case SCALAR -> loadScalar(port);
                case NUMBERS -> loadNumbers(port);
                case MAT -> loadMat(port);
            };
        }
        return value;
    }

    public void writeOutputPort(Port port, Value value) {
        writeOutputPort(port, value, false);
    }

    public void writeOutputPort(Port port, Value value, boolean preserveExisting) {
        Objects.requireNonNull(port, "Null port");
        // Note: value == null is possible, when it is gotten via getMember call for non-existing property,
        // like in writeOutputPorts method
        if (!port.isOutput()) {
            throw new IllegalArgumentException("Non-output port: " + port);
        }
        if (preserveExisting && port.hasData()) {
            return;
        }
        final DataType dataType = port.getDataType();
        if (value == null || value.isNull()) {
            port.removeData();
        } else if (dataType != null) {
            // - normally port.getDataType() == null is impossible
            switch (dataType) {
                case SCALAR -> storeScalar(port, value);
                case NUMBERS -> storeNumbers(port, value);
                case MAT -> storeMat(port, value);
            }
        }
    }

    public void loadScalar(Value bindings, ExecutionBlock executor, String portName, String defaultValue) {
        bindings.putMember(portName, loadScalar(executor, portName, defaultValue));
    }

    public Object loadScalar(ExecutionBlock executor, String portName, String defaultValue) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        return loadScalar(executor.getRequiredInputPort(portName), defaultValue);
    }

    public Object loadScalar(Port port) {
        return loadScalar(port, null);
    }

    public Object loadScalar(Port port, String defaultValue) {
        Objects.requireNonNull(port, "Null port");
        final String value = port.getData(SScalar.class, true).getValueOrDefault(defaultValue);
        if (value != null && convertInputScalarToNumber) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return value;
    }

    public void storeScalar(ExecutionBlock executor, String portName, Value value) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        storeScalar(executor.getRequiredOutputPort(portName), value);
    }

    public void storeScalar(Port port, Value value) {
        Objects.requireNonNull(port, "Null port");
        Objects.requireNonNull(value, "Null value");
        port.getData(SScalar.class, true).setTo(
                GraalValues.toSmartString(value, convertOutputIntegersToBriefForm));
    }

    public void loadNumbers(
            Value bindings,
            ExecutionBlock executor,
            String portName,
            boolean putNullBindingForUninitialized,
            String... alternativeBindingNames) {
        Objects.requireNonNull(bindings, "Null bindings");
        Objects.requireNonNull(alternativeBindingNames, "Null alternativeBindingNames");
        final Object data = loadNumbers(executor, portName);
        if (putNullBindingForUninitialized || data != null) {
            bindings.putMember(portName, data);
            for (String name : alternativeBindingNames) {
                bindings.putMember(name, data);
            }
        }
    }

    public Object loadNumbers(ExecutionBlock executor, String portName) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        return loadNumbers(executor.getRequiredInputPort(portName));
    }

    public Object loadNumbers(Port port) {
        Objects.requireNonNull(port, "Null port");
        final SNumbers data = port.getData(SNumbers.class, true);
        if (!data.isInitialized()) {
            return null;
        }
        if (!convertInputNumbersToArray) {
            return data;
        }
        return convertInputArraysToDouble ? data.toDoubleArray() : data.getArray();
    }

    public void storeNumbers(ExecutionBlock executor, String portName, Value value) {
        storeNumbers(executor, portName, value, 1);
    }

    public void storeNumbers(ExecutionBlock executor, String portName, Value value, int defaultBlockLength) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        storeNumbers(executor.getRequiredOutputPort(portName), value, defaultBlockLength);
    }

    public void storeNumbers(Port port, Value value) {
        storeNumbers(port, value, 1);
    }

    public void storeNumbers(Port port, Value value, int defaultBlockLength) {
        Objects.requireNonNull(port, "Null port");
        Objects.requireNonNull(value, "Null value");
        Object object = value.isNull() ? null : value.as(Object.class);
        if (object == null) {
            port.removeData();
        } else {
            final SNumbers data = port.getData(SNumbers.class, true);
            if (object instanceof SNumbers) {
                data.setTo((SNumbers) object);
            } else if (object instanceof Collection<?>) {
                int blockLength = 1;
                if (value.hasMember(JS_ARRAY_BLOCK_LENGTH_PROPERTY_NAME)) {
                    final Value blockLengthValue = value.getMember(JS_ARRAY_BLOCK_LENGTH_PROPERTY_NAME);
                    if (blockLengthValue.fitsInInt()) {
                        blockLength = blockLengthValue.asInt();
                    }
                }
                data.setTo((Collection<?>) object, blockLength);
            } else {
                if (!SNumbers.isJavaArraySupported(object)) {
                    throw new IllegalArgumentException(
                            "Illegal type for output \"" + port.getName() +
                                    "\": JavaScript code must return java-array of primitive types or " +
                                    SMat.class.getCanonicalName()
                                    + ", but it returned " + object.getClass().getCanonicalName());
                }
                data.setToArray(object, defaultBlockLength);
            }
        }
    }

    public void loadMat(
            Value bindings,
            ExecutionBlock executor,
            String portName, boolean putNullBindingForUninitialized,
            String... alternativeBindingNames) {
        Objects.requireNonNull(bindings, "Null bindings");
        Objects.requireNonNull(alternativeBindingNames, "Null alternativeBindingNames");
        final Object data = loadMat(executor, portName);
        if (putNullBindingForUninitialized || data != null) {
            bindings.putMember(portName, data);
            for (String name : alternativeBindingNames) {
                bindings.putMember(name, data);
            }
        }
    }

    public Object loadMat(ExecutionBlock executor, String portName) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        return loadMat(executor.getRequiredInputPort(portName));
    }

    public Object loadMat(Port port) {
        Objects.requireNonNull(port, "Null port");
        final SMat data = port.getData(SMat.class, true);
        if (!data.isInitialized()) {
            return null;
        }
        return data;
    }

    public void storeMat(ExecutionBlock executor, String portName, Value value) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(portName, "Null portName");
        storeMat(executor.getRequiredOutputPort(portName), value);
    }

    public void storeMat(Port port, Value value) {
        Objects.requireNonNull(port, "Null port");
        Objects.requireNonNull(value, "Null value");
        Object object = value.isNull() ? null : value.as(Object.class);
        if (object == null) {
            port.removeData();
        } else {
            final SMat data = port.getData(SMat.class, true);
            if (!(object instanceof SMat)) {
                throw new IllegalStateException(
                        "Illegal type for of output \"" + port.getName() +
                                "\": JavaScript code must return " + SMat.class.getCanonicalName()
                                + ", but it returned " + object.getClass().getCanonicalName());
            }
            data.setTo((SMat) object);
        }
    }

    public static Value storedCreateEmptyObjectJSFunction(GraalPerformer performer) {
        Objects.requireNonNull(performer, "Null performer");
        final Value result = performer.getProperty(STANDARD_API_CREATE_OBJECT_PROPERTY_NAME, Value.class);
        if (result == null) {
            throw new IllegalStateException("Performer is not configured properly: " + performer);
        }
        return result;
    }

    public static GraalPerformerContainer initializeJS(GraalPerformerContainer performerContainer) {
        Objects.requireNonNull(performerContainer, "Null performerContainer");
        final boolean addStandardClasses = performerContainer.getCustomizer().isJavaAccessSupported();
        performerContainer.setConfigurator(performer -> standardConfigureJS(performer, addStandardClasses));
        return performerContainer;
    }

    public static GraalPerformerContainer.Local initializeJS(GraalPerformerContainer.Local performerContainer) {
        return (GraalPerformerContainer.Local) initializeJS((GraalPerformerContainer) performerContainer);
    }

    public static GraalPerformerContainer.Shared initializeJS(GraalPerformerContainer.Shared performerContainer) {
        return (GraalPerformerContainer.Shared) initializeJS((GraalPerformerContainer) performerContainer);
    }

    public static void standardConfigureJS(GraalPerformer performer, boolean addStandardClasses) {
        Objects.requireNonNull(performer, "Null performer");
        final Value serviceValues = performer.perform(
                addStandardClasses ?
                        STANDARD_API_JS_SERVICE_SOURCE_CONTAINER :
                        STANDARD_API_JS_SERVICE_SOURCE_CONTAINER_PURE);
        final Value createEmptyObjectFunction = serviceValues.getArrayElement(0);
        performer.putPropertyIfAbsent(STANDARD_API_CREATE_OBJECT_PROPERTY_NAME, createEmptyObjectFunction);
        final Value bindings = performer.bindingsJS();
        // - note that context.getPolyglotBindings().putMember() does not help,
        // necessary getBinding for specified language
        bindings.putMember(STANDARD_API_LOGGER_NAME, JAVA_LOG);
        if (addStandardClasses) {
            bindings.putMember(STANDARD_API_SCALAR_CLASS, serviceValues.getArrayElement(1));
            bindings.putMember(STANDARD_API_NUMBERS_CLASS, serviceValues.getArrayElement(2));
            bindings.putMember(STANDARD_API_MAT_CLASS, serviceValues.getArrayElement(3));
        }
    }
}
