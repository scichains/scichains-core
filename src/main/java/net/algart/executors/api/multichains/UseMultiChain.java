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

package net.algart.executors.api.multichains;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import net.algart.executors.api.chains.ChainLoadingException;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.chains.UseSubChain;
import net.algart.executors.api.extensions.InstalledPlatformsForTechnology;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.settings.UseSettings;
import net.algart.executors.api.system.CreateMode;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.io.FileOperation;
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
            "If set, the parameters of this multi-chain are determined by the section \""
                    + SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" "
                    + "of the input settings JSON (when it exists). If cleared, the parameters of this multi-chain "
                    + "are always extracted directly from the top level of the input settings JSON. "
                    + "Parameters below have less priority than the contents of input settings.";
    public static final boolean EXTRACT_SUB_SETTINGS_PARAMETER_DEFAULT = true;
    public static final String LOG_SETTINGS_PARAMETER_NAME = "_mch___logSettings";
    public static final String LOG_SETTINGS_PARAMETER_CAPTION = "Log settings";
    public static final String LOG_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, all settings, passed to the selected chain variant of this multi-chain, "
                    + "are logged with level WARNING.";
    public static final String IGNORE_PARAMETERS_PARAMETER_NAME = "_mch___ignoreInputParameter";
    public static final String IGNORE_PARAMETERS_PARAMETER_CAPTION = "Ignore parameters below";
    public static final String IGNORE_PARAMETERS_PARAMETER_DESCRIPTION =
            "If set, the behavior is fully determined by the input settings port and internal settings " +
                    "of the sub-chain. All parameters below are not included into the settings JSON " +
                    "even if they are not specified in the section \"" +
                    SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" " +
                    "of the input settings JSON.\n" +
                    "However: if there are parameters in the selected chain that are specified in the chain blocks " +
                    "and not in the JSON, they are copied from the corresponding parameters below in any case.";
    public static final boolean IGNORE_PARAMETERS_PARAMETER_DEFAULT = false;

    static final String RECURSIVE_LOADING_BLOCKED_MESSAGE = "[recursive loading blocked]";

    private static final InstalledPlatformsForTechnology MULTI_CHAIN_PLATFORMS =
            InstalledPlatformsForTechnology.of(MULTICHAIN_TECHNOLOGY);
    private static final DefaultExecutorLoader<MultiChain> MULTI_CHAIN_LOADER =
            new DefaultExecutorLoader<>("multi-chains loader");

    static {
        globalLoaders().register(MULTI_CHAIN_LOADER);
    }

    private boolean fileExistenceRequired = true;
    private boolean alsoSubChains = false;
    private boolean strictMode = true;

    public UseMultiChain() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseMultiChain getInstance(String sessionId) {
        return setSession(new UseMultiChain(), sessionId);
    }

    public static UseMultiChain getSharedInstance() {
        return setShared(new UseMultiChain());
    }

    public static DefaultExecutorLoader<MultiChain> multiChainLoader() {
        return MULTI_CHAIN_LOADER;
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

    public static MultiChainExecutor newSharedExecutor(Path file) throws IOException {
        return newSharedExecutor(file, CreateMode.NORMAL);
    }

    public static MultiChainExecutor newSharedExecutor(Path file, CreateMode createMode)
            throws IOException {
        return newSharedExecutor(MultiChainSpecification.read(file), createMode);
    }

    public static MultiChainExecutor newSharedExecutor(MultiChainSpecification specification, CreateMode createMode)
            throws IOException {
        return getSharedInstance().newExecutor(specification, createMode);
    }

    public MultiChainExecutor newExecutor(Path file, CreateMode createMode)
            throws IOException {
        return newExecutor(MultiChainSpecification.read(file), createMode);
    }

    public MultiChainExecutor newExecutor(MultiChainSpecification specification, CreateMode createMode)
            throws IOException {
        //noinspection resource
        return use(specification).newExecutor(createMode);
    }

    @Override
    public void process() {
        // Note: unlike UseSubChain, this function does not allow to specify multi-chain in a text parameter.
        // It is useless because it does not allow avoiding files at all:
        // a multi-chain in any case requires several files for sub-chain variants.
        try {
            useSeveralPaths(completeSeveralFilePaths());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void useSeveralPaths(List<Path> multiChainSpecificationPaths) throws IOException {
        Objects.requireNonNull(multiChainSpecificationPaths, "Null multi-chains paths");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : multiChainSpecificationPaths) {
            usePath(path, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public int usePath(Path multiChainSpecificationPath) throws IOException {
        return usePath(multiChainSpecificationPath, null);
    }

    public int usePath(Path multiChainSpecificationPath, StringBuilder report) throws IOException {
        Objects.requireNonNull(multiChainSpecificationPath, "Null multiChainSpecificationPath");
        final List<MultiChainSpecification> multiChainSpecifications;
        final List<ChainSpecification> chainSpecifications;
        if (!Files.exists(multiChainSpecificationPath)) {
            if (fileExistenceRequired) {
                throw new FileNotFoundException("Multi-chain file or multi-chains folder " +
                        multiChainSpecificationPath + " does not exist");
            } else {
                return 0;
            }
        }
        if (Files.isDirectory(multiChainSpecificationPath)) {
            multiChainSpecifications = MultiChainSpecification.readAllIfValid(multiChainSpecificationPath);
            // - always recursive
            chainSpecifications = alsoSubChains ?
                    ChainSpecification.readAllIfValid(multiChainSpecificationPath, true) :
                    Collections.emptyList();
        } else if (!alsoSubChains) {
            multiChainSpecifications = Collections.singletonList(
                    MultiChainSpecification.read(multiChainSpecificationPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
            chainSpecifications = Collections.emptyList();
        } else {
            final MultiChainSpecification multiChainSpecification = MultiChainSpecification.readIfValid(
                    multiChainSpecificationPath);
            multiChainSpecifications = multiChainSpecification == null ?
                    Collections.emptyList() : Collections.singletonList(multiChainSpecification);
            final ChainSpecification chainSpecification = ChainSpecification.readIfValid(multiChainSpecificationPath);
            chainSpecifications = chainSpecification == null ?
                    Collections.emptyList() :
                    Collections.singletonList(chainSpecification);
            if (multiChainSpecification == null && chainSpecification == null) {
                throw new JsonException("JSON " + multiChainSpecificationPath
                        + " is not a valid multi-chain or sub-chain configuration");

            }
        }
        use(multiChainSpecifications, report);
        try (UseSubChain chainFactory = createChainFactory()) {
            chainFactory.use(chainSpecifications, report);
        }
        return multiChainSpecifications.size();
    }

    public void use(List<MultiChainSpecification> multiChainSpecifications, StringBuilder report) throws IOException {
        MultiChainSpecification.checkIdDifference(multiChainSpecifications);
        final UseSubChain chainFactory = createChainFactory();
        final UseMultiChainSettings settingsFactory = createSettingsFactory();
        for (int i = 0, n = multiChainSpecifications.size(); i < n; i++) {
            final MultiChainSpecification multiChainSpecification = multiChainSpecifications.get(i);
            final MultiChain multiChain;
            long t1 = infoTime();
            try {
                // Note: recursion is not a problem here; in this case, all sub-chains will be just skipped
                multiChain = use(multiChainSpecification, chainFactory, settingsFactory);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                // - but not IOException
                throw new ChainLoadingException("Cannot load multi-chain "
                        + multiChainSpecification.getMultiChainSpecificationFile(), e);
            }
            long t2 = infoTime();
            final List<ChainSpecification> chainSpecifications = multiChain.chainSpecifications();
            final Set<String> blockedChainModelNames = multiChain.blockedChainSpecificationNames();
            // - Note: in a multi-chain, all chain variants always have different names
            // (it is checked in MultiChainSpecification.readChainVariants method).
            // Note: recursive usage of multi-chain is a sub-chain is possible, but seems to be an error,
            // so, we use WARNING level here.
            final int index = i;
            LOG.log(blockedChainModelNames.isEmpty() ? Level.DEBUG : Level.WARNING,
                    () -> String.format(Locale.US, "Multi-chain %s\"%s\" loaded from %s in %.3f ms; " +
                                    "%d chain variants:%n%s",
                            n > 1 ? (index + 1) + "/" + n + " " : "",
                            multiChainSpecification.getName(),
                            multiChainSpecification.getMultiChainSpecificationFile().toAbsolutePath(),
                            (t2 - t1) * 1e-6,
                            chainSpecifications.size(),
                            chainSpecifications.stream().map(
                                            m -> String.format("  \"%s\"%s from %s",
                                                    m.chainName(),
                                                    blockedChainModelNames.contains(m.chainName()) ?
                                                            " " + RECURSIVE_LOADING_BLOCKED_MESSAGE : "",
                                                    m.getChainSpecificationFile()))
                                    .collect(Collectors.joining(String.format("%n")))));
        }
        if (report != null) {
            for (MultiChainSpecification multiChainSpecification : multiChainSpecifications) {
                final Path file = multiChainSpecification.getMultiChainSpecificationFile();
                final String message = file != null ? file.toString() : multiChainSpecification.canonicalName() + " (no file)";
                report.append(message).append("\n");
            }
        }
    }

    public MultiChain use(MultiChainSpecification multiChainSpecification) throws IOException {
        // Note: recursion is not a problem here; in this case, all sub-chains will be just skipped
        final UseSubChain chainFactory = createChainFactory();
        final UseMultiChainSettings settingsFactory = createSettingsFactory();
        return use(multiChainSpecification, chainFactory, settingsFactory);
    }

    public MultiChain use(
            MultiChainSpecification multiChainSpecification,
            UseSubChain chainFactory,
            UseMultiChainSettings settingsFactory)
            throws IOException {
        final MultiChain multiChain = MultiChain.of(multiChainSpecification, chainFactory, settingsFactory);
        // - Actually use all sub-chains and built-in multi-chain settings combiner
        if (strictMode) {
            multiChain.checkImplementationCompatibility();
        }
        MULTI_CHAIN_LOADER.registerWorker(getSessionId(), buildMultiChainSpecification(multiChain), multiChain);
        return multiChain;
    }

    public static ExecutorSpecification buildMultiChainSpecification(MultiChain multiChain) {
        Objects.requireNonNull(multiChain, "Null multiChain");
        final MultiChainSpecification specification = multiChain.specification();
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(new InterpretMultiChain());
        // - adds JavaConf and (maybe) parameters with setters
        result.setSourceInfo(multiChain.multiChainSpecificationFile(), null);
        result.setId(multiChain.id());
        result.setCategory(ExecutorSpecification.correctDynamicCategory(multiChain.category()));
        result.setName(multiChain.name());
        result.setDescription(multiChain.description());
        result.setLanguage(MULTICHAIN_LANGUAGE);
        result.setInputPorts(specification.getInputPorts());
        result.setOutputPorts(specification.getOutputPorts());
        UseSubChain.addSettingsPorts(result);
        final SettingsBuilder multiChainSettingsBuilder = multiChain.settingsBuilder();
        addSystemParameters(result, multiChain);
        UseSettings.addMultiChainControlsAndPorts(result, multiChain.multiChainOnlyCommonSettingsBuilder());
        // - Here we add to multi-chain executor the same ports/controls that we already added to its settings.
        // Also, we add ABSOLUTE_PATHS_NAME_PARAMETER_NAME if necessary.
        // Note: here we should SKIP sub-settings for chain variants, added by usual multiChainSettingsBuilder
        result.setSettings(multiChainSettingsBuilder.specification());
        // - important: we MUST store the full multi-chain settings as normal settings,
        // not a reduced version from multiChainOnlyCommonSettings()
        final ExecutorSpecification.Options options = result.createOptionsIfAbsent();
        options.createControllingIfAbsent()
                .setGrouping(true)
                .setGroupSelector(multiChain.selectedChainParameter());
        options.createServiceIfAbsent().setSettingsId(multiChainSettingsBuilder.id());
        final ExecutorSpecification.ControlConf visibleResult = UseSubChain.createVisibleResultControl(
                result, VISIBLE_RESULT_PARAMETER_NAME);
        if (visibleResult != null) {
            result.addControl(visibleResult);
        }
        return result;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseMultiChain useMultiChain = UseMultiChain.getSharedInstance();
        for (String folder : MULTI_CHAIN_PLATFORMS.installedSpecificationFolders()) {
            final long t1 = System.nanoTime();
            final int n = useMultiChain.usePath(Paths.get(folder));
            final long t2 = System.nanoTime();
            logInfo(() -> String.format(Locale.US,
                    "Loading %d installed multi-chain specifications from %s: %.3f ms",
                    n, folder, (t2 - t1) * 1e-6));
        }
    }

    private static void addSystemParameters(ExecutorSpecification result, MultiChain multiChain) {
        final String multiChainName = multiChain.name();
        if (multiChain.specification().isBehaviourSkippable()) {
            result.addControl(new ExecutorSpecification.ControlConf()
                    .setName(DO_ACTION_NAME)
                    .setCaption(DO_ACTION_CAPTION)
                    .setDescription(DO_ACTION_DESCRIPTION)
                    .setValueType(ParameterValueType.BOOLEAN)
                    .setDefaultJsonValue(JsonValue.TRUE)
                    .setAdvanced(false));
        }
        result.addControl(UseSubChain.createLogTimingControl(LOG_TIMING_NAME));
        result.addControl(UseSubChain.createTimingLogLevelControl(TIMING_LOG_LEVEL_NAME));
        result.addControl(UseSubChain.createTimingNumberOfCallsControl(TIMING_NUMBER_OF_CALLS_NAME));
        result.addControl(UseSubChain.createTimingNumberOfPercentilesControl(TIMING_NUMBER_OF_PERCENTILES_NAME));
        result.addControl(new ExecutorSpecification.ControlConf()
                .setName(EXTRACT_SUB_SETTINGS_PARAMETER_NAME)
                .setCaption(EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION.replace("%%%", multiChainName))
                .setDescription(EXTRACT_SUB_SETTINGS_PARAMETER_DESCRIPTION.replace("%%%", multiChainName))
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(Jsons.toJsonBooleanValue(EXTRACT_SUB_SETTINGS_PARAMETER_DEFAULT))
                .setAdvanced(true));
        result.addControl(new ExecutorSpecification.ControlConf()
                .setName(LOG_SETTINGS_PARAMETER_NAME)
                .setCaption(LOG_SETTINGS_PARAMETER_CAPTION)
                .setDescription(LOG_SETTINGS_PARAMETER_DESCRIPTION)
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.FALSE)
                .setAdvanced(true));
        result.addControl(new ExecutorSpecification.ControlConf()
                .setName(IGNORE_PARAMETERS_PARAMETER_NAME)
                .setCaption(IGNORE_PARAMETERS_PARAMETER_CAPTION)
                .setDescription(IGNORE_PARAMETERS_PARAMETER_DESCRIPTION.replace("%%%", multiChainName))
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(JsonValue.FALSE)
                .setAdvanced(true));
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
