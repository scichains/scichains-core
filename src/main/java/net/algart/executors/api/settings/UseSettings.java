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

package net.algart.executors.api.settings;

import jakarta.json.JsonValue;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledPlatformsForTechnology;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.InstantiationMode;
import net.algart.executors.modules.core.common.io.FileOperation;
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

    public static final String SETTINGS_NAME_OUTPUT_NAME = "_ss___settings_name";
    public static final String SETTINGS_NAME_OUTPUT_CAPTION = "settings_name";
    public static final String SETTINGS_ID_OUTPUT_NAME = "_ss___settings_id";
    public static final String SETTINGS_ID_OUTPUT_CAPTION = "Settings\u00A0ID";
    public static final String SETTINGS_ID_OUTPUT_HINT = "ID of the corresponding settings executor";
    public static final String ALL_SETTINGS_PARAMETER_NAME = SettingsSpecification.SETTINGS;
    public static final String ALL_SETTINGS_PARAMETER_CAPTION = "settings (all)";
    public static final String ALL_SETTINGS_PARAMETER_DESCRIPTION =
            "If contains non-empty string and if the input \"settings\" port is NOT initialized, " +
                    "it should be JSON, containing ALL settings for this combiner " +
                    "(in other words, full settings set for the chain). " +
                    "It will be used instead of input \"settings\" JSON. " +
                    "In this case, as well as \"settings\" port is initialized by some JSON, " +
                    "all parameters below will be overridden by this JSON.\n" +
                    "Note: \"overriding\" does not mean \"replacing\": if JSON does not contain some settings, " +
                    "this function will use settings from parameters below or from other input settings ports.";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_NAME = "_cs___absolutePaths";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION = "Auto-replace paths to absolute";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION =
            "If set, all parameters below, describing paths to files or folders, are automatically replaced " +
                    "with full absolute disk paths. It can be useful if you need to pass these parameters " +
                    "to other sub-chains, that probably work in other \"current\" directories.\n" +
                    "Also, in this case you can use in such parameters Java system properties: " +
                    "\"${name}\", like \"${java.io.tmpdir}\", and executor system properties \"${path.name.ext}\", " +
                    "\"${path.name}\", \"${file.name.ext}\", \"${file.name}\" " +
                    "(chain path/file name with/without extension).\n" +
                    "NOTE: this flag does not affect the settings JSON passed via \"settings (all)\" " +
                    "parameter or via the input \"settings\" port.";

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
            "If set, the parameters of this sub-chain are determined by the section \"" +
                    SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" " +
                    "of the input settings JSON. If cleared, the parameters of this sub-chain " +
                    "are extracted directly from the top level of the input settings JSON. " +
                    "Parameters below have less priority, they are used only if there are no " +
                    "parameters with same names in the input settings JSON or its section \"" +
                    SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" " +
                    "and if the flag \"Ignore parameters below\" is not set.\n" +
                    "Normal state of this flag — set to true. Usually every sub-chain B1, B2, ... of your chain B " +
                    "is customized by some sub-settings of main JSON settings, specifying behaviour of the chain B." +
                    "However, sometimes you need just to pass all settings to next sub-chaining level " +
                    "without changes; then you can clear this flag.";
    public static final boolean EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DEFAULT = true;
    public static final String IGNORE_PARAMETERS_PARAMETER_NAME = "_cs___ignoreInputParameter";
    public static final String IGNORE_PARAMETERS_PARAMETER_CAPTION = "Ignore parameters below";
    public static final String IGNORE_PARAMETERS_PARAMETER_DESCRIPTION =
            "If set, the behavior is completely determined by the input settings port and internal settings " +
                    "of the sub-chain. All parameters below are not included into the settings JSON " +
                    "even if there is no input settings.\n" +
                    "However: if there are parameters in the chain that are specified in the chain blocks " +
                    "and not in the JSON, they are copied from the corresponding parameters below in any case.";
    public static final boolean IGNORE_PARAMETERS_PARAMETER_DEFAULT = false;
    // - Note: when we add IGNORE_PARAMETERS_PARAMETER_DESCRIPTION, we never add PORTS for sub-settings,
    // only (usually advanced) parameters - see UseSettings.addInputControlsAndPorts
    public static final String LOG_SETTINGS_PARAMETER_NAME = "_cs___logSettings";
    public static final String LOG_SETTINGS_PARAMETER_CAPTION = "Log settings";
    public static final String LOG_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, all settings, passed to the sub-chain, are logged with level WARNING. " +
                    "Can be used for debugging needs.";

    public static final String PATH_PARENT_FOLDER_HINT =
            "Parent folder of the previous path.";
    public static final String PATH_FILE_NAME_HINT =
            "File/subfolder name of the previous path (without parent folders).";

    private static final InstalledPlatformsForTechnology SETTINGS_PLATFORMS =
            InstalledPlatformsForTechnology.of(SETTINGS_TECHNOLOGY);
    private static final DefaultExecutorLoader<Settings> SETTINGS_LOADER =
            new DefaultExecutorLoader<>("settings loader");

    static {
        globalLoaders().register(SETTINGS_LOADER);
    }

    private boolean recursiveScanning = true;
    private String settingsCombinerJsonContent = "";

    private boolean mainSettings;
    // - See comments to isMainChainSettings()
    // This variable affects the result of that function (when it is not overridden)
    // only in points 4 and later. Points 1-3 are always checked when mainSettings=false.
    private Settings settings = null;
    private ExecutorSpecification combineExecutorSpecification = null;
    private ExecutorSpecification splitExecutorSpecification = null;
    private ExecutorSpecification getNamesExecutorSpecification = null;

    private volatile ExecutorFactory executorFactory = null;

    public UseSettings() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_COMBINE_SPECIFICATION);
        addOutputScalar(OUTPUT_SPLIT_SPECIFICATION);
        addOutputScalar(OUTPUT_GET_NAMES_SPECIFICATION);
    }

    public static UseSettings getInstance(String sessionId) {
        return setSession(new UseSettings(), sessionId);
    }

    public static UseSettings getSharedInstance() {
        return setShared(new UseSettings());
    }

    public static DefaultExecutorLoader<Settings> settingsLoader() {
        return SETTINGS_LOADER;
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

    public Settings settings() {
        if (settings == null) {
            throw new IllegalStateException("Settings were not registered yet");
        }
        return settings;
    }

    public static SettingsExecutor newSharedCombine(Path file, InstantiationMode instantiationMode)
            throws IOException {
        return newSharedCombine(SettingsSpecification.read(file), instantiationMode);
    }

    public static SettingsExecutor newSharedCombine(
            SettingsSpecification specification,
            InstantiationMode instantiationMode) {
        return getSharedInstance().newCombine(specification, instantiationMode);
    }

    public SettingsExecutor newCombine(Path chainFile, InstantiationMode instantiationMode) throws IOException {
        return newCombine(SettingsSpecification.read(chainFile), instantiationMode);
    }

    public SettingsExecutor newCombine(SettingsSpecification specification, InstantiationMode instantiationMode) {
        //noinspection resource
        return use(specification).newCombine(executorFactory(), instantiationMode);
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
        throw new IllegalArgumentException("One of arguments \"Settings JSON file/folder\" "
                + "or \"Settings JSON content\" must be non-empty");
    }

    public UseSettings setExecutorFactory(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
        return this;
    }

    public ExecutorFactory executorFactory() {
        final String sessionId = getSessionId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot register new chain: session ID was not set");
        }
        var executorFactory = this.executorFactory;
        if (executorFactory == null) {
            this.executorFactory = executorFactory = globalLoaders().newFactory(sessionId);
        }
        return executorFactory;
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

    public int usePath(
            Path settingsSpecificationPath,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(settingsSpecificationPath, "Null settings specification path");
        mainSettings = false;
        // - we need to reinitialize this field for an improbable case of re-using this executor
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
            logDebug("Loading settings " + (n > 1 ? (i + 1) + "/" + n + " " : "")
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
        return n;
    }

    public void useContent(String settingsSpecificationContent) {
        final SettingsSpecification settingsSpecification = SettingsSpecification.of(
                Jsons.toJson(settingsSpecificationContent),
                false);
        // - we don't require strict accuracy for JSON, entered in a little text area
        logDebug("Using settings '"
                + settingsSpecification.getName() + "' from the text argument...");
        use(settingsSpecification);
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo(settingsSpecification.jsonString());
        }
    }

    public Settings use(SettingsSpecification settingsSpecification) {
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
        final Settings settings = newSettings(settingsSpecification);
        final ExecutorSpecification combineSpecification = buildCombineSpecification(settings);
        SETTINGS_LOADER.registerWorker(sessionId, combineSpecification, settings);
        final ExecutorSpecification splitSpecification = buildSplitSpecification(settings);
        if (splitSpecification != null) {
            SETTINGS_LOADER.registerWorker(sessionId, splitSpecification, settings);
        }
        final ExecutorSpecification getNamesSpecification = buildGetNamesSpecification(settings);
        if (getNamesSpecification != null) {
            SETTINGS_LOADER.registerWorker(sessionId, getNamesSpecification, settings);
        }
        return this.settings = settings;
    }

    public void useAllInstalled() throws IOException {
        for (ExtensionSpecification.Platform platform : SETTINGS_PLATFORMS.installedPlatforms()) {
            if (platform.hasSpecifications()) {
                final long t1 = System.nanoTime();
                final int n = usePath(platform.specificationsFolder(), platform, null);
                final long t2 = System.nanoTime();
                logInfo(() -> String.format(Locale.US,
                        "Loading %d %s from %s: %.3f ms",
                        n, installedSpecificationsCaption(), platform.specificationsFolder(),
                        (t2 - t1) * 1e-6));
            }
        }
    }

    // Note: for multi-chain settings, this method is called as a result of
    // the call "settingsFactory.use" inside MultiChain constructor
    public ExecutorSpecification buildCombineSpecification(Settings settings) {
        Objects.requireNonNull(settings, "Null settings");
        ExecutorSpecification result = buildCommon(newCombineSettings(), settings);
        result.setId(settings.id());
        result.setName(settings.combineName());
        result.setDescription(settings.combineDescription());
        addOwner(result, settings);
        result.createOptionsIfAbsent().createRoleIfAbsent()
                .setClassName(settings.className())
                .setSettings(true)
                .setResultPort(SettingsSpecification.SETTINGS)
                .setMain(isMainChainSettings());
        addInputControlsAndPorts(
                result,
                settings,
                isMainChainSettings(),
                false,
                false);
        result.setSettings(settings.specification());
        addOutputPorts(result, settings);
        addSpecialOutputPorts(result);
        return combineExecutorSpecification = result;
    }

    public ExecutorSpecification buildSplitSpecification(Settings settings) {
        Objects.requireNonNull(settings, "Null settings");
        if (settings.splitId() == null) {
            return splitExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newSplitSettings(), settings);
        result.setId(settings.splitId());
        result.setName(settings.splitName());
        result.setDescription(settings.splitDescription());
        addOwner(result, settings);
        addOutputPorts(result, settings);
        addSpecialOutputPorts(result);
        return splitExecutorSpecification = result;
    }

    public ExecutorSpecification buildGetNamesSpecification(Settings settings) {
        Objects.requireNonNull(settings, "Null settings");
        if (settings.getNamesId() == null) {
            return getNamesExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newGetNamesOfSettings(), settings);
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
        result.setId(settings.getNamesId());
        result.setName(settings.getNamesName());
        result.setDescription(settings.getNamesDescription());
        addOwner(result, settings);
        addSpecialOutputPorts(result);
        return getNamesExecutorSpecification = result;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        getSharedInstance().useAllInstalled();
    }

    // Used for adding controls and ports to InterpretSubChain executor
    public static void addSubChainControlsAndPorts(ExecutorSpecification result, Settings settings) {
        final boolean withSettings = settings != null;
        if (withSettings) {
            addInputControlsAndPorts(
                    result, settings, false, true, false);
        }
        addSystemOutputPorts(result, withSettings);
    }

    // Used for adding controls and ports to InterpretMultiChain executor
    public static void addMultiChainControlsAndPorts(ExecutorSpecification result, Settings settings) {
        final boolean withSettings = settings != null;
        if (withSettings) {
            addInputControlsAndPorts(result, settings, false, false, true);
        }
        addSystemOutputPorts(result, withSettings);
    }

    // Note: overridden in UseChainSettings (where it always returns true)

    /**
     * Returns <code>true</code> if this setting executor is <b>main</b> (i.e. a main settings set for some chain).
     * In this case:
     * <ol>
     *     <li>if this function uses a directory, then only "main-settings" JSONs will be loaded;</li>
     *     <li>at least 1 actual executor must exist in the list of all executors, used by this function (but it
     *     can be changed by overriding {@link #isExistingSettingsRequired()});</li>
     *     <li>a list of more than one path is not supported;</li>
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
     * So, the behavior is usually uniquely defined by the specification file.</p>
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

    protected Settings newSettings(SettingsSpecification settingsSpecification) {
        return Settings.of(settingsSpecification);
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
        return "installed settings specifications";
    }

    private void addOwner(ExecutorSpecification result, Settings settings) {
        if (isNeedToAddOwner()) {
            final Object contextId = getContextId();
            final String ownerId = getOwnerId();
            if (contextId != null || ownerId != null) {
                // So, we inform the execution system, which chain has actually created these settings
                // (by UseChainSettings static executor). It will help the execution system to detect, which
                // from execution blocks is the "main" settings for the current chain: its owner ID will
                // be equal to ID of this chain.
                result.createOptionsIfAbsent().createOwnerIfAbsent()
                        .setName(getContextName())
                        .setId(ownerId)
                        .setContextId(contextId == null ? null : contextId.toString());
            }
        }
    }

    private static ExecutorSpecification buildCommon(Executor executor, Settings settings) {
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(executor);
        // - adds JavaConf, (maybe) parameters and some ports
        result.setSourceInfo(settings.settingsSpecificationFile(), null);
        if (settings.hasPlatformId()) {
            result.setPlatformId(settings.platformId());
        }
        result.setLanguage(SETTINGS_LANGUAGE);
        result.setTags(settings.tags());
        result.setCategory(ExecutorSpecification.correctDynamicCategory(settings.category()));
        result.createOptionsIfAbsent().createServiceIfAbsent().setSettingsId(settings.id());
        result.updateCategoryPrefix(settings.platformCategory());
        return result;
    }

    private static void addInputControlsAndPorts(
            ExecutorSpecification result,
            Settings settings,
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
            final String settingsName = settings.name();
            /*
            // We decided not to add this information: the settings are usually created by external dashboard,
            // direct using settings combiners is not a typical case
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
            if (settings.hasPathControl()) {
                addBooleanControl(result,
                        ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                        ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION,
                        ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION,
                        Settings.ABSOLUTE_PATHS_DEFAULT_VALUE,
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
                    true);
        }
        final SettingsSpecification specification = settings.specification();
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
                result.addInputPort(portConf);
            }
            final SettingsSpecification.ControlConfExtension controlExtension = controlExtensions.get(name);
            if (controlExtension != null) {
                controlExtension.load(specification);
                controlExtension.completeControlConf(controlConf);
            }
            // Here we could set controlConf.setValueClass, when it does not exist, according the current
            // settings category:
            //      ExecutorSpecification.defaultClassName(specification.getCategory(), name)
            // But this is a bad idea!
            // Then we will not be able to find it from a chain (settings) with another category,
            // but this is sometimes necessary.
            // It is better to keep non-specified value class: this is a signal that we need more smart search.
            result.addControl(controlConf);
        }
    }

    private static void addOutputPorts(ExecutorSpecification result, Settings settings) {
        final Map<String, ExecutorSpecification.ControlConf> controls = settings.specification().getControls();
        for (ExecutorSpecification.ControlConf controlConf : controls.values()) {
            final String name = Settings.portName(controlConf);
            final ExecutorSpecification.PortConf portConf = new ExecutorSpecification.PortConf()
                    .setName(name)
                    .setCaption(controlConf.getCaption())
                    .setHint(controlConf.getDescription())
                    .setValueType(DataType.SCALAR);
            if (controlConf.getValueType().isSettings()) {
                portConf.setAdvanced(true);
            }
            result.addOutputPort(portConf);
            if (controlConf.getValueType() == ParameterValueType.STRING && controlConf.getEditionType().isPath()) {
                result.addOutputPort(new ExecutorSpecification.PortConf()
                        .setName(name + Settings.PATH_PARENT_FOLDER_SUFFIX)
                        .setHint(PATH_PARENT_FOLDER_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
                result.addOutputPort(new ExecutorSpecification.PortConf()
                        .setName(name + Settings.PATH_FILE_NAME_SUFFIX)
                        .setHint(PATH_FILE_NAME_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
            }
        }
    }

    private static void addSystemOutputPorts(ExecutorSpecification result, boolean addSettingsIdPort) {
        result.addSystemExecutorIdPort();
        if (addSettingsIdPort) {
            result.addOutputPort(new ExecutorSpecification.PortConf()
                    .setName(SETTINGS_ID_OUTPUT_NAME)
                    .setCaption(SETTINGS_ID_OUTPUT_CAPTION)
                    .setHint(SETTINGS_ID_OUTPUT_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
        if (result.hasPlatformId()) {
            result.addSystemPlatformIdPort();
            result.addSystemResourceFolderPort();
            // - resource folder may be especially convenient for settings,
            // where we have no functions like "ReadScalar" with the ability
            // to replace ${resource} string (see PathPropertyReplacement)
        }
    }

    private static void addSpecialOutputPorts(ExecutorSpecification result) {
        addSystemOutputPorts(result, false);
        if (!result.getOutputPorts().containsKey(SETTINGS_NAME_OUTPUT_NAME)) {
            result.addOutputPort(new ExecutorSpecification.PortConf()
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
