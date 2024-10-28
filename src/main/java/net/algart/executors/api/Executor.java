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

package net.algart.executors.api;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;
import net.algart.executors.api.model.InstalledExtensions;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.external.UsedForExternalCommunication;
import net.algart.json.Jsons;

import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public abstract class Executor extends ExecutionBlock {
    public enum SystemParameter {
        CURRENT_FOLDER("$__system.working_directory") {
            @Override
            void update(Executor executor, String value) {
                executor.setCurrentDirectory(Paths.get(value));
            }
        },
        ENABLED("$__system.enabled");

        final String parameterName;

        public String parameterName() {
            return parameterName;
        }

        SystemParameter(String parameterName) {
            this.parameterName = parameterName;
        }

        void update(Executor executor, String value) {
        }

        public static boolean isSystemParameter(String parameterName) {
            Objects.requireNonNull(parameterName, "Null parameter name");
            return parameterName.startsWith("$__");
        }
    }

    private static final Map<String, SystemParameter> SYSTEM_PARAMETERS = new HashMap<>();

    static {
        for (SystemParameter parameter : SystemParameter.values()) {
            SYSTEM_PARAMETERS.put(parameter.parameterName, parameter);
        }
        addTaskBeforeExecutingAll(Executor::startTimingOfExecutingAll);
        addTaskAfterExecutingAll(Executor::finishTimingOfExecutingAll);
    }

    public static final String ENUM_VALUE_OF_NAME_CUSTOM_METHOD = "valueOfName";
    public static final String STANDARD_VISIBLE_RESULT_PARAMETER_NAME = "visibleResult";

    public static final String OUTPUT_EXECUTOR_ID_NAME = "_sys___executor_id";
    public static final String OUTPUT_PLATFORM_ID_NAME = "_sys___platform_id";
    public static final String OUTPUT_RESOURCE_FOLDER_NAME = "_sys___resource_folder";

    public static final Logger LOG = System.getLogger(Executor.class.getName());

    protected static final boolean LOGGABLE_INFO = LOG.isLoggable(Logger.Level.INFO);
    protected static final boolean LOGGABLE_DEBUG = LOG.isLoggable(Logger.Level.DEBUG);
    protected static final boolean LOGGABLE_TRACE = LOG.isLoggable(Logger.Level.TRACE);

    private static final boolean CREATE_EXECUTION_KEY_FILE = SystemEnvironment.getBooleanProperty(
            "net.algart.executors.api.createExecutionKeyFile", false);

    private static final Map<String, Map<String, ParameterSetter>> EXECUTOR_CLASS_SETTERS = new HashMap<>();
    private static final Map<String, Map<String, ParameterValueType>> EXECUTOR_CLASS_PARAMETER_TYPES = new HashMap<>();
    static final Set<String> NON_SETTERS = new HashSet<>(Arrays.asList(
            "setDefaultInputPort",
            "setDefaultOutputPort",
            "setDefaultInputMat",
            "setDefaultOutputMat",
            "setDefaultInputNumbers",
            "setDefaultOutputNumbers",
            "setDefaultInputScalar",
            "setDefaultOutputScalar",
            "setBooleanParameter",
            "setIntParameter",
            "setLongParameter",
            "setDoubleParameter",
            "setStringParameter",
            "setVisibleResultNecessary",
            "setAllOutputsNecessary",
            "setCurrentDirectory",
            "setMultithreadingEnvironment",
            "setTimingEnabled",
            "setSessionId",
            "setOwnerId",
            "setContextId",
            "setContextName",
            "setContextPath",
            "setInterruptionRequested"
    ));

    private final Set<String> automaticUpdateDisabledParameters = new HashSet<>();
    private boolean automaticUpdateParameters = true;
    private final Map<String, ParameterSetter> parameterSetters;
    private final Map<String, ParameterValueType> parameterTypes;

    private String defaultInputPortName = DEFAULT_INPUT_PORT;
    private String defaultOutputPortName = DEFAULT_OUTPUT_PORT;

    private final ExecutionStatus status = ExecutionStatus.newNamedInstance(getClass().getName());
    private Path currentDirectory = null;

    private boolean multithreadingEnvironment = false;

    private volatile long startProcessingTimeStamp = Long.MIN_VALUE;
    private volatile long endProcessingTimeStamp = Long.MIN_VALUE;
    private final AtomicLong serviceTime = new AtomicLong(0);
    private boolean timingEnabled = true;

    private volatile boolean cancellingFurtherExecutionRequested = false;

    private boolean usingVisibleResultParameter = false;

    protected Executor() {
        final String className = getClass().getName();
        if (loggingEnabled()) {
            logDebug(() -> "Creating executor " + className);
        }
        synchronized (EXECUTOR_CLASS_SETTERS) {
            Map<String, ParameterSetter> setters = EXECUTOR_CLASS_SETTERS.get(className);
            Map<String, ParameterValueType> parameterTypes = EXECUTOR_CLASS_PARAMETER_TYPES.get(className);
            if (setters == null) {
                assert parameterTypes == null;
                setters = ParameterSetter.findSetters(this);
                parameterTypes = new TreeMap<>();
                for (Map.Entry<String, ParameterSetter> entry : setters.entrySet()) {
                    parameterTypes.put(entry.getKey(), entry.getValue().getControlValueType());
                }
                EXECUTOR_CLASS_SETTERS.put(className, setters);
                EXECUTOR_CLASS_PARAMETER_TYPES.put(className, parameterTypes);
            }
            this.parameterSetters = setters;
            this.parameterTypes = parameterTypes;
        }
    }

    @Override
    public String defaultInputPortName() {
        return defaultInputPortName;
    }

    public void defaultInputPortName(String newDefaultInputPortName) {
        Objects.requireNonNull(newDefaultInputPortName, "Null newDefaultInputPortName");
        this.defaultInputPortName = newDefaultInputPortName;
    }

    @Override
    public String defaultOutputPortName() {
        return defaultOutputPortName;
    }

    public void defaultOutputPortName(String newDefaultOutputPortName) {
        Objects.requireNonNull(newDefaultOutputPortName, "Null newDefaultOutputPortName");
        this.defaultOutputPortName = newDefaultOutputPortName;
    }

    public final void setDefaultInputPort(String newDefaultInputPortName, DataType dataType) {
        Objects.requireNonNull(newDefaultInputPortName, "Null newDefaultInputPortName");
        Objects.requireNonNull(dataType, "Null dataType");
        removeInputPort(defaultInputPortName);
        this.defaultInputPortName = newDefaultInputPortName;
        if (!addPort(Port.newInput(newDefaultInputPortName, dataType))) {
            throw new IllegalStateException("Cannot add new default input port " + newDefaultInputPortName);
        }
    }

    public final void setDefaultOutputPort(String newDefaultOutputPortName, DataType dataType) {
        Objects.requireNonNull(newDefaultOutputPortName, "Null newDefaultOutputPortName");
        Objects.requireNonNull(dataType, "Null dataType");
        removeOutputPort(defaultOutputPortName);
        this.defaultOutputPortName = newDefaultOutputPortName;
        if (!addPort(Port.newOutput(newDefaultOutputPortName, dataType))) {
            throw new IllegalStateException("Cannot add new default output port " + newDefaultOutputPortName);
        }
    }

    public final void setDefaultInputMat(String newDefaultInputPortName) {
        setDefaultInputPort(newDefaultInputPortName, DataType.MAT);
    }

    public final void setDefaultOutputMat(String newDefaultOutputPortName) {
        setDefaultOutputPort(newDefaultOutputPortName, DataType.MAT);
    }

    public final void setDefaultInputNumbers(String newDefaultInputPortName) {
        setDefaultInputPort(newDefaultInputPortName, DataType.NUMBERS);
    }

    public final void setDefaultOutputNumbers(String newDefaultOutputPortName) {
        setDefaultOutputPort(newDefaultOutputPortName, DataType.NUMBERS);
    }

    public final void setDefaultInputScalar(String newDefaultInputPortName) {
        setDefaultInputPort(newDefaultInputPortName, DataType.SCALAR);
    }

    public final void setDefaultOutputScalar(String newDefaultOutputPortName) {
        setDefaultOutputPort(newDefaultOutputPortName, DataType.SCALAR);
    }

    public final <T extends Data> Map<String, T> allOutputContainers(
            Class<? extends T> dataClass,
            boolean onlyRequested) {
        Objects.requireNonNull(dataClass, "Null data class");
        final Map<String, T> result = new LinkedHashMap<>();
        for (Port port : allOutputPorts()) {
            assert port != null : "Null output port (impossible)";
            assert port.getName() != null : "Null output port name (impossible)";
            assert port.getDataType() != null : "Null output port data type (impossible) in port " + port.getName();
//            System.out.printf("!!!%s (%s): %s%n", port.getName(), port.getDataType(), port.isConnected());
            if (port.getDataType().typeClass().isAssignableFrom(dataClass)
                    && (!onlyRequested || checkOutputNecessary(port))) {
                result.put(port.getName(), port.getData(dataClass, true));
            }
        }
        return result;
    }

    public boolean checkOutputNecessary(Port outputPort) {
        return super.checkOutputNecessary(outputPort) ||
                (outputPort != null && isVisibleResultNecessary() && isVisiblePort(outputPort));
    }

    @Override
    public ExecutionStatus status() {
        return status;
    }

    @Override
    public String statusData(int dataCode) {
        final ExecutionStatus.DataKind dataKind = ExecutionStatus.DataKind.valueOfCodeOrNull(dataCode);
        return dataKind == null ? null : dataKind.data(status);
    }

    public final ExecutorJson executorModel() throws JsonException {
        return executorModel(getSessionId(), getExecutorId());
    }

    public final ExtensionJson.Platform executorPlatform() {
        final String id = getPlatformId();
        if (id == null) {
            return null;
        }
        return InstalledExtensions.allInstalledPlatformsMap().get(id);
    }

    public final Path executorResourceFolder() {
        final ExtensionJson.Platform platform = executorPlatform();
        if (platform == null) {
            return null;
        }
        return platform.resourcesFolderOrNull();
    }

    public final void showStatus(String message) {
        this.status().setMessageString(message);
    }

    public final void showStatus(Supplier<String> message) {
        this.status().setMessage(message);
    }

    /**
     * Returns the current folder, passed to this executor via parameter
     * {@link SystemParameter#CURRENT_FOLDER}.
     * It is supposed that the external program sets some "current folder" in this parameter.
     *
     * <p>It is used for all relative paths. For example, when
     * {@link FileOperation#getFile() the file} parameter in file operatations is
     * "images/test.png" and current working folder is "c:\tmp", the actual file will be "c:\tmp\images\test.png".
     *
     * <p>If this parameter was not set via {@link #onChangeParameter(String)} or
     * {@link #setCurrentDirectory(Path)} method, this method returns <code>null</code>.
     * In this case, relative paths will be resolved in the current system directory.
     *
     * @return current working directory, that was set via parameter {@link SystemParameter#CURRENT_FOLDER}.
     */
    public final Path getCurrentDirectory() {
        return currentDirectory;
    }

    public final void setCurrentDirectory(Path currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * Returns the value of flag, stored by {@link #setMultithreadingEnvironment(boolean)} method.
     *
     * @return the value of flag, stored by {@link #setMultithreadingEnvironment(boolean)} method.
     */
    public final boolean isMultithreadingEnvironment() {
        return multithreadingEnvironment;
    }

    /**
     * Informs the executor, is it performed in multithreading environment: other executors
     * are probable performed in other threads.
     *
     * <p>You <b>must</b> call this method before actual executing, if the executor uses
     * native-code or other non-Java resources, like GPU memory: then, maybe, it will
     * make efforts to correctly synchronize or serialize external resources, that cannot be
     * synchronized by standard Java synchronization mechanism.
     * (For example, it is possible that the object, created in GPU memory and stored
     * in the external port, will be unavailable from other Java threads if the executor
     * does not call special native methods before finishing execution.)
     *
     * <p>Default implementation of {@link #postprocess()} method (called from {@link #execute()})
     * automatically performs necessary synchronization for the content of all output ports
     * by calling {@link Data#serializeMemory()} method.
     *
     * <p>By default, this flag contains <code>false</code>.
     *
     * @param multithreadingEnvironment whether this object is executed in multithreading environment.
     */
    public final void setMultithreadingEnvironment(boolean multithreadingEnvironment) {
        this.multithreadingEnvironment = multithreadingEnvironment;
    }

    public final boolean isTimingEnabled() {
        return timingEnabled;
    }

    /**
     * Enabled / disable accumulating times in {@link #execute()} method.
     * By default, it is <code>true</code>.
     * Should be cleared to <code>false</code> if {@link #execute()} method calls the same method
     * of other objects (to avoid double counting the elapsed time).
     *
     * @param timingEnabled whether {@link #execute()} method counts its time in the global accumulator.
     */
    public final void setTimingEnabled(boolean timingEnabled) {
        this.timingEnabled = timingEnabled;
    }

    public final boolean isUsingVisibleResultParameter() {
        return usingVisibleResultParameter;
    }

    public final void useVisibleResultParameter() {
        this.usingVisibleResultParameter = true;
        disableOnChangeParameterAutomatic(STANDARD_VISIBLE_RESULT_PARAMETER_NAME);
    }

    public final boolean isCancellingExecutionRequested() {
        for (Port port : allInputPorts()) {
            if (port.isCancellingExecutionRequested()) {
                return true;
            }
        }
        return false;
    }

    public final boolean isCancellingFurtherExecutionRequested() {
        return cancellingFurtherExecutionRequested;
    }

    public final void requestCancellingFurtherExecution() {
        cancellingFurtherExecutionRequested = true;
        for (Port port : allOutputPorts()) {
            if (port.isConnected()) {
                // - no sense to set a cancellation flag in output ports, that are not connected to anything
                port.requestCancellingExecution();
            }
        }
    }

    public final void requestContinuingFurtherExecution() {
        cancellingFurtherExecutionRequested = false;
        for (Port port : allOutputPorts()) {
            port.requestContinuingExecution();
        }
    }

    public final void fillSystemOutputs() {
        setOutputScalar(OUTPUT_EXECUTOR_ID_NAME, this::getExecutorId);
        final boolean platformNecessary = isOutputNecessary(OUTPUT_PLATFORM_ID_NAME)
                || isOutputNecessary(OUTPUT_RESOURCE_FOLDER_NAME);
        // - not always: parsing executorSpecification may require some time
        if (platformNecessary) {
            setOutputScalar(OUTPUT_PLATFORM_ID_NAME, getPlatformId());
            setOutputScalar(OUTPUT_RESOURCE_FOLDER_NAME, this::executorResourceFolder);
        }
    }


    /**
     * Returns names of all parameters, for which this class provides standard setters (setXxx method).
     *
     * @return all parameters' names (unmodifiable).
     */
    public final Collection<String> allParameters() {
        return Collections.unmodifiableSet(parameterTypes.keySet());
    }

    public final ParameterValueType parameterControlValueType(String parameterName) {
        return parameterTypes.get(parameterName);
    }

    public final Class<?> parameterJavaType(String parameterName) {
        final ParameterSetter setter = parameterSetters.get(parameterName);
        return setter == null ? null : setter.parameterType;
    }

    @Override
    public void reset() {
        // Note: this method should be executed EVEN we requested cancelling execution
        // (it usually performs necessary initialization)
        initialize();
    }

    @Override
    public void execute() {
        execute(false);
    }

    public void execute(boolean silentMode) {
//        System.out.println("Executing...");
        long t1 = System.nanoTime(), t1Processing, t2Processing;
        resetTiming();
        if (isCancellingExecutionRequested()) {
            requestCancellingFurtherExecution();
            if (!silentMode && loggingEnabled() && LOGGABLE_DEBUG) {
                logDebug(() -> (getExecutorId() == null ? "  " : "  [" + getExecutorId() + "] ")
                        + getClass().getName() + " - execution SKIPPED");
            }
            return;
        }
        requestContinuingFurtherExecution();
        // - clear state of all cancellation flags
        fillSystemOutputs();
        final Path keyFile = Paths.get("___execution_" + getClass().getName());
        try {
            if (CREATE_EXECUTION_KEY_FILE) {
                try {
                    Files.createFile(keyFile);
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.WARNING, "Cannot create key file " + keyFile, e);
                }
            }
            final ExecutionBlock caller = getCaller();
            status.open(caller instanceof Executor ? ((Executor) caller).status : null);
            // - instanceof includes "!= null"
            status.setStartProcessingTimeStamp(t1);
            status.setExecutorClass(this.getClass());
            status.setClassInformationIncluded(true);
            t1Processing = System.nanoTime();
            process();
            postprocess();
            t2Processing = System.nanoTime();
        } catch (RuntimeException | Error e) {
            if (!silentMode) {
                LOG.log(System.Logger.Level.ERROR, "Cannot execute " +
                        getClass().getSimpleName() + " due to: " + e);
            }
            throw e;
        } finally {
            status.close();
            // - instanceof includes "!= null"
            if (CREATE_EXECUTION_KEY_FILE) {
                try {
                    Files.delete(keyFile);
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.WARNING, "Cannot delete key file " + keyFile, e);
                }
            }
        }
        final boolean noInputOuput =
                startProcessingTimeStamp == Long.MIN_VALUE && endProcessingTimeStamp == Long.MIN_VALUE;
        long startTime = startProcessingTimeStamp == Long.MIN_VALUE ? t1Processing : startProcessingTimeStamp;
        long endTime = endProcessingTimeStamp == Long.MIN_VALUE ? t2Processing : endProcessingTimeStamp;
        final long inputTime;
        final long outputTime;
        final long processingTime;
        if (noInputOuput) {
            inputTime = 0;
            outputTime = 0;
            processingTime = t2Processing - t1Processing;
        } else {
            inputTime = startTime - t1Processing;
            outputTime = t2Processing - endTime;
            processingTime = endTime - startTime - serviceTime.get();
        }
        if (!silentMode && loggingEnabled() && LOGGABLE_DEBUG) {
            String timing = (getExecutorId() == null ? "  " : "  [" + getExecutorId() + "] ")
                    + String.format("@%08X ", System.identityHashCode(this))
                    + getClass().getName() + " executed in "
                    + (noInputOuput ?
                    String.format(Locale.US,
                            "%.3f ms",
                            (t2Processing - t1Processing) * 1e-6) :
                    String.format(Locale.US,
                            "%.3f ms: %.3f ms in + %s%.3f ms executing + %.3f ms out",
                            (t2Processing - t1Processing) * 1e-6,
                            inputTime * 1e-6,
                            (serviceTime.get() == 0 ?
                                    "" :
                                    String.format(Locale.US, "%.3f ms service + ",
                                            serviceTime.get() * 1e-6)),
                            processingTime * 1e-6,
                            outputTime * 1e-6));
            logDebug(timing);
        }
        long t2 = System.nanoTime();
        if (isTimingEnabled()) {
            long systemTime = (t1Processing - t1) + (t2 - t2Processing);
            Timing.INSTANCE.accumulate(
                    t2 - t1, processingTime, inputTime, outputTime, serviceTime.get(), systemTime);
        }
    }

/*  // Deprecated logic of selecting port to preview and auto-contrasting
    public Data createVisibleResult() {
        if (isVisibleResultDisabled()) {
            return null;
        }
//        System.out.println("Creating visible results...");
        final String portName = visibleOutputPortName();
        if (portName != null) {
            final Port port = getOutputPort(portName);
            if (port != null) {
                Data data = port.getData();
                if (data.isInitialized()) {
                    if (loggingEnabled()) {
                        LOG.log(System.Logger.Level.TRACE, () -> "Showing " + portName + "...");
                    }
                    if (data instanceof SMat && isAutoContrastVisibleResult()) {
                        data = ((SMat) data).autoContrast();
                    }
                    return data;
                } else {
                    if (loggingEnabled()) {
                        LOG.log(System.Logger.Level.TRACE, () -> "Visible output in " + portName + " is not
                        initialized");
                    }
                    return null;
                }
            }
        }
        return null;
    }
*/

    @UsedForExternalCommunication
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return defaultVisibleResultsInformation(Port.Type.OUTPUT, visibleOutputPortName());
    }

    public final ExecutionVisibleResultsInformation defaultVisibleResultsInformation(
            Port.Type portType,
            String portName) {
        Objects.requireNonNull(portType, "Null port type");
        final ExecutionVisibleResultsInformation result = new ExecutionVisibleResultsInformation();
        if (isVisibleResultDisabled()) {
            return result;
        }
        if (portName != null) {
            final Port port = getPort(portType, portName);
            if (port != null) {
                result.setPorts(port);
            }
        }
        return result;
    }

    public boolean isVisibleResultDisabled() {
        return isCancellingFurtherExecutionRequested();
    }

    public boolean isVisiblePort(Port port) {
        Objects.requireNonNull(port);
        return port.isOutput() && port.getName().equals(visibleOutputPortName());
    }

    /**
     * Called from {@link #reset()}.
     * We recommend overriding this method instead of {@link #reset()}.
     */
    public void initialize() {
    }

    /**
     * Called from {@link #execute()}.
     * We recommend overriding this method instead of {@link #execute()}.
     */
    public abstract void process();

    public void postprocess() {
        if (multithreadingEnvironment) {
            for (Port outputPort : allOutputPorts()) {
                outputPort.getData().serializeMemory();
            }
        }
    }

    public String visibleOutputPortName() {
        final String defaultResult = defaultOutputPortName();
        if (usingVisibleResultParameter) {
            return parameters().getString(STANDARD_VISIBLE_RESULT_PARAMETER_NAME, defaultResult);
        } else {
            return defaultResult;
        }
    }

    @Override
    public void onChangeParameter(String name) {
        if (SystemParameter.isSystemParameter(name)) {
            // - also checks null
            final SystemParameter systemParameter = SYSTEM_PARAMETERS.get(name);
            if (systemParameter != null) {
                final String value = parameters().getString(name);
                systemParameter.update(this, value);
            } else {
                LOG.log(System.Logger.Level.WARNING, () ->
                        getClass() + ": attempt to set unknown system parameter \"" + name + "\" ("
                                + (getContextName() == null ? "no context" : "context \"" + getContextName() + "\"")
                                + (getContextPath() == null ? "" : " at " + getContextPath())
                                + ")");
            }
        } else if (automaticUpdateParameters && !automaticUpdateDisabledParameters.contains(name)) {
            onChangeParameterAutomatic(name);
        }
    }

    public final void disableOnChangeParametersAutomatic() {
        this.automaticUpdateParameters = false;
    }

    public final void disableOnChangeParameterAutomatic(String parameterName) {
        this.automaticUpdateDisabledParameters.add(parameterName);
    }

    public final Path translateCurrentDirectory(Path path) {
        Objects.requireNonNull(path, "Null path");
        return !path.isAbsolute() && currentDirectory != null ?
                currentDirectory.resolve(path).toAbsolutePath() :
                path;
    }

    public void freeAllInputPortData() {
        for (Port port : allInputPorts()) {
            port.removeData();
        }
    }

    public void freeAllOutputPortData() {
        for (Port port : allOutputPorts()) {
            port.removeData();
        }
    }

    public void freeAllPortData() {
        freeAllInputPortData();
        freeAllOutputPortData();
    }

    @Override
    public void close() {
        super.close();
        if (loggingEnabled()) {
            logDebug(() -> "Destroying executor " + getClass().getName());
        }
        freeAllPortData();
    }

    @Override
    public String toString() {
        return "Executor " + getClass().getName();
    }

    public final long getStartProcessingTimeStamp() {
        return startProcessingTimeStamp;
    }

    public final long getEndProcessingTimeStamp() {
        return endProcessingTimeStamp;
    }

    public final long getServiceTime() {
        return serviceTime.get();
    }

    /**
     * <b>Last</b> call of this method marks the beginning of actual execution.
     * All other calls are ignored.
     */
    public final void setStartProcessingTimeStamp() {
        startProcessingTimeStamp = System.nanoTime();
    }

    /**
     * <b>First</b> call of this method marks the end of actual execution.
     * All other calls are ignored.
     */
    public final void setEndProcessingTimeStamp() {
        if (endProcessingTimeStamp == Long.MIN_VALUE) {
            // only the last setStartProcessingTimeStamp() and first setEndProcessingTimeStamp() will be considered
            endProcessingTimeStamp = System.nanoTime();
        }
    }

    public final void addServiceTime(long elapsedServiceTimeInNanoseconds) {
        serviceTime.addAndGet(elapsedServiceTimeInNanoseconds);
    }

    public final void resetTiming() {
        this.startProcessingTimeStamp = Long.MIN_VALUE;
        this.endProcessingTimeStamp = Long.MIN_VALUE;
        this.serviceTime.set(0);
    }

    /**
     * You may override this method to disable built-in logging. (By default, it returns <code>true</code>.)
     * This method is called also <i>in the constructor</i>, so, it probably will not "see" some
     * object fields, if you use them.
     *
     * @return whether some not-important operations should be logged (object creation, closing, execution time, etc.)
     */
    protected boolean loggingEnabled() {
        return true;
    }


    /**
     * If this method returns <code>true</code>, {@link #Executor() constructor of this class} will not register
     * standard parameters, processed by {@link Executor} itself.
     * It is provided for possible future needs; the current version has no standard parameters.
     *
     * <p>Note: for correct work, this method must return <b>constant</b> (the same value for
     * all instances of the inheritor).
     */
    protected boolean skipStandardAutomaticParameters() {
        return false;
    }

    protected final void onChangeParameterAutomatic(String name) {
        long t1 = LOGGABLE_TRACE ? System.nanoTime() : 0;
        final ParameterSetter setter = parameterSetters.get(name);
        if (setter != null) {
            setter.set(this);
        } else {
            // don't check loggingEnabled(): it is a probable inconsistency in the programming code
            LOG.log(System.Logger.Level.WARNING, () ->
                    getClass() + " has no setter for parameter \"" + name + "\" ("
                            + (getContextName() == null ? "no context" : "context \"" + getContextName() + "\"")
                            + (getContextPath() == null ? "" : " at " + getContextPath())
                            + ")");
        }
        long t2 = LOGGABLE_TRACE ? System.nanoTime() : 0;
        if (loggingEnabled()) {
            LOG.log(System.Logger.Level.TRACE, () -> Executor.this.getClass()
                    + " set parameter \"" + name + "\": " + (t2 - t1) * 1e-3 + " mcs");
        }
    }

    /**
     * Returns <code>System.nanoTime()</code> if <code>{@link #LOG}.isLoggable(Level.INFO)</code>,
     * in another case returns 0.
     * Please use this method for time measuring, that will be used only for logging with level {@code <=Level.INFO}.
     *
     * @return <code>System.nanoTime()</code>, if logging level is {@code Level.INFO} or lower.
     */
    public static long infoTime() {
        return LOGGABLE_INFO ? System.nanoTime() : 0;
    }

    /**
     * Returns <code>System.nanoTime()</code> if <code>{@link #LOG}.isLoggable(Level.DEBUG)</code>,
     * in another case returns 0.
     * Please use this method for time measuring, that will be used only for logging with level {@code <=Level.DEBUG}.
     *
     * @return <code>System.nanoTime()</code>, if logging level is {@code Level.DEBUG} or lower.
     */
    public static long debugTime() {
        return LOGGABLE_DEBUG ? System.nanoTime() : 0;
    }

    public static long allocatedMemory() {
        final Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static long configAllocatedMemory() {
        return LOGGABLE_DEBUG ? allocatedMemory() : 0;
    }

    public static long fineAllocatedMemory() {
        return LOGGABLE_TRACE ? allocatedMemory() : 0;
    }

    public static void startTimingOfExecutingAll() {
        LOG.log(System.Logger.Level.INFO, () -> String.format("Starting executing in Java%n%s",
                Timing.INSTANCE.startingInfo()));
        Timing.INSTANCE.startTiming();
    }

    public static void finishTimingOfExecutingAll() {
        Timing.INSTANCE.finishTiming();
        LOG.log(System.Logger.Level.INFO, () -> String.format(
                "Finishing executing in Java%n%s", Timing.INSTANCE.finishingInfo()));
        Timing.INSTANCE.startTiming();
        // - to be on the safe side (if startExecutingAllTiming was not called)
    }

    public static ExecutorJson executorModel(String sessionId, String executorId) throws JsonException {
        final String description = ExecutionBlock.getExecutorModelDescription(sessionId, executorId);
        if (description == null) {
            return null;
        }
        final JsonObject json;
        try {
            json = Jsons.toJson(description);
            return ExecutorJson.valueOf(json);
        } catch (JsonException e) {
            throw new JsonException("Executor with ID \"" + executorId
                    + "\" has is specified by description, which is not a correct JSON");
        }
    }

    protected static void logInfo(Supplier<String> msgSupplier) {
        LOG.log(Logger.Level.INFO, msgSupplier);
    }

    protected static void logInfo(String msg) {
        LOG.log(Logger.Level.INFO, msg);
    }

    protected static void logDebug(Supplier<String> msgSupplier) {
        LOG.log(Logger.Level.DEBUG, msgSupplier);
    }

    protected static void logDebug(String msg) {
        LOG.log(Logger.Level.DEBUG, msg);
    }

    protected static void logTrace(Supplier<String> msgSupplier) {
        LOG.log(Logger.Level.TRACE, msgSupplier);
    }

    protected static void logTrace(String msg) {
        LOG.log(Logger.Level.TRACE, msg);
    }

    /**
     * Analog of {@link Logger.Level#valueOf(String)}, extended for compatibility with
     * <code>java.util.logging.Level</code>
     * string constants. In addition to {@link Logger.Level} names, recognizes the following names:
     * <ul>
     * <li><code>"SEVERE"</code> (recognized as {@link Logger.Level#ERROR}),</li>
     * <li><code>"CONFIG"</code>, <code>"FINE"</code> (recognized as {@link Logger.Level#DEBUG}),</li>
     * <li><code>"FINER"</code>, <code>"FINEST"</code> (recognized as {@link Logger.Level#TRACE}),</li>
     * <li>numeric values <code>"1000"</code>, <code>"900"</code>, <code>"800"</code>, <code>"700"</code>,
     * <code>"500"</code>, <code>"400"</code>, <code>"300"</code>
     * (see severities of <code>java.util.logging.Level</code>).</li>
     * </ul>
     *
     * @param name name of logging level.
     * @return corresponding level
     * @throws IllegalArgumentException – if the specified name does not match to {@link Logger.Level} enum type
     *                                  or constant listed above.
     */
    protected static Logger.Level valueOfLogLevel(String name) {
        return switch (name) {
            case "SEVERE", "1000" -> Logger.Level.ERROR;
            case "900" -> Logger.Level.WARNING;
            case "800" -> Logger.Level.INFO;
            case "CONFIG", "700", "FINE", "500" -> Logger.Level.DEBUG;
            case "FINER", "400", "FINEST", "300" -> Logger.Level.TRACE;
            default -> Logger.Level.valueOf(name);
        };
    }

    public static final class Timing {
        private static final Timing INSTANCE = new Timing();

        private volatile boolean active = false;
        private long startTimeStamp = Long.MIN_VALUE;
        private long finishTimeStamp = Long.MIN_VALUE;
        private long totalTime = 0;
        private long processingTime = 0;
        private long inputTime = 0;
        private long outputTime = 0;
        private long serviceTime = 0;
        private long systemTime = 0;

        private final Object lock = new Object();

        private Timing() {
        }

        public static Timing getInstance() {
            return INSTANCE;
        }

        // Must be used only if we can guarantee the following "finish()", for example, in try / finally
        public void start() {
            synchronized (lock) {
                startTiming();
                active = true;
            }
        }

        public void finish() {
            synchronized (lock) {
                active = false;
                finishTiming();
            }
        }

        public boolean isActive() {
            return active;
        }

        public String startingInfo() {
            synchronized (lock) {
                return "Start " + commonInfo();
            }
        }

        public String finishingInfo() {
            synchronized (lock) {
                final double elapsed = (double) finishTimeStamp - (double) startTimeStamp;
                return "Finish " + commonInfo() + String.format(Locale.US,
                        "Timing:%n"
                                + "  elapsed time %.3f ms%n"
                                + "    total execute() time %.3f ms%n"
                                + "      processing           %.3f ms%n"
                                + "      input                %.3f ms%n"
                                + "      output               %.3f ms%n"
                                + "      service              %.3f ms%n"
                                + "      timing/logging       %.3f ms%n"
                                + "    external operations    %.3f ms%n",
                        elapsed * 1e-6,
                        totalTime * 1e-6,
                        processingTime * 1e-6,
                        inputTime * 1e-6,
                        outputTime * 1e-6,
                        serviceTime * 1e-6,
                        systemTime * 1e-6,
                        (elapsed - (double) totalTime) * 1e-6);
            }
        }

        public static String memoryInfo() {
            final Runtime rt = Runtime.getRuntime();
            return String.format(Locale.US, "Used memory: %.5f MB / %.5f MB%n",
                    (rt.totalMemory() - rt.freeMemory()) / 1048576.0,
                    rt.maxMemory() / 1048576.0);
        }

        void startTiming() {
            synchronized (lock) {
                if (active) {
                    return;
                }
                totalTime = 0;
                processingTime = 0;
                inputTime = 0;
                outputTime = 0;
                serviceTime = 0;
                systemTime = 0;
                startTimeStamp = System.nanoTime();
            }
        }

        void finishTiming() {
            synchronized (lock) {
                finishTimeStamp = System.nanoTime();
                // - Unlike clearTiming(), we should not skip this if "active" is set:
                // the following call of "finish()" will rewrite this value.
                // (If we do not set this value, the logging in executor() will show invalid elapsed time.)
            }
        }

        void accumulate(
                long fullTime,
                long processingTime,
                long inputTime,
                long outputTime,
                long serviceTime,
                long systemTime) {
            synchronized (lock) {
                this.totalTime += fullTime;
                this.processingTime += processingTime;
                this.inputTime += inputTime;
                this.outputTime += outputTime;
                this.serviceTime += serviceTime;
                this.systemTime += systemTime;
            }
        }

        private static String commonInfo() {
            return String.format(Locale.US, "time: %s%n", new Date()) + memoryInfo();
        }
    }
}
