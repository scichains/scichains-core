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

package net.algart.executors.api.system;

import jakarta.json.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.chains.*;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.data.Port;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Detailed specification of an executor: ports, parameters, some key features of its behavior.</p>
 *
 * <p>Note: this class is not absolutely safe in relation of copying mutable data into and from this object.
 * Therefore, this object can become incorrect after creation, for example, by setting duplicated names
 * in several ports.</p>
 */
public class ExecutorSpecification extends AbstractConvertibleToJson {
    public static final String EXECUTOR_FILE_PATTERN = ".*\\.json$";
    public static final String APP_NAME = "executor";
    public static final String CURRENT_VERSION = "1.0";

    public static final String DYNAMIC_CATEGORY_PREFIX = "$";
    public static final String CATEGORY_PREFIX_DISABLING_DYNAMIC = "$no-prefix$";

    public static final String OUTPUT_EXECUTOR_ID_CAPTION = "Executor\u00A0ID";
    public static final String OUTPUT_EXECUTOR_ID_HINT = "ID of this executor";
    public static final String OUTPUT_PLATFORM_ID_CAPTION = "Platform\u00A0ID";
    public static final String OUTPUT_PLATFORM_ID_HINT =
            "ID of the platform, where this executor is installed";
    public static final String OUTPUT_RESOURCE_FOLDER_CAPTION = "Resource\u00A0folder";
    public static final String OUTPUT_RESOURCE_FOLDER_ID_HINT =
            "Resource folder (if exist) of the platform, where this executor is installed";

    private static final Pattern COMPILED_EXECUTOR_FILE_PATTERN = Pattern.compile(EXECUTOR_FILE_PATTERN);

    public static final class Options extends AbstractConvertibleToJson {
        public static final class Role extends AbstractConvertibleToJson {
            private String name = null;
            // - Some "name" of this role. For example, for settings combiners it can be a name of this setting type
            // (for main chain settings it is usually just the name of the chain, like Owner.name).
            private String resultPort = null;
            // - Port, containing the main results of this executor.
            // Not important for usual executors, but can be useful for special roles like "settings",
            // where the external client should know something about executor behavior.
            // In particular, if "settings" flag is true, this string should contain the name of
            // the output JSON/XML port, where the source parameters are combined.
            private boolean settings = false;
            private boolean main = false;
            // - Together with settings, means that the executor customizes the main settings of the chain.

            public Role() {
            }

            private Role(JsonObject json, Path file) {
                this.name = json.getString("name", null);
                this.resultPort = json.getString("result_port", null);
                this.settings = json.getBoolean("settings", false);
                this.main = json.getBoolean("main", false);
            }

            public String getName() {
                return name;
            }

            public Role setName(String name) {
                this.name = name;
                return this;
            }

            public String getResultPort() {
                return resultPort;
            }

            public Role setResultPort(String resultPort) {
                this.resultPort = resultPort;
                return this;
            }

            public boolean isSettings() {
                return settings;
            }

            public Role setSettings(boolean settings) {
                this.settings = settings;
                return this;
            }

            public boolean isMain() {
                return main;
            }

            public Role setMain(boolean main) {
                this.main = main;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Role{" +
                        "name='" + name + '\'' +
                        ", resultPort='" + resultPort + '\'' +
                        ", settings=" + settings +
                        ", main=" + main +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (name != null) {
                    builder.add("name", name);
                }
                if (resultPort != null) {
                    builder.add("result_port", resultPort);
                }
                builder.add("settings", settings);
                builder.add("main", main);
            }
        }

        public static final class Owner extends AbstractConvertibleToJson {
            private String category = null;
            private String name = null;
            private String id = null;
            private String contextId = null;

            public Owner() {
            }

            private Owner(JsonObject json, Path file) {
                this.category = json.getString("category", null);
                this.name = json.getString("name", null);
                this.id = json.getString("id", null);
                this.contextId = json.getString("context_id", null);
            }

            public String getCategory() {
                return category;
            }

            public Owner setCategory(String category) {
                this.category = category;
                return this;
            }

            public String getName() {
                return name;
            }

            public Owner setName(String name) {
                this.name = name;
                return this;
            }

            public String getId() {
                return id;
            }

            public Owner setId(String id) {
                this.id = id;
                return this;
            }

            public String getContextId() {
                return contextId;
            }

            public Owner setContextId(String contextId) {
                this.contextId = contextId;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Owner{" +
                        "category='" + category + '\'' +
                        ", name='" + name + '\'' +
                        ", id='" + id + '\'' +
                        ", contextId='" + contextId + '\'' +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (category != null) {
                    builder.add("category", category);
                }
                if (name != null) {
                    builder.add("name", name);
                }
                if (id != null) {
                    builder.add("id", id);
                }
                if (contextId != null) {
                    builder.add("context_id", contextId);
                }
            }
        }

        public static final class Service extends AbstractConvertibleToJson {
            private String settingsId = null;

            public Service() {
            }

            private Service(JsonObject json, Path file) {
                this.settingsId = json.getString("settings_id", null);
            }

            public String getSettingsId() {
                return settingsId;
            }

            public Service setSettingsId(String settingsId) {
                this.settingsId = settingsId;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Service{" +
                        "settingsId='" + settingsId + '\'' +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (settingsId != null) {
                    builder.add("settings_id", settingsId);
                }
            }
        }

        public static final class Controlling extends AbstractConvertibleToJson {
            private boolean grouping = false;
            private String groupSelector = null;

            public Controlling() {
            }

            private Controlling(JsonObject json, Path file) {
                this.grouping = json.getBoolean("grouping", false);
                this.groupSelector = json.getString("group_selector", null);
            }

            public boolean isGrouping() {
                return grouping;
            }

            public Controlling setGrouping(boolean grouping) {
                this.grouping = grouping;
                return this;
            }

            public String getGroupSelector() {
                return groupSelector;
            }

            public Controlling setGroupSelector(String groupSelector) {
                this.groupSelector = groupSelector;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Controlling{" +
                        "grouping=" + grouping +
                        ", groupSelector='" + groupSelector + '\'' +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("grouping", grouping);
                if (groupSelector != null) {
                    builder.add("group_selector", groupSelector);
                }
            }
        }

        public static final class Behavior extends AbstractConvertibleToJson {
            private boolean input = false;
            private boolean output = false;
            private boolean data = false;
            private boolean copy = false;
            private ParameterValueType dataType = null;
            private ControlEditionType editionType = ControlEditionType.VALUE;
            // - dataType and editionType are used in setTo(Chain chain) method
            // for choosing valueType and editionType of chain controls

            public Behavior() {
            }

            public Behavior(JsonObject json, Path file) {
                this.input = json.getBoolean("input", false);
                this.output = json.getBoolean("output", false);
                this.data = json.getBoolean("data", false);
                this.copy = json.getBoolean("copy", false);
                final String dataType = json.getString("data_type", null);
                if (dataType != null) {
                    this.dataType = ParameterValueType.valueOfTypeNameOrNull(dataType);
                    Jsons.requireNonNull(this.dataType, json, "data_type",
                            "unknown (\"" + dataType + "\")", file);
                }
                final String editionType = json.getString("edition_type", null);
                if (editionType != null) {
                    this.editionType = ControlEditionType.valueOfEditionTypeNameOrNull(editionType);
                    Jsons.requireNonNull(this.editionType, json, "edition_type",
                            "unknown (\"" + editionType + "\")", file);
                }
            }

            public boolean isInput() {
                return input;
            }

            public Behavior setInput(boolean input) {
                this.input = input;
                return this;
            }

            public boolean isOutput() {
                return output;
            }

            public Behavior setOutput(boolean output) {
                this.output = output;
                return this;
            }

            public boolean isData() {
                return data;
            }

            public Behavior setData(boolean data) {
                this.data = data;
                return this;
            }

            public boolean isCopy() {
                return copy;
            }

            public Behavior setCopy(boolean copy) {
                this.copy = copy;
                return this;
            }

            public ParameterValueType getDataType() {
                return dataType;
            }

            public Behavior setDataType(ParameterValueType dataType) {
                this.dataType = dataType;
                return this;
            }

            public ControlEditionType getEditionType() {
                return editionType;
            }

            public Behavior setEditionType(ControlEditionType editionType) {
                this.editionType = editionType;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Behavior{" +
                        "input=" + input +
                        ", output=" + output +
                        ", data=" + data +
                        ", copy=" + copy +
                        (dataType == null ? "" : ", dataType=" + dataType) +
                        (editionType == null ? "" : ", editionType=" + editionType) +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("input", input);
                builder.add("output", output);
                builder.add("data", data);
                builder.add("copy", copy);
                if (dataType != null) {
                    builder.add("data_type", dataType.typeName());
                }
                if (editionType != null) {
                    builder.add("edition_type", editionType.editionTypeName());
                }
            }
        }

        private ExecutionStage stage = ExecutionStage.RUN_TIME;
        private Role role = null;
        private Owner owner = null;
        // - usually specified for "main" blocks in some chain,
        // usually only for dynamic executors like "chain settings",
        // which are created as a serving companion for the chain
        private Service service = null;
        // - information about some service executors;
        // usually applied for sub-chains or multi-chains
        private Behavior behavior = null;
        private Controlling controlling = null;
        private JsonObject extension = null;

        public Options() {
        }

        public Options(JsonObject json, Path file) {
            final String stage = json.getString("stage", ExecutionStage.RUN_TIME.stageName());
            this.stage = ExecutionStage.valueOfStageNameOrNull(stage);
            Jsons.requireNonNull(this.stage, json, "stage", "unknown (\"" + stage + "\")", file);
            final JsonObject roleJson = json.getJsonObject("role");
            if (roleJson != null) {
                this.role = new Role(roleJson, file);
            }
            final JsonObject ownerJson = json.getJsonObject("owner");
            if (ownerJson != null) {
                this.owner = new Owner(ownerJson, file);
            }
            final JsonObject serviceJson = json.getJsonObject("service");
            if (serviceJson != null) {
                this.service = new Service(serviceJson, file);
            }
            final JsonObject behaviorJson = json.getJsonObject("behavior");
            if (behaviorJson != null) {
                this.behavior = new Behavior(behaviorJson, file);
            }
            final JsonObject controllingJson = json.getJsonObject("controlling");
            if (controllingJson != null) {
                this.controlling = new Controlling(controllingJson, file);
            }
            this.extension = json.getJsonObject("extension");
        }

        public ExecutionStage getStage() {
            return stage;
        }

        public Options setStage(ExecutionStage stage) {
            this.stage = Objects.requireNonNull(stage, "Null stage");
            return this;
        }

        public Role createRoleIfAbsent() {
            if (role == null) {
                role = new Role();
            }
            return role;
        }

        public Role getRole() {
            return role;
        }

        public Options setRole(Role role) {
            this.role = role;
            return this;
        }

        public Owner createOwnerIfAbsent() {
            if (owner == null) {
                owner = new Owner();
            }
            return owner;
        }

        public Owner getOwner() {
            return owner;
        }

        public Options setOwner(Owner owner) {
            this.owner = owner;
            return this;
        }

        public Service createServiceIfAbsent() {
            if (service == null) {
                service = new Service();
            }
            return service;
        }

        public Service getService() {
            return service;
        }

        public Options setService(Service service) {
            this.service = service;
            return this;
        }

        public Behavior createBehaviorIfAbsent() {
            if (behavior == null) {
                behavior = new Behavior();
            }
            return behavior;
        }

        public Behavior getBehavior() {
            return behavior;
        }

        public Options setBehavior(Behavior behavior) {
            this.behavior = behavior;
            return this;
        }

        public Controlling createControllingIfAbsent() {
            if (controlling == null) {
                controlling = new Controlling();
            }
            return controlling;
        }

        public Controlling getControlling() {
            return controlling;
        }

        public Options setControlling(Controlling controlling) {
            this.controlling = controlling;
            return this;
        }

        public JsonObject getExtension() {
            return extension;
        }

        public Options setExtension(JsonObject extension) {
            this.extension = extension;
            return this;
        }

        @Override
        public void checkCompleteness() {
        }

        @Override
        public String toString() {
            return "Options{" +
                    "stage=" + stage +
                    ", role=" + role +
                    ", owner=" + owner +
                    ", service=" + service +
                    ", behavior=" + behavior +
                    ", controlling=" + controlling +
                    ", extension=" + extension +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("stage", stage.stageName());
            if (role != null) {
                builder.add("role", role.toJson());
            }
            if (owner != null) {
                builder.add("owner", owner.toJson());
            }
            if (service != null) {
                builder.add("service", service.toJson());
            }
            if (behavior != null) {
                builder.add("behavior", behavior.toJson());
            }
            if (controlling != null) {
                builder.add("controlling", controlling.toJson());
            }
            if (extension != null) {
                builder.add("extension", extension);
            }
        }
    }

    public static final class SourceInfo extends AbstractConvertibleToJson {
        private String languageName = null;
        // - this language name is user-friendly, unlike root "language" field
        private String specificationPath = null;
        private String modulePath = null;

        public SourceInfo() {
        }

        private SourceInfo(JsonObject json, Path file) {
            this.languageName = json.getString("language_name", null);
            this.specificationPath = json.getString("specification_path", null);
            this.modulePath = json.getString("module_path", null);
        }

        public String getLanguageName() {
            return languageName;
        }

        public SourceInfo setLanguageName(String languageName) {
            this.languageName = languageName;
            return this;
        }

        public String getSpecificationPath() {
            return specificationPath;
        }

        public SourceInfo setSpecificationPath(String specificationPath) {
            this.specificationPath = specificationPath;
            return this;
        }

        public SourceInfo setAbsoluteSpecificationPath(Path specificationPath) {
            return setSpecificationPath(specificationPath == null ?
                    null :
                    specificationPath.toAbsolutePath().toString());
        }

        public String getModulePath() {
            return modulePath;
        }

        public SourceInfo setModulePath(String modulePath) {
            this.modulePath = modulePath;
            return this;
        }

        public SourceInfo setAbsoluteModulePath(Path modulePath) {
            return setModulePath(modulePath == null ? null : modulePath.toAbsolutePath().toString());
        }

        @Override
        public void checkCompleteness() {
        }

        @Override
        public String toString() {
            return "SourceInfo{" +
                    "languageName='" + languageName + '\'' +
                    ", specificationPath='" + specificationPath + '\'' +
                    ", modulePath='" + modulePath + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            if (languageName != null) {
                builder.add("language_name", languageName);
            }
            if (specificationPath != null) {
                builder.add("specification_path", specificationPath);
            }
            if (modulePath != null) {
                builder.add("module_path", modulePath);
            }
        }
    }

