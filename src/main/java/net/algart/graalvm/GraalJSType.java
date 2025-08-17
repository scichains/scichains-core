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

package net.algart.graalvm;

import java.util.Objects;

public enum GraalJSType {
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

    GraalJSType(String typeName) {
        this.typeName = typeName;
    }

    public String typeName() {
        return typeName;
    }

    public static GraalJSType valueOfTypeNameOrNull(String name) {
        Objects.requireNonNull(name, "Null type name");
        for (GraalJSType type : values()) {
            if (name.equals(type.typeName)) {
                return type;
            }
        }
        return null;
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
