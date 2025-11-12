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

package net.algart.executors.api.chains.core;

import jakarta.json.JsonValue;
import net.algart.arrays.Arrays;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.*;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledPlatformsForTechnology;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.core.UseChainSettings;
import net.algart.executors.api.settings.core.UseSettings;
import net.algart.executors.api.system.*;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class UseChain extends FileOperation {
    public static final String CHAIN_LANGUAGE_NAME = "chain";
    public static final String ADDITIONAL_STANDARD_CHAINS_PATH = Arrays.SystemSettings.getStringProperty(
            "net.algart.executors.api.chains.path", null);

    public static final String DO_ACTION_NAME = "_sch___doAction";
    public static final String DO_ACTION_CAPTION = "Do actions";
    public static final String DO_ACTION_DESCRIPTION = "If set, function is executed normally. "
            + "If cleared, this function just copies all input data "
            + "to the output ports with the same names and types (if they exist) "
            + "and does not anything else.";
    public static final String LOG_TIMING_NAME = "_sch___logTiming";
    public static final String LOG_TIMING_CAPTION = "Log timing";
    public static final String LOG_TIMING_DESCRIPTION = "If set, function analyses and prints "
            + "timing statistics for all executed blocks on the log level, specified below.\n"
            + "Note: actual collecting timing statistics does not depend on this flag. "
            + "So, you may disable this flag for some executions and enable it only sometimes, for example, "
            + "at the end of some loop: statistics will be collected always, but printed only when you need.";
    public static final boolean LOG_TIMING_DEFAULT = true;
    public static final String TIMING_LOG_LEVEL_NAME = "_sch___timingLogLevel";
    public static final String TIMING_LOG_LEVEL_CAPTION = "Timing logging level";
    public static final String TIMING_LOG_LEVEL_DESCRIPTION = "If the previous flag is set, function prints "
            + "timing statistics on this log level.\nIf this parameter is specified via an input port, "
            + "it may be a string, equal to one of predefined names in System.Logger.Level "
            + "(\"INFO\", \"DEBUG\", etc), or an integer value, returned by Level.intValue() method.\n"
            + "Note: if the current system logging level is greater this value, logging "
            + "is not performed and timing statistics is not collected.";
    public static final String TIMING_LOG_LEVEL_DEFAULT = System.Logger.Level.DEBUG.getName();
    public static final String TIMING_NUMBER_OF_CALLS_NAME = "_sch___timingNumberOfCalls";
    public static final String TIMING_NUMBER_OF_CALLS_CAPTION = "Number N of calls for timing";
    public static final String TIMING_NUMBER_OF_CALLS_DESCRIPTION = "This function collects and analyses timing "
            + "statistics for execution time for the last N calls of every block.\n"
            + "Must be non-negative. Zero value N=0 disables timing.\n"
            + "Note: changing this value leads to resetting the statistics.";
    public static final int TIMING_NUMBER_OF_CALLS_DEFAULT = 10;
    public static final String TIMING_NUMBER_OF_PERCENTILES_NAME = "_sch___timingNumberOfPercentiles";
    public static final String TIMING_NUMBER_OF_PERCENTILES_CAPTION = "Number M of percentiles for timing";
    public static final String TIMING_NUMBER_OF_PERCENTILES_DESCRIPTION = "If timing is performed, "
            + "this function finds M percentiles from all N times of execution of every block, "
            + "including minimum and maximum. For example, M=5 means "
            + "finding 5 percentiles 0% (minimum), 25%, 50% (median), 75%, 100% (maximum). "
            + "M=2 means finding only minimum and maximum. M=1 means finding only 50% percentile (median).\n"
            + "Minimal value M=0, then percentiles are not analysed at all.";
    public static final int TIMING_NUMBER_OF_PERCENTILES_DEFAULT = 5;
    public static final String VISIBLE_RESULT_PARAMETER_NAME = "_sch___visibleResult";
    public static final String VISIBLE_RESULT_PARAMETER_CAPTION = "Visible result";

    static final String RECURSIVE_LOADING_BLOCKED_MESSAGE = "[recursive loading chain blocked]";

    private static final InstalledPlatformsForTechnology CHAIN_PLATFORMS =
            InstalledPlatformsForTechnology.of(ChainSpecification.CHAIN_TECHNOLOGY);
    private static final DefaultExecutorLoader<Chain> CHAIN_LOADER =
            new DefaultExecutorLoader<>("chains loader");

    static {
        globalLoaders().register(CHAIN_LOADER);
    }

    private static final Set<String> NOW_USED_CHAIN_IDS = Collections.synchronizedSet(new HashSet<>());

    private String chainSpecification = "";
    private boolean executeIsolatedLoadingTimeFunctions = true;
    private boolean overrideBehaviour = false;
    private boolean multithreading = false;
    private boolean executeAll = false;

    private volatile ExecutorFactory executorFactory = null;
    // - Note: this factory is not shared with chains called from blocks of this chain.
    // This could be implemented in executeLoadingTimeBlocksWithoutInputs, but there is no serious point
    // in doing so: it is a minor optimization, and it can lead to potential problems with freeing resources
    // (cache inside the executor factory).

    private ExecutorSpecification chainExecutorSpecification = null;

    final AtomicInteger loadedChainsCount = new AtomicInteger(0);
    // - for logging needs

    public UseChain() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseChain getInstance(String sessionId) {
        return setSession(new UseChain(), sessionId);
    }

    public static UseChain getSharedInstance() {
        return setShared(new UseChain());
    }

    public static DefaultExecutorLoader<Chain> chainLoader() {
        return CHAIN_LOADER;
    }

    @Override
    public UseChain setFile(String file) {
        super.setFile(file);
        return this;
    }

    public String getChainSpecification() {
        return chainSpecification;
    }

    public UseChain setChainSpecification(String chainSpecification) {
        this.chainSpecification = nonNull(chainSpecification);
        return this;
    }

    public boolean executeIsolatedLoadingTimeFunctions() {
        return executeIsolatedLoadingTimeFunctions;
    }

    public UseChain setExecuteIsolatedLoadingTimeFunctions(boolean executeIsolatedLoadingTimeFunctions) {
        this.executeIsolatedLoadingTimeFunctions = executeIsolatedLoadingTimeFunctions;
        return this;
    }

    public boolean isOverrideBehaviour() {
        return overrideBehaviour;
    }

    public UseChain setOverrideBehaviour(boolean overrideBehaviour) {
        this.overrideBehaviour = overrideBehaviour;
        return this;
    }

    public boolean isMultithreading() {
        return multithreading;
    }

    public UseChain setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
        return this;
    }

    public boolean isExecuteAll() {
        return executeAll;
    }

    public UseChain setExecuteAll(boolean executeAll) {
        this.executeAll = executeAll;
        return this;
    }

    public ExecutorSpecification chainExecutorSpecification() {
        return chainExecutorSpecification;
    }

    public static ChainExecutor newSharedExecutor(Path file) throws IOException {
        return newSharedExecutor(file, CreateMode.REQUEST_ALL);
    }

    public static ChainExecutor newSharedExecutor(Path file, CreateMode createMode) throws IOException {
        return newSharedExecutor(ChainSpecification.read(file), createMode);
    }

    public static ChainExecutor newSharedExecutor(ChainSpecification specification, CreateMode createMode) {
        return getSharedInstance().newExecutor(specification, createMode);
    }

    public ChainExecutor newExecutor(Path file, CreateMode createMode) throws IOException {
        return newExecutor(ChainSpecification.read(file), createMode);
    }

    public ChainExecutor newExecutor(ChainSpecification specification, CreateMode createMode) {
        //noinspection resource
        return use(specification).newExecutor(createMode);
    }

    @Override
    public void process() {
        if (hasFile()) {
            try {
                useSeveralPaths(completeSeveralFilePaths());
            } catch (IOException e) {
                throw new IOError(e);
            }
        } else {
            final String chainSpecification = this.chainSpecification.trim();
            if (chainSpecification.isEmpty()) {
                throw new IllegalArgumentException("One of arguments \"Chain JSON file/folder\" "
                        + "or \"Chain specification\" must be non-empty");
            }
            useContent(chainSpecification);
        }
    }

    public ExecutorFactory executorFactory() {
        final String sessionId = getSessionId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot create executor factory: session ID was not set");
        }
        var executorFactory = this.executorFactory;
        if (executorFactory == null) {
            this.executorFactory = executorFactory = globalLoaders().newFactory(sessionId);
        }
        return executorFactory;
    }

    public void useSeveralPaths(List<Path> chainSpecificationPaths) throws IOException {
        Objects.requireNonNull(chainSpecificationPaths, "Null chains paths");
        final StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : chainSpecificationPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path chainSpecificationPath) throws IOException {
        usePath(chainSpecificationPath, null, null);
    }

    public int usePath(
            Path path,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(path, "Null chain specification path");
        // - for empty path string, useSeveralPaths will be called with an empty list, but list elements are never null
        if (skipIfMissingOrThrow(path, false,
                () -> "Chain file or chains folder " + path + " does not exist")) {
            return 0;
        }
        final List<ChainSpecification> chainSpecifications;
        if (Files.isDirectory(path)) {
            chainSpecifications = ChainSpecification.readAllIfValid(path, true);
        } else {
            chainSpecifications = Collections.singletonList(ChainSpecification.read(path));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        use(chainSpecifications, platform, report);
        return chainSpecifications.size();
    }

    public void useContent(String chainSpecificationContent) {
        final ChainSpecification chainSpecification = ChainSpecification.of(chainSpecificationContent);
        long t1 = infoTime();
        final Optional<Chain> chain = useIfNonRecursive(chainSpecification);
        long t2 = infoTime();
        LOG.log(chain.isPresent() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO,
                () -> String.format(Locale.US, "Chain \"%s\"%s %screated from text parameter in %.3f ms",
                        chain.isEmpty() ? RECURSIVE_LOADING_BLOCKED_MESSAGE : chain.get().name(),
                        additionalChainInformation(chain.orElse(null)),
                        chain.isPresent() ? "" : "not ",
                        (t2 - t1) * 1e-6));
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo("Chain:\nCategory: '" + chainSpecification.chainCategory()
                    + "'\nName: '" + chainSpecification.chainName() + "'");
        }
    }

    public void use(List<ChainSpecification> chainSpecifications, StringBuilder report) {
        use(chainSpecifications, null, report);
    }

    public Chain use(ChainSpecification chainSpecification) {
        Optional<Chain> result = useIfNonRecursive(chainSpecification);
        if (result.isEmpty()) {
            throw new IllegalStateException("Recursive using of the chain is not allowed here");
        }
        return result.get();
    }

    public Optional<Chain> useIfNonRecursive(ChainSpecification chainSpecification) {
        Objects.requireNonNull(chainSpecification, "Null chainSpecification");
        final String chainId = chainSpecification.getExecutor().getId();
        chainExecutorSpecification = null;
        synchronized (NOW_USED_CHAIN_IDS) {
            if (NOW_USED_CHAIN_IDS.contains(chainId)) {
                // - Avoid infinite recursion.
                // Note that we cannot do this via fields of this class:
                // CHAIN_LOADER.registerWorker method always creates new Chain instances
                // with new clean blocks and their executors.
                return Optional.empty();
            }
            NOW_USED_CHAIN_IDS.add(chainId);
        }
        try {
            return Optional.of(register(chainSpecification));
        } finally {
            NOW_USED_CHAIN_IDS.remove(chainId);
        }
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("subChainJsonContent") ? "chainSpecification" : name;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseChain useChain = UseChain.getSharedInstance();
        for (ExtensionSpecification.Platform platform : CHAIN_PLATFORMS.installedPlatforms()) {
            if (platform.hasSpecifications()) {
                useInstalledFolder(useChain, platform.specificationsFolder(), platform,
                        "installed chain specifications");
            }
        }
        if (ADDITIONAL_STANDARD_CHAINS_PATH != null) {
            for (String folder : UseChain.ADDITIONAL_STANDARD_CHAINS_PATH.split("[\\;]")) {
                useInstalledFolder(useChain, Paths.get(folder), null,
                        "additional chain specifications");
            }
        }
    }

    public static ControlSpecification createLogTimingControl(String parameterName) {
        ControlSpecification result = new ControlSpecification();
        result.setName(parameterName);
        result.setCaption(LOG_TIMING_CAPTION);
        result.setDescription(LOG_TIMING_DESCRIPTION);
        result.setValueType(ParameterValueType.BOOLEAN);
        result.setDefaultJsonValue(JsonValue.TRUE);
        result.setAdvanced(true);
        return result;
    }

    public static ControlSpecification createTimingLogLevelControl(String parameterName) {
        ControlSpecification result = new ControlSpecification();
        result.setName(parameterName);
        result.setCaption(TIMING_LOG_LEVEL_CAPTION);
        result.setDescription(TIMING_LOG_LEVEL_DESCRIPTION);
        result.setValueType(ParameterValueType.STRING);
        result.setDefaultStringValue(TIMING_LOG_LEVEL_DEFAULT);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(List.of(
                new ControlSpecification.EnumItem(System.Logger.Level.WARNING.getName()),
                new ControlSpecification.EnumItem(System.Logger.Level.INFO.getName()),
                new ControlSpecification.EnumItem(System.Logger.Level.DEBUG.getName()),
                new ControlSpecification.EnumItem(System.Logger.Level.TRACE.getName())));
        result.setAdvanced(true);
        return result;
    }

    public static ControlSpecification createTimingNumberOfCallsControl(String parameterName) {
        ControlSpecification result = new ControlSpecification();
        result.setName(parameterName);
        result.setCaption(TIMING_NUMBER_OF_CALLS_CAPTION);
        result.setDescription(TIMING_NUMBER_OF_CALLS_DESCRIPTION);
        result.setValueType(ParameterValueType.INT);
        result.setDefaultJsonValue(Jsons.toJsonIntValue(TIMING_NUMBER_OF_CALLS_DEFAULT));
        result.setEditionType(ControlEditionType.VALUE);
        result.setAdvanced(true);
        return result;
    }

    public static ControlSpecification createTimingNumberOfPercentilesControl(String parameterName) {
        ControlSpecification result = new ControlSpecification();
        result.setName(parameterName);
        result.setCaption(TIMING_NUMBER_OF_PERCENTILES_CAPTION);
        result.setDescription(TIMING_NUMBER_OF_PERCENTILES_DESCRIPTION);
        result.setValueType(ParameterValueType.INT);
        result.setDefaultJsonValue(Jsons.toJsonIntValue(TIMING_NUMBER_OF_PERCENTILES_DEFAULT));
        result.setEditionType(ControlEditionType.VALUE);
        result.setAdvanced(true);
        return result;
    }

    public static ControlSpecification createVisibleResultControl(
            ExecutorSpecification specification,
            String parameterName) {
        String firstEnumValue = null;
        final List<ControlSpecification.EnumItem> items = new ArrayList<>();
        for (PortSpecification portSpecification : specification.getOutputPorts().values()) {
            final String executorPortName = portSpecification.getName();
            if (firstEnumValue == null && !executorPortName.equals(SETTINGS)) {
                firstEnumValue = executorPortName;
            }
            items.add(new ControlSpecification.EnumItem(executorPortName));
        }
        if (items.size() < 2) {
            // - no sense to add visible result control
            return null;
        }
        if (firstEnumValue == null) {
            // - there is only SETTINGS port
            firstEnumValue = specification.getOutputPorts().values().iterator().next().getName();
        }
        ControlSpecification result = new ControlSpecification();
        result.setName(parameterName);
        result.setCaption(VISIBLE_RESULT_PARAMETER_CAPTION);
        result.setValueType(ParameterValueType.ENUM_STRING);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(items);
        result.setDefaultStringValue(firstEnumValue);
        return result;
    }

    public static void addSettingsPorts(ExecutorSpecification result) {
        result.addFirstInputPort(new PortSpecification()
                .setName(SETTINGS)
                .setValueType(DataType.SCALAR));
        result.addFirstOutputPort(new PortSpecification()
                .setName(SETTINGS)
                .setHint("Actually used settings (JSON)")
                .setAdvanced(true)
                .setValueType(DataType.SCALAR));
    }

    private void use(
            List<ChainSpecification> chainSpecifications,
            ExtensionSpecification.Platform platform,
            StringBuilder report) {
        ChainSpecification.checkIdDifference(chainSpecifications);
        for (int i = 0, n = chainSpecifications.size(); i < n; i++) {
            ChainSpecification chainSpecification = chainSpecifications.get(i);
            long t1 = infoTime();
            if (platform != null) {
                chainSpecification.addTags(platform.getTags());
                chainSpecification.setPlatformId(platform.getId());
                chainSpecification.setPlatformCategory(platform.getCategory());
            }
            final Optional<Chain> chain;
            try {
                chain = useIfNonRecursive(chainSpecification);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ChainRunningException("Cannot load the chain " + chainSpecification.getSpecificationFile(),
                        e);
            }
            long t2 = infoTime();
            final int index = i;
            // Note: recursive usage of chains is a rare situation,
            // but NOT an error, so we use only the INFO level here.
            LOG.log(chain.isPresent() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO,
                    () -> String.format(Locale.US, "Chain %s\"%s\"%s %sloaded from %s in %.3f ms",
                            n > 1 ? (index + 1) + "/" + n + " " : "",
                            chainSpecification.getExecutor().getName(),
                            additionalChainInformation(chain.orElse(null)),
                            chain.isPresent() ? "" : "not ",
                            chainSpecification.getSpecificationFile().toAbsolutePath(),
                            (t2 - t1) * 1e-6));
        }
        if (report != null) {
            for (ChainSpecification specification : chainSpecifications) {
                final Path file = specification.getSpecificationFile();
                final String message = file != null ? file.toString() : specification.canonicalName() + " (no file)";
                report.append(message).append("\n");
            }
        }
    }

    private Chain register(ChainSpecification chainSpecification) {
        Objects.requireNonNull(chainSpecification, "Null chainSpecification");
        if (getSessionId() == null) {
            throw new IllegalStateException("Cannot register new chain: session ID was not set");
        }
        final ExecutorFactory executorFactory = executorFactory();
        Chain chain = Chain.of(this, executorFactory, chainSpecification);
        if (chain.getCurrentDirectory() == null) {
            // - If the chain was loaded not from a file, but from the executor text parameter,
            // chainSpecification does not contain information about the current folder;
            // in this case, we suppose that the current folder is equal
            // to the current folder of this UseChain executor.
            chain.setCurrentDirectory(this.getCurrentDirectory());
        }
        if (overrideBehaviour) {
            chain.setMultithreading(multithreading);
            chain.setExecuteAll(executeAll);
        }
        CHAIN_LOADER.registerWorker(
                executorFactory.sessionId(),
                buildChainSpecificationAndExecuteLoadingTimeWithoutInputs(chain),
                chain);
        loadedChainsCount.incrementAndGet();
        return chain;
    }

    private ExecutorSpecification buildChainSpecificationAndExecuteLoadingTimeWithoutInputs(Chain chain) {
        Objects.requireNonNull(chain, "Null chain");
        final ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(new InterpretChain());
        // - adds JavaConf and (maybe) parameters with setters
        result.setTo(chain);
        result.setSourceInfo(
                null,
                chain.chainSpecificationPath()).setLanguageName(CHAIN_LANGUAGE_NAME);
        if (chain.hasPlatformId()) {
            result.setPlatformId(chain.platformId());
        }
        final String category = result.getCategory();
        if (category != null) {
            result.setCategory(
                    ExecutorSpecification.correctDynamicCategory(category, !chain.isAutogeneratedCategory()));
            // Note: if the chain category is explicitly specified (!isAutogeneratedCategory),
            // we disable correcting it (this is probably a usual function, like functions in other languages).
            // Unlike this, UseSettings always adds DYNAMIC_CATEGORY_PREFIX
            result.updateCategoryPrefix(chain.platformCategory());
        }
        executeLoadingTimeBlocksWithoutInputs(chain, executeIsolatedLoadingTimeFunctions);
        result.addControl(new ControlSpecification()
                .setName(DO_ACTION_NAME)
                .setCaption(DO_ACTION_CAPTION)
                .setDescription(DO_ACTION_DESCRIPTION)
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.TRUE)
                .setAdvanced(false));
        result.addControl(createLogTimingControl(LOG_TIMING_NAME));
        result.addControl(createTimingLogLevelControl(TIMING_LOG_LEVEL_NAME));
        result.addControl(createTimingNumberOfCallsControl(TIMING_NUMBER_OF_CALLS_NAME));
        result.addControl(createTimingNumberOfPercentilesControl(TIMING_NUMBER_OF_PERCENTILES_NAME));
        addChainSettings(result, chain);
        final ControlSpecification visibleResult = createVisibleResultControl(
                result, VISIBLE_RESULT_PARAMETER_NAME);
        if (visibleResult != null) {
            result.addControl(visibleResult);
        }
        return chainExecutorSpecification = result;
    }

    private void executeLoadingTimeBlocksWithoutInputs(
            Chain chain,
            boolean executeIsolatedLoadingTimeFunctions) {
        for (ChainBlock block : chain.getAllBlocks().values()) {
            if (block.isExecutedAtLoadingTime()) {
                block.reinitialize(true);
                if (block.numberOfConnectedInputPorts() == 0) {
                    // Note: we do not execute loading-time chains,
                    // only isolated blocks without input ports.
                    // It is enough for most needs.
                    final ExecutionBlock executor = block.getExecutor();
                    if (executeIsolatedLoadingTimeFunctions || executor instanceof UseSettings) {
                        // Note: UseSettings and UseChainSettings are executed always
                        try {
                            executor.reset();
                            executor.execute(ExecutionMode.SILENT);
                        } catch (ChainLoadingException e) {
                            throw e;
                        } catch (RuntimeException | AssertionError | IOError e) {
                            throw new ChainLoadingException(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private static void useInstalledFolder(
            UseChain useChain,
            Path folder,
            ExtensionSpecification.Platform platform,
            String name) throws IOException {
        final long t1 = System.nanoTime();
        final int n = useChain.usePath(folder, platform, null);
        final long t2 = System.nanoTime();
        logInfo(() -> String.format(Locale.US,
                "Loading %d %s from %s: %.3f ms",
                n, name, folder, (t2 - t1) * 1e-6));
    }

    private static String additionalChainInformation(Chain chain) {
        if (chain == null) {
            return " " + RECURSIVE_LOADING_BLOCKED_MESSAGE;
        }
        return !chain.hasSettings() ? "" : ", " + chain.getSettingsBuilder();
    }

    private static void addChainSettings(ExecutorSpecification result, Chain chain) {
        final ChainBlock useChainSettingsBlock = findUseChainSettings(chain);
        if (useChainSettingsBlock == null) {
            UseSettings.addChainControlsAndPorts(result, null);
            return;
        }
        final UseChainSettings useChainSettings = (UseChainSettings) useChainSettingsBlock.getExecutor();
        final SettingsBuilder mainSettingsBuilder = useChainSettings.settingsBuilder();
        // - mainSettings was already executed in executeLoadingTimeBlocksWithoutInputs(chain)
        chain.assignSettings(mainSettingsBuilder);
        UseSettings.addChainControlsAndPorts(result, mainSettingsBuilder);
        result.setSettings(mainSettingsBuilder.specification());
        result.createOptionsIfAbsent().createServiceIfAbsent().setSettingsId(mainSettingsBuilder.id());
        addSettingsPorts(result);
    }


    private static ChainBlock findUseChainSettings(Chain chain) {
        // The current version uses the simplest algorithm for searching main settings: the first UseChainSettings.
        for (ChainBlock block : chain.getAllBlocks().values()) {
            if (block.isExecutedAtLoadingTime()) {
                if (block.getExecutor() instanceof UseChainSettings) {
                    // - this means: this executor is the main settings combiner
                    return block;
                }
            }
        }
        return null;
    }
}
