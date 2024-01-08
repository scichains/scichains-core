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

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.TimingStatistics;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class Chain implements AutoCloseable {
    private static final AtomicLong CURRENT_CONTEXT_ID = new AtomicLong(99000000000L);
    // - Some magic value helps to reduce the chance of accidental coincidence with other contextIDs,
    // probable used in the system in other ways (99 is an ASCII-code of letter 'c').

    private volatile long contextId;
    // - Unique ID for every chain
    private final Executor executionContext;
    // - May be null. When non-null, may be used, for example, for customizing some future system parameters.
    private final String id;
    private final ExecutorProvider executorProvider;
    // - Usually should be not-null. If null, the chain will be unable to create executors.
    private final Map<String, ChainBlock> allBlocks = new LinkedHashMap<>();
    private final Map<String, ChainPort<?>> allPorts = new LinkedHashMap<>();
    private final Set<ChainLink> allLinks = new LinkedHashSet<>();

    private boolean autogeneratedCategory = false;
    private String category = null;
    private boolean autogeneratedName = false;
    private String name = null;
    private String description = null;
    private Set<String> tags = new LinkedHashSet<>();;
    private String platformId = null;
    private String platformCategory = null;
    private Path chainJsonPath = null;
    // - Previous 8 properties are not important for execution, but may make usage more comfortable.

    private volatile Path currentDirectory = null;
    private volatile boolean multithreading = false;
    private volatile boolean executeAll = false;
    private volatile boolean ignoreExceptions = false;
    private volatile boolean timingByExecutorsEnabled = false;
    // - This flag enables executors, called from the chain, to collect statistics about their timing.
    // By default, disabled: measuring time while multithreading execution cannot be correct;
    // instead, we will measure the time of SubChain executor, that execute this chain.
    // But this flag can be set by debugging applications, like ExecutingChain class.
    private volatile Object customChainInformation = null;

    private volatile List<ChainBlock> allInputs = null;
    private volatile List<ChainBlock> allOutputs = null;
    private volatile List<ChainBlock> allData = null;

    private final Object chainLock = new Object();
    // - We must not execute the same chain from different threads:
    // setting and passing input data and parameter will interfere.
    final Object blocksInteractionLock = new Object();
    final AtomicInteger executionIndex = new AtomicInteger(0);
    volatile boolean needToRepeat = false;
    private volatile Executor caller = null;

    private Chain(Executor executionContext, String id, ExecutorProvider executorProvider) {
        this.contextId = CURRENT_CONTEXT_ID.getAndIncrement();
        this.executionContext = executionContext;
        this.id = Objects.requireNonNull(id, "Null chain ID");
        this.executorProvider = executorProvider;
    }

    private Chain(Chain chain) {
        this.contextId = CURRENT_CONTEXT_ID.getAndIncrement();
        this.executionContext = chain.executionContext;
        this.id = chain.id;
        this.executorProvider = chain.executorProvider;
        this.autogeneratedCategory = chain.autogeneratedCategory;
        this.category = chain.category;
        this.autogeneratedName = chain.autogeneratedName;
        this.name = chain.name;
        this.description = chain.description;
        this.tags = new LinkedHashSet<>(chain.tags);
        this.platformId = chain.platformId;
        this.platformCategory = chain.platformCategory;
        this.chainJsonPath = chain.chainJsonPath;

        this.currentDirectory = chain.currentDirectory;
        this.multithreading = chain.multithreading;
        this.executeAll = chain.executeAll;
        this.ignoreExceptions = chain.ignoreExceptions;
        this.timingByExecutorsEnabled = chain.timingByExecutorsEnabled;

        this.customChainInformation = chain.customChainInformation;

        this.allInputs = null;
        this.allOutputs = null;
        this.allData = null;
        // - like in default constructor: they will be automatically recalculated and cached (see getAllInputs etc.)
        this.needToRepeat = false;
        this.caller = null;
        // - like in default constructor; should be set again when necessary

        chain.allBlocks.values().forEach(chainBlock -> this.addBlock(chainBlock.cleanCopy(this)));
        // - also fills this.allPorts
        chain.allLinks.forEach(this::addLink);
    }

    public static Chain newInstance(Executor executionContext, String id, ExecutorProvider executorProvider) {
        return new Chain(executionContext, id, executorProvider);
    }

    /**
     * Returns an exact copy of this chain, but without any data in the ports.
     *
     * @return new clean copy of this chain without processed data in the ports.
     */
    public Chain cleanCopy() {
        return new Chain(this);
        // Copying constructor is better than cloning, because we do not clone final fields like "lock"

//        final Chain clone;
//        try {
//            clone = (Chain) super.clone();
//        } catch (CloneNotSupportedException e) {
//            throw new AssertionError(e);
//        }
//        clone.chainLock = new Object();
        // - must not be synchronized by the same lock!
//        clone.renewContextId();
//        clone.clearCache();
//        clone.allBlocks = new LinkedHashMap<>();
//        clone.allPorts = new LinkedHashMap<>();
//        clone.allLinks = new LinkedHashSet<>();
//        allBlocks.values().forEach(chainBlock -> clone.addBlock(chainBlock.cleanCopy(clone)));
//        allLinks.forEach(clone::addLink);
//        clone.executionIndex = new AtomicInteger(0);
//        clone.needToRepeat = false;
//        clone.caller = null;
//        return clone;
    }

    public static Chain valueOf(Executor executionContext, ExecutorProvider executorProvider, ChainJson chainJson) {
        Objects.requireNonNull(chainJson, "Null chain JSON model");
        final Chain result = newInstance(executionContext, chainJson.chainId(), executorProvider);
        final ChainJson.Executor executor = chainJson.getExecutor();
        result.autogeneratedCategory = executor.isAutogeneratedCategory();
        result.category = executor.getCategory();
        result.autogeneratedName = executor.isAutogeneratedName();
        result.name = executor.getName();
        result.description = executor.getDescription();
        result.tags = new LinkedHashSet<>(chainJson.getTags());
        result.platformId = chainJson.getPlatformId();
        result.platformCategory = chainJson.getPlatformCategory();
        result.chainJsonPath = chainJson.getChainJsonFile();
        // result.chainJson = chainJson;
        // - it is not safe, because ChainJson is mutable and can be modifying outside; not used in current version
        final ChainJson.Executor.Options.Execution execution = executor.getOptions().getExecution();
        result.setExecuteAll(execution.isAll());
        result.setMultithreading(execution.isMultithreading());
        result.setIgnoreExceptions(execution.isIgnoreExceptions());
        for (ChainJson.ChainBlockConf blockConf : chainJson.getBlocks()) {
            result.addBlock(ChainBlock.valueOf(result, blockConf));
        }
        for (ChainJson.ChainLinkConf linkConf : chainJson.getLinks()) {
            result.addLink(ChainLink.valueOf(linkConf));
        }
        if (chainJson.hasChainJsonFile()) {
            result.setCurrentDirectory(chainJson.getChainJsonFile().getParent());
        }
        result.setAllDefaultInputNames();
        result.setAllDefaultOutputNames();
        return result;
    }

    public Executor executionContext() {
        return executionContext;
    }

    public long contextId() {
        // Note: it will be another for result of cleanCopy!
        return contextId;
    }

    public String id() {
        return id;
    }

    public ExecutorProvider getExecutorProvider() {
        return executorProvider;
    }

    public boolean isAutogeneratedCategory() {
        return autogeneratedCategory;
    }

    public String category() {
        return category;
    }

    public boolean isAutogeneratedName() {
        return autogeneratedName;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Set<String> tags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean hasPlatformId() {
        return platformId != null;
    }

    public String platformId() {
        return platformId;
    }

    public String platformCategory() {
        return platformCategory;
    }

    public Path chainJsonPath() {
        return chainJsonPath;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public Chain setCurrentDirectory(Path currentDirectory) {
        this.currentDirectory = currentDirectory;
        return this;
    }

    public boolean isMultithreading() {
        return multithreading;
    }

    public Chain setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
        return this;
    }

    public boolean isExecuteAll() {
        return executeAll;
    }

    public Chain setExecuteAll(boolean executeAll) {
        this.executeAll = executeAll;
        return this;
    }

    public boolean isIgnoreExceptions() {
        return ignoreExceptions;
    }

    public Chain setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
        return this;
    }

    public boolean isTimingByExecutorsEnabled() {
        return timingByExecutorsEnabled;
    }

    public Chain setTimingByExecutorsEnabled(boolean timingByExecutorsEnabled) {
        this.timingByExecutorsEnabled = timingByExecutorsEnabled;
        return this;
    }

    public Object getCustomChainInformation() {
        return customChainInformation;
    }

    /**
     * Sets some additional data, that can be useful while executing the chain.
     * You can call this method, for example, at the stage of loading and analysing the chain,
     * if it requires essential time &mdash; and then use this information via {@link #getCustomChainInformation()}
     * at the stage of executing the chain.
     *
     * @param customChainInformation any data.
     * @return a reference to this object.
     */
    public Chain setCustomChainInformation(Object customChainInformation) {
        this.customChainInformation = customChainInformation;
        return this;
    }

    public Map<String, ChainBlock> getAllBlocks() {
        return Collections.unmodifiableMap(allBlocks);
    }

    public Map<String, ChainPort<?>> getAllPorts() {
        return Collections.unmodifiableMap(allPorts);
    }

    public ChainBlock getBlock(String blockId) {
        return allBlocks.get(blockId);
    }

    public void addBlock(ChainBlock block) {
        Objects.requireNonNull(block, "Null block");
        clearCache();
        if (allBlocks.putIfAbsent(block.id, block) != null) {
            throw new IllegalArgumentException("Duplicate block id: " + block.id);
        }
        for (ChainInputPort port : block.inputPorts.values()) {
            if (port.id != null && allPorts.putIfAbsent(port.id, port) != null) {
                throw new IllegalArgumentException("Duplicate input port id: " + port.id);
            }
        }
        for (ChainOutputPort port : block.outputPorts.values()) {
            if (port.id != null && allPorts.putIfAbsent(port.id, port) != null) {
                throw new IllegalArgumentException("Duplicate output port id: " + port.id);
            }
        }
    }

    public void addLink(ChainLink link) {
        Objects.requireNonNull(link, "Null link");
        final ChainPort<?> srcPort = allPorts.get(link.srcPortId);
        if (srcPort == null) {
            throw new IllegalArgumentException("Non-existing source port " + link.srcPortId);
        }
        if (!(srcPort instanceof ChainOutputPort)) {
            throw new IllegalArgumentException("Source port " + link.srcPortId + " is not an output port");
        }
        final ChainPort<?> destPort = allPorts.get(link.destPortId);
        if (destPort == null) {
            throw new IllegalArgumentException("Non-existing destination port " + link.destPortId);
        }
        if (!(destPort instanceof ChainInputPort)) {
            throw new IllegalArgumentException("Destination port " + link.destPortId + " is not an input port");
        }
        if (!allLinks.add(link)) {
            throw new IllegalArgumentException("Duplicate link: " + link);
        }
        if (srcPort.block.isExecutedAtRunTime() && destPort.block.isExecutedAtRunTime()) {
            // - ignore links from/to disabled or non-runtime blocks
            ((ChainOutputPort) srcPort).addConnection((ChainInputPort) destPort);
            ((ChainInputPort) destPort).addConnection((ChainOutputPort) srcPort);
        }
    }

    public void setAllDefaultInputNames() {
        int count = 0;
        for (ChainBlock block : getAllInputs()) {
            final String systemName = block.getSystemName();
            if (systemName != null) {
                block.setStandardInputOutputPortName(systemName);
            } else {
                ++count;
                block.setStandardInputOutputPortName(Executor.DEFAULT_INPUT_PORT + (count == 1 ? "" : "_" + count));
            }
        }
    }

    public void setAllDefaultOutputNames() {
        int count = 0;
        for (ChainBlock block : getAllOutputs()) {
            final String systemName = block.getSystemName();
            if (systemName != null) {
                block.setStandardInputOutputPortName(systemName);
            } else {
                ++count;
                block.setStandardInputOutputPortName(Executor.DEFAULT_OUTPUT_PORT + (count == 1 ? "" : "_" + count));
            }
        }
    }

    public Collection<ChainBlock> getAllInputs() {
        List<ChainBlock> allInputs = this.allInputs;
        if (allInputs == null) {
            this.allInputs = allInputs = allBlocks.values().stream().filter(ChainBlock::isStandardInput).toList();
        }
        return Collections.unmodifiableList(allInputs);
    }

    public Collection<ChainBlock> getAllOutputs() {
        List<ChainBlock> allOutputs = this.allOutputs;
        if (allOutputs == null) {
            this.allOutputs = allOutputs = allBlocks.values().stream().filter(ChainBlock::isStandardOutput).toList();
        }
        return Collections.unmodifiableList(allOutputs);
    }

    public Collection<ChainBlock> getAllData() {
        List<ChainBlock> allData = this.allData;
        if (allData == null) {
            this.allData = allData = allBlocks.values().stream().filter(ChainBlock::isStandardData).toList();
        }
        return Collections.unmodifiableList(allData);
    }

    public Collection<ChainBlock> getAllNecessaryOutputs(ExecutionBlock executor) {
        Objects.requireNonNull(executor, "Null executor");
        List<ChainBlock> result = new ArrayList<>();
        for (ChainBlock block : getAllOutputs()) {
            final String executorPortName = block.getStandardInputOutputName();
            if (executor.isOutputNecessary(executorPortName)) {
                result.add(block);
            }
        }
        return result;
    }

    public int numberOfBlocks() {
        return allBlocks.size();
    }

    public int numberOfReadyBlocks() {
        return (int) allBlocks.values().stream().filter(ChainBlock::isReady).count();
    }

    public void reinitializeAll() throws IllegalStateException {
        synchronized (chainLock) {
            this.contextId = CURRENT_CONTEXT_ID.getAndIncrement();
            checkRecursiveDependencies();
            allBlocks.values().forEach(block -> block.reinitialize(false));
        }
    }

    public Executor getCaller() {
        return caller;
    }

    public Chain setCaller(Executor caller) {
        synchronized (chainLock) {
            this.caller = caller;
            allBlocks.values().forEach(block -> block.setCaller(caller));
        }
        return this;
    }

    public Chain setTimingSettings(int numberOfAnalysedCalls, TimingStatistics.Settings settings) {
        synchronized (chainLock) {
            allBlocks.values().forEach(block -> block.setTimingSettings(numberOfAnalysedCalls, settings));
        }
        return this;
    }

    /**
     * Sets parameters of the chain, that exist in the argument; other parameters preserve their default values.
     *
     * @param parameters parameters to set.
     * @return a reference
     */
    public Chain setParameters(Parameters parameters) {
        Objects.requireNonNull(parameters, "Null parameters map");
        synchronized (chainLock) {
            for (ChainBlock block : getAllData()) {
                final String subChainParameterName = block.getStandardParameterName();
                if (subChainParameterName == null) {
                    continue;
                }
                final ChainInputPort inputPort = block.reqStandardDataPort();
                final Data data = inputPort.getData();
                if (!(data instanceof SScalar)) {
                    // - corresponds to ExecutorJson.setTo(Chain) logic
                    continue;
                }
                final String value = parameters.getString(subChainParameterName, null);
                // - should be called after checking for scalar type: for other types,
                // ExecutorJson.setTo(Chain) does not add parameters
                if (value != null) {
                    // - if null, let the parameter to have its default value
                    ((SScalar) data).setTo(value);
                }
            }
        }
        return this;
    }

    /**
     * Sets inputs of the chain, that exist in the argument; other inputs preserve their values.
     *
     * @param inputs input values to set.
     * @return a reference to this object.
     */
    public Chain setInputData(Map<String, Data> inputs) {
        setAllInputData(inputs, false);
        return this;
    }

    public Chain setAllInputData(Map<String, Data> inputs) {
        setAllInputData(inputs, true);
        return this;
    }

    public Map<String, Data> getOutputDataClone() {
        final Map<String, Data> result = new LinkedHashMap<>();
        for (ChainBlock block : getAllOutputs()) {
            final ChainOutputPort outputPort = block.reqStandardOutputPort();
            final String executorPortName = block.getStandardInputOutputName();
            result.put(executorPortName, outputPort.getData().clone());
            // - cloning data, because ports in the chain can be freed
        }
        return result;
    }

    public void readInputPortsFromExecutor(ExecutionBlock executor) {
        Objects.requireNonNull(executor, "Null executor");
        synchronized (chainLock) {
            for (ChainBlock block : getAllInputs()) {
                final ChainInputPort chainInputPort = block.reqStandardInputPort();
                final String executorPortName = block.getStandardInputOutputName();
                if (executor.hasInputPort(executorPortName)) {
                    final Data data = executor.getInputData(executorPortName, true);
                    chainInputPort.getData().setTo(data, true);
                    // - cloning data, because ports in the chain can be freed
                }
            }
        }
    }

    public void writeOutputPortsToExecutor(ExecutionBlock executor) {
        Objects.requireNonNull(executor, "Null executor");
        for (ChainBlock block : getAllOutputs()) {
            final ChainOutputPort chainOutputPort = block.reqStandardOutputPort();
            final String executorPortName = block.getStandardInputOutputName();
            final Data data = chainOutputPort.getData();
            executor.addOutputData(executorPortName, data.type());
            executor.getData(executorPortName).setTo(data, true);
            // - cloning data, because ports in the chain can be freed
        }
    }

    public static void exchangeOutputDataWithExecutor(Map<String, Data> outputs, ExecutionBlock executor) {
        Objects.requireNonNull(outputs, "Null outputs");
        Objects.requireNonNull(executor, "Null executor");
        for (Map.Entry<String, Data> entry : outputs.entrySet()) {
            executor.getData(entry.getKey()).exchange(entry.getValue());
        }
    }

    public void checkRecursiveDependencies() {
        synchronized (chainLock) {
            prepareExecution(true);
            allBlocks.values().forEach(ChainBlock::checkRecursiveDependencies);
        }
    }

    public void execute() {
        executeNecessary(null);
    }

    public void executeNecessary(ExecutionBlock executor) {
        synchronized (chainLock) {
            prepareExecution(true);
            final Collection<ChainBlock> all = allBlocks.values();
            all.forEach(ChainBlock::reset);
            for (; ; ) {
                this.needToRepeat = false;
                Collection<ChainBlock> blocksToExecute = executeAll ? all :
                        executor == null || executor.isAllOutputsNecessary() ?
                                // This executor (like SubChain from the extensions) probably don't know
                                // its output ports yet: they will be added dynamically by this sub-chain.
                                // So, if it wants to receive ALL results, we should use ALL outputs of this chain.
                                getAllOutputs() :
                                getAllNecessaryOutputs(executor);
                ChainBlock.executeWithAllDependentInputs(blocksToExecute, multithreading);
                if (!this.needToRepeat) {
                    break;
                }
                prepareExecution(false);
                // - but not calling reset() again!
            }
        }
    }

    public void freeData() {
        synchronized (chainLock) {
            allBlocks.values().forEach(ChainBlock::freeData);
        }
    }

    public void freeResources() {
        synchronized (chainLock) {
            allBlocks.values().forEach(ChainBlock::freeResources);
        }
    }

    public Executor toExecutor() {
        if (executorProvider == null) {
            throw new IllegalStateException("Cannot convert chain with ID " + id
                    + " to executor: executor provider is not set");
        }
        ExecutionBlock result;
        try {
            result = executorProvider.newExecutor(id());
            // - we suppose that someone has registered executor, which execute this chain
        } catch (ClassNotFoundException | ExecutorNotFoundException e) {
            throw new IllegalStateException("Chain with ID " + id + " was not successfully registered", e);
        }
        if (!(result instanceof Executor)) {
            throw new IllegalStateException("Chain  with ID " + id + " is executed by some non-standard way: "
                + "its executor is not an instance of Executor class");
        }
        return (Executor) result;
    }

    public String timingInfo() {
        final StringBuilder sb = new StringBuilder(String.format(
                "  Detailed timing of chain%s (id=%s, %d blocks)%n"
                        + "Sorted by time (only summary time for each block):%n",
                name == null ? "" : " " + name, id, numberOfBlocks()));
        final List<ChainBlock> all = new ArrayList<>(allBlocks.values());
        all.forEach(ChainBlock::analyseTiming);
        final List<ChainBlock> withTiming = all.stream().filter(ChainBlock::hasTiming).collect(Collectors.toList());
        all.sort(Comparator.comparingInt(ChainBlock::executionOrder));
        withTiming.sort(Comparator.comparingDouble(Chain::averageTime).reversed());
        final double sum = withTiming.stream().mapToDouble(
                block -> block.timing().summaryTimeOfLastAnalysedCalls()).sum();
        for (ChainBlock block : withTiming) {
            sb.append(String.format("    %s%n", block.simpleTimingInfo(sum)));
        }
        sb.append(String.format("  Detailed, sorted by execution order:%n"));
        for (ChainBlock block : all) {
            sb.append(String.format("    %s%n", block.timingInfo()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Chain"
                + (name == null ? "" : " " + name + " ")
                + "{\n  id=" + id + "\n  blocks=[\n");
        for (ChainBlock block : allBlocks.values()) {
            sb.append("    ").append(block).append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    @Override
    public void close() {
        freeResources();
    }

    private void setAllInputData(Map<String, Data> inputs, boolean requireToSetAllInputs) {
        Objects.requireNonNull(inputs, "Null inputs");
        synchronized (chainLock) {
            for (ChainBlock block : getAllInputs()) {
                final ChainInputPort inputPort = block.reqStandardInputPort();
                final String executorPortName = block.getStandardInputOutputName();
                final Data input = inputs.get(executorPortName);
                if (input != null) {
                    if (input.type() != inputPort.getDataType()) {
                        throw new IllegalArgumentException("Cannot assign " + input.getClass().getSimpleName()
                                + " to input port \"" + executorPortName + "\" of the block with ID \""
                                + block.getId() + "\": type mismatch (" + input.type()
                                + " instead of expected " + inputPort.getDataType());
                    }
                    inputPort.getData().setTo(input, true);
                    // - cloning data, because ports in the chain can be freed
                } else if (requireToSetAllInputs) {
                    throw new IllegalArgumentException("No data for input block '" + executorPortName
                            + "': " + block);
                }
            }
        }
    }

    private void prepareExecution(boolean firstIteration) {
        synchronized (chainLock) {
            executionIndex.set(0);
            ChainBlock.prepareExecution(allBlocks.values());
        }
    }

    private void clearCache() {
        this.allData = null;
        this.allInputs = null;
        this.allOutputs = null;
    }

    private static double averageTime(ChainBlock chainBlock) {
        return chainBlock.timing().summary().averageTimeOfLastAnalysedCalls();
    }

    void debugCheck(String where) {
        synchronized (blocksInteractionLock) {
            final ChainBlock block = getBlock("14fca6f5-4240-4b31-b27e-bb7e08029dda");
            // - ID of some block which we want to control
            if (block != null) {
                final ChainOutputPort output =
                        block.outputPorts.get(new ChainPortKey(ChainPortType.OUTPUT_PORT, "output"));
                if (block.isReady() && output.getCountOfConnectedInputs() > 0 && !output.data.isInitialized()) {
                    System.out.println(where + " !!!!!!!!!! PROBLEM! " + contextId);
                }
            }
        }
    }
}
