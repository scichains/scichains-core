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

package net.algart.executors.api.extensions;

import jakarta.json.*;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ExtensionSpecification extends AbstractConvertibleToJson {
    private static final boolean SUPPORT_OLD_MODELS_SYNTAX = true;
    // Can be false since 4.4.11

    public static final String APP_NAME = "executors-extension";
    public static final String CURRENT_VERSION = "1.0";
    public static final String DEFAULT_EXTENSION_FILE_NAME = "extension.json";

    public static final class Platform extends AbstractConvertibleToJson {
        public static final String JVM_TECHNOLOGY = "jvm";
        public static final String DEFAULT_NAME = "Unnamed";

        private static final AtomicLong DYNAMIC_ID_INDEX = new AtomicLong(0);

        public static final class Folders extends AbstractConvertibleToJson {
            private String specifications = null;
            private String modules = null;
            private String libraries = null;
            private String resources = null;

            private Path root = null;

            private boolean immutable = false;

            public Folders() {
            }

            private Folders(JsonObject json, Path file) {
                String specifications = json.getString("specifications", null);
                if (SUPPORT_OLD_MODELS_SYNTAX && specifications == null) {
                    specifications = json.getString("models", null);
                    // - alias from the versions before 4.4.11
                }
                setSpecifications(specifications);
                setModules(json.getString("modules", null));
                setLibraries(json.getString("libraries", null));
                setResources(json.getString("resources", null));
                if (file != null) {
                    setRoot(file.getParent());
                }
            }

            /**
             * Returns name of the folder, containing the files with specifications of all executors,
             * added by this platform (see {@link ExecutorSpecification}).
             * Usually it is a subfolder of the {@link #getRoot() root folder} of the extension.
             *
             * <p>Note: this function may return <code>null</code>,
             * if this platform actually adds no new executors.</p>
             *
             * <p>Note: if this string defines a relative path, it should be resolved against
             * {@link #getRoot() root folder}; in this case, the root folder must not be <code>null</code>.</p>
             *
             * @return folder, containing the files with specifications of the executors.
             */
            public String getSpecifications() {
                return specifications;
            }

            public Folders setSpecifications(String specifications) {
                checkImmutable();
                this.specifications = specifications == null ? null : nonEmpty(specifications);
                return this;
            }

            /**
             * Returns name of the folder, containing implementations of all executors,
             * added by this platform.
             * Usually it is a subfolder of the {@link #getRoot() root folder} of the extension.
             *
             * <p>Note: this function may return <code>null</code>,
             * if this platform actually adds no new executors or if their implementation
             * are placed inside the {@link #getLibraries() libraries} folder. The latter variant
             * is possible if we don't need to analyse implementation files in some special way,
             * for example, for Java platform, when all executors are delivered via <code>.jar</code>-files.</p>
             *
             * <p>Note: if this string defines a relative path, it should be resolved against
             * {@link #getRoot() root folder}; in this case, the root folder must not be <code>null</code>.</p>
             *
             * @return folder, containing implementations of the executors.
             */
            public String getModules() {
                return modules;
            }

            public Folders setModules(String modules) {
                checkImmutable();
                this.modules = modules == null ? null : nonEmpty(modules);
                return this;
            }

            /**
             * Returns name of the folder, containing service libraries,
             * added by this platform , which can be used by executors.
             * Usually it is a subfolder of the {@link #getRoot() root folder} of the extension.
             *
             * <p>Note: this function may return <code>null</code>,
             * if this platform doesn't need any service libraries.
             *
             * <p>Note: if this string defines a relative path, it should be resolved against
             * {@link #getRoot() root folder}; in this case, the root folder must not be <code>null</code>.</p>
             *
             * @return folder, containing service libraries.
             */
            public String getLibraries() {
                return libraries;
            }

            public Folders setLibraries(String libraries) {
                checkImmutable();
                this.libraries = libraries == null ? null : nonEmpty(libraries);
                return this;
            }

            /**
             * Returns name of the folder, containing some additional files, that can be supplied together with
             * executors, added by this platform. For example, they can be datasets or models for machine
             * learning, some specific settings in XML or JSON files, demo data, etc.
             * Usually it is a subfolder of the {@link #getRoot() root folder} of the extension.
             *
             * <p>Note: this function may return <code>null</code>,
             * if this platform actually adds no resources.</p>
             *
             * <p>Note: if this string defines a relative path, it should be resolved against
             * {@link #getRoot() root folder}; in this case, the root folder must not be <code>null</code>.</p>
             *
             * @return folder, containing resource files.
             */
            public String getResources() {
                return resources;
            }

            public Folders setResources(String resources) {
                this.resources = resources;
                return this;
            }

            /**
             * Returns an absolute path to the root folder of this platform.
             *
             * <p>While creating from .json-file (<code>file</code> argument
             * of the constructor is not <code>null</code>), it is automatically set by the constructor
             * to a parent folder of this file (usually the same for all platform of this extension).
             * While creating without .json-file (for example,
             * by a constructor without arguments), it is <code>null</code> by default, but
             * may be set manually by {@link #setRoot(Path)}.</p>
             *
             * <p>Note: this folder must be specified to non-<code>null</code> value, if other sub-folders
             * define relative paths, or if you want to use {@link Platform#validClassPaths()} method.</p>
             *
             * <p>Note: this property is not included into JSON.</p>
             *
             * @return the root folder of this platform.
             */
            public Path getRoot() {
                return root;
            }

            public Folders setRoot(Path root) {
                checkImmutable();
                this.root = root == null ? null : root.toAbsolutePath();
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Folders{" +
                        "specifications='" + specifications + '\'' +
                        ", modules='" + modules + '\'' +
                        ", libraries='" + libraries + '\'' +
                        ", resources='" + resources + '\'' +
                        ", root=" + root +
                        ", immutable=" + immutable +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                if (specifications != null) {
                    builder.add("specifications", specifications);
                }
                if (modules != null) {
                    builder.add("modules", modules);
                }
                if (libraries != null) {
                    builder.add("libraries", libraries);
                }
                if (resources != null) {
                    builder.add("resources", resources);
                }
            }

            private void checkImmutable() {
                if (immutable) {
                    throw new UnsupportedOperationException(
                            "The platform folders set is immutable and cannot be changed");
                }
            }

            private Path resolve(String path, String folderNameForExceptionMessageForNullPath) {
                if (path == null) {
                    throw new IllegalStateException("Folder \"" + folderNameForExceptionMessageForNullPath
                            + "\" is not specified in this platform");
                }
                return resolve(path);
            }

            private Path resolve(String path) {
                Objects.requireNonNull(path, "Null path");
                final Path p = Paths.get(path);
                if (p.isAbsolute()) {
                    return p;
                }
                if (this.root == null) {
                    throw new IllegalStateException("The folder \"" + path
                            + "\" is relative and cannot be resolved, because "
                            + "the platform root folder is not specified; "
                            + "it is probable when an extension JSON was not loaded from a usual .json-file. "
                            + "You must use absolute paths in this case.");
                }
                return this.root.resolve(p);
            }
        }

        /**
         * Custom platform configuration, depending on technology.
         * For example, in the case of JVM it contains "classpath" and "vm_options" (JSON array).
         * Used by an external system, for example, while staring JVM from native code or from OS command script.
         */
        public static final class Configuration {
            // Does not extend AbstractConvertibleToJson, because must declare
            // toJson method instead of overriding buildJson
            private Path file;
            private JsonObject json = Jsons.newEmptyJson();
            // - Added to preserve original JSON (which can contain some additional fields)
            private List<String> classpath = Collections.emptyList();
            // - Cannot be null; if not-applicable, should be empty
            private boolean requireExistingPaths = false;
            private List<String> vmOptions = null;
            // - May be null, for example, if non-applicable to technology (for example, for Python)

            public Configuration() {
            }

            public Configuration(JsonObject json, Path file) {
                setJson(json);
                this.file = file;
                // - note: this should be AFTER setJson, which sets this.file = null
            }


            public JsonObject getJson() {
                return json;
            }

            public Configuration setJson(JsonObject json) {
                this.json = Objects.requireNonNull(json, "Null json");
                final JsonArray classPathJson = Jsons.getJsonArray(json, "classpath", file);
                this.classpath = classPathJson == null ?
                        Collections.emptyList() :
                        Jsons.toStrings(classPathJson, "classpath", file);
                this.requireExistingPaths = json.getBoolean("require_existing_paths", false);
                final JsonArray vmOptionsJson = Jsons.getJsonArray(json, "vm_options", file);
                this.vmOptions = vmOptionsJson == null ?
                        null :
                        Jsons.toStrings(vmOptionsJson, "vm_options", file);
                this.file = null;
                return this;
            }

            public void setJson(String json) {
                setJson(Jsons.toJson(json));
            }

            public List<String> getClasspath() {
                return Collections.unmodifiableList(classpath);
            }

            public Configuration setClasspath(List<String> classpath) {
                this.classpath = new ArrayList<>(classpath);
                return this;
            }

            public boolean isRequireExistingPaths() {
                return requireExistingPaths;
            }

            public Configuration setRequireExistingPaths(boolean requireExistingPaths) {
                this.requireExistingPaths = requireExistingPaths;
                return this;
            }

            public List<String> getVmOptions() {
                return Collections.unmodifiableList(vmOptions);
            }

            public Configuration setVmOptions(List<String> vmOptions) {
                this.vmOptions = vmOptions == null ? null : new ArrayList<>(vmOptions);
                return this;
            }

            public void checkCompleteness() {
                AbstractConvertibleToJson.checkNull(json, "json", getClass());
            }

            /**
             * Note: this method always returns original JSON, passed to the constructor.
             *
             * @return JSON representation of this object.
             */
            public JsonObject toJson() {
                checkCompleteness();
                return json;
            }

            @Override
            public String toString() {
                return "Configuration{" +
                        "json=<<<" + json +
                        ">>>, classpath=" + classpath +
                        ", requireExistingPaths=" + requireExistingPaths +
                        ", vmOptions=" + vmOptions +
                        '}';
            }
        }

        public static final class Dependency extends AbstractConvertibleToJson {
            private String id;
            private String name = null;
            private String description = null;

            private boolean immutable = false;


            public Dependency() {
            }

            public Dependency(JsonObject json, Path file) {
                setId(Jsons.reqString(json, "id", file));
                setName(json.getString("name", null));
                setDescription(json.getString("description", null));
            }

            public String getId() {
                return id;
            }

            public Dependency setId(String id) {
                checkImmutable();
                this.id = nonNull(id);
                return this;
            }

            public String getName() {
                return name;
            }

            public Dependency setName(String name) {
                checkImmutable();
                this.name = name;
                return this;
            }

            public String getDescription() {
                return description;
            }

            public Dependency setDescription(String description) {
                checkImmutable();
                this.description = description;
                return this;
            }

            @Override
            public void checkCompleteness() {
            }

            @Override
            public String toString() {
                return "Dependency{" +
                        "id='" + id + '\'' +
                        ", name='" + name + '\'' +
                        ", description='" + description + '\'' +
                        ", immutable=" + immutable +
                        '}';
            }

            @Override
            public void buildJson(JsonObjectBuilder builder) {
                builder.add("id", id);
                if (name != null) {
                    builder.add("name", name);
                }
                if (description != null) {
                    builder.add("description", description);
                }
            }

            private void checkImmutable() {
                if (immutable) {
                    throw new UnsupportedOperationException(
                            "The platform dependency is immutable and cannot be changed");
                }
            }

        }

        private Path file;
        private String id = makeUniqueId();
        private String category = null;
        private String name = DEFAULT_NAME;
        private String description = null;
        private Set<String> tags = new LinkedHashSet<>();
        private Set<String> technologies;
        private boolean jvmTechnology;
        private String language = null;
        private Folders folders = new Folders();
        private Configuration configuration = new Configuration();
        private List<Dependency> dependencies = new ArrayList<>();

        private boolean immutable = false;

        public Platform() {
        }

        public Platform(JsonObject json, Path file) {
            this.file = file;
            setId(json.getString("id", id));
            setCategory(json.getString("category", null));
            setName(json.getString("name", name));
            setDescription(json.getString("description", null));
            final List<String> tags = Jsons.getStrings(json, "tags", file);
            if (tags != null) {
                this.tags.addAll(tags);
            }
            List<String> technologies = Jsons.getStrings(json,"technologies", file);
            if (technologies == null) {
                final String technology = json.getString("technology", null);
                if (technology == null) {
                    throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                            + ": \"technologies\" or \"technology\" value must exist");
                }
                technologies = List.of(technology);
            }
            setTechnologies(technologies);
            setLanguage(json.getString("language", null));
            final JsonObject foldersJson = json.getJsonObject("folders");
            if (foldersJson != null) {
                setFolders(new Folders(foldersJson, file));
            } else {
                setFolders(new Folders().setRoot(file == null ? null : file.getParent()));
            }
            final JsonObject configurationJson = json.getJsonObject("configuration");
            if (configurationJson != null) {
                setConfiguration(new Configuration(configurationJson, file));
            }
            final JsonArray dependenciesJson = Jsons.getJsonArray(
                    json, "dependencies", file, true);
            if (dependenciesJson != null) {
                for (JsonValue jsonValue : dependenciesJson) {
                    this.dependencies.add(new Dependency((JsonObject) jsonValue, file));
                }
            }
        }

        public String getId() {
            return id;
        }

        public Platform setId(String id) {
            checkImmutable();
            this.id = nonNull(id);
            return this;
        }

        public String getCategory() {
            return category;
        }

        public Platform setCategory(String category) {
            this.category = category;
            return this;
        }

        public String getName() {
            return name;
        }

        public Platform setName(String name) {
            checkImmutable();
            this.name = nonNull(name);
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Platform setDescription(String description) {
            checkImmutable();
            this.description = description;
            return this;
        }

        public Set<String> getTags() {
            return Collections.unmodifiableSet(tags);
        }

        public Platform setTags(Collection<String> tags) {
            Objects.requireNonNull(tags, "Null tags");
            checkImmutable();
            this.tags = new LinkedHashSet<>(tags);
            return this;
        }

        public Set<String> getTechnologies() {
            return Collections.unmodifiableSet(technologies);
        }

        public Platform setTechnologies(Collection<String> technologies) {
            Objects.requireNonNull(technologies, "Null technologies");
            checkImmutable();
            LinkedHashSet<String> technologiesSet = new LinkedHashSet<>(technologies);
            if (technologiesSet.isEmpty()) {
                throw new IllegalArgumentException("Empty technologies");
            }
            this.technologies = technologiesSet;
            this.jvmTechnology = this.technologies.size() == 1 && this.technologies.contains(JVM_TECHNOLOGY);
            return this;
        }

        public boolean containsTechnology(String technology) {
            Objects.requireNonNull(technology, "Null technology");
            return technologies.contains(technology);
        }

        public boolean isJvmTechnology() {
            return jvmTechnology;
        }

        /**
         * Returns <code>true</code> if this platform works from very beginning, without any additional interpreters.
         *
         * @return whether the platform is built in and its executors can be called directly.
         */
        public boolean isBuiltIn() {
            return isJvmTechnology();
        }

        public String getLanguage() {
            return language;
        }

        public Platform setLanguage(String language) {
            checkImmutable();
            this.language = language;
            return this;
        }

        public Folders getFolders() {
            return folders;
        }

        public Platform setFolders(Folders folders) {
            checkImmutable();
            this.folders = nonNull(folders);
            return this;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public Platform setConfiguration(Configuration configuration) {
            checkImmutable();
            this.configuration = nonNull(configuration);
            return this;
        }

        public List<Dependency> getDependencies() {
            return Collections.unmodifiableList(dependencies);
        }

        public Platform setDependencies(List<Dependency> dependencies) {
            checkImmutable();
            this.dependencies = new ArrayList<>(nonNull(dependencies));
            return this;
        }

        public boolean hasSpecifications() {
            return folders.specifications != null;
        }

        public Path specificationsFolder() {
            return folders.resolve(folders.specifications, "specifications");
        }

        public Path specificationsFolderOrNull() {
            return hasSpecifications() ? specificationsFolder() : null;
        }

        public boolean hasModules() {
            return folders.modules != null;
        }

        public Path modulesFolder() {
            return folders.resolve(folders.modules, "modules");
        }

        public Path modulesFolderOrNull() {
            return hasModules() ? modulesFolder() : null;
        }

        public boolean hasLibraries() {
            return folders.libraries != null;
        }

        public Path librariesFolder() {
            return folders.resolve(folders.libraries, "libraries");
        }

        public Path librariesFolderOrNull() {
            return hasLibraries() ? librariesFolder() : null;
        }

        public boolean hasResources() {
            return folders.resources != null;
        }

        public Path resourcesFolder() {
            return folders.resolve(folders.resources, "resources");
        }

        public Path resourcesFolderOrNull() {
            return hasResources() ? resourcesFolder() : null;
        }

        public List<Path> validClassPaths() {
            final List<Path> result = new ArrayList<>();
            for (String singleClassPath : configuration.getClasspath()) {
                if (singleClassPath != null) {
                    // - null is possible after manual setting classpath
                    try {
                        result.add(folders.resolve(singleClassPath));
                    } catch (InvalidPathException e) {
                        // - classpath can contain usual files, but also MAY contain
                        // "strange" strings like "lib/*" (for java "-cp" parameter)
                    }
                }
            }
            return result;
        }

        /**
         * Makes this object immutable.
         *
         * <p>Note: this is <b>not thread-safe!</b> While parallel usage, it is possible
         * to modify some fields even after calling this method.</p>
         */
        public void setImmutable() {
            this.immutable = true;
            // - Note: while multithreading usage, it will not be enough to specify something "volatile"!
            // For example, thread A calls setXxx and performs "checkImmutable()",
            // then thread B calls "setImmutable()",
            // then thread A actually modifies the field.
            // But while usage from InstalledPlatformsHolder, there is no problem.
            if (this.folders != null) {
                // - "!= null" is not necessary check in the current versions (to be on the safe side)
                this.folders.immutable = true;
            }
            this.dependencies.forEach(dependency -> dependency.immutable = true);
        }

        // Note: we throw IOException instead of NoSuchFileException to provide
        // more detailed error message in ExecutionBlock.initializeExecutionSystem method
        public void checkExistingPathsIfRequired() throws IOException {
            if (!configuration.isRequireExistingPaths()) {
                return;
            }
            for (Path path : validClassPaths()) {
                if (!Files.exists(path)) {
                    throw new IOException("Invalid Java classpath" +
                            (file == null ? "" : " in " + file) +
                            ": it contains non-existing path \"" + path.toAbsolutePath() +
                            "\" in the full paths list " + configuration.getClasspath());
                }
            }
        }

        @Override
        public void checkCompleteness() {
            nonNull(technologies, "technologies");
        }

        @Override
        public String toString() {
            return "Platform{" +
                    "id='" + id + '\'' +
                    ", category='" + category + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", tags=" + tags +
                    ", technologies='" + technologies + '\'' +
                    ", jvmTechnology=" + jvmTechnology +
                    ", language='" + language + '\'' +
                    ", folders=" + folders +
                    ", configuration=" + configuration +
                    ", dependencies=" + dependencies +
                    ", immutable=" + immutable +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("id", id);
            if (category != null) {
                builder.add("category", category);
            }
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
            if (technologies == null || technologies.isEmpty()) {
                throw new AssertionError(
                        "setTechnologies/checkCompleteness must check null/empty technologies");
            }
            if (technologies.size() == 1) {
                builder.add("technology", technologies.iterator().next());
            } else {
                final JsonArrayBuilder technologiesBuilder = Json.createArrayBuilder();
                for (String technology : technologies) {
                    technologiesBuilder.add(technology);
                }
                builder.add("technologies", technologiesBuilder.build());
            }
            if (language != null) {
                builder.add("language", language);
            }
            builder.add("folders", folders.toJson());
            builder.add("configuration", configuration.toJson());
            if (!dependencies.isEmpty()) {
                final JsonArrayBuilder dependenciesBuilder = Json.createArrayBuilder();
                for (Dependency dependency : dependencies) {
                    dependenciesBuilder.add(dependency.toJson());
                }
                builder.add("dependencies", dependenciesBuilder.build());
            }
        }

        private static String makeUniqueId() {
            return "_dynamic_id_" + DYNAMIC_ID_INDEX.incrementAndGet() + "--006b06cc-70b4-4245-8392-18a580af19ef";
        }

        private void checkImmutable() {
            if (immutable) {
                throw new UnsupportedOperationException("This platform is immutable and cannot be changed");
            }
        }
    }

    private final Path extensionSpecificationFile;
    private String version = CURRENT_VERSION;
    private List<Platform> platforms = new ArrayList<>();

    public ExtensionSpecification() {
        this.extensionSpecificationFile = null;
    }

    private ExtensionSpecification(JsonObject json, Path file) {
        Objects.requireNonNull(json, "Null extension JSON");
        if (!APP_NAME.equals(json.getString("app", null))) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not an executor configuration: no \"app\":\"" + APP_NAME + "\" element");
        }
        this.extensionSpecificationFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        for (JsonObject platformJson : Jsons.reqJsonObjects(json, "platforms")) {
            this.platforms.add(new Platform(platformJson, file));
        }
    }

    public static ExtensionSpecification of(JsonObject extensionSpecification) {
        return new ExtensionSpecification(extensionSpecification, null);
    }

    public static ExtensionSpecification read(Path extensionSpecificationFile) throws IOException {
        Objects.requireNonNull(extensionSpecificationFile, "Null extensionSpecificationFile");
        final JsonObject json = Jsons.readJson(extensionSpecificationFile);
        return new ExtensionSpecification(json, extensionSpecificationFile);
    }

    public static ExtensionSpecification readIfValid(Path extensionSpecificationFile) throws IOException {
        Objects.requireNonNull(extensionSpecificationFile, "Null extensionSpecificationFile");
        final JsonObject json = Jsons.readJson(extensionSpecificationFile);
        Objects.requireNonNull(json, "Null extension JSON");
        if (!APP_NAME.equals(json.getString("app", null))) {
            return null;
        }
        return new ExtensionSpecification(json, extensionSpecificationFile);
    }

    public static ExtensionSpecification readFromFolder(Path extensionFolder) throws IOException {
        Objects.requireNonNull(extensionFolder, "Null extensionFolder");
        if (!Files.isDirectory(extensionFolder)) {
            throw new FileNotFoundException("Extension folder \"" + extensionFolder
                    + "\" is not an existing directory");
        }
        final Path extensionSpecificationFile = defaultExtensionSpecificationFile(extensionFolder);
        if (!Files.exists(extensionSpecificationFile)) {
            throw new FileNotFoundException("Extension specification file \"" + extensionSpecificationFile
                    + "\" does not exist in " + extensionFolder);
        }
        return read(extensionSpecificationFile);
    }

    public void write(Path extensionSpecificationFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(extensionSpecificationFile, "Null extensionSpecificationFile");
        Files.writeString(extensionSpecificationFile, jsonString(), options);
    }

    public static Path defaultExtensionSpecificationFile(Path extensionFolder) {
        Objects.requireNonNull(extensionFolder, "Null extensionFolder");
        return extensionFolder.resolve(DEFAULT_EXTENSION_FILE_NAME).toAbsolutePath();
    }

    public static boolean isExtensionSpecification(JsonObject extensionSpecification) {
        Objects.requireNonNull(extensionSpecification, "Null extensionSpecification");
        return APP_NAME.equals(extensionSpecification.getString("app", null));
    }

    public static boolean isExtensionFolder(Path extensionFolder) {
        return Files.isDirectory(extensionFolder)
                && Files.exists(defaultExtensionSpecificationFile(extensionFolder));
    }

    public static List<Path> allExtensionFolders(Path extensionRoot) throws IOException {
        Objects.requireNonNull(extensionRoot, "Null extensionRoot");
        try (Stream<Path> walk = Files.walk(extensionRoot)) {
            return walk.sorted().filter(ExtensionSpecification::isExtensionFolder).toList();
        }
    }

    public static <T> List<T> readAllIfValid(
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
        if (result == null) {
            result = new ArrayList<>();
        }
        if (Files.isDirectory(containingJsonPath)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(containingJsonPath)) {
                for (Path file : files) {
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

    public Path getExtensionSpecificationFile() {
        return extensionSpecificationFile;
    }

    public String getVersion() {
        return version;
    }

    public ExtensionSpecification setVersion(String version) {
        this.version = Objects.requireNonNull(version, "Null version");
        return this;
    }

    public List<Platform> getPlatforms() {
        return Collections.unmodifiableList(platforms);
    }

    public ExtensionSpecification setPlatforms(List<Platform> platforms) {
        this.platforms = new ArrayList<>(nonNull(platforms));
        return this;
    }

    @Override
    public void checkCompleteness() {
        checkNull(platforms, "platforms");
    }

    @Override
    public String toString() {
        return "ExtensionSpecification{" +
                "extensionSpecificationFile=" + extensionSpecificationFile +
                ", version='" + version + '\'' +
                ", platforms=" + platforms +
                '}';
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", APP_NAME);
        builder.add("version", version);
        final JsonArrayBuilder platformsBuilder = Json.createArrayBuilder();
        for (Platform platform : platforms) {
            platformsBuilder.add(platform.toJson());
        }
        builder.add("platforms", platformsBuilder.build());
    }
}
