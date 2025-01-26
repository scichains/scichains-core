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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SettingsSpecification extends AbstractConvertibleToJson {
    public static final String SETTINGS_FILE_PATTERN = ".*\\.(json|ss|mss)$";

    public static final String APP_NAME = "settings";
    public static final String APP_NAME_FOR_MAIN = "main-settings";
    public static final String CURRENT_VERSION = "1.0";

    public static final String SUBSETTINGS_PREFIX = "@";
    public static final String SYSTEM_PREFIX = "$";
    public static final String CLASS_KEY = SYSTEM_PREFIX + "class";
    public static final String SETTINGS = "settings";
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
        private String enumItemsFile = null;
        private List<String> enumItemNames = null;
        private List<String> enumItemCaptions = null;

        public ControlConfExtension() {
        }

        public ControlConfExtension(JsonObject json, Path file) {
            this.enumItemsFile = json.getString("enum_items_file", null);
        }

        public String getEnumItemsFile() {
            return enumItemsFile;
        }

        public ControlConfExtension setEnumItemsFile(String enumItemsFile) {
            this.enumItemsFile = enumItemsFile;
            return this;
        }

        public Path enumItemsFile(SettingsSpecification specification) {
            return enumItemsFile == null ?
                    null :
                    specification.resolve(Paths.get(enumItemsFile), "enum items");
        }

        public List<String> enumItemNames() {
            return enumItemNames;
        }

        public List<String> enumItemCaptions() {
            return enumItemCaptions;
        }

        @Override
        public void checkCompleteness() {
        }

        public void load(SettingsSpecification specification) {
            final Path file = enumItemsFile(specification);
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
            this.enumItemNames = items.lines();
            this.enumItemCaptions = items.comments();
        }

        public void completeControlConf(ExecutorSpecification.ControlConf controlConf) {
            if (controlConf.getItems() == null && this.enumItemNames != null) {
                controlConf.setItemsFromLists(enumItemNames, enumItemCaptions);
            }
        }

        @Override
        public String toString() {
            return "ControlConfExtension{" +
                    "enumItemsFile='" + enumItemsFile + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            if (enumItemsFile != null) {
                builder.add("enum_items_file", enumItemsFile);
            }
        }
    }

    private Path settingsSpecificationFile = null;
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

    private final Object controlsLock = new Object();
    // - allows correct changes in the controls from parallel threads:
    // can be useful while building settings tree

    public SettingsSpecification() {
    }

    private SettingsSpecification(JsonObject json, boolean strictMode, Path file) {
        final int settingsType = settingsType(json);
        if (settingsType == CODE_FOR_INVALID && strictMode) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a settings combiner configuration: no \"app\":\""
                    + APP_NAME + "\" or \"app\":\"" + APP_NAME_FOR_MAIN + "\" element");
        }
        this.settingsSpecificationFile = file;
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
            final ControlConfExtension controlExtension = new ControlConfExtension(jsonObject, file);
            controlExtensions.put(name, controlExtension);
        }
    }

    public static SettingsSpecification read(Path settingsSpecificationFile) throws IOException {
        Objects.requireNonNull(settingsSpecificationFile, "Null settingsSpecificationFile");
        final JsonObject json = Jsons.readJson(settingsSpecificationFile);
        return new SettingsSpecification(json, true, settingsSpecificationFile);
    }

    public static SettingsSpecification readIfValid(Path settingsSpecificationFile, boolean onlyMainCombiner) {
        Objects.requireNonNull(settingsSpecificationFile, "Null settingsSpecificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(settingsSpecificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        final int settingsType = settingsType(json);
        if (settingsType == CODE_FOR_INVALID || (onlyMainCombiner && settingsType != CODE_FOR_MAIN)) {
            return null;
        }
        return new SettingsSpecification(json, true, settingsSpecificationFile);
    }

    public static List<SettingsSpecification> readAllIfValid(
            Path containingJsonPath,
            boolean recursive,
            boolean onlyMainCombiners)
            throws IOException {
        return ExtensionSpecification.readAllIfValid(
                null,
                containingJsonPath,
                recursive,
                path -> readIfValid(path, onlyMainCombiners),
                SettingsSpecification::isSettingsSpecificationFile);
    }

    public void write(Path settingsSpecificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(settingsSpecificationFile, "Null settingsSpecificationFile");
        Files.writeString(settingsSpecificationFile, Jsons.toPrettyString(toJson()), options);
    }

    public static SettingsSpecification valueOf(JsonObject settingsSpecification) {
        return valueOf(settingsSpecification, true);
    }

    public static SettingsSpecification valueOf(JsonObject settingsSpecification, boolean strictMode) {
        return new SettingsSpecification(settingsSpecification, strictMode, null);
    }

    public static boolean isSettingsSpecificationFile(Path file) {
        Objects.requireNonNull(file, "Null file");
        return COMPILED_SETTINGS_FILE_PATTERN.matcher(file.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isSettingsSpecification(JsonObject settingsSpecification) {
        return settingsType(settingsSpecification) != CODE_FOR_INVALID;
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
        Objects.requireNonNull(settingsSpecifications, "Null settings combiner JSONs collection");
        final Set<String> ids = new HashSet<>();
        for (SettingsSpecification settingsSpecification : settingsSpecifications) {
            final String id = settingsSpecification.getId();
            assert id != null;
            final String splitId = settingsSpecification.getSplitId();
            final String getNamesId = settingsSpecification.getGetNamesId();
            // - note: splitId/getNamesId MAY be null
            if (id.equals(splitId)) {
                throw new IllegalArgumentException("Identical id and split_id for settings combiner \""
                        + settingsSpecification.getName() + "\": " + id);
            }
            if (id.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical id and get_names_id for settings combiner \""
                        + settingsSpecification.getName() + "\": " + id);
            }
            if (splitId != null && splitId.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical split_id and get_names_id for settings combiner \""
                        + settingsSpecification.getName() + "\": " + splitId);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + id
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
            if (splitId != null && !ids.add(splitId)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + splitId
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
            if (getNamesId != null && !ids.add(getNamesId)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + getNamesId
                        + ", one of them is \"" + settingsSpecification.getName() + "\"");
            }
        }
    }

    public static void checkParameterName(String name, Path file) throws JsonException {
        if (name.equals(SETTINGS)) {
            throw new JsonException("Non-allowed parameter name \"" + name
                    + "\"" + (file == null ? "" : " in JSON " + file));
        }
        if (!SourceVersion.isIdentifier(name)) {
            throw new JsonException("Parameter name \"" + name + "\" is not a valid Java identifier; "
                    + "such named are not allowed" + (file == null ? "" : " in JSON " + file));
        }
    }

    public Path getSettingsSpecificationFile() {
        return settingsSpecificationFile;
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

    public SettingsSpecification setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Null category");
        this.autogeneratedCategory = false;
        return this;
    }

    public boolean isAutogeneratedName() {
        return autogeneratedName;
    }

    public String getName() {
        return name;
    }

    public SettingsSpecification setName(String name) {
        Objects.requireNonNull(name, "Null name");
        checkSettingsName(name, null);
        this.name = name;
        this.autogeneratedName = false;
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
        synchronized (controlsLock) {
            return Collections.unmodifiableMap(controls);
        }
    }

    public SettingsSpecification setControls(Map<String, ExecutorSpecification.ControlConf> controls) {
        controls = ExecutorSpecification.checkControls(controls);
        for (ExecutorSpecification.ControlConf controlConf : controls.values()) {
            checkParameterName(controlConf.getName(), null);
        }
        synchronized (controlsLock) {
            this.controls = controls;
        }
        return this;
    }

    public ExecutorSpecification.ControlConf getControl(String name) {
        synchronized (controlsLock) {
            return controls.get(name);
        }
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

    public void addControl(ExecutorSpecification.ControlConf control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        synchronized (controlsLock) {
            controls.put(control.getName(), control);
        }
    }

    public Set<String> controlKeySet() {
        synchronized (controlsLock) {
            return controls.values().stream().map(SettingsSpecification::controlKey).collect(Collectors.toSet());
        }
    }

    public String parentFolderName() {
        if (settingsSpecificationFile == null) {
            return null;
        }
        Path parent = settingsSpecificationFile.getParent();
        if (parent == null) {
            return null;
        }
        parent = parent.getFileName();
        if (parent == null) {
            return null;
        }
        return parent.toString();
    }

    public void updateAutogeneratedCategory(boolean removeLastElementInClassName) {
        final String parent = parentFolderName();
        if (parent != null && autogeneratedCategory) {
            // For main settings (typical usage of settings combiners) we usually have
            // the following structure:
            // folder
            //     org.algart.ia.frame
            // contains the file
            //     frame.json
            // Usually there are a lot of analogous subfolders inside the same root folder.
            // (Note that the file MUST be named "frame" to correctly detect, what is it.)
            // Without removing the last element, we will produce a tree of functions like the following:
            //     org
            //         algart
            //             ia
            //                 frame
            //                     Combine frame
            //                 pyramid
            //                     Combine pyramid
            // ...
            // Removing last element "frame" helps to produce more simple tree:
            //     org
            //         algart
            //             ia
            //                 Combine frame
            //                 Combine pyramid
            // ...
            final String recommendedCategory = !removeLastElementInClassName ?
                    parent :
                    ExecutionBlock.recommendedCategory(parent);
            if (recommendedCategory != null) {
                // Note: if className does not contain ".", the result will be null,
                // and we will not change the category name
                setCategory(DEFAULT_SETTINGS_CATEGORY_PREFIX + recommendedCategory);
            }
        }
    }

    public void updateAutogeneratedName(String className) {
        if (className != null && autogeneratedName) {
            setName(ExecutionBlock.recommendedName(className));
        }
    }

    // Idea for the future?
    private boolean matchesClass(String settingsClass) {
        Objects.requireNonNull(settingsClass, "Null settingsClass");
        try {
            checkSettingsName(name, null);
        } catch (JsonException e) {
            throw new AssertionError("Was not checked before!", e);
        }
        return settingsClass.endsWith(ExecutorSpecification.CATEGORY_SEPARATOR + name);
    }

    public boolean hasPathControl() {
        synchronized (controlsLock) {
            return controls.values().stream().anyMatch(
                    controlConf -> controlConf.getEditionType().isPath());
        }
    }

    public SettingsTree buildTree(SettingsSpecificationFactory factory) {
        return new SettingsTree(factory, this);
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
    }

    @Override
    public String toString() {
        return "SettingsSpecification{" +
                "settingsSpecificationFile=" + settingsSpecificationFile +
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

    public JsonObject toJsonTree(SettingsSpecificationFactory specificationFactory) {
        Objects.requireNonNull(specificationFactory, "Null specification factory");
        checkCompleteness();
        return buildTree(specificationFactory).toJson();
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        buildJson(builder, null);
    }

    private static int settingsType(JsonObject settingsSpecification) {
        Objects.requireNonNull(settingsSpecification, "Null settingsSpecification");
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

    void buildJson(JsonObjectBuilder builder, Function<String, JsonObject> subSettingsJsonBuilder) {
        builder.add("app", main ? APP_NAME_FOR_MAIN : APP_NAME);
        builder.add("version", version);
        builder.add("category", category);
        builder.add("name", name);
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
            final String name = entry.getKey();
            final ExecutorSpecification.ControlConf control = entry.getValue();
            control.checkCompleteness();
            final JsonObjectBuilder controlBuilder = Json.createObjectBuilder();
            control.buildJson(controlBuilder);
            final ControlConfExtension controlExtension = controlExtensions.get(entry.getKey());
            if (controlExtension != null) {
                controlExtension.buildJson(controlBuilder);
            }
            if (subSettingsJsonBuilder != null && control.getValueType().isSettings()) {
                JsonObject subSettingsJson = subSettingsJsonBuilder.apply(name);
                if (subSettingsJson != null) {
                    controlBuilder.add(SETTINGS, subSettingsJson);
                }
            }
            controlsBuilder.add(controlBuilder.build());
        }
        builder.add("controls", controlsBuilder.build());
    }

    private Path resolve(Path path, String whatFile) {
        if (path.isAbsolute()) {
            return path;
        }
        if (this.settingsSpecificationFile == null) {
            throw new IllegalStateException("Name of " + whatFile
                    + " file is relative and cannot be resolved, because "
                    + "settings combiner JSON was not loaded from file; you must use absolute paths in this case");
        }
        return settingsSpecificationFile.getParent().resolve(path);
    }

    public static void main(String[] args) throws IOException {
        final SettingsSpecification settingsSpecification = read(Paths.get(args[0]));
        System.out.println(settingsSpecification);
        System.out.println(Jsons.toPrettyString(settingsSpecification.toJson()));
    }
}
