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

package net.algart.executors.modules.core.logic.compiler.settings.model;

import jakarta.json.*;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.api.model.ChainJson;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;

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

public final class SettingsCombinerJson extends AbstractConvertibleToJson {
    public static final String SETTINGS_COMBINER_FILE_PATTERN = ".*\\.(json|scm)$";

    public static final String APP_NAME = "settings-combiner";
    public static final String APP_NAME_FOR_MAIN = "main-settings-combiner";
    public static final String CURRENT_VERSION = "1.0";

    public static final String SUBSETTINGS_PREFIX = "@";
    public static final String SYSTEM_PREFIX = "$";
    public static final String CLASS_KEY = SYSTEM_PREFIX + "class";
    public static final String SETTINGS = "settings";
    public static final String DEFAULT_SETTINGS_CATEGORY = SETTINGS;
    public static final String DEFAULT_SETTINGS_CATEGORY_PREFIX = SETTINGS + ChainJson.CATEGORY_SEPARATOR;
    public static final String DEFAULT_SETTINGS_COMBINE_PREFIX = "Combine ";
    public static final String DEFAULT_SETTINGS_SPLIT_PREFIX = "Split ";
    public static final String DEFAULT_SETTINGS_GET_NAMES_PREFIX = "Get names of ";
    // Note: split and get-names executors are optional, they are created only if splitId/getNamesId are specified!

    private static final Pattern COMPILED_SETTINGS_COMBINER_FILE_PATTERN =
            Pattern.compile(SETTINGS_COMBINER_FILE_PATTERN);

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

