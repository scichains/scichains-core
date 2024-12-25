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

import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.SimpleExecutorLoader;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.model.ChainJson;
import net.algart.executors.api.model.ChainLoadingException;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.InstalledPlatformsForTechnology;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombinerJson;
import net.algart.executors.modules.core.logic.compiler.subchains.interpreters.InterpretMultiChain;
import net.algart.executors.modules.core.logic.compiler.subchains.model.MultiChain;
import net.algart.executors.modules.core.logic.compiler.subchains.model.MultiChainJson;
import net.algart.json.Jsons;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class UseMultiChain extends FileOperation {
    public static final String MULTICHAIN_TECHNOLOGY = "multichain";
    public static final String MULTICHAIN_LANGUAGE = "multichain";

    public static final String DO_ACTION_NAME = "_mch___doAction";
    public static final String DO_ACTION_CAPTION = "Do actions";
    public static final String DO_ACTION_DESCRIPTION = "If set, function is executed normally. "
            + "If cleared, this function just copies all input data "
            + "to the output ports with the same names and types (if they exist) "
            + "and does not anything else.";
    public static final String LOG_TIMING_NAME = "_mch___logTiming";
    public static final String TIMING_LOG_LEVEL_NAME = "_mch___timingLogLevel";
    public static final String TIMING_NUMBER_OF_CALLS_NAME = "_mch___timingNumberOfCalls";
    public static final String TIMING_NUMBER_OF_PERCENTILES_NAME = "_mch___timingNumberOfPercentiles";
    public static final String VISIBLE_RESULT_PARAMETER_NAME = "_mch___visibleResult";
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_NAME = "_mch___extractSubSettings";
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION =
            "Extract sub-settings \"%%%\" from source JSON";
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, the parameters of this multichain are determined by the section \""
                    + SettingsCombinerJson.SUBSETTINGS_PREFIX + "%%%\" "
                    + "of the input settings JSON. If cleared, the parameters of this multichain "
                    + "are extracted directly from the top level of the input settings JSON. "
                    + "Parameters below have less priority, then the content of input settings.";
    public static final boolean EXTRACT_SUB_SETTINGS_PARAMETER_DEFAULT = true;
    public static final String LOG_SETTINGS_PARAMETER_NAME = "_mch___logSettings";
    public static final String LOG_SETTINGS_PARAMETER_CAPTION = "Log settings";
    public static final String LOG_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, all settings, passed to the selected chain variant of this multi-chain, "
                    + "are logged with level WARNING.";
    public static final String IGNORE_PARAMETERS_PARAMETER_NAME = "_mch___ignoreInputParameter";
    public static final String IGNORE_PARAMETERS_PARAMETER_CAPTION = "Ignore parameters below";
    public static final String IGNORE_PARAMETERS_PARAMETER_DESCRIPTION =
            "If set, the behavior is fully determined by the input settings port and internal settings "
                    + "of the sub-chain. All parameters below are fully ignored, even "
                    + "if they are not specified in the section \""
                    + SettingsCombinerJson.SUBSETTINGS_PREFIX + "%%%\" "
                    + "of the input settings JSON.\n"
                    + "We recommend to set this flag always in multichain configuration, "
                    + "if you are allowing and planning to replace some sub-chains in future.";
    public static final boolean IGNORE_PARAMETERS_PARAMETER_DEFAULT = false;

    private static final InstalledPlatformsForTechnology MULTICHAIN_PLATFORMS =
            InstalledPlatformsForTechnology.getInstance(MULTICHAIN_TECHNOLOGY);
    private static final SimpleExecutorLoader<MultiChain> MULTICHAIN_LOADER =
            new SimpleExecutorLoader<>("multi-chains loader");

    static {
        ExecutionBlock.registerExecutorLoader(MULTICHAIN_LOADER);
    }

    private boolean fileExistenceRequired = true;
    private boolean alsoSubChains = false;
    private boolean strictMode = true;

    public UseMultiChain() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseMultiChain getInstance() {
        return new UseMultiChain();
    }

    public static SimpleExecutorLoader<MultiChain> multiChainLoader() {
        return MULTICHAIN_LOADER;
    }

    public boolean isFileExistenceRequired() {
        return fileExistenceRequired;
    }

    public UseMultiChain setFileExistenceRequired(boolean fileExistenceRequired) {
        this.fileExistenceRequired = fileExistenceRequired;
        return this;
    }

    public boolean isAlsoSubChains() {
        return alsoSubChains;
    }

    public UseMultiChain setAlsoSubChains(boolean alsoSubChains) {
        this.alsoSubChains = alsoSubChains;
        return this;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public UseMultiChain setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
        return this;
    }

    @Override
    public void process() {
        // Note: unlike UseSubChain, this function does not allow to specify multichain in a text parameter.
        // It is useless, because does not allow to avoid files at all:
        // multichain in any case requires several files for subchain variants.
        try {
            useSeveralPaths(completeSeveralFilePaths());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void useSeveralPaths(List<Path> multiChainJsonPaths) throws IOException {
        Objects.requireNonNull(multiChainJsonPaths, "Null multichains paths");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : multiChainJsonPaths) {
            usePath(path, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path multiChainJsonPath) throws IOException {
        usePath(multiChainJsonPath, null);
    }

    public void usePath(Path multiChainJsonPath, StringBuilder report) throws IOException {
        Objects.requireNonNull(multiChainJsonPath, "Null multichain path");
        final List<MultiChainJson> multiChainJsons;
        final List<ChainJson> chainJsons;
        if (!Files.exists(multiChainJsonPath)) {
            if (fileExistenceRequired) {
                throw new FileNotFoundException("Multichain file or multi-chains folder " + multiChainJsonPath
                        + " does not exist");
            } else {
                return;
            }
        }
        if (Files.isDirectory(multiChainJsonPath)) {
            multiChainJsons = MultiChainJson.readAllIfValid(multiChainJsonPath);
            // - always recursive
            chainJsons = alsoSubChains ?
                    ChainJson.readAllIfValid(multiChainJsonPath, true) :
                    Collections.emptyList();
        } else if (!alsoSubChains) {
            multiChainJsons = Collections.singletonList(MultiChainJson.read(multiChainJsonPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
            chainJsons = Collections.emptyList();
        } else {
            final MultiChainJson multiChainJson = MultiChainJson.readIfValid(multiChainJsonPath);
            multiChainJsons = multiChainJson == null ?
                    Collections.emptyList() : Collections.singletonList(multiChainJson);
            final ChainJson chainJson = ChainJson.readIfValid(multiChainJsonPath);
            chainJsons = chainJson == null ? Collections.emptyList() : Collections.singletonList(chainJson);
            if (multiChainJson == null && chainJson == null) {
                throw new JsonException("JSON " + multiChainJsonPath
                        + " is not a valid multichain or sub-chain configuration");

            }
        }
        use(multiChainJsons, report);
        try (UseSubChain chainFactory = createChainFactory()) {
            chainFactory.use(chainJsons, report);
        }
    }

    public void use(List<MultiChainJson> multiChainJsons, StringBuilder report) throws IOException {
        MultiChainJson.checkIdDifference(multiChainJsons);
        final UseSubChain chainFactory = createChainFactory();
        final UseMultiChainSettings settingsFactory = createSettingsFactory();
        for (int i = 0, n = multiChainJsons.size(); i < n; i++) {
            final MultiChainJson multiChainJson = multiChainJsons.get(i);
            final MultiChain multiChain;
            long t1 = infoTime();
            try {
                // Note: recursion is not a problem here; in this case, all sub-chains will be just skipped
                multiChain = use(multiChainJson, chainFactory, settingsFactory);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                // - but not IOException
                throw new ChainLoadingException("Cannot load multichain "
                        + multiChainJson.getMultiChainJsonFile(), e);
            }
            long t2 = infoTime();
            final List<ChainJson> chainModels = multiChain.chainModels();
            final Set<String> blockedChainModelNames = multiChain.blockedChainModelNames();
            // - Note: in a multichain, all chain variants always have different names
            // (it is checked in MultiChainJson.readChainVariants method).
            // Note: recursive usage of multichain is a sub-chain is possible, but seems to be an error,
            // so, we use WARNING level here.
            final int index = i;
            LOG.log(blockedChainModelNames.isEmpty() ? Level.DEBUG : Level.WARNING,
                    () -> String.format(Locale.US, "Multichain %s\"%s\" loaded from %s in %.3f ms; " +
                                    "%d chain variants:%n%s",
                            n > 1 ? (index + 1) + "/" + n + " " : "",
                            multiChainJson.getName(),
                            multiChainJson.getMultiChainJsonFile().toAbsolutePath(),
                            (t2 - t1) * 1e-6,
                            chainModels.size(),
                            chainModels.stream().map(
                                            m -> String.format("  \"%s\"%s from %s",
                                                    m.chainName(),
                                                    blockedChainModelNames.contains(m.chainName()) ?
                                                            " " + UseSubChain.RECURSIVE_LOADING_BLOCKED_MESSAGE : "",
                                                    m.getChainJsonFile()))
                                    .collect(Collectors.joining(String.format("%n")))));
        }
        if (report != null) {
            for (MultiChainJson multiChainJson : multiChainJsons) {
                final Path file = multiChainJson.getMultiChainJsonFile();
                final String message = file != null ? file.toString() : multiChainJson.canonicalName() + " (no file)";
                report.append(message).append("\n");
            }
        }
    }

    public void use(MultiChainJson multiChainJson) throws IOException {
        // Note: recursion is not a problem here; in this case, all sub-chains will be just skipped
        final UseSubChain chainFactory = createChainFactory();
        final UseMultiChainSettings settingsFactory = createSettingsFactory();
        use(multiChainJson, chainFactory, settingsFactory);
    }

    public MultiChain use(
            MultiChainJson multiChainJson,
            UseSubChain chainFactory,
            UseMultiChainSettings settingsFactory)
            throws IOException {
        final MultiChain multiChain = MultiChain.valueOf(multiChainJson, chainFactory, settingsFactory);
        // - Actually use all sub-chains and built-in multichain settings combiner
        if (strictMode) {
            multiChain.checkImplementationCompatibility();
        }
        MULTICHAIN_LOADER.registerWorker(
                getSessionId(), multiChain.id(), multiChain, buildMultiChainModel(multiChain));
        return multiChain;
    }

    public static ExecutorJson buildMultiChainModel(MultiChain multiChain) {
        Objects.requireNonNull(multiChain, "Null multiChain");
        final MultiChainJson model = multiChain.model();
        ExecutorJson result = new ExecutorJson();
        result.setTo(new InterpretMultiChain());
        // - adds JavaConf and (maybe) parameters with setters
        result.setSourceInfo(multiChain.multiChainJsonFile(), null);
        result.setExecutorId(multiChain.id());
        result.setCategory(ExecutorJson.correctDynamicCategory(multiChain.category()));
        result.setName(multiChain.name());
        result.setDescription(multiChain.description());
        result.setLanguage(MULTICHAIN_LANGUAGE);
        result.setInPorts(model.getInPorts());
        result.setOutPorts(model.getOutPorts());
        UseSubChain.addSettingsPorts(result);
        final SettingsCombiner settingsCombinerForMultiChain = multiChain.multiChainOnlyCommonSettingsCombiner();
        addSystemParameters(result, multiChain);
        UseSettings.addExecuteMultiChainControlsAndPorts(result, settingsCombinerForMultiChain);
        // - also adds ABSOLUTE_PATHS_NAME_PARAMETER_NAME if necessary;
        // note: here we should SKIP sub-settings for chain variants, added by usual multiChainSettingsCombiner
        final ExecutorJson.Options options = result.createOptionsIfAbsent();
        options.createControllingIfAbsent()
                .setGrouping(true)
                .setGroupSelector(MultiChain.SELECTED_CHAIN_ID_PARAMETER_NAME);
        options.createServiceIfAbsent()
                .setSettingsId(settingsCombinerForMultiChain.id());
        final ExecutorJson.ControlConf visibleResult = UseSubChain.createVisibleResultControl(
                result, VISIBLE_RESULT_PARAMETER_NAME);
        if (visibleResult != null) {
            result.addControl(visibleResult);
        }
        return result;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseMultiChain useMultiChain = UseMultiChain.getInstance();
        useMultiChain.setSessionId(GLOBAL_SHARED_SESSION_ID);
        for (String folder : MULTICHAIN_PLATFORMS.installedModelFolders()) {
            final long t1 = System.nanoTime();
            useMultiChain.usePath(Paths.get(folder));
            final long t2 = System.nanoTime();
            logInfo(() -> String.format(Locale.US,
                    "Loading installed multichain models from %s: %.3f ms",
                    folder, (t2 - t1) * 1e-6));
        }
    }

    private static void addSystemParameters(ExecutorJson result, MultiChain multiChain) {
        final String multiChainName = multiChain.name();
        final MultiChainJson.Options options = multiChain.model().getOptions();
        if (options != null && options.getBehavior() != null && options.getBehavior().isSkippable()) {
            result.addControl(new ExecutorJson.ControlConf()
                    .setName(DO_ACTION_NAME)
                    .setCaption(DO_ACTION_CAPTION)
                    .setDescription(DO_ACTION_DESCRIPTION)
                    .setValueType(ParameterValueType.BOOLEAN)
                    .setDefaultJsonValue(JsonValue.TRUE));
        }
        result.addControl(UseSubChain.createLogTimingControl(LOG_TIMING_NAME));
        result.addControl(UseSubChain.createTimingLogLevelControl(TIMING_LOG_LEVEL_NAME));
        result.addControl(UseSubChain.createTimingNumberOfCallsControl(TIMING_NUMBER_OF_CALLS_NAME));
        result.addControl(UseSubChain.createTimingNumberOfPercentilesControl(TIMING_NUMBER_OF_PERCENTILES_NAME));
        result.addControl(new ExecutorJson.ControlConf()
                .setName(EXTRACT_SUB_SETTINGS_PARAMETER_NAME)
                .setCaption(EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION.replace("%%%", multiChainName))
                .setDescription(EXTRACT_SUB_SETTINGS_PARAMETER_DESCRIPTION.replace("%%%", multiChainName))
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(Jsons.toJsonBooleanValue(EXTRACT_SUB_SETTINGS_PARAMETER_DEFAULT))
                .setAdvanced(true));
        result.addControl(new ExecutorJson.ControlConf()
                .setName(LOG_SETTINGS_PARAMETER_NAME)
                .setCaption(LOG_SETTINGS_PARAMETER_CAPTION)
                .setDescription(LOG_SETTINGS_PARAMETER_DESCRIPTION)
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.FALSE)
                .setAdvanced(true));
        result.addControl(new ExecutorJson.ControlConf()
                .setName(IGNORE_PARAMETERS_PARAMETER_NAME)
                .setCaption(IGNORE_PARAMETERS_PARAMETER_CAPTION)
                .setDescription(IGNORE_PARAMETERS_PARAMETER_DESCRIPTION.replace("%%%", multiChainName))
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.FALSE)
                .setAdvanced(false));
    }

    private UseSubChain createChainFactory() {
        final UseSubChain chainFactory = new UseSubChain();
        chainFactory.setSessionId(getSessionId());
        // - Besides this, we use standard parameters of this executor: do not try to customize them.
        // It is a lightweight object, no sense to optimize its creation.
        return chainFactory;
    }

    private UseMultiChainSettings createSettingsFactory() {
        final UseMultiChainSettings settingsFactory = new UseMultiChainSettings();
        settingsFactory.setSessionId(getSessionId());
        // - Besides this, we use standard parameters of this executor: do not try to customize them.
        // It is a lightweight object, no sense to optimize its creation.
        return settingsFactory;
    }

}
