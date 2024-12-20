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

package net.algart.executors.api.model;

import jakarta.json.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.ExecutionStage;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static net.algart.executors.api.model.ExecutorJson.quote;

// Note: this class is not absolutely safe in relation of copying mutable data into and from this object.
// As a result, this object can become incorrect after creation, for example, by setting duplicated names
// in several ports.
public final class ChainJson extends AbstractConvertibleToJson {
    public static final String CHAIN_TECHNOLOGY = "chain";
    public static final String CHAIN_LANGUAGE = "chain";
    public static final String CHAIN_FILE_PATTERN = ".*\\.(json|chain)$";

    public static final String CHAIN_SECTION = "chain";
    public static final String CHAIN_APP_NAME = "chain";
    public static final String CHAIN_CURRENT_VERSION = "1.1";

    public static final String DEFAULT_CHAIN_CATEGORY = "subchains";
    public static final String DEFAULT_CHAIN_NAME = "Sub-chain";
    public static final char CATEGORY_SEPARATOR = '.';

    private static final String CHAIN_SECTION_ALIAS = "stare_chain";
    private static final String CHAIN_APP_NAME_ALIAS = "stare-chain";
    private static final Pattern COMPILED_CHAIN_FILE_PATTERN = Pattern.compile(CHAIN_FILE_PATTERN);

    public static final class Executor extends AbstractConvertibleToJson {
        public static final class Options extends AbstractConvertibleToJson {
            public static final class Execution extends AbstractConvertibleToJson {
                private boolean all = false;
                private boolean multithreading = true;
                private boolean ignoreExceptions = false;

                public Execution() {
                }

                private Execution(JsonObject json, Path file) {
                    this.all = json.getBoolean("all", false);
                    this.multithreading = json.getBoolean("multithreading", true);
                    this.ignoreExceptions = json.getBoolean("ignore_exceptions", false);
                }

                public boolean isAll() {
                    return all;
                }

                public Execution setAll(boolean all) {
                    this.all = all;
                    return this;
                }

                public boolean isMultithreading() {
                    return multithreading;
                }

                public Execution setMultithreading(boolean multithreading) {
                    this.multithreading = multithreading;
                    return this;
                }

                public boolean isIgnoreExceptions() {
                    return ignoreExceptions;
                }

                public Execution setIgnoreExceptions(boolean ignoreExceptions) {
                    this.ignoreExceptions = ignoreExceptions;
                    return this;
                }

                @Override
                public void checkCompleteness() {
                }

                @Override
                public String toString() {
                    return "Execution{" +
                            "all=" + all +
                            ", multithreading=" + multithreading +
                            ", ignoreExceptions=" + ignoreExceptions +
                            '}';
                }

                @Override
                public void buildJson(JsonObjectBuilder builder) {
                    builder.add("all", all);
                    builder.add("multithreading", multithreading);
                    builder.add("ignore_exceptions", ignoreExceptions);
                }
            }

            private Execution execution = new Execution();

            public Options() {
            }

            private Options(JsonObject json, Path file) {
                final JsonObject executionJson = json.getJsonObject("execution");
                if (executionJson != null) {
                    this.execution = new Execution(executionJson, file);
                }
            }

            public Execution getExecution() {
                return execution;
            }

            public Options setExecution(Execution execution) {
                this.execution = Objects.requireNonNull(execution, "Null execution");
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Options{" +
                        "execution=" + execution +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("execution", execution.toJson());
            }
        }

        private boolean autogeneratedCategory = false;
        private String category;
        private boolean autogeneratedName = false;
        private String name;
        private String description = null;
        private String id;
        private Options options = new Options();

        public Executor() {
        }

        private Executor(JsonObject json, Path file) {
            final String fileName = file == null ? null : removeExtension(file.getFileName().toString());
            final String recommendedName = ExecutionBlock.recommendedName(fileName);
            final String recommendedCategory = ExecutionBlock.recommendedCategory(fileName);
            this.category = json.getString("category", null);
            if (this.category == null) {
                this.category = recommendedCategory == null ? DEFAULT_CHAIN_CATEGORY :
                        DEFAULT_CHAIN_CATEGORY + CATEGORY_SEPARATOR + recommendedCategory;
                autogeneratedCategory = true;
            }
            this.name = json.getString("name", null);
            if (this.name == null) {
                this.name = recommendedName == null ? DEFAULT_CHAIN_NAME : recommendedName;
                this.autogeneratedName = true;
            }
            this.description = json.getString("description", null);
            this.id = Jsons.reqStringWithAlias(json, "id", "uuid", file);
            final JsonObject optionsJson = json.getJsonObject("options");
            if (optionsJson != null) {
                this.options = new Options(optionsJson, file);
            }
        }