    public static final class JavaConf {
        // Does not extend AbstractConvertibleToJson, because must declare
        // toJson method instead of overriding buildJson
        public static final String JAVA_LANGUAGE = "java";
        public static final String JAVA_CONF_NAME = "java";
        public static final String CLASS_PROPERTY_NAME = "class";
        public static final String NEW_INSTANCE_METHOD_PROPERTY_NAME = "new_instance_method";

        private JsonObject json;
        // - Added to preserve original JSON (which can contain some additional fields)
        private Path file;
        private String className = null;
        private String newInstanceMethod = null;
        private Class<?> executorClass = null;

        public JavaConf() {
        }

        public JavaConf(JsonObject json, Path file) {
            this.file = file;
            setJson(json);
        }

        public JsonObject getJson() {
            return json;
        }

        public JavaConf setJson(JsonObject json) {
            this.json = Objects.requireNonNull(json, "Null json");
            this.className = Jsons.reqString(json, CLASS_PROPERTY_NAME, file);
            this.newInstanceMethod = json.getString(NEW_INSTANCE_METHOD_PROPERTY_NAME, null);
            this.file = null;
            return this;
        }

        public void setJson(String json) {
            setJson(Jsons.toJson(json));
        }

        public String getClassName() {
            return className;
        }

        /**
         * Returns the name of instantiation static method.
         *
         * <p>Note: usually new executors are created without calling this method,
         * but via parsing JSON with using {@link #NEW_INSTANCE_METHOD_PROPERTY_NAME} constant.</p>
         */
        public String getNewInstanceMethod() {
            return newInstanceMethod;
        }

        public Class<?> executorClass() {
            resolveSupportedExecutor();
            return executorClass;
        }

        public void resolveSupportedExecutor() {
            if (executorClass == null && className != null) {
                try {
                    this.executorClass = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new JsonException("Invalid JSON"
                            + (file == null ? "" : " " + file)
                            + ": execution block class " + className
                            + " not found <<<" + json + ">>>", e);
                }
            }
        }

        public void checkCompleteness() {
            AbstractConvertibleToJson.checkNull(json, "json", getClass());
        }

        public JsonObject toJson() {
            checkCompleteness();
            return json;
        }

        @Override
        public String toString() {
            return "<<<" + json + ">>>";
        }

        public static JsonObject standardJson(String className) {
            return Json.createObjectBuilder().add(CLASS_PROPERTY_NAME, className).build();
        }
    }

    public static final class PortConf extends AbstractConvertibleToJson {
        private String name;
        private DataType valueType;
        private String caption = null;
        private String hint = null;
        private boolean advanced = false;

        public PortConf() {
        }

        public PortConf(JsonObject json, Path file) {
            this.name = Jsons.reqString(json, "name", file);
            this.valueType = DataType.valueOfTypeNameOrNull(Jsons.reqString(json, "value_type", file));
            Jsons.requireNonNull(valueType, json, "value_type", file);
            this.caption = json.getString("caption", null);
            this.hint = json.getString("hint", null);
            this.advanced = json.getBoolean("advanced", false);
        }

        public String getName() {
            return name;
        }

        public PortConf setName(String name) {
            this.name = Objects.requireNonNull(name, "Null name");
            return this;
        }

        public DataType getValueType() {
            return valueType;
        }

        public PortConf setValueType(DataType valueType) {
            this.valueType = Objects.requireNonNull(valueType, "Null valueType");
            return this;
        }

        public String getCaption() {
            return caption;
        }

        public PortConf setCaption(String caption) {
            this.caption = caption;
            return this;
        }

        public String getHint() {
            return hint;
        }

        public PortConf setHint(String hint) {
            this.hint = hint;
            return this;
        }

        public boolean isAdvanced() {
            return advanced;
        }

        public PortConf setAdvanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public boolean isCompatible(PortConf other) {
            Objects.requireNonNull(other, "Null other");
            return other.valueType == valueType;
        }

        @Override
        public void checkCompleteness() {
            checkNull(name, "name");
            checkNull(valueType, "valueType");
        }

        @Override
        public String toString() {
            return "Port{" +
                    "name='" + name + '\'' +
                    ", valueType=" + valueType +
                    ", caption=" + caption +
                    ", hint=" + hint +
                    ", advanced=" + advanced +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("name", name);
            builder.add("value_type", valueType.typeName());
            if (caption != null) {
                builder.add("caption", caption);
            }
            if (hint != null) {
                builder.add("hint", hint);
            }
            if (advanced) {
                builder.add("advanced", advanced);
            }
        }
    }

    public static final class ControlConf extends AbstractConvertibleToJson implements Cloneable {
        public static final String SUPPESS_WARNING_NO_SETTER = "no_setter";

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
                this.value = Jsons.toJsonStringValue(value);
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
        private ParameterValueType valueType;
        private String valueClass = null;
        // - can be the name of some class of similar values; for example,
        // for value-type "settings" it may be the name of the settings specification
        private ControlEditionType editionType = ControlEditionType.VALUE;
        private String builderId = null;
        // - can be executor ID of some executor, that can create this parameter (usually value-type "settings"),
        // for example, may be ID of some settings combiner
        private String groupId = null;
        // - if controls are grouped into some logical groups, here may be ID of the group, containing this control
        private boolean multiline = false;
        private Integer lines = null;
        // - recommended number of lines in "multiline" mode
        private boolean resources = false;
        // - note: by default, it is true if editionType.isResources() is true
        private boolean advanced = false;
        private List<EnumItem> items = null;
        private List<String> suppressWarnings = null;
        private JsonValue defaultJsonValue = null;

        public ControlConf() {
        }

        public ControlConf(JsonObject json, Path file) {
            this.name = Jsons.reqString(json, "name", file);
            this.description = json.getString("description", null);
            this.caption = json.getString("caption", null);
            this.hint = json.getString("hint", null);
            String valueType = Jsons.reqString(json, "value_type", file);
            this.valueType = ParameterValueType.valueOfTypeNameOrNull(valueType);
            Jsons.requireNonNull(this.valueType, json, "value_type",
                    "unknown (\"" + valueType + "\")", file);
            this.valueClass = json.getString("value_class", null);
            String editionType = json.getString("edition_type", ControlEditionType.VALUE.editionTypeName());
            this.editionType = ControlEditionType.valueOfEditionTypeNameOrNull(editionType);
            Jsons.requireNonNull(this.editionType, json, "edition_type",
                    "unknown (\"" + editionType + "\")", file);
            this.builderId = json.getString("builder_id", null);
            this.groupId = json.getString("group_id", null);
            this.multiline = json.getBoolean("multiline", false);
            final JsonNumber lines = json.getJsonNumber("lines");
            this.lines = lines == null ? null : lines.intValue();
            if (this.lines != null && this.lines <= 0) {
                throw new IllegalArgumentException("Zero or negative lines = " + this.lines);
            }
            this.resources = json.getBoolean("resources", this.editionType.isResources());
            this.advanced = json.getBoolean("advanced", false);
            if (this.editionType == ControlEditionType.ENUM) {
                if (this.valueType == ParameterValueType.STRING) {
                    // for other value types, "enum" edition type does not affect
                    // the way of setting the value: Executor still has a setter
                    // like setXxx(int value)
                    this.valueType = ParameterValueType.ENUM_STRING;
                }
                // Note: we allow to skip "items" in this case, because
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
            try {
                setDefaultJsonValue(json.get("default"));
            } catch (IllegalArgumentException e) {
                throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                        + ": invalid control \"" + name + "\"", e);
            }
        }

