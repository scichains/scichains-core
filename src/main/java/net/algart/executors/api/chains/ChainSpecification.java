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

package net.algart.executors.api.chains;

import jakarta.json.*;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.system.ExecutionStage;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.algart.executors.api.system.ExecutorSpecification.quote;

// Note: this class is not absolutely safe in relation of copying mutable data into and from this object.
// As a result, this object can become incorrect after creation, for example, by setting duplicated names
// in several ports.
public final class ChainSpecification extends AbstractConvertibleToJson {
    public static final String CHAIN_TECHNOLOGY = "chain";
    public static final String CHAIN_LANGUAGE = "chain";

    /**
     * Chain file extensions:<br>
     * .json<br>
     * .chain
     */
    public static final String CHAIN_FILE_PATTERN = ".*\\.(json|chain)$";

    public static final String CHAIN_SECTION = "chain";
    public static final String CHAIN_APP_NAME = "chain";
    public static final String CURRENT_VERSION = "1.1";

    public static final String DEFAULT_CHAIN_CATEGORY = "chains";
    public static final String DEFAULT_CHAIN_CATEGORY_PREFIX =
            DEFAULT_CHAIN_CATEGORY + ExecutorSpecification.CATEGORY_SEPARATOR;
    public static final String DEFAULT_CHAIN_NAME = "chain";

    private static final String CHAIN_SECTION_LEGACY_ALIAS = "stare_chain";
    private static final String CHAIN_APP_NAME_LEGACY_ALIAS = "stare-chain";

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
                this.category = recommendedCategory == null ?
                        DEFAULT_CHAIN_CATEGORY :
                        DEFAULT_CHAIN_CATEGORY_PREFIX + recommendedCategory;
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

        public Executor setCategory(String category, boolean autogeneratedCategory) {
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

        public Executor setName(String name, boolean autogeneratedName) {
            this.name = Objects.requireNonNull(name, "Null name");
            this.autogeneratedName = autogeneratedName;
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
            if (!autogeneratedCategory) {
                builder.add("category", category);
            }
            if (!autogeneratedCategory) {
                builder.add("name", name);
            }
            if (description != null) {
                builder.add("description", description);
            }
            builder.add("id", id);
            builder.add("options", options.toJson());
        }
    }

    public static final class Block extends AbstractConvertibleToJson {
        public static final class Port extends AbstractConvertibleToJson {
            private String uuid;
            private String name;
            private ChainPortType portType;
            private DataType dataType;

            public Port() {
            }

