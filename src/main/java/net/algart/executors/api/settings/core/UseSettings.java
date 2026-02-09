/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.api.settings.core;

import jakarta.json.JsonValue;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.extensions.InstalledPlatformsForTechnology;
import net.algart.executors.api.parameters.ValueType;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.system.*;
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
                    "it should be a JSON containing ALL settings for this function " +
                    "(in other words, the full settings set for the chain). " +
                    "If the input \"settings\" port is initialized, it plays the same role: ALL settings. " +
                    "In both cases, all parameters below and all other input ports (with sub-settings sections) " +
                    "are overridden by this JSON.\n" +
                    "Note: \"overriding\" does not mean \"replacing\": if the JSON does not contain some settings, " +
                    "this function will use the settings from the parameters below / other input settings ports.";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_NAME = "_cs___absolutePaths";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION = "Auto-replace paths to absolute";
    public static final String ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION =
            "If set, all parameters below, describing paths to files or folders, are automatically replaced " +
                    "with full absolute disk paths. It can be useful if you need to pass these parameters " +
                    "to other chains, that probably work in other \"current\" directories.\n" +
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
    public static final String EXTRACT_SUB_SETTINGS_PARAMETER_FOR_CHAIN_DESCRIPTION =
            "If set, the parameters of this chain are determined by the section \"" +
                    SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" " +
                    "of the input settings JSON. If cleared, the parameters of this chain " +
                    "are extracted directly from the top level of the input settings JSON. " +
                    "Parameters below have less priority, they are used only if there are no " +
                    "parameters with same names in the input settings JSON or its section \"" +
                    SettingsSpecification.SUBSETTINGS_PREFIX + "%%%\" " +
                    "and if the flag \"Ignore parameters below\" is not set.\n" +
                    "Normal state of this flag — set to true. Usually every chain B1, B2, ... of your chain B " +
                    "is customized by some sub-settings of main JSON settings, specifying behaviour of the chain B." +
                    "However, sometimes you need just to pass all settings to next chaining level " +
                    "without changes; then you can clear this flag.";
    public static final boolean EXTRACT_SUB_SETTINGS_PARAMETER_FOR_CHAIN_DEFAULT = true;
    public static final String IGNORE_PARAMETERS_PARAMETER_NAME = "_cs___ignoreInputParameter";
    public static final String IGNORE_PARAMETERS_PARAMETER_CAPTION = "Ignore parameters below";
    public static final String IGNORE_PARAMETERS_PARAMETER_DESCRIPTION =
            "If set, the behavior is completely determined by the input settings port and internal settings " +
                    "of the chain. All parameters below are not included into the settings JSON " +
                    "even if there is no input settings.\n" +
                    "However: if there are parameters in the chain that are specified in the chain blocks " +
                    "and not in the JSON, they are copied from the corresponding parameters below in any case.";
    public static final boolean IGNORE_PARAMETERS_PARAMETER_DEFAULT = false;
    // - Note: when we add IGNORE_PARAMETERS_PARAMETER_DESCRIPTION, we never add PORTS for sub-settings,
    // only (usually advanced) parameters - see UseSettings.addInputControlsAndPorts
    public static final String LOG_SETTINGS_PARAMETER_NAME = "_cs___logSettings";
    public static final String LOG_SETTINGS_PARAMETER_CAPTION = "Log settings";
    public static final String LOG_SETTINGS_PARAMETER_DESCRIPTION =
            "If set, all settings, passed to the chain, are logged with level WARNING. " +
                    "Can be used for debugging needs.";

    public static final String PATH_PARENT_FOLDER_HINT =
            "Parent folder of the previous path.";
    public static final String PATH_FILE_NAME_HINT =
            "File/subfolder name of the previous path (without parent folders).";

    private static final InstalledPlatformsForTechnology SETTINGS_PLATFORMS =
            InstalledPlatformsForTechnology.of(SETTINGS_TECHNOLOGY);
    private static final DefaultExecutorLoader<SettingsBuilder> SETTINGS_LOADER =
            new DefaultExecutorLoader<>("settings loader");

    static {
        globalLoaders().register(SETTINGS_LOADER);
    }

    private boolean recursiveScanning = true;
    private String settingsSpecification = "";

    private boolean mainSettings;
    // - See comments to isMainChainSettings()
    // This variable affects the result of that function (when it is not overridden)
    // only in points 4 and later. Points 1-3 are always checked when mainSettings=false.
    private SettingsBuilder settingsBuilder = null;
    private ExecutorSpecification combineExecutorSpecification = null;
    private ExecutorSpecification splitExecutorSpecification = null;
    private ExecutorSpecification getNamesExecutorSpecification = null;

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

    public static DefaultExecutorLoader<SettingsBuilder> settingsLoader() {
        return SETTINGS_LOADER;
    }

    public final boolean isRecursiveScanning() {
        return recursiveScanning;
    }

    public final UseSettings setRecursiveScanning(boolean recursiveScanning) {
        this.recursiveScanning = recursiveScanning;
        return this;
    }

    public final String getSettingsSpecification() {
        return settingsSpecification;
    }

    public final UseSettings setSettingsSpecification(String settingsSpecification) {
        this.settingsSpecification = nonNull(settingsSpecification);
        return this;
    }

    @Override
    public final UseSettings setFile(String file) {
        super.setFile(file);
        return this;
    }

    public SettingsBuilder settingsBuilder() {
        if (settingsBuilder == null) {
            throw new IllegalStateException("Settings were not registered yet");
        }
        return settingsBuilder;
    }

    public static CombineSettings newSharedExecutor(ExecutorFactory factory, Path file) throws IOException {
        return newSharedExecutor(factory, SettingsSpecification.read(file));
    }

    public static CombineSettings newSharedExecutor(ExecutorFactory factory, SettingsSpecification specification) {
        return getSharedInstance().newExecutor(factory, specification);
    }

    public CombineSettings newExecutor(ExecutorFactory factory, Path file) throws IOException {
        Objects.requireNonNull(factory, "Null executor factory");
        return newExecutor(factory, SettingsSpecification.read(file));
    }

    public CombineSettings newExecutor(ExecutorFactory factory, SettingsSpecification specification) {
        Objects.requireNonNull(factory, "Null executor factory");
        return factory.newExecutor(CombineSettings.class, use(specification).id(), CreateMode.REQUEST_DEFAULT);
        // - for settings, we can be sure that the default output port is enough for normal using this executor
    }

    @Override
    public void process() {
        if (hasFile()) {
            try {
                useSeveralPaths(completeSeveralFilePaths());
            } catch (IOException e) {
                throw new IOError(e);
            }
            return;
        }
        final String settingsSpecification = this.settingsSpecification.trim();
        if (!settingsSpecification.isEmpty()) {
            useContent(settingsSpecification);
            return;
        }
        throw new IllegalArgumentException("One of arguments \"Settings JSON file/folder\" "
                + "or \"Settings specification\" must be non-empty");
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
            Path path,
            ExtensionSpecification.Platform platform,
            StringBuilder report)
            throws IOException {
        Objects.requireNonNull(path, "Null settings specification path");
        // - for empty path string, useSeveralPaths will be called with an empty list, but list elements are never null
        mainSettings = false;
        // - we need to reinitialize this field for an improbable case of re-using this executor
        // (well be set again in use() method)
        if (skipIfMissingOrThrow(path,false,
                () -> "Settings specification file or folder " + path + " does not exist")) {
            return 0;
        }
        final List<SettingsSpecification> settingsSpecifications;
        if (Files.isDirectory(path)) {
            settingsSpecifications = SettingsSpecification.readAllIfValid(
                    path, recursiveScanning, isMainChainSettings());
            if (isExistingSettingsRequired() && settingsSpecifications.isEmpty()) {
                throw new IllegalArgumentException("No any main chain settings was found in a folder " + path);
            }
        } else {
            settingsSpecifications = Collections.singletonList(SettingsSpecification.read(path));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        SettingsSpecification.checkIdDifference(settingsSpecifications);
        final int n = settingsSpecifications.size();
        final boolean showContent = isMainChainSettings() && n == 1;
        for (int i = 0; i < n; i++) {
            final SettingsSpecification settingsSpecification = settingsSpecifications.get(i);
            logDebug("Loading settings " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + settingsSpecification.getSpecificationFile().toAbsolutePath() + "...");
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
                report.append(settingsSpecification.getSpecificationFile()).append("\n");
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

    public SettingsBuilder use(SettingsSpecification settingsSpecification) {
        this.mainSettings = settingsSpecification.isMain();
//      Below is a very bad idea (commented code): it leads to an effect, when the settings NAMES
//      will depend on the order of loading chains!
//      It is possible when the same main chain settings are used both as actual main settings for some chain A
//      and as a settings specification for some another chain B.
//      Moreover, the category of the settings will depend on a fact, which chain uses it.
//
//        final String chainName = removeExtension(contextFileName());
//      // - note: will be null while calling from MultiChain constructor
//        if (chainName != null) {
//
//            if (isMainChainSettings()) {
//                settingsSpecification.updateAutogeneratedName(chainName);
//                // - for example, it is important if the main chain settings specification file is named
//                // "sc_specification.json", but the chain is named "com.xxxxx.ia.frame": we need to rename
//                // automatically chosen name "sc_specification" to "frame"
//            }
//
//            settingsSpecification.updateAutogeneratedCategory(chainName, isMainChainSettings());
//        }
        settingsSpecification.updateAutogeneratedCategory(isMainChainSettings());
        final String sessionId = getSessionId();
        final SettingsBuilder settingsBuilder = newSettings(settingsSpecification);
        final ExecutorSpecification combineSpecification = buildCombineSpecification(settingsBuilder);
        SETTINGS_LOADER.registerWorker(sessionId, combineSpecification, settingsBuilder);
        final ExecutorSpecification splitSpecification = buildSplitSpecification(settingsBuilder);
        if (splitSpecification != null) {
            SETTINGS_LOADER.registerWorker(sessionId, splitSpecification, settingsBuilder);
        }
        final ExecutorSpecification getNamesSpecification = buildGetNamesSpecification(settingsBuilder);
        if (getNamesSpecification != null) {
            SETTINGS_LOADER.registerWorker(sessionId, getNamesSpecification, settingsBuilder);
        }
        return this.settingsBuilder = settingsBuilder;
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

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("settingsCombinerJsonContent") ? "settingsSpecification" : name;
    }


    // Note: for multi-chain settings, this method is called as a result of
    // the call "settingsFactory.use" inside MultiChain constructor
    public ExecutorSpecification buildCombineSpecification(SettingsBuilder settingsBuilder) {
        Objects.requireNonNull(settingsBuilder, "Null settings");
        ExecutorSpecification result = buildCommon(newCombineSettings(), settingsBuilder);
        result.setId(settingsBuilder.id());
        result.setName(settingsBuilder.combineName());
        result.setDescription(settingsBuilder.combineDescription());
        addOwner(result, settingsBuilder);
        result.createOptionsIfAbsent().createRoleIfAbsent()
                .setClassName(settingsBuilder.className())
                .setSettings(true)
                .setResultPort(SettingsSpecification.SETTINGS)
                .setMain(isMainChainSettings());
        addInputControlsAndPorts(
                result,
                settingsBuilder,
                isMainChainSettings(),
                false,
                false);
        result.setSettings(settingsBuilder.specification());
        addOutputPorts(result, settingsBuilder);
        addSpecialOutputPorts(result);
        return combineExecutorSpecification = result;
    }

    public ExecutorSpecification buildSplitSpecification(SettingsBuilder settingsBuilder) {
        Objects.requireNonNull(settingsBuilder, "Null settings");
        if (settingsBuilder.splitId() == null) {
            return splitExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newSplitSettings(), settingsBuilder);
        result.setId(settingsBuilder.splitId());
        result.setName(settingsBuilder.splitName());
        result.setDescription(settingsBuilder.splitDescription());
        addOwner(result, settingsBuilder);
        addOutputPorts(result, settingsBuilder);
        addSpecialOutputPorts(result);
        return splitExecutorSpecification = result;
    }

    public ExecutorSpecification buildGetNamesSpecification(SettingsBuilder settingsBuilder) {
        Objects.requireNonNull(settingsBuilder, "Null settings");
        if (settingsBuilder.getNamesId() == null) {
            return getNamesExecutorSpecification = null;
        }
        ExecutorSpecification result = buildCommon(newGetNamesOfSettings(), settingsBuilder);
        final Map<String, ControlSpecification> executorControls =
                new LinkedHashMap<>(result.getControls());
        executorControls.get("resultType").setCaption("Result type");
        executorControls.get("resultJsonKey").setCaption("Key in result JSON").setDefaultStringValue("names");
        for (ControlSpecification controlSpecification : executorControls.values()) {
            if (controlSpecification.getValueType() == ValueType.BOOLEAN
                    && controlSpecification.getName().startsWith("extract")) {
                controlSpecification.setDefaultJsonValue(JsonValue.TRUE);
            }
        }
        result.setControls(executorControls);
        result.setId(settingsBuilder.getNamesId());
        result.setName(settingsBuilder.getNamesName());
        result.setDescription(settingsBuilder.getNamesDescription());
        addOwner(result, settingsBuilder);
        addSpecialOutputPorts(result);
        return getNamesExecutorSpecification = result;
    }

    public static void useAllInstalledInSharedContext() throws IOException {
        getSharedInstance().useAllInstalled();
    }

    // Used for adding controls and ports to InterpretChain executor
    public static void addChainControlsAndPorts(ExecutorSpecification result, SettingsBuilder settingsBuilder) {
        final boolean withSettings = settingsBuilder != null;
        if (withSettings) {
            addInputControlsAndPorts(
                    result, settingsBuilder, false, true, false);
        }
        addSystemOutputPorts(result, withSettings);
    }

    // Used for adding controls and ports to InterpretMultiChain executor
    public static void addMultiChainControlsAndPorts(ExecutorSpecification result, SettingsBuilder settingsBuilder) {
        final boolean withSettings = settingsBuilder != null;
        if (withSettings) {
            addInputControlsAndPorts(result, settingsBuilder, false, false, true);
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

    protected SettingsBuilder newSettings(SettingsSpecification settingsSpecification) {
        return SettingsBuilder.of(settingsSpecification);
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

    private void addOwner(ExecutorSpecification result, SettingsBuilder settingsBuilder) {
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

    private static ExecutorSpecification buildCommon(SettingsExecutor executor, SettingsBuilder settingsBuilder) {
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(executor);
        // - adds JavaConf, (maybe) parameters and some ports
        result.setSourceInfo(settingsBuilder.specificationFile(), null);
        if (settingsBuilder.hasPlatformId()) {
            result.setPlatformId(settingsBuilder.platformId());
        }
        result.setLanguage(SETTINGS_LANGUAGE);
        result.setTags(settingsBuilder.tags());
        result.setCategory(ExecutorSpecification.correctDynamicCategory(settingsBuilder.category()));
        result.createOptionsIfAbsent().createServiceIfAbsent().setSettingsId(settingsBuilder.id());
        result.updateCategoryPrefix(settingsBuilder.platformCategory());
        return result;
    }

    private static void addInputControlsAndPorts(
            ExecutorSpecification result,
            SettingsBuilder settingsBuilder,
            boolean mainForChain,
            boolean chainMode,
            boolean noSystemFlags) {
        if (mainForChain) {
            result.addControl(new ControlSpecification()
                    .setName(ALL_SETTINGS_PARAMETER_NAME)
                    .setCaption(ALL_SETTINGS_PARAMETER_CAPTION)
                    .setDescription(ALL_SETTINGS_PARAMETER_DESCRIPTION)
                    .setValueType(ValueType.SETTINGS)
                    .setDefaultJsonValue(Jsons.newEmptyJson())
                    .setAdvanced(true));
        }
        if (!noSystemFlags) {
            final String settingsName = settingsBuilder.name();
            /*
            // We decided not to add this information: the settings are usually created by external dashboard,
            // direct using settings combiners is not a typical case
            if (!chainMode) {
                addBooleanControl(result,
                        ADD_SETTINGS_CLASS_PARAMETER_NAME,
                        ADD_SETTINGS_CLASS_PARAMETER_CAPTION.replace("%%%", settingsName),
                        ADD_SETTINGS_CLASS_PARAMETER_DESCRIPTION.replace("%%%", settingsName),
                        false,
                        true);
            }
             */
            if (chainMode) {
                addBooleanControl(result,
                        EXTRACT_SUB_SETTINGS_PARAMETER_NAME,
                        EXTRACT_SUB_SETTINGS_PARAMETER_CAPTION.replace("%%%", settingsName),
                        EXTRACT_SUB_SETTINGS_PARAMETER_FOR_CHAIN_DESCRIPTION.replace("%%%", settingsName),
                        EXTRACT_SUB_SETTINGS_PARAMETER_FOR_CHAIN_DEFAULT,
                        false);
            }
            if (settingsBuilder.hasPathControl()) {
                addBooleanControl(result,
                        ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                        ABSOLUTE_PATHS_NAME_PARAMETER_CAPTION,
                        ABSOLUTE_PATHS_NAME_PARAMETER_DESCRIPTION,
                        SettingsBuilder.ABSOLUTE_PATHS_DEFAULT_VALUE,
                        false);
            }
        }
        if (chainMode && !noSystemFlags) {
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
        final SettingsSpecification specification = settingsBuilder.specification();
        for (Map.Entry<String, ControlSpecification> entry : specification.getControls().entrySet()) {
            final String name = entry.getKey();
            ControlSpecification controlSpecification = entry.getValue().clone();
            if (controlSpecification.getValueType().isSettings() && !chainMode) {
                final PortSpecification portSpecification = new PortSpecification();
                portSpecification.setName(name);
                portSpecification.setCaption(controlSpecification.getCaption());
                portSpecification.setHint(controlSpecification.getDescription());
                portSpecification.setValueType(DataType.SCALAR);
                result.addInputPort(portSpecification);
            }
            // Here we could also set controlSpecification.setValueClassName(),
            // when it does not exist, according the current settings category:
            //      ExecutorSpecification.defaultClassName(specification.getCategory(), name)
            // But this is a bad idea!
            // Then we will not be able to find it from a chain (settings) with another category,
            // but this is sometimes necessary.
            // It is better to keep non-specified value class: this is a signal that we need more smart search.
            result.addControl(controlSpecification);
        }
    }

    private static void addOutputPorts(ExecutorSpecification result, SettingsBuilder settingsBuilder) {
        final Map<String, ControlSpecification> controls = settingsBuilder.specification().getControls();
        for (ControlSpecification controlSpecification : controls.values()) {
            final String name = SettingsBuilder.portName(controlSpecification);
            final PortSpecification portSpecification = new PortSpecification()
                    .setName(name)
                    .setCaption(controlSpecification.getCaption())
                    .setHint(controlSpecification.getDescription())
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(controlSpecification.isAdvanced()
                            && !controlSpecification.getValueType().isSettings());
            // - Note: we NEVER make settings ports "advanced"!
            // This kind of ports may be very important for passing settings or sub-settings to other executors,
            // and hiding some of such ports as "advanced" may confuse the user.

            // if (controlSpecification.getValueType().isSettings()) {
            //    portSpecification.setAdvanced(true);
            // }
            // - bad idea:
            // if we see the sub-settings, this is an advice to like their to the corresponding subtasks
            result.addOutputPort(portSpecification);
            if (controlSpecification.getValueType() == ValueType.STRING && controlSpecification.getEditionType().isPath()) {
                result.addOutputPort(new PortSpecification()
                        .setName(name + SettingsBuilder.PATH_PARENT_FOLDER_SUFFIX)
                        .setHint(PATH_PARENT_FOLDER_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
                result.addOutputPort(new PortSpecification()
                        .setName(name + SettingsBuilder.PATH_FILE_NAME_SUFFIX)
                        .setHint(PATH_FILE_NAME_HINT)
                        .setValueType(DataType.SCALAR)
                        .setAdvanced(true));
            }
        }
    }

    private static void addSystemOutputPorts(ExecutorSpecification result, boolean addSettingsIdPort) {
        result.addSystemExecutorIdPort();
        if (addSettingsIdPort) {
            result.addOutputPort(new PortSpecification()
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
            result.addOutputPort(new PortSpecification()
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
        result.addControl(new ControlSpecification()
                .setName(name)
                .setCaption(caption)
                .setDescription(description)
                .setValueType(ValueType.BOOLEAN)
                .setDefaultJsonValue(Jsons.booleanValue(defaultValue))
                .setAdvanced(advanced));
    }
}
