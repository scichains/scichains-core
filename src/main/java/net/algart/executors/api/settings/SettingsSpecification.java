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

import jakarta.json.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.io.MatrixIO;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import javax.lang.model.SourceVersion;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SettingsSpecification extends AbstractConvertibleToJson {
    public static final boolean USE_CONTROL_EXTENSIONS = false;

    /**
     * Settings specification file extensions:<br>
     * .json<br>
     * .ss ("settings specification")<br>
     * .mss ("main settings specification")
     */
    public static final String SETTINGS_FILE_PATTERN = ".*\\.(json|ss|mss)$";

    public static final String APP_NAME = "settings";
    public static final String APP_NAME_FOR_MAIN = "main-settings";
    public static final String CURRENT_VERSION = "1.0";

    public static final String SUBSETTINGS_PREFIX = "@";
    public static final String SYSTEM_PREFIX = "$";
    public static final String CLASS_KEY = SYSTEM_PREFIX + "class";
    public static final String SETTINGS = ExecutorSpecification.SETTINGS;
    public static final String DEFAULT_SETTINGS_CATEGORY = SETTINGS;
    public static final String DEFAULT_SETTINGS_CATEGORY_PREFIX = SETTINGS + ExecutorSpecification.CATEGORY_SEPARATOR;
    public static final String DEFAULT_SETTINGS_COMBINE_PREFIX = "Combine ";
    public static final String DEFAULT_SETTINGS_SPLIT_PREFIX = "Split ";
    public static final String DEFAULT_SETTINGS_GET_NAMES_PREFIX = "Get names of ";
    // Note: split and get-names executors are optional, they are created only if splitId/getNamesId are specified!

    private static final Pattern COMPILED_SETTINGS_FILE_PATTERN = Pattern.compile(SETTINGS_FILE_PATTERN);

    private static final String APP_NAME_ALIAS = "settings-combiner";
    private static final String APP_NAME_FOR_MAIN_ALIAS = "main-settings-combiner";

    private static final int CODE_FOR_MAIN = 2;
    private static final int CODE_FOR_ORDINARY = 1;
    private static final int CODE_FOR_INVALID = 0;

    public static final class ControlConfExtension extends AbstractConvertibleToJson {
        private String itemsFile = null;
        private List<String> itemNames = null;
        private List<String> itemCaptions = null;

        public ControlConfExtension() {
        }

        public ControlConfExtension(JsonObject json, Path file) {
            this.itemsFile = json.getString("items_file", null);
        }

        public String getItemsFile() {
            return itemsFile;
        }

        public ControlConfExtension setItemsFile(String itemsFile) {
            this.itemsFile = itemsFile;
            return this;
        }

        public Path itemsFile(SettingsSpecification specification) {
            return itemsFile == null ?
                    null :
                    specification.resolve(Paths.get(itemsFile), "enum items");
        }

        public List<String> itemNames() {
            return itemNames;
        }

        public List<String> itemCaptions() {
            return itemCaptions;
        }

        @Override
        public void checkCompleteness() {
        }

        public void load(SettingsSpecification specification) {
            final Path file = itemsFile(specification);
            if (file == null) {
                return;
            }
            final String s;
            try {
                s = Files.readString(file);
            } catch (IOException e) {
                throw new JsonException("Cannot load items file " + file.toAbsolutePath(), e);
            }
            final SScalar.MultiLineOrJsonSplitter items = SScalar.splitJsonOrTrimmedLinesWithComments(s);
            if (items.numberOfLines() == 0) {
                throw new JsonException("No enum items in the file " + file.toAbsolutePath());
            }
            this.itemNames = items.lines();
            this.itemCaptions = items.comments();
        }

        public void completeControlConf(ExecutorSpecification.ControlConf controlConf) {
            if (controlConf.getItems() == null && this.itemNames != null) {
                controlConf.setItemsFromLists(itemNames, itemCaptions);
            }
        }

        @Override
        public String toString() {
            return "ControlConfExtension{" +
                    "itemsFile='" + itemsFile + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            if (itemsFile != null) {
                builder.add("items_file", itemsFile);
            }
        }
    }

    private Path specificationFile = null;
    private boolean main = false;
    private String version = CURRENT_VERSION;
    private boolean autogeneratedCategory = false;
    private String category;
    private boolean autogeneratedName = false;
    private String name;
    private String combineName = null;
    private String splitName = null;
    private String getNamesName = null;
    private String className = null;
    private String description = null;
    private String combineDescription = null;
    private String splitDescription = null;
    private String getNamesDescription = null;
    private String id;
    private String splitId = null;
    private String getNamesId = null;
    private Map<String, ExecutorSpecification.ControlConf> controls = new LinkedHashMap<>();
    private Map<String, ControlConfExtension> controlExtensions = new LinkedHashMap<>();

    // The following properties are not loaded from JSON-file, but are set later,
    // while loading all specifications for some platform
    private Set<String> tags = new LinkedHashSet<>();
    private String platformId = null;
    private String platformCategory = null;

    public SettingsSpecification() {
    }

    private SettingsSpecification(JsonObject json, boolean strictMode, Path file) {
        final int settingsType = settingsType(json);
        if (settingsType == CODE_FOR_INVALID && strictMode) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a settings specification: no \"app\":\""
                    + APP_NAME + "\" or \"app\":\"" + APP_NAME_FOR_MAIN + "\" element");
        }
        this.specificationFile = file;
        this.main = settingsType == CODE_FOR_MAIN;
        this.version = json.getString("version", CURRENT_VERSION);
        this.category = json.getString("category", null);
        if (this.category == null) {
            this.category = DEFAULT_SETTINGS_CATEGORY;
            autogeneratedCategory = true;
        }
        final Path fileName = file == null ? null : file.getFileName();
        this.name = json.getString("name", null);
        if (this.name == null) {
            this.name = fileName == null ? SETTINGS : MatrixIO.removeExtension(fileName.toString());
            autogeneratedName = true;
        }
        checkSettingsName(this.name, file);
        this.combineName = json.getString("combine_name", null);
        this.splitName = json.getString("split_name", null);
        this.getNamesName = json.getString("get_names_name", null);
        this.className = json.getString("class_name", null);
        this.description = json.getString("description", null);
        this.combineDescription = json.getString("combine_description", description);
        this.splitDescription = json.getString("split_description", description);
        this.getNamesDescription = json.getString("get_names_description", description);
        this.id = Jsons.reqString(json, "id", file);
        this.splitId = json.getString("split_id", null);
        this.getNamesId = json.getString("get_names_id", null);
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "controls", file)) {
            final ExecutorSpecification.ControlConf control = new ExecutorSpecification.ControlConf(jsonObject, file);
            final String name = control.getName();
            checkParameterName(name, file);
            controls.put(name, control);
            if (USE_CONTROL_EXTENSIONS) {
                final ControlConfExtension controlExtension = new ControlConfExtension(jsonObject, file);
                controlExtensions.put(name, controlExtension);
            }
        }
    }

    public static SettingsSpecification read(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        return new SettingsSpecification(json, true, specificationFile);
    }

    public static SettingsSpecification readIfValid(Path specificationFile) {
        return readIfValid(specificationFile, false);
    }

    public static SettingsSpecification readIfValid(Path specificationFile, boolean onlyMainSettings) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(specificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        final int settingsType = settingsType(json);
        if (settingsType == CODE_FOR_INVALID || (onlyMainSettings && settingsType != CODE_FOR_MAIN)) {
            return null;
        }
        return new SettingsSpecification(json, true, specificationFile);
    }

    public static List<SettingsSpecification> readAllIfValid(
            Path containingJsonPath,
            boolean recursive,
            boolean onlyMainSettings)
            throws IOException {
        return ExtensionSpecification.readAllIfValid(
                null,
                containingJsonPath,
                recursive,
                path -> readIfValid(path, onlyMainSettings),
                SettingsSpecification::isSettingsSpecificationFile);
    }

    public void write(Path specificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        Files.writeString(specificationFile, Jsons.toPrettyString(toJson()), options);
    }

    public static SettingsSpecification of(String specificationString) {
        return of(Jsons.toJson(specificationString));
    }

    public static SettingsSpecification of(JsonObject specificationJson) {
        return of(specificationJson, true);
    }

    public static SettingsSpecification of(JsonObject specificationJson, boolean strictMode) {
        return new SettingsSpecification(specificationJson, strictMode, null);
    }

    public static boolean isSettingsSpecificationFile(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        return COMPILED_SETTINGS_FILE_PATTERN.matcher(specificationFile.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isSettingsSpecification(JsonObject specificationJson) {
        return settingsType(specificationJson) != CODE_FOR_INVALID;
    }

    public static boolean isSettingsKey(String key) {
        Objects.requireNonNull(key, "Null key");
        return key.startsWith(SUBSETTINGS_PREFIX);
    }

    public static String controlKey(ExecutorSpecification.ControlConf controlConf) {
        Objects.requireNonNull(controlConf, "Null controlConf");
        final String name = controlConf.getName();
        return controlConf.getValueType().isSettings() ? settingsKey(name) : name;
    }

    public static String settingsKey(String subSettingsName) {
        Objects.requireNonNull(subSettingsName, "Null sub-settings name");
        return SUBSETTINGS_PREFIX + subSettingsName;
    }

    public static void checkIdDifference(Collection<SettingsSpecification> settingsSpecifications) {
        Objects.requireNonNull(settingsSpecifications, "Null settings specifications collection");
        final Set<String> ids = new HashSet<>();
        for (SettingsSpecification settingsSpecification : settingsSpecifications) {
            final String id = settingsSpecification.getId();
            assert id != null;
            final String splitId = settingsSpecification.getSplitId();
            final String getNamesId = settingsSpecification.getGetNamesId();
            // - note: splitId/getNamesId MAY be null
            if (id.equals(splitId)) {
                throw new IllegalArgumentException("Identical id and split_id for settings \""
                        + settingsSpecification.getName() + "\": " + id);
            }
            if (id.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical id and get_names_id for settings \""
                        + settingsSpecification.getName() + "\": " + id);
            }
            if (splitId != null && splitId.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical split_id and get_names_id for settings \""
                        + settingsSpecification.getName() + "\": " + splitId);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two settings JSONs have identical IDs " + id
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
            if (splitId != null && !ids.add(splitId)) {
                throw new IllegalArgumentException("Two settings JSONs have identical IDs " + splitId
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
            if (getNamesId != null && !ids.add(getNamesId)) {
                throw new IllegalArgumentException("Two settings JSONs have identical IDs " + getNamesId
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
        }
    }

    public static void checkParameterName(String name, Path file) throws JsonException {
        Objects.requireNonNull(name, "Null parameter name");
        if (name.equals(SETTINGS)) {
            throw new JsonException("Non-allowed parameter name \"" + name
                    + "\"" + (file == null ? "" : " in JSON " + file));
        }
        if (!SourceVersion.isIdentifier(name)) {
            throw new JsonException("Parameter name \"" + name + "\" is not a valid Java identifier; "
                    + "such named are not allowed" + (file == null ? "" : " in JSON " + file));
        }
    }

    public Path getSpecificationFile() {
        return specificationFile;
    }

    public boolean isMain() {
        return main;
    }

    public SettingsSpecification setMain(boolean main) {
        this.main = main;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SettingsSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public boolean isAutogeneratedCategory() {
        return autogeneratedCategory;
    }

    public String getCategory() {
        return category;
    }

    public SettingsSpecification setCategory(String category, boolean autogeneratedCategory) {
        this.category = Objects.requireNonNull(category, "Null category");
        this.autogeneratedCategory = autogeneratedCategory;
        return this;
    }

    public boolean isAutogeneratedName() {
        return autogeneratedName;
    }

    public String getName() {
        return name;
    }

    public SettingsSpecification setName(String name, boolean autogeneratedName) {
        Objects.requireNonNull(name, "Null name");
        checkSettingsName(name, null);
        this.name = name;
        this.autogeneratedName = autogeneratedName;
        return this;
    }

    public String combineName() {
        return combineName == null ? DEFAULT_SETTINGS_COMBINE_PREFIX + name : combineName;
    }

    public String getCombineName() {
        return combineName;
    }

    public SettingsSpecification setCombineName(String combineName) {
        this.combineName = combineName;
        return this;
    }

    public String splitName() {
        return splitName == null ? DEFAULT_SETTINGS_SPLIT_PREFIX + name : splitName;
    }

    public String getSplitName() {
        return splitName;
    }

    public SettingsSpecification setSplitName(String splitName) {
        this.splitName = splitName;
        return this;
    }

    public String getNamesName() {
        return getNamesName == null ? DEFAULT_SETTINGS_GET_NAMES_PREFIX + name : getNamesName;
    }

    public String getGetNamesName() {
        return getNamesName;
    }

    public SettingsSpecification setGetNamesName(String getNamesName) {
        this.getNamesName = getNamesName;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public String className() {
        return className == null ? ExecutorSpecification.className(category, name) : className;
    }

    public SettingsSpecification setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SettingsSpecification setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCombineDescription() {
        return combineDescription;
    }

    public SettingsSpecification setCombineDescription(String combineDescription) {
        this.combineDescription = combineDescription;
        return this;
    }

    public String getSplitDescription() {
        return splitDescription;
    }

    public SettingsSpecification setSplitDescription(String splitDescription) {
        this.splitDescription = splitDescription;
        return this;
    }

    public String getGetNamesDescription() {
        return getNamesDescription;
    }

    public SettingsSpecification setGetNamesDescription(String getNamesDescription) {
        this.getNamesDescription = getNamesDescription;
        return this;
    }

    public String getId() {
        return id;
    }

    public SettingsSpecification setId(String id) {
        this.id = Objects.requireNonNull(id, "Null id");
        return this;
    }

    public String getSplitId() {
        return splitId;
    }

    public SettingsSpecification setSplitId(String splitId) {
        this.splitId = splitId;
        return this;
    }

    public String getGetNamesId() {
        return getNamesId;
    }

    public SettingsSpecification setGetNamesId(String getNamesId) {
        this.getNamesId = getNamesId;
        return this;
    }

    public Map<String, ExecutorSpecification.ControlConf> getControls() {
        return Collections.unmodifiableMap(controls);
    }

    public SettingsSpecification setControls(Map<String, ExecutorSpecification.ControlConf> controls) {
        controls = ExecutorSpecification.checkControls(controls);
        for (ExecutorSpecification.ControlConf controlConf : controls.values()) {
            checkParameterName(controlConf.getName(), null);
        }
        this.controls = controls;
        return this;
    }

    public ExecutorSpecification.ControlConf getControl(String name) {
        return controls.get(name);
    }

    public Map<String, ControlConfExtension> getControlExtensions() {
        return controlExtensions;
    }

    public SettingsSpecification setControlExtensions(Map<String, ControlConfExtension> controlExtensions) {
        this.controlExtensions = Objects.requireNonNull(controlExtensions, "Null controlExtensions");
        return this;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public SettingsSpecification setTags(Set<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags = new LinkedHashSet<>(tags);
        return this;
    }

    public void addTags(Collection<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags.addAll(tags);
    }

    public boolean hasPlatformId() {
        return platformId != null;
    }

    public String getPlatformId() {
        return platformId;
    }

    public SettingsSpecification setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public String getPlatformCategory() {
        return platformCategory;
    }

    public SettingsSpecification setPlatformCategory(String platformCategory) {
        this.platformCategory = platformCategory;
        return this;
    }

    public Set<String> controlKeySet() {
        return controls.values().stream().map(SettingsSpecification::controlKey).collect(Collectors.toSet());
    }

    public String parentFolderName() {
        if (specificationFile == null) {
            return null;
        }
        Path parent = specificationFile.getParent();
        if (parent == null) {
            return null;
        }
        parent = parent.getFileName();
        if (parent == null) {
            return null;
        }
        return parent.toString();
    }

    /**
     * Sets the category to a more intellectual value if it is {@link #isAutogeneratedCategory() auto-genetered}.
     *
     * <p>Typically we have the following file structure:</p>
     * <pre>
     *      (The chains are stored in the files)
     *          net.algart.project.task_a.chain
     *          net.algart.project.task_b.chain
     *          ...
     *      (an we have the associated folders)
     *          net.algart.project.task_a/
     *          net.algart.project.task_b/
     *          ...
     *      (which contain the main settings specification files)
     *          net.algart.project.task_a/task_a.ss
     *          net.algart.project.task_a/some_subtask_of_a.ss
     *          net.algart.project.task_a/some_other_subtask_of_a.ss
     *          net.algart.project.task_b/task_b.ss
     *          ...
     * </pre>
     *
     * <p>By default, the chain category and name are calculated based on the chain file name.
     * In this example, the chain category for the task A will be <code>$subchains.net.algart.project</code>
     * (with the prefix <code>$subchains</code>),
     * and the name will be <code>task_a</code>: the substring after the last dot ".".
     * The additional dots (.) in the category are interpreted as separators between packages/subpackages,
     * as in the Java language.
     * So in the function tree, we will see the chain executors as:</p>
     * <pre>
     *      $subchains
     *          net
     *              algart
     *                  project
     *                      <b>task_a</b>
     *                      <b>task_b</b>
     *                      ...
     * </pre>
     *
     * <p>If the argument <code>removeLastElementInClassName</code> is <code>true</code>, this method calculates
     * the category for this settings combiner based on the <b>parent subfolder</b>,
     * using a similar scheme:
     * the part after the last dot is removed, and the settings category will be
     * <code>$settings.net.algart.project</code>.
     * The auto-generated name of the setting will be the name of the settings specification
     * <b>file</b> without extension,
     * i.e. <code>task_a</code> (or <code>task_b</code> etc.);
     * dots before the extension are not allowed in the settings file name.
     * So in the function tree, we will see the settings combiners as:</p>
     * <pre>
     *      $settings
     *          net
     *              algart
     *                  project
     *                      <b>Combine task_a</b>
     *                      <b>Combine task_b</b>
     *                      ...
     * </pre>
     *
     * <p>This is a good mode if these settings are the {@link UseSettings#isMainChainSettings() main chain settings}.
     * For other kinds of settings, for example, settings of some internal sub-chains or other functions
     * ("some_subtask_of_a.ss" in the example above), the argument of this method should be <code>false</code>.
     * Then the full parent subfolder name will become the category, and we will see the following tree:</p>

     * <pre>
     *      $settings
     *          net
     *              algart
     *                  project
     *                      task_a
     *                          <b>Combine some_subtask_of_a</b>
     *                      <b>Combine task_a</b>
     *                      ...
     * </pre>
     *
     * @param removeLastElementInClassName whether we need to remove the ending part of the parent folder name
     *                                     after the last dot ".".
     */
    public void updateAutogeneratedCategory(boolean removeLastElementInClassName) {
        final String parent = parentFolderName();
        if (parent != null && autogeneratedCategory) {
            final String recommendedCategory = !removeLastElementInClassName ?
                    parent :
                    ExecutionBlock.recommendedCategory(parent);
            if (recommendedCategory != null) {
                // Note: if className does not contain ".", the result will be null,
                // and we will not change the category name
                setCategory(DEFAULT_SETTINGS_CATEGORY_PREFIX + recommendedCategory, true);
            }
        }
    }

    public void updateAutogeneratedName(String className) {
        if (className != null && autogeneratedName) {
            setName(ExecutionBlock.recommendedName(className), true);
        }
    }

    public boolean hasPathControl() {
        return controls.values().stream().anyMatch(
                controlConf -> controlConf.getEditionType().isPath());
    }

    public void setTo(ExecutorSpecification specification) {
        Objects.requireNonNull(specification, "Null executor specification");
        setCategory(specification.getCategory(), false);
        setName(specification.getName(), false);
        // - category and name cannot be autogenerated in ExecutorSpecification
        setDescription(specification.getDescription());
        setId(specification.getId());
        Map<String, ExecutorSpecification.ControlConf> controls = new LinkedHashMap<>(specification.getControls());
        controls.remove(SETTINGS);
        // - non-allowed parameter name, which is added automatically by settings combiners
        setControls(controls);
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
    }

    public JsonObject toJson(boolean includeAutogenerated) {
        checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        buildJson(builder, includeAutogenerated);
        return builder.build();
    }

    @Override
    public String toString() {
        return "SettingsSpecification{" +
                "specificationFile=" + specificationFile +
                ", main=" + main +
                ", version='" + version + '\'' +
                ", tags='" + tags + '\'' +
                ", platformId='" + platformId + '\'' +
                ", platformCategory='" + platformCategory + '\'' +
                ", autogeneratedCategory=" + autogeneratedCategory +
                ", category='" + category + '\'' +
                ", autogeneratedName=" + autogeneratedName +
                ", name='" + name + '\'' +
                ", combineName='" + combineName + '\'' +
                ", splitName='" + splitName + '\'' +
                ", getNamesName='" + getNamesName + '\'' +
                ", className='" + className + '\'' +
                ", description='" + description + '\'' +
                ", combineDescription='" + combineDescription + '\'' +
                ", splitDescription='" + splitDescription + '\'' +
                ", getNamesDescription='" + getNamesDescription + '\'' +
                ", id='" + id + '\'' +
                ", splitId='" + splitId + '\'' +
                ", getNamesId='" + getNamesId + '\'' +
                ", controls=" + controls +
                ", controlExtensions=" + controlExtensions +
                '}';
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        buildJson(builder, false);
    }

    public void buildJson(JsonObjectBuilder builder, boolean includeAutogenerated) {
        builder.add("app", main ? APP_NAME_FOR_MAIN : APP_NAME);
        if (!version.equals(CURRENT_VERSION)) {
            builder.add("version", version);
        }
        if (includeAutogenerated || !autogeneratedCategory) {
            builder.add("category", category);
        }
        if (includeAutogenerated || !autogeneratedName) {
            builder.add("name", name);
        }
        if (combineName != null) {
            // - possible when this object is built by public constructor and setters
            builder.add("combine_name", combineName);
        }
        if (splitName != null) {
            builder.add("split_name", splitName);
        }
        if (getNamesName != null) {
            builder.add("get_names_name", getNamesName);
        }
        if (className != null) {
            builder.add("class_name", className);
        }
        if (description != null) {
            builder.add("description", description);
        }
        if (combineDescription != null && !combineDescription.equals(description)) {
            builder.add("combine_description", combineDescription);
        }
        if (splitDescription != null && !splitDescription.equals(description)) {
            builder.add("split_description", splitDescription);
        }
        if (getNamesDescription != null && !getNamesDescription.equals(description)) {
            builder.add("get_names_description", getNamesDescription);
        }
        builder.add("id", id);
        if (splitId != null) {
            builder.add("split_id", splitId);
        }
        if (getNamesId != null) {
            builder.add("get_names_id", getNamesId);
        }
        final JsonArrayBuilder controlsBuilder = Json.createArrayBuilder();
        for (Map.Entry<String, ExecutorSpecification.ControlConf> entry : controls.entrySet()) {
            final ExecutorSpecification.ControlConf control = entry.getValue();
            control.checkCompleteness();
            final JsonObjectBuilder controlBuilder = Json.createObjectBuilder();
            control.buildJson(controlBuilder);
            final ControlConfExtension controlExtension = controlExtensions.get(entry.getKey());
            if (controlExtension != null) {
                controlExtension.buildJson(controlBuilder);
            }
            controlsBuilder.add(controlBuilder.build());
        }
        builder.add("controls", controlsBuilder.build());
    }

    private static int settingsType(JsonObject settingsSpecification) {
        Objects.requireNonNull(settingsSpecification, "Null settings specification");
        final String app = settingsSpecification.getString("app", null);
        return APP_NAME.equals(app) || APP_NAME_ALIAS.equals(app) ?
                CODE_FOR_ORDINARY :
                APP_NAME_FOR_MAIN.equals(app) || APP_NAME_FOR_MAIN_ALIAS.equals(app) ?
                        CODE_FOR_MAIN : CODE_FOR_INVALID;
    }

    private static void checkSettingsName(String name, Path file) throws JsonException {
        if (name.contains(String.valueOf(ExecutorSpecification.CATEGORY_SEPARATOR))) {
            throw new JsonException("Non-allowed settings name \"" + name
                    + "\"" + (file == null ? "" : " in JSON " + file)
                    + ": it contains \"" + ExecutorSpecification.CATEGORY_SEPARATOR + "\" character");
        }
    }

    private Path resolve(Path path, String whatFile) {
        if (path.isAbsolute()) {
            return path;
        }
        if (this.specificationFile == null) {
            throw new IllegalStateException("Name of " + whatFile
                    + " file is relative and cannot be resolved, because "
                    + "settings specification was not loaded from file; you must use absolute paths in this case");
        }
        return specificationFile.getParent().resolve(path);
    }

    public static void main(String[] args) throws IOException {
        final SettingsSpecification settingsSpecification = read(Paths.get(args[0]));
        System.out.println(settingsSpecification);
        System.out.println(Jsons.toPrettyString(settingsSpecification.toJson()));
    }
}
