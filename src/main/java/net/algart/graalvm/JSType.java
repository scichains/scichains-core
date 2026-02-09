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

package net.algart.graalvm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public enum JSType {
    /**
     * Common behavior. However, if it is a file with a name, ending by ".mjs", or if you manually specify a name
     * ending with ".mjs", it will work as a module.
     */
    COMMON("common") {
        @Override
        void doConfigure(GraalSourceContainer container) {
        }
    },
    /**
     * ECMAScript 6 module.
     */
    MODULE("module") {
        @Override
        void doConfigure(GraalSourceContainer container) {
            container.setMimeType("application/javascript+module");
        }
    };

    private final String typeName;

    JSType(String typeName) {
        this.typeName = Objects.requireNonNull(typeName);
    }

    public static Collection<String> typeNames() {
        return Arrays.stream(values()).map(JSType::typeName).toList();
    }

    public String typeName() {
        return typeName;
    }

    /**
     * Returns an {@link Optional} containing the {@link JSType} with the given {@link #typeName()}.
     * <p>If no JS type with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param typeName the type name; may be {@code null}.
     * @return an optional JS type.
     */
    public static Optional<JSType> fromTypeName(String typeName) {
        for (JSType type : values()) {
            if (type.typeName.equals(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public void configure(GraalSourceContainer container, Object scriptOrigin, String name) {
        Objects.requireNonNull(container, "Null container");
        Objects.requireNonNull(scriptOrigin, "Null script origin");
        container.setLanguage(GraalSourceContainer.JAVASCRIPT_LANGUAGE);
        container.setOrigin(scriptOrigin, name);
        doConfigure(container);
    }

    abstract void doConfigure(GraalSourceContainer container);

}
