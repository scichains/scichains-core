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

import jakarta.json.JsonObject;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.json.Jsons;

import java.util.Locale;

public class InterpretMapping extends MappingExecutor implements ReadOnlyExecutionInput {
    public static final String MAPPING = MappingSpecification.MAPPING;
    public static final String OUTPUT_KEYS = "keys";

    public InterpretMapping() {
        setDefaultOutputScalar(MAPPING);
        addOutputScalar(OUTPUT_KEYS);
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        long t1 = debugTime();
        final MappingBuilder mappingBuilder = mappingBuilder();
        final JsonObject mappingJson = mappingBuilder.build(this);
        final String mappingString = Jsons.toPrettyString(mappingJson);
        getScalar(MAPPING).setTo(mappingString);
        getScalar(OUTPUT_KEYS).setTo(String.join("\n", mappingJson.keySet()));
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Building mapping \"%s\": %.3f ms%s",
                mappingBuilder.name(),
                (t2 - t1) * 1e-6,
                LOGGABLE_TRACE ?
                        "\n" + mappingString
                        : ""));
    }

    @Override
    public String toString() {
        return "Executor of " + (mappingBuilder != null ? mappingBuilder : "some non-initialized mapping");
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }
}
