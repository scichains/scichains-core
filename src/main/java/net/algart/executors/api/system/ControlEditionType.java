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

package net.algart.executors.api.system;

import net.algart.executors.api.data.ParameterValueType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public enum ControlEditionType {
    VALUE("value", false),
    ENUM("enum", false),
    FILE("file", true),
    FOLDER("folder", true),
    COLOR("color", false),
    RANGE("range", false);

    private final String editionTypeName;
    private final boolean resources;

    private static final Map<String, ControlEditionType> ALL_TYPES = new LinkedHashMap<>();

    static {
        for (ControlEditionType type : values()) {
            ALL_TYPES.put(type.editionTypeName, type);
        }
    }

    ControlEditionType(String editionTypeName, boolean resources) {
        this.editionTypeName = editionTypeName;
        this.resources = resources;
    }

    public String editionTypeName() {
        return editionTypeName;
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

    public static ControlEditionType valueOfEditionTypeName(String name) {
        final ControlEditionType result = ALL_TYPES.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Unknown control edition type: " + name);
        }
        return result;
    }

    public static ControlEditionType valueOfEditionTypeNameOrNull(String name) {
        return ALL_TYPES.get(name);
    }

    public static ControlEditionType defaultEditionType(ParameterValueType valueType) {
        Objects.requireNonNull(valueType, "Null valueType");
        return valueType == ParameterValueType.ENUM_STRING ? ENUM : VALUE;
    }

    public static void main(String[] args) {
        for (ControlEditionType type : values()) {
            System.out.printf("%s: %s%n", type.editionTypeName(), valueOfEditionTypeName(type.editionTypeName()));
        }
    }
}
