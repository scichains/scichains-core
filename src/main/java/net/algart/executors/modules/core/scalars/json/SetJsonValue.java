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

package net.algart.executors.modules.core.scalars.json;

import net.algart.json.Jsons;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

public final class SetJsonValue extends ScalarFilter {
    public static final String INPUT_JSON = "json";
    public static final String OUTPUT_JSON = "json";

    public enum JsonValueType {
        STRING() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, value);
            }
        },
        DOUBLE() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, Double.parseDouble(value));
            }
        },
        INTEGER() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, Long.parseLong(value));
            }
        },
        BOOLEAN() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, Boolean.parseBoolean(value));
            }
        },
        JSON_OBJECT() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, Jsons.toJson(value));
            }
        },
        JSON_ARRAY() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, Jsons.toJsonArray(value));
            }
        },
        NULL() {
            @Override
            void add(JsonObjectBuilder builder, String key, String value) {
                builder.add(key, JsonValue.NULL);
            }
        };

        abstract void add(JsonObjectBuilder builder, String key, String value);
    }

    private String key = "jsonKey";
    private String value = "value";
    private JsonValueType jsonValueType = JsonValueType.STRING;

    public SetJsonValue() {
        setDefaultInputScalar(INPUT_JSON);
        setDefaultOutputScalar(OUTPUT_JSON);
    }

    public String getKey() {
        return key;
    }

    public SetJsonValue setKey(String key) {
        this.key = nonEmpty(key);
        return this;
    }

    public String getValue() {
        return value;
    }

    public SetJsonValue setValue(String value) {
        this.value = nonNull(value);
        return this;
    }

    public JsonValueType getJsonValueType() {
        return jsonValueType;
    }

    public SetJsonValue setJsonValueType(JsonValueType jsonValueType) {
        this.jsonValueType = nonNull(jsonValueType);
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        final JsonObject json = Jsons.toJson(source.getValue(), true);
        final JsonObjectBuilder builder = Jsons.createObjectBuilder(json);
        jsonValueType.add(builder, key, value.trim());
        return SScalar.valueOf(Jsons.toPrettyString(builder.build()));
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