        public boolean isAutogeneratedCategory() {
            return autogeneratedCategory;
        }

        public String getCategory() {
            return category;
        }

        public Executor setCategory(String category) {
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

        public Executor setName(String name) {
            this.name = Objects.requireNonNull(name, "Null name");
            this.autogeneratedName = false;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Executor setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getId() {
            return id;
        }

        public Executor setId(String id) {
            this.id = Objects.requireNonNull(id, "Null ID");
            return this;
        }

        public Options getOptions() {
            return options;
        }

        public Executor setOptions(Options options) {
            this.options = Objects.requireNonNull(options, "Null options");
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(category, "category");
            checkNull(name, "name");
            checkNull(id, "id");
        }

        @Override
        public String toString() {
            return "Executor{\n" +
                    "    category='" + category + '\'' +
                    ",\n    name='" + name + '\'' +
                    ",\n    description=" + (description == null ? null : "'" + description + "'") +
                    ",\n    id=" + id +
                    ",\n    options=" + options +
                    "\n  }";
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("category", category);
            builder.add("name", name);
            if (description != null) {
                builder.add("description", description);
            }
            builder.add("id", id);
            builder.add("options", options.toJson());
        }
    }

    public static final class ChainBlockConf extends AbstractConvertibleToJson {
        public static final class PortConf extends AbstractConvertibleToJson {
            private String uuid;
            private String name;
            private ChainPortType portType;
            private DataType dataType;

            public PortConf() {
            }

            private PortConf(JsonObject json, Path file) {
                this.uuid = Jsons.reqString(json, "uuid", file);
                this.name = Jsons.reqStringWithAlias(json, "name", "port_name", file);
                this.portType = ChainPortType.valueOfCodeOrNull(
                        Jsons.reqIntWithAlias(json, "type", "port_type", file));
                Jsons.requireNonNull(portType, json, "type", file);
                this.dataType = DataType.valueOfUuidOrNull(
                        Jsons.reqString(json, "data_type_uuid", file));
                Jsons.requireNonNull(dataType, json, "data_type_uuid", file);
                assert portType != null : "was checked in requireNonNull";
                if (portType.isVirtual() && dataType != DataType.SCALAR) {
                    throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                            + ": \"port " + name + "\" is virtual (" + portType
                            + ") and must contain scalar data only, but data type is " + dataType
                            + " <<<" + json + ">>>");
                }
            }

            public String getUuid() {
                return uuid;
            }

            public PortConf setUuid(String uuid) {
                this.uuid = Objects.requireNonNull(uuid, "Null uuid");
                return this;
            }

            public String getName() {
                return name;
            }

            public PortConf setName(String name) {
                this.name = Objects.requireNonNull(name, "Null name");
                return this;
            }

            public ChainPortType getPortType() {
                return portType;
            }

            public PortConf setPortType(ChainPortType portType) {
                this.portType = Objects.requireNonNull(portType, "Null portType");
                return this;
            }

            public DataType getDataType() {
                return dataType;
            }

            public PortConf setDataType(DataType dataType) {
                this.dataType = Objects.requireNonNull(dataType, "Null dataType");
                return this;
            }

            @Override
            public void checkCompleteness() {
                checkNull(uuid, "uuid");
                checkNull(name, "name");
                checkNull(portType, "portType");
                checkNull(dataType, "dataType");
            }

            @Override
            public String toString() {
                return "Port{" +
                        "uuid='" + uuid + '\'' +
                        ", name='" + name + '\'' +
                        ", portType=" + portType +
                        ", dataType=" + dataType +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("uuid", uuid);
                builder.add("name", name);
                builder.add("type", portType.code());
                builder.add("data_type_uuid", dataType.uuid().toString());
            }
        }

        public static final class PropertyConf extends AbstractConvertibleToJson {
            private String name;
            private JsonValue value = null;

            public PropertyConf() {
            }

            private PropertyConf(JsonObject json, Path file) {
                this.name = Jsons.reqString(json, "name", file);
                this.value = json.get("value");
            }

            public String getName() {
                return name;
            }

            public PropertyConf setName(String name) {
                this.name = Objects.requireNonNull(name, "Null name");
                return this;
            }

            public JsonValue getValue() {
                return value;
            }

            public PropertyConf setValue(JsonValue value) {
                this.value = value;
                return this;
            }

            @Override
            public void checkCompleteness() {
                checkNull(name, "name");
            }

            @Override
            public String toString() {
                return "Property{" +
                        "name='" + name + '\'' +
                        ", value=" + value +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("name", name);
                if (value != null) {
                    builder.add("value", value);
                }
            }
        }

        public static final class SystemConf extends AbstractConvertibleToJson {
            private String name = null;
            private String caption = null;
            private String description = null;
            private boolean enabled = true;

            public SystemConf() {
            }

            private SystemConf(JsonObject json, Path file) {
                this.name = json.getString("name", null);
                this.caption = json.getString("caption", null);
                this.description = json.getString("description", null);
                this.enabled = json.getBoolean("enabled", true);
            }

            public String getName() {
                return name;
            }

            public SystemConf setName(String name) {
                this.name = name;
                return this;
            }

            public String getCaption() {
                return caption;
            }

            public SystemConf setCaption(String caption) {
                this.caption = caption;
                return this;
            }

            public String getDescription() {
                return description;
            }

            public SystemConf setDescription(String description) {
                this.description = description;
                return this;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public SystemConf setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public String name() {
                if (this.name == null) {
                    return null;
                }
                String name = this.name.trim();
                return name.isEmpty() ? null : name;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "SystemConf{" +
                        "name='" + name + '\'' +
                        ", caption='" + caption + '\'' +
                        ", description='" + description + '\'' +
                        ", enabled=" + enabled +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (name != null) {
                    builder.add("name", name);
                }
                if (caption != null) {
                    builder.add("caption", caption);
                }
                if (description != null) {
                    builder.add("description", description);
                }
                builder.add("enabled", enabled);
            }
        }

        private static final String[] OLD_PORT_ARRAYS = {
                "in_ports",
                "out_ports",
                "in_control_ports",
                "out_control_ports"};

        private String uuid;
        private String executorId;
        private String executorName = null;
        private String executorCategory = null;
        private ExecutionStage executionStage = ExecutionStage.RUN_TIME;
        private Map<String, ChainBlockConf.PortConf> uuidToPortMap = new LinkedHashMap<>();
        private Map<String, PropertyConf> nameToPropertyMap = new LinkedHashMap<>();
        private SystemConf system = new SystemConf();

        public ChainBlockConf() {
        }

        private ChainBlockConf(JsonObject json, Path file) {
            this.uuid = Jsons.reqString(json, "uuid", file);
            this.executorId = Jsons.reqStringWithAlias(json, "model_uuid", "executor_id", file);
            this.executorName = json.getString("executor_name", null);
            this.executorCategory = json.getString("executor_category", null);
            final String executionStage = json.getString("execution_stage", ExecutionStage.RUN_TIME.stageName());
            this.executionStage = ExecutionStage.valueOfStageNameOrNull(executionStage);
            Jsons.requireNonNull(this.executionStage, json,
                    "execution_stage", "unknown (\"" + executionStage + "\")", file);
            boolean oldFormat = false;
            if (!json.containsKey("ports")) {
                for (String name : OLD_PORT_ARRAYS) {
                    oldFormat |= json.containsKey(name);
                }
            }
            if (oldFormat) {
                for (String name : OLD_PORT_ARRAYS) {
                    if (json.containsKey(name)) {
                        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, name, file)) {
                            final ChainBlockConf.PortConf port = new ChainBlockConf.PortConf(jsonObject, file);
                            uuidToPortMap.put(port.uuid, port);
                        }
                    }
                }
            } else {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "ports", file)) {
                    final ChainBlockConf.PortConf port = new ChainBlockConf.PortConf(jsonObject, file);
                    uuidToPortMap.put(port.uuid, port);
                }
            }
            for (JsonObject jsonObject : reqJsonObjectsWithAlias(
                    json, "properties", "primitives", file)) {
                final PropertyConf property = new PropertyConf(jsonObject, file);
                nameToPropertyMap.put(property.name, property);
            }
            final JsonObject systemJson = json.getJsonObject("system");
            if (systemJson != null) {
                system = new SystemConf(systemJson, file);
            }
        }

