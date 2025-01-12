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

package net.algart.executors.modules.core.logic.compiler.mappings.model;

import jakarta.json.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.parameters.Parameters;
import net.algart.math.IRange;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Note: current version does not actually need cloning, but we implement clone() for possible future extensions.
public final class Mapping implements Cloneable {
    private final MappingSpecification specification;
    private final List<String> keys;
    private final List<String> keyCaptions;
    private final List<String> enumItems;
    private final List<String> enumItemCaptions;

    private Mapping(
            MappingSpecification specification,
            List<String> keys,
            List<String> keyCaptions,
            List<String> enumItems,
            List<String> enumItemCaptions) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        Objects.requireNonNull(keys, "Null keys");
        final Set<String> ignoredKeys = specification.getIgnoredKeys();
        this.keys = new ArrayList<>();
        this.keyCaptions = new ArrayList<>();
        for (int i = 0, n = keys.size(); i < n; i++) {
            final String keyOrRange = keys.get(i);
            Objects.requireNonNull(keyOrRange, "Null key #" + i);
            final String caption = keyCaptions == null || i >= keyCaptions.size() ? null : keyCaptions.get(i);
            final IRange range = keysRange(keyOrRange);
            if (range != null) {
                // - actually int range, not long
                for (int key = (int) range.min(), max = (int) range.max(); key <= max; key++) {
                    addKey(Integer.toString(key), caption, ignoredKeys);
                }
            } else {
                addKey(keyOrRange, caption, ignoredKeys);
            }
        }
        if (this.keys.isEmpty()) {
            throw new IllegalArgumentException("Empty list of keys");
        }
        for (String key : this.keys) {
            checkKey(key);
        }
        if (specification.isEnum()) {
            this.enumItems = new ArrayList<>(Objects.requireNonNull(enumItems, "Null enum items"));
            this.enumItemCaptions = enumItemCaptions == null ? null : new ArrayList<>(enumItemCaptions);
            if (this.enumItems.isEmpty()) {
                throw new IllegalArgumentException("Empty list of enum items, but value type is "
                        + specification.getControlTemplate().getValueType());
            }
        } else {
            this.enumItems = null;
            this.enumItemCaptions = null;
        }
    }

    public static Mapping valueOf(
            MappingSpecification specification,
            List<String> keys,
            List<String> keyCaptions,
            List<String> enumItems,
            List<String> enumItemCaptions) {
        return new Mapping(specification, keys, keyCaptions, enumItems, enumItemCaptions);
    }

    public MappingSpecification specification() {
        return specification;
    }


    public Path mappingSpecificationFile() {
        return specification.getMappingSpecificationFile();
    }

    public String id() {
        return specification.getId();
    }

    public String category() {
        return specification.getCategory();
    }

    public String name() {
        return specification.getName();
    }

    public String description() {
        return specification.getDescription();
    }

    public int numberOfKeys() {
        return keys.size();
    }

    public List<String> keys() {
        return Collections.unmodifiableList(keys);
    }

    public String key(int k) {
        return keys.get(k);
    }

    public List<String> keyCaptions() {
        return Collections.unmodifiableList(keyCaptions);
    }

    public String keyCaption(int k) {
        return keyCaptions.get(k);
    }

    public List<String> enumItems() {
        return enumItems == null ? null : Collections.unmodifiableList(enumItems);
    }

    public List<String> enumItemCaptions() {
        return enumItemCaptions == null ? null : Collections.unmodifiableList(enumItemCaptions);
    }

    public JsonObject createMapping(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Parameters parameters = executor.parameters();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final MappingSpecification.ControlConfTemplate controlTemplate = specification.getControlTemplate();
        for (String key : keys) {
            JsonValue jsonValue = getJsonValue(key, controlTemplate, parameters);
            assert jsonValue != null;
            builder.add(key, jsonValue);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "mapping \"" + category() + "." + name() + "')";
    }

    @Override
    public Mapping clone() {
        try {
            return (Mapping) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private void checkKey(String key) {
        try {
            Long.parseLong(key);
            // - numbers are also allowed in mapping
            return;
        } catch (NumberFormatException ignored) {
        }
        if (!SourceVersion.isIdentifier(key)) {
            throw new JsonException("Key \"" + key + "\" in mapping \"" + name()
                    + "\" is not a valid Java identifier, not an integer number and not a correct integer range "
                    + "min..max; such keys are not allowed");
        }
    }

    private IRange keysRange(String key) {
        final int p = key.indexOf("..");
        if (p == -1) {
            return null;
        }
        try {
            final int min = Integer.parseInt(key.substring(0, p));
            final int max = Integer.parseInt(key.substring(p + 2));
            return IRange.valueOf(min, max);
        } catch (NumberFormatException e) {
            throw new JsonException("Key \"" + key + "\" in mapping \"" + name()
                    + "\" is not a correct integer range min..max", e);
        }
    }

    private void addKey(String key, String caption, Set<String> ignoredKeys) {
        if (ignoredKeys == null || !ignoredKeys.contains(key)) {
            this.keys.add(key);
            if (caption != null) {
                caption = caption.replace("$$$", key);
            }
            this.keyCaptions.add(caption);
        }
    }

    public static String readNames(Path file) throws IOException {
        Objects.requireNonNull(file, "Null file");
        return Files.readString(file);
    }

    private static JsonValue getJsonValue(
            String name,
            MappingSpecification.ControlConfTemplate controlConfTemplate,
            Parameters parameters) {
        final ParameterValueType valueType = controlConfTemplate.getValueType();
        JsonValue jsonValue = null;
        if (parameters.containsKey(name)) {
            jsonValue = valueType.toJsonValue(parameters, name);
        }
        if (jsonValue == null) {
            jsonValue = controlConfTemplate.getDefaultJsonValue();
        }
        if (jsonValue == null) {
            jsonValue = valueType.emptyJsonValue();
        }
        return jsonValue;
    }

}
