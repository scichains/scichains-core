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

package net.algart.executors.api.model;

import net.algart.contexts.InterruptionException;
import net.algart.executors.api.*;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.modules.core.common.FunctionTiming;
import net.algart.executors.modules.core.common.TimingStatistics;

import java.io.IOError;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class ChainBlock {
    private static final String DEFAULT_CHAIN_PORT_CAPTION_PATTERN = "[$$$]";

    private static final boolean ANALYSE_CONDITIONAL_INPUTS = SystemEnvironment.getBooleanProperty(
            "net.algart.executors.api.analyseConditionalInputs", true);
    // - can be set to false for debugging needs; it will decrease the speed of executing some sub-chains
    // and will lead to stack overflow in recursive sub-chains

    private static final Logger LOG = System.getLogger(ChainBlock.class.getName());

    Chain chain;
    final String id;
    private final String executorId;
    final ExecutorJson executorJson;
    // - The last field MAY stay to be null if it refers to a dynamic executor (like another sub-chain).
    // We use this information:
    // 1) for detecting standardInput, standardOutput, standardData in ChainBlock.valueOf method
    // and (for standard data) for their getDataType() in setTo(Chain) method;
    // so, we require that such special types of executors MUST be static (available while loading the chain);
    // 2) for creating additional "dynamic" ports (not specified in the chain JSON-file) in loadPorts() method;
    // so, we require that the behaviour of dynamic executors must not depend on the set of existing ports
    // (like in some static Java executors, inherited from SeveralMultiMatricesProcessing);
    // 3) for loading default values in the properties in ChainProperty.valueOf();
    // 4) for diagnostic messages.

    ChainJson.ChainBlockConf blockConfJson = null;
    // - Correctly filled while typical usage, but not necessary for this technology.
    // It is used mostly for diagnostic messages and can be useful for external clients,
    // and also for making more user-friendly executor JSON in ExecutorJson.setTo(Chain) method

    private ExecutionStage executionStage = ExecutionStage.RUN_TIME;
    private boolean enabled = true;
    private String systemName = null;

    private boolean standardInput = false;
    private boolean standardOutput = false;
    private boolean standardData = false;
    private String standardInputOutputPortName = null;

    volatile Executor executor = null;

    private final Object lock = new Object();

    final Map<String, ChainProperty> properties = new LinkedHashMap<>();
    final Map<ChainPortKey, ChainInputPort> inputPorts = new LinkedHashMap<>();
    final Map<ChainPortKey, ChainOutputPort> outputPorts = new LinkedHashMap<>();
    // - Unlike ChainJson, the keys in inputPorts and outputPorts are name + port type pairs (not UUID).
    // Note: we must use such a pair, not a single name: it is a correct situation when a virtual port
    // has the same name as an actual port.

    private final AtomicInteger numberOfExecutionsForAssertion = new AtomicInteger(0);
    private final AtomicBoolean needToReset = new AtomicBoolean(true);

    // The following fields are filled in initialize() method
    private volatile boolean ready;
    private volatile boolean readyAlwaysNecessaryInputs;
    private volatile boolean dataFreed;
    private volatile boolean closed;
    private volatile boolean checkingNow;

    private FunctionTiming timing;
    private volatile int executionOrder;

    private ChainBlock(Chain chain, String id, String executorId) {
        this.chain = Objects.requireNonNull(chain, "Null containing chain");
        this.id = Objects.requireNonNull(id, "Null block id");
        this.executorId = Objects.requireNonNull(executorId, "Null block executorId");
        ExecutorProvider executorProvider = chain.getExecutorProvider();
        this.executorJson = executorProvider == null ? null : executorProvider.executorJson(executorId);
        // - Note: executorJson MAY be null until initializing and registering all dynamic executors:
        // see comments to this field.
        // We must be able to CREATE a new chain when some executors are not registered yet:
        // we do it, for example, while registering new sub-chains-as-executors
        // (see comments inside StandardExecutorProvider.executorJson).
        // But we can delay actual assigning correct executorJson until reinitialize method.
        initialize();
    }

    private ChainBlock(ChainBlock block, Chain newChain) {
        this(block);
        Objects.requireNonNull(newChain, "Null new chain");
        this.chain = newChain;
    }

    // Classic copy constructor: we create it (in addition to the previous one) to help IDEs
    // to check the correctness of copying all fields.
    private ChainBlock(ChainBlock block) {
        Objects.requireNonNull(block, "Null chain block");
        this.chain = block.chain;
        this.id = block.id;
        this.executorId = block.executorId;
        this.executorJson = block.executorJson;

        this.blockConfJson = block.blockConfJson;
        this.executionStage = block.executionStage;
        this.enabled = block.enabled;
        this.systemName = block.systemName;

        this.standardInput = block.standardInput;
        this.standardOutput = block.standardOutput;
        this.standardData = block.standardData;
        this.standardInputOutputPortName = block.standardInputOutputPortName;

        this.executor = null;
        // - IMPORTANT: executor must not be shallow-cloned here!
        // Executors almost always are not thread-safe: they store some information in output ports.
        // If the same executor instance does this in parallel in different threads
        // (that is possible in multithreading recursive chains),
        // the result will be chaos in output ports.
        // Results may be even worse if an executor has a non-trivial internal state.
        //
        // Note: this problem rarely occurs because usually the cloned chain contains
        // non-initialized executors (created by UseSubChain or UseMultiChain).
        // But here is an important exception: method ExecutorJson.setTo(Chain chain),
        // used inside UseSubChain to build a chain executor model,
        // initializes data blocks (with options.behavior.data = true) to know the default
        // corresponding parameters values of the sub-chain executor.
        // This situation really can lead to bug in recursive chains.

        initialize();
        this.properties.putAll(block.properties);
        block.inputPorts.forEach((key, port) -> this.inputPorts.put(key, port.cleanCopy(this)));
        block.outputPorts.forEach((key, port) -> this.outputPorts.put(key, port.cleanCopy(this)));
        // - copying ports, not their data (all ports will be empty at the beginning)
    }

    public static ChainBlock newInstance(Chain chain, String id, String executorId) {
        return new ChainBlock(chain, id, executorId);
    }

    public ChainBlock cleanCopy(Chain newChain) {
        return new ChainBlock(this, newChain);
        // Copying constructor is better than cloning, because we do not clone final fields like "lock"

//        Objects.requireNonNull(newChain, "Null new chain");
//        final ChainBlock clone;
//        try {
//            clone = (ChainBlock) super.clone();
//        } catch (CloneNotSupportedException e) {
//            throw new AssertionError(e);
//        }
//        clone.chain = newChain;
//        clone.lock = new Object();
        // - must not be synchronized by the same lock!
//        clone.initialize();
//        clone.properties.putAll(properties);
//        inputPorts.forEach((key, value) -> clone.inputPorts.put(key, value.cleanCopy(clone)));
//        outputPorts.forEach((key, value) -> clone.outputPorts.put(key, value.cleanCopy(clone)));
//        return clone;
    }

    public static String standardInputOutputPortCaption(String systemName) {
        return systemName == null ? null : DEFAULT_CHAIN_PORT_CAPTION_PATTERN.replace("$$$", systemName);
    }

    public static ChainBlock valueOf(Chain chain, ChainJson.ChainBlockConf blockConf) {
        Objects.requireNonNull(blockConf, "Null blockConf");
        final String executorId = blockConf.getExecutorId();
        final ChainBlock result = newInstance(chain, blockConf.getUuid(), executorId);
        result.blockConfJson = blockConf;
        result.setExecutionStage(blockConf.getExecutionStage());
        result.setEnabled(blockConf.getSystem().isEnabled());
        result.setSystemName(blockConf.getSystem().name());
        final boolean enabledRunTime = result.isExecutedAtRunTime();
        result.setStandardInput(enabledRunTime && result.executorJson != null && result.executorJson.isInput());
        result.setStandardOutput(enabledRunTime && result.executorJson != null && result.executorJson.isOutput());
        result.setStandardData(enabledRunTime && result.executorJson != null && result.executorJson.isData());
        result.loadProperties(blockConf);
        result.loadPorts(blockConf);
        result.setEnabledByLegacyWayIfNecessary();
        result.setSystemNameByLegacyWayIfNecessary();
        if (result.executorJson == null) {
            LOG.log(Logger.Level.DEBUG, () -> "Model for executor " + executorId + " is not registered yet "
                    + "(while creating chain block) and will be probably loaded later; "
                    + result.detailedMessage());
        }
        return result;
    }

    public static void prepareExecution(Collection<ChainBlock> blocks) {
        blocks.forEach(ChainBlock::prepareExecution);
    }

    public static void executeWithAllDependentInputs(Collection<ChainBlock> blocks, boolean multithreading) {
        final Stream<ChainBlock> blockStream = multithreading ?
                blocks.parallelStream() :
                blocks.stream();
        blockStream.forEach(ChainBlock::executeWithAllDependentInputs);
    }

    public String getId() {
        return id;
    }

    public String getExecutorId() {
        return executorId;
    }

    /**
     * Returns executor's JSON or <code>null</code> if it is unknown (possible for dynamically loaded executors).
     *
     * @return description of the executor.
     */
    public ExecutorJson getModel() {
        return executorJson;
    }

    public ChainJson.ChainBlockConf getBlockConfJson() {
        return blockConfJson;
    }

    public Map<String, ChainProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Collection<ChainInputPort> getAllInputPorts() {
        return Collections.unmodifiableCollection(inputPorts.values());
    }

    public Collection<ChainOutputPort> getAllOutputPorts() {
        return Collections.unmodifiableCollection(outputPorts.values());
    }

    public ChainProperty getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public void addProperty(ChainProperty property) {
        Objects.requireNonNull(property, "Null property");
        if (properties.putIfAbsent(property.getName(), property) != null) {
            throw new IllegalArgumentException("Duplicate property name: " + property.getName());
        }
    }

    public ChainInputPort getActualInputPort(String portName) {
        return inputPorts.get(new ChainPortKey(ChainPortType.INPUT_PORT, portName));
    }

    public void addInputPort(ChainInputPort inputPort) {
        Objects.requireNonNull(inputPort, "Null input port");
        if (inputPorts.putIfAbsent(inputPort.key, inputPort) != null) {
            throw new IllegalArgumentException("Duplicate input port name: " + inputPort.key);
        }
    }

    public ChainOutputPort getActualOutputPort(String portName) {
        return outputPorts.get(new ChainPortKey(ChainPortType.OUTPUT_PORT, portName));
    }

    public void addOutputPort(ChainOutputPort outputPort) {
        Objects.requireNonNull(outputPort, "Null output port");
        if (outputPorts.putIfAbsent(outputPort.key, outputPort) != null) {
            throw new IllegalArgumentException("Duplicate output port name: " + outputPort.key);
        }
    }

    public int numberOfConnectedInputPorts() {
        return (int) inputPorts.values().stream().filter(ChainPort::isConnected).count();
    }

    public int numberOfConnectedOutputPorts() {
        return (int) outputPorts.values().stream().filter(ChainPort::isConnected).count();
    }

    public ExecutionStage getExecutionStage() {
        return executionStage;
    }

    public ChainBlock setExecutionStage(ExecutionStage executionStage) {
        this.executionStage = Objects.requireNonNull(executionStage, "Null executionStage");
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ChainBlock setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getSystemName() {
        return systemName;
    }

    public ChainBlock setSystemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public Executor getExecutor() {
        synchronized (lock) {
            if (executor == null) {
                throw new IllegalStateException("Executor is not initialized for " + this);
            }
            return executor;
        }
    }

    public boolean isStandardInput() {
        return standardInput;
    }

    public ChainBlock setStandardInput(boolean standardInput) {
        this.standardInput = standardInput;
        return this;
    }

    public boolean isStandardOutput() {
        return standardOutput;
    }

    public ChainBlock setStandardOutput(boolean standardOutput) {
        this.standardOutput = standardOutput;
        return this;
    }

    public boolean isStandardData() {
        return standardData;
    }

    public ChainBlock setStandardData(boolean standardData) {
        this.standardData = standardData;
        return this;
    }

    /**
     * Returns the input/output argument nameof the whole chain, recommended while using the chain
     * as a single function. Should be customized, for example, by {@link Chain#setAllDefaultInputNames()}
     * and {@link Chain#setAllDefaultOutputNames()} methods.
     *
     * @return the name of input or output, to which this block corresponds while using the chain as a function.
     */
    public String getStandardInputOutputName() {
        return standardInputOutputPortName;
    }

    public ChainBlock setStandardInputOutputPortName(String standardInputOutputPortName) {
        this.standardInputOutputPortName = standardInputOutputPortName;
        return this;
    }

    public String getStandardInputOutputPortCaption() {
        return standardInputOutputPortCaption(getSystemName());
    }

    /**
     * Returns the customization parameter name of the whole chain, recommended while using the chain
     * as a single function. Cannot be customized in the current version: it is always equal to
     * {@link #getSystemName()}.
     *
     * @return the name of customization parameter, to which this block corresponds while using the chain
     * as a function.
     */
    public String getStandardParameterName() {
        return getSystemName();
    }

    public ChainInputPort reqStandardDataPort() {
        if (!isStandardData()) {
            throw new IllegalStateException("This block is not a standard data block: " + this);
        }
        final ChainInputPort result = getActualInputPort(Executor.DEFAULT_INPUT_PORT);
        if (result == null) {
            throw new IllegalStateException("Standard data block must have the input port \""
                    + Executor.DEFAULT_INPUT_PORT + "\" (" + this + ")");
        }
        return result;
    }

    public ChainInputPort reqStandardInputPort() {
        if (!isStandardInput()) {
            throw new IllegalStateException("This block is not a standard input block: " + this);
        }
        final ChainInputPort result = getActualInputPort(Executor.DEFAULT_INPUT_PORT);
        if (result == null) {
            throw new IllegalStateException("Standard input block must have the input port \""
                    + Executor.DEFAULT_INPUT_PORT + "\" (" + this + ")");
        }
        return result;
    }

    public ChainOutputPort reqStandardOutputPort() {
        if (!isStandardOutput()) {
            throw new IllegalStateException("This block is not a standard output block: " + this);
        }
        final ChainOutputPort result = getActualOutputPort(Executor.DEFAULT_OUTPUT_PORT);
        if (result == null) {
            throw new IllegalStateException("Standard output block must have the output port \""
                    + Executor.DEFAULT_OUTPUT_PORT + "\" (" + this + ")");
        }
        return result;
    }

    public ChainInputPort reqActualInputPort(String portName) {
        Objects.requireNonNull(portName, "Null portName");
        final ChainInputPort result = getActualInputPort(portName);
        if (result == null) {
            throw new IllegalStateException("Block has no input port \"" + portName + "\" (" + this + ")");
        }
        return result;
    }

    public ChainOutputPort reqActualOutputPort(String portName) {
        Objects.requireNonNull(portName, "Null portName");
        final ChainOutputPort result = getActualOutputPort(portName);
        if (result == null) {
            throw new IllegalStateException("Block has no output port \"" + portName + "\" (" + this + ")");
        }
        return result;
    }

    public void setActualInputData(String portName, Data data) {
        Objects.requireNonNull(data, "Null data");
        final ChainInputPort inputPort = reqActualInputPort(portName);
        inputPort.getData().setTo(data, true);
        // - cloning data, because ports in the chain can be freed
    }

    public void getActualOutputData(String portName, Data resultData) {
        Objects.requireNonNull(resultData, "Null resultData");
        final ChainOutputPort outputPort = reqActualOutputPort(portName);
        resultData.setTo(outputPort.getData(), true);
        // - cloning resultData, because ports in the chain can be freed
    }

    public boolean isExecutedAtLoadingTime() {
        synchronized (lock) {
            return isEnabled() && executionStage == ExecutionStage.LOADING_TIME;
        }
    }

    public boolean isExecutedAtRunTime() {
        synchronized (lock) {
            return isEnabled() && executionStage == ExecutionStage.RUN_TIME;
        }
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * Whether {@link #freeData()} was called after {@link #prepareExecution()}.
     * Can be used for debugging needs.
     *
     * @return whether the data are freed.
     */
    public boolean isDataFreed() {
        return dataFreed;
    }

    /**
     * Whether {@link #freeResources()} was called after {@link #prepareExecution()}.
     * Can be used for debugging needs.
     *
     * @return whether the object is freed.
     */
    public boolean isClosed() {
        return closed;
    }

    public int executionOrder() {
        return executionOrder;
    }

    public void reinitialize(boolean initializeAlsoNonRunTime) throws IllegalStateException {
        synchronized (lock) {
            if (isEnabled() && (initializeAlsoNonRunTime || isExecutedAtRunTime())) {
                // Note: we need to initialize also loading-time blocks.
                // This execution algorithm does not use them, but they can become necessary
                // for some external clients.
                if (this.executor == null) {
                    final ExecutionBlock executionBlock;
                    try {
                        ExecutorProvider executorProvider = chain.getExecutorProvider();
                        if (executorProvider == null) {
                            throw new IllegalStateException("Cannot initialize block with executor ID " + executorId
                                    + ": executor provider is not set");
                        }
                        //noinspection resource
                        executionBlock = executorProvider.newExecutor(executorId);
                    } catch (ClassNotFoundException | ExecutorNotFoundException e) {
                        throw new IllegalStateException("Cannot initialize block with executor ID " + executorId
                                + (this.blockConfJson == null ?
                                "" :
                                " (name=" + ExecutorJson.quote(blockConfJson.getExecutorName())
                                        + ", category=" + ExecutorJson.quote(blockConfJson.getExecutorCategory())
                                        + ")")
                                + (e instanceof ClassNotFoundException ?
                                " - Java class not found: " + e.getMessage() :
                                " - non-registered ID"),
                                e);
                    }
                    // - calling constructor; maybe, some ports are created here
                    if (!(executionBlock instanceof final Executor newExecutor)) {
                        throw new IllegalStateException("Unsupported executor class "
                                + executionBlock.getClass().getName() + ": it must be subclass of "
                                + Executor.class.getName() + " in " + this);
                    }
                    initializePortsSpecifiedInChainConf(newExecutor);
                    newExecutor.setOwnerId(chain.id());
                    newExecutor.setContextId(chain.contextId());
                    newExecutor.setContextName(chain.name());
                    final Path path = chain.chainJsonPath();
                    if (path != null) {
                        newExecutor.setContextPath(path.toAbsolutePath().toString());
                    }
                    updateSystemProperties(newExecutor);
                    updateProperties(newExecutor);
                    newExecutor.setTimingEnabled(chain.isTimingByExecutorsEnabled());
                    this.executor = newExecutor;
                    // - it must be the LAST operation: if previous initialization threw an exception,
                    // this.executor should stay be null (block was NOT correctly initialized)
                }
            }
        }
    }

    public void reset() {
        synchronized (lock) {
            if (isExecutedAtRunTime()) {
                needToReset.set(true);
                executionOrder = -1;
            }
        }
    }

    public boolean needToRepeat() {
        synchronized (lock) {
            return isExecutedAtRunTime() && getExecutor().needToRepeat();
        }
    }

    // This method does not access executor, unlike reset() method
    public void prepareExecution() {
        synchronized (lock) {
            ready = false;
            dataFreed = false;
            closed = false;
            readyAlwaysNecessaryInputs = false;
            numberOfExecutionsForAssertion.set(0);
            for (ChainOutputPort chainOutputPort : outputPorts.values()) {
                chainOutputPort.resetConnectedInputsInformation();
            }
        }
    }

    public void execute() {
        synchronized (lock) {
            // - must be synchronized, because can be called from several threads
            if (!ready) {
                try {
                    if (isExecutedAtRunTime()) {
                        final Executor executor = getExecutor();
                        copyInputPortsToExecutor();
                        try {
                            final Executor caller = chain.getCaller();
                            ExecutionStatus status = caller == null ? null : caller.status();
                            if (status != null) {
                                status.setComment(this::friendlyCaption);
                            }
                            status = executor.status();
                            if (status != null) {
                                // - note that executor.status() cannot be null in the current version
                                status.setExecutorClassId(executorId);
                                status.setExecutorInstanceId(id);
                            }
                            final long t1 = timing.currentTime();
                            if (caller != null && caller.isInterrupted()) {
                                throw new InterruptionException("Execution aborted");
                            }
                            if (needToReset.getAndSet(false)) {
                                executor.reset();
                            }
                            executor.execute();
                            if (executor.needToRepeat()) {
                                chain.needToRepeat = true;
                            }
                            final long t2 = timing.currentTime();
                            timing.updateExecution(t2 - t1);

                        } catch (RuntimeException | AssertionError | IOError e) {
                            if (chain.isIgnoreExceptions()) {
                                Executor.LOG.log(System.Logger.Level.INFO, "IGNORING EXCEPTION:\n      " + e);
                            } else {
                                if (isHighLevelException(e)) {
                                    throw e;
                                }
                                throw translateException(e);
                            }
                        }
                        copyOutputPortsFromExecutor();
                    }
                    executionOrder = chain.executionIndex.getAndIncrement();
                } finally {
                    ready = true;
                    // - it is important to set "ready" also after any exception,
                    // in another case we will have an assertion due to numberOfExecutionsForAssertion
                }
            }
        }
    }

    public void executeWithAllDependentInputs() {
        if (ready) {
            return;
        }
        if (!isExecutedAtRunTime()) {
            return;
        }

//        debugInformation("A");
        final List<ChainInputPort> necessaryAlways = new ArrayList<>();
        final List<ChainInputPort> necessarySometimes = new ArrayList<>();
        checkConnectedInputs(necessaryAlways, necessarySometimes);
        streamOfInputs(necessaryAlways).forEach(chainInputPort -> {
            if (!ready) {
                // - no sense to continue if another thread has already finished processing this block
                chainInputPort.connectedSourceBlock().executeWithAllDependentInputs();
            }
        });
        List<ChainInputPort> actualInputPorts = necessaryAlways;
        if (!necessarySometimes.isEmpty()) {
            final List<ChainInputPort> necessaryNow;
            synchronized (lock) {
                if (!readyAlwaysNecessaryInputs) {
                    // - Important! While multithreading, it could become ready while executing
                    // connected blocks above, as a result of some parallel execution.
                    // In this case, we must not call copyFromConnectedPort() again:
                    // it will lead to IllegalStateException in reduceCountOfConnectedInputs() call.
                    copyFromConnectedPorts(necessaryAlways);
                    copyInputPortsToExecutor(necessaryAlways);
                    readyAlwaysNecessaryInputs = true;
                }
                necessaryNow = allNecessaryNow(necessarySometimes);
            }
            streamOfInputs(necessaryNow).forEach(chainInputPort -> {
                if (!ready) {
                    // - no sense to continue if another thread already finished processing this block
                    chainInputPort.connectedSourceBlock().executeWithAllDependentInputs();
                }
            });
            actualInputPorts = necessaryNow;
        }
        synchronized (lock) {
            if (ready) {
                // - Important! While multithreading, it could become ready while executing
                // connected blocks above, as a result of some parallel execution.
                // In this case, we must not call execute(), and also we must not call copyFromConnectedPort() again:
                // it will lead to IllegalStateException in reduceCountOfConnectedInputs() call.
                return;
            }
            if (numberOfExecutionsForAssertion.incrementAndGet() > 1) {
                throw new AssertionError("Cannot be called more than once: " + this);
            }
            final long t1 = timing.currentTime();
            copyFromConnectedPorts(actualInputPorts);
//            debugInformation("C");
            final long t2 = timing.currentTime();
            execute();
            final long t3 = timing.currentTime();
            timing.updatePassingData(t2 - t1);
            timing.updateSummary(t3 - t1);
        }
    }

    public void freeData() {
        synchronized (lock) {
            for (ChainPort<?> port : inputPorts.values()) {
                port.removeData();
            }
            for (ChainPort<?> port : outputPorts.values()) {
                port.removeData();
            }
            if (executor != null) {
                executor.freeAllPortData();
            }
            dataFreed = true;
        }
    }

    public void freeResources() {
        synchronized (lock) {
            prepareExecution();
            freeData();
            Executor executor = this.executor;
            if (executor != null) {
                this.executor = null;
                // - for a case of recursive calls
                executor.close();
            }
            closed = true;
        }
    }

    public ChainRunningException translateException(Throwable e) {
        return new ChainRunningException(detailedMessage(), e);
    }

    public FunctionTiming timing() {
        return timing;
    }

    public boolean hasTiming() {
        return !timing.isEmpty();
    }

    public void analyseTiming() {
        timing.analyse();
    }

    public String simpleTimingInfo(Double totalTimeOfLastAnalysedCalls) {
        return timing.toSimpleStringForSummary(totalTimeOfLastAnalysedCalls)
                + " - block ID '" + id + "', " + friendlyCaption();
    }

    public String timingInfo() {
        final String blockName = blockConfJson != null ? blockConfJson.getSystem().name() : null;
        return String.format("block%s%s%s%s%s%s ID '%s', executor ID '%s'%s, %s [%X]: %s",
                !isEnabled() ? " [DISABLED]" : "",
                executionStage != ExecutionStage.RUN_TIME ? " [" + executionStage + "]" : "",
                standardInput ? " [input]" : "",
                standardOutput ? " [output]" : "",
                standardData ? " [data]" : "",
                blockName != null ? " \"" + blockName + "\"" : "",
                id,
                executorId,
                executor == null ? "" : ", class " + executor.getClass().getSimpleName(),
                friendlyCaption(true),
                System.identityHashCode(this),
                timing);
    }

    public String friendlyName() {
        return friendlyName(false);
    }

    public void setCaller(Executor caller) {
        synchronized (lock) {
            if (isExecutedAtRunTime()) {
                getExecutor().setCaller(caller);
            }
        }
    }

    public void setTimingSettings(int numberOfAnalysedCalls, TimingStatistics.Settings settings) {
        synchronized (lock) {
            if (isExecutedAtRunTime()) {
                timing.setSettings(numberOfAnalysedCalls, settings);
            }
        }
    }

    @Override
    public String toString() {
        final String systemName = getSystemName();
        final StringBuilder sb = new StringBuilder("ChainBlock"
                + (!isEnabled() ? " [DISABLED]" : "")
                + (executionStage != ExecutionStage.RUN_TIME ? " [" + executionStage + "]" : "")
                + (standardInput ? " [input]" : "")
                + (standardOutput ? " [output]" : "")
                + (standardData ? " [data]" : "")
                + (ready ? ", ready" : "")
                + (dataFreed ? ", data freed" : "")
                + (closed ? ", closed" : "")
                + " {\n"
                + "      ID='" + id + "'\n"
                + (systemName == null ? "" : "      system name='" + systemName + "'\n")
                + "      executor ID='" + executorId + "'"
                + (executorJson == null ? " (no executor JSON)" : " (name='" + executorJson.getName() + "')")
                + "\n"
                + "      address='" + System.identityHashCode(this)
                + " (belongs to " + System.identityHashCode(chain) + ")'\n"
                + "      properties=[\n");
        for (ChainProperty property : properties.values()) {
            sb.append("        ").append(property).append('\n');
        }
        sb.append("      ],\n      inputPorts=[\n");
        for (ChainInputPort port : inputPorts.values()) {
            sb.append("        ").append(port).append('\n');
        }
        sb.append("      ] (").append(numberOfConnectedInputPorts()).append(" connected),\n      outputPorts=[\n");
        for (ChainOutputPort port : outputPorts.values()) {
            sb.append("        ").append(port).append('\n');
        }
        sb.append("      ] (").append(numberOfConnectedOutputPorts()).append(" connected),\n    }");
        return sb.toString();
    }

    public static boolean isHighLevelException(Throwable e) {
        return e instanceof HighLevelException || e instanceof InterruptionException;
        // - actually InterruptionException is also high-level: it does not mean an error in algorithm,
        // it is just a signal that the program was stopped
    }

    // This method must not be called in multithreading mode, unlike execute() method
    void checkRecursiveDependencies() {
        if (ready) {
            return;
        }
        checkingNow = true;
        try {
            for (ChainInputPort chainInputPort : inputPorts.values()) {
                if (chainInputPort.isConnected()) {
                    final ChainBlock sourceBlock = chainInputPort.connectedSourceBlock();
                    if (sourceBlock.checkingNow) {
                        throw new RecursiveDependenceException("Recursive dependence in the chain: "
                                + "cannot calculate " + sourceBlock);
                    }
                    sourceBlock.checkRecursiveDependencies();
                }
                ready = true;
            }
        } finally {
            checkingNow = false;
        }
    }

    void copyInputPortsToExecutor() {
        copyInputPortsToExecutor(inputPorts.values());
    }

    private String friendlyName(boolean useCaption) {
        final String executorName = executorJson != null ? executorJson.getName() :
                blockConfJson != null ? blockConfJson.getExecutorName() : null;
        String caption = null;
        if (useCaption && blockConfJson != null) {
            caption = blockConfJson.getSystem().getCaption();
            if (Objects.equals(caption, executorName)) {
                caption = null;
            }
        }
        return (executorJson == null ? "dynamic " : "")
                + (executorName != null ? "executor '" + executorName + "'" : "executor")
                + (caption != null ? " ('" + caption + "')" : "");
    }

    private String friendlyCaption() {
        return friendlyCaption(false);
    }

    private String friendlyCaption(boolean quoted) {
        final String executorName = executorJson != null ? executorJson.getName() :
                blockConfJson != null ? blockConfJson.getExecutorName() : null;
        String caption = blockConfJson == null ? null : blockConfJson.getSystem().getCaption();
        if (caption == null) {
            caption = executorName;
        }
        return caption != null ?
                (quoted ? "'" + caption + "'" : caption) :
                (executorJson == null ? "dynamic " : "") + "executor";
    }

    private String detailedMessage() {
        final String systemName = getSystemName();
        return "occurred in " + friendlyName(true) + ":\n      "
                + "block ID " + id + "\n      "
                + (systemName == null ? "" : "system name '" + systemName + "'\n      ")
                + (executor == null ? "" : executor.getClass() + "\n      ")
                + (executorJson == null ?
                "probably dynamic executor" :
                "executor name '" + executorJson.getName() + "'")
                + " (executor ID " + executorId + ")\n      "
                + "in the chain '" + chain.name() + "' (ID " + chain.id() + ")";
    }

    private void copyFromConnectedPorts(Collection<ChainInputPort> inputPorts) {
        for (ChainInputPort chainInputPort : inputPorts) {
            chainInputPort.copyFromConnectedPort();
        }
    }

    private void copyInputPortsToExecutor(Collection<ChainInputPort> inputPorts) {
        for (ChainInputPort chainInputPort : inputPorts) {
            try {
                chainInputPort.copyToExecutorPort();
                // - do this even if the port is not connected;
                // actually it allows using data, manually written into the global chain input ports
            } catch (RuntimeException | AssertionError e) {
                throw new ChainRunningException("Occurred while copying input port of "
                        + executor.getClass().getName() + " from "
                        + chainInputPort + " in block " + this, e);
            }
        }
    }

    private void copyOutputPortsFromExecutor() {
        for (ChainOutputPort chainOutputPort : outputPorts.values()) {
            chainOutputPort.copyFromExecutorPort();
        }
    }

    private void checkConnectedInputs(List<ChainInputPort> necessaryAlways, List<ChainInputPort> necessarySometimes) {
        synchronized (lock) {
            assert isExecutedAtRunTime() : "this method should be used for executable blocks only";
            necessaryAlways.clear();
            necessarySometimes.clear();
            for (ChainInputPort inputPort : inputPorts.values()) {
                if (inputPort.isConnected()) {
                    final Boolean necessary = inputPort.necessary();
                    if (!ANALYSE_CONDITIONAL_INPUTS || necessary == null) {
                        necessaryAlways.add(inputPort);
                    } else {
                        necessarySometimes.add(inputPort);
                    }
                }
            }
        }
    }

    private static List<ChainInputPort> allNecessaryNow(List<ChainInputPort> necessarySometimes) {
        final List<ChainInputPort> result = new ArrayList<>();
        for (ChainInputPort inputPort : necessarySometimes) {
            final Boolean necessary = inputPort.necessary();
            if (necessary != null && necessary) {
                result.add(inputPort);
            }
        }
        return result;
    }

    private Stream<ChainInputPort> streamOfInputs(Collection<ChainInputPort> inputs) {
        return chain.isMultithreading() ? inputs.parallelStream() : inputs.stream();
    }

    private void loadProperties(ChainJson.ChainBlockConf blockConf) {
        this.properties.clear();
        for (ChainJson.ChainBlockConf.PropertyConf propertyConf : blockConf.getNameToPropertyMap().values()) {
            addProperty(ChainProperty.valueOf(this, propertyConf));
        }
    }

    private void setEnabledByLegacyWayIfNecessary() {
        final ChainProperty property = properties.get("$__system.enabled");
        if (property != null) {
            final Object value = property.getValue();
            if (value instanceof Boolean) {
                this.enabled = (Boolean) value;
            }
        }
    }

    private void setSystemNameByLegacyWayIfNecessary() {
        final ChainProperty property = properties.get("$__system.name");
        if (property != null) {
            String systemName = property.toScalar();
            if (systemName != null && !(systemName = systemName.trim()).isEmpty()) {
                this.systemName = systemName;
            }
        }
    }

    private void loadPorts(ChainJson.ChainBlockConf blockConf) {
        this.inputPorts.clear();
        this.outputPorts.clear();
        if (this.executorJson != null) {
            for (ExecutorJson.PortConf portConf : this.executorJson.getInPorts().values()) {
                ChainInputPort inputPort = ChainInputPort.valueOf(this, portConf);
                Objects.requireNonNull(inputPort, "Null input port");
                if (inputPorts.putIfAbsent(inputPort.key, inputPort) != null) {
                    throw new IllegalArgumentException("Duplicate input port name: " + inputPort.key);
                }
            }
            for (ExecutorJson.PortConf portConf : this.executorJson.getOutPorts().values()) {
                ChainOutputPort outputPort = ChainOutputPort.valueOf(this, portConf);
                Objects.requireNonNull(outputPort, "Null output port");
                if (outputPorts.putIfAbsent(outputPort.key, outputPort) != null) {
                    throw new IllegalArgumentException("Duplicate output port name: " + outputPort.key);
                }
            }
        }
        // Now input/output ports are loaded from executor's JSON without any ID.
        // It can be important to provide a correct set of ports (even if chain's JSON doesn't specify all ports).
        final Map<ChainPortKey, ChainInputPort> chainInputPorts = new LinkedHashMap<>();
        final Map<ChainPortKey, ChainOutputPort> chainOutputPorts = new LinkedHashMap<>();
        for (ChainJson.ChainBlockConf.PortConf portConf : blockConf.getUuidToPortMap().values()) {
            switch (portConf.getPortType().actualPortType()) {
                // We must add here both actual and virtual ports;
                // we distinguish them by ChainPort.key (i.e., by name + type)
                case INPUT -> {
                    final ChainInputPort inputPort = ChainInputPort.valueOf(this, portConf);
                    if (chainInputPorts.putIfAbsent(inputPort.key, inputPort) != null) {
                        throw new IllegalArgumentException("Duplicate input port name \"" + inputPort.key
                                + "\" in " + blockConf);
                    }
                }
                case OUTPUT -> {
                    final ChainOutputPort outputPort = ChainOutputPort.valueOf(this, portConf);
                    if (chainOutputPorts.putIfAbsent(outputPort.key, outputPort) != null) {
                        throw new IllegalArgumentException("Duplicate output port name \"" + outputPort.key
                                + "\" in " + blockConf);
                    }
                }
            }
            // We replaced existing ports, specified in executor's JSON,
            // with ports, specified in chain's JSON
        }
        this.inputPorts.putAll(chainInputPorts);
        this.outputPorts.putAll(chainOutputPorts);
    }

    private void initializePortsSpecifiedInChainConf(Executor executor) {
        for (ChainInputPort chainInputPort : inputPorts.values()) {
            if (chainInputPort.portType.isActual()) {
                // - Virtual ports do not correspond to any Executor's PORTS, they correspond to its PARAMETERS.
                // Without this check, we have a risk to overwrite correct actual port with incorrect virtual one
                // with the same name - there was such a bug in previous versions.
                final Port port = Port.newInput(chainInputPort.name, chainInputPort.dataType);
                port.setConnected(chainInputPort.isConnected());
                executor.replacePort(port);
                // - we need to replace existing port for correct new one with correct "connected" and data type
            }
        }
        for (ChainOutputPort chainOutputPort : outputPorts.values()) {
            if (chainOutputPort.portType.isActual()) {
                // - virtual ports do not correspond to any Executor's PORTS, they correspond to its PARAMETERS
                final Port port = Port.newOutput(chainOutputPort.name, chainOutputPort.dataType);
                port.setConnected(chainOutputPort.isConnected());
                executor.replacePort(port);
                // - we need to replace existing port for correct new one with correct "connected" and data type
            }
        }
    }

    private void updateProperties(Executor executor) {
        final Parameters executorProperties = executor.parameters();
        for (ChainProperty property : this.properties.values()) {
            executorProperties.put(property.getName(), property.getValue());
        }
        for (String name : this.properties.keySet()) {
            executor.onChangeParameter(name);
        }
    }

    private void updateSystemProperties(Executor executor) {
        executor.setCurrentDirectory(chain.getCurrentDirectory());
        executor.setMultithreadingEnvironment(chain.isMultithreading());
    }

    // Calling only in the constructor and in clone() method
    private void initialize() {
        this.ready = false;
        this.dataFreed = false;
        this.closed = false;
        this.readyAlwaysNecessaryInputs = false;
        this.checkingNow = false;
        this.timing = FunctionTiming.newDisabledInstance();
        this.executionOrder = -1;
        // - NOT clear this.executor: the reference to it should be quickly cloned!
    }

    private void debugInformation(String name) {
        synchronized (chain.blocksInteractionLock) {
            final ChainBlock block = chain.getBlock("14fca6f5-4240-4b31-b27e-bb7e08029dda");
            // - ID of some block which we want to control
            if (block != null) {
                final ChainInputPort input =
                        block.inputPorts.get(new ChainPortKey(ChainPortType.INPUT_PORT, "input"));
                final ChainOutputPort output =
                        block.outputPorts.get(new ChainPortKey(ChainPortType.OUTPUT_PORT, "output"));
                System.out.println(chain.contextId() + "/" + System.identityHashCode(block)
                        + " !!!! " + name + ": "
                        + (input.data.isInitialized() ? input.data : "")
                        + " -> "
                        + output.data
                        + ", " + output.getCountOfConnectedInputs()
                        + (block.isReady() ? ", ready" : ", NOT ready")
                        + (block.isDataFreed() ? ", freed " : "")
                        + " from: " + this.friendlyCaption() + ", " + System.identityHashCode(this)
                );
                chain.debugCheck("BLOCK ");
            }
        }
    }
}