        public String getName() {
            return name;
        }

        public ControlConf setName(String name) {
            this.name = Objects.requireNonNull(name, "Null name");
            return this;
        }

        public String getDescription() {
            return description;
        }

        public ControlConf setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getCaption() {
            return caption;
        }

        public ControlConf setCaption(String caption) {
            this.caption = caption;
            return this;
        }

        public String getHint() {
            return hint;
        }

        public ControlConf setHint(String hint) {
            this.hint = hint;
            return this;
        }

        public ParameterValueType getValueType() {
            assert valueType != null : "valueType cannot be null";
            return valueType;
        }

        public ControlConf setValueType(ParameterValueType valueType) {
            this.valueType = Objects.requireNonNull(valueType, "Null valueType");
            return this;
        }

        public String getValueClass() {
            return valueClass;
        }

        public ControlConf setValueClass(String valueClass) {
            this.valueClass = valueClass;
            return this;
        }

        public ControlEditionType getEditionType() {
            return editionType;
        }

        public ControlConf setEditionType(ControlEditionType editionType) {
            this.editionType = Objects.requireNonNull(editionType, "Null editionType");
            return this;
        }

        public String getBuilderId() {
            return builderId;
        }

        public ControlConf setBuilderId(String builderId) {
            this.builderId = builderId;
            return this;
        }

        public String getGroupId() {
            return groupId;
        }

        public ControlConf setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public boolean isMultiline() {
            return multiline;
        }

        public ControlConf setMultiline(boolean multiline) {
            this.multiline = multiline;
            return this;
        }

        public Integer getLines() {
            return lines;
        }

        public ControlConf setLines(Integer lines) {
            if (lines != null && lines <= 0) {
                throw new IllegalArgumentException("Zero or negative lines = " + lines);
            }
            this.lines = lines;
            return this;
        }

        public boolean isResources() {
            return resources;
        }

        public ControlConf setResources(boolean resources) {
            this.resources = resources;
            return this;
        }

        public boolean isAdvanced() {
            return advanced;
        }

        public ControlConf setAdvanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public List<EnumItem> getItems() {
            return items == null ? null : Collections.unmodifiableList(items);
        }

        public ControlConf setItems(List<EnumItem> items) {
            this.items = items == null ? null : new ArrayList<>(items);
            return this;
        }

        public List<String> getSuppressWarnings() {
            return suppressWarnings == null ? null : Collections.unmodifiableList(suppressWarnings);
        }

        public ControlConf setSuppressWarnings(List<String> suppressWarnings) {
            this.suppressWarnings = suppressWarnings == null ? null : new ArrayList<>(suppressWarnings);
            return this;
        }

        public JsonValue getDefaultJsonValue() {
            return defaultJsonValue;
        }

        public Object getDefaultValue() {
            return getValueType().toParameter(this.defaultJsonValue);
        }

        public ControlConf setDefaultJsonValue(JsonValue defaultJsonValue) {
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

        public ControlConf setDefaultStringValue(String defaultStringValue) {
            if (defaultStringValue == null) {
                this.defaultJsonValue = null;
            } else {
                this.defaultJsonValue = Jsons.toJsonStringValue(defaultStringValue);
            }
            return this;
        }

        public ControlConf setItemsFromLists(List<String> itemValues, List<String> itemCaptions) {
            Objects.requireNonNull(itemValues, "Null itemValues");
            final int itemCaptionsSize = itemCaptions == null ? 0 : itemCaptions.size();
            this.items = new ArrayList<>();
            for (int i = 0, n = itemValues.size(); i < n; i++) {
                String enumItem = itemValues.get(i);
                EnumItem item = new EnumItem(enumItem);
                if (i < itemCaptionsSize) {
                    assert itemCaptions != null : i + " cannot be negative";
                    item.setCaption(itemCaptions.get(i));
                }
                this.items.add(item);
            }
            if (defaultJsonValue == null && !this.items.isEmpty()) {
                // - usually enumItemNames cannot be empty; it is checked in the constructor of Mapping class
                setDefaultJsonValue(items.get(0).value);
            }
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(name, "name");
            checkNull(valueType, "valueType");
            assert editionType != null;
        }

        @Override
        public String toString() {
            return "ControlConf{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", caption='" + caption + '\'' +
                    ", hint='" + hint + '\'' +
                    ", valueType=" + valueType +
                    ", valueClass='" + valueClass + '\'' +
                    ", editionType=" + editionType +
                    ", builderId='" + builderId + '\'' +
                    ", groupId='" + groupId + '\'' +
                    ", multiline=" + multiline +
                    ", lines=" + lines +
                    ", resources=" + resources +
                    ", advanced=" + advanced +
                    ", items=" + items +
                    ", suppressWarnings=" + suppressWarnings +
                    ", defaultJsonValue=" + defaultJsonValue +
                    '}';
        }

        @Override
        public ControlConf clone() {
            try {
                final ControlConf result = (ControlConf) super.clone();
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
            if (valueClass != null) {
                builder.add("value_class", valueClass);
            }
            builder.add("edition_type", editionType.editionTypeName());
            if (builderId != null) {
                builder.add("builder_id", builderId);
            }
            if (groupId != null) {
                builder.add("group_id", groupId);
            }
            if (multiline) {
                builder.add("multiline", multiline);
            }
            if (lines != null) {
                builder.add("lines", lines);
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
    }

    private Path executorSpecificationFile = null;
    private String version = CURRENT_VERSION;
    private String platformId = null;
    // - usually not loaded from JSON file, but set later, while loading all JSON specification for some platform
    private String category;
    private String name;
    private String description = null;
    private Set<String> tags = new LinkedHashSet<>();
    private String executorId;
    private Options options = null;
    private String language = null;
    private JavaConf java = null;
    private Map<String, PortConf> inPorts = new LinkedHashMap<>();
    private Map<String, PortConf> outPorts = new LinkedHashMap<>();
    private Map<String, ControlConf> controls = new LinkedHashMap<>();
    private SourceInfo sourceInfo = null;
    // - note: "sourceInfo" field is not usually loaded from FILE, it should be defined by external means
    // (but it can be loaded from JSON STRING while program interactions)
    private boolean javaExecutor = false;
    private boolean chainExecutor = false;

    private volatile String minimalSpecification = null;

    public ExecutorSpecification() {
    }

    protected ExecutorSpecification(JsonObject json, Path file) {
        if (!isExecutorSpecification(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not an executor configuration: no \"app\":\"" + APP_NAME + "\" element");
        }
        this.executorSpecificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        this.platformId = json.getString("platform_id", null);
        try {
            this.executorId = Jsons.reqStringWithAlias(json, "id", "uuid", file);
            this.category = Jsons.reqString(json, "category", file);
            this.name = Jsons.reqString(json, "name", file);
            this.description = json.getString("description", null);
            JsonArray tags = Jsons.getJsonArray(json, "tags", file);
            if (tags != null) {
                for (JsonValue jsonValue : tags) {
                    if (!(jsonValue instanceof JsonString)) {
                        throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                                + ": \"tags\" array contains non-string element " + jsonValue);
                    }
                    this.tags.add(((JsonString) jsonValue).getString());
                }
            }
            final JsonObject optionsJson = json.getJsonObject("options");
            if (optionsJson != null) {
                this.options = new Options(optionsJson, file);
            }
            this.setLanguage(json.getString("language", null));
            final JsonObject javaJson = json.getJsonObject(JavaConf.JAVA_CONF_NAME);
            if (javaExecutor && javaJson == null) {
                throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                        + ": \"" + JavaConf.JAVA_CONF_NAME + "\" section required when \"language\" is \"java\"");
            }
            this.java = javaJson == null ? null : new JavaConf(javaJson, file);
            if (json.containsKey("in_ports")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "in_ports", file)) {
                    final PortConf port = new PortConf(jsonObject, file);
                    putOrException(inPorts, port.name, port, file, "in_ports");
                }
            }
            if (json.containsKey("out_ports")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "out_ports", file)) {
                    final PortConf port = new PortConf(jsonObject, file);
                    putOrException(outPorts, port.name, port, file, "out_ports");
                }
            }
            if (json.containsKey("controls")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "controls", file)) {
                    final ControlConf control = new ControlConf(jsonObject, file);
                    putOrException(controls, control.name, control, file, "controls");
                }
            }
            final JsonObject sourceJson = json.getJsonObject("source");
            if (sourceJson != null) {
                this.sourceInfo = new SourceInfo(sourceJson, file);
            }
        } catch (JsonException e) {
            if (file != null || executorId == null) {
                throw e;
                // - file name is enough information to find a mistake
            }
            throw new JsonException("Problem in JSON specification for executor with ID '" + executorId + "\'"
                    + (name == null ? "" : ", name \'" + name + "\'")
                    + (description == null ? "" : ", description \'" + name + "\'"), e);
        }
    }

