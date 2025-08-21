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
import jep.SharedInterpreter;
import net.algart.jep.JepPerformerContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum JepType {
    NORMAL("normal", "normal"),
    GLOBAL("global", "JVM-global"),
    SUB_INTERPRETER("sub-interpreter", "sub-interpreter (local)");

    private final String typeName;
    private final String prettyName;

    private static final Map<String, JepType> ALL_MODES = new LinkedHashMap<>();

    static {
        for (JepType type : values()) {
            ALL_MODES.put(type.typeName, type);
        }
    }

    JepType(String typeName, String prettyName) {
        this.typeName = typeName;
        this.prettyName = prettyName;
    }

    public String typeName() {
        return typeName;
    }

    public String prettyName() {
        return prettyName;
    }

    public boolean isPure() {
        return this == SUB_INTERPRETER;
    }

    /**
     * Returns <code>true</code> if this interpreter uses the single thread, global to the entire JVM.
     *
     * <p>Note: in this case, you <b>must globally synchronize</b>
     * the entire code from creation {@link jep.SharedInterpreter}
     * (usually via {@link net.algart.jep.JepPerformerContainer}) until destroying by
     * {@link SharedInterpreter#close()} (usually via {@link JepPerformerContainer#close()}.
     * You may use {@link JepInterpretation#executeWithJVMGlobalLock(Runnable, Runnable, Runnable)} method to do
     * this.
     *
     * @return whether this interpreter executes Python code in the single thread for the entire JVM.
     */
    public boolean isJVMGlobal() {
        return this == GLOBAL;
    }

    public ConfiguredInterpreter newLowLevelInterpreter(Supplier<JepConfig> configurationSupplier) {
        return newLowLevelInterpreter(configurationSupplier == null ? null : configurationSupplier.get());
    }

    public ConfiguredInterpreter newLowLevelInterpreter(JepConfig configuration) {
        if (configuration == null) {
            configuration = new JepExtendedConfiguration();
        }
        final Interpreter interpreter = this == SUB_INTERPRETER ?
                JepCreationTools.newSubInterpreter(configuration, this) :
                JepCreationTools.newSharedInterpreter(configuration, this);
        return new ConfiguredInterpreter(interpreter, configuration);
    }

    public static JepType ofOrNull(String name) {
        return ALL_MODES.get(name);
    }
}
