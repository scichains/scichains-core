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

package net.algart.json;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public abstract class AbstractConvertibleToJson extends PropertyChecker {
    public JsonObject toJson() {
        checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        buildJson(builder);
        return builder.build();
    }

    public String jsonString() {
        return Jsons.toPrettyString(toJson());
    }

    public void checkCompleteness() {
    }

    public abstract void buildJson(JsonObjectBuilder builder);

    public void checkNull(Object value, String name) {
        checkNull(value, name, getClass());
    }

    public static void checkNull(Object value, String name, Class<?> clazz) {
        if (value == null) {
            throw new IllegalStateException("\"" + name + "\" property is not set in " + clazz);
        }
    }
}