    public static ExecutorSpecification read(Path executorSpecificationFile) throws IOException {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        final JsonObject json = Jsons.readJson(executorSpecificationFile);
        return new ExecutorSpecification(json, executorSpecificationFile);
    }

    public static ExecutorSpecification readIfValid(Path executorSpecificationFile) throws IOException {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        final JsonObject json = Jsons.readJson(executorSpecificationFile);
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new ExecutorSpecification(json, executorSpecificationFile);
    }

    public void write(Path executorSpecificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        Files.writeString(executorSpecificationFile, jsonString(), options);
    }

    public static ExecutorSpecification valueOf(JsonObject executorSpecification) {
        return new ExecutorSpecification(executorSpecification, null);
    }

    public static ExecutorSpecification valueOf(String executorSpecificationString) throws JsonException {
        Objects.requireNonNull(executorSpecificationString, "Null executorSpecificationString");
        final JsonObject executorSpecification = Jsons.toJson(executorSpecificationString);
        return new ExecutorSpecification(executorSpecification, null);
    }

    public static ExecutorSpecification valueOf(Executor executor, String executorId) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(executorId, "Null executor ID");
        final ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(executor);
        result.setExecutorId(executorId);
        return result;
    }

    public static boolean isExecutorSpecificationFile(Path file) {
        Objects.requireNonNull(file, "Null file");
        return COMPILED_EXECUTOR_FILE_PATTERN.matcher(file.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isExecutorSpecification(JsonObject executorSpecification) {
        Objects.requireNonNull(executorSpecification, "Null executor specification");
        return APP_NAME.equals(executorSpecification.getString("app", null));
    }

    public static void checkIdDifference(Collection<? extends ExecutorSpecification> executorSpecifications) {
        Objects.requireNonNull(executorSpecifications, "Null executor specifications collection");
        final Set<String> ids = new HashSet<>();
        for (ExecutorSpecification executorSpecification : executorSpecifications) {
            final String id = executorSpecification.getExecutorId();
            assert id != null;
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two executor JSONs have identical IDs " + id
                        + ", one of them is \"" + executorSpecification.getName() + "\"");
            }
        }
    }

    public final boolean hasExecutorSpecificationFile() {
        return executorSpecificationFile != null;
    }

    public final Path getExecutorSpecificationFile() {
        return executorSpecificationFile;
    }

    public final String getVersion() {
        return version;
    }

    public final ExecutorSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public final boolean hasPlatformId() {
        return platformId != null;
    }

    public final String getPlatformId() {
        return platformId;
    }

    public final ExecutorSpecification setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public final String getCategory() {
        return category;
    }

