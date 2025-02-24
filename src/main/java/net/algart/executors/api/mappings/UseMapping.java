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

import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.CreateMode;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UseMapping extends FileOperation {
    public static final String MAPPING_LANGUAGE = "mapping";
    public static final String CATEGORY_PREFIX = "$";

    private static final DefaultExecutorLoader<MappingBuilder> MAPPING_LOADER =
            new DefaultExecutorLoader<>("mappings loader");

    static {
        globalLoaders().register(MAPPING_LOADER);
    }

    private String mappingKeysFile = null;
    private String mappingEnumItemsFile = null;
    private String mappingJsonContent = "";
    private boolean advancedParameters = false;

    public UseMapping() {
        setDefaultOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public static UseMapping getInstance() {
        return new UseMapping();
    }

    public static UseMapping getSharedInstance() {
        return setShared(new UseMapping());
    }

    public static DefaultExecutorLoader<MappingBuilder> mappingLoader() {
        return MAPPING_LOADER;
    }

    public String getMappingKeysFile() {
        return mappingKeysFile;
    }

    public UseMapping setMappingKeysFile(String mappingKeysFile) {
        this.mappingKeysFile = mappingKeysFile;
        return this;
    }

    public String getMappingEnumItemsFile() {
        return mappingEnumItemsFile;
    }

    public UseMapping setMappingEnumItemsFile(String mappingEnumItemsFile) {
        this.mappingEnumItemsFile = mappingEnumItemsFile;
        return this;
    }

    public String getMappingJsonContent() {
        return mappingJsonContent;
    }

    public UseMapping setMappingJsonContent(String mappingJsonContent) {
        this.mappingJsonContent = nonNull(mappingJsonContent);
        return this;
    }

    public boolean isAdvancedParameters() {
        return advancedParameters;
    }

    public UseMapping setAdvancedParameters(boolean advancedParameters) {
        this.advancedParameters = advancedParameters;
        return this;
    }

    @Override
    public UseMapping setFile(String file) {
        super.setFile(file);
        return this;
    }

    public static MappingExecutor newSharedExecutor(ExecutorFactory factory, Path file) throws IOException {
        return newSharedExecutor(factory, MappingSpecification.read(file));
    }

    public static MappingExecutor newSharedExecutor(ExecutorFactory factory, MappingSpecification specification)
            throws IOException {
        return getSharedInstance().newExecutor(factory, specification);
    }

    public MappingExecutor newExecutor(ExecutorFactory factory, Path file) throws IOException {
        Objects.requireNonNull(factory, "Null executor factory");
        return newExecutor(factory, MappingSpecification.read(file));
    }

    public MappingExecutor newExecutor(ExecutorFactory factory, MappingSpecification specification)
            throws IOException {
        Objects.requireNonNull(factory, "Null executor factory");
        return factory.newExecutor(MappingExecutor.class, use(specification).id(), CreateMode.NORMAL);
    }

    @Override
    public void process() {
        try {
            if (!this.getFile().trim().isEmpty()) {
                useSeveralPaths(completeSeveralFilePaths());
                return;
            }
            final String mappingJsonContent = this.mappingJsonContent.trim();
            if (!mappingJsonContent.isEmpty()) {
                useContent(mappingJsonContent);
                return;
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
        throw new IllegalArgumentException("One of arguments \"Mapping JSON file/folder\" "
                + "or \"Mapping JSON content\" must be non-empty");
    }

    public void useSeveralPaths(List<Path> mappingSpecificationsPaths) throws IOException {
        Objects.requireNonNull(mappingSpecificationsPaths, "Null mapping paths");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : mappingSpecificationsPaths) {
            usePath(path, sb);
        }
        if (sb != null) {
            getScalar().setTo(sb.toString());
        }
    }

    public void usePath(Path mappingSpecificationPath, StringBuilder report) throws IOException {
        Objects.requireNonNull(mappingSpecificationPath, "Null mapping path");
        final List<MappingSpecification> mappingSpecifications;
        if (Files.isDirectory(mappingSpecificationPath)) {
            mappingSpecifications = MappingSpecification.readAllIfValid(mappingSpecificationPath);
        } else {
            mappingSpecifications = Collections.singletonList(MappingSpecification.read(mappingSpecificationPath));
            // Note: for a single file, we REQUIRE that it must be a correct JSON
        }
        MappingSpecification.checkIdDifference(mappingSpecifications);
        for (int i = 0, n = mappingSpecifications.size(); i < n; i++) {
            final MappingSpecification mappingSpecification = mappingSpecifications.get(i);
            logDebug("Loading settings " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + mappingSpecification.getSpecificationFile().toAbsolutePath() + "...");
            use(mappingSpecification);
            if (report != null) {
                report.append(mappingSpecification.getSpecificationFile()).append("\n");
            }
        }
    }

    public void useContent(String mappingJsonContent) throws IOException {
        final MappingSpecification mappingSpecification =
                MappingSpecification.of(Jsons.toJson(mappingJsonContent), false);
        // - we don't require strict accuracy for JSON, entered in a little text area
        logDebug("Using mapping '" + mappingSpecification.getName() + "' from the text argument...");
        use(mappingSpecification);
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo(mappingSpecification.jsonString());
        }
    }

    public MappingBuilder use(MappingSpecification mappingSpecification) throws IOException {
        mappingSpecification.updateAutogeneratedCategory(false);
        final String sessionId = getSessionId();
        final MappingBuilder mappingBuilder = mappingBuilder(mappingSpecification);
        final ExecutorSpecification specification = buildMappingSpecification(mappingBuilder);
        MAPPING_LOADER.registerWorker(sessionId, specification, mappingBuilder);
        return mappingBuilder;
    }

    public MappingBuilder mappingBuilder(MappingSpecification mappingSpecification) throws IOException {
        return mappingBuilder(mappingSpecification, true);
    }

    MappingBuilder mappingBuilder(MappingSpecification mappingSpecification, boolean calledWithObject) throws IOException {
        final SScalar.MultiLineOrJsonSplitter keys = keys(mappingSpecification, calledWithObject);
        final SScalar.MultiLineOrJsonSplitter items = enumItems(mappingSpecification, calledWithObject);
        return MappingBuilder.of(
                mappingSpecification,
                keys.lines(),
                keys.comments(),
                items == null ? null : items.lines(),
                items == null ? null : items.comments());
    }

    public ExecutorSpecification buildMappingSpecification(MappingBuilder mappingBuilder) {
        Objects.requireNonNull(mappingBuilder, "Null mapping");
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(new MappingExecutor());
        // - adds JavaConf, (maybe) parameters and some ports
        result.setSourceInfo(mappingBuilder.specificationFile(), null);
        result.setLanguage(MAPPING_LANGUAGE);
        result.setId(mappingBuilder.id());
        result.setCategory(CATEGORY_PREFIX + mappingBuilder.category());
        result.setName(mappingBuilder.name());
        result.setDescription(mappingBuilder.description());
        result.createOptionsIfAbsent().createRoleIfAbsent()
                .setClassName(mappingBuilder.className())
                .setSettings(true)
                .setResultPort(MappingExecutor.MAPPING);
        addInputControls(result, mappingBuilder);
        return result;
    }

    private void addInputControls(ExecutorSpecification result, MappingBuilder mappingBuilder) {
        final List<String> enumItems = mappingBuilder.enumItems();
        final List<String> enumItemCaptions = mappingBuilder.enumItemCaptions();
        for (int i = 0, n = mappingBuilder.numberOfKeys(); i < n; i++) {
            String key = mappingBuilder.key(i);
            ExecutorSpecification.ControlConf controlConf = mappingBuilder.specification().buildControlConf(
                    key, enumItems, enumItemCaptions, advancedParameters);
            controlConf.setCaption(mappingBuilder.keyCaption(i));
            controlConf.setHint("\"" + controlConf.getName() + "\" key in the result JSON");
            result.addControl(controlConf);
        }
    }

    private SScalar.MultiLineOrJsonSplitter keys(MappingSpecification specification, boolean calledWithObject)
            throws IOException {
        if (specification.hasKeys()) {
            return SScalar.MultiLineOrJsonSplitter.ofCommentedLines(specification.getKeys().toArray(new String[0]));
        }
        Path file = specification.keysFile();
        if (file == null) {
            file = requireFile(specification, customKeysOrEnumItemsFile(this.mappingKeysFile),
                    "keys", calledWithObject ? "Keys file" : null);
        }
        return SScalar.splitJsonOrTrimmedLinesWithComments(MappingBuilder.readNames(file));
    }

    private SScalar.MultiLineOrJsonSplitter enumItems(MappingSpecification specification, boolean calledWithObject)
            throws IOException {
        if (!specification.isEnum()) {
            return null;
        }
        if (specification.hasEnumItems()) {
            return SScalar.MultiLineOrJsonSplitter.ofCommentedLines(
                    specification.getEnumItems().toArray(new String[0]));
        }
        Path file = specification.enumItemsFile();
        if (file == null) {
            file = requireFile(specification, customKeysOrEnumItemsFile(this.mappingEnumItemsFile),
                    "enum items", calledWithObject ? "Enum items file" : null);
        }
        return SScalar.splitJsonOrTrimmedLinesWithComments(MappingBuilder.readNames(file));
    }

    private Path customKeysOrEnumItemsFile(String file) {
        return file == null || (file = file.trim()).isEmpty() ?
                null :
                completeFilePath(file, false);
    }

    private static Path requireFile(
            MappingSpecification specification,
            Path file,
            String whatFile,
            String whatParameter) {
        if (file == null || !Files.exists(file)) {
            throw new IllegalStateException("Mapping specification \"" + specification.getName() + "\"" +
                    (specification.getSpecificationFile() == null ?
                            "" :
                            ", loaded from " + specification.getSpecificationFile() + ",") +
                    " has no " + whatFile + " file" +
                    (whatParameter == null ? "" :
                            "; in this case the parameter \"" + whatParameter +
                                    "\" must contain a correct existing file" + (file == null ? "" :
                                    " " + file.toAbsolutePath())));
        }
        return file;
    }
}
