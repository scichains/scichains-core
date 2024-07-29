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

package net.algart.executors.modules.core.logic.compiler.settings.model;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import net.algart.json.Jsons;

import java.util.Map;

public enum SubSettingsInheritanceMode {
    NONE() {
        @Override
        public JsonObject inherit(JsonObject parentSettings, JsonObject subSettings) {
            return subSettings;
        }
    },
    ADD_NEW_TO_PARENT() {
        @Override
        public JsonObject inherit(JsonObject parentSettings, JsonObject subSettings) {
            return Jsons.addNonExistingEntries(extractSimpleValues(parentSettings), subSettings);
        }
    },
    OVERRIDE_PARENT() {
        @Override
        public JsonObject inherit(JsonObject parentSettings, JsonObject subSettings) {
            return Jsons.overrideEntries(extractSimpleValues(parentSettings), subSettings);
        }
    },
    OVERRIDE_BY_EXISTING_IN_PARENT() {
        @Override
        public JsonObject inherit(JsonObject parentSettings, JsonObject subSettings) {
            return Jsons.overrideOnlyExistingInBoth(subSettings, extractSimpleValues(parentSettings));
        }
    };

    public abstract JsonObject inherit(JsonObject parentSettings, JsonObject subSettings);

    private static JsonObject extractSimpleValues(JsonObject parent) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : parent.entrySet()) {
            final JsonValue value = entry.getValue();
            switch (value.getValueType()) {
                case NUMBER:
                case STRING:
                case FALSE:
                case TRUE:
                case NULL: {
                    builder.add(entry.getKey(), value);
                }
            }
        }
        return builder.build();
    }
}