        public String getUuid() {
            return uuid;
        }

        public ChainBlockConf setUuid(String uuid) {
            this.uuid = Objects.requireNonNull(uuid, "Null UUID");
            return this;
        }

        public String getExecutorId() {
            return executorId;
        }

        public ChainBlockConf setExecutorId(String executorId) {
            this.executorId = Objects.requireNonNull(executorId, "Null executor UUID");
            return this;
        }

        public String getExecutorName() {
            return executorName;
        }

        public ChainBlockConf setExecutorName(String executorName) {
            this.executorName = executorName;
            return this;
        }

        public String getExecutorCategory() {
            return executorCategory;
        }

        public ChainBlockConf setExecutorCategory(String executorCategory) {
            this.executorCategory = executorCategory;
            return this;
        }

        public ExecutionStage getExecutionStage() {
            return executionStage;
        }

        public ChainBlockConf setExecutionStage(ExecutionStage executionStage) {
            this.executionStage = Objects.requireNonNull(executionStage, "Null execution stage");
            return this;
        }

        public Map<String, ChainBlockConf.PortConf> getUuidToPortMap() {
            return Collections.unmodifiableMap(uuidToPortMap);
        }

        public ChainBlockConf setUuidToPortMap(Map<String, ChainBlockConf.PortConf> uuidToPortMap) {
            this.uuidToPortMap = checkPorts(uuidToPortMap);
            return this;
        }

