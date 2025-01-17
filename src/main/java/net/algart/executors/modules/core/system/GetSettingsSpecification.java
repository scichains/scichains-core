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
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.system.ExecutorFactory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GetSettingsSpecification extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_CATEGORY = "category";
    public static final String OUTPUT_NAME = "name";
    public static final String OUTPUT_DESCRIPTION = "description";
    public static final String OUTPUT_ID = "id";

    private static final List<String> ALL_OUTPUT_PORTS = List.of(
            DEFAULT_OUTPUT_PORT,
            OUTPUT_CATEGORY,
            OUTPUT_NAME,
            OUTPUT_DESCRIPTION,
            OUTPUT_ID);

    private String id = "n/a";
    private boolean buildTree = true;

    public GetSettingsSpecification() {
        addInputScalar(GetExecutorSpecification.INPUT_EXECUTOR_ID);
        ALL_OUTPUT_PORTS.forEach(this::addOutputScalar);
    }

    public String getId() {
        return id;
    }

    public GetSettingsSpecification setId(String id) {
        this.id = nonNull(id);
        return this;
    }

    public boolean isBuildTree() {
        return buildTree;
    }

    public GetSettingsSpecification setBuildTree(boolean buildTree) {
        this.buildTree = buildTree;
        return this;
    }

    @Override
    public void process() {
        ALL_OUTPUT_PORTS.forEach(s -> getScalar(s).remove());
        String id = getInputScalar(GetExecutorSpecification.INPUT_EXECUTOR_ID, true).getValue();
        if (id == null) {
            id = this.id;
        }
        final SettingsSpecification specification = findSpecification(id);
        if (specification == null) {
            getScalar().setTo("No settings with ID \"" + id + "\"");
            return;
        }
        getScalar(OUTPUT_CATEGORY).setTo(specification.getCategory());
        getScalar(OUTPUT_NAME).setTo(specification.getName());
        getScalar(OUTPUT_DESCRIPTION).setTo(specification.getCombineDescription());
        getScalar(OUTPUT_ID).setTo(specification.getId());
    }

    public SettingsSpecification findSpecification(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        long t1 = debugTime();
        final ExecutorFactory factory = globalLoaders().newFactory(getSessionId());
        final var specification = factory.getSettingsSpecification(executorId);
        if (specification == null) {
            return null;
        }
        long t2 = debugTime();
        final String result;
        if (buildTree) {
            // TODO!! find children IDs (should be new specification method)
            result = specification.jsonTreeString(factory);
        } else {
            result = specification.jsonString();
        }
        long t3 = debugTime();
        getScalar().setTo(result);
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Settings \"%s\": %.5f ms = " +
                        "%.3f mcs requesting description + %.3f mcs building JSON + %.3f mcs returning",
                executorId,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
        return specification;
    }
}
