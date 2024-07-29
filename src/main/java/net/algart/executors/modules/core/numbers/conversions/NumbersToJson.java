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

package net.algart.executors.modules.core.numbers.conversions;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.json.Jsons;

public final class NumbersToJson extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_KEYS = "keys";
    public static final String OUTPUT_JSON = "json";

    public enum JsonValueType {
        DOUBLE() {
            @Override
            void add(JsonObjectBuilder builder, String key, double value) {
                builder.add(key, value);
            }
        },
        INTEGER() {
            @Override
            void add(JsonObjectBuilder builder, String key, double value) {
                builder.add(key, Math.round(value));
            }
        },
        BOOLEAN() {
            @Override
            void add(JsonObjectBuilder builder, String key, double value) {
                builder.add(key, value != 0.0);
            }
        },
        STRING() {
            @Override
            void add(JsonObjectBuilder builder, String key, double value) {
                final long longValue = (long) value;
                builder.add(key, value == longValue ? String.valueOf(longValue) : String.valueOf(value));
            }
        };

        abstract void add(JsonObjectBuilder builder, String key, double value);
    }

    private JsonValueType jsonValueType = JsonValueType.DOUBLE;
    private double startValue = 1.0;
    private double increment = 1.0;

    public NumbersToJson() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addInputScalar(INPUT_KEYS);
        setDefaultOutputScalar(OUTPUT_JSON);
    }

    public JsonValueType getJsonValueType() {
        return jsonValueType;
    }

    public NumbersToJson setJsonValueType(JsonValueType jsonValueType) {
        this.jsonValueType = nonNull(jsonValueType);
        return this;
    }

    public double getStartValue() {
        return startValue;
    }

    public NumbersToJson setStartValue(double startValue) {
        this.startValue = startValue;
        return this;
    }

    public double getIncrement() {
        return increment;
    }

    public NumbersToJson setIncrement(double increment) {
        this.increment = increment;
        return this;
    }

    @Override
    public void process() {
        final SNumbers numbers = getInputNumbers(true);
        final String[] keys = getInputScalar(INPUT_KEYS).toTrimmedLinesWithoutCommentsArray();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final int n = numbers.getArrayLength();
        double currentValue = startValue;
        int index = 0;
        for (String key : keys) {
            final double value = index < n ? numbers.getValue(index) : currentValue;
            jsonValueType.add(builder, key, value);
            index++;
            currentValue += increment;
        }
        final JsonObject result = builder.build();
        getScalar(OUTPUT_JSON).setTo(Jsons.toPrettyString(result));
    }

}
