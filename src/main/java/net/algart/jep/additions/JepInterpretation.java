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
import java.util.Objects;
import java.util.function.Supplier;

public class JepInterpretation {
    public enum Kind {
        SUB_INTERPRETER("sub-interpreter", "sub-interpreter (local)"),
        SHARED("shared", "shared"),
        GLOBAL("global", "JVM-global");

        private final String kindName;
        private final String prettyName;

        private static final Map<String, Kind> ALL_KINDS = new LinkedHashMap<>();

        static {
            for (Kind type : values()) {
                ALL_KINDS.put(type.kindName, type);
            }
        }

        Kind(String kindName, String prettyName) {
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
            return this == SUB_INTERPRETER;
        }

        /**
         * Returns <code>true</code> if this interpreter uses the single thread, global to the entire JVM.
         *
         * <p>Note: in this case, you <b>must globally synchronize</b>
         * the entire code from creation {@link jep.SharedInterpreter}
         * (usually via {@link net.algart.jep.JepPerformerContainer}) until destroying by
         * {@link SharedInterpreter#close()} (usually via {@link JepPerformerContainer#close()}.
         * You may use {@link JepInterpretation#executeWithJVMGlobalLock(Runnable, Runnable, Runnable)} method to do this.
         *
         * @return whether this interpreter executes Python code in the single thread for the entire JVM.
         */
        public boolean isJVMGlobal() {
            return this == GLOBAL;
        }

        public ConfiguredInterpreter newInterpreter(Supplier<JepConfig> configurationSupplier) {
            return newInterpreter(configurationSupplier == null ? null : configurationSupplier.get());
        }

        public ConfiguredInterpreter newInterpreter(JepConfig configuration) {
            if (configuration == null) {
                configuration = new JepExtendedConfiguration();
            }
            final Interpreter interpreter = this == SUB_INTERPRETER ?
                    JepCreationTools.newSubInterpreter(configuration, this) :
                    JepCreationTools.newSharedInterpreter(configuration, this);
            return new ConfiguredInterpreter(interpreter, configuration);
        }

        public static Kind ofOrNull(String name) {
            return ALL_KINDS.get(name);
        }
    }

    private JepInterpretation() {
    }

    public static Object getJVMGlobalLock() {
        return JepSingleThreadInterpreter.getGlobalLock();
    }

    /**
     * Executes a sequence of actions under the JVM-global synchronization lock: {@link #getJVMGlobalLock()}.
     * Here:
     * <ul>
     *     <li><code>creation</code>:
     *     creates the interpreter (usually {@link JepSingleThreadInterpreter}
     *     or {@link JepPerformerContainer});</li>
     *     <li><code>processing</code>: performs the main logic using the created interpteter;</li>
     *     <li><code>closing</code>: closes the resource (for example, {@link JepPerformerContainer#close()} or
     *     {@link JepPerformerContainer#close()}; guaranteed to be called even in case of exception.
     * </ul>
     *
     * <p>This method uses <code>synchronized (getJVMGlobalLock()) {...}</code> operators
     * for executing all 3 stages.
     *
     * @param creation   create the interpreter.
     * @param processing process Python operations.
     * @param closing    close the interpreter.
     * @throws NullPointerException if any of arguments is <code>null</code>.
     */
    public static void executeWithJVMGlobalLock(Runnable creation, Runnable processing, Runnable closing) {
        Objects.requireNonNull(creation, "Null creation");
        Objects.requireNonNull(processing, "Null processing");
        Objects.requireNonNull(closing, "Null closing");
        synchronized (getJVMGlobalLock()) {
            try {
                creation.run();
                // - should create a single SharedInterpreter
                processing.run();
            } finally {
                closing.run();
                // - should close that SharedInterpreter; must be called even in case of exception!
            }
        }
    }
}
