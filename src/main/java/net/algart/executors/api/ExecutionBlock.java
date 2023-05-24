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

package net.algart.executors.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import net.algart.executors.api.data.*;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExecutorJsonSet;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.modules.core.logic.compiler.js.UseJS;
import net.algart.executors.modules.core.logic.compiler.python.UsingPython;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.subchains.UseMultiChain;
import net.algart.executors.modules.core.logic.compiler.subchains.UseSubChain;
import net.algart.external.UsedByNativeCode;
import net.algart.json.PropertyChecker;

import java.io.StringReader;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class ExecutionBlock extends PropertyChecker implements AutoCloseable {
    public static String DEFAULT_INPUT_PORT = "input";
    public static String DEFAULT_OUTPUT_PORT = "output";

    /**
     * Executors, created with this session ID, will be available in all sessions.
     */
    public static final String GLOBAL_SHARED_SESSION_ID = "$~~GLOBAL-SESSION~~_699d349b-3312-4d5d-8fc4-0444dd2b387f";

    public static final boolean SHOW_INFO_ON_STARTUP = SystemEnvironment.getBooleanProperty(
            "net.algart.executors.api.showInfo", false);

    static {
        if (SHOW_INFO_ON_STARTUP) {
            System.out.printf("%nJava executors system started%n");
            System.out.printf("Java version: %s%n", SystemEnvironment.getStringProperty("java.version"));
            String arch = SystemEnvironment.getStringProperty("os.arch");
            boolean java32 = arch != null && !arch.contains("64") && arch.toLowerCase().contains("x86");
            System.out.printf("Architecture: %s (%d-bit)%n", arch, java32 ? 32 : 64);
            if (SystemEnvironment.getBooleanProperty("net.algart.executors.api.showLibraryPath", false)) {
                String javaLibraryPath = SystemEnvironment.getStringProperty("java.library.path");
                if (javaLibraryPath != null) {
                    javaLibraryPath = javaLibraryPath.replace(";", String.format(";%n    "));
                    System.out.printf("Native library path:%n    %s%n", javaLibraryPath);
                }
            }
            Runtime rt = Runtime.getRuntime();
            System.out.printf("Available processors: %d%n", rt.availableProcessors());
            System.out.printf("Number of processors, used by AlgART: %d%n",
                    net.algart.arrays.Arrays.SystemSettings.cpuCount());
            System.out.printf("Maximal available memory: %.2f Mb%n", rt.maxMemory() / 1048576.0);
            // overrideLogLevel(); // - usually very bad idea
        }
    }

    private static final List<ExecutionBlockLoader> executionBlockLoaders = new ArrayList<>();

    private static final ExecutionBlockLoader.StandardJavaExecutorLoader standardJavaExecutorLoader =
            new ExecutionBlockLoader.StandardJavaExecutorLoader();

    static {
        registerExecutionBlockLoader(standardJavaExecutorLoader);
    }

    private static final Map<Integer, Runnable> tasksBeforeExecutingAll =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Runnable> tasksAfterExecutingAll =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Integer, Runnable> oneTimeTasksAfterExecutingAll =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * input ports
     */
    private final Map<String, Port> inputPorts = new LinkedHashMap<>();

    /**
     * output ports
     */
    private final Map<String, Port> outputPorts = new LinkedHashMap<>();

    /**
     * Parameters, updated automatically by host application.
     */
    private final Parameters parameters = Parameters.newInstance();

    private boolean visibleResultNecessary = false;
    private boolean allOutputsNecessary = false;
    private ExecutionBlock caller = null;
    private ExecutionBlock rootCaller = this;
    private String sessionId = null;
    private String executorId = null;
    private String executorSpecification = null;
    private JsonObject executorSpecificationJson = null;
    private String ownerId = null;
    private Object contextId = null;
    private String contextName = null;
    private String contextPath = null;
    private volatile boolean interruptionRequested = false;
    private volatile boolean closed = false;

    protected ExecutionBlock() {
    }


    /**
     * Add in/out port only if absent
     *
     * @param port added port
     * @return success operation flag
     */
    @UsedByNativeCode
    public final boolean addPort(Port port) {
        Objects.requireNonNull(port, "Null port");
        return switch (port.getPortType()) {
            case INPUT -> inputPorts.putIfAbsent(port.getName(), port) == null;
            case OUTPUT -> outputPorts.putIfAbsent(port.getName(), port) == null;
            default -> throw new AssertionError("Unsupported port type");
        };
    }

    public final boolean replacePort(Port port) {
        Objects.requireNonNull(port, "Null port");
        return switch (port.getPortType()) {
            case INPUT -> inputPorts.put(port.getName(), port) == null;
            case OUTPUT -> outputPorts.put(port.getName(), port) == null;
            default -> throw new AssertionError("Unsupported port type");
        };
    }

    public final boolean removeInputPort(String portName) {
        Objects.requireNonNull(portName, "Null portName");
        return inputPorts.remove(portName) != null;
    }

    public final boolean removeOutputPort(String portName) {
        Objects.requireNonNull(portName, "Null portName");
        return outputPorts.remove(portName) != null;
    }

    // May be overridden.
    public String defaultInputPortName() {
        return DEFAULT_INPUT_PORT;
    }

    // May be overridden
    public String defaultOutputPortName() {
        return DEFAULT_OUTPUT_PORT;
    }

    @UsedByNativeCode
    public final boolean hasInputPort(String name) {
        return inputPorts.containsKey(name);
    }

    @UsedByNativeCode
    public final boolean hasOutputPort(String name) {
        return outputPorts.containsKey(name);
    }

    public final boolean hasDefaultInputPort() {
        return hasInputPort(defaultInputPortName());
    }

    public final boolean hasDefaultOutputPort() {
        return hasOutputPort(defaultOutputPortName());
    }

    public final Collection<Port> allInputPorts() {
        return Collections.unmodifiableCollection(inputPorts.values());
    }

    public final Collection<Port> allOutputPorts() {
        return Collections.unmodifiableCollection(outputPorts.values());
    }

    @UsedByNativeCode
    public final Port getInputPort(String name) {
        Objects.requireNonNull(name, "Null input port name");
        return inputPorts.get(name);
    }

    @UsedByNativeCode
    public final Port getOutputPort(String name) {
        Objects.requireNonNull(name, "Null output port name");
        return outputPorts.get(name);
    }

    public final Port getPort(Port.Type type, String name) {
        Objects.requireNonNull(type, "Null port type");
        Objects.requireNonNull(name, "Null port name");
        return type.getPort(this, name);
    }

    public final Port getRequiredInputPort(String name) {
        final Port result = getInputPort(name);
        if (result == null) {
            throw new IllegalArgumentException("No input port \"" + name + "\"");
        }
        return result;
    }

    public final Port getRequiredOutputPort(String name) {
        final Port result = getOutputPort(name);
        if (result == null) {
            throw new IllegalArgumentException("No output port \"" + name + "\"");
        }
        return result;
    }

    /**
     * Returns <tt>true</tt> if and only {@link #getInputPort(String) getInputPort(inputPortName)} returns
     * non-null value and {@link #checkInputNecessary(Port)} returns <tt>null</tt> or <tt>true</tt> for it.
     *
     * @param inputPortName the name of input port.
     * @return whether this port exists and its content is necessary for calculations.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public final boolean isInputNecessary(String inputPortName) {
        final Port inputPort = getInputPort(inputPortName);
        if (inputPort == null) {
            return false;
        }
        final Boolean check = checkInputNecessary(inputPort);
        return check == null || check;
    }

    public final boolean isOutputNecessary(String outputPortName) {
        return checkOutputNecessary(getOutputPort(outputPortName));
    }

    /**
     * Usually returns <tt>null</tt>; may return <tt>true</tt>/<tt>false</tt> if the content of this input port
     * is really necessary / not necessary for processing data.
     * May be overridden for better performance of execution system, if some existing ports are actually
     * not used.
     *
     * <p>Note: if this method returns non-null value for some input ports, its result may depend
     * on executor's parameters (available via {@link #parameters()} method) and, maybe,
     * on the content of other ports, for which this method returns <tt>null</tt>.
     * The result of this method <b>must not</b> depend on the content of the ports,
     * for which this method returns non-null value!
     *
     * <p>Default implementation returns <tt>null</tt> always.
     *
     * <p>If the argument is <tt>null</tt>, the result may be any (<tt>null</tt>, <tt>true</tt> or <tt>false</tt>):
     * it is ignored. An exception is not thrown in this case.
     *
     * @param inputPort the checked input port.
     * @return whether this input port is really necessary for calculations.
     */
    public Boolean checkInputNecessary(Port inputPort) {
        return null;
    }

    public boolean checkOutputNecessary(Port outputPort) {
        return outputPort != null && (allOutputsNecessary || outputPort.isConnected());
    }

    public final boolean addInputData(String name, DataType dataType) {
        return !inputPorts.containsKey(name) && addPort(Port.newInput(name, dataType));
    }

    public final boolean addOutputData(String name, DataType dataType) {
        return !outputPorts.containsKey(name) && addPort(Port.newOutput(name, dataType));
    }

    public final boolean addInputMat(String name) {
        return !inputPorts.containsKey(name) && addPort(Port.newInput(name, DataType.MAT));
    }

    public final boolean addOutputMat(String name) {
        return !outputPorts.containsKey(name) && addPort(Port.newOutput(name, DataType.MAT));
    }

    public final boolean addInputNumbers(String name) {
        return !inputPorts.containsKey(name) && addPort(Port.newInput(name, DataType.NUMBERS));
    }

    public final boolean addOutputNumbers(String name) {
        return !outputPorts.containsKey(name) && addPort(Port.newOutput(name, DataType.NUMBERS));
    }

    public final boolean addInputScalar(String name) {
        return !inputPorts.containsKey(name) && addPort(Port.newInput(name, DataType.SCALAR));
    }

    public final boolean addOutputScalar(String name) {
        return !outputPorts.containsKey(name) && addPort(Port.newOutput(name, DataType.SCALAR));
    }

    public final Data getInputData(String name) {
        return getInputData(name, false);
    }

    public final Data getInputDataContainer(String name) {
        return getInputData(name, true);
    }

    public final Data getInputData(String name, boolean allowUninitializedData) {
        return getRequiredInputPort(name).getData(Data.class, allowUninitializedData);
    }

    public final Data getData(String name) {
        return getRequiredOutputPort(name).getData(Data.class, true);
    }

    public final Data getInputData() {
        return getInputData(defaultInputPortName());
    }

    public final Data getInputData(boolean allowUninitializedData) {
        return getInputData(defaultInputPortName(), allowUninitializedData);
    }

    public final Data getInputDataContainer() {
        return getInputDataContainer(defaultInputPortName());
    }

    public final Data getData() {
        return getData(defaultOutputPortName());
    }

    public final SMat getInputMat(String name) {
        return getInputMat(name, false);
    }

    public final SMat getInputMatContainer(String name) {
        return getInputMat(name, true);
    }

    public final SMat getInputMat(String name, boolean allowUninitializedData) {
        return getRequiredInputPort(name).getData(SMat.class, allowUninitializedData);
    }

    public final SMat getMat(String name) {
        return getRequiredOutputPort(name).getData(SMat.class, true);
    }

    public final void putMat(String name, SMat mat) {
        addInputMat(name);
        getInputMatContainer(name).setTo(mat);
    }

    public final SMat getInputMat() {
        return getInputMat(defaultInputPortName());
    }

    public final SMat getInputMat(boolean allowUninitializedData) {
        return getInputMat(defaultInputPortName(), allowUninitializedData);
    }

    public final SMat getInputMatContainer() {
        return getInputMatContainer(defaultInputPortName());
    }

    public final SMat getMat() {
        return getMat(defaultOutputPortName());
    }

    public final void putMat(SMat mat) {
        putMat(defaultInputPortName(), mat);
    }

    public final SNumbers getInputNumbers(String name) {
        return getInputNumbers(name, false);
    }

    public final SNumbers getInputNumbersContainer(String name) {
        return getInputNumbers(name, true);
    }

    public final SNumbers getInputNumbers(String name, boolean allowUninitializedData) {
        return getRequiredInputPort(name).getData(SNumbers.class, allowUninitializedData);
    }

    public final SNumbers getNumbers(String name) {
        return getRequiredOutputPort(name).getData(SNumbers.class, true);
    }

    public final void putNumbers(String name, SNumbers numbers) {
        addInputNumbers(name);
        getInputNumbersContainer(name).setTo(numbers);
    }

    public final void putNumbers(String name, Object javaArray, int blockLength) {
        addInputNumbers(name);
        getInputNumbersContainer(name).setToArray(javaArray, blockLength);
    }

    public final SNumbers getInputNumbers() {
        return getInputNumbers(defaultInputPortName());
    }

    public final SNumbers getInputNumbers(boolean allowUninitializedData) {
        return getInputNumbers(defaultInputPortName(), allowUninitializedData);
    }

    public final SNumbers getInputNumbersContainer() {
        return getInputNumbersContainer(defaultInputPortName());
    }

    public final SNumbers getNumbers() {
        return getNumbers(defaultOutputPortName());
    }

    public final void putNumbers(SNumbers numbers) {
        putNumbers(defaultInputPortName(), numbers);
    }

    public final void putNumbers(Object javaArray, int blockLength) {
        putNumbers(defaultInputPortName(), javaArray, blockLength);
    }

    public final SScalar getInputScalar(String name) {
        return getInputScalar(name, false);
    }

    public final SScalar getInputScalarContainer(String name) {
        return getInputScalar(name, true);
    }

    public final SScalar getInputScalar(String name, boolean allowUninitializedData) {
        return getRequiredInputPort(name).getData(SScalar.class, allowUninitializedData);
    }

    public final SScalar getScalar(String name) {
        return getRequiredOutputPort(name).getData(SScalar.class, true);
    }

    public final String getStringScalar(String name) {
        return getScalar(name).getValue();
    }

    public final int getIntScalar(String name) {
        return getScalar(name).toInt();
    }

    public final long getLongScalar(String name) {
        return getScalar(name).toLong();
    }

    public final double getDoubleScalar(String name) {
        return getScalar(name).toDouble();
    }

    public final void putScalar(String name, SScalar scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final void putStringScalar(String name, String scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final void putJsonScalar(String name, JsonObject scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final void putIntScalar(String name, int scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final void putLongScalar(String name, long scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final void putDoubleScalar(String name, double scalar) {
        addInputScalar(name);
        getInputScalarContainer(name).setTo(scalar);
    }

    public final SScalar getInputScalar() {
        return getInputScalar(defaultInputPortName());
    }

    public final SScalar getInputScalar(boolean allowUninitializedData) {
        return getInputScalar(defaultInputPortName(), allowUninitializedData);
    }

    public final SScalar getInputScalarContainer() {
        return getInputScalarContainer(defaultInputPortName());
    }

    public final SScalar getScalar() {
        return getScalar(defaultOutputPortName());
    }

    public final String getStringScalar() {
        return getScalar().getValue();
    }

    public final int getIntScalar() {
        return getScalar().toInt();
    }

    public final long getLongScalar() {
        return getScalar().toLong();
    }

    public final double getDoubleScalar() {
        return getScalar().toDouble();
    }

    public final void putScalar(SScalar scalar) {
        putScalar(defaultInputPortName(), scalar);
    }

    public final void putStringScalar(String value) {
        putStringScalar(defaultInputPortName(), value);
    }

    public final void putJsonScalar(JsonObject value) {
        putJsonScalar(defaultInputPortName(), value);
    }

    public final void putIntScalar(int value) {
        putIntScalar(defaultInputPortName(), value);
    }

    public final void putLongScalar(long value) {
        putLongScalar(defaultInputPortName(), value);
    }

    public final void putDoubleScalar(double value) {
        putDoubleScalar(defaultInputPortName(), value);
    }

    @UsedByNativeCode
    public final Parameters parameters() {
        return parameters;
    }

    public final void setBooleanParameter(String name, boolean value) {
        parameters.setBoolean(name, value);
        onChangeParameter(name);
    }

    public final void setIntParameter(String name, int value) {
        parameters.setInteger(name, value);
        onChangeParameter(name);
    }

    public final void setLongParameter(String name, long value) {
        parameters.setLong(name, value);
        onChangeParameter(name);
    }

    public final void setDoubleParameter(String name, double value) {
        parameters.setDouble(name, value);
        onChangeParameter(name);
    }

    public final void setStringParameter(String name, String value) {
        parameters.setString(name, value);
        onChangeParameter(name);
    }


    @UsedByNativeCode
    public final boolean isVisibleResultNecessary() {
        return visibleResultNecessary;
    }

    /**
     * Native code sets visible result to <tt>true</tt> when it is necessary to display result in any way,
     * even if execution block does not have any other connections.
     *
     * @param visibleResultNecessary new visible result flag.
     */
    @UsedByNativeCode
    public final void setVisibleResultNecessary(boolean visibleResultNecessary) {
        this.visibleResultNecessary = visibleResultNecessary;
    }

    @UsedByNativeCode
    public final boolean isAllOutputsNecessary() {
        return allOutputsNecessary;
    }

    /**
     * If this flag is set, all output ports are considered always necessary, even when they are not connected.
     * It can be useful to simplify using executors from Java or other programming languages:
     * you can just set all input data and parameters, executor the function and retrieve all results,
     * without separate call of {@link Executor#requestOutput(String...)}
     * or similar methods.
     * <p>Default value is <tt>false</tt>.
     *
     * @param allOutputsNecessary always output necessary flag.
     */
    @UsedByNativeCode
    public final void setAllOutputsNecessary(boolean allOutputsNecessary) {
        this.allOutputsNecessary = allOutputsNecessary;
    }

    public final ExecutionBlock getRootCaller() {
        return rootCaller;
    }

    public final ExecutionBlock getCaller() {
        return caller;
    }

    /**
     * Sets caller of this executor (optional). Used for managing status:
     * setting status of this object, by default, affects the status of the caller.
     *
     * @param caller some another executor, that probably calls this one as a part.
     */
    public final void setCaller(ExecutionBlock caller) {
        this.caller = caller;
        this.rootCaller = caller == null ? this : caller.rootCaller;
    }

    public final String getSessionId() {
        return sessionId;
    }

    /**
     * Sets ID of the session, in which this executor works. Called automatically while creating by
     * {@link #newExecutionBlock(String, String, String)}. Can be useful in executors,
     * that compiles new functions and changes the current session environment.
     *
     * @param sessionId unique session ID (1st argument of {@link #newExecutionBlock(String, String, String)}).
     */
    public final void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the unique ID of this executor, which was set while creating by
     * {@link #newExecutionBlock(String, String, String)}.
     */
    public final String getExecutorId() {
        return executorId;
    }

    /**
     * Gets the specification of this executor, which was set while creating by
     * {@link #newExecutionBlock(String, String, String)}.
     */
    public final String getExecutorSpecification() {
        return executorSpecification;
    }

    public final String getPlatformId() {
        final JsonObject specification = executorSpecificationJson();
        return specification == null ? null : specification.getString("platform_id", null);
    }

    public final String getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the unique ID of some other executor or essence, to which this executor "belongs".
     * It is optional ID. For example, for executor, customizing main settings of some chain,
     * it can be executor ID of this chain.
     *
     * @param ownerId unique ID of the "owner" of this executor.
     */
    @UsedByNativeCode
    public final ExecutionBlock setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public final Object getContextId() {
        return contextId;
    }

    /**
     * Sets unique ID of the context, in which this executor works.
     * It defines the scope of variables and other objects, that should be shared between
     * several executors. For example, if it is an element of some chain, it can be an unique ID
     * of this chain in the memory.
     *
     * <p>Note: unlike ID, returned by {@link #getExecutorId()} method, this identifier should be
     * unique for every <b>instance</b> of the context. For example, if the chain calls itself recursively,
     * the called instance will have <b>another</b> context ID.
     *
     * <p>Also note: if the same context frees all its resources ("closing"), but may be used again
     * (reinitialization), then the context ID <b>must be renewed</b> after reinitialization.
     * It is necessary, for example, if some resources are stored in <tt>WeakHashMap</tt> with context ID
     * as its keys or if there are some external resources (files), location of which is based on this ID.
     * When such resources became a garbage, we must not try to use them again.
     *
     * <p>This method should be called after
     * creating instance before first calling {@link #reset()} or {@link #execute()} method.
     * Note that most executors do not use this ability, and you can stay undefined (<tt>null</tt>)
     * context ID for them.
     *
     * @param contextId unique context ID; may be <tt>null</tt>.
     */
    public final void setContextId(Object contextId) {
        this.contextId = contextId;
    }

    @UsedByNativeCode
    public final void setContextId(long contextId) {
        setContextId(Long.valueOf(contextId));
    }

    public final String getContextName() {
        return contextName;
    }

    /**
     * Sets some textual name of the context, in which this executor works.
     * For example, if it is an element of some chain, it can be the name
     * of this chain.
     *
     * <p>This method should be called after
     * creating instance before first calling {@link #reset()} or {@link #execute()} method.
     * Note that most executors do not use this ability, and you can stay undefined (<tt>null</tt>)
     * context name for them.
     *
     * @param contextName some textual name of the context; may be <tt>null</tt>.
     */
    @UsedByNativeCode
    public final void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public final String getContextPath() {
        return contextPath;
    }

    /**
     * Sets the path to file or folder, where the context of this executor is stored.
     * For example, if it is an element of some chain, it can be the path to the chain JSON file.
     *
     * <p>This method should be called after
     * creating instance before first calling {@link #reset()} or {@link #execute()} method.
     * Note that most executorы do not use this ability, and you can stay undefined (<tt>null</tt>)
     * context path for them.
     *
     * @param contextPath the path fo file/folder, storing the context; may be <tt>null</tt>.
     */
    @UsedByNativeCode
    public final void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public final Path contextPath() {
        try {
            return contextPath == null ? null : Paths.get(contextPath);
        } catch (RuntimeException ignored) {
            // - unlikely case of error in Paths.get
            return null;
        }
    }

    @UsedByNativeCode
    public Object status() {
        return null;
    }

    @UsedByNativeCode
    public String statusData(int dataCode) {
        return null;
    }

    public boolean isInterruptionRequested() {
        return interruptionRequested;
    }

    @UsedByNativeCode
    public void setInterruptionRequested(boolean interruptionRequested) {
        this.interruptionRequested = interruptionRequested;
    }

    public boolean isInterrupted() {
        return interruptionRequested || rootCaller.interruptionRequested;
        // - usually rootCaller.interruptionRequested is enough;
        // but interruptionRequested is also checked - for situation, when we (maybe) ignore logic of "caller"
    }

    /**
     * Tries to interrupt current executor. May be overridden in some executors to inform more low-level
     * functions about interruption request.
     */
    @UsedByNativeCode
    public void interrupt() {
        setInterruptionRequested(true);
    }

    /**
     * Function invoked when the parameter with the specified name has been changed.
     * <p>Note: this function <b>must</b> be called after any changes in
     * the parameters set. If it was not called, the executor may not work properly.
     *
     * <p>Example:
     * <pre>
     * if (name.equals("factor")) {
     *     double factor = executor.{@link #parameters() parameters()}.getDouble(name, 0.0);
     * }
     * </pre>
     *
     * @param name parameter name
     */
    @UsedByNativeCode
    public void onChangeParameter(String name) {
    }

    /**
     * Function called before {@link #execute()}, but, if there is a loop, called only before <b>1st iteration</b>
     * of the loop.
     * If this executor stores some state (like index of the processed
     * file inside a folder), this method should reset it to initial state.
     *
     * <p>You may override this method, but we recommend to override {@link Executor#initialize()} instead.
     */
    @UsedByNativeCode
    public void reset() {
    }

    public final boolean isReadOnlyInput() {
        return this instanceof ReadOnlyExecutionInput && ((ReadOnlyExecutionInput) this).isReadOnly();
    }

    public final boolean isClosed() {
        return closed;
    }

    /**
     * Main execution.
     *
     * <p>You may override this method, but we recommend to override {@link Executor#process()} instead.
     */
    @UsedByNativeCode
    public abstract void execute();

    /**
     * If this function returns <tt>true</tt>, it means that calculations
     * are not finished and should be repeated (for example, all the chain should be restarted and executed again).
     * Note: this function may be called <b>without</b> {@link #execute()}.
     *
     * @return whether the caller should repeat execution.
     */
    @UsedByNativeCode
    public boolean needToRepeat() {
        return false;
    }

    // Never returns <tt>null</tt>
    @UsedByNativeCode
    public abstract ExecutionVisibleResultsInformation visibleResultsInformation();

    @UsedByNativeCode
    @Override
    public void close() {
        closed = true;
        caller = null;
        // - to be on the safe side: helps to collect garbage
    }

    /**
     * This method must be called before <b>any</b> usage of ExecutionBlock instances.
     */
    @UsedByNativeCode
    public static void initializeExecutionSystem() {
        Initialization.initialize();
    }

    @UsedByNativeCode
    public static void beforeExecutingAll() {
        synchronized (tasksBeforeExecutingAll) {
            for (Runnable task : tasksBeforeExecutingAll.values()) {
                task.run();
            }
        }
    }

    @UsedByNativeCode
    public static void afterExecutingAll() {
        synchronized (oneTimeTasksAfterExecutingAll) {
            for (Runnable task : oneTimeTasksAfterExecutingAll.values()) {
                task.run();
            }
            oneTimeTasksAfterExecutingAll.clear();
        }
        synchronized (tasksAfterExecutingAll) {
            for (Runnable task : tasksAfterExecutingAll.values()) {
                task.run();
            }
        }
    }

    public static boolean addTaskBeforeExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return tasksBeforeExecutingAll.put(System.identityHashCode(task), task) == null;
    }

    public static boolean removeTaskBeforeExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return tasksBeforeExecutingAll.remove(System.identityHashCode(task)) != null;
    }

    public static Collection<Runnable> allTasksBeforeExecutingAll() {
        return Collections.unmodifiableCollection(tasksBeforeExecutingAll.values());
    }

    public static boolean addTaskAfterExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return tasksAfterExecutingAll.put(System.identityHashCode(task), task) == null;
    }

    public static boolean removeTaskAfterExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return tasksAfterExecutingAll.remove(System.identityHashCode(task)) != null;
    }

    public static Collection<Runnable> allTasksAfterExecutingAll() {
        return Collections.unmodifiableCollection(tasksAfterExecutingAll.values());
    }

    public static boolean addOneTimeTaskAfterExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return oneTimeTasksAfterExecutingAll.put(System.identityHashCode(task), task) == null;
    }

    public static boolean removeOneTimeTaskAfterExecutingAll(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        return oneTimeTasksAfterExecutingAll.remove(System.identityHashCode(task)) != null;
    }

    public static Collection<Runnable> allOneTimeTasksAfterExecutingAll() {
        return Collections.unmodifiableCollection(oneTimeTasksAfterExecutingAll.values());
    }

    public static String recommendedName(String className) {
        if (className == null) {
            return null;
        }
        final int p = className.lastIndexOf('.');
        return className.substring(p + 1);
    }

    public static String recommendedCategory(String className) {
        if (className == null) {
            return null;
        }
        final int p = className.lastIndexOf('.');
        return p == -1 ? null : className.substring(0, p);
    }

    /**
     * <p>Creates new instance of {@link ExecutionBlock} on the base of it's description, usually in JSON format.</p>
     *
     * <p>This description is passed by <tt>executorSpecification</tt> parameter. It may be the full description
     * of executor model (with "app":"executor" and other fields), but may be also its part.
     * In any case it must contain all information, necessary for constructing and initializing Java class
     * of the executor, usually in "java" section.
     * Below is a minimal example of <tt>executorSpecification</tt> for most standard Java executors:</p>
     *
     * <pre>
     * {
     *     "java": {
     *         "class": "net.algart.executors.modules.core.system.SystemInformation"
     *     }
     * }
     * </pre>
     *
     * @param sessionId             unique ID of current session while multi-session usage;
     *                              may be <tt>null</tt> while simple usage.
     * @param executorId            unique ID of this executor in the system (maybe a field inside
     *                              <tt>executorSpecification</tt>, but it is not necessary).
     * @param executorSpecification specification of the executor, usually JSON.
     * @return newly created executor.
     * @throws ClassNotFoundException if Java class, required for creating executing block,
     *                                is not available in the current <tt>classpath</tt> environment.
     * @throws NullPointerException   if <tt>executorId==null</tt> or <tt>executorSpecification==null</tt>.
     */
    @UsedByNativeCode
    public static ExecutionBlock newExecutionBlock(String sessionId, String executorId, String executorSpecification)
            throws ClassNotFoundException {
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(executorSpecification, "Null executorSpecification");
        final List<ExecutionBlockLoader> loaders = executionBlockLoaders();
        for (int k = loaders.size() - 1; k >= 0; k--) {
            // Last registered loaders override previous
            final ExecutionBlockLoader loader = loaders.get(k);
            final ExecutionBlock executor = loader.newExecutionBlock(sessionId, executorId, executorSpecification);
            if (executor != null) {
                executor.sessionId = sessionId;
                executor.executorId = executorId;
                executor.executorSpecification = executorSpecification;
//                System.out.println("Specification: " + executorSpecification);
                return executor;
            }
        }
        throw new IllegalArgumentException("Cannot create executor with ID " + executorId
                + ": unknown executor model format");
    }

    @UsedByNativeCode
    public static String[] availableExecutorModelArray(String sessionId) {
        return availableExecutorModelDescriptions(sessionId).values().toArray(new String[0]);
    }

    /**
     * Returns executors' descriptions (probable JSONs, like in
     * {@link ExecutorJson})
     * for all executors, dynamically created by some Java functions for the given session
     * <b>and</b> for global session {@link #GLOBAL_SHARED_SESSION_ID}.
     * Keys in the result are ID of every executor, values are descriptions.
     * May be used for the user interface to show information for available executors.
     *
     * @param sessionId unique ID of current session; may be <tt>null</tt>, than only global session will be checked.
     * @return all available executors' descriptions for dynamically created executors.
     * @throws NullPointerException if <tt>sessionId==null</tt>.
     */
    public static Map<String, String> availableExecutorModelDescriptions(String sessionId) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (ExecutionBlockLoader loader : executionBlockLoaders()) {
            result.putAll(loader.availableExecutorModelDescriptions(GLOBAL_SHARED_SESSION_ID));
            if (sessionId != null) {
                result.putAll(loader.availableExecutorModelDescriptions(sessionId));
            }
//            System.out.println("!!! " + loader + ": " + result.size() + " executors");
        }
        return result;
    }

    /**
     * Returns <tt>{@link #availableExecutorModelDescriptions(String)
     * availableExecutorModelDescriptions}(sessionId).get(executorId)</tt>,
     * but works quickly (without creating new map).
     *
     * @param sessionId  unique ID of current session; may be <tt>null</tt>, than only global session will be checked.
     * @param executorId unique ID of this executor in the system.
     * @return description of this dynamic executor (probably JSON) or <tt>null</tt> if there is not such executor.
     * @throws NullPointerException if one of arguments is <tt>null</tt>.
     */
    public static String getExecutorModelDescription(String sessionId, String executorId) {
        synchronized (executionBlockLoaders) {
            for (ExecutionBlockLoader loader : executionBlockLoaders) {
                String result = loader.getExecutorModelDescription(GLOBAL_SHARED_SESSION_ID, executorId);
                if (result != null) {
                    return result;
                }
                if (sessionId != null) {
                    result = loader.getExecutorModelDescription(sessionId, executorId);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Removes all executors, dynamically created for the given session.
     *
     * @param sessionId unique ID of current session.
     * @throws NullPointerException if <tt>sessionId==null</tt>.
     */
    @UsedByNativeCode
    public static void clearSession(String sessionId) {
        Objects.requireNonNull(sessionId, "Null executorId");
        for (ExecutionBlockLoader loader : executionBlockLoaders()) {
            loader.clearSession(sessionId);
        }
    }

    public static void registerExecutionBlockLoader(ExecutionBlockLoader loader) {
        synchronized (executionBlockLoaders) {
            executionBlockLoaders.add(loader);
        }
    }

    private JsonObject executorSpecificationJson() {
        final String executorSpecification = this.executorSpecification;
        if (executorSpecification == null) {
            return null;
        }
        JsonObject executorSpecificationJson = this.executorSpecificationJson;
        if (executorSpecificationJson == null) {
            try (final JsonReader reader = Json.createReader(new StringReader(executorSpecification))) {
                executorSpecificationJson = reader.readObject();
            } catch (RuntimeException e) {
                executorSpecificationJson = Json.createObjectBuilder()
                        .add("exception", e.getMessage())
                        .build();
            }
            this.executorSpecificationJson = executorSpecificationJson;
        }
        return executorSpecificationJson;

    }

    private static List<ExecutionBlockLoader> executionBlockLoaders() {
        synchronized (executionBlockLoaders) {
            return new ArrayList<>(executionBlockLoaders);
        }
    }

    private static class Initialization {
        private static boolean initialized = false;

        // Usually called only once. So, it is better to provide an explicit function instead of
        // static initialization block: it allows to avoid strange exception ExceptionInInitializerError
        private static synchronized void initialize() {
            if (!initialized) {
                try {
                    ExecutorJsonSet.findAllBuiltIn();
                    standardJavaExecutorLoader.registerAllStandardModels();
                    UsingPython.initializePython();
                    UsingPython.useAllInstalledInSharedContext();
                    UseJS.useAllInstalledInSharedContext();
                    UseSettings.useAllInstalledInSharedContext();
                    UseSubChain.useAllInstalledInSharedContext();
                    UseMultiChain.useAllInstalledInSharedContext();
                    initialized = true;
                    // - in a case of any error, next initialization will raise an error again
                } catch (ExecutionSystemConfigurationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ExecutionSystemConfigurationException("Some problem occurred while initialization; "
                            + "please check configuration files"
                            + (e instanceof NoSuchFileException ?
                            " (no such file/folder: " + ((NoSuchFileException) e).getFile() + ")" :
                            ""),
                            e);
                }
            }
        }

        static void dummy() {
        }
    }
}