        public Path enumItemsFile(SettingsCombinerJson model) {
            return enumItemsFile == null ? null : model.resolve(Paths.get(enumItemsFile), "enum items");
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

        public void load(SettingsCombinerJson model) {
            final Path file = enumItemsFile(model);
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

        public void completeControlConf(ExecutorJson.ControlConf controlConf) {
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

    private Path settingsCombinerJsonFile = null;
    private boolean main = false;
    private String version = CURRENT_VERSION;
    private boolean autogeneratedCategory = false;
    private String category;
    private boolean autogeneratedName = false;
    private String name;
    private String combineName;
    private String splitName;
    private String getNamesName;
    private String combineDescription = null;
    private String splitDescription = null;
    private String getNamesDescription = null;
    private String id;
    private String splitId = null;
    private String getNamesId = null;
    private Map<String, ExecutorJson.ControlConf> controls = new LinkedHashMap<>();
    private Map<String, ControlConfExtension> controlExtensions = new LinkedHashMap<>();

    // The following properties are not loaded from JSON-file, but are set later,
    // while loading all JSON models for some platform
    private Set<String> tags = new LinkedHashSet<>();;
    private String platformId = null;
    private String platformCategory = null;

    public SettingsCombinerJson() {
    }

    private SettingsCombinerJson(JsonObject json, boolean strictMode, Path file) {
        final int settingsCombinerType = settingsCombinerType(json);
        if (settingsCombinerType == CODE_FOR_INVALID && strictMode) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a settings combiner configuration: no \"app\":\""
                    + APP_NAME + "\" or \"app\":\"" + APP_NAME_FOR_MAIN + "\" element");
        }
        this.settingsCombinerJsonFile = file;
        this.main = settingsCombinerType == CODE_FOR_MAIN;
        this.version = json.getString("version", CURRENT_VERSION);
        this.category = json.getString("category", null);
        if (this.category == null) {
            this.category = DEFAULT_SETTINGS_CATEGORY;
            autogeneratedCategory = true;
        }
        final Path fileName = file == null ? null : file.getFileName();
        this.name = json.getString("name", null);
        if (this.name == null) {
            this.name = fileName == null ? SETTINGS : FileOperation.removeExtension(fileName.toString());
            autogeneratedName = true;
        }
        checkSettingsName(this.name, file);
        this.combineName = json.getString("combine_name", null);
        this.splitName = json.getString("split_name", null);
        this.getNamesName = json.getString("get_names_name", null);
        final String description = json.getString("description", null);
        this.combineDescription = json.getString("combine_description", description);
        this.splitDescription = json.getString("split_description", description);
        this.getNamesDescription = json.getString("get_names_description", description);
        this.id = Jsons.reqString(json, "id", file);
        this.splitId = json.getString("split_id", null);
        this.getNamesId = json.getString("get_names_id", null);
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "controls", file)) {
            final ExecutorJson.ControlConf control = new ExecutorJson.ControlConf(jsonObject, file);
            final String name = control.getName();
            checkParameterName(name, file);
            controls.put(name, control);
            final ControlConfExtension controlExtension = new ControlConfExtension(jsonObject, file);
            controlExtensions.put(name, controlExtension);
        }
    }

    public static SettingsCombinerJson read(Path settingsCombinerJsonFile) throws IOException {
        Objects.requireNonNull(settingsCombinerJsonFile, "Null settingsCombinerJsonFile");
        final JsonObject json = Jsons.readJson(settingsCombinerJsonFile);
        return new SettingsCombinerJson(json, true, settingsCombinerJsonFile);
    }

    public static SettingsCombinerJson readIfValid(Path settingsCombinerJsonFile, boolean onlyMainCombiner) {
        Objects.requireNonNull(settingsCombinerJsonFile, "Null settingsCombinerJsonFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(settingsCombinerJsonFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        final int settingsCombinerType = settingsCombinerType(json);
        if (settingsCombinerType == CODE_FOR_INVALID || (onlyMainCombiner && settingsCombinerType != CODE_FOR_MAIN)) {
            return null;
        }
        return new SettingsCombinerJson(json, true, settingsCombinerJsonFile);
    }

    public static List<SettingsCombinerJson> readAllIfValid(
            Path containingJsonPath,
            boolean recursive,
            boolean onlyMainCombiners)
            throws IOException {
        return ExtensionJson.readAllIfValid(
                null,
                containingJsonPath,
                recursive,
                path -> readIfValid(path, onlyMainCombiners),
                SettingsCombinerJson::isSettingsCombinerJsonFile);
    }

    public void write(Path settingsCombinerJsonFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(settingsCombinerJsonFile, "Null settingsCombinerJsonFile");
        Files.writeString(settingsCombinerJsonFile, Jsons.toPrettyString(toJson()), options);
    }

    public static SettingsCombinerJson valueOf(JsonObject settingsCombinerJson, boolean strictMode) {
        return new SettingsCombinerJson(settingsCombinerJson, strictMode, null);
    }

    public static boolean isSettingsCombinerJsonFile(Path file) {
        Objects.requireNonNull(file, "Null file");
        return COMPILED_SETTINGS_COMBINER_FILE_PATTERN.matcher(file.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isSettingsCombinerJson(JsonObject settingsCombinerJson) {
        return settingsCombinerType(settingsCombinerJson) != CODE_FOR_INVALID;
    }

    public static boolean isSettingsJsonKey(String jsonKey) {
        Objects.requireNonNull(jsonKey, "Null JSON key");
        return jsonKey.startsWith(SUBSETTINGS_PREFIX);
    }

    public static String controlJsonKey(ExecutorJson.ControlConf controlConf) {
        Objects.requireNonNull(controlConf, "Null controlConf");
        final String name = controlConf.getName();
        return controlConf.getValueType() == ParameterValueType.SETTINGS ?
                settingsJsonKey(name) :
                name;
    }

    public static String settingsJsonKey(String subSettingsName) {
        Objects.requireNonNull(subSettingsName, "Null sub-settings name");
        return SUBSETTINGS_PREFIX + subSettingsName;
    }

    public static void checkIdDifference(Collection<SettingsCombinerJson> settingsCombinerJsons) {
        Objects.requireNonNull(settingsCombinerJsons, "Null settings combiner JSONs collection");
        final Set<String> ids = new HashSet<>();
        for (SettingsCombinerJson settingsCombinerJson : settingsCombinerJsons) {
            final String id = settingsCombinerJson.getId();
            assert id != null;
            final String splitId = settingsCombinerJson.getSplitId();
            final String getNamesId = settingsCombinerJson.getGetNamesId();
            // - note: splitId/getNamesId MAY be null
            if (id.equals(splitId)) {
                throw new IllegalArgumentException("Identical id and split_id for settings combiner \""
                        + settingsCombinerJson.getName() + "\": " + id);
            }
            if (id.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical id and get_names_id for settings combiner \""
                        + settingsCombinerJson.getName() + "\": " + id);
            }
            if (splitId != null && splitId.equals(getNamesId)) {
                throw new IllegalArgumentException("Identical split_id and get_names_id for settings combiner \""
                        + settingsCombinerJson.getName() + "\": " + splitId);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + id
                        + ", one of them is \"" + settingsCombinerJson.getName() + "\"");
            }
            if (splitId != null && !ids.add(splitId)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + splitId
                        + ", one of them is \"" + settingsCombinerJson.getName() + "\"");
            }
            if (getNamesId != null && !ids.add(getNamesId)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + getNamesId
                        + ", one of them is \"" + settingsCombinerJson.getName() + "\"");
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

    public Path getSettingsCombinerJsonFile() {
        return settingsCombinerJsonFile;
    }

    public boolean isMain() {
        return main;
    }

    public SettingsCombinerJson setMain(boolean main) {
        this.main = main;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SettingsCombinerJson setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public boolean isAutogeneratedCategory() {
        return autogeneratedCategory;
    }

    public String getCategory() {
        return category;
    }

    public SettingsCombinerJson setCategory(String category) {
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

    public SettingsCombinerJson setName(String name) {
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

    public SettingsCombinerJson setCombineName(String combineName) {
        this.combineName = Objects.requireNonNull(combineName, "Null combineName");
        return this;
    }

    public String splitName() {
        return splitName == null ? DEFAULT_SETTINGS_SPLIT_PREFIX + name : splitName;
    }

    public String getSplitName() {
        return splitName;
    }

    public SettingsCombinerJson setSplitName(String splitName) {
        this.splitName = Objects.requireNonNull(splitName, "Null splitName");
        return this;
    }

    public String getNamesName() {
        return getNamesName == null ? DEFAULT_SETTINGS_GET_NAMES_PREFIX + name : getNamesName;
    }

    public String getGetNamesName() {
        return getNamesName;
    }

    public SettingsCombinerJson setGetNamesName(String getNamesName) {
        this.getNamesName = Objects.requireNonNull(getNamesName, "Null getNamesName");
        return this;
    }

    public String getCombineDescription() {
        return combineDescription;
    }

    public SettingsCombinerJson setCombineDescription(String combineDescription) {
        this.combineDescription = combineDescription;
        return this;
    }

    public String getSplitDescription() {
        return splitDescription;
    }

    public SettingsCombinerJson setSplitDescription(String splitDescription) {
        this.splitDescription = splitDescription;
        return this;
    }

    public String getGetNamesDescription() {
        return getNamesDescription;
    }

    public SettingsCombinerJson setGetNamesDescription(String getNamesDescription) {
        this.getNamesDescription = getNamesDescription;
        return this;
    }

    public String getId() {
        return id;
    }

    public SettingsCombinerJson setId(String id) {
        this.id = Objects.requireNonNull(id, "Null id");
        return this;
    }

    public String getSplitId() {
        return splitId;
    }

    public SettingsCombinerJson setSplitId(String splitId) {
        this.splitId = splitId;
        return this;
    }

    public String getGetNamesId() {
        return getNamesId;
    }

    public SettingsCombinerJson setGetNamesId(String getNamesId) {
        this.getNamesId = getNamesId;
        return this;
    }

    public Map<String, ExecutorJson.ControlConf> getControls() {
        return Collections.unmodifiableMap(controls);
    }

    public SettingsCombinerJson setControls(Map<String, ExecutorJson.ControlConf> controls) {
        controls = ExecutorJson.checkControls(controls);
        this.controls = controls;
        for (ExecutorJson.ControlConf controlConf : controls.values()) {
            checkParameterName(controlConf.getName(), null);
        }
        return this;
    }

    public ExecutorJson.ControlConf getControl(String name) {
        return controls.get(name);
    }

    public Map<String, ControlConfExtension> getControlExtensions() {
        return controlExtensions;
    }

    public SettingsCombinerJson setControlExtensions(Map<String, ControlConfExtension> controlExtensions) {
        this.controlExtensions = Objects.requireNonNull(controlExtensions, "Null controlExtensions");
        return this;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public SettingsCombinerJson setTags(Set<String> tags) {
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

    public SettingsCombinerJson setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public String getPlatformCategory() {
        return platformCategory;
    }

    public SettingsCombinerJson setPlatformCategory(String platformCategory) {
        this.platformCategory = platformCategory;
        return this;
    }

    public void addControl(ExecutorJson.ControlConf control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        controls.put(control.getName(), control);
    }

    public void addFirstControl(ExecutorJson.ControlConf control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        final Map<String, ExecutorJson.ControlConf> controls = new LinkedHashMap<>();
        controls.put(control.getName(), control);
        controls.putAll(this.controls);
        this.controls = controls;
    }

    public Set<String> controlJsonKeySet() {
        return controls.values().stream().map(SettingsCombinerJson::controlJsonKey).collect(Collectors.toSet());
    }

    public String settingsClassMame() {
        return category + ChainJson.CATEGORY_SEPARATOR + name;
    }

    public String parentFolderName() {
        if (settingsCombinerJsonFile == null) {
            return null;
        }
        Path parent = settingsCombinerJsonFile.getParent();
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
            // Usually there are a lot of analogous sub-folders inside the same root folder.
            // (Note that the file MUST be named "frame" to correctly detect, what is it.)
            // Without removing last element, we will produce a tree of functions like the following:
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
                // Note: if className does not contain ".", result will be null, and we will not change category name
                setCategory(DEFAULT_SETTINGS_CATEGORY_PREFIX + recommendedCategory);
            }
        }
    }

    public void updateAutogeneratedName(String className) {
        if (className != null && autogeneratedName) {
            setName(ExecutionBlock.recommendedName(className));
        }
    }

    public boolean matchesClassName(String settingsClassName) {
        Objects.requireNonNull(settingsClassName, "Null settingsClassName");
        try {
            checkSettingsName(name, null);
        } catch (JsonException e) {
            throw new AssertionError("Was not checked before!", e);
        }
        return settingsClassName.endsWith(ChainJson.CATEGORY_SEPARATOR + name);
    }

    public boolean hasPathControl() {
        return controls.values().stream().anyMatch(controlConf -> controlConf.getEditionType().isPath());
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
    }

    @Override
    public String toString() {
        return "SettingsCombinerJson{" +
                "settingsCombinerJsonFile=" + settingsCombinerJsonFile +
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
        if (combineDescription != null) {
            builder.add("combine_description", combineDescription);
        }
        if (splitDescription != null) {
            builder.add("split_description", splitDescription);
        }
        if (getNamesDescription != null) {
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
        for (Map.Entry<String, ExecutorJson.ControlConf> entry : controls.entrySet()) {
            final ExecutorJson.ControlConf control = entry.getValue();
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

    private static int settingsCombinerType(JsonObject settingsCombinerJson) {
        Objects.requireNonNull(settingsCombinerJson, "Null settings combiner JSON");
        final String app = settingsCombinerJson.getString("app", null);
        return APP_NAME.equals(app) ? CODE_FOR_ORDINARY : APP_NAME_FOR_MAIN.equals(app) ? CODE_FOR_MAIN : CODE_FOR_INVALID;
    }

    private static void checkSettingsName(String name, Path file) throws JsonException {
        if (name.contains(String.valueOf(ChainJson.CATEGORY_SEPARATOR))) {
            throw new JsonException("Non-allowed settings name \"" + name
                    + "\"" + (file == null ? "" : " in JSON " + file)
                    + ": it contains \"" + ChainJson.CATEGORY_SEPARATOR + "\" character");
        }
    }

    private Path resolve(Path path, String whatFile) {
        if (path.isAbsolute()) {
            return path;
        }
        if (this.settingsCombinerJsonFile == null) {
            throw new IllegalStateException("Name of " + whatFile
                    + " file is relative and cannot be resolved, because "
                    + "settings combiner JSON was not loaded from file; you must use absolute paths in this case");
        }
        return settingsCombinerJsonFile.getParent().resolve(path);
    }

    public static void main(String[] args) throws IOException {
        SettingsCombinerJson settingsCombinerJson = read(Paths.get(args[0]));
        System.out.println(settingsCombinerJson);
        System.out.println(Jsons.toPrettyString(settingsCombinerJson.toJson()));
    }
}
