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

package net.algart.executors.modules.core.logic.compiler.mappings;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.DefaultExecutorLoader;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.logic.compiler.mappings.interpreters.InterpretMapping;
import net.algart.executors.modules.core.logic.compiler.mappings.model.Mapping;
import net.algart.executors.modules.core.logic.compiler.mappings.model.MappingSpecification;
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

    private static final DefaultExecutorLoader<Mapping> MAPPING_LOADER =
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

    public static DefaultExecutorLoader<Mapping> mappingLoader() {
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

    public void useSeveralPaths(List<Path> settingsCombinerSpecificationPaths) throws IOException {
        Objects.requireNonNull(settingsCombinerSpecificationPaths, "Null settings combiner paths");
        StringBuilder sb = isOutputNecessary(DEFAULT_OUTPUT_PORT) ? new StringBuilder() : null;
        for (Path path : settingsCombinerSpecificationPaths) {
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
            logDebug("Loading settings combiner " + (n > 1 ? (i + 1) + "/" + n + " " : "")
                    + "from " + mappingSpecification.getMappingSpecificationFile().toAbsolutePath() + "...");
            use(mappingSpecification);
            if (report != null) {
                report.append(mappingSpecification.getMappingSpecificationFile()).append("\n");
            }
        }
    }

    public void useContent(String mappingJsonContent) throws IOException {
        final MappingSpecification mappingSpecification =
                MappingSpecification.valueOf(Jsons.toJson(mappingJsonContent), false);
        // - we don't require strict accuracy for JSON, entered in a little text area
        logDebug("Using mapping '" + mappingSpecification.getName() + "' from the text argument...");
        use(mappingSpecification);
        if (isOutputNecessary(DEFAULT_OUTPUT_PORT)) {
            getScalar().setTo(mappingSpecification.jsonString());
        }
    }

    public void use(MappingSpecification mappingSpecification) throws IOException {
        mappingSpecification.updateAutogeneratedCategory(true);
        final String sessionId = getSessionId();
        final SScalar.MultiLineOrJsonSplitter keys = keys(mappingSpecification);
        final SScalar.MultiLineOrJsonSplitter items = enumItems(mappingSpecification);
        final Mapping mapping = Mapping.valueOf(
                mappingSpecification,
                keys.lines(),
                keys.comments(),
                items == null ? null : items.lines(),
                items == null ? null : items.comments());
        final ExecutorSpecification specification = buildMappingSpecification(mapping);
        MAPPING_LOADER.registerWorker(sessionId, specification, mapping);
    }

    public ExecutorSpecification buildMappingSpecification(Mapping mapping) {
        Objects.requireNonNull(mapping, "Null mapping");
        ExecutorSpecification result = new ExecutorSpecification();
        result.setTo(new InterpretMapping());
        // - adds JavaConf, (maybe) parameters and some ports
        result.setSourceInfo(mapping.mappingSpecificationFile(), null);
        result.setLanguage(MAPPING_LANGUAGE);
        result.setExecutorId(mapping.id());
        result.setCategory(CATEGORY_PREFIX + mapping.category());
        result.setName(mapping.name());
        result.setDescription(mapping.description());
        result.createOptionsIfAbsent().createRoleIfAbsent()
                .setName(mapping.name())
                .setSettings(true)
                .setResultPort(InterpretMapping.OUTPUT_MAPPING);
        addInputControls(result, mapping);
        return result;
    }

    private void addInputControls(ExecutorSpecification result, Mapping mapping) {
        final List<String> enumItems = mapping.enumItems();
        final List<String> enumItemCaptions = mapping.enumItemCaptions();
        for (int i = 0, n = mapping.numberOfKeys(); i < n; i++) {
            String key = mapping.key(i);
            ExecutorSpecification.ControlConf controlConf = mapping.specification().buildControlConf(
                    key, enumItems, enumItemCaptions, advancedParameters);
            controlConf.setCaption(mapping.keyCaption(i));
            controlConf.setHint("\"" + controlConf.getName() + "\" key in the result JSON");
            result.addControl(controlConf);
        }
    }

    private SScalar.MultiLineOrJsonSplitter keys(MappingSpecification specification) throws IOException {
        if (specification.hasKeys()) {
            return SScalar.MultiLineOrJsonSplitter.valueOfCommentedLines(specification.getKeys().toArray(new String[0]));
        }
        Path file = specification.keysFile();
        if (file == null) {
            file = requireFile(specification, customKeysOrEnumItemsFile(this.mappingKeysFile),
                    "keys", "Keys file");
        }
        return SScalar.splitJsonOrTrimmedLinesWithComments(Mapping.readNames(file));
    }

    private SScalar.MultiLineOrJsonSplitter enumItems(MappingSpecification specification) throws IOException {
        if (!specification.isEnum()) {
            return null;
        }
        if (specification.hasEnumItems()) {
            return SScalar.MultiLineOrJsonSplitter.valueOfCommentedLines(
                    specification.getEnumItems().toArray(new String[0]));
        }
        Path file = specification.enumItemsFile();
        if (file == null) {
            file = requireFile(specification, customKeysOrEnumItemsFile(this.mappingEnumItemsFile),
                    "enum items", "Enum items file");
        }
        return SScalar.splitJsonOrTrimmedLinesWithComments(Mapping.readNames(file));
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
            throw new IllegalStateException("Mapping specification \"" + specification.getName() + "\""
                    + (specification.getMappingSpecificationFile() == null ?
                    "" :
                    ", loaded from " + specification.getMappingSpecificationFile() + ",")
                    + " has no " + whatFile + " file; in this case the parameter \"" + whatParameter
                    + "\" must contain a correct existing file" + (file == null ? "" : " " + file.toAbsolutePath()));
        }
        return file;
    }
}
