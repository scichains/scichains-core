/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jakarta.json.*;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Jsons {

    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    // - Note: "Users are recommended to cache the result of this method" (from JavaDoc to provider() method).
    // But we don't always use it: in most cases this optimization is not important.

    private Jsons() {
    }

    public static JsonObject newEmptyJson() {
        return Json.createObjectBuilder().build();
    }

    public static JsonObject readJson(Path path) throws IOException {
        Objects.requireNonNull(path, "Null path");
        if (!Files.exists(path)) {
            throw new NoSuchFileException("JSON file does not exist: " + path);
            // - little better message than in Files.newBufferedReader
        }
        try {
            try (final JsonReader reader = Json.createReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
                return reader.readObject();
            }
        } catch (JsonException e) {
            throw new JsonException("Invalid JSON in the file " + path, e);
        }
    }

    public static JsonObject toJson(String jsonString) {
        Objects.requireNonNull(jsonString, "Null JSON string");
        try (final JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            return reader.readObject();
        }
    }

    public static JsonObject toJson(String jsonString, boolean allowNullOrEmptyString) {
        if (allowNullOrEmptyString && jsonString == null || (jsonString = jsonString.trim()).isEmpty()) {
            return newEmptyJson();
        } else {
            return toJson(jsonString);
        }
    }

    public static JsonArray toJsonArray(String jsonString) {
        Objects.requireNonNull(jsonString, "Null JSON string");
        try (final JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            return reader.readArray();
        }
    }

    public static JsonArray toJsonArray(Collection<String> strings) {
        Objects.requireNonNull(strings, "Null strings");
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (String s : strings) {
            builder.add(s);
        }
        return builder.build();
    }

    public static JsonString stringValue(String value) {
        Objects.requireNonNull(value, "Null value");
        // - Important: createValue MAY accept null, but the returned value will have unpredictable behavior
        return JSON_PROVIDER.createValue(value);
        // - this is faster than Json.createValue(): see JSON_PROVIDER
        // Deprecated way (before the version 1.1):
        // return JSON_PROVIDER.createObjectBuilder().add("x", value).build().getJsonString("x");
    }

    public static JsonNumber intValue(int value) {
        return JSON_PROVIDER.createValue(value);
        // Deprecated way (before the version 1.1):
        // return JSON_PROVIDER.createObjectBuilder().add("x", value).build().getJsonNumber("x");
    }

    public static JsonNumber longValue(long value) {
        return JSON_PROVIDER.createValue(value);
        // Deprecated way (before the version 1.1):
        // return JSON_PROVIDER.createObjectBuilder().add("x", value).build().getJsonNumber("x");
    }

    public static JsonNumber doubleValue(double value) {
        return JSON_PROVIDER.createValue(value);
        // Deprecated way (before the version 1.1):
        // return JSON_PROVIDER.createObjectBuilder().add("x", value).build().getJsonNumber("x");
    }

    public static JsonValue booleanValue(boolean value) {
        return value ? JsonValue.TRUE : JsonValue.FALSE;
    }

    public static void addAllJson(JsonObjectBuilder builder, JsonObject existingJson) {
        Objects.requireNonNull(builder, "Null builder");
        Objects.requireNonNull(existingJson, "Null existingJson");
        for (Map.Entry<String, JsonValue> entry : existingJson.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
    }

    public static JsonObjectBuilder createObjectBuilder(JsonObject existingJson) {
        Objects.requireNonNull(existingJson, "Null existingJson");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        addAllJson(builder, existingJson);
        return builder;
    }

    public static JsonObject filterJson(JsonObject sourceJson, Set<String> keysToPreserve) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        Objects.requireNonNull(keysToPreserve, "Null keysToPreserve");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : sourceJson.entrySet()) {
            final String key = entry.getKey();
            if (keysToPreserve.contains(key)) {
                builder.add(key, entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject overrideEntries(JsonObject sourceJson, JsonObject overridingJson) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        Objects.requireNonNull(overridingJson, "Null overridingJson");
        final JsonObjectBuilder builder = createObjectBuilder(sourceJson);
        addAllJson(builder, overridingJson);
        return builder.build();
    }

    public static JsonObject addNonExistingEntries(JsonObject sourceJson, JsonObject overridingJson) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        Objects.requireNonNull(overridingJson, "Null overridingJson");
        final JsonObjectBuilder builder = createObjectBuilder(sourceJson);
        for (Map.Entry<String, JsonValue> entry : overridingJson.entrySet()) {
            final String key = entry.getKey();
            if (!sourceJson.containsKey(key)) {
                builder.add(key, entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject overrideOnlyExistingInBoth(JsonObject sourceJson, JsonObject overridingJson) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        Objects.requireNonNull(overridingJson, "Null overridingJson");
        final JsonObjectBuilder builder = createObjectBuilder(sourceJson);
        for (Map.Entry<String, JsonValue> entry : overridingJson.entrySet()) {
            final String key = entry.getKey();
            if (sourceJson.containsKey(key)) {
                builder.add(key, entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject extractSimpleValues(JsonObject sourceJson) {
        Objects.requireNonNull(sourceJson, "Null sourceJson");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : sourceJson.entrySet()) {
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

    public static String toPrettyString(JsonValue jsonValue) {
        Objects.requireNonNull(jsonValue, "Null jsonValue");
        return switch (jsonValue.getValueType()) {
            case STRING -> ((JsonString) jsonValue).getString();
            case OBJECT -> toPrettyString((JsonObject) jsonValue);
            case ARRAY -> toPrettyString((JsonArray) jsonValue);
            default -> jsonValue.toString();
        };
    }

    public static String toPrettyString(JsonObject json) {
        Objects.requireNonNull(json, "Null json");
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(json);
            return stringWriter.toString().trim();
            // - trim() removes extra starting empty line
        }
    }

    public static String toPrettyString(JsonArray json) {
        Objects.requireNonNull(json, "Null json");
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeArray(json);
            return stringWriter.toString().trim();
            // - trim() removes extra starting empty line
        }
    }

    public static List<JsonObject> toJsonObjects(JsonArray jsonArray) {
        return toJsonObjects(jsonArray, null);
    }

    public static List<JsonObject> toJsonObjects(JsonArray jsonArray, String nameForException) {
        return toJsonObjects(jsonArray, nameForException, null);
    }

    public static List<JsonObject> toJsonObjects(JsonArray jsonArray, String nameForException, Path file) {
        Objects.requireNonNull(jsonArray, "Null jsonArray");
        final List<JsonObject> result = new ArrayList<>();
        for (JsonValue jsonValue : jsonArray) {
            if (!(jsonValue instanceof JsonObject)) {
                throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file) + ": "
                        + (nameForException == null ? "an" : "\"" + nameForException + "\"")
                        + " array contains non-object element " + jsonValue);
            }
            result.add((JsonObject) jsonValue);
        }
        return result;
    }

    public static List<String> toStrings(JsonArray jsonArray) {
        return toStrings(jsonArray, null);
    }

    public static List<String> toStrings(JsonArray jsonArray, String nameForException) {
        return toStrings(jsonArray, nameForException, null);
    }

    public static List<String> toStrings(JsonArray jsonArray, String nameForException, Path file) {
        Objects.requireNonNull(jsonArray, "Null jsonArray");
        final List<String> result = new ArrayList<>();
        for (JsonValue jsonValue : jsonArray) {
            if (!(jsonValue instanceof JsonString)) {
                throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file) + ": "
                        + (nameForException == null ? "an" : "\"" + nameForException + "\"")
                        + " array contains non-string element " + jsonValue);
            }
            result.add(((JsonString) jsonValue).getString());
        }
        return result;
    }

    public static int reqInt(JsonObject json, String name) {
        return reqInt(json, name, null);
    }

    public static int reqInt(JsonObject json, String name, Path file) {
        return reqIntWithAlias(json, name, null, file);
    }

    public static int reqIntWithAlias(JsonObject json, String name, String aliasName, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonNumber result;
        String usedName = name;
        try {
            result = json.getJsonNumber(name);
            if (result == null && aliasName != null) {
                usedName = aliasName;
                result = json.getJsonNumber(aliasName);
            }
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + usedName + "\" value is not a number"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (result == null) {
            throw new JsonException("Invalid JSON " + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" numeric value required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result.intValueExact();
    }

    public static void addDouble(JsonObjectBuilder builder, String name, double value) {
        if (value == Double.NEGATIVE_INFINITY) {
            builder.add(name, "\u2212\u221E");
        } else if (value == Double.POSITIVE_INFINITY) {
            builder.add(name, "+\u221E");
        } else if (Double.isNaN(value)) {
            builder.add(name, "NaN");
        } else {
            builder.add(name, value);
        }
    }

    public static double getDouble(JsonObject json, String name, double defaultValue) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonValue jsonValue = json.get(name);
        if (jsonValue == null) {
            return defaultValue;
        }
        final Double special = specialDoubleValue(jsonValue);
        if (special != null) {
            return special;
        }
        try {
            return ((JsonNumber) jsonValue).doubleValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double reqDouble(JsonObject json, String name) {
        return reqDouble(json, name, null);
    }

    public static double reqDouble(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonValue jsonValue = json.get(name);
        if (jsonValue == null) {
            throw new JsonException("Invalid JSON " + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" double value required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        final Double special = specialDoubleValue(jsonValue);
        if (special != null) {
            return special;
        }
        JsonNumber result;
        try {
            result = (JsonNumber) jsonValue;
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a number"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result.doubleValue();
    }

    public static long getLong(JsonObject json, String name, long defaultValue) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        try {
            return json.getJsonNumber(name).longValueExact();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long reqLong(JsonObject json, String name) {
        return reqLong(json, name, null);
    }

    public static long reqLong(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonNumber result;
        try {
            result = json.getJsonNumber(name);
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a number"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (result == null) {
            throw new JsonException("Invalid JSON " + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" long value required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result.longValueExact();
    }

    public static String reqString(JsonObject json, String name) {
        return reqString(json, name, null);
    }

    public static String reqString(JsonObject json, String name, Path file) {
        return reqStringWithAlias(json, name, null, file);
    }

    public static String reqStringWithAlias(JsonObject json, String name, String aliasName, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonString result;
        String usedName = name;
        try {
            result = json.getJsonString(name);
            if (result == null && aliasName != null) {
                usedName = aliasName;
                result = json.getJsonString(aliasName);
            }
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + usedName + "\" value is not a string"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (result == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result.getString();
    }

    public static JsonValue reqJsonValue(JsonObject json, String name) {
        return reqJsonValue(json, name, null);
    }

    public static JsonValue reqJsonValue(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonValue result = json.get(name);
        if (result == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result;
    }

    public static List<JsonObject> getJsonObjectsOrEmptyList(JsonObject json, String name) {
        return getJsonObjectsOrEmptyList(json, name, null);
    }

    public static List<JsonObject> getJsonObjectsOrEmptyList(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonArray jsonArray = json.getJsonArray(name);
        if (jsonArray == null) {
            return Collections.emptyList();
        }
        return toJsonObjects(jsonArray, name, file);
    }

    public static JsonObject reqJsonObject(JsonObject json, String name) {
        return reqJsonObject(json, name, null);
    }

    public static JsonObject reqJsonObject(JsonObject json, String name, Path file) {
        return reqJsonObjectWithAlias(json, name, null, file);
    }

    public static JsonObject reqJsonObjectWithAlias(JsonObject json, String name, String aliasName, Path file) {
        Objects.requireNonNull(json, "Null json");
        JsonObject result = getJsonObject(json, name, file);
        if (result == null && aliasName != null) {
            result = getJsonObject(json, aliasName, file);
        }
        if (result == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" JSON object required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result;
    }

    /**
     * Analog of <code>json.getJsonObject(name)</code> with two differences:
     * 1) if <code>json==null</code>, it returns <code>null</code>;
     * 2) if <code>json</code> contains value with a given name, but it is <b>not</b> an object,
     * it throws detailed {@link JsonException} (instead of <code>ClassCastException</code>).
     *
     * @param json JSON object to by analysed
     * @param name the name whose associated value is to be returned
     * @return the object value to which the specified name is mapped,
     * or <code>null</code> if there is no necessary mapping
     * @throws JsonException if the value to which the specified name is mapped is not assignable to JsonObject type
     */
    public static JsonObject getJsonObject(JsonObject json, String name) {
        return getJsonObject(json, name, null);
    }

    public static JsonObject getJsonObject(JsonObject json, String name, Path file) {
        Objects.requireNonNull(name, "Null name");
        if (json == null) {
            return null;
        }
        try {
            return json.getJsonObject(name);
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a JSON object"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
    }

    public static JsonArray reqJsonArray(JsonObject json, String name, Path file) {
        return reqJsonArray(json, name, file, false);
    }

    public static JsonArray reqJsonArray(JsonObject json, String name, Path file, boolean mustContainOnlyObjects) {
        JsonArray result = getJsonArray(json, name, file, mustContainOnlyObjects);
        if (result == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" JSON array required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result;
    }

    public static JsonArray getJsonArray(JsonObject json, String name, Path file) {
        return getJsonArray(json, name, file, false);
    }

    public static JsonArray getJsonArray(JsonObject json, String name, Path file, boolean mustContainOnlyObjects) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonArray result;
        try {
            result = json.getJsonArray(name);
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a JSON array"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (mustContainOnlyObjects && result != null) {
            for (JsonValue jsonValue : result) {
                if (!(jsonValue instanceof JsonObject)) {
                    throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file) + ": \"" + name
                            + "\" array contains non-object element " + jsonValue);
                }
            }
        }
        return result;
    }

    public static List<String> reqStrings(JsonObject json, String name) {
        return reqStrings(json, name, null);
    }

    public static List<String> reqStrings(JsonObject json, String name, Path file) {
        final List<String> result = getStrings(json, name, file);
        if (result == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" JSON array required"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        return result;
    }

    public static List<String> getStrings(JsonObject json, String name) {
        return getStrings(json, name, null);
    }

    public static List<String> getStrings(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        final JsonArray jsonArray;
        try {
            jsonArray = json.getJsonArray(name);
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a JSON array"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (jsonArray == null) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        for (JsonValue jsonValue : jsonArray) {
            if (!(jsonValue instanceof JsonString)) {
                throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file) + ": \"" + name
                        + "\" array contains non-string element " + jsonValue);
            }
            result.add(((JsonString) jsonValue).getString());
        }
        return result;
    }

    public static List<JsonObject> reqJsonObjects(JsonObject json, String name) {
        return reqJsonObjects(json, name, null);
    }

    public static List<JsonObject> reqJsonObjects(JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        JsonArray jsonArray;
        try {
            jsonArray = json.getJsonArray(name);
        } catch (ClassCastException e) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" value is not a JSON array"
                    + (file == null ? " <<<" + json + ">>>" : ""));
        }
        if (jsonArray == null) {
            throw new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                    + ": \"" + name + "\" array required");
        }
        return toJsonObjects(jsonArray, name, file);
    }

    public static String toString(Color color) {
        Objects.requireNonNull(color, "Null color");
        final int rgb = color.getRGB() & 0xFFFFFF;
        return String.format("#%06X", rgb);
    }

    public static Color toColor(String s) {
        Objects.requireNonNull(s, "Null string");
        return Color.decode(s);
    }

    // The following functions are deprecated and useless: replaced with using unknownValueException
    @Deprecated(forRemoval = true)
    public static <T> T requireNonNull(T value, JsonObject json, String name) {
        return requireNonNull(value, json, name, null);
    }

    @Deprecated(forRemoval = true)
    public static <T> T requireNonNull(T value, JsonObject json, String name, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        if (value == null) {
            throw incorrectValue(json, name, "required", file);
        }
        return value;
    }

    @Deprecated(forRemoval = true)
    public static <T> T requireNonNull(T value, JsonObject json, String name, String message, Path file) {
        Objects.requireNonNull(json, "Null json");
        Objects.requireNonNull(name, "Null name");
        if (value == null) {
            throw incorrectValue(json, name, message, file);
        }
        return value;
    }

    public static JsonException badValue(JsonObject json, String name) {
        return badValue(json, name, (Path) null);
    }

    public static JsonException badValue(JsonObject json, String name, Path file) {
        return incorrectValue(json, name, "unknown value", file);
    }

    public static JsonException badValue(JsonObject json, String name, String actualValue) {
        return badValue(json, name, actualValue, (Path) null);
    }

    public static JsonException badValue(JsonObject json, String name, String actualValue, Path file) {
        return incorrectValue(json, name, "unknown value: \"" + actualValue + "\"", file);
    }

    public static JsonException badValue(JsonObject json, String name, String actual, Collection<?> values) {
        return badValue(json, name, actual, values, null);
    }

    public static JsonException badValue(
            JsonObject json,
            String name,
            String actual,
            Collection<?> values,
            Path file) {
        return badValue(json, name, actual, values.stream(), file);
    }

    public static JsonException badValue(JsonObject json, String name, String actual, Stream<?> allowed) {
        return badValue(json, name, actual, allowed, null);
    }

    public static JsonException badValue(JsonObject json, String name, String actual, Stream<?> allowed, Path file) {
        return incorrectValue(json, name, "unknown value: \"" + actual +
                "\" (allowed variants: "
                + allowed.map(v -> "\"" + v + "\"").collect(Collectors.joining(", ")) + ")",
                file);
    }

    public static JsonException incorrectValue(JsonObject json, String name, String message) {
        return incorrectValue(json, name, message, null);
    }

    public static JsonException incorrectValue(JsonObject json, String name, String message, Path file) {
        return new JsonException("Invalid JSON" + (file == null ? "" : " " + file)
                + ": \"" + name + "\" property contains " + message
                + (file == null ? " <<<" + json + ">>>" : ""));
    }

    private static Double specialDoubleValue(JsonValue jsonValue) {
        if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
            final String s = ((JsonString) jsonValue).getString();
            if ("\u2212\u221E".equals(s)) {
                // - characters: "minus" and "infinity"
                return Double.NEGATIVE_INFINITY;
            } else if ("+\u221E".equals(s)) {
                // - characters: "+" and "infinity"
                return Double.POSITIVE_INFINITY;
            } else if ("NaN".equals(s)) {
                return Double.NaN;
            }
        }
        return null;
    }
}
