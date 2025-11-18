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
import net.algart.executors.api.data.Port;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * <p>Detailed specification of an executor: ports, parameters, some key features of its behavior.</p>
 *
 * <p>Note: this class is not absolutely safe in relation of copying mutable data into and from this object.
 * Therefore, this object can become incorrect after creation, for example, by setting duplicated names
 * in several ports.</p>
 */
public class ExecutorSpecification extends AbstractConvertibleToJson {
    public enum JsonMode {
        FULL,
        MEDIUM,
        CONTROLS_ONLY;

        public boolean isSettingsSectionIncluded() {
            return this == FULL;
        }

        public boolean isTagsIncluded() {
            return this != CONTROLS_ONLY;
        }

        public boolean isOptionsIncluded() {
            return this != CONTROLS_ONLY;
        }

        public boolean isPortsIncluded() {
            return this != CONTROLS_ONLY;
        }
    }

    /**
     * Executor specification file extension: .json
     */
    public static final String EXECUTOR_FILE_PATTERN = ".*\\.json$";
    public static final String APP_NAME = "executor";
    public static final String CURRENT_VERSION = "1.0";
    public static final String SETTINGS = "settings";

    public static final char CATEGORY_SEPARATOR = '.';
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
            private String className = null;
            // - Some "class" of this role.
            // For example, for settings, by default, it is the settings className
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
                this.className = json.getString("class_name", null);
                this.resultPort = json.getString("result_port", null);
                this.settings = json.getBoolean("settings", false);
                this.main = json.getBoolean("main", false);
            }

            public String getClassName() {
                return className;
            }

            public Role setClassName(String className) {
                this.className = className;
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


            public boolean equalsClass(String className) {
                if (className == null || this.className == null) {
                    return false;
                }
                return this.className.equals(className);
            }

            public boolean matchesClass(String someEntityName) {
                if (someEntityName == null || className == null) {
                    return false;
                }
                return className.equals(someEntityName) || className.endsWith(CATEGORY_SEPARATOR + someEntityName);
            }

            @Override
            public String toString() {
                return "Role{" +
                        "className='" + className + '\'' +
                        ", resultPort='" + resultPort + '\'' +
                        ", settings=" + settings +
                        ", main=" + main +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (className != null) {
                    builder.add("class_name", className);
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
            // - for example, for a chain executor, this is an ID of the main settings

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
                    this.dataType = ParameterValueType.ofOrNull(dataType);
                    Jsons.requireNonNull(this.dataType, json, "data_type",
                            "unknown (\"" + dataType + "\")", file);
                }
                final String editionType = json.getString("edition_type", null);
                if (editionType != null) {
                    this.editionType = ControlEditionType.ofOrNull(editionType);
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
        // usually applied for chains or multi-chains
        private Behavior behavior = null;
        private Controlling controlling = null;
        private JsonObject extension = null;

        public Options() {
        }

        public Options(JsonObject json, Path file) {
            final String executionStage = json.getString("stage", ExecutionStage.RUN_TIME.stageName());
            this.stage = ExecutionStage.from(executionStage).orElseThrow(
                    () -> Jsons.unknownValueException(json, "stage", executionStage, file));
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

    public static final class Java {
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

        public Java() {
        }

        public Java(JsonObject json, Path file) {
            this.file = file;
            setJson(json);
        }

        public JsonObject getJson() {
            return json;
        }

        public Java setJson(JsonObject json) {
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

    private Path specificationFile = null;
    private String version = CURRENT_VERSION;
    private String platformId = null;
    // - usually not loaded from JSON file, but set later, while loading all JSON specification for some platform
    private String category;
    private String name;
    // - category and name cannot be autogenerated here, unlike SettingsSpecification and others
    private String description = null;
    private Set<String> tags = new LinkedHashSet<>();
    private String id;
    private Options options = null;
    private String language = null;
    private Java java = null;
    private Map<String, PortSpecification> inputPorts = new LinkedHashMap<>();
    private Map<String, PortSpecification> outputPorts = new LinkedHashMap<>();
    Map<String, ControlSpecification> controls = new LinkedHashMap<>();
    private SettingsSpecification settings = null;
    private SourceInfo sourceInfo = null;
    // - note: "sourceInfo" field is not usually loaded from FILE, it should be defined by external means
    // (but it can be loaded from JSON STRING while program interactions)
    private boolean javaExecutor = false;
    private boolean chainExecutor = false;

    private final Object controlsLock = new Object();
    // - allows correct changes in the controls from parallel threads:
    // can be useful while building settings tree

    private volatile String minimalSpecification = null;

    public ExecutorSpecification() {
    }

    protected ExecutorSpecification(JsonObject json, Path file) {
        if (!isExecutorSpecification(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not an executor configuration: no \"app\":\"" + APP_NAME + "\" element");
        }
        this.specificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        this.platformId = json.getString("platform_id", null);
        try {
            this.id = Jsons.reqStringWithAlias(json, "id", "uuid", file);
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
            final JsonObject javaJson = json.getJsonObject(Java.JAVA_CONF_NAME);
            if (javaExecutor && javaJson == null) {
                throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                        + ": \"" + Java.JAVA_CONF_NAME + "\" section required when \"language\" is \"java\"");
            }
            this.java = javaJson == null ? null : new Java(javaJson, file);
            if (json.containsKey("in_ports")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "in_ports", file)) {
                    final PortSpecification port = new PortSpecification(jsonObject, file);
                    putOrException(inputPorts, port.getName(), port, file, "in_ports");
                }
            }
            if (json.containsKey("out_ports")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "out_ports", file)) {
                    final PortSpecification port = new PortSpecification(jsonObject, file);
                    putOrException(outputPorts, port.getName(), port, file, "out_ports");
                }
            }
            if (json.containsKey("controls")) {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "controls", file)) {
                    final ControlSpecification control = new ControlSpecification(jsonObject, file);
                    putOrException(controls, control.getName(), control, file, "controls");
                }
            }
            final JsonObject settingsJson = json.getJsonObject(SETTINGS);
            if (settingsJson != null) {
                this.settings = SettingsSpecification.of(settingsJson);
            }
            final JsonObject sourceJson = json.getJsonObject("source");
            if (sourceJson != null) {
                this.sourceInfo = new SourceInfo(sourceJson, file);
            }
        } catch (JsonException e) {
            if (file != null || id == null) {
                throw e;
                // - file name is enough information to find a mistake
            }
            throw new JsonException("Problem in JSON specification for executor with ID '" + id + "\'"
                    + (name == null ? "" : ", name \'" + name + "\'")
                    + (description == null ? "" : ", description \'" + name + "\'"), e);
        }
    }

    public static ExecutorSpecification read(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        return new ExecutorSpecification(json, specificationFile);
    }

    public static ExecutorSpecification readIfValid(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new ExecutorSpecification(json, specificationFile);
    }

    public void write(Path specificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        Files.writeString(specificationFile, jsonString(), options);
    }

    public static <T> List<T> readAllJsonIfValid(
            List<T> result,
            Path containingJsonPath,
            Function<Path, T> reader)
            throws IOException {
        return readAllIfValid(
                result,
                containingJsonPath,
                true,
                reader,
                path -> path.getFileName().toString().toLowerCase().endsWith(".json"));
    }

    public static <S> List<S> readAllIfValid(
            List<S> result,
            Path containingJsonPath,
            boolean recursive,
            Function<Path, S> reader,
            Predicate<Path> isAllowedPath)
            throws IOException {
        Objects.requireNonNull(containingJsonPath, "Null containingJsonPath");
        Objects.requireNonNull(isAllowedPath, "Null isAllowedPath");
        if (result == null) {
            result = new ArrayList<>();
        }
        if (Files.isDirectory(containingJsonPath)) {
            try (Stream<Path> files = Files.list(containingJsonPath)) {
                for (Path file : files.sorted().toList()) {
                    // Important: we guarantee that the result will always be listed
                    // in the alphabetical order, not randomly
                    if (recursive || Files.isRegularFile(file)) {
                        readAllIfValid(result, file, recursive, reader, isAllowedPath);
                    }
                }
            }
        } else if (Files.isRegularFile(containingJsonPath) && isAllowedPath.test(containingJsonPath)) {
            final S specification = reader.apply(containingJsonPath);
            if (specification != null) {
                result.add(specification);
            }
        }
        return result;
    }


    public static ExecutorSpecification of(JsonObject specificationJson) {
        return new ExecutorSpecification(specificationJson, null);
    }

    public static ExecutorSpecification of(String specificationString) throws JsonException {
        Objects.requireNonNull(specificationString, "Null specificationString");
        final JsonObject executorSpecification = Jsons.toJson(specificationString);
        return new ExecutorSpecification(executorSpecification, null);
    }

    public static ExecutorSpecification of(Executor executor, String executorId) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(executorId, "Null executor ID");
        final ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(executor);
        result.setId(executorId);
        return result;
    }

    public static boolean isExecutorSpecificationFile(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        return COMPILED_EXECUTOR_FILE_PATTERN.matcher(specificationFile.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isExecutorSpecification(JsonObject specificationJson) {
        Objects.requireNonNull(specificationJson, "Null executor specification");
        return APP_NAME.equals(specificationJson.getString("app", null));
    }

    public static void checkIdDifference(Collection<? extends ExecutorSpecification> specifications) {
        Objects.requireNonNull(specifications, "Null executor specifications collection");
        final Set<String> ids = new HashSet<>();
        for (ExecutorSpecification specification : specifications) {
            final String id = specification.getId();
            assert id != null;
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two executor specifications have identical IDs " + id
                        + ", one of them is \"" + specification.getName() + "\"");
            }
        }
    }

    public final boolean hasSpecificationFile() {
        return specificationFile != null;
    }

    public final Path getSpecificationFile() {
        return specificationFile;
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

    public final String canonicalName() {
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

    public final String getId() {
        return id;
    }

    public final ExecutorSpecification setId(String id) {
        this.id = Objects.requireNonNull(id, "Null executor ID");
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

    public final Options.Role getRole() {
        return options == null ? null : options.getRole();
    }

    public final boolean isRoleSettings() {
        final Options.Role role = getRole();
        return role != null && role.isSettings();
    }

    public final String getLanguage() {
        return language;
    }

    public final ExecutorSpecification setLanguage(String language) {
        this.language = language;
        this.javaExecutor = Java.JAVA_LANGUAGE.equals(language);
        this.chainExecutor = ChainSpecification.CHAIN_LANGUAGE.equals(language);
        return this;
    }

    /**
     * Returns <code>true</code> if the current {@link #getLanguage() language} is
     * {@value Java#JAVA_LANGUAGE}.
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
    public final Java getJava() {
        return java;
    }

    public final ExecutorSpecification setJava(Java java) {
        this.java = java;
        return this;
    }

    public final PortSpecification getInputPort(String name) {
        return inputPorts.get(name);
    }

    public final Map<String, PortSpecification> getInputPorts() {
        return Collections.unmodifiableMap(inputPorts);
    }

    public final ExecutorSpecification setInputPorts(Map<String, PortSpecification> inputPorts) {
        this.inputPorts = checkInputPorts(inputPorts);
        return this;
    }

    public final PortSpecification getOutputPort(String name) {
        return outputPorts.get(name);
    }

    public final Map<String, PortSpecification> getOutputPorts() {
        return Collections.unmodifiableMap(outputPorts);
    }

    public final ExecutorSpecification setOutputPorts(Map<String, PortSpecification> outputPorts) {
        this.outputPorts = checkOutputPorts(outputPorts);
        return this;
    }

    public final ControlSpecification getControl(String name) {
        synchronized (controlsLock) {
            return controls.get(name);
        }
    }

    public final Map<String, ControlSpecification> getControls() {
        synchronized (controlsLock) {
            return Collections.unmodifiableMap(controls);
        }
    }

    public final ExecutorSpecification setControls(Map<String, ControlSpecification> controls) {
        synchronized (controlsLock) {
            this.controls = checkControls(controls);
        }
        return this;
    }

    public void updateControlSettingsId(String name, String settingsId) {
        Objects.requireNonNull(name, "Null control name");
        synchronized (controlsLock) {
            final ControlSpecification control = controls.get(name);
            if (control == null) {
                throw new IllegalArgumentException("No control with name " + name);
            }
            control.setSettingsId(settingsId);
        }
    }


    public SettingsSpecification getSettings() {
        return settings;
    }

    public ExecutorSpecification setSettings(SettingsSpecification settings) {
        this.settings = settings;
        return this;
    }

    public boolean hasSettings() {
        return settings != null;
    }

    public final SourceInfo getSourceInfo() {
        return sourceInfo;
    }

    public final ExecutorSpecification setSourceInfo(SourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
        return this;
    }

    public boolean hasSourceInfo() {
        return sourceInfo != null;
    }

    /**
     * Sets the source files information for this executor specification.
     *
     * <p>Usually it is set by {@link #setSourceInfoForSpecification()} method, which uses JSON file, passed
     * to  {@link #read(Path)} or {@link #readIfValid(Path)} methods, as a specification file, and does not try
     * to set the module source file (if that method is not overridden).
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
        return setSourceInfo(specificationFile, null);
    }

    public final void updateCategoryPrefix(String categoryPrefix) {
        this.category = updateCategoryPrefix(this.category, categoryPrefix);
    }

    public final void addTags(Collection<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags.addAll(tags);
    }

    public final void addInputPort(PortSpecification port) {
        Objects.requireNonNull(port, "Null input port");
        port.checkCompleteness();
        inputPorts.put(port.getName(), port);
    }

    public final void addFirstInputPort(PortSpecification port) {
        Objects.requireNonNull(port, "Null input port");
        port.checkCompleteness();
        final Map<String, PortSpecification> inputPorts = new LinkedHashMap<>();
        inputPorts.put(port.getName(), port);
        inputPorts.putAll(this.inputPorts);
        this.inputPorts = inputPorts;
    }

    public final void addOutputPort(PortSpecification port) {
        Objects.requireNonNull(port, "Null output port");
        port.checkCompleteness();
        outputPorts.put(port.getName(), port);
    }

    public final void addFirstOutputPort(PortSpecification port) {
        Objects.requireNonNull(port, "Null output port");
        port.checkCompleteness();
        final Map<String, PortSpecification> outputPorts = new LinkedHashMap<>();
        outputPorts.put(port.getName(), port);
        outputPorts.putAll(this.outputPorts);
        this.outputPorts = outputPorts;
    }

    public final void addSystemExecutorIdPort() {
        if (!outputPorts.containsKey(Executor.OUTPUT_EXECUTOR_ID_NAME)) {
            addOutputPort(new PortSpecification()
                    .setName(Executor.OUTPUT_EXECUTOR_ID_NAME)
                    .setCaption(OUTPUT_EXECUTOR_ID_CAPTION)
                    .setHint(OUTPUT_EXECUTOR_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addSystemPlatformIdPort() {
        if (!outputPorts.containsKey(Executor.OUTPUT_PLATFORM_ID_NAME)) {
            addOutputPort(new PortSpecification()
                    .setName(Executor.OUTPUT_PLATFORM_ID_NAME)
                    .setCaption(OUTPUT_PLATFORM_ID_CAPTION)
                    .setHint(OUTPUT_PLATFORM_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addSystemResourceFolderPort() {
        if (!outputPorts.containsKey(Executor.OUTPUT_RESOURCE_FOLDER_NAME)) {
            addOutputPort(new PortSpecification()
                    .setName(Executor.OUTPUT_RESOURCE_FOLDER_NAME)
                    .setCaption(OUTPUT_RESOURCE_FOLDER_CAPTION)
                    .setHint(OUTPUT_RESOURCE_FOLDER_ID_HINT)
                    .setValueType(DataType.SCALAR)
                    .setAdvanced(true));
        }
    }

    public final void addControl(ControlSpecification control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        controls.put(control.getName(), control);
    }

    public final void addFirstControl(ControlSpecification control) {
        Objects.requireNonNull(control, "Null control");
        control.checkCompleteness();
        final Map<String, ControlSpecification> controls = new LinkedHashMap<>();
        controls.put(control.getName(), control);
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
            builder.add("id", id);
            if (java != null) {
                builder.add(Java.JAVA_CONF_NAME, java.toJson());
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
            this.setJava(new Java().setJson(Java.standardJson(className)));
        }
        final Map<String, PortSpecification> inputPorts = new LinkedHashMap<>(this.inputPorts);
        for (Port port : executor.inputPorts()) {
            final String name = port.getName();
            final PortSpecification portSpecification = inputPorts.getOrDefault(name, new PortSpecification());
            inputPorts.put(name, portSpecification.setName(name).setValueType(port.getDataType()));
        }
        this.setInputPorts(inputPorts);
        final Map<String, PortSpecification> outputPorts = new LinkedHashMap<>(this.outputPorts);
        for (Port port : executor.outputPorts()) {
            final String name = port.getName();
            final PortSpecification portSpecification = outputPorts.getOrDefault(name, new PortSpecification());
            outputPorts.put(name, portSpecification.setName(name).setValueType(port.getDataType()));
        }
        this.setOutputPorts(outputPorts);
        final Map<String, ControlSpecification> controls = new LinkedHashMap<>(this.controls);
        for (String name : executor.allParameters()) {
            final ControlSpecification controlSpecification = controls.getOrDefault(name, new ControlSpecification());
            final ParameterValueType parameterValueType = executor.parameterControlValueType(name);
            controlSpecification.setName(name).setValueType(parameterValueType);
            if (parameterValueType == ParameterValueType.ENUM_STRING) {
                controlSpecification.setEditionType(ControlEditionType.ENUM);
                final Class<?> enumClass = executor.parameterJavaType(name);
                if (enumClass == null || !Enum.class.isAssignableFrom(enumClass)) {
                    throw new AssertionError("Invalid propertyJavaType method result: not enum ("
                            + enumClass + ")");
                }
                if (controlSpecification.getItems() == null) {
                    String firstEnumName = null;
                    final List<ControlSpecification.EnumItem> items = new ArrayList<>();
                    for (Enum<?> enumConstant : enumClass.asSubclass(Enum.class).getEnumConstants()) {
                        final String enumName = enumConstant.name();
                        if (firstEnumName == null) {
                            firstEnumName = enumName;
                        }
                        items.add(new ControlSpecification.EnumItem().setValue(enumName));
                    }
                    if (firstEnumName == null) {
                        throw new AssertionError("No constants in enum class: impossible in Java");
                    }
                    controlSpecification.setItems(items);
                    controlSpecification.setDefaultStringValue(firstEnumName);
                }
            } else {
                controlSpecification.setEditionType(ControlEditionType.VALUE);
            }
            controls.put(name, controlSpecification);
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
        this.setId(chain.id());
        this.setLanguage(ChainSpecification.CHAIN_LANGUAGE);
        final Map<String, PortSpecification> inputPorts = new LinkedHashMap<>(this.inputPorts);
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
            final PortSpecification portSpecification = new PortSpecification();
            portSpecification.setName(inputName);
            portSpecification.setValueType(inputPort.getDataType());
            setAdditionalFields(portSpecification, block);
            inputPorts.put(portSpecification.getName(), portSpecification);

        }
        this.setInputPorts(inputPorts);
        final Map<String, PortSpecification> outputPorts = new LinkedHashMap<>(this.outputPorts);
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
            final PortSpecification portSpecification = new PortSpecification();
            portSpecification.setName(outputName);
            portSpecification.setValueType(outputPort.getDataType());
            setAdditionalFields(portSpecification, block);
            outputPorts.put(portSpecification.getName(), portSpecification);
        }
        this.setOutputPorts(outputPorts);
        final Map<String, ControlSpecification> controls = new LinkedHashMap<>(this.controls);
        for (ChainBlock block : chain.getAllData()) {
            // - data blocks (with options.behavior.data = true) are used as parameters of the chain executor
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
                final ControlSpecification controlSpecification = controls.getOrDefault(parameterName, new ControlSpecification());
                controlSpecification.setName(parameterName);
                final ExecutorSpecification specification = block.getExecutorSpecification();
                ParameterValueType valueType = specification != null ? specification.dataType() : null;
                ControlEditionType editionType = specification != null ? specification.editionType() : null;
                if (valueType == null) {
                    valueType = ParameterValueType.STRING;
                }
                controlSpecification.setValueType(valueType);
                controlSpecification.setEditionType(editionType != null ? editionType : ControlEditionType.VALUE);
                String defaultStringValue = null;
                final ChainOutputPort outputPort = block.getActualOutputPort(Executor.DEFAULT_OUTPUT_PORT);
                if (outputPort != null && outputPort.getDataType() == DataType.SCALAR) {
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
                    controlSpecification.setDefaultJsonValue(valueType.toJsonValue(defaultStringValue));
                }
                setAdditionalFields(controlSpecification, block);
                controls.put(controlSpecification.getName(), controlSpecification);
            }
        }
        this.setControls(controls);
    }

    public final SettingsSpecification toSettingsSpecification() {
        SettingsSpecification result = new SettingsSpecification();
        result.setTo(this);
        return result;
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
        if (javaExecutor) {
            checkNull(java, Java.JAVA_CONF_NAME);
        }
    }

    public final JsonObject toJson(JsonMode mode) {
        checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        buildJson(builder, mode);
        return builder.build();
    }

    public final String jsonString(JsonMode mode) {
        return Jsons.toPrettyString(toJson(mode));
    }

    public final JsonObject defaultSettingsJson() {
        checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        buildDefaultSettingsJson(builder, null);
        return builder.build();
    }

    public final String defaultSettingsJsonString() {
        return Jsons.toPrettyString(defaultSettingsJson());
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        buildJson(builder, JsonMode.FULL);
    }

    public final void buildJson(JsonObjectBuilder builder, JsonMode mode) {
        buildJson(builder, mode, null);
    }

    void buildJson(
            JsonObjectBuilder builder,
            JsonMode mode,
            Function<String, JsonObject> subSettingsJsonBuilder) {
        Objects.requireNonNull(builder, "Null builder");
        Objects.requireNonNull(mode, "Null JSON mode");
        builder.add("app", APP_NAME);
        if (!version.equals(CURRENT_VERSION)) {
            builder.add("version", version);
        }
        if (platformId != null) {
            builder.add("platform_id", platformId);
        }
        builder.add("category", category);
        builder.add("name", name);
        if (description != null) {
            builder.add("description", description);
        }
        if (mode.isTagsIncluded() && !tags.isEmpty()) {
            final JsonArrayBuilder tagsBuilder = Json.createArrayBuilder();
            for (String tag : tags) {
                tagsBuilder.add(tag);
            }
            builder.add("tags", tagsBuilder.build());
        }
        builder.add("id", id);
        if (mode.isOptionsIncluded() && options != null) {
            builder.add("options", options.toJson());
        }
        if (language != null) {
            builder.add("language", language);
        }
        buildLanguageJson(builder);
        if (mode.isPortsIncluded()) {
            final JsonArrayBuilder inputPortsBuilder = Json.createArrayBuilder();
            for (PortSpecification port : inputPorts.values()) {
                inputPortsBuilder.add(port.toJson());
            }
            builder.add("in_ports", inputPortsBuilder.build());
            final JsonArrayBuilder outputPortsBuilder = Json.createArrayBuilder();
            for (PortSpecification port : outputPorts.values()) {
                outputPortsBuilder.add(port.toJson());
            }
            builder.add("out_ports", outputPortsBuilder.build());
        }
        final JsonArrayBuilder controlsBuilder = Json.createArrayBuilder();
        for (Map.Entry<String, ControlSpecification> entry : controls.entrySet()) {
            final String name = entry.getKey();
            final ControlSpecification control = entry.getValue();
            control.checkCompleteness();
            final JsonObjectBuilder controlBuilder = Json.createObjectBuilder();
            control.buildJson(controlBuilder);
            if (subSettingsJsonBuilder != null && control.isSubSettings()) {
                final JsonObject subSettingsJson = subSettingsJsonBuilder.apply(name);
                if (subSettingsJson != null) {
                    controlBuilder.add(SETTINGS, subSettingsJson);
                }
            }
            controlsBuilder.add(controlBuilder.build());
        }
        builder.add("controls", controlsBuilder.build());
        if (mode.isSettingsSectionIncluded() && settings != null) {
            builder.add(SETTINGS, settings.toJson(true));
            // - settings section is typically used for information needs,
            // so it is better to include here all information including autogenerated
        }
        if (sourceInfo != null) {
            builder.add("source", sourceInfo.toJson());
        }
    }

    void buildDefaultSettingsJson(
            JsonObjectBuilder builder,
            Function<String, JsonObject> subSettingsJsonBuilder) {
        // - subSettingsJsonBuilder is not used now: see SettingsTree.buildSettingsJson()
        Objects.requireNonNull(builder, "Null builder");
        for (Map.Entry<String, ControlSpecification> entry : controls.entrySet()) {
            final String name = entry.getKey();
            final ControlSpecification control = entry.getValue();
            control.checkCompleteness();
            if (subSettingsJsonBuilder != null) {
                if (SETTINGS.equals(name)) {
                    // - no sense to include this parameter while building tree:
                    // this should be filled by resulting JSON tree
                    continue;
                }
                if (control.isSubSettings()) {
                    final JsonObject subSettingsJson = subSettingsJsonBuilder.apply(name);
                    if (subSettingsJson != null) {
                        builder.add(ControlSpecification.settingsKey(name), subSettingsJson);
                        continue;
                    }
                }
            }
            if (control.hasDefaultJsonValue()) {
                builder.add(name, control.getDefaultJsonValue());
                // - note: without subSettingsJsonBuilder, we use simple keys even for sub-settings
            }
        }
    }

    @Override
    public String toString() {
        return "ExecutorSpecification{\n" +
                "  specificationFile=" + specificationFile +
                ",\n  version='" + version + '\'' +
                ",\n  platformId='" + platformId + '\'' +
                ",\n  category='" + category + '\'' +
                ",\n  name='" + name + '\'' +
                ",\n  description=" + (description == null ? null : "'" + description + "'") +
                ",\n  tags=" + tags +
                ",\n  id='" + id + '\'' +
                ",\n  options=" + options +
                ",\n  language=" + language +
                ",\n  java=" + java +
                ",\n  inputPorts=" + inputPorts +
                ",\n  outputPorts=" + outputPorts +
                ",\n  controls=" + controls +
                ",\n  settings=" + settings +
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

    public static Map<String, PortSpecification> checkInputPorts(Map<String, PortSpecification> ports) {
        return checkPorts(ports, "input");
    }

    public static Map<String, PortSpecification> checkOutputPorts(Map<String, PortSpecification> ports) {
        return checkPorts(ports, "output");
    }

    public static Map<String, ControlSpecification> checkControls(Map<String, ControlSpecification> controls) {
        Objects.requireNonNull(controls, "Null controls");
        controls = new LinkedHashMap<>(controls);
        for (Map.Entry<String, ControlSpecification> control : controls.entrySet()) {
            if (control.getKey() == null) {
                throw new IllegalArgumentException("Illegal control: null key");
            }
            final ControlSpecification specification = control.getValue();
            if (specification == null) {
                throw new IllegalArgumentException("Illegal control[" + quote(control.getKey()) + "]: null");
            }
            if (!control.getKey().equals(specification.getName())) {
                throw new IllegalArgumentException("Illegal control[" + quote(control.getKey())
                        + "]: its name is " + quote(specification.getName())
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

    public static String className(String category, String name) {
        return category + CATEGORY_SEPARATOR + name;
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

    protected void buildLanguageJson(JsonObjectBuilder builder) {
        if (java != null) {
            builder.add(Java.JAVA_CONF_NAME, java.toJson());
        }
    }

    private static void setAdditionalFields(ControlSpecification controlSpecification, ChainBlock chainBlock) {
        final ChainSpecification.Block block = chainBlock.getBlock();
        if (block != null) {
            final ChainSpecification.Block.System system = block.getSystem();
            controlSpecification.setCaption(makeCaption(chainBlock, system.getCaption()));
            controlSpecification.setDescription(system.getDescription());
        }
    }

    private static void setAdditionalFields(PortSpecification portSpecification, ChainBlock chainBlock) {
        final ChainSpecification.Block block = chainBlock.getBlock();
        if (block != null) {
            final ChainSpecification.Block.System system = block.getSystem();
            portSpecification.setCaption(makeCaption(chainBlock, system.getCaption()));
            portSpecification.setHint(system.getDescription());
        }
    }

    private static String makeCaption(ChainBlock chainBlock, String customCaption) {
        final String standardCaption = chainBlock.getStandardInputOutputPortCaption();
        if (standardCaption != null && !standardCaption.equals(customCaption)) {
            assert chainBlock.getSystemName() != null : "getStandardInputOutputPortCaption() returns non-null "
                    + standardCaption + " for null system name";
            // Note: we don't set specific caption if the corresponding chain block
            // has no system name or has standard caption like "[labels]",
            // formed on the base of system name;
            return customCaption;
        } else {
            return null;
        }
    }

    private static Map<String, PortSpecification> checkPorts(Map<String, PortSpecification> ports, String title) {
        Objects.requireNonNull(ports, "Null " + title + " ports");
        ports = new LinkedHashMap<>(ports);
        for (Map.Entry<String, PortSpecification> port : ports.entrySet()) {
            if (port.getKey() == null) {
                throw new IllegalArgumentException("Illegal " + title + " port: null key");
            }
            final PortSpecification specification = port.getValue();
            if (specification == null) {
                throw new IllegalArgumentException("Illegal " + title + " port[" + quote(port.getKey()) + "]: null");
            }
            if (!port.getKey().equals(specification.getName())) {
                throw new IllegalArgumentException("Illegal " + title + " port[" + quote(port.getKey())
                        + "]: its name is " + quote(specification.getName())
                        + " (must be equal to key " + quote(port.getKey()) + ")");
            }
        }
        return ports;
    }
}