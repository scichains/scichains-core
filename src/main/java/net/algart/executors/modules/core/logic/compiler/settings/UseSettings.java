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

package net.algart.executors.modules.core.logic.compiler.settings;

import jakarta.json.JsonValue;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledPlatformsForTechnology;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.*;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsSpecification;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class UseSettings extends FileOperation {
    public static final String SETTINGS_TECHNOLOGY = "settings";
    public static final String SETTINGS_LANGUAGE = "settings";

    public static final String OUTPUT_COMBINE_SPECIFICATION = "combine-specification";
    public static final String OUTPUT_SPLIT_SPECIFICATION = "split-specification";
    public static final String OUTPUT_GET_NAMES_SPECIFICATION = "get-names-specification";

    public static final String SETTINGS_NAME_OUTPUT_NAME = "_cs___settings_name";
    public static final String SETTINGS_NAME_OUTPUT_CAPTION = "settings_name";
    public static final String ALL_SETTINGS_PARAMETER_NAME = SettingsSpecification.SETTINGS;
    public static final String ALL_SETTINGS_PARAMETER_CAPTION = "settings (all)";
    public static final String ALL_SETTINGS_PARAMETER_DESCRIPTION =
            "If contains non-empty string and if the input \"settings\" port is NOT initialized, "
                    + "it should be JSON, containing ALL settings for this combiner "
                    + "(in other words, full settings set for the chain). "
                    + "It will be used instead of input \"settings\" JSON. "
                    + "In this case, as well as \"settings\" port is initialized by some JSON, "
                    + "all parameters below will be overridden by this JSON.\n"
                    + "Note: \"overriding\" does not mean \"replacing\": if JSON does not contain some settings, "
                    + "this function will use settings from parameters below or from other input settings ports.";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_NAME = "_cs___absolutePaths";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION = "Auto-replace paths to absolute";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION =
            "If set, all parameters below, describing paths to files or folders, are automatically replaced "
                    + "with full absolute disk paths. It is useful, if you need to pass these parameters "
                    + "to other sub-chains, that probably work in other \"current\" directories.\n"
                    + "Also in this case you can use in such parameters Java system properties: "
                    + "\"${name}\", like \"${java.io.tmpdir}\", and executor system properties \"${path.name.ext}\", "
                    + "\"${path.name}\", \"${file.name.ext}\", \"${file.name}\" "
                    + "(chain path/file name with/without extension).";

// See commented code in CombineSettings.process() and addInputControlsAndPorts()
//    public static final String ADD_SETTINGS_CLASS_PARAMETER_NAME = "_cs___addSettingsName";
//    public static final String ADD_SETTINGS_CLASS_PARAMETER_CAPTION = "Add settings class \"%%%\"";
//    public static final String ADD_SETTINGS_CLASS_PARAMETER_DESCRIPTION =
//            "If set, settings JSON will contain additional string value, named \""
//                    + SettingsSpecification.CLASS_KEY +
//                    "\" and containing category and name of these settings: "
//                    + "\"%%%\".";

    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_NAME = "_cs___extractSubSettings";
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION =
            "Extract sub-settings \"%%%\" from input \"settings\" JSON";
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DESCRIPTION =
            "If set, the parameters of this sub-chain are determined by the section \""
                    + SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" "
                    + "of the input settings JSON. If cleared, the parameters of this sub-chain "
                    + "are extracted directly from the top level of the input settings JSON. "
                    + "Parameters below have less priority, they are used only if there are no "
                    + "parameters with same names in the input settings JSON or its section \""
                    + SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" "
                    + "and if the following flag is not set.\n"
                    + "Normal state of this flag — set to true. Usually every sub-chain B1, B2, ... of your chain B "
                    + "is customized by some sub-settings of main JSON settings, specifying behaviour of the chain B."
                    + "However, sometimes you need just to pass all settings to next sub-chaining level "
                    + "without changes; then you can clear this flag.";
    public static final boolean EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DEFAULT = true;
    public static final String IGNORE_PARAMETERS_PARAMETER_NAME = "_cs___ignoreInputParameter";
    public static final String IGNORE_PARAMETERS_PARAMETER_CAPTION = "Ignore parameters below";
    public static final String IGNORE_PARAMETERS_PARAMETER_DESCRIPTION =
            "If set, the behavior is completely determined by the input settings port and internal settings "
                    + "of the sub-chain. All parameters below are ignored.\n"
                    + "We recommend to set this flag always in multi-chain configuration, "
                    + "if you allow and plan replacing some sub-chains in future.";
    public static final boolean IGNORE_PARAMETERS_PARAMETER_DEFAULT = false;
    // - Note: when we add IGNORE_PARAMETERS_PARAMETER_DESCRIPTION, we never add PORTS for sub-settings,
    // only (usually advanced) parameters - see UseSettings.addInputControlsAndPorts
    public static final String LOG_SETTINGS_PARAMETER_NAME = "_cs___logSettings";
    public static final String LOG_SETTINGS_PARAMETER_CAPTION = "Log settings";
    public static final String LOG_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, all settings, passed to the sub-chain, are logged with level WARNING. "
                    + "May be used for debugging needs.";

    public static final String PATH_PARENT_FOLDER_HINT =
            "Parent folder of the previous path.";
    public static final String PATH_FILE_NAME_HINT =
            "File/subfolder name of the previous path (without parent folders).";

    private static final InstalledPlatformsForTechnology SETTINGS_PLATFORMS =
            InstalledPlatformsForTechnology.getInstance(SETTINGS_TECHNOLOGY);
    private static final DefaultExecutorLoader<SettingsCombiner> SETTINGS_COMBINER_LOADER =
            new DefaultExecutorLoader<>("settings loader");

    static {
        globalLoaders().register(SETTINGS_COMBINER_LOADER);
    }

    private boolean recursiveScanning = true;
    private String settingsCombinerJsonContent = "";

    private boolean mainSettings;
    // - See comments to isMainChainSettings()
    // This variable affects to result of that function (when it is not overridden)
    // only in points 4 and later. Points 1-3 are always checked when mainSettings=false.
    private SettingsCombiner settingsCombiner = null;
    private ExecutorSpecification combineExecutorSpecification = null;
    private ExecutorSpecification splitExecutorSpecification = null;
    private ExecutorSpecification getNamesExecutorSpecification = null;

    public UseSettings() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_COMBINE_SPECIFICATION);
        addOutputScalar(OUTPUT_SPLIT_SPECIFICATION);
        addOutputScalar(OUTPUT_GET_NAMES_SPECIFICATION);
    }

    public static UseSettings getInstance() {
        return new UseSettings();
    }

    public static DefaultExecutorLoader<SettingsCombiner> settingsCombinerLoader() {
        return SETTINGS_COMBINER_LOADER;
    }

    public final boolean isRecursiveScanning() {
        return recursiveScanning;
    }

    public final UseSettings setRecursiveScanning(boolean recursiveScanning) {
        this.recursiveScanning = recursiveScanning;
        return this;
    }

    public final String getSettingsCombinerJsonContent() {
        return settingsCombinerJsonContent;
    }

    public final UseSettings setSettingsCombinerJsonContent(String settingsCombinerJsonContent) {
        this.settingsCombinerJsonContent = nonNull(settingsCombinerJsonContent);
        return this;
    }

    @Override
    public final UseSettings setFile(String file) {
        super.setFile(file);
        return this;
    }

    public SettingsCombiner settingsCombiner() {
        if (settingsCombiner == null) {
            throw new IllegalStateException("Settings combiner was not registered yet");
        }
        return settingsCombiner;
    }

    @Override
    public void process() {
        if (!this.getFile().trim().isEmpty()) {
            try {
                useSeveralPaths(completeSeveralFilePaths());
            } catch (IOException e) {
                throw new IOError(e);
            }
            return;
        }
        final String settingsCombinerJsonContent = this.settingsCombinerJsonContent.trim();
        if (!settingsCombinerJsonContent.isEmpty()) {
            useContent(settingsCombinerJsonContent);
            return;
        }
        throw new IllegalArgumentException("One of arguments \"Settings combiner JSON file/folder\" "
                + "or \"Settings combiner JSON content\" must be non-empty");
    }

    public void useSeveralPaths(List<Path> settingsSpecificationPaths) throws IOException {
        Objects.requireNonNull(settingsSpecificationPaths, "Null settings paths");
        mainSettings = false;
        // - we need to reinitialize this field for an improbable case of re-using this executor
        if (isMainChainSettings() && settingsSpecificationPaths.size() > 1) {
            throw new IllegalArgumentException("Processing several paths is not allowed here, but you specified "
                    + settingsSpecificationPaths.size() + " paths: " + settingsSpecificationPaths);
        }
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : settingsSpecificationPaths) {
            usePath(path, null, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path settingsSpecificationPath) throws IOException {
        usePath(settingsSpecificationPath, null, null);
    }

    public void usePath(
            Path settingsSpecificationPath,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(settingsSpecificationPath, "Null settings combiner path");
        mainSettings = false;
        // - we need to reinitialize this field for improbable case of re-using this executor
        // (well be set again in use() method)
        final List<SettingsSpecification> settingsSpecifications;
        if (Files.isDirectory(settingsSpecificationPath)) {
            settingsSpecifications = SettingsSpecification.readAllIfValid(
                    settingsSpecificationPath, recursiveScanning, isMainChainSettings());
            if (isExistingSettingsRequired() && settingsSpecifications.isEmpty()) {
                throw new IllegalArgumentException("No any standard chain settings was found in a folder "
                        + settingsSpecificationPath);
            }
        } else {
            settingsSpecifications =
                    Collections.singletonList(SettingsSpecification.read(settingsSpecificationPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        SettingsSpecification.checkIdDifference(settingsSpecifications);
        final int n = settingsSpecifications.size();
        final boolean showContent = isMainChainSettings() && n == 1;
        for (int i = 0; i < n; i++) {
            final SettingsSpecification settingsSpecification = settingsSpecifications.get(i);
            logDebug("Loading settings combiner " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + settingsSpecification.getSettingsSpecificationFile().toAbsolutePath() + "...");
            if (platform != null) {
                settingsSpecification.addTags(platform.getTags());
                settingsSpecification.setPlatformId(platform.getId());
                settingsSpecification.setPlatformCategory(platform.getCategory());
            }
            use(settingsSpecification);
            if (showContent && report != null) {
                report.append(settingsSpecification.jsonString());
            }
        }
        if (!showContent && report != null) {
            for (SettingsSpecification settingsSpecification : settingsSpecifications) {
                report.append(settingsSpecification.getSettingsSpecificationFile()).append("\n");
            }
        }
        if (n == 1) {
            if (combineExecutorSpecification != null) {
                getScalar(OUTPUT_COMBINE_SPECIFICATION).setTo(combineExecutorSpecification.jsonString());
            }
            if (splitExecutorSpecification != null) {
                getScalar(OUTPUT_SPLIT_SPECIFICATION).setTo(splitExecutorSpecification.jsonString());
            }
            if (getNamesExecutorSpecification != null) {
                getScalar(OUTPUT_GET_NAMES_SPECIFICATION).setTo(getNamesExecutorSpecification.jsonString());
//                System.out.println("!!!getNamesExecutorSpecification=" + getNamesExecutorSpecification.jsonString());
            }
        }
    }

    public void useContent(String settingsSpecificationContent) {
        final SettingsSpecification settingsSpecification = SettingsSpecification.valueOf(
                Jsons.toJson(settingsSpecificationContent),
                false);
        // - we don't require strict accuracy for JSON, entered in a little text area
        logDebug("Using settings combiner '"
                + settingsSpecification.getName() + "' from the text argument...");
        use(settingsSpecification);
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo(settingsSpecification.jsonString());
        }
    }

    public SettingsCombiner use(SettingsSpecification settingsSpecification) {
        this.mainSettings = settingsSpecification.isMain();
//      Below is a very bad idea (commented code): it leads to an effect, when the settings NAMES
//      will depend on the order of loading chains!
//      It is possible, when the same main chain settings are used both as actual main settings for some chain A
//      and as a settings specification for some another chain B.
//      Moreover, the category of the settings will depend on a fact, which sub-chain uses it.
//
//        final String chainName = removeExtension(contextFileName());
//      // - note: will be null while calling from MultiChain constructor
//        if (chainName != null) {
//
//            if (isMainChainSettings()) {
//                settingsSpecification.updateAutogeneratedName(chainName);
//                // - for example, it is important if main chain settings specification file is named
//                // "sc_specification.json", but the chain is named "com.xxxxx.ia.frame": we need to rename
//                // automatically chosen name "sc_specification" to "frame"
//            }
//
//            settingsSpecification.updateAutogeneratedCategory(chainName, isMainChainSettings());
//        }
        settingsSpecification.updateAutogeneratedCategory(isMainChainSettings());
        final String sessionId = getSessionId();
        final SettingsCombiner settingsCombiner = SettingsCombiner.valueOf(settingsSpecification);
        settingsCombiner.setCustomSettingsInformation(customSettingsInformation());
        final ExecutorSpecification combineSpecification = buildCombineSpecification(settingsCombiner);
        SETTINGS_COMBINER_LOADER.registerWorker(sessionId, combineSpecification, settingsCombiner);
        final ExecutorSpecification splitSpecification = buildSplitSpecification(settingsCombiner);
        if (splitSpecification != null) {
            SETTINGS_COMBINER_LOADER.registerWorker(sessionId, splitSpecification, settingsCombiner);
        }
        final ExecutorSpecification getNamesSpecification = buildGetNamesSpecification(settingsCombiner);
        if (getNamesSpecification != null) {
            SETTINGS_COMBINER_LOADER.registerWorker(sessionId, getNamesSpecification, settingsCombiner);
        }
        return this.settingsCombiner = settingsCombiner;
    }

    public void useAllInstalled() throws IOException {
        for (ExtensionSpecification.Platform platform : SETTINGS_PLATFORMS.installedPlatforms()) {
            if (platform.hasSpecifications()) {
                final long t1 = System.nanoTime();
                usePath(platform.specificationsFolder(), platform, null);
                final long t2 = System.nanoTime();
                logInfo(() -> String.format(Locale.US,
                        "Loading %s from %s: %.3f ms",
                        installedSpecificationsCaption(), platform.specificationsFolder(), (t2 - t1) * 1e-6));
            }
        }
    }

    // Note: for multi-chain settings, this method is called as a result of
    // the call "settingsFactory.use" inside MultiChain constructor
    public ExecutorSpecification buildCombineSpecification(SettingsCombiner settingsCombiner) {
        Objects.requireNonNull(settingsCombiner, "Null settingsCombiner");
        ExecutorSpecification result = buildCommon(newCombineSettings(), settingsCombiner);
        result.setExecutorId(settingsCombiner.id());
        result.setName(settingsCombiner.combineName());
        result.setDescription(settingsCombiner.combineDescription());
        addOwner(result, settingsCombiner);
        result.createOptionsIfAbsent().createRoleIfAbsent()
                .setName(settingsCombiner.name())
                .setSettings(true)
                .setResultPort(SettingsSpecification.SETTINGS)
                .setMain(isMainChainSettings());
        addInputControlsAndPorts(
                result,
                settingsCombiner,
                isMainChainSettings(),
                false,
                false);
        addOutputPorts(result, settingsCombiner);
        addSpecialOutputPorts(result);
        return combineExecutorSpecification = result;
    }

    public ExecutorSpecification buildSplitSpecification(SettingsCombiner settingsCombiner) {
        Objects.requireNonNull(settingsCombiner, "Null settingsCombiner");
        if (settingsCombiner.splitId() == null) {
            return splitExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newSplitSettings(), settingsCombiner);
        result.setExecutorId(settingsCombiner.splitId());
        result.setName(settingsCombiner.splitName());
        result.setDescription(settingsCombiner.splitDescription());
        addOwner(result, settingsCombiner);
        addOutputPorts(result, settingsCombiner);
        addSpecialOutputPorts(result);
        return splitExecutorSpecification = result;
    }

    public ExecutorSpecification buildGetNamesSpecification(SettingsCombiner settingsCombiner) {
        Objects.requireNonNull(settingsCombiner, "Null settingsCombiner");
        if (settingsCombiner.getNamesId() == null) {
            return getNamesExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newGetNamesOfSettings(), settingsCombiner);
        final Map<String, ExecutorSpecification.ControlConf> executorControls =
                new LinkedHashMap<>(result.getControls());
        executorControls.get("resultType").setCaption("Result type");
        executorControls.get("resultJsonKey").setCaption("Key in result JSON").setDefaultStringValue("names");
        for (ExecutorSpecification.ControlConf controlConf : executorControls.values()) {
            if (controlConf.getValueType() == ParameterValueType.BOOLEAN
                    && controlConf.getName().startsWith("extract")) {
                controlConf.setDefaultJsonValue(JsonValue.TRUE);
            }
        }
        result.setControls(executorControls);
        result.setExecutorId(settingsCombiner.getNamesId());
        result.setName(settingsCombiner.getNamesName());
        result.setDescription(settingsCombiner.getNamesDescription());
        addOwner(result, settingsCombiner);
        addSpecialOutputPorts(result);
        return getNamesExecutorSpecification = result;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        final UseSettings useSettings = getInstance();
        useSettings.setSessionId(GLOBAL_SHARED_SESSION_ID);
        useSettings.useAllInstalled();
    }

    // Used for adding controls and ports to ExecuteSubChain executor
    public static void addExecuteSubChainControlsAndPorts(
            ExecutorSpecification result,
            SettingsCombiner settingsCombiner) {
        addInputControlsAndPorts(result, settingsCombiner, false, true, false);
    }

    // Used for adding controls and ports to ExecuteMultiChain executor
    public static void addExecuteMultiChainControlsAndPorts(
            ExecutorSpecification result,
            SettingsCombiner settingsCombiner) {
        addInputControlsAndPorts(result, settingsCombiner, false, false, true);
    }

    // Note: overridden in UseChainSettings (where it always returns true)

    /**
     * Returns <code>true</code> if this setting executor is <b>main</b> (i.e. a main settings set for some chain).
     * In this case:
     * <ol>
     *     <li>if this function uses a directory, then only "main-settings-combiner" JSONs will be loaded;</li>
     *     <li>at least 1 actual executor must exist in the list of all executors, used by this function (but it
     *     can be changed by overriding {@link #isExistingSettingsRequired()});</li>
     *     <li>list of more than 1 paths is not supported;</li>
     *     <li>executors will contain "owner" section and the "role" will be set to "main":
     *     it helps execution system to detect, which
     *     from execution blocks is the "main" settings for the current chain &mdash; its owner ID will
     *     the ID of this chain,
     *     (this behaviour actually depends on {@link #isNeedToAddOwner()} method,
     *     which may be overridden, for example in mult-chains);</li>
     *     <li>Java classes of executors with alternate names will be used ({@link CombineChainSettings}
     *     instead of {@link CombineSettings} etc.)</li>
     *     <li>auto-generated category name will be shorter: "org.algart.ia" instead of "org.algart.ia.frame".</li>
     * </ol>
     * <p>This function (by default) returns a value of internal field, that is loaded from the specification file
     * (excepting points 1-3, where this function returns <code>false</code>).
     * In {@link UseChainSettings} class, this function is overridden always returns <code>true</code>.
     * So, the behaviour is usually uniquely defined by the specification file.</p>
     * <p>There is only one exception. If, in some chain A, you call
     * {@link UseChainSettings} for <i>usual</i> (non-main) settings, and also,
     * in other chain B, load the same settings via {@link UseSettings}, then the results will depend
     * on an order of loading chains A and B. However, it is usually not a problem, because additions
     * for main settings do not violate typical usage of non-main settings.
     * In any case, we do not recommend doing this &mdash; please use {@link UseChainSettings} only
     * with actually main settings.</p>
     *
     * @return whether this settings executor is main.
     */
    protected boolean isMainChainSettings() {
        return mainSettings;
    }

    protected boolean isNeedToAddOwner() {
        return isMainChainSettings();
    }

    protected boolean isExistingSettingsRequired() {
        return isMainChainSettings();
    }

    protected CombineSettings newCombineSettings() {
        return isMainChainSettings() ? new CombineChainSettings() : new CombineSettings();
    }

    protected SplitSettings newSplitSettings() {
        return isMainChainSettings() ? new SplitChainSettings() : new SplitSettings();
    }

    protected GetNamesOfSettings newGetNamesOfSettings() {
        return isMainChainSettings() ? new GetNamesOfChainSettings() : new GetNamesOfSettings();
    }

    protected Object customSettingsInformation() {
        return null;
    }

    String installedSpecificationsCaption() {
        return "installed settings combiner specifications";
    }

    private void addOwner(ExecutorSpecification result, SettingsCombiner settingsCombiner) {
        if (isNeedToAddOwner()) {
            final Object contextId = getContextId();
            final String ownerId = getOwnerId();
            if (contextId != null || ownerId != null) {
                // So, we inform the execution system, which chain has actually created this settings combiner
                // (by UseChainSettings static executor). It will help execution system to detect, which
                // from execution blocks is the "main" settings for the current chain: its owner ID will
                // be equal to ID of this chain.
                result.createOptionsIfAbsent().createOwnerIfAbsent()
                        .setName(getContextName())
                        .setId(ownerId)
                        .setContextId(contextId == null ? null : contextId.toString());
            }
        }
    }

    private static ExecutorSpecification buildCommon(Executor executor, SettingsCombiner settingsCombiner) {
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(executor);
        // - adds JavaConf, (maybe) parameters and some ports
        result.setSourceInfo(settingsCombiner.settingsSpecificationFile(), null);
        if (settingsCombiner.hasPlatformId()) {
            result.setPlatformId(settingsCombiner.platformId());
        }
        result.setLanguage(SETTINGS_LANGUAGE);
        result.setTags(settingsCombiner.tags());
        result.setCategory(ExecutorSpecification.correctDynamicCategory(settingsCombiner.category()));
        result.updateCategoryPrefix(settingsCombiner.platformCategory());
        return result;
    }

    private static void addInputControlsAndPorts(
            ExecutorSpecification result,
            SettingsCombiner settingsCombiner,
            boolean mainForChain,
            boolean subChainMode,
            boolean noSystemFlags) {
        if (mainForChain) {
            result.addControl(new ExecutorSpecification.ControlConf()
                    .setName(ALL_SETTINGS_PARAMETER_NAME)
                    .setCaption(ALL_SETTINGS_PARAMETER_CAPTION)
                    .setDescription(ALL_SETTINGS_PARAMETER_DESCRIPTION)
                    .setValueType(ParameterValueType.SETTINGS)
                    .setDefaultJsonValue(Jsons.newEmptyJson())
                    .setAdvanced(true));
        }
        if (!noSystemFlags) {
            final String settingsName = settingsCombiner.name();
            /*
            // We decided not to add this information: the settings are usually created by external dashboard,
            // direct using combiner is not a typical case
            if (!subChainMode) {
                addBooleanControl(result,
                        ADD_SETTINGS_CLASS_PARAMETER_NAME,
                        ADD_SETTINGS_CLASS_PARAMETER_CAPTION.replace("%%%", settingsName),
                        ADD_SETTINGS_CLASS_PARAMETER_DESCRIPTION.replace("%%%", settingsName),
                        false,
                        true);
            }
             */
            if (subChainMode) {
                addBooleanControl(result,
                        EXTRACT_SUB_SETTINGS_PARAMETER_NAME,
                        EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION.replace("%%%", settingsName),
                        EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DESCRIPTION.replace("%%%", settingsName),
                        EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DEFAULT,
                        false);
            }
            if (settingsCombiner.hasPathControl()) {
                addBooleanControl(result,
                        ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                        ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION,
                        ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION,
                        SettingsCombiner.ABSOLUTE_PATHS_DEFAULT_VALUE,
                        false);
            }
        }
        if (subChainMode && !noSystemFlags) {
            addBooleanControl(result,
                    LOG_SETTINGS_PARAMETER_NAME,
                    LOG_SETTINGS_PARAMETER_CAPTION,
                    LOG_SETTINGS_PARAMETER_DESCRIPTION,
                    false,
                    true);
            addBooleanControl(result,
                    IGNORE_PARAMETERS_PARAMETER_NAME,
                    IGNORE_PARAMETERS_PARAMETER_CAPTION,
                    IGNORE_PARAMETERS_PARAMETER_DESCRIPTION,
                    IGNORE_PARAMETERS_PARAMETER_DEFAULT,
                    false);
        }
        final SettingsSpecification specification = settingsCombiner.specification();
        final Map<String, SettingsSpecification.ControlConfExtension> controlExtensions =
                specification.getControlExtensions();
        for (Map.Entry<String, ExecutorSpecification.ControlConf> entry : specification.getControls().entrySet()) {
            final String name = entry.getKey();
            ExecutorSpecification.ControlConf controlConf = entry.getValue().clone();
            if (controlConf.getValueType().isSettings() && !subChainMode) {
                final ExecutorSpecification.PortConf portConf = new ExecutorSpecification.PortConf();
                portConf.setName(name);
                portConf.setCaption(controlConf.getCaption());
                portConf.setHint(controlConf.getDescription());
                portConf.setValueType(DataType.SCALAR);
                result.addInPort(portConf);
            }
            final SettingsSpecification.ControlConfExtension controlExtension = controlExtensions.get(name);
            if (controlExtension != null) {
                controlExtension.load(specification);
                controlExtension.completeControlConf(controlConf);
            }
            result.addControl(controlConf);
        }
    }

    private static void addOutputPorts(ExecutorSpecification result, SettingsCombiner settingsCombiner) {
        final Map<String, ExecutorSpecification.ControlConf> controls = settingsCombiner.specification().getControls();
        for (ExecutorSpecification.ControlConf controlConf : controls.values()) {
            final String name = SettingsCombiner.portName(controlConf);
            final ExecutorSpecification.PortConf portConf = new ExecutorSpecification.PortConf()
                    .setName(name)
                    .setCaption(controlConf.getCaption())
                    .setHint(controlConf.getDescription())
                    .setValueType(DataType.SCALAR);
            if (controlConf.getValueType().isSettings()) {
                portConf.setAdvanced(true);
            }
            result.addOutPort(portConf);
            if (controlConf.getValueType() == ParameterValueType.STRING && controlConf.getEditionType().isPath()) {
                result.addOutPort(new ExecutorSpecification.PortConf()
                        .setName(name + SettingsCombiner.PATH_PARENT_FOLDER_SUFFIX)
                        .setHint(PATH_PARENT_FOLDER_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
                result.addOutPort(new ExecutorSpecification.PortConf()
                        .setName(name + SettingsCombiner.PATH_FILE_NAME_SUFFIX)
                        .setHint(PATH_FILE_NAME_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
            }
        }
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        // - to be on the safe side (maybe, the user defined the output port with the same name)
        result.addSystemExecutorIdPort();
        if (result.hasPlatformId()) {
            result.addSystemPlatformIdPort();
            result.addSystemResourceFolderPort();
            // - resource folder may be especially convenient for settings,
            // where we have no functions like "ReadScalar" with ability
            // to replace ${resource} string (see PathPropertyReplacement)
        }
        if (!result.getOutPorts().containsKey(SETTINGS_NAME_OUTPUT_NAME)) {
            result.addOutPort(new ExecutorSpecification.PortConf()
                    .setName(SETTINGS_NAME_OUTPUT_NAME)
                    .setCaption(SETTINGS_NAME_OUTPUT_CAPTION)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    private static void addBooleanControl(
            ExecutorSpecification result,
            String name,
            String caption,
            String description,
            boolean defaultValue,
            boolean advanced) {
        result.addControl(new ExecutorSpecification.ControlConf()
                .setName(name)
                .setCaption(caption)
                .setDescription(description)
                .setValueType(ParameterValueType.BOOLEAN)
                .setDefaultJsonValue(Jsons.toJsonBooleanValue(defaultValue))
                .setAdvanced(advanced));
    }
}