            private Port(JsonObject json, Path file) {
                this.uuid = Jsons.reqString(json, "uuid", file);
                this.name = Jsons.reqStringWithAlias(json, "name", "port_name", file);
                final int portTypeCode = Jsons.reqIntWithAlias(json, "type", "port_type", file);
                this.portType = ChainPortType.fromCode(portTypeCode)
                        .orElseThrow(() -> Jsons.badValue(json, "type", String.valueOf(portTypeCode), file));
                final String dataTypeUuid = Jsons.reqString(json, "data_type_uuid", file);
                this.dataType = DataType.fromUUID(dataTypeUuid)
                        .orElseThrow(() -> Jsons.badValue(json, "data_type_uuid", dataTypeUuid,
                                Stream.of(DataType.values()).map(DataType::uuid), file));
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

            public Port setUuid(String uuid) {
                this.uuid = Objects.requireNonNull(uuid, "Null uuid");
                return this;
            }

            public String getName() {
                return name;
            }

            public Port setName(String name) {
                this.name = Objects.requireNonNull(name, "Null name");
                return this;
            }

            public ChainPortType getPortType() {
                return portType;
            }

            public Port setPortType(ChainPortType portType) {
                this.portType = Objects.requireNonNull(portType, "Null portType");
                return this;
            }

            public DataType getDataType() {
                return dataType;
            }

            public Port setDataType(DataType dataType) {
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

        public static final class Parameter extends AbstractConvertibleToJson {
            private String name;
            private JsonValue value = null;

            public Parameter() {
            }

            private Parameter(JsonObject json, Path file) {
                this.name = Jsons.reqString(json, "name", file);
                this.value = json.get("value");
            }

            public String getName() {
                return name;
            }

            public Parameter setName(String name) {
                this.name = Objects.requireNonNull(name, "Null name");
                return this;
            }

            public JsonValue getValue() {
                return value;
            }

            public Parameter setValue(JsonValue value) {
                this.value = value;
                return this;
            }

            @Override
            public void checkCompleteness() {
                checkNull(name, "name");
            }

            @Override
            public String toString() {
                return "Parameter{" +
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

        public static final class System extends AbstractConvertibleToJson {
            private String name = null;
            private String caption = null;
            private String description = null;
            private boolean enabled = true;

            public System() {
            }

            private System(JsonObject json, Path file) {
                this.name = json.getString("name", null);
                this.caption = json.getString("caption", null);
                this.description = json.getString("description", null);
                this.enabled = json.getBoolean("enabled", true);
            }

            public String getName() {
                return name;
            }

            public System setName(String name) {
                this.name = name;
                return this;
            }

            public String getCaption() {
                return caption;
            }

            public System setCaption(String caption) {
                this.caption = caption;
                return this;
            }

            public String getDescription() {
                return description;
            }

            public System setDescription(String description) {
                this.description = description;
                return this;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public System setEnabled(boolean enabled) {
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
                return "System{" +
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
        private Map<String, Port> uuidToPortMap = new LinkedHashMap<>();
        private Map<String, Parameter> nameToParameterMap = new LinkedHashMap<>();
        private System system = new System();

        public Block() {
        }

        private Block(JsonObject json, Path file) {
            this.uuid = Jsons.reqString(json, "uuid", file);
            this.executorId = Jsons.reqStringWithAlias(json, "model_uuid", "executor_id", file);
            this.executorName = json.getString("executor_name", null);
            this.executorCategory = json.getString("executor_category", null);
            String executionStageName = json.getString("execution_stage", ExecutionStage.RUN_TIME.stageName());
            this.executionStage = ExecutionStage.fromStageName(executionStageName).orElseThrow(
                    () -> Jsons.badValue(json, "stage", executionStageName, ExecutionStage.stageNames(), file));
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
                            final Port port = new Port(jsonObject, file);
                            uuidToPortMap.put(port.uuid, port);
                        }
                    }
                }
            } else {
                for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "ports", file)) {
                    final Port port = new Port(jsonObject, file);
                    uuidToPortMap.put(port.uuid, port);
                }
            }
            List<JsonObject> jsonParameters = reqJsonObjectsWithAlias(
                    json, "parameters", "properties", "primitives", file);
            for (JsonObject jsonObject : jsonParameters) {
                final Parameter parameter = new Parameter(jsonObject, file);
                nameToParameterMap.put(parameter.name, parameter);
            }
            final JsonObject systemJson = json.getJsonObject("system");
            if (systemJson != null) {
                system = new System(systemJson, file);
            }
        }

        public String getUuid() {
            return uuid;
        }

        public Block setUuid(String uuid) {
            this.uuid = Objects.requireNonNull(uuid, "Null UUID");
            return this;
        }

        public String getExecutorId() {
            return executorId;
        }

        public Block setExecutorId(String executorId) {
            this.executorId = Objects.requireNonNull(executorId, "Null executor UUID");
            return this;
        }

        public String getExecutorName() {
            return executorName;
        }

        public Block setExecutorName(String executorName) {
            this.executorName = executorName;
            return this;
        }

        public String getExecutorCategory() {
            return executorCategory;
        }

        public Block setExecutorCategory(String executorCategory) {
            this.executorCategory = executorCategory;
            return this;
        }

        public ExecutionStage getExecutionStage() {
            return executionStage;
        }

        public Block setExecutionStage(ExecutionStage executionStage) {
            this.executionStage = Objects.requireNonNull(executionStage, "Null execution stage");
            return this;
        }

        public Map<String, Port> getUuidToPortMap() {
            return Collections.unmodifiableMap(uuidToPortMap);
        }

        public Block setUuidToPortMap(Map<String, Port> uuidToPortMap) {
            this.uuidToPortMap = checkPorts(uuidToPortMap);
            return this;
        }

        public Map<String, Parameter> getNameToParameterMap() {
            return Collections.unmodifiableMap(nameToParameterMap);
        }

        public Block setNameToParameterMap(Map<String, Parameter> nameToParameterMap) {
            this.nameToParameterMap = checkParameters(nameToParameterMap);
            return this;
        }

        public System getSystem() {
            assert system != null;
            return system;
        }

        public Block setSystem(System system) {
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
            return "Block{" +
                    "uuid='" + uuid + '\'' +
                    ", executorId='" + executorId + '\'' +
                    ", executorName='" + executorName + '\'' +
                    ", executorCategory='" + executorCategory + '\'' +
                    ", executionStage=" + executionStage +
                    ", uuidToPortMap=" + uuidToPortMap +
                    ", nameToParameterMap=" + nameToParameterMap +
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
            for (Port port : uuidToPortMap.values()) {
                portsBuilder.add(port.toJson());
            }
            builder.add("ports", portsBuilder.build());
            final JsonArrayBuilder parametersBuilder = Json.createArrayBuilder();
            for (Parameter parameter : nameToParameterMap.values()) {
                parametersBuilder.add(parameter.toJson());
            }
            builder.add("parameters", parametersBuilder.build());
            builder.add("system", system.toJson());
        }

        private static Map<String, Port> checkPorts(Map<String, Port> ports) {
            Objects.requireNonNull(ports, "Null ports");
            ports = new LinkedHashMap<>(ports);
            for (Map.Entry<String, Port> port : ports.entrySet()) {
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

        private static Map<String, Parameter> checkParameters(Map<String, Parameter> parameters) {
            Objects.requireNonNull(parameters, "Null parameters");
            parameters = new LinkedHashMap<>(parameters);
            for (Map.Entry<String, Parameter> parameter : parameters.entrySet()) {
                if (parameter.getKey() == null) {
                    throw new IllegalArgumentException("Illegal parameter: null key");
                }
                if (parameter.getValue() == null) {
                    throw new IllegalArgumentException("Illegal parameter[" + quote(parameter.getKey())
                            + "]: null");
                }
                if (!parameter.getKey().equals(parameter.getValue().name)) {
                    throw new IllegalArgumentException("Illegal parameter[" + quote(parameter.getKey())
                            + "]: its name is " + quote(parameter.getValue().name)
                            + " (must be equal to key " + quote(parameter.getKey()) + ")");
                }
            }
            return parameters;
        }
    }

    public static final class Link extends AbstractConvertibleToJson {
        private String uuid;
        private String srcPortUuid;
        private String destPortUuid;

        public Link() {
        }

        private Link(JsonObject json, Path file) {
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

        public Link setUuid(String uuid) {
            this.uuid = Objects.requireNonNull(uuid, "Null uuid");
            return this;
        }

        public String getSrcPortUuid() {
            return srcPortUuid;
        }

        public Link setSrcPortUuid(String srcPortUuid) {
            this.srcPortUuid = Objects.requireNonNull(srcPortUuid, "Null srcPortUuid");
            return this;
        }

        public String getDestPortUuid() {
            return destPortUuid;
        }

        public Link setDestPortUuid(String destPortUuid) {
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
            return "Link{" +
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

    private Path specificationFile = null;
    private String version = CURRENT_VERSION;
    private Executor executor;
    private List<Block> blocks = new ArrayList<>();
    private List<Link> links = new ArrayList<>();

    // The following properties are not loaded from JSON file, but are set later,
    // while loading all JSON specifications for some platform
    private Set<String> tags = new LinkedHashSet<>();
    private String platformId = null;
    private String platformCategory = null;

    public ChainSpecification() {
    }

    private ChainSpecification(JsonObject json, Path file) {
        if (!isChainSpecification(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + (json == null ? " does not contain \"" : " contains illegal \"")
                    + CHAIN_SECTION + "\" section with chain configuration"
                    + (json == null ? "" : ": it does not contain \"app\":\"" + CHAIN_APP_NAME + "\" element"));
        }
        this.specificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        this.executor = new Executor(Jsons.reqJsonObject(json, "executor", file), file);
        for (JsonObject jsonObject : reqJsonObjectsWithAlias(
                json, "blocks", "data_processes", null, file)) {
            this.blocks.add(new Block(jsonObject, file));
        }
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "links", file)) {
            final Link link = new Link(jsonObject, file);
            if (link.isValid()) {
                this.links.add(link);
            }
        }
    }

    public static ChainSpecification of(JsonObject chainSpecification) {
        return new ChainSpecification(chainSpecification, null);
    }

    public static ChainSpecification of(String chainSpecificationString) {
        return of(chainSpecificationString, true);
    }

    public static ChainSpecification read(Path containingJsonFile) throws IOException {
        return read(containingJsonFile, true);
    }

    public static ChainSpecification readIfValid(Path containingJsonFile) {
        try {
            return read(containingJsonFile, false);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
    }

    public static List<ChainSpecification> readAllIfValid(Path containingJsonPath, boolean recursive) throws IOException {
        return readAllIfValid(null, containingJsonPath, recursive);
    }

    public static List<ChainSpecification> readAllIfValid(
            List<ChainSpecification> result,
            Path containingJsonPath,
            boolean recursive)
            throws IOException {
        return ExecutorSpecification.readAllIfValid(
                result,
                containingJsonPath,
                recursive,
                ChainSpecification::readIfValid,
                ChainSpecification::isChainSpecificationFile);
    }

    public void rewriteChainSection(Path containingJsonFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(containingJsonFile, "Null containingJsonFile");
        final LinkedHashMap<String, JsonValue> clone;
        if (Files.exists(containingJsonFile)) {
            final JsonObject existingJson = Jsons.readJson(containingJsonFile);
            Jsons.reqJsonObjectWithAlias(existingJson, CHAIN_SECTION, CHAIN_SECTION_LEGACY_ALIAS, containingJsonFile);
            clone = new LinkedHashMap<>(existingJson);
        } else {
            clone = new LinkedHashMap<>();
        }
        clone.remove(CHAIN_SECTION_LEGACY_ALIAS);
        clone.put(CHAIN_SECTION, toJson());
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : clone.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        final JsonObject json = builder.build();
        Files.writeString(containingJsonFile, Jsons.toPrettyString(json), options);
    }

    public static boolean isChainSpecificationFile(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        return COMPILED_CHAIN_FILE_PATTERN.matcher(specificationFile.getFileName().toString().toLowerCase()).matches();
    }

    public static boolean isChainSpecification(JsonObject specificationJson) {
        if (specificationJson == null) {
            return false;
        }
        final String appName = specificationJson.getString("app", null);
        return CHAIN_APP_NAME.equals(appName) || CHAIN_APP_NAME_LEGACY_ALIAS.equals(appName);
    }

    public static JsonObject getChainSpecification(JsonObject json) {
        Objects.requireNonNull(json, "Null json");
        if (json.containsKey(CHAIN_SECTION)) {
            return Jsons.getJsonObject(json, CHAIN_SECTION, null);
        } else if (json.containsKey(CHAIN_SECTION_LEGACY_ALIAS)) {
            return Jsons.getJsonObject(json, CHAIN_SECTION_LEGACY_ALIAS, null);
        }
        return null;
    }

    public static JsonObject getChainSpecification(JsonObject json, JsonObject defaultValue) {
        final JsonObject chainSpecification = getChainSpecification(json);
        return chainSpecification != null ? chainSpecification : defaultValue;
    }

    public static boolean isChainSpecificationContainer(JsonObject json) {
        return isChainSpecification(getChainSpecification(json, json));
    }

    public static void checkIdDifference(Collection<ChainSpecification> chains) {
        Objects.requireNonNull(chains, "Null chain JSONs collection");
        final Set<String> ids = new HashSet<>();
        for (ChainSpecification chain : chains) {
            if (!ids.add(chain.chainId())) {
                throw new IllegalArgumentException("Two chain variants have identical ID " + chain.chainId()
                        + (chain.specificationFile == null ? "" :
                        ", the 2nd chain is loaded from the file " + chain.specificationFile));
            }
        }
    }

    public boolean hasSpecificationFile() {
        return specificationFile != null;
    }

    public Path getSpecificationFile() {
        return specificationFile;
    }

    public String getVersion() {
        return version;
    }

    public ChainSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public ChainSpecification setExecutor(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "Null executor");
        return this;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public ChainSpecification setBlocks(List<Block> blocks) {
        this.blocks = ExecutorSpecification.checkNonNullObjects(blocks);
        return this;
    }

    public List<Link> getLinks() {
        return links;
    }

    public ChainSpecification setLinks(List<Link> links) {
        this.links = links;
        return this;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public ChainSpecification setTags(Set<String> tags) {
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

    public ChainSpecification setPlatformId(String platformId) {
        this.platformId = platformId;
        return this;
    }

    public String getPlatformCategory() {
        return platformCategory;
    }

    public ChainSpecification setPlatformCategory(String platformCategory) {
        this.platformCategory = platformCategory;
        return this;
    }

    public String chainCategory() {
        return executor.category;
    }

    public String chainName() {
        assert executor.name != null;
        return executor.name;
    }

    public String chainId() {
        assert executor.id != null;
        return executor.id;
    }

    public String canonicalName() {
        return ExecutorSpecification.className(chainCategory(), chainName());
    }

    @Override
    public void checkCompleteness() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChainSpecification{\n  version=" + version +
                ",\n  executor=" + executor +
                ",\n  blocks=[\n");
        for (Block block : blocks) {
            sb.append("    ").append(block).append('\n');
        }
        sb.append("  ],\n  links=[\n");
        for (Link link : links) {
            sb.append("    ").append(link).append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", CHAIN_APP_NAME);
        if (!version.equals(CURRENT_VERSION)) {
            builder.add("version", version);
        }
        builder.add("executor", executor.toJson());
        final JsonArrayBuilder blocksBuilder = Json.createArrayBuilder();
        for (Block block : blocks) {
            blocksBuilder.add(block.toJson());
        }
        builder.add("blocks", blocksBuilder.build());
        final JsonArrayBuilder linksBuilder = Json.createArrayBuilder();
        for (Link link : links) {
            linksBuilder.add(link.toJson());
        }
        builder.add("links", linksBuilder.build());
    }

    private static ChainSpecification of(String chainSpecificationString, boolean requireValid) {
        Objects.requireNonNull(chainSpecificationString, "Null chainSpecificationString");
        JsonObject json = Jsons.toJson(chainSpecificationString);
        json = getChainSpecification(json, json);
        if (!ChainSpecification.isChainSpecification(json) && !requireValid) {
            return null;
        }
        return new ChainSpecification(json, null);
    }

    private static ChainSpecification read(Path containingJsonFile, boolean requireValid) throws IOException {
        Objects.requireNonNull(containingJsonFile, "Null containingJsonFile");
        final JsonObject json = Jsons.readJson(containingJsonFile);
        JsonObject chainSpecification = Jsons.getJsonObject(json, CHAIN_SECTION, containingJsonFile);
        if (chainSpecification == null) {
            chainSpecification = Jsons.getJsonObject(json, CHAIN_SECTION_LEGACY_ALIAS, containingJsonFile);
        }
        if (!ChainSpecification.isChainSpecification(chainSpecification) && !requireValid) {
            return null;
        }
        return new ChainSpecification(chainSpecification, containingJsonFile);
    }

    private static String removeExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        return p == -1 ? fileName : fileName.substring(0, p);
    }

    private static List<JsonObject> reqJsonObjectsWithAlias(
            JsonObject json,
            String name,
            String aliasName1,
            String aliasName2,
            Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonArray jsonArray;
        try {
            jsonArray = json.getJsonArray(name);
            if (jsonArray == null && aliasName1 != null) {
                jsonArray = json.getJsonArray(aliasName1);
            }
            if (jsonArray == null && aliasName2 != null) {
                jsonArray = json.getJsonArray(aliasName2);
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