        public Map<String, PropertyConf> getNameToPropertyMap() {
            return Collections.unmodifiableMap(nameToPropertyMap);
        }

        public ChainBlockConf setNameToPropertyMap(Map<String, PropertyConf> nameToPropertyMap) {
            this.nameToPropertyMap = checkProperties(nameToPropertyMap);
            return this;
        }

        public SystemConf getSystem() {
            assert system != null;
            return system;
        }

        public ChainBlockConf setSystem(SystemConf system) {
            this.system = Objects.requireNonNull(system, "Null system");
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(uuid, "uuid");
            checkNull(executorId, "executorUuid");
        }

        @Override
        public String toString() {
            return "ChainBlockConf{" +
                    "uuid='" + uuid + '\'' +
                    ", executorId='" + executorId + '\'' +
                    ", executorName='" + executorName + '\'' +
                    ", executorCategory='" + executorCategory + '\'' +
                    ", executionStage=" + executionStage +
                    ", uuidToPortMap=" + uuidToPortMap +
                    ", nameToPropertyMap=" + nameToPropertyMap +
                    ", system=" + system +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("uuid", uuid);
            builder.add("executor_id", executorId);
            if (executorName != null) {
                builder.add("executor_name", executorName);
            }
            if (executorCategory != null) {
                builder.add("executor_category", executorCategory);
            }
            if (executionStage != ExecutionStage.RUN_TIME) {
                builder.add("execution_stage", executionStage.stageName());
            }
            final JsonArrayBuilder portsBuilder = Json.createArrayBuilder();
            for (ChainBlockConf.PortConf port : uuidToPortMap.values()) {
                portsBuilder.add(port.toJson());
            }
            builder.add("ports", portsBuilder.build());
            final JsonArrayBuilder propertiesBuilder = Json.createArrayBuilder();
            for (PropertyConf property : nameToPropertyMap.values()) {
                propertiesBuilder.add(property.toJson());
            }
            builder.add("properties", propertiesBuilder.build());
            builder.add("system", system.toJson());
        }

