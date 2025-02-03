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
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.io.MatrixIO;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class MultiChainSpecification extends AbstractConvertibleToJson {
    public static final String APP_NAME = "multi-chain";
    public static final String CURRENT_VERSION = "1.0";

    public static final String DEFAULT_MULTICHAIN_CATEGORY = "modules";
    public static final String DEFAULT_MULTICHAIN_NAME = "multichain";

    private static final String DEFAULT_MULTICHAIN_SETTINGS_NAME = "Multi-settings";
    private static final String DEFAULT_MULTICHAIN_SETTINGS_PREFIX = "Multi-settings ";

    public static final class Options extends AbstractConvertibleToJson {
        public static final class Behavior extends AbstractConvertibleToJson {
            private boolean skippable = false;

            public Behavior() {
            }

            public Behavior(JsonObject json, Path file) {
                this.skippable = json.getBoolean("skippable", false);
            }

            public boolean isSkippable() {
                return skippable;
            }

            public Behavior setSkippable(boolean skippable) {
                this.skippable = skippable;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Behavior{" +
                        "skippable=" + skippable +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("skippable", skippable);
            }
        }

        private Behavior behavior = null;

        public Options() {
        }

        public Options(JsonObject json, Path file) {
            final JsonObject behaviorJson = json.getJsonObject("behavior");
            if (behaviorJson != null) {
                this.behavior = new Behavior(behaviorJson, file);
            }
        }

        public Behavior getBehavior() {
            return behavior;
        }

        public Options setBehavior(Behavior behavior) {
            this.behavior = behavior;
            return this;
        }

        @Override
        public void checkCompleteness() {
        }

        @Override
        public String toString() {
            return "Options{" +
                    "behavior=" + behavior +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            if (behavior != null) {
                builder.add("behavior", behavior.toJson());
            }
        }
    }

    private Path multiChainSpecificationFile = null;
    private String version = CURRENT_VERSION;
    private String category;
    private String settingsCategory;
    private String name;
    private String settingsName;
    private String description = null;
    private String settingsDescription = null;
    private String id;
    private String settingsId;
    private Options options = null;
    private List<String> chainVariantPaths = new ArrayList<>();
    private String defaultChainVariantId = null;
    private Map<String, ExecutorSpecification.PortConf> inPorts = new LinkedHashMap<>();
    private Map<String, ExecutorSpecification.PortConf> outPorts = new LinkedHashMap<>();
    private Map<String, ExecutorSpecification.ControlConf> controls = new LinkedHashMap<>();

    public MultiChainSpecification() {
    }

    private MultiChainSpecification(JsonObject json, boolean strictMode, Path file) {
        if (!isMultiChainSpecification(json) && strictMode) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a multi-chain configuration: no \"app\":\""
                    + APP_NAME + "\" element");
        }
        this.multiChainSpecificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        final String fileName;
        fileName = file == null ? null : MatrixIO.removeExtension(file.getFileName().toString());
        final String recommendedName = ExecutionBlock.recommendedName(fileName);
        final String recommendedCategory = ExecutionBlock.recommendedCategory(fileName);
        this.category = json.getString("category",
                recommendedCategory != null ?
                        DEFAULT_MULTICHAIN_CATEGORY + ExecutorSpecification.CATEGORY_SEPARATOR + recommendedCategory :
                        DEFAULT_MULTICHAIN_CATEGORY);
        this.settingsCategory = json.getString("settings_category",
                recommendedCategory != null ?
                        SettingsSpecification.DEFAULT_SETTINGS_CATEGORY_PREFIX + recommendedCategory :
                        SettingsSpecification.DEFAULT_SETTINGS_CATEGORY);
        this.name = json.getString("name",
                recommendedName != null ? recommendedName : DEFAULT_MULTICHAIN_NAME);
        this.settingsName = json.getString("settings_name",
                recommendedName != null ?
                        DEFAULT_MULTICHAIN_SETTINGS_PREFIX + recommendedName :
                        DEFAULT_MULTICHAIN_SETTINGS_NAME);
        this.description = json.getString("description", null);
        this.settingsDescription = json.getString("settings_description", null);
        this.id = Jsons.reqString(json, "id", file);
        this.settingsId = modifyIdForSettings(id);
        final JsonObject optionsJson = json.getJsonObject("options");
        if (optionsJson != null) {
            this.options = new Options(optionsJson, file);
        }
        this.chainVariantPaths = toFileNames(new ArrayList<>(),
                Jsons.reqJsonArray(json, "chain_variant_paths", file), "chain_variant_paths");
        this.defaultChainVariantId = json.getString("default_variant_id", null);
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "in_ports", file)) {
            final ExecutorSpecification.PortConf port = new ExecutorSpecification.PortConf(jsonObject, file);
            ExecutorSpecification.putOrException(inPorts, port.getName(), port, file, "in_ports");
        }
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "out_ports", file)) {
            final ExecutorSpecification.PortConf port = new ExecutorSpecification.PortConf(jsonObject, file);
            ExecutorSpecification.putOrException(outPorts, port.getName(), port, file, "out_ports");
        }
        for (JsonObject jsonObject : Jsons.reqJsonObjects(json, "controls", file)) {
            final ExecutorSpecification.ControlConf control = new ExecutorSpecification.ControlConf(jsonObject, file);
            ExecutorSpecification.putOrException(controls, control.getName(), control, file, "controls");
        }
    }

    public static MultiChainSpecification read(Path multiChainSpecificationFile) throws IOException {
        Objects.requireNonNull(multiChainSpecificationFile, "Null multiChainSpecificationFile");
        final JsonObject json = Jsons.readJson(multiChainSpecificationFile);
        return new MultiChainSpecification(json, true, multiChainSpecificationFile);
    }

    public static MultiChainSpecification readIfValid(Path multiSubChainSpecificationFile) {
        Objects.requireNonNull(multiSubChainSpecificationFile, "Null multiSubChainSpecificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(multiSubChainSpecificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isMultiChainSpecification(json)) {
            return null;
        }
        return new MultiChainSpecification(json, true, multiSubChainSpecificationFile);
    }

    public static List<MultiChainSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return ExtensionSpecification.readAllIfValid(null, containingJsonPath, MultiChainSpecification::readIfValid);
    }

    public void write(Path multiChainSpecificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(multiChainSpecificationFile, "Null multiChainSpecificationFile");
        Files.writeString(multiChainSpecificationFile, Jsons.toPrettyString(toJson()), options);
    }

    public static MultiChainSpecification of(JsonObject multiSpecificationJson, boolean strictMode) {
        return new MultiChainSpecification(multiSpecificationJson, strictMode, null);
    }

    public static boolean isMultiChainSpecification(JsonObject multiChainSpecification) {
        Objects.requireNonNull(multiChainSpecification, "Null multiChainSpecification");
        return APP_NAME.equals(multiChainSpecification.getString("app", null));
    }

    public static void checkIdDifference(Collection<MultiChainSpecification> multiChainSpecifications) {
        Objects.requireNonNull(multiChainSpecifications, "Null multiChainSpecifications");
        final Set<String> ids = new HashSet<>();
        for (MultiChainSpecification multiChainSpecification : multiChainSpecifications) {
            final String id = multiChainSpecification.getId();
            final String settingsId = multiChainSpecification.getSettingsId();
            assert id != null;
            if (id.equals(settingsId)) {
                // - impossible
                throw new AssertionError("Identical id and settingsId for multi-chain \""
                        + multiChainSpecification.getName() + "\": " + id);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Two multi-chain JSONs have identical IDs " + id
                        + ", one of them is \"" + multiChainSpecification.getName() + "\"");
            }
            if (settingsId != null && !ids.add(settingsId)) {
                throw new IllegalArgumentException("Two settings-combiner JSONs have identical IDs " + settingsId
                        + ", one of them is \"" + multiChainSpecification.getName() + "\"");
            }
        }
    }

    public static void checkNameDifference(Collection<ChainSpecification> chains) {
        Objects.requireNonNull(chains, "Null chain JSONs collection");
        final Set<String> names = new HashSet<>();
        for (ChainSpecification chain : chains) {
            final String name = chain.chainName();
            if (!names.add(name)) {
                final Path file = chain.getChainSpecificationFile();
                throw new IllegalArgumentException("Two chain variants have identical name \"" + name
                        + "\", but it is prohibited inside the single multi-chain! "
                        + "(One of 2 chain variants has ID \"" + chain.chainId() + "\""
                        + (file == null ? "" : " and loaded from the file " + file + ".)"));
            }
        }
    }

    public Path getMultiChainSpecificationFile() {
        return multiChainSpecificationFile;
    }

    public String getVersion() {
        return version;
    }

    public MultiChainSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public String getCategory() {
        return category;
    }

    public MultiChainSpecification setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Null category");
        return this;
    }

    public String getSettingsCategory() {
        return settingsCategory;
    }

    public MultiChainSpecification setSettingsCategory(String settingsCategory) {
        this.settingsCategory = Objects.requireNonNull(settingsCategory, "Null settingsCategory");
        return this;
    }

    public String getName() {
        return name;
    }

    public MultiChainSpecification setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        return this;
    }

    public String getSettingsName() {
        return settingsName;
    }

    public MultiChainSpecification setSettingsName(String settingsName) {
        this.settingsName = Objects.requireNonNull(settingsName, "Null settingsName");
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MultiChainSpecification setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSettingsDescription() {
        return settingsDescription;
    }

    public MultiChainSpecification setSettingsDescription(String settingsDescription) {
        this.settingsDescription = settingsDescription;
        return this;
    }

    public String getId() {
        return id;
    }

    public MultiChainSpecification setId(String id) {
        this.id = Objects.requireNonNull(id, "Null id");
        this.settingsId = modifyIdForSettings(this.id);
        return this;
    }

    public String getSettingsId() {
        return settingsId;
    }

    public MultiChainSpecification setSettingsId(String settingsId) {
        this.settingsId = settingsId;
        return this;
    }

    public Options getOptions() {
        return options;
    }

    public MultiChainSpecification setOptions(Options options) {
        this.options = options;
        return this;
    }

    public List<String> getChainVariantPaths() {
        return Collections.unmodifiableList(chainVariantPaths);
    }

    public MultiChainSpecification setChainVariantPaths(List<String> chainVariantPaths) {
        this.chainVariantPaths = checkChainVariantPaths(chainVariantPaths);
        return this;
    }

    public String getDefaultChainVariantId() {
        return defaultChainVariantId;
    }

    public MultiChainSpecification setDefaultChainVariantId(String defaultChainVariantId) {
        this.defaultChainVariantId = defaultChainVariantId;
        return this;
    }

    public Map<String, ExecutorSpecification.PortConf> getInPorts() {
        return Collections.unmodifiableMap(inPorts);
    }

    public MultiChainSpecification setInPorts(Map<String, ExecutorSpecification.PortConf> inPorts) {
        this.inPorts = ExecutorSpecification.checkInPorts(inPorts);
        return this;
    }

    public Map<String, ExecutorSpecification.PortConf> getOutPorts() {
        return Collections.unmodifiableMap(outPorts);
    }

    public MultiChainSpecification setOutPorts(Map<String, ExecutorSpecification.PortConf> outPorts) {
        this.outPorts = ExecutorSpecification.checkOutPorts(outPorts);
        return this;
    }

    public Map<String, ExecutorSpecification.ControlConf> getControls() {
        return Collections.unmodifiableMap(controls);
    }

    public MultiChainSpecification setControls(Map<String, ExecutorSpecification.ControlConf> controls) {
        this.controls = ExecutorSpecification.checkControls(controls);
        return this;
    }

    public String canonicalName() {
        return ExecutorSpecification.className(category, name);
    }

    public List<Path> resolveChainVariantPaths() {
        return chainVariantPaths.stream().map(p -> resolve(p, "sub-chain path")).toList();
    }

    public List<ChainSpecification> readChainVariants() throws IOException {
        final List<ChainSpecification> result = new ArrayList<>();
        final List<Path> paths = resolveChainVariantPaths();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                ChainSpecification.readAllIfValid(result, path, false);
            } else {
                result.add(ChainSpecification.read(path));
            }
        }
        if (result.isEmpty()) {
            throw new FileNotFoundException("No valid sub-chains found for multi-chain \"" + name
                    + "\" among the following paths: " + paths);
        }
        ChainSpecification.checkIdDifference(result);
        result.sort(Comparator.comparing(ChainSpecification::chainName));
        checkNameDifference(result);
        return result;
    }

    public void checkImplementationCompatibility(ExecutorSpecification implementationSpecification) {
        Objects.requireNonNull(implementationSpecification, "Null implementationSpecification");
        for (ExecutorSpecification.PortConf port : inPorts.values()) {
            final ExecutorSpecification.PortConf implementationPort =
                    implementationSpecification.getInPort(port.getName());
            if (implementationPort == null) {
                continue;
                // - if an implementation has no corresponding input port, it is not a problem:
                // it means that this implementation does not use this information
            }
            if (!port.isCompatible(implementationPort)) {
                throw new IncompatibleChainException(checkImplementationMessageStart(implementationSpecification)
                        + " has incompatible input port \"" + port.getName() + "\"");
            }
        }
        for (ExecutorSpecification.PortConf port : outPorts.values()) {
            final ExecutorSpecification.PortConf implementationPort =
                    implementationSpecification.getOutPort(port.getName());
            if (implementationPort == null) {
                throw new IncompatibleChainException(checkImplementationMessageStart(implementationSpecification)
                        + " has no output port \"" + port.getName() + "\"");
            }
            if (!port.isCompatible(implementationPort)) {
                throw new IncompatibleChainException(checkImplementationMessageStart(implementationSpecification)
                        + " has incompatible output port \"" + port.getName() + "\"");
            }
        }
    }

    private String checkImplementationMessageStart(ExecutorSpecification implementationSpecification) {
        return "Implementation \""
                + implementationSpecification.getName() + "\" (ID \""
                + implementationSpecification.getId() + "\") of multi-chain \""
                + name + "\" (ID \"" + id + "\")";
    }

    @Override
    public void checkCompleteness() {
        checkNull(category, "category");
        checkNull(name, "name");
        checkNull(id, "id");
        checkNull(settingsId, "settingsId");
    }

    @Override
    public String toString() {
        return "MultiChainSpecification{" +
                "multiChainSpecificationFile=" + multiChainSpecificationFile +
                ", version='" + version + '\'' +
                ", category='" + category + '\'' +
                ", settingsCategory='" + settingsCategory + '\'' +
                ", name='" + name + '\'' +
                ", settingsName='" + settingsName + '\'' +
                ", description='" + description + '\'' +
                ", settingsDescription='" + settingsDescription + '\'' +
                ", id='" + id + '\'' +
                ", settingsId='" + settingsId + '\'' +
                ", options=" + options +
                ", chainVariantPaths=" + chainVariantPaths +
                ", defaultChainVariantId='" + defaultChainVariantId + '\'' +
                ", inPorts=" + inPorts +
                ", outPorts=" + outPorts +
                ", controls=" + controls +
                '}';
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", APP_NAME);
        builder.add("version", version);
        builder.add("category", category);
        if (settingsCategory != null) {
            builder.add("settings_category", settingsCategory);
        }
        builder.add("name", name);
        if (settingsName != null) {
            builder.add("settings_name", settingsName);
        }
        if (description != null) {
            builder.add("description", description);
        }
        if (settingsDescription != null) {
            builder.add("settings_description", settingsDescription);
        }
        builder.add("id", id);
        if (options != null) {
            builder.add("options", options.toJson());
        }
        final JsonArrayBuilder chainVariantPathsBuilder = Json.createArrayBuilder();
        for (String path : chainVariantPaths) {
            chainVariantPathsBuilder.add(path);
        }
        builder.add("chain_variant_paths", chainVariantPathsBuilder.build());
        if (defaultChainVariantId != null) {
            builder.add("default_variant_id", defaultChainVariantId);
        }
        final JsonArrayBuilder inPortsBuilder = Json.createArrayBuilder();
        for (ExecutorSpecification.PortConf port : inPorts.values()) {
            inPortsBuilder.add(port.toJson());
        }
        builder.add("in_ports", inPortsBuilder.build());
        final JsonArrayBuilder outPortsBuilder = Json.createArrayBuilder();
        for (ExecutorSpecification.PortConf port : outPorts.values()) {
            outPortsBuilder.add(port.toJson());
        }
        builder.add("out_ports", outPortsBuilder.build());
        final JsonArrayBuilder controlsBuilder = Json.createArrayBuilder();
        for (ExecutorSpecification.ControlConf control : controls.values()) {
            controlsBuilder.add(control.toJson());
        }
        builder.add("controls", controlsBuilder.build());
    }

    private Path resolve(String path, String whatFile) {
        path = PathPropertyReplacement.translateProperties(path, multiChainSpecificationFile);
        final Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        if (this.multiChainSpecificationFile == null) {
            throw new IllegalStateException("The " + whatFile
                    + " is relative and cannot be resolved, because "
                    + "multi-chain JSON was not loaded from file; you must use absolute paths in this case");
        }
        return multiChainSpecificationFile.getParent().resolve(p);
    }

    private static List<String> checkChainVariantPaths(List<String> chainFileNames) {
        Objects.requireNonNull(chainFileNames, "Null array of sub-chain file names");
        chainFileNames = new ArrayList<>(chainFileNames);
        if (chainFileNames.isEmpty()) {
            throw new IllegalArgumentException("Empty array of sub-chain file names");
        }
        for (int k = 0, n = chainFileNames.size(); k < n; k++) {
            Objects.requireNonNull(chainFileNames.get(k), "Null element #" + k
                    + " in list \"" + chainFileNames + "\"");
        }
        return chainFileNames;
    }

    private static <C extends Collection<String>> C toFileNames(C result, JsonArray fileNames, String whatList) {
        assert fileNames != null;
        for (JsonValue value : fileNames) {
            if (!(value instanceof JsonString)) {
                throw new JsonException("Illegal value \"" + value + "\"in the list \""
                        + whatList + "\": it is not JSON string");
            }
            result.add(((JsonString) value).getString());
        }
        if (result.isEmpty()) {
            throw new JsonException("Empty list \"" + whatList + "\"");
        }
        return result;
    }

    private static String modifyIdForSettings(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (Exception e) {
            return id + "-settings";
        }
        uuid = new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ^ 0x73657474L);
        // - first characters of "sett"; we modify low 4 bytes of "node" element
        return uuid.toString();
    }

    public static void main(String[] args) throws IOException {
        String id = "9eb7f278-20dc-4d05-abdb-1c2a73657474";
        System.out.println(id);
        System.out.println(modifyIdForSettings(id));
        id = "some-non-uuid";
        System.out.println(id);
        System.out.println(modifyIdForSettings(id));

        MultiChainSpecification multiChainSpecification = read(Paths.get(args[0]));
        System.out.println("Multi sub-chain:");
        System.out.println(multiChainSpecification);
        System.out.println("Sub-chain paths:");
        System.out.println(multiChainSpecification.resolveChainVariantPaths());
        System.out.println("JSON:");
        System.out.println(Jsons.toPrettyString(multiChainSpecification.toJson()));
    }
}
