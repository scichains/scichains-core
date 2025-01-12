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

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.api.system.ExecutorSpecificationSet;
import net.algart.json.Jsons;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InstalledExecutorSpecification extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_PLATFORM_ID = "platform_id";
    public static final String OUTPUT_CATEGORY = "category";
    public static final String OUTPUT_NAME = "name";
    public static final String OUTPUT_DESCRIPTION = "description";
    public static final String OUTPUT_ID = "id";
    public static final String OUTPUT_LANGUAGE = "language";
    public static final String OUTPUT_BUILT_IN = "built_in";

    private static final List<String> ALL_OUTPUT_PORTS = List.of(
            DEFAULT_OUTPUT_PORT,
            OUTPUT_PLATFORM_ID,
            OUTPUT_CATEGORY,
            OUTPUT_NAME,
            OUTPUT_DESCRIPTION,
            OUTPUT_ID,
            OUTPUT_LANGUAGE,
            OUTPUT_BUILT_IN);

    private String id = "n/a";
    private boolean specialSearchInBuiltIn = true;

    public InstalledExecutorSpecification() {
        ALL_OUTPUT_PORTS.forEach(this::addOutputScalar);
    }

    public String getId() {
        return id;
    }

    public InstalledExecutorSpecification setId(String id) {
        this.id = nonEmpty(id);
        return this;
    }

    public boolean isSpecialSearchInBuiltIn() {
        return specialSearchInBuiltIn;
    }

    public InstalledExecutorSpecification setSpecialSearchInBuiltIn(boolean specialSearchInBuiltIn) {
        this.specialSearchInBuiltIn = specialSearchInBuiltIn;
        return this;
    }

    @Override
    public void process() {
        ALL_OUTPUT_PORTS.forEach(s -> getScalar(s).remove());
        final ExecutorSpecification specification = findSpecification(id);
        if (specification == null) {
            getScalar().setTo("No executor with ID \"" + id + "\"");
            return;
        }
        getScalar(OUTPUT_PLATFORM_ID).setTo(specification.getPlatformId());
        getScalar(OUTPUT_CATEGORY).setTo(specification.getCategory());
        getScalar(OUTPUT_NAME).setTo(specification.getName());
        getScalar(OUTPUT_DESCRIPTION).setTo(specification.getDescription());
        getScalar(OUTPUT_ID).setTo(specification.getExecutorId());
        getScalar(OUTPUT_LANGUAGE).setTo(specification.getLanguage());
    }

    public ExecutorSpecification findSpecification(String executorId) {
        Objects.requireNonNull(executorId, "Null executorId");
        long t1 = debugTime();
        final ExecutorSpecification builtIn = ExecutorSpecificationSet.allBuiltIn().get(executorId);
        getScalar(OUTPUT_BUILT_IN).setTo(builtIn != null);
        long t2 = debugTime();
        if (builtIn != null && specialSearchInBuiltIn) {
            final JsonObject json = builtIn.toJson();
            long t3 = debugTime();
            getScalar().setTo(Jsons.toPrettyString(json));
            long t4 = debugTime();
            logDebug(() -> String.format(Locale.US,
                    "Executor \"%s\" is a built-in executor \"%s\": %.5f ms = " +
                            "%.3f mcs quick check + %.3f mcs making JSON + %.3f mcs returning",
                    executorId, builtIn.getName(),
                    (t4 - t1) * 1e-6, (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
            return builtIn;
        }
        final ExecutorSpecification extended = globalLoaders().getSpecification(
                getSessionId(), executorId, true);
        if (extended == null) {
            return null;
        }
        long t3 = debugTime();
        getScalar().setTo(extended.jsonString());
        // - note: this JSON can have more information than ExecutorSpecification extended
        // (for example, it has additional fields for PythonCallerSpecification)
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Executor \"%s\" is an extended executor \"%s\": %.5f ms = " +
                        "%.3f mcs quick check + %.3f mcs requesting description + %.3f mcs returning",
                executorId, extended.getName(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
        return extended;
    }
}
