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

package net.algart.executors.modules.core.logic.compiler.mappings.model;

import jakarta.json.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.system.ControlEditionType;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.io.MatrixIO;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public final class MappingSpecification extends AbstractConvertibleToJson {
    public static final String MAPPING_FILE_PATTERN = ".*\\.(json|mapping)$";
    public static final String APP_NAME = "mapping";
    public static final String CURRENT_VERSION = "1.0";

    public static final String MAPPING = "mapping";
    public static final String DEFAULT_MAPPING_CATEGORY = "mappings";
    public static final String DEFAULT_MAPPING_CATEGORY_PREFIX =
            DEFAULT_MAPPING_CATEGORY + ChainSpecification.CATEGORY_SEPARATOR;

    private static final Pattern COMPILED_MAPPING_FILE_PATTERN = Pattern.compile(MAPPING_FILE_PATTERN);

    public static final class ControlConfTemplate extends AbstractConvertibleToJson {
        private ParameterValueType valueType = ParameterValueType.STRING;
        private ControlEditionType editionType = null;
        private JsonValue defaultJsonValue = null;

        public ControlConfTemplate() {
        }

        public ControlConfTemplate(JsonObject json, Path file) {
            final String valueType = json.getString("value_type", ParameterValueType.STRING.typeName());
            this.valueType = ParameterValueType.valueOfTypeNameOrNull(valueType);
            Jsons.requireNonNull(this.valueType, json, "value_type",
                    "unknown (\"" + valueType + "\")", file);
            final String editionType = json.getString("edition_type", null);
            if (editionType != null) {
                this.editionType = ControlEditionType.valueOfEditionTypeNameOrNull(editionType);
                Jsons.requireNonNull(this.editionType, json, "edition_type",
                        "unknown (\"" + editionType + "\")", file);
            }
            try {
                setDefaultJsonValue(json.get("default"));
            } catch (IllegalArgumentException e) {
                throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                        + ": invalid control template", e);
            }
        }

        public ParameterValueType getValueType() {
            return valueType;
        }

        public ControlConfTemplate setValueType(ParameterValueType valueType) {
            this.valueType = Objects.requireNonNull(valueType, "Null valueType");
            return this;
        }

        public JsonValue getDefaultJsonValue() {
            return defaultJsonValue;
        }

        public ControlConfTemplate setDefaultJsonValue(JsonValue defaultJsonValue) {
            assert valueType != null;
            if (defaultJsonValue != null) {
                if (valueType.toParameter(defaultJsonValue) == null) {
                    throw new IllegalArgumentException("Incorrect default JSON value \"" + defaultJsonValue
                            + "\": it is not " + valueType);
                }
            }
            this.defaultJsonValue = defaultJsonValue;
            return this;
        }

        public ControlEditionType getEditionType() {
            return editionType;
        }

        public ControlConfTemplate setEditionType(ControlEditionType editionType) {
            this.editionType = editionType;
            return this;
        }

        @Override
        public void checkCompleteness() {
        }

        @Override
        public String toString() {
            return "ControlConfTemplate{" +
                    "valueType=" + valueType +
                    ", editionType=" + editionType +
                    ", defaultJsonValue=" + defaultJsonValue +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("value_type", valueType.typeName());
            if (editionType != null) {
                builder.add("edition_type", editionType.editionTypeName());
            }
            if (defaultJsonValue != null) {
                builder.add("default", defaultJsonValue);
            }
        }
    }

    private Path mappingSpecificationFile = null;
    private String version = CURRENT_VERSION;
    private boolean autogeneratedCategory = false;
    private String category;
    private boolean autogeneratedName = false;
    private String name;
    private String description = null;
    private String id;
    private ControlConfTemplate controlTemplate;
    private String keysFile = null;
    private String enumItemsFile = null;
    private List<String> keys = null;
    private List<String> enumItems = null;
    private Set<String> importantKeys = null;
    private Set<String> ignoredKeys = null;

    public MappingSpecification() {
    }

    private MappingSpecification(JsonObject json, boolean strictMode, Path file) {
        if (!isMappingSpecification(json) && strictMode) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a mapping configuration: no \"app\":\""
                    + APP_NAME + "\" element");
        }
        this.mappingSpecificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        this.category = json.getString("category", null);
        if (this.category == null) {
            this.category = DEFAULT_MAPPING_CATEGORY;
            autogeneratedCategory = true;
        }
        final String fileName = file == null ? null : file.getFileName().toString();
        this.name = json.getString("name", null);
        if (this.name == null) {
            this.name = fileName == null ? MAPPING : MatrixIO.removeExtension(fileName);
            autogeneratedName = true;
        }
        this.description = json.getString("description", null);
        this.id = Jsons.reqString(json, "id", file);
        final JsonObject controlTemplateJson = json.getJsonObject("control_template");
        this.controlTemplate = controlTemplateJson == null ?
                new ControlConfTemplate() :
                new ControlConfTemplate(Jsons.reqJsonObject(json, "control_template"), file);
        this.keysFile = json.getString("keys_file", null);
        this.enumItemsFile = json.getString("enum_items_file", null);
        this.keys = toNames(new ArrayList<>(),
                Jsons.getJsonArray(json, "keys", file), "keys");
        this.enumItems = toNames(new ArrayList<>(),
                Jsons.getJsonArray(json, "enum_items", file), "enum_items");
        this.importantKeys = toNames(new LinkedHashSet<>(),
                Jsons.getJsonArray(json, "important_keys", file), "important_keys");
        this.ignoredKeys = toNames(new LinkedHashSet<>(),
                Jsons.getJsonArray(json, "ignored_keys", file), "ignored_keys");
    }

    public static MappingSpecification read(Path mappingSpecificationFile) throws IOException {
        Objects.requireNonNull(mappingSpecificationFile, "Null mappingSpecificationFile");
        final JsonObject json = Jsons.readJson(mappingSpecificationFile);
        return new MappingSpecification(json, true, mappingSpecificationFile);
    }

    public static MappingSpecification readIfValid(Path mappingSpecificationFile) {
        Objects.requireNonNull(mappingSpecificationFile, "Null mappingSpecificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(mappingSpecificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isMappingSpecification(json)) {
            return null;
        }
        return new MappingSpecification(json, true, mappingSpecificationFile);
    }

    public static List<MappingSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return ExtensionSpecification.readAllJsonIfValid(null, containingJsonPath, MappingSpecification::readIfValid);
    }

    public void write(Path mappingSpecificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(mappingSpecificationFile, "Null mappingSpecificationFile");
        Files.writeString(mappingSpecificationFile, Jsons.toPrettyString(toJson()), options);
    }

    public static MappingSpecification valueOf(JsonObject mappingSpecification, boolean strictMode) {
        return new MappingSpecification(mappingSpecification, strictMode, null);
    }

    public static boolean isMappingSpecificationFile(Path file) {
        Objects.requireNonNull(file, "Null file");
        return COMPILED_MAPPING_FILE_PATTERN.matcher(file.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isMappingSpecification(JsonObject mappingSpecification) {
        Objects.requireNonNull(mappingSpecification, "Null mappingSpecification");
        return APP_NAME.equals(mappingSpecification.getString("app", null));
    }

    public static void checkIdDifference(Collection<MappingSpecification> mappingSpecifications) {
        Objects.requireNonNull(mappingSpecifications, "Null mapping JSONs collection");
        final Set<String> ids = new HashSet<>();
        for (MappingSpecification mappingSpecification : mappingSpecifications) {
            final String id = mappingSpecification.getId();
            assert id != null;
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two mapping JSONs have identical IDs " + id
                        + ", one of them is \"" + mappingSpecification.getName() + "\"");
            }
        }
    }

    public Path getMappingSpecificationFile() {
        return mappingSpecificationFile;
    }

    public String getVersion() {
        return version;
    }

    public MappingSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public boolean isAutogeneratedCategory() {
        return autogeneratedCategory;
    }

    public String getCategory() {
        return category;
    }

    public MappingSpecification setCategory(String category) {
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

    public MappingSpecification setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        this.autogeneratedName = false;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MappingSpecification setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getId() {
        return id;
    }

    public MappingSpecification setId(String id) {
        this.id = Objects.requireNonNull(id, "Null id");
        return this;
    }

    public ControlConfTemplate getControlTemplate() {
        return controlTemplate;
    }

    public MappingSpecification setControlTemplate(ControlConfTemplate controlTemplate) {
        this.controlTemplate = Objects.requireNonNull(controlTemplate, "Null controlTemplate");
        return this;
    }

    public String getKeysFile() {
        return keysFile;
    }

    public MappingSpecification setKeysFile(String keysFile) {
        this.keysFile = keysFile;
        return this;
    }

    public String getEnumItemsFile() {
        return enumItemsFile;
    }

    public MappingSpecification setEnumItemsFile(String enumItemsFile) {
        this.enumItemsFile = enumItemsFile;
        return this;
    }

    public boolean hasKeys() {
        return keys != null;
    }

    public List<String> getKeys() {
        return keys;
    }

    public MappingSpecification setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }

    public boolean hasEnumItems() {
        return enumItems != null;
    }

    public List<String> getEnumItems() {
        return enumItems;
    }

    public MappingSpecification setEnumItems(List<String> enumItems) {
        this.enumItems = enumItems;
        return this;
    }

    public Set<String> getImportantKeys() {
        return importantKeys;
    }

    public MappingSpecification setImportantKeys(Set<String> importantKeys) {
        this.importantKeys = importantKeys;
        return this;
    }

    public Set<String> getIgnoredKeys() {
        return ignoredKeys;
    }

    public MappingSpecification setIgnoredKeys(Set<String> ignoredKeys) {
        this.ignoredKeys = ignoredKeys;
        return this;
    }

    public ControlEditionType editionTypeOrDefault() {
        if (controlTemplate.editionType != null) {
            return controlTemplate.editionType;
        }
        return hasEnumItems() || enumItemsFile != null ? ControlEditionType.ENUM : ControlEditionType.VALUE;
    }

    public String parentFolderName() {
        if (mappingSpecificationFile == null) {
            return null;
        }
        Path parent = mappingSpecificationFile.getParent();
        if (parent == null) {
            return null;
        }
        parent = parent.getFileName();
        if (parent == null) {
            return null;
        }
        return parent.toString();
    }

    public void updateAutogeneratedCategory(boolean useFullClassName) {
        final String parent = parentFolderName();
        if (parent != null && autogeneratedCategory) {
            final String recommendedCategory = useFullClassName ?
                    parent :
                    ExecutionBlock.recommendedCategory(parent);
            if (recommendedCategory != null) {
                setCategory(DEFAULT_MAPPING_CATEGORY_PREFIX + recommendedCategory);
            }
        }
    }

    public boolean isEnum() {
        return editionTypeOrDefault().isEnum();
    }

    public Path keysFile() {
        return keysFile == null ? null : resolve(Paths.get(keysFile), "keys");
    }

    public Path enumItemsFile() {
        return enumItemsFile == null ? null : resolve(Paths.get(enumItemsFile), "enum items");
    }

    public ExecutorSpecification.ControlConf buildControlConf(
            String name,
            List<String> enumItemValues,
            List<String> enumItemCaptions,
            boolean advancedParameters) {
        final ExecutorSpecification.ControlConf result = new ExecutorSpecification.ControlConf()
                .setName(name)
                .setValueType(controlTemplate.valueType)
                .setEditionType(editionTypeOrDefault())
                .setDefaultJsonValue(controlTemplate.defaultJsonValue);
        if (advancedParameters && importantKeys != null && !importantKeys.contains(name)) {
            result.setAdvanced(true);
        }
        if (enumItemValues != null) {
            result.setItemsFromLists(enumItemValues, enumItemCaptions);
        }
        return result;
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
        checkNull(controlTemplate, "controlTemplate");
    }

    @Override
    public String toString() {
        return "MappingSpecification{" +
                "mappingSpecificationFile=" + mappingSpecificationFile +
                ", version='" + version + '\'' +
                ", category='" + category + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", id='" + id + '\'' +
                ", controlTemplate=" + controlTemplate +
                ", keysFile='" + keysFile + '\'' +
                ", enumItemsFile='" + enumItemsFile + '\'' +
                ", keys=" + keys +
                ", enumItems=" + enumItems +
                ", importantKeys=" + importantKeys +
                ", ignoredKeys=" + ignoredKeys +
                '}';
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", APP_NAME);
        builder.add("version", version);
        builder.add("category", category);
        builder.add("name", name);
        if (description != null) {
            builder.add("description", description);
        }
        builder.add("id", id);
        builder.add("control_template", controlTemplate.toJson());
        if (keysFile != null) {
            builder.add("keys_file", keysFile);
        }
        if (enumItemsFile != null) {
            builder.add("enum_items_file", enumItemsFile);
        }
        if (keys != null) {
            builder.add("keys", Jsons.toJsonArray(keys));
        }
        if (enumItems != null) {
            builder.add("enum_items", Jsons.toJsonArray(enumItems));
        }
        if (importantKeys != null) {
            builder.add("important_keys", Jsons.toJsonArray(importantKeys));
        }
        if (ignoredKeys != null) {
            builder.add("ignored_keys", Jsons.toJsonArray(ignoredKeys));
        }
    }

    private Path resolve(Path path, String whatFile) {
        if (path.isAbsolute()) {
            return path;
        }
        if (this.mappingSpecificationFile == null) {
            throw new IllegalStateException("Name of " + whatFile
                    + " file is relative and cannot be resolved, because "
                    + "mapping JSON was not loaded from file; you must use absolute paths for such mapping JSONs");
        }
        return mappingSpecificationFile.getParent().resolve(path);
    }

    private static <C extends Collection<String>> C toNames(C result, JsonArray names, String whatList) {
        if (names == null) {
            return null;
        }
        for (JsonValue value : names) {
            if (!(value instanceof JsonString jsonString)) {
                throw new JsonException("Illegal value \"" + value + "\" in the list \""
                        + whatList + "\": it is not JSON string");
            }
            result.add(jsonString.getString());
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        MappingSpecification mappingSpecification = read(Paths.get(args[0]));
        System.out.println(mappingSpecification);
        System.out.println(Jsons.toPrettyString(mappingSpecification.toJson()));
    }
}
