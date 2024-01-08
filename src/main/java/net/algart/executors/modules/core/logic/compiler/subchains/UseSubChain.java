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

package net.algart.executors.modules.core.logic.compiler.subchains;

import jakarta.json.JsonValue;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.SimpleExecutionBlockLoader;
import net.algart.executors.api.SystemEnvironment;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.model.*;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.logic.compiler.settings.UseChainSettings;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.CombineSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.subchains.interpreters.InterpretSubChain;
import net.algart.json.Jsons;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class UseSubChain extends FileOperation {
    public static final String SUB_CHAIN_LANGUAGE_NAME = "sub-chain";
    public static final String ADDITIONAL_STANDARD_SUBCHAINS_PATH = SystemEnvironment.getStringProperty(
            "net.algart.executors.logic.compiler.subchains.path");

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

    static final String RECURSIVE_LOADING_BLOCKED_MESSAGE = "[recursive loading blocked]";

    private static final InstalledPlatformsForTechnology SUB_CHAIN_PLATFORMS =
            InstalledPlatformsForTechnology.getInstance(ChainJson.CHAIN_TECHNOLOGY);
    private static final SimpleExecutionBlockLoader<Chain> SUB_CHAIN_LOADER =
            new SimpleExecutionBlockLoader<>("sub-chains loader");

    static {
        ExecutionBlock.registerExecutionBlockLoader(SUB_CHAIN_LOADER);
    }

    private static final Set<String> NOW_USED_CHAIN_IDS = Collections.synchronizedSet(new HashSet<>());

    private String subChainJsonContent = "";
    private boolean fileExistenceRequired = true;
    private boolean executeIsolatedLoadingTimeFunctions = true;
    private boolean overrideBehaviour = false;
    private boolean multithreading = false;
    private boolean executeAll = false;

    private ExecutorJson chainExecutorModel = null;

    final AtomicInteger loadedChainsCount = new AtomicInteger(0);
    // - for logging needs

    public UseSubChain() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseSubChain getInstance() {
        return new UseSubChain();
    }

    public static UseSubChain getShared() {
        final UseSubChain result = new UseSubChain();
        result.setSessionId(GLOBAL_SHARED_SESSION_ID);
        return result;
    }

    public static SimpleExecutionBlockLoader<Chain> subChainLoader() {
        return SUB_CHAIN_LOADER;
    }

    @Override
    public UseSubChain setFile(String file) {
        super.setFile(file);
        return this;
    }

    public String getSubChainJsonContent() {
        return subChainJsonContent;
    }

    public UseSubChain setSubChainJsonContent(String subChainJsonContent) {
        this.subChainJsonContent = nonNull(subChainJsonContent);
        return this;
    }

    public boolean isFileExistenceRequired() {
        return fileExistenceRequired;
    }

    public UseSubChain setFileExistenceRequired(boolean fileExistenceRequired) {
        this.fileExistenceRequired = fileExistenceRequired;
        return this;
    }

    public boolean executeIsolatedLoadingTimeFunctions() {
        return executeIsolatedLoadingTimeFunctions;
    }

    public UseSubChain setExecuteIsolatedLoadingTimeFunctions(boolean executeIsolatedLoadingTimeFunctions) {
        this.executeIsolatedLoadingTimeFunctions = executeIsolatedLoadingTimeFunctions;
        return this;
    }

    public boolean isOverrideBehaviour() {
        return overrideBehaviour;
    }

    public UseSubChain setOverrideBehaviour(boolean overrideBehaviour) {
        this.overrideBehaviour = overrideBehaviour;
        return this;
    }

    public boolean isMultithreading() {
        return multithreading;
    }

    public UseSubChain setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
        return this;
    }

    public boolean isExecuteAll() {
        return executeAll;
    }

    public UseSubChain setExecuteAll(boolean executeAll) {
        this.executeAll = executeAll;
        return this;
    }

    public ExecutorJson chainExecutorModel() {
        return chainExecutorModel;
    }

    @Override
    public void process() {
        if (!this.getFile().trim().isEmpty()) {
            try {
                useSeveralPaths(completeSeveralFilePaths());
            } catch (IOException e) {
                throw new IOError(e);
            }
        } else {
            final String subChainJsonContent = this.subChainJsonContent.trim();
            if (subChainJsonContent.isEmpty()) {
                throw new IllegalArgumentException("One of arguments \"Sub-chain JSON file/folder\" "
                        + "or \"Sub-chain JSON content\" must be non-empty");
            }
            useContent(subChainJsonContent);
        }
    }

    public void useSeveralPaths(List<Path> chainJsonPaths) throws IOException {
        Objects.requireNonNull(chainJsonPaths, "Null chains paths");
        final StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : chainJsonPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path chainJsonPath) throws IOException {
        usePath(chainJsonPath, null, null);
    }

    public void usePath(
            Path chainJsonPath,
            ExtensionJson.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(chainJsonPath, "Null chains path");
        final List<ChainJson> chainJsons;
        if (!Files.exists(chainJsonPath)) {
            if (fileExistenceRequired) {
                throw new FileNotFoundException("Sub-chain file or sub-chains folder " + chainJsonPath
                        + " does not exist");
            } else {
                return;
            }
        }
        if (Files.isDirectory(chainJsonPath)) {
            chainJsons = ChainJson.readAllIfValid(chainJsonPath, true);
        } else {
            chainJsons = Collections.singletonList(ChainJson.read(chainJsonPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        use(chainJsons, platform, report);
    }

    public void useContent(String chainJsonContent) {
        final ChainJson chainJson = ChainJson.valueOf(chainJsonContent);
        long t1 = infoTime();
        final Optional<Chain> chain = useIfNonRecursive(chainJson);
        long t2 = infoTime();
        LOG.log(chain.isPresent() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO,
                () -> String.format(Locale.US, "Sub-chain \"%s\"%s %screated from text parameter in %.3f ms",
                        chain.isEmpty() ? RECURSIVE_LOADING_BLOCKED_MESSAGE : chain.get().name(),
                        additionalChainInformation(chain),
                        chain.isPresent() ? "" : "not ",
                        (t2 - t1) * 1e-6));
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo("Sub-chain:\nCategory: '" + chainJson.chainCategory()
                    + "'\nName: '" + chainJson.chainName() + "'");
        }
    }

    public void use(List<ChainJson> chainJsons, StringBuilder report) {
        use(chainJsons, null, report);
    }

    public Chain use(ChainJson chainJson) {
        Optional<Chain> result = useIfNonRecursive(chainJson);
        if (result.isEmpty()) {
            throw new IllegalStateException("Recursive using of the chain is not allowed here");
        }
        return result.get();
    }

    public Optional<Chain> useIfNonRecursive(ChainJson chainJson) {
        Objects.requireNonNull(chainJson, "Null chain JSON model");
        final String chainId = chainJson.getExecutor().getId();
        chainExecutorModel = null;
        synchronized (NOW_USED_CHAIN_IDS) {
            if (NOW_USED_CHAIN_IDS.contains(chainId)) {
                // - Avoid infinite recursion.
                // Note that we cannot do this via fields of this class:
                // SUB_CHAIN_LOADER.registerWorker method always creates new Chain instances
                // with new clean blocks and their executors.
                return Optional.empty();
            }
            NOW_USED_CHAIN_IDS.add(chainId);
        }
        try {
            return Optional.of(register(chainJson));
        } finally {
            NOW_USED_CHAIN_IDS.remove(chainId);
        }
    }

    public static ExecutionBlock createExecutor(ChainJson chainJson) {
        return getShared().toExecutor(chainJson);
    }

    public static ExecutionBlock createExecutor(Path containingJsonFile) throws IOException {
        return createExecutor(ChainJson.read(containingJsonFile));
    }

    public ExecutionBlock toExecutor(ChainJson chainJson) {
        //noinspection resource
        return use(chainJson).toExecutor();
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseSubChain useSubChain = UseSubChain.getShared();
        for (ExtensionJson.Platform platform : SUB_CHAIN_PLATFORMS.installedPlatforms()) {
            if (platform.hasModels()) {
                useInstalledFolder(useSubChain, platform.modelsFolder(), platform, "installed chain models");
            }
        }
        if (ADDITIONAL_STANDARD_SUBCHAINS_PATH != null) {
            for (String folder : UseSubChain.ADDITIONAL_STANDARD_SUBCHAINS_PATH.split("[\\;]")) {
                useInstalledFolder(useSubChain, Paths.get(folder), null, "additional chain models");
            }
        }
    }

    public static MainChainSettingsInformation getMainChainSettingsInformation(Chain chain) {
        final Object result = chain.getCustomChainInformation();
        return result instanceof MainChainSettingsInformation ? (MainChainSettingsInformation) result : null;
    }

    public static String getMainChainSettingsCombinerId(Chain chain) {
        final MainChainSettingsInformation information = getMainChainSettingsInformation(chain);
        return information != null ? information.chainSettingsCombiner().id() : null;
    }

    private void use(List<ChainJson> chainJsons, ExtensionJson.Platform platform, StringBuilder report) {
        ChainJson.checkIdDifference(chainJsons);
        for (int i = 0, n = chainJsons.size(); i < n; i++) {
            ChainJson chainJson = chainJsons.get(i);
            long t1 = infoTime();
            if (platform != null) {
                chainJson.addTags(platform.getTags());
                chainJson.setPlatformId(platform.getId());
                chainJson.setPlatformCategory(platform.getCategory());
            }
            final Optional<Chain> chain;
            try {
                chain = useIfNonRecursive(chainJson);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ChainRunningException("Cannot load sub-chain " + chainJson.getChainJsonFile(), e);
            }
            long t2 = infoTime();
            final int index = i;
            // Note: recursive usage of sub-chains is rare situation, but NOT an error, so we use only INFO level here.
            LOG.log(chain.isPresent() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO,
                    () -> String.format(Locale.US, "Sub-chain %s\"%s\"%s %sloaded from %s in %.3f ms",
                            n > 1 ? (index + 1) + "/" + n + " " : "",
                            chainJson.getExecutor().getName(),
                            additionalChainInformation(chain),
                            chain.isPresent() ? "" : "not ",
                            chainJson.getChainJsonFile().toAbsolutePath(),
                            (t2 - t1) * 1e-6));
        }
        if (report != null) {
            for (ChainJson chainJson : chainJsons) {
                final Path file = chainJson.getChainJsonFile();
                final String message = file != null ? file.toString() : chainJson.canonicalName() + " (no file)";
                report.append(message).append("\n");
            }
        }
    }

    private Chain register(ChainJson chainJson) {
        Objects.requireNonNull(chainJson, "Null chain JSON model");
        final String sessionId = getSessionId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot register new chain: session ID was not set");
        }
        final ExecutorProvider executorProvider = ExecutorProvider.newStandardInstance(sessionId);
        Chain chain = Chain.valueOf(this, executorProvider, chainJson);
        if (chain.getCurrentDirectory() == null) {
            // - If the chain was loaded not from file, but from the executor text parameter,
            // chainJson does not contain information about current folder;
            // in this case, we suppose that the current folder is equal
            // to the current folder of this UseSubChain executor.
            chain.setCurrentDirectory(this.getCurrentDirectory());
        }
        if (overrideBehaviour) {
            chain.setMultithreading(multithreading);
            chain.setExecuteAll(executeAll);
        }
        SUB_CHAIN_LOADER.registerWorker(
                sessionId, chain.id(), chain, buildSubChainModelAndExecuteLoadingTimeWithoutInputs(chain));
        loadedChainsCount.incrementAndGet();
        return chain;
    }

    private ExecutorJson buildSubChainModelAndExecuteLoadingTimeWithoutInputs(Chain chain) {
        Objects.requireNonNull(chain, "Null chain");
        final ExecutorJson result = new ExecutorJson();
        result.setTo(new InterpretSubChain());
        // - adds JavaConf and (maybe) parameters with setters
        result.setTo(chain);
        result.setSourceInfo(null, chain.chainJsonPath()).setLanguageName(SUB_CHAIN_LANGUAGE_NAME);
        if (chain.hasPlatformId()) {
            result.setPlatformId(chain.platformId());
        }
        final String category = result.getCategory();
        if (category != null) {
            result.setCategory(ExecutorJson.correctDynamicCategory(category, !chain.isAutogeneratedCategory()));
            // Note: if the chain category is explicitly specified (!isAutogeneratedCategory),
            // we disable correcting it (this is probably a usual function, like functions in other languages).
            // Unlike this, UseSettings always adds DYNAMIC_CATEGORY_PREFIX
            result.updateCategoryPrefix(chain.platformCategory());
        }
        executeLoadingTimeBlocksWithoutInputs(chain, executeIsolatedLoadingTimeFunctions);
        result.addControl(new ExecutorJson.ControlConf()
                .setName(DO_ACTION_NAME)
                .setCaption(DO_ACTION_CAPTION)
                .setDescription(DO_ACTION_DESCRIPTION)
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.TRUE));
        result.addControl(createLogTimingControl(LOG_TIMING_NAME));
        result.addControl(createTimingLogLevelControl(TIMING_LOG_LEVEL_NAME));
        result.addControl(createTimingNumberOfCallsControl(TIMING_NUMBER_OF_CALLS_NAME));
        result.addControl(createTimingNumberOfPercentilesControl(TIMING_NUMBER_OF_PERCENTILES_NAME));
        addChainSettingsCombiner(result, chain);
        final ExecutorJson.ControlConf visibleResult = createVisibleResultControl(
                result, VISIBLE_RESULT_PARAMETER_NAME);
        if (visibleResult != null) {
            result.addControl(visibleResult);
        }
        if (result.hasPlatformId()) {
            result.addSystemPlatformIdPort();
        }
        return chainExecutorModel = result;
    }

    static ExecutorJson.ControlConf createLogTimingControl(String parameterName) {
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(parameterName);
        result.setCaption(LOG_TIMING_CAPTION);
        result.setDescription(LOG_TIMING_DESCRIPTION);
        result.setValueType(ParameterValueType.BOOLEAN);
        result.setDefaultJsonValue(JsonValue.TRUE);
        result.setAdvanced(true);
        return result;
    }

    static ExecutorJson.ControlConf createTimingLogLevelControl(String parameterName) {
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(parameterName);
        result.setCaption(TIMING_LOG_LEVEL_CAPTION);
        result.setDescription(TIMING_LOG_LEVEL_DESCRIPTION);
        result.setValueType(ParameterValueType.STRING);
        result.setDefaultStringValue(TIMING_LOG_LEVEL_DEFAULT);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(List.of(
                new ExecutorJson.ControlConf.EnumItem(System.Logger.Level.WARNING.getName()),
                new ExecutorJson.ControlConf.EnumItem(System.Logger.Level.INFO.getName()),
                new ExecutorJson.ControlConf.EnumItem(System.Logger.Level.DEBUG.getName()),
                new ExecutorJson.ControlConf.EnumItem(System.Logger.Level.TRACE.getName())));
        result.setAdvanced(true);
        return result;
    }

    static ExecutorJson.ControlConf createTimingNumberOfCallsControl(String parameterName) {
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(parameterName);
        result.setCaption(TIMING_NUMBER_OF_CALLS_CAPTION);
        result.setDescription(TIMING_NUMBER_OF_CALLS_DESCRIPTION);
        result.setValueType(ParameterValueType.INT);
        result.setDefaultJsonValue(Jsons.toJsonIntValue(TIMING_NUMBER_OF_CALLS_DEFAULT));
        result.setEditionType(ControlEditionType.VALUE);
        result.setAdvanced(true);
        return result;
    }

    static ExecutorJson.ControlConf createTimingNumberOfPercentilesControl(String parameterName) {
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(parameterName);
        result.setCaption(TIMING_NUMBER_OF_PERCENTILES_CAPTION);
        result.setDescription(TIMING_NUMBER_OF_PERCENTILES_DESCRIPTION);
        result.setValueType(ParameterValueType.INT);
        result.setDefaultJsonValue(Jsons.toJsonIntValue(TIMING_NUMBER_OF_PERCENTILES_DEFAULT));
        result.setEditionType(ControlEditionType.VALUE);
        result.setAdvanced(true);
        return result;
    }

    static ExecutorJson.ControlConf createVisibleResultControl(ExecutorJson executorJson, String parameterName) {
        String firstEnumValue = null;
        final List<ExecutorJson.ControlConf.EnumItem> items = new ArrayList<>();
        for (ExecutorJson.PortConf portConf : executorJson.getOutPorts().values()) {
            final String executorPortName = portConf.getName();
            if (firstEnumValue == null && !executorPortName.equals(CombineSettings.SETTINGS)) {
                firstEnumValue = executorPortName;
            }
            items.add(new ExecutorJson.ControlConf.EnumItem(executorPortName));
        }
        if (items.size() < 2) {
            // - no sense to add visible result control
            return null;
        }
        if (firstEnumValue == null) {
            // - there is only SETTINGS port
            firstEnumValue = executorJson.getOutPorts().values().iterator().next().getName();
        }
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(parameterName);
        result.setCaption(VISIBLE_RESULT_PARAMETER_CAPTION);
        result.setValueType(ParameterValueType.ENUM_STRING);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(items);
        result.setDefaultStringValue(firstEnumValue);
        return result;
    }

    static void addSettingsPorts(ExecutorJson result) {
        result.addFirstInPort(new ExecutorJson.PortConf()
                .setName(CombineSettings.SETTINGS)
                .setValueType(DataType.SCALAR));
        result.addFirstOutPort(new ExecutorJson.PortConf()
                .setName(CombineSettings.SETTINGS)
                .setHint("Actually used settings (JSON)")
                .setAdvanced(true)
                .setValueType(DataType.SCALAR));
    }

    private static void useInstalledFolder(
            UseSubChain useSubChain,
            Path folder,
            ExtensionJson.Platform platform,
            String name) throws IOException {
        final long t1 = System.nanoTime();
        useSubChain.usePath(folder, platform, null);
        final long t2 = System.nanoTime();
        logInfo(() -> String.format(Locale.US,
                "Loading %s from %s: %.3f ms",
                name, folder, (t2 - t1) * 1e-6));
    }

    private static String additionalChainInformation(Optional<Chain> chain) {
        if (chain.isEmpty()) {
            return " " + RECURSIVE_LOADING_BLOCKED_MESSAGE;
        }
        final MainChainSettingsInformation info = getMainChainSettingsInformation(chain.get());
        return info == null ? "" : ", " + info.chainSettingsCombiner();
    }

    private static void executeLoadingTimeBlocksWithoutInputs(
            Chain chain,
            boolean executeIsolatedLoadingTimeFunctions) {
        for (ChainBlock block : chain.getAllBlocks().values()) {
            if (block.isExecutedAtLoadingTime()) {
                block.reinitialize(true);
                if (block.numberOfConnectedInputPorts() == 0) {
                    // Note: we do not execute loading-time SUBCHAINS,
                    // only isolated blocks without input ports.
                    // It is enough for most needs.
                    final Executor executor = block.getExecutor();
                    if (executeIsolatedLoadingTimeFunctions || executor instanceof UseSettings) {
                        // Note: UseSettings and UseChainSettings are executed always
                        try {
                            executor.reset();
                            executor.execute(true);
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

    private static void addChainSettingsCombiner(ExecutorJson executorJson, Chain chain) {
        final ChainBlock useChainSettingsBlock = findUseSettings(chain);
        if (useChainSettingsBlock == null) {
            return;
        }
        final UseChainSettings useChainSettings = (UseChainSettings) useChainSettingsBlock.getExecutor();
        final SettingsCombiner combiner = useChainSettings.settingsCombiner();
        // - combiner was already executed in executeLoadingTimeBlocksWithoutInputs(chain)
        chain.setCustomChainInformation(new MainChainSettingsInformation(chain, combiner));
        UseSettings.addExecuteSubChainControlsAndPorts(executorJson, combiner);
        executorJson.createOptionsIfAbsent().createServiceIfAbsent().setSettingsId(combiner.id());
        addSettingsPorts(executorJson);
    }

    public static ChainBlock findUseSettings(Chain chain) {
        for (ChainBlock block : chain.getAllBlocks().values()) {
            if (block.isExecutedAtLoadingTime()) {
                if (block.getExecutor() instanceof UseChainSettings) {
                    return block;
                }
            }
        }
        return null;
    }
}
