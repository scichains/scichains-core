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

package net.algart.executors.api.mappings;

import jakarta.json.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.system.ControlEditionType;
import net.algart.executors.api.system.ControlSpecification;
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
    /**
     * Mapping specification file extensions:<br>
     * .json<br>
     * .map ("mapping")<br>
     * .mapping
     */
    public static final String MAPPING_FILE_PATTERN = ".*\\.(json|map|mapping)$";
    public static final String APP_NAME = "mapping";
    public static final String CURRENT_VERSION = "1.0";

    public static final String MAPPING = "mapping";
    public static final String DEFAULT_MAPPING_CATEGORY = "mappings";
    public static final String DEFAULT_MAPPING_CATEGORY_PREFIX =
            DEFAULT_MAPPING_CATEGORY + ExecutorSpecification.CATEGORY_SEPARATOR;

    private static final Pattern COMPILED_MAPPING_FILE_PATTERN = Pattern.compile(MAPPING_FILE_PATTERN);

    public static final class ControlTemplate extends AbstractConvertibleToJson {
        private ParameterValueType valueType = ParameterValueType.STRING;
        private ControlEditionType editionType = null;
        private JsonValue defaultJsonValue = null;

        public ControlTemplate() {
        }

        public ControlTemplate(JsonObject json, Path file) {
            final String valueTypeName = json.getString("value_type", ParameterValueType.STRING.typeName());
            this.valueType = ParameterValueType.fromTypeName(valueTypeName).orElseThrow(
                    () -> Jsons.unknownValue(json, "value_type", valueTypeName, file));
            final String editionTypeName = json.getString("edition_type", null);
            if (editionTypeName != null) {
                this.editionType = ControlEditionType.fromTypeName(editionTypeName).orElseThrow(
                        () -> Jsons.unknownValue(json, "edition_type", editionTypeName, file));
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

        public ControlTemplate setValueType(ParameterValueType valueType) {
            this.valueType = Objects.requireNonNull(valueType, "Null valueType");
            return this;
        }

        public JsonValue getDefaultJsonValue() {
            return defaultJsonValue;
        }

        public ControlTemplate setDefaultJsonValue(JsonValue defaultJsonValue) {
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

        public ControlTemplate setEditionType(ControlEditionType editionType) {
            this.editionType = editionType;
            return this;
        }

        @Override
        public void checkCompleteness() {
        }

        @Override
        public String toString() {
            return "ControlTemplate{" +
                    "valueType=" + valueType +
                    ", editionType=" + editionType +
                    ", defaultJsonValue=" + defaultJsonValue +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            if (valueType != ParameterValueType.STRING) {
                builder.add("value_type", valueType.typeName());
            }
            if (editionType != null) {
                builder.add("edition_type", editionType.typeName());
            }
            if (defaultJsonValue != null) {
                builder.add("default", defaultJsonValue);
            }
        }
    }

    private Path specificationFile = null;
    private String version = CURRENT_VERSION;
    private boolean autogeneratedCategory = false;
    private String category;
    private boolean autogeneratedName = false;
    private String name;
    private String className = null;
    private String description = null;
    private String id;
    private ControlTemplate controlTemplate;
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
        this.specificationFile = file;
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
        checkMappingName(this.name, file);
        this.className = json.getString("class_name", null);
        this.description = json.getString("description", null);
        this.id = Jsons.reqString(json, "id", file);
        final JsonObject controlTemplateJson = json.getJsonObject("control_template");
        this.controlTemplate = controlTemplateJson == null ?
                new ControlTemplate() :
                new ControlTemplate(Jsons.reqJsonObject(json, "control_template"), file);
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

    public static MappingSpecification read(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        return new MappingSpecification(json, true, specificationFile);
    }

    public static MappingSpecification readIfValid(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(specificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isMappingSpecification(json)) {
            return null;
        }
        return new MappingSpecification(json, true, specificationFile);
    }

    public static List<MappingSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return ExecutorSpecification.readAllIfValid(
                null,
                containingJsonPath,
                true,
                MappingSpecification::readIfValid,
                MappingSpecification::isMappingSpecificationFile);
    }

    public void write(Path specificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        Files.writeString(specificationFile, Jsons.toPrettyString(toJson()), options);
    }

    public static MappingSpecification of(JsonObject specificationJson, boolean strictMode) {
        return new MappingSpecification(specificationJson, strictMode, null);
    }

    public static boolean isMappingSpecificationFile(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        return COMPILED_MAPPING_FILE_PATTERN.matcher(specificationFile.getFileName().toString().toLowerCase()).matches();
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

    public Path getSpecificationFile() {
        return specificationFile;
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

    public MappingSpecification setCategory(String category, boolean autogeneratedCategory) {
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

    public MappingSpecification setName(String name, boolean autogeneratedName) {
        Objects.requireNonNull(name, "Null name");
        checkMappingName(name, null);
        this.name = name;
        this.autogeneratedName = autogeneratedName;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public String className() {
        return className == null ? ExecutorSpecification.className(category, name) : className;
    }

    public MappingSpecification setClassName(String className) {
        this.className = className;
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

    public ControlTemplate getControlTemplate() {
        return controlTemplate;
    }

    public MappingSpecification setControlTemplate(ControlTemplate controlTemplate) {
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

    public void updateAutogeneratedCategory(boolean removeLastElementInClassName) {
        final String parent = parentFolderName();
        if (parent != null && autogeneratedCategory) {
            final String recommendedCategory = !removeLastElementInClassName ?
                    parent :
                    ExecutionBlock.recommendedCategory(parent);
            if (recommendedCategory != null) {
                setCategory(DEFAULT_MAPPING_CATEGORY_PREFIX + recommendedCategory, true);
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

    public ControlSpecification buildControlSpecification(
            String name,
            List<String> enumItemValues,
            List<String> enumItemCaptions,
            boolean advancedParameters) {
        final ControlSpecification result = new ControlSpecification()
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
                "specificationFile=" + specificationFile +
                ", version='" + version + '\'' +
                ", category='" + category + '\'' +
                ", name='" + name + '\'' +
                ", className='" + className + '\'' +
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
        if (!version.equals(CURRENT_VERSION)) {
            builder.add("version", version);
        }
        if (!autogeneratedCategory) {
            builder.add("category", category);
        }
        if (!autogeneratedName) {
            builder.add("name", name);
        }
        if (className != null) {
            builder.add("class_name", className);
        }
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
        if (this.specificationFile == null) {
            throw new IllegalStateException("Name of " + whatFile
                    + " file is relative and cannot be resolved, because "
                    + "mapping JSON was not loaded from file; you must use absolute paths for such mapping JSONs");
        }
        return specificationFile.getParent().resolve(path);
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

    private static void checkMappingName(String name, Path file) throws JsonException {
        if (name.contains(String.valueOf(ExecutorSpecification.CATEGORY_SEPARATOR))) {
            throw new JsonException("Non-allowed mapping name \"" + name
                    + "\"" + (file == null ? "" : " in JSON " + file)
                    + ": it contains \"" + ExecutorSpecification.CATEGORY_SEPARATOR + "\" character");
        }
    }


    public static void main(String[] args) throws IOException {
        MappingSpecification mappingSpecification = read(Paths.get(args[0]));
        System.out.println(mappingSpecification);
        System.out.println(Jsons.toPrettyString(mappingSpecification.toJson()));
    }
}
