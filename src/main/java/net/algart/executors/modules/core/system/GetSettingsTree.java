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

package net.algart.executors.modules.core.system;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.system.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GetSettingsTree extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SETTINGS_SPECIFICATION = "settings_specification";
    public static final String OUTPUT_CATEGORY = "category";
    public static final String OUTPUT_NAME = "name";
    public static final String OUTPUT_DESCRIPTION = "description";
    public static final String OUTPUT_ID = "id";
    public static final String OUTPUT_COMPLETE = "complete";

    private static final List<String> ALL_OUTPUT_PORTS = List.of(
            DEFAULT_OUTPUT_PORT,
            OUTPUT_SETTINGS_SPECIFICATION,
            OUTPUT_CATEGORY,
            OUTPUT_NAME,
            OUTPUT_DESCRIPTION,
            OUTPUT_ID,
            OUTPUT_COMPLETE);

    private String id = "n/a";
    private boolean buildTree = true;
    private boolean smartSearch = true;
    private ExecutorSpecification.JsonMode jsonMode = ExecutorSpecification.JsonMode.MEDIUM;
    private System.Logger.Level logLevel = System.Logger.Level.DEBUG;

    private volatile ExecutorFactory factory = null;
    private volatile SmartSearchSettings smartSearchSettings = null;

    public GetSettingsTree() {
        addInputScalar(GetExecutorSpecification.INPUT_EXECUTOR_ID);
        ALL_OUTPUT_PORTS.forEach(this::addOutputScalar);
    }

    public String getId() {
        return id;
    }

    public GetSettingsTree setId(String id) {
        this.id = nonNull(id);
        return this;
    }

    public boolean isBuildTree() {
        return buildTree;
    }

    public GetSettingsTree setBuildTree(boolean buildTree) {
        this.buildTree = buildTree;
        return this;
    }

    public boolean isSmartSearch() {
        return smartSearch;
    }

    public GetSettingsTree setSmartSearch(boolean smartSearch) {
        this.smartSearch = smartSearch;
        return this;
    }

    public ExecutorSpecification.JsonMode getJsonMode() {
        return jsonMode;
    }

    public GetSettingsTree setJsonMode(ExecutorSpecification.JsonMode jsonMode) {
        this.jsonMode = nonNull(jsonMode);
        return this;
    }

    public System.Logger.Level getLogLevel() {
        return logLevel;
    }

    public GetSettingsTree setLogLevel(System.Logger.Level logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    @Override
    public void process() {
        ALL_OUTPUT_PORTS.forEach(s -> getScalar(s).remove());
        getScalar(OUTPUT_COMPLETE).setTo(false);
        String id = getInputScalar(GetExecutorSpecification.INPUT_EXECUTOR_ID, true).getValue();
        if (id == null) {
            id = this.id;
        }
        SettingsTree tree = buildSettingsTree(id);
        if (tree == null) {
            // - result remains empty
            return;
        }
        final ExecutorSpecification specification = tree.specification();
        if (specification.hasSettings()) {
            getScalar(OUTPUT_SETTINGS_SPECIFICATION).setTo(specification.getSettings().jsonString());
        }
        getScalar(OUTPUT_CATEGORY).setTo(specification.getCategory());
        getScalar(OUTPUT_NAME).setTo(specification.getName());
        getScalar(OUTPUT_DESCRIPTION).setTo(specification.getDescription());
        getScalar(OUTPUT_ID).setTo(specification.getId());
    }

    public SettingsTree buildSettingsTree(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        long t1 = debugTime();
        if (factory == null || smartSearchSettings == null) {
            final ExecutorLoaderSet globalLoaders = globalLoaders();
            final String sessionId = getSessionId();
            factory = globalLoaders.newFactory(sessionId);
            smartSearchSettings = SmartSearchSettings.of(factory, globalLoaders, sessionId);
        }
        smartSearchSettings.setLogLevel(logLevel);
        // - must be called always, not only once: the user may change this level
        final ExecutorSpecification specification = factory.getSpecification(executorId);
        if (specification == null) {
            return null;
        }
        long t2 = debugTime(), t3;
        final String jsonTree;
        final SettingsTree tree = smartSearch ?
                SettingsTree.of(smartSearchSettings, specification) :
                SettingsTree.of(factory, specification);
        // - actually, we build the tree always: this is a quick operation
        t3 = debugTime();
        if (buildTree) {
            getScalar(OUTPUT_COMPLETE).setTo(tree.isComplete());
            jsonTree = tree.jsonString(jsonMode);
        } else {
            jsonTree = specification.jsonString(jsonMode);
        }
        long t4 = debugTime();
        getScalar().setTo(jsonTree);
        logDebug(() -> String.format(Locale.US,
                "Settings \"%s\": %.5f ms = " +
                        "%.3f mcs requesting description + %.3f mcs building tree " +
                        "+ %.3f mcs building JSON",
                executorId,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
        return tree;
    }
}
