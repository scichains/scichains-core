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

package net.algart.executors.api.system;

import jakarta.json.*;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.parameters.ValueType;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ControlSpecification extends AbstractConvertibleToJson implements Cloneable {
    public static final String SUPPRESS_WARNING_NO_SETTER = "no_setter";

    public static final class EnumItem extends AbstractConvertibleToJson implements Cloneable {
        private JsonValue value;
        private String caption = null;

        public EnumItem() {
        }

        public EnumItem(JsonValue value) {
            setValue(value);
        }

        public EnumItem(String value) {
            setValue(value);
        }

        public EnumItem(JsonObject json, Path file) {
            this.value = Jsons.reqJsonValue(json, "value", file);
            this.caption = json.getString("caption", null);
        }

        public JsonValue getValue() {
            return value;
        }

        public EnumItem setValue(JsonValue value) {
            this.value = Objects.requireNonNull(value, "Null value");
            return this;
        }

        public EnumItem setValue(String value) {
            Objects.requireNonNull(value, "Null value");
            this.value = Jsons.stringValue(value);
            return this;
        }

        public String getCaption() {
            return caption;
        }

        public EnumItem setCaption(String caption) {
            this.caption = caption;
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(value, "value");
        }

        @Override
        public String toString() {
            return "EnumItem{" +
                    "value='" + value + '\'' +
                    ", caption='" + caption + '\'' +
                    '}';
        }

        @Override
        public EnumItem clone() {
            try {
                return (EnumItem) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("value", value);
            if (caption != null) {
                builder.add("caption", caption);
            }
        }
    }

    private String name;
    private String description = null;
    private String caption = null;
    private String hint = null;
    private ValueType valueType;
    private volatile String valueClassName = null;
    // - can be the name of some class of similar values; for example,
    // for value-type "settings" it may be the SettingsSpecification.settingsClass()
    private EditionType editionType = EditionType.VALUE;
    private volatile String settingsId = null;
    // - settings ID (for value-type "settings");
    // it is the only field that is sometimes modifying in a ready specification
    // (by SmartSearchSettings class)
    private boolean multiline = false;
    private Integer editionRows = null;
    // - recommended number of lines in "multiline" mode
    private boolean resources = false;
    // - note: by default, it is true if editionType.isResources() is true
    private boolean advanced = false;
    private List<EnumItem> items = null;
    private String itemsFile = null;
    private List<String> itemNamesInFile = null;
    private List<String> itemCaptionsInFile = null;
    private List<String> suppressWarnings = null;
    private JsonValue defaultJsonValue = null;

    public ControlSpecification() {
    }

    public ControlSpecification(JsonObject json, Path file) {
        this.name = Jsons.reqString(json, "name", file);
        this.description = json.getString("description", null);
        this.caption = json.getString("caption", null);
        this.hint = json.getString("hint", null);
        final String valueTypeName = Jsons.reqString(json, "value_type", file);
        this.valueType = ValueType.fromTypeName(valueTypeName).orElseThrow(
                () -> Jsons.badValue(json, "value_type", valueTypeName, ValueType.typeNames(), file));
        this.valueClassName = json.getString("value_class_name", null);
        final String editionTypeName = json.getString("edition_type", EditionType.VALUE.typeName());
        this.editionType = EditionType.fromTypeName(editionTypeName).orElseThrow(
                () -> Jsons.badValue(json, "edition_type", editionTypeName, EditionType.typeNames(), file));
        this.settingsId = json.getString("settings_id", null);
        this.multiline = json.getBoolean("multiline", false);
        final JsonNumber editionRows = json.getJsonNumber("edition_rows");
        this.editionRows = editionRows == null ? null : editionRows.intValue();
        if (this.editionRows != null && this.editionRows <= 0) {
            throw new IllegalArgumentException("Zero or negative number of rows = " + this.editionRows);
        }
        this.resources = json.getBoolean("resources", this.editionType.isResources());
        this.advanced = json.getBoolean("advanced", false);
        if (this.editionType == EditionType.ENUM) {
            if (this.valueType == ValueType.STRING) {
                // for other value types, the "enum" edition type does not affect
                // the way of setting the value: Executor still has a setter
                // like setXxx(int value)
                this.valueType = ValueType.ENUM_STRING;
            }
            // Note: we allow skipping "items" in this case because
            // some external libraries can add items from other sources.
        }
        final JsonArray itemsJson = json.getJsonArray("items");
        if (itemsJson != null) {
            this.items = new ArrayList<>();
            for (JsonValue jsonValue : itemsJson) {
                if (!(jsonValue instanceof JsonObject)) {
                    throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                            + ": in control \"" + name + "\", \"items\" array contains non-object element "
                            + jsonValue);
                }
                this.items.add(new EnumItem((JsonObject) jsonValue, file));
            }
        }
        this.itemsFile = json.getString("items_file", null);
        final JsonArray suppressWarningsJson = json.getJsonArray("suppress_warnings");
        if (suppressWarningsJson != null) {
            this.suppressWarnings = new ArrayList<>();
            for (JsonValue jsonValue : suppressWarningsJson) {
                if (!(jsonValue instanceof JsonString jsonString)) {
                    throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                            + ": in control \"" + name +
                            "\", \"suppress_warnings\" array contains non-string element "
                            + jsonValue);
                }
                this.suppressWarnings.add(jsonString.getString());
            }
        }
        loadExternalData(file);
        try {
            setDefaultJsonValue(json.get("default"));
        } catch (IllegalArgumentException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": invalid control \"" + name + "\" (" + e.getMessage() + ")", e);
        }
    }

    public String getName() {
        return name;
    }

    public ControlSpecification setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ControlSpecification setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public ControlSpecification setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getHint() {
        return hint;
    }

    public ControlSpecification setHint(String hint) {
        this.hint = hint;
        return this;
    }

    public ValueType getValueType() {
        assert valueType != null : "valueType cannot be null";
        return valueType;
    }

    public ControlSpecification setValueType(ValueType valueType) {
        this.valueType = Objects.requireNonNull(valueType, "Null valueType");
        return this;
    }

    public String getValueClassName() {
        return valueClassName;
    }

    public ControlSpecification setValueClassName(String valueClassName) {
        this.valueClassName = valueClassName;
        return this;
    }

    public EditionType getEditionType() {
        return editionType;
    }

    public ControlSpecification setEditionType(EditionType editionType) {
        this.editionType = Objects.requireNonNull(editionType, "Null editionType");
        return this;
    }

    public String getSettingsId() {
        return settingsId;
    }

    public ControlSpecification setSettingsId(String settingsId) {
        this.settingsId = settingsId;
        return this;
    }

    public boolean isMultiline() {
        return multiline;
    }

    public ControlSpecification setMultiline(boolean multiline) {
        this.multiline = multiline;
        return this;
    }

    public Integer getEditionRows() {
        return editionRows;
    }

    public ControlSpecification setEditionRows(Integer editionRows) {
        if (editionRows != null && editionRows <= 0) {
            throw new IllegalArgumentException("Zero or negative number of rows = " + editionRows);
        }
        this.editionRows = editionRows;
        return this;
    }

    public boolean isResources() {
        return resources;
    }

    public ControlSpecification setResources(boolean resources) {
        this.resources = resources;
        return this;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public ControlSpecification setAdvanced(boolean advanced) {
        this.advanced = advanced;
        return this;
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public List<EnumItem> getItems() {
        return items == null ? null : Collections.unmodifiableList(items);
    }

    public ControlSpecification setItems(List<EnumItem> items) {
        this.items = items == null ? null : new ArrayList<>(items);
        return this;
    }

    public String getItemsFile() {
        return itemsFile;
    }

    public ControlSpecification setItemsFile(String itemsFile) {
        this.itemsFile = itemsFile;
        return this;
    }

    public Path itemsFile(Path siblingSpecificationFile) {
        return itemsFile == null ?
                null :
                resolveAgainstParent(siblingSpecificationFile, Paths.get(itemsFile));
    }

    public List<String> itemNamesInFile() {
        return itemNamesInFile;
        // - unmodifiable
    }

    public List<String> itemCaptionsInFile() {
        return itemCaptionsInFile;
        // - unmodifiable
    }

    public List<String> getSuppressWarnings() {
        return suppressWarnings == null ? null : Collections.unmodifiableList(suppressWarnings);
    }

    public ControlSpecification setSuppressWarnings(List<String> suppressWarnings) {
        this.suppressWarnings = suppressWarnings == null ? null : new ArrayList<>(suppressWarnings);
        return this;
    }

    public boolean hasDefaultJsonValue() {
        return defaultJsonValue != null;
    }

    public JsonValue getDefaultJsonValue() {
        return defaultJsonValue;
    }

    public Object getDefaultValue() {
        return getValueType().toParameter(this.defaultJsonValue);
    }

    public ControlSpecification setDefaultJsonValue(JsonValue defaultJsonValue) {
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

    public ControlSpecification setDefaultStringValue(String defaultStringValue) {
        if (defaultStringValue == null) {
            this.defaultJsonValue = null;
        } else {
            this.defaultJsonValue = Jsons.stringValue(defaultStringValue);
        }
        return this;
    }

    public void setItemsFromLists(List<String> itemValues, List<String> itemCaptions) {
        Objects.requireNonNull(itemValues, "Null itemValues");
        final int itemCaptionsSize = itemCaptions == null ? 0 : itemCaptions.size();
        this.items = new ArrayList<>();
        for (int i = 0, n = itemValues.size(); i < n; i++) {
            String enumItem = itemValues.get(i);
            EnumItem item = new EnumItem(enumItem);
            if (i < itemCaptionsSize) {
                item.setCaption(itemCaptions.get(i));
            }
            this.items.add(item);
        }
        if (defaultJsonValue == null && !this.items.isEmpty()) {
            // - usually enumItemNames cannot be empty; it is checked in the constructor of MappingBuilder class
            setDefaultJsonValue(items.get(0).value);
        }
    }

    public boolean isSubSettings() {
        return getValueType().isSettings() && !SettingsSpecification.SETTINGS.equals(name);
        // - SETTINGS is a special parameter (probably inside CombineSettings)
        // for customizing the whole settings tree, this is not a SUB-settings
    }

    public String key() {
        return getValueType().isSettings() ? settingsKey(name) : name;
    }

    public static String settingsKey(String subSettingsName) {
        Objects.requireNonNull(subSettingsName, "Null sub-settings name");
        return SettingsSpecification.SUBSETTINGS_PREFIX + subSettingsName;
    }

    @Override
    public void checkCompleteness() {
        checkNull(name, "name");
        checkNull(valueType, "valueType");
        assert editionType != null;
    }

    @Override
    public String toString() {
        return "ControlSpecification{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", caption='" + caption + '\'' +
                ", hint='" + hint + '\'' +
                ", valueType=" + valueType +
                ", valueClassName='" + valueClassName + '\'' +
                ", editionType=" + editionType +
                ", settingsID='" + settingsId + '\'' +
                ", multiline=" + multiline +
                ", editionRows=" + editionRows +
                ", resources=" + resources +
                ", advanced=" + advanced +
                ", items=" + items +
                ", itemsFile='" + itemsFile + '\'' +
                ", suppressWarnings=" + suppressWarnings +
                ", defaultJsonValue=" + defaultJsonValue +
                '}';
    }

    @Override
    public ControlSpecification clone() {
        try {
            final ControlSpecification result = (ControlSpecification) super.clone();
            if (this.items != null) {
                result.items = this.items.stream().map(EnumItem::clone).collect(Collectors.toList());
            }
            if (this.suppressWarnings != null) {
                result.suppressWarnings = new ArrayList<>(this.suppressWarnings);
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("name", name);
        if (description != null) {
            builder.add("description", description);
        }
        if (caption != null) {
            builder.add("caption", caption);
        }
        if (hint != null) {
            builder.add("hint", hint);
        }
        builder.add("value_type", valueType.typeName());
        if (valueClassName != null) {
            builder.add("value_class_name", valueClassName);
        }
        builder.add("edition_type", editionType.typeName());
        if (settingsId != null) {
            builder.add("settings_id", settingsId);
        }
        if (multiline) {
            builder.add("multiline", multiline);
        }
        if (editionRows != null) {
            builder.add("edition_rows", editionRows);
        }
        if (resources) {
            builder.add("resources", resources);
        }
        if (advanced) {
            builder.add("advanced", advanced);
        }
        if (items != null) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (EnumItem value : items) {
                arrayBuilder.add(value.toJson());
            }
            builder.add("items", arrayBuilder.build());
        }
        if (itemsFile != null) {
            builder.add("items_file", itemsFile);
        }
        if (suppressWarnings != null) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (String value : suppressWarnings) {
                arrayBuilder.add(value);
            }
            builder.add("suppress_warnings", arrayBuilder.build());
        }
        if (defaultJsonValue != null) {
            builder.add("default", defaultJsonValue);
        }
    }

    /**
     * Loads all additional data stored in external files, like {@link #getItemsFile()},
     * if the argument is not <code>null</code>.
     *
     * <p>If the necessary data is already specified in JSON, for example,
     * if the enum items are already loaded and serialized in JSON, this method does nothing.
     * In the other case, and if we really have some external files ({@link #getItemsFile()}),
     * this method uses the argument <code>siblingSpecificationFile</code>,
     * usually the specification file of the executor or some other object like {@link SettingsSpecification}:
     * relative paths to external files will be resolved against its parent folder.
     *
     * <p>If <code>siblingSpecificationFile==null</code>, this method does nothing.
     * This is a typical situation, for example, while deserialization from JSON;
     * in this case, all external data should be loaded while first reading the specification from the file
     * and then included in the JSON when serializing.
     *
     * <p>This method is automatically called from the constructor
     * {@link #ControlSpecification(JsonObject, Path)}.</p>
     *
     * @param siblingSpecificationFile some file (usually a specification file) for resolving
     *                                 relative external files against its parent folder;
     *                                 can be <code>null</code>, then the method does nothing.
     */
    public void loadExternalData(Path siblingSpecificationFile) {
        if (siblingSpecificationFile == null) {
            return;
        }
        if (items == null) {
            final Path file = itemsFile(siblingSpecificationFile);
            if (file != null) {
                try {
                    loadItems(file);
                } catch (IOException e) {
                    throw new JsonException("Cannot load items file " + file.toAbsolutePath(), e);
                }
                assert items != null : "items were not correctly loaded";
            }
        }
    }

    public void loadItems(Path itemsFile) throws IOException {
        Objects.requireNonNull(itemsFile, "Null items file");
        final String s = Files.readString(itemsFile);
        final SScalar.MultiLineOrJsonSplitter items = SScalar.splitJsonOrTrimmedLinesWithComments(s);
        if (items.numberOfLines() == 0) {
            throw new JsonException("No enum items in the file " + itemsFile.toAbsolutePath());
        }
        this.itemNamesInFile = items.lines();
        this.itemCaptionsInFile = items.comments();
        assert this.itemNamesInFile != null;
        assert this.itemCaptionsInFile != null;
        // - unmodifiable
        setItemsFromLists(itemNamesInFile, itemCaptionsInFile);
    }

    private static Path resolveAgainstParent(Path siblingSpecificationFile, Path path) {
        Objects.requireNonNull(siblingSpecificationFile, "Null sibling specification file");
        if (path.isAbsolute()) {
            return path;
        }
        final Path parent = siblingSpecificationFile.getParent();
        return parent == null ? path : parent.resolve(path);
    }
}
