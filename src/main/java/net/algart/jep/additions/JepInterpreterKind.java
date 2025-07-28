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

package net.algart.jep.additions;

import jep.Interpreter;
import jep.JepConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum JepInterpreterKind {
    LOCAL("local", "local (sub-interpreter)"),
    SHARED("shared", "shared"),
    GLOBAL("global", "JVM-global");

    private final String kindName;
    private final String prettyName;

    private static final Map<String, JepInterpreterKind> ALL_KINDS = new LinkedHashMap<>();

    static {
        for (JepInterpreterKind type : values()) {
            ALL_KINDS.put(type.kindName, type);
        }
    }
    JepInterpreterKind(String kindName, String prettyName) {
        this.kindName = kindName;
        this.prettyName = prettyName;
    }

    public String kindName() {
        return kindName;
    }

    public String prettyName() {
        return prettyName;
    }

    public boolean isPure() {
        return this == LOCAL;
    }

    public boolean isGlobalInJVM() {
        return this == GLOBAL;
    }

    public ConfiguredInterpreter newInterpreter(Supplier<JepConfig> configurationSupplier) {
        return newInterpreter(configurationSupplier == null ? null : configurationSupplier.get());
    }

    public ConfiguredInterpreter newInterpreter(JepConfig configuration) {
        if (configuration == null) {
            configuration = new JepExtendedConfiguration();
        }
        Interpreter interpreter = this == LOCAL ?
                JepCreationTools.newSubInterpreter(configuration, this) :
                JepCreationTools.newSharedInterpreter(configuration, this);
        return new ConfiguredInterpreter(interpreter, configuration);
    }

    public static JepInterpreterKind ofOrNull(String name) {
        return ALL_KINDS.get(name);
    }
}
