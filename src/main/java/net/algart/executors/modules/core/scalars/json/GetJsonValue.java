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

package net.algart.executors.modules.core.scalars.json;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;
import net.algart.json.Jsons;

public final class GetJsonValue extends ScalarFilter {
    public static final String INPUT_JSON = "json";

    private String key = "jsonKey";
    private String defaultValue = "";
    private boolean useDefaultValue = true;

    public GetJsonValue() {
        setDefaultInputScalar(INPUT_JSON);
    }

    public String getKey() {
        return key;
    }

    public GetJsonValue setKey(String key) {
        this.key = nonEmpty(key);
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public GetJsonValue setDefaultValue(String defaultValue) {
        this.defaultValue = nonNull(defaultValue);
        return this;
    }

    public boolean isUseDefaultValue() {
        return useDefaultValue;
    }

    public GetJsonValue setUseDefaultValue(boolean useDefaultValue) {
        this.useDefaultValue = useDefaultValue;
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        final JsonObject json = Jsons.toJson(source.getValue(), true);
        final JsonValue jsonValue = json.get(key);
        return jsonValue != null ? SScalar.valueOf(Jsons.toPrettyString(jsonValue)) :
                useDefaultValue ? SScalar.valueOf(defaultValue) : new SScalar();
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
