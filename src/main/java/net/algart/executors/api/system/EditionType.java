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

package net.algart.executors.api.system;

import net.algart.executors.api.parameters.ValueType;

import java.util.*;

public enum EditionType {
    VALUE("value", false),
    ENUM("enum", false),
    FILE("file", true),
    FILE_TO_SAVE("file_to_save", true),
    FOLDER("folder", true),
    COLOR("color", false),
    RANGE("range", false);

    private final String typeName;
    private final boolean resources;

    private static final Map<String, EditionType> ALL_TYPES = new LinkedHashMap<>();

    static {
        for (EditionType type : values()) {
            ALL_TYPES.put(type.typeName, type);
        }
    }

    EditionType(String typeName, boolean resources) {
        this.typeName = Objects.requireNonNull(typeName);
        this.resources = resources;
    }

    public static Collection<String> typeNames() {
        return Collections.unmodifiableCollection(ALL_TYPES.keySet());
    }

    public String typeName() {
        return typeName;
    }

    public boolean isResources() {
        return resources;
    }

    public boolean isPath() {
        return isResources();
        // - in the current version it is the same thing
    }

    public boolean isEnum() {
        return this == ENUM;
    }

    public static EditionType ofTypeName(String typeName) {
        Objects.requireNonNull(typeName, "Null type name");
        return fromTypeName(typeName).orElseThrow(
                () -> new IllegalArgumentException("Unknown control edition type \"" + typeName + "\""));
    }

    /**
     * Returns an {@link Optional} containing the {@link EditionType} with the given {@link #typeName()}.
     * <p>If no edition type with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param typeName the value type name; may be {@code null}.
     * @return an optional edition type.
     */
    public static Optional<EditionType> fromTypeName(String typeName) {
        return Optional.ofNullable(ALL_TYPES.get(typeName));
    }

    public static EditionType defaultEditionType(ValueType valueType) {
        Objects.requireNonNull(valueType, "Null valueType");
        return valueType == ValueType.ENUM_STRING ? ENUM : VALUE;
    }

    public static void main(String[] args) {
        for (EditionType type : values()) {
            System.out.printf("%s: %s%n", type.typeName(), ofTypeName(type.typeName()));
        }
    }
}