        private static Map<String, ChainBlockConf.PortConf> checkPorts(Map<String, ChainBlockConf.PortConf> ports) {
            Objects.requireNonNull(ports, "Null ports");
            ports = new LinkedHashMap<>(ports);
            for (Map.Entry<String, ChainBlockConf.PortConf> port : ports.entrySet()) {
                if (port.getKey() == null) {
                    throw new IllegalArgumentException("Illegal port: null key");
                }
                if (port.getValue() == null) {
                    throw new IllegalArgumentException("Illegal port[" + quote(port.getKey()) + "]: null");
                }
                if (!port.getKey().equals(port.getValue().uuid)) {
                    throw new IllegalArgumentException("Illegal port[" + quote(port.getKey())
                            + "]: its uuid is " + quote(port.getValue().uuid)
                            + " (must be equal to key " + quote(port.getKey()) + ")");
                }
            }
            return ports;
        }

        private static Map<String, PropertyConf> checkProperties(Map<String, PropertyConf> properties) {
            Objects.requireNonNull(properties, "Null properties");
            properties = new LinkedHashMap<>(properties);
            for (Map.Entry<String, PropertyConf> property : properties.entrySet()) {
                if (property.getKey() == null) {
                    throw new IllegalArgumentException("Illegal property: null key");
                }
                if (property.getValue() == null) {
                    throw new IllegalArgumentException("Illegal property[" + quote(property.getKey())
                            + "]: null");
                }
                if (!property.getKey().equals(property.getValue().name)) {
                    throw new IllegalArgumentException("Illegal property[" + quote(property.getKey())
                            + "]: its name is " + quote(property.getValue().name)
                            + " (must be equal to key " + quote(property.getKey()) + ")");
                }
            }
            return properties;
        }
    }

    public static final class ChainLinkConf extends AbstractConvertibleToJson {
        private String uuid;
        private String srcPortUuid;
        private String destPortUuid;

        public ChainLinkConf() {
        }

        private ChainLinkConf(JsonObject json, Path file) {
            this.uuid = json.getString("uuid", null);
            this.srcPortUuid = json.getString("src_port_uuid", null);
            this.destPortUuid = json.getString("dest_port_uuid", null);
            // - in some "damaged" files, links without necessary fields can appear
        }

        private boolean isValid() {
            return this.uuid != null && this.srcPortUuid != null && this.destPortUuid != null;
        }

        public String getUuid() {
            return uuid;
        }

        public ChainLinkConf setUuid(String uuid) {
            this.uuid = Objects.requireNonNull(uuid, "Null uuid");
            return this;
        }

        public String getSrcPortUuid() {
            return srcPortUuid;
        }

        public ChainLinkConf setSrcPortUuid(String srcPortUuid) {
            this.srcPortUuid = Objects.requireNonNull(srcPortUuid, "Null srcPortUuid");
            return this;
        }

        public String getDestPortUuid() {
            return destPortUuid;
        }

        public ChainLinkConf setDestPortUuid(String destPortUuid) {
            this.destPortUuid = Objects.requireNonNull(destPortUuid, "Null destPortUuid");
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(uuid, "uuid");
            checkNull(srcPortUuid, "srcPortUuid");
            checkNull(destPortUuid, "destPortUuid");
        }

        @Override
        public String toString() {
            return "ChainLink{" +
                    "uuid='" + uuid + '\'' +
                    ", srcPortUuid='" + srcPortUuid + '\'' +
                    ", destPortUuid='" + destPortUuid + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("uuid", uuid);
            builder.add("src_port_uuid", srcPortUuid);
            builder.add("dest_port_uuid", destPortUuid);
        }
    }

    private Path chainJsonFile = null;
    private String version = CHAIN_CURRENT_VERSION;
    private Executor executor;
    private List<ChainBlockConf> blocks = new ArrayList<>();
    private List<ChainLinkConf> links = new ArrayList<>();

    // The following properties are not loaded from JSON file, but are set later,
    // while loading all JSON models for some platform
    private Set<String> tags = new LinkedHashSet<>();
    private String platformId = null;
    private String platformCategory = null;

    public ChainJson() {
    }

    private ChainJson(JsonObject json, Path file) {
        if (!isChainJson(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + (json == null ? " does not contain \"" : " contains illegal \"")
                    + CHAIN_SECTION + "\" section with chain configuration"
                    + (json == null ? "" : ": it does not contain \"app\":\"" + CHAIN_APP_NAME + "\" element"));
        }
        this.chainJsonFile = file;
        this.version = json.getString("version", CHAIN_CURRENT_VERSION);
        this.executor = new Executor(Jsons.reqJsonObject(json, "executor", file), file);
        for (JsonObject jsonObject : reqJsonObjectsWithAlias(
                json, "blocks", "data_processes", file)) {
            this.blocks.add(new ChainBlockConf(jsonObject, file));
        }
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "links", file)) {
            final ChainLinkConf link = new ChainLinkConf(jsonObject, file);
            if (link.isValid()) {
                this.links.add(link);
            }
        }
    }

    public static ChainJson valueOf(JsonObject chainJson) {
        return new ChainJson(chainJson, null);
    }

    public static ChainJson valueOf(String chainJsonString) {
        return valueOf(chainJsonString, true);
    }

    public static ChainJson valueOfIfValid(String chainJsonString) {
        return valueOf(chainJsonString, false);
    }

    public static ChainJson read(Path containingJsonFile) throws IOException {
        return read(containingJsonFile, true);
    }

    public static ChainJson readIfValid(Path containingJsonFile) {
        try {
            return read(containingJsonFile, false);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
    }

    public static List<ChainJson> readAllIfValid(Path containingJsonPath, boolean recursive) throws IOException {
        return readAllIfValid(null, containingJsonPath, recursive);
    }

    public static List<ChainJson> readAllIfValid(
            List<ChainJson> result,
            Path containingJsonPath,
            boolean recursive)
            throws IOException {
        return ExtensionJson.readAllIfValid(
                result, containingJsonPath, recursive, ChainJson::readIfValid, ChainJson::isChainJsonFile);
    }

    public void rewriteChainSection(Path containingJsonFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(containingJsonFile, "Null containingJsonFile");
        final LinkedHashMap<String, JsonValue> clone;
        if (Files.exists(containingJsonFile)) {
            final JsonObject existingJson = Jsons.readJson(containingJsonFile);
            Jsons.reqJsonObjectWithAlias(existingJson, CHAIN_SECTION, CHAIN_SECTION_ALIAS, containingJsonFile);
            clone = new LinkedHashMap<>(existingJson);
        } else {
            clone = new LinkedHashMap<>();
        }
        clone.put(CHAIN_SECTION, toJson());
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : clone.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        final JsonObject json = builder.build();
        Files.writeString(containingJsonFile, Jsons.toPrettyString(json), options);
    }

    public static boolean isChainJsonFile(Path file) {
        Objects.requireNonNull(file, "Null file");
        return COMPILED_CHAIN_FILE_PATTERN.matcher(file.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isChainJson(JsonObject chainJson) {
        if (chainJson == null) {
            return false;
        }
        final String appName = chainJson.getString("app", null);
        return CHAIN_APP_NAME.equals(appName) || CHAIN_APP_NAME_ALIAS.equals(appName);
    }

    public static JsonObject getChainJson(JsonObject json) {
        Objects.requireNonNull(json, "Null json");
        if (json.containsKey(CHAIN_SECTION)) {
            return Jsons.getJsonObject(json, CHAIN_SECTION, null);
        } else if (json.containsKey(CHAIN_SECTION_ALIAS)) {
            return Jsons.getJsonObject(json, CHAIN_SECTION_ALIAS, null);
        }
        return null;
    }

    public static JsonObject getChainJson(JsonObject json, JsonObject defaultValue) {
        final JsonObject chainJson = getChainJson(json);
        return chainJson != null ? chainJson : defaultValue;
    }

    public static boolean isChainJsonContainer(JsonObject json) {
        return isChainJson(getChainJson(json, json));
    }

    public static void checkIdDifference(Collection<ChainJson> chains) {
        Objects.requireNonNull(chains, "Null chain JSONs collection");
        final Map<String, String> ids = new HashMap<>();
        for (ChainJson chain : chains) {
            final String name = ids.put(chain.chainId(), chain.executor.getName());
            if (name != null) {
                throw new IllegalArgumentException("Two chains with names \"" + name + "\" and \""
                        + chain.executor.getName() + "\" have identical ID " + chain.chainId()
                        + (chain.chainJsonFile == null ? "" :
                        ", the 2nd chain is loaded from the file " + chain.chainJsonFile));
            }
        }
    }

    public boolean hasChainJsonFile() {
        return chainJsonFile != null;
    }

    public Path getChainJsonFile() {
        return chainJsonFile;
    }

    public ChainJson setChainJsonFile(Path chainJsonFile) {
        this.chainJsonFile = chainJsonFile;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public ChainJson setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public ChainJson setExecutor(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "Null executor");
        return this;
    }

    public List<ChainBlockConf> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public ChainJson setBlocks(List<ChainBlockConf> blocks) {
        this.blocks = ExecutorJson.checkNonNullObjects(blocks);
        return this;
    }

    public List<ChainLinkConf> getLinks() {
        return links;
    }

    public ChainJson setLinks(List<ChainLinkConf> links) {
        this.links = links;
        return this;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public ChainJson setTags(Set<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags = new LinkedHashSet<>(tags);
        return this;
    }

    public void addTags(Collection<String> tags) {
        Objects.requireNonNull(tags, "Null tags");
        this.tags.addAll(tags);
    }

    public String getPlatformId() {
        return platformId;
    }

    public ChainJson setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public String getPlatformCategory() {
        return platformCategory;
    }

    public ChainJson setPlatformCategory(String platformCategory) {
        this.platformCategory = platformCategory;
        return this;
    }

    public String chainCategory() {
        return executor.category;
    }

    public String chainName() {
        return executor.name;
    }

    public String chainId() {
        return executor.id;
    }

    public String canonicalName() {
        return chainCategory() + CATEGORY_SEPARATOR + chainName();
    }

    @Override
    public void checkCompleteness() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChainJson{\n  version=" + version +
                ",\n  executor=" + executor +
                ",\n  blocks=[\n");
        for (ChainBlockConf block : blocks) {
            sb.append("    ").append(block).append('\n');
        }
        sb.append("  ],\n  links=[\n");
        for (ChainLinkConf link : links) {
            sb.append("    ").append(link).append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", CHAIN_APP_NAME);
        builder.add("version", version);
        builder.add("executor", executor.toJson());
        final JsonArrayBuilder blocksBuilder = Json.createArrayBuilder();
        for (ChainBlockConf block : blocks) {
            blocksBuilder.add(block.toJson());
        }
        builder.add("blocks", blocksBuilder.build());
        final JsonArrayBuilder linksBuilder = Json.createArrayBuilder();
        for (ChainLinkConf link : links) {
            linksBuilder.add(link.toJson());
        }
        builder.add("links", linksBuilder.build());
    }

    private static ChainJson valueOf(String chainJsonString, boolean requireValid) {
        Objects.requireNonNull(chainJsonString, "Null chainJsonString");
        JsonObject json = Jsons.toJson(chainJsonString);
        json = getChainJson(json, json);
        if (!ChainJson.isChainJson(json) && !requireValid) {
            return null;
        }
        return new ChainJson(json, null);
    }

    private static ChainJson read(Path containingJsonFile, boolean requireValid) throws IOException {
        Objects.requireNonNull(containingJsonFile, "Null containingJsonFile");
        final JsonObject json = Jsons.readJson(containingJsonFile);
        JsonObject chainJson = Jsons.getJsonObject(json, CHAIN_SECTION, containingJsonFile);
        if (chainJson == null) {
            chainJson = Jsons.getJsonObject(json, CHAIN_SECTION_ALIAS, containingJsonFile);
        }
        if (!ChainJson.isChainJson(chainJson) && !requireValid) {
            return null;
        }
        return new ChainJson(chainJson, containingJsonFile);
    }

    private static String removeExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        return p == -1 ? fileName : fileName.substring(0, p);
    }

    private static List<JsonObject> reqJsonObjectsWithAlias(
            JsonObject json,
            String name,
            String aliasName,
            Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonArray jsonArray;
        try {
            jsonArray = json.getJsonArray(name);
            if (jsonArray == null && aliasName != null) {
                jsonArray = json.getJsonArray(aliasName);
            }
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a JSON array"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (jsonArray == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" array required");
        }
        return Jsons.toJsonObjects(jsonArray, name, file);
    }


}