    public final ExecutorSpecification setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Null category");
        return this;
    }

    public final String getName() {
        return name;
    }

    public final ExecutorSpecification setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        return this;
    }

    public final String getCanonicalName() {
        return category + "." + name;
    }

    public final String getDescription() {
        return description;
    }

    public final ExecutorSpecification setDescription(String description) {
        this.description = description;
        return this;
    }

    public final Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public final ExecutorSpecification setTags(Set<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags = new LinkedHashSet<>(tags);
        return this;
    }

    public final String getExecutorId() {
        return executorId;
    }

    public final ExecutorSpecification setExecutorId(String executorId) {
        this.executorId = Objects.requireNonNull(executorId, "Null executor ID");
        return this;
    }

    public final Options createOptionsIfAbsent() {
        if (options == null) {
            options = new Options();
        }
        return options;
    }

    public final Options getOptions() {
        return options;
    }

    public final ExecutorSpecification setOptions(Options options) {
        this.options = options;
        return this;
    }

    public final String getLanguage() {
        return language;
    }

    public final ExecutorSpecification setLanguage(String language) {
        this.language = language;
        this.javaExecutor = JavaConf.JAVA_LANGUAGE.equals(language);
        this.chainExecutor = ChainSpecification.CHAIN_LANGUAGE.equals(language);
        return this;
    }

    /**
     * Returns <code>true</code> if the current {@link #getLanguage() language} is
     * {@value JavaConf#JAVA_LANGUAGE}.
     *
     * @return whether this executor is Java-based.
     */
    public final boolean isJavaExecutor() {
        return javaExecutor;
    }

    public boolean isChainExecutor() {
        return chainExecutor;
    }

    /**
     * Returns configuration of the current Java executor or <code>null</code>
     * if it is not a Java executor.
     * <p>The result of this function is never <code>null</code>, if this specification was constructed from JSON
     * and if {@link #isExecutorSpecification(JsonObject)} returns <code>true</code>. But this can be not so
     * if this object was constructed manually via setter methods.
     *
     * @return configuration of Java executor.
     */
    public final JavaConf getJava() {
        return java;
    }

    public final ExecutorSpecification setJava(JavaConf java) {
        this.java = java;
        return this;
    }

    public final PortConf getInPort(String name) {
        return inPorts.get(name);
    }

    public final Map<String, PortConf> getInPorts() {
        return Collections.unmodifiableMap(inPorts);
    }

    public final ExecutorSpecification setInPorts(Map<String, PortConf> inPorts) {
        this.inPorts = checkInPorts(inPorts);
        return this;
    }

    public final PortConf getOutPort(String name) {
        return outPorts.get(name);
    }

    public final Map<String, PortConf> getOutPorts() {
        return Collections.unmodifiableMap(outPorts);
    }

    public final ExecutorSpecification setOutPorts(Map<String, PortConf> outPorts) {
        this.outPorts = checkOutPorts(outPorts);
        return this;
    }

    public final ControlConf getControl(String name) {
        return controls.get(name);
    }

    public final Map<String, ControlConf> getControls() {
        return Collections.unmodifiableMap(controls);
    }

    public final ExecutorSpecification setControls(Map<String, ControlConf> controls) {
        this.controls = checkControls(controls);
        return this;
    }

    public final SourceInfo getSourceInfo() {
        return sourceInfo;
    }

    public final ExecutorSpecification setSourceInfo(SourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
        return this;
    }

    /**
     * Sets the source files information for this executor specification.
     *
     * <p>Usually it is set by {@link #setSourceInfoForSpecification()} method, which uses JSON file, passed
     * to  {@link #read(Path)} or {@link #readIfValid(Path)} methods, as a specification file, and does not try
     * to set module source file (if that method is not overridden).
     * However, it can be useful to set some of these two paths manually even in a case, when this object
     * is built on the base some other source, for example, a chain ({@link #setTo(Chain)} method).
     * For example, it can be used to allow GUI to open the source file of the given executor.</p>
     *
     * @param specificationPath path to some resource, defining this specification of the executor,
     *                          usually a disk path (but also can be, for example, URL or something else).
     * @param modulePath        path to some source text of the module (if applicable and available, for example,
     *                          JavaScript or chain file), usually a disk path
     *                          (but also can be, for example, URL or something else).
     * @return a reference to {@link SourceInfo} field inside this object (for possible additional corrections).
     */
    public final SourceInfo setSourceInfo(Path specificationPath, Path modulePath) {
        this.sourceInfo = new SourceInfo();
        this.sourceInfo.setAbsoluteSpecificationPath(specificationPath);
        this.sourceInfo.setAbsoluteModulePath(modulePath);
        return sourceInfo;
    }

    public final SourceInfo setSourceInfoForSpecification() {
        return setSourceInfo(executorSpecificationFile, null);
    }

    public final void updateCategoryPrefix(String categoryPrefix) {
        this.category = updateCategoryPrefix(this.category, categoryPrefix);
    }

    public final void addTags(Collection<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags.addAll(tags);
    }

    public final void addInPort(PortConf port) {
        Objects.requireNonNull(port, "Null input port");
        port.checkCompleteness();
        inPorts.put(port.name, port);
    }

    public final void addFirstInPort(PortConf port) {
        Objects.requireNonNull(port, "Null input port");
        port.checkCompleteness();
        final Map<String, PortConf> inPorts = new LinkedHashMap<>();
        inPorts.put(port.name, port);
        inPorts.putAll(this.inPorts);
        this.inPorts = inPorts;
    }

    public final void addOutPort(PortConf port) {
        Objects.requireNonNull(port, "Null output port");
        port.checkCompleteness();
        outPorts.put(port.name, port);
    }

    public final void addFirstOutPort(PortConf port) {
        Objects.requireNonNull(port, "Null output port");
        port.checkCompleteness();
        final Map<String, PortConf> outPorts = new LinkedHashMap<>();
        outPorts.put(port.name, port);
        outPorts.putAll(this.outPorts);
        this.outPorts = outPorts;
    }

    public final void addSystemExecutorIdPort() {
        if (!outPorts.containsKey(Executor.OUTPUT_EXECUTOR_ID_NAME)) {
            addOutPort(new PortConf()
                    .setName(Executor.OUTPUT_EXECUTOR_ID_NAME)
                    .setCaption(OUTPUT_EXECUTOR_ID_CAPTION)
                    .setHint(OUTPUT_EXECUTOR_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addSystemPlatformIdPort() {
        if (!outPorts.containsKey(Executor.OUTPUT_PLATFORM_ID_NAME)) {
            addOutPort(new PortConf()
                    .setName(Executor.OUTPUT_PLATFORM_ID_NAME)
                    .setCaption(OUTPUT_PLATFORM_ID_CAPTION)
                    .setHint(OUTPUT_PLATFORM_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addSystemResourceFolderPort() {
        if (!outPorts.containsKey(Executor.OUTPUT_RESOURCE_FOLDER_NAME)) {
            addOutPort(new PortConf()
                    .setName(Executor.OUTPUT_RESOURCE_FOLDER_NAME)
                    .setCaption(OUTPUT_RESOURCE_FOLDER_CAPTION)
                    .setHint(OUTPUT_RESOURCE_FOLDER_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addControl(ControlConf control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        controls.put(control.name, control);
    }

    public final void addFirstControl(ControlConf control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        final Map<String, ControlConf> controls = new LinkedHashMap<>();
        controls.put(control.name, control);
        controls.putAll(this.controls);
        this.controls = controls;
    }

    public final boolean isInput() {
        return options != null && options.behavior != null && options.behavior.input;
    }

    public final boolean isOutput() {
        return options != null && options.behavior != null && options.behavior.output;
    }

    public final boolean isData() {
        return options != null && options.behavior != null && options.behavior.data
                && !(options.behavior.input || options.behavior.output);
    }

    public final boolean isCopy() {
        return options != null && options.behavior != null && options.behavior.copy;
    }

    public final ParameterValueType dataType() {
        return isData() ? options.behavior.dataType : null;
    }

    public final ControlEditionType editionType() {
        return isData() ? options.behavior.editionType : null;
    }

    public final String minimalSpecification() {
        String minimalSpecification = this.minimalSpecification;
        if (minimalSpecification == null) {
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("app", APP_NAME);
            builder.add("category", category);
            builder.add("name", name);
            builder.add("id", executorId);
            if (java != null) {
                builder.add(JavaConf.JAVA_CONF_NAME, java.toJson());
            }
            if (platformId != null) {
                builder.add("platform_id", platformId);
            }
            this.minimalSpecification = minimalSpecification = builder.build().toString();
        }
        return minimalSpecification;
    }

    /**
     * Fills this object to describe an existing executor.
     * This method:
     * <ul>
     * <li>does not set id, language and path to source;</li>
     * <li>makes name and category from executor class name (if not specified).</li>
     * </ul>
     *
     * @param executor some existing executor.
     */
    public final void setTo(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final String className = executor.getClass().getName();
        final int lastDotIndex = className.lastIndexOf(".");
        if (this.category == null) {
            this.setCategory(lastDotIndex == -1 ? "" : className.substring(0, lastDotIndex));
        }
        if (this.name == null) {
            this.setName(executor.getClass().getSimpleName());
        }
        if (this.java == null) {
            this.setJava(new JavaConf().setJson(JavaConf.standardJson(className)));
        }
        final Map<String, PortConf> inPorts = new LinkedHashMap<>(this.inPorts);
        for (Port port : executor.allInputPorts()) {
            final String name = port.getName();
            final PortConf portConf = inPorts.getOrDefault(name, new PortConf());
            inPorts.put(name, portConf.setName(name).setValueType(port.getDataType()));
        }
        this.setInPorts(inPorts);
        final Map<String, PortConf> outPorts = new LinkedHashMap<>(this.outPorts);
        for (Port port : executor.allOutputPorts()) {
            final String name = port.getName();
            final PortConf portConf = outPorts.getOrDefault(name, new PortConf());
            outPorts.put(name, portConf.setName(name).setValueType(port.getDataType()));
        }
        this.setOutPorts(outPorts);
        final Map<String, ControlConf> controls = new LinkedHashMap<>(this.controls);
        for (String name : executor.allParameters()) {
            final ControlConf controlConf = controls.getOrDefault(name, new ControlConf());
            final ParameterValueType parameterValueType = executor.parameterControlValueType(name);
            controlConf.setName(name).setValueType(parameterValueType);
            if (parameterValueType == ParameterValueType.ENUM_STRING) {
                controlConf.setEditionType(ControlEditionType.ENUM);
                final Class<?> enumClass = executor.parameterJavaType(name);
                if (enumClass == null || !Enum.class.isAssignableFrom(enumClass)) {
                    throw new AssertionError("Invalid propertyJavaType method result: not enum ("
                            + enumClass + ")");
                }
                if (controlConf.items == null) {
                    String firstEnumName = null;
                    final List<ControlConf.EnumItem> items = new ArrayList<>();
                    for (Enum<?> enumConstant : enumClass.asSubclass(Enum.class).getEnumConstants()) {
                        final String enumName = enumConstant.name();
                        if (firstEnumName == null) {
                            firstEnumName = enumName;
                        }
                        items.add(new ControlConf.EnumItem().setValue(enumName));
                    }
                    if (firstEnumName == null) {
                        throw new AssertionError("No constants in enum class: impossible in Java");
                    }
                    controlConf.setItems(items);
                    controlConf.setDefaultStringValue(firstEnumName);
                }
            } else {
                controlConf.setEditionType(ControlEditionType.VALUE);
            }
            controls.put(name, controlConf);
        }
        this.setControls(controls);
    }

    /**
     * Fills this object to describe a chain as an executor.
     * This method:
     * <ul>
     * <li>sets id to be equal <code>chain.getId()</code>;</li>
     * <li>sets language to {@link ChainSpecification#CHAIN_LANGUAGE};</li>
     * <li>does not set the name, category and path to source;</li>
     * <li>does not set "java" section.</li>
     * </ul>
     *
     * @param chain some chain.
     */
    public final void setTo(Chain chain) {
        Objects.requireNonNull(chain, "Null chain");
        this.setCategory(chain.category());
        this.setName(chain.name());
        this.setDescription(chain.description());
        this.setTags(chain.tags());
        this.setExecutorId(chain.id());
        this.setLanguage(ChainSpecification.CHAIN_LANGUAGE);
        final Map<String, PortConf> inPorts = new LinkedHashMap<>(this.inPorts);
        for (ChainBlock block : chain.getAllInputs()) {
            final ChainInputPort inputPort = block.getActualInputPort(Executor.DEFAULT_INPUT_PORT);
            if (inputPort == null) {
                throw new IllegalArgumentException("Chain contains standard input block "
                        + "without default input port \"" + Executor.DEFAULT_INPUT_PORT + "\": " + block);
            }
            final String inputName = block.getStandardInputOutputName();
            if (inputName == null) {
                throw new IllegalArgumentException("Chain contains standard input block "
                        + "with non-initialized input name: " + block);
            }
            final PortConf portConf = new PortConf();
            portConf.setName(inputName);
            portConf.setValueType(inputPort.getDataType());
            setAdditionalFields(portConf, block);
            inPorts.put(portConf.getName(), portConf);

        }
        this.setInPorts(inPorts);
        final Map<String, PortConf> outPorts = new LinkedHashMap<>(this.outPorts);
        for (ChainBlock block : chain.getAllOutputs()) {
            final ChainOutputPort outputPort = block.getActualOutputPort(Executor.DEFAULT_OUTPUT_PORT);
            if (outputPort == null) {
                throw new IllegalArgumentException("Chain contains standard output block "
                        + "without default output port \"" + Executor.DEFAULT_OUTPUT_PORT + "\": " + block);
            }
            final String outputName = block.getStandardInputOutputName();
            if (outputName == null) {
                throw new IllegalArgumentException("Chain contains standard output block "
                        + "with non-initialized output name: " + block);
            }
            final PortConf portConf = new PortConf();
            portConf.setName(outputName);
            portConf.setValueType(outputPort.getDataType());
            setAdditionalFields(portConf, block);
            outPorts.put(portConf.getName(), portConf);
        }
        this.setOutPorts(outPorts);
        final Map<String, ControlConf> controls = new LinkedHashMap<>(this.controls);
        for (ChainBlock block : chain.getAllData()) {
            // - data blocks (with options.behavior.data = true) are used as parameters of the sub-chain executor
            final String parameterName = block.getStandardParameterName();
            if (parameterName == null) {
                continue;
            }
            final ChainInputPort inputPort = block.getActualInputPort(Executor.DEFAULT_INPUT_PORT);
            if (inputPort == null) {
                throw new IllegalArgumentException("Chain contains standard data block "
                        + "without default input port \"" + Executor.DEFAULT_INPUT_PORT + "\": " + block);
            }
            if (inputPort.getDataType() == DataType.SCALAR) {
                // - other types of data block, like numbers, cannot be used for specifying chain parameters
                // (though can be used as internal chain constants)
                final ControlConf controlConf = controls.getOrDefault(parameterName, new ControlConf());
                controlConf.setName(parameterName);
                final ExecutorSpecification specification = block.getExecutorSpecification();
                ParameterValueType valueType = specification != null ? specification.dataType() : null;
                ControlEditionType editionType = specification != null ? specification.editionType() : null;
                if (valueType == null) {
                    valueType = ParameterValueType.STRING;
                }
                controlConf.setValueType(valueType);
                controlConf.setEditionType(editionType != null ? editionType : ControlEditionType.VALUE);
                String defaultStringValue = null;
                final ChainOutputPort outputPort = block.getActualOutputPort(Executor.DEFAULT_OUTPUT_PORT);
                if (chain.getExecutorFactory() != null
                        && outputPort != null && outputPort.getDataType() == DataType.SCALAR) {
                    block.reinitialize(false);
                    final var executor = block.getExecutor();
                    // - If we have an executor, and we have a simple standard output scalar port,
                    // we will try to execute it to get the default value.
                    // It should not be a problem, because data block usually corresponds to
                    // a very simple executor, that just copy their arguments into the scalar port.
                    // Note: getExecutor will throw an exception if setStandardData was not called properly
                    // (usually it should not be set for disabled blocks and LOADING_TIME-blocks)
                    block.copyInputPortsToExecutor();
                    executor.execute();
                    defaultStringValue = executor.getScalar(Executor.DEFAULT_OUTPUT_PORT).getValue();
                }
                if (defaultStringValue != null) {
                    controlConf.setDefaultJsonValue(valueType.toJsonValue(defaultStringValue));
                }
                setAdditionalFields(controlConf, block);
                controls.put(controlConf.getName(), controlConf);
            }
        }
        this.setControls(controls);
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(executorId, "id");
        if (javaExecutor) {
            checkNull(java, JavaConf.JAVA_CONF_NAME);
        }
    }

    @Override
    public String toString() {
        return "ExecutorSpecification{\n" +
                "  executorSpecificationFile=" + executorSpecificationFile +
                ",\n  version='" + version + '\'' +
                ",\n  platformId='" + platformId + '\'' +
                ",\n  category='" + category + '\'' +
                ",\n  name='" + name + '\'' +
                ",\n  description=" + (description == null ? null : "'" + description + "'") +
                ",\n  tags=" + tags +
                ",\n  id='" + executorId + '\'' +
                ",\n  options=" + options +
                ",\n  language=" + language +
                ",\n  javaConf=" + java +
                ",\n  inPorts=" + inPorts +
                ",\n  outPorts=" + outPorts +
                ",\n  controls=" + controls +
                ",\n  javaExecutor=" + javaExecutor +
                ",\n  chainExecutor=" + chainExecutor +
                ",\n  sourceInfo=" + sourceInfo +
                "\n}\n";
    }

    public static String updateCategoryPrefix(String category, String categoryPrefix) {
        Objects.requireNonNull(category, "Null category");
        return categoryPrefix != null ? categoryPrefix + "." + category : category;
    }

    public static <K, V> void putOrException(Map<K, V> map, K key, V value, Path file, String mapName) {
        if (map.put(key, value) != null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": duplicate key \"" + key + "\" in \"" + mapName + "\" array");
        }
    }

    public static Map<String, PortConf> checkInPorts(Map<String, PortConf> ports) {
        return checkPorts(ports, "input");
    }

    public static Map<String, PortConf> checkOutPorts(Map<String, PortConf> ports) {
        return checkPorts(ports, "output");
    }

    public static Map<String, ControlConf> checkControls(Map<String, ControlConf> controls) {
        Objects.requireNonNull(controls, "Null controls");
        controls = new LinkedHashMap<>(controls);
        for (Map.Entry<String, ControlConf> control : controls.entrySet()) {
            if (control.getKey() == null) {
                throw new IllegalArgumentException("Illegal control: null key");
            }
            if (control.getValue() == null) {
                throw new IllegalArgumentException("Illegal control[" + quote(control.getKey()) + "]: null");
            }
            if (!control.getKey().equals(control.getValue().name)) {
                throw new IllegalArgumentException("Illegal control[" + quote(control.getKey())
                        + "]: its name is " + quote(control.getValue().name)
                        + " (must be equal to key " + quote(control.getKey()) + ")");
            }
        }
        return controls;
    }

    public static <T> List<T> checkNonNullObjects(List<T> objects) {
        if (objects == null) {
            return null;
        }
        objects = new ArrayList<>(objects);
        for (int k = 0, n = objects.size(); k < n; k++) {
            Objects.requireNonNull(objects.get(k), "Null element #" + k + " in list \"" + objects + "\"");
        }
        return objects;
    }

    public static String correctDynamicCategory(String category) {
        return correctDynamicCategory(category, false);
    }

    public static String correctDynamicCategory(String category, boolean disableDynamicPrefix) {
        if (category == null) {
            return null;
        }
        if (category.startsWith(CATEGORY_PREFIX_DISABLING_DYNAMIC)) {
            final int prefixLength = CATEGORY_PREFIX_DISABLING_DYNAMIC.length();
            if (category.length() > prefixLength) {
                return category.substring(prefixLength);
            }
            return category;
            // - improbable case, and we prefer not to throw an exception
        }
        if (disableDynamicPrefix) {
            return category;
        }
        return DYNAMIC_CATEGORY_PREFIX + category;
    }

    public static String quote(String value) {
        return value == null ? null : "\"" + value + "\"";
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", APP_NAME);
        builder.add("version", version);
        if (platformId != null) {
            builder.add("platform_id", platformId);
        }
        builder.add("category", category);
        builder.add("name", name);
        if (description != null) {
            builder.add("description", description);
        }
        if (!tags.isEmpty()) {
            final JsonArrayBuilder tagsBuilder = Json.createArrayBuilder();
            for (String tag : tags) {
                tagsBuilder.add(tag);
            }
            builder.add("tags", tagsBuilder.build());
        }
        builder.add("id", executorId);
        if (options != null) {
            builder.add("options", options.toJson());
        }
        if (language != null) {
            builder.add("language", language);
        }
        buildLanguageJson(builder);
        final JsonArrayBuilder inPortsBuilder = Json.createArrayBuilder();
        for (PortConf port : inPorts.values()) {
            inPortsBuilder.add(port.toJson());
        }
        builder.add("in_ports", inPortsBuilder.build());
        final JsonArrayBuilder outPortsBuilder = Json.createArrayBuilder();
        for (PortConf port : outPorts.values()) {
            outPortsBuilder.add(port.toJson());
        }
        builder.add("out_ports", outPortsBuilder.build());
        final JsonArrayBuilder controlsBuilder = Json.createArrayBuilder();
        for (ControlConf control : controls.values()) {
            controlsBuilder.add(control.toJson());
        }
        builder.add("controls", controlsBuilder.build());
        if (sourceInfo != null) {
            builder.add("source", sourceInfo.toJson());
        }
    }

    protected void buildLanguageJson(JsonObjectBuilder builder) {
        if (java != null) {
            builder.add(JavaConf.JAVA_CONF_NAME, java.toJson());
        }
    }

    private static void setAdditionalFields(ControlConf controlConf, ChainBlock block) {
        final ChainSpecification.ChainBlockConf blockConf = block.getBlockConfJson();
        if (blockConf != null) {
            final ChainSpecification.ChainBlockConf.SystemConf systemConf = blockConf.getSystem();
            controlConf.setCaption(makeCaption(block, systemConf.getCaption()));
            controlConf.setDescription(systemConf.getDescription());
        }
    }

    private static void setAdditionalFields(PortConf portConf, ChainBlock block) {
        final ChainSpecification.ChainBlockConf blockConf = block.getBlockConfJson();
        if (blockConf != null) {
            final ChainSpecification.ChainBlockConf.SystemConf systemConf = blockConf.getSystem();
            portConf.setCaption(makeCaption(block, systemConf.getCaption()));
            portConf.setHint(systemConf.getDescription());
        }
    }

    private static String makeCaption(ChainBlock block, String customCaption) {
        final String standardCaption = block.getStandardInputOutputPortCaption();
        if (standardCaption != null && !standardCaption.equals(customCaption)) {
            assert block.getSystemName() != null : "getStandardInputOutputPortCaption() returns non-null "
                    + standardCaption + " for null system name";
            // Note: we don't set specific caption if the corresponding block
            // has no system name or has standard caption like "[labels]",
            // formed on the base of system name;
            return customCaption;
        } else {
            return null;
        }
    }

    private static Map<String, PortConf> checkPorts(Map<String, PortConf> ports, String title) {
        Objects.requireNonNull(ports, "Null " + title + " ports");
        ports = new LinkedHashMap<>(ports);
        for (Map.Entry<String, PortConf> port : ports.entrySet()) {
            if (port.getKey() == null) {
                throw new IllegalArgumentException("Illegal " + title + " port: null key");
            }
            if (port.getValue() == null) {
                throw new IllegalArgumentException("Illegal " + title + " port[" + quote(port.getKey()) + "]: null");
            }
            if (!port.getKey().equals(port.getValue().name)) {
                throw new IllegalArgumentException("Illegal " + title + " port[" + quote(port.getKey())
                        + "]: its name is " + quote(port.getValue().name)
                        + " (must be equal to key " + quote(port.getKey()) + ")");
            }
        }
        return ports;
    }
}