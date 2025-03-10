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
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.system.ControlSpecification;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings combiner, allowing to make and parse settings JSON.
 * Note: this class does not contain settings itself, the settings are stored in JSON,
 * this class only allows manipulating them.
 */
public class SettingsBuilder implements Cloneable {
    public static final boolean ABSOLUTE_PATHS_DEFAULT_VALUE = true;
    public static final String PATH_PARENT_FOLDER_SUFFIX = "_parent";
    public static final String PATH_FILE_NAME_SUFFIX = "_name";

    private final SettingsSpecification specification;
    // - unlike such classes as Chain or Executor, the settings specification exactly describes this object

    private boolean absolutePaths = ABSOLUTE_PATHS_DEFAULT_VALUE;
    private boolean addSettingsClass = false;
    private boolean extractSubSettings = false;

    private volatile Object customSettingsInformation = null;

    protected SettingsBuilder(SettingsSpecification specification) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.specification.checkCompleteness();
    }

    public static SettingsBuilder read(Path specificationFile) throws IOException {
        return of(SettingsSpecification.read(specificationFile));
    }

    public static SettingsBuilder of(SettingsSpecification specification) {
        return new SettingsBuilder(specification);
    }

    public boolean isAbsolutePaths() {
        return absolutePaths;
    }

    public SettingsBuilder setAbsolutePaths(boolean absolutePaths) {
        this.absolutePaths = absolutePaths;
        return this;
    }

    public boolean isAddSettingsClass() {
        return addSettingsClass;
    }

    // We decided not to add this information by UseSettings.ADD_SETTINGS_CLASS_PARAMETER_NAME:
    // the settings are usually created by external dashboard, direct using combiner is not a typical case
    public SettingsBuilder setAddSettingsClass(boolean addSettingsClass) {
        this.addSettingsClass = addSettingsClass;
        return this;
    }

    public boolean isExtractSubSettings() {
        return extractSubSettings;
    }

    public SettingsBuilder setExtractSubSettings(boolean extractSubSettings) {
        this.extractSubSettings = extractSubSettings;
        return this;
    }

    public Object getCustomSettingsInformation() {
        return customSettingsInformation;
    }

    public SettingsBuilder setCustomSettingsInformation(Object customSettingsInformation) {
        this.customSettingsInformation = customSettingsInformation;
        return this;
    }

    public SettingsSpecification specification() {
        return specification;
    }

    public Path specificationFile() {
        return specification.getSpecificationFile();
    }

    /**
     * Return settings ID. This is always equal to executor ID of the corresponding "Combine settings" executor.
     *
     * @return settings ID.
     */
    public String id() {
        return specification.getId();
    }

    /**
     * Returns executor ID for the corresponding "Split settings" executor.
     * May be <code>null</code>, in which case that executor is not created.
     *
     * @return executor ID for "Split settings".
     */
    public String splitId() {
        return specification.getSplitId();
    }

    /**
     * Returns executor ID for the corresponding "Get names" executor.
     * May be <code>null</code>, in which case that executor is not created.
     *
     * @return executor ID for "Get names of settings".
     */
    public String getNamesId() {
        return specification.getGetNamesId();
    }

    public boolean isAutogeneratedCategory() {
        return specification.isAutogeneratedCategory();
    }

    public String category() {
        return specification.getCategory();
    }

    public String name() {
        return specification.getName();
    }

    public String combineName() {
        return specification.combineName();
    }

    public String splitName() {
        return specification.splitName();
    }

    public String className() {
        return specification.className();
    }

    public String getNamesName() {
        return specification.getNamesName();
    }

    public String combineDescription() {
        return specification.getCombineDescription();
    }

    public String splitDescription() {
        return specification.getSplitDescription();
    }

    public String getNamesDescription() {
        return specification.getGetNamesDescription();
    }

    public boolean hasPlatformId() {
        return specification.hasPlatformId();
    }

    public Set<String> tags() {
        return specification.getTags();
    }

    public String platformId() {
        return specification.getPlatformId();
    }

    public String platformCategory() {
        return specification.getPlatformCategory();
    }

    public Set<String> settingsKeySet() {
        return specification.controlKeySet();
    }

    public boolean hasPathControl() {
        return specification.hasPathControl();
    }

    public JsonObject buildDefault() {
        return buildExecutor(null, false);
    }

    public JsonObject build(Executor executor, JsonObject defaultSettings) {
        final JsonObject result = build(executor);
        return defaultSettings == null ? result : Jsons.overrideEntries(defaultSettings, result);
    }

    public JsonObject build(Executor executor) {
        return buildExecutor(executor, true);
    }

    public JsonObject build(Parameters parameters) {
        return buildParametersAndPorts(null, parameters, null);
    }

    public void parseSettingsToParameters(Parameters parameters, JsonObject settings) {
        Objects.requireNonNull(parameters, "Null parameters");
        Objects.requireNonNull(settings, "Null settings");
        for (ControlSpecification control : specification.getControls().values()) {
            JsonValue jsonValue = settings.get(control.getName());
            if (jsonValue != null) {
                setJsonValue(control, parameters, jsonValue);
            }
        }
    }

    public void splitSettingsToOutputPorts(Executor executor, JsonObject settings) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(settings, "Null settings");
        for (ControlSpecification control : specification.getControls().values()) {
            final String name = portName(control);
            if (executor.hasOutputPort(name)) {
                final ParameterValueType valueType = control.getValueType();
                final String jsonKey = control.key();
                JsonValue jsonValue = settings.get(jsonKey);
                if (jsonValue == null) {
                    jsonValue = control.getDefaultJsonValue();
                }
                if (jsonValue == null) {
                    jsonValue = valueType.emptyJsonValue();
                }
                assert jsonValue != null;
                final Object value;
                if (valueType.isSettings()) {
                    JsonObject subSettings = jsonValue instanceof JsonObject jo ? jo : Jsons.newEmptyJson();
                    // - subSettings CAN be not a JsonObject, if the source JSON was created manually
                    subSettings = Jsons.overrideOnlyExistingInBoth(subSettings, Jsons.extractSimpleValues(settings));
                    value = Jsons.toPrettyString(subSettings);
                } else {
                    value = valueType.toSmartParameter(jsonValue);
                    // - note: we don't insist on returning value of a correct type, because SScalar
                    // does not distinguish types
                }
                executor.getScalar(name).setTo(value);
                if (valueType == ParameterValueType.STRING
                        && value instanceof String
                        && control.getEditionType().isPath()) {
                    final Path path;
                    try {
                        path = Paths.get((String) value);
                    } catch (Exception ignored) {
                        // - if it is not a path, it is better just to stay 2 advanced ports empty
                        continue;
                    }
                    final String parentFolderPort = name + PATH_PARENT_FOLDER_SUFFIX;
                    if (executor.hasOutputPort(parentFolderPort)) {
                        final Path parent = path.getParent();
                        if (parent != null) {
                            executor.getScalar(parentFolderPort).setTo(parent.toString());
                        }
                    }
                    final String fileNamePort = name + PATH_FILE_NAME_SUFFIX;
                    if (executor.hasOutputPort(fileNamePort)) {
                        final Path fileName = path.getFileName();
                        if (fileName != null) {
                            executor.getScalar(fileNamePort).setTo(fileName.toString());
                        }
                    }
                }
            }
        }
    }

    public JsonObject overrideSettings(JsonObject executorSettings, JsonObject overridingParent) {
        Objects.requireNonNull(executorSettings, "Null executorSettings");
        Objects.requireNonNull(overridingParent, "Null overridingParent");
        if (overridingParent.isEmpty()) {
            return executorSettings;
            // - micro-optimization
        }
        if (extractSubSettings) {
            final String settingsName = name();
            final JsonObject subSettings = getSubSettingsByName(overridingParent, settingsName);
            if (subSettings != null) {
                final JsonObject onlyActual = Jsons.filterJson(subSettings, settingsKeySet());
                // - maybe this sub-settings contains a lot of other information for deeper levels
                final JsonObject overriddenBySubSettings = Jsons.overrideEntries(executorSettings, onlyActual);
                return overrideEntriesExceptingGivenSettings(overriddenBySubSettings, overridingParent, settingsName);
                // - this sub-settings subSettingsKey is already used, no sense to add it
            }
        }
        return Jsons.overrideEntries(executorSettings, overridingParent);
    }

    public static JsonObject getSubSettingsByName(JsonObject parentSettings, String subSettingsName) {
        Objects.requireNonNull(subSettingsName, "Null sub-settings name");
        return getSubSettingsByKey(parentSettings, ControlSpecification.settingsKey(subSettingsName));
    }

    public static JsonObject getSubSettingsByKey(JsonObject parentSettings, String subSettingsKey) {
        Objects.requireNonNull(parentSettings, "Null parent settings");
        Objects.requireNonNull(subSettingsKey, "Null sub-settings key");
        final JsonValue subSettings = parentSettings.get(subSettingsKey);
        if (subSettings == null) {
            return null;
        } else {
            if (!(subSettings instanceof JsonObject)) {
                throw new JsonException("Cannot extract sub-settings \"" + subSettingsKey
                        + "\" from the source JSON: "
                        + "this key does not correspond to sub-JSON"
                        + " - " + toShortString(Jsons.toPrettyString(parentSettings)));
            }
            return (JsonObject) subSettings;
        }
    }

    public static JsonObject overrideEntriesExceptingGivenSettings(
            JsonObject sourceJson,
            JsonObject overridingJson,
            String... ignoredSettingsNames) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        Objects.requireNonNull(overridingJson, "Null overridingJson");
        final Set<String> ignoredKeys = Arrays.stream(ignoredSettingsNames).map(
                ControlSpecification::settingsKey).collect(Collectors.toSet());
        final JsonObjectBuilder builder = Jsons.createObjectBuilder(sourceJson);
        for (Map.Entry<String, JsonValue> entry : overridingJson.entrySet()) {
            if (!ignoredKeys.contains(entry.getKey())) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public static String portName(ControlSpecification controlSpecification) {
        return controlSpecification.getName();
    }

    @Override
    public String toString() {
        return "settings (\"" + category() + "." + name() + "\")";
    }

    @Override
    public SettingsBuilder clone() {
        try {
            return (SettingsBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private JsonObject buildExecutor(Executor executor, boolean useExecutorParameters) {
        final Parameters parameters = useExecutorParameters ? executor.parameters() : null;
        final Map<String, Port> inputPortsMap = useExecutorParameters ? executor.inputPortsMap() : null;
        return buildParametersAndPorts(executor, parameters, inputPortsMap);
    }

    private JsonObject buildParametersAndPorts(
            Executor executor,
            Parameters parameters,
            Map<String, Port> inputPortsMap) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (addSettingsClass) {
            builder.add(SettingsSpecification.CLASS_KEY, specification.className());
        }
        for (ControlSpecification control : specification.getControls().values()) {
            JsonValue jsonValue = getJsonValue(control, parameters, inputPortsMap);
            assert jsonValue != null;
            if (executor != null) {
                jsonValue = replaceToAbsolutePath(executor, control, jsonValue);
            }
            final String jsonKey = control.key();
            builder.add(jsonKey, jsonValue);
        }
        return builder.build();
    }

    private static JsonValue getJsonValue(
            ControlSpecification control,
            Parameters parameters,
            Map<String, Port> inputPortsMap) {
        final String name = control.getName();
        final ParameterValueType valueType = control.getValueType();
        JsonValue jsonValue = null;
        if (inputPortsMap != null && valueType.isSettings()) {
            final Port inputPort = inputPortsMap.get(name);
            if (inputPort != null) {
                final SScalar scalar = inputPort.getData(SScalar.class, true);
                if (scalar.isInitialized()) {
                    final String s = scalar.getValueOrDefault("").trim();
                    jsonValue = s.isEmpty() ? Jsons.newEmptyJson() : Jsons.toJson(s);
                    return jsonValue;
                }
            }
        }
        if (parameters != null && parameters.containsKey(name)) {
            jsonValue = valueType.toJsonValue(parameters, name);
        }
        if (jsonValue == null) {
            jsonValue = control.getDefaultJsonValue();
        }
        if (jsonValue == null) {
            jsonValue = valueType.emptyJsonValue();
        }
        return jsonValue;
    }

    private static void setJsonValue(ControlSpecification control, Parameters parameters, JsonValue jsonValue) {
        final String name = control.getName();
        final ParameterValueType valueType = control.getValueType();
        Object parameterValue = valueType.toParameter(jsonValue);
        if (parameterValue == null) {
            // - if the parameter is a correctly written value, try STRING value
            parameterValue = ParameterValueType.STRING.toParameter(jsonValue);
        }
        if (parameterValue != null) {
            parameters.put(name, parameterValue);
        }
    }

    private JsonValue replaceToAbsolutePath(Executor executor, ControlSpecification control, JsonValue jsonValue) {
        assert jsonValue != null;
        if (control.getValueType() == ParameterValueType.STRING
                && control.getEditionType().isPath()
                && absolutePaths) {
            assert jsonValue instanceof JsonString : "Invalid " + control + ": did not check default value";
            String path = ((JsonString) jsonValue).getString().trim();
            if (!path.isEmpty()) {
                // - it is better to stay empty string unchanged: it is probably not a reference
                // to the current folder, but just a non-initialized parameter
                path = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(path, executor).toString();
            }
            jsonValue = Jsons.toJsonStringValue(path);
        }
        return jsonValue;
    }

    private static String toShortString(Object value) {
        String result = String.valueOf(value);
        return result.length() > 512 ? result.substring(0, 512) + "..." : result;
    }
}
