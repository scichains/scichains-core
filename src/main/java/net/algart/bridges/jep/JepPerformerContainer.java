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

package net.algart.bridges.jep;

import jep.Interpreter;
import jep.JepConfig;
import net.algart.bridges.jep.additions.JepInterpreterKind;
import net.algart.bridges.jep.additions.JepSingleThreadInterpreter;

import java.util.Objects;
import java.util.function.Supplier;

public final class JepPerformerContainer implements AutoCloseable {
    private Supplier<Interpreter> contextSupplier;
    private Supplier<JepConfig> configSupplier = null;
    private JepInterpreterKind kind = JepInterpreterKind.SHARED;
    private String name = kind.kindName();

    private volatile JepPerformer performer = null;
    private final Object lock = new Object();

    private JepPerformerContainer() {
        resetSupplier();
    }

    public static JepPerformerContainer getContainer() {
        return new JepPerformerContainer();
    }

    public static JepPerformerContainer getContainer(JepInterpreterKind kind) {
        return getContainer().setKind(kind);
    }

    public JepInterpreterKind getKind() {
        return kind;
    }

    public JepPerformerContainer setKind(JepInterpreterKind kind) {
        this.kind = Objects.requireNonNull(kind, "Null kind");
        this.name = kind.kindName();
        resetSupplier();
        return this;
    }

    public Supplier<JepConfig> getConfigSupplier() {
        return configSupplier;
    }

    /**
     * Sets supplier for JEP configuration. Used to create {@link JepConfig} object,
     * used for creating JEP {@link Interpreter}.
     *
     * <p>Note: using supplier instead of explicit configuration object (stored inside this container)
     * helps to avoid long-time and possibly problematic operations while customizing this container object.
     * For example, maybe, you need to specify include paths inside {@link JepConfig},
     * but you cannot find this paths without serious disc operations.
     *
     * @param configSupplier configuration supplier; may be <tt>null</tt>, then will be ignored.
     * @return reference to this object.
     */
    public JepPerformerContainer setConfigSupplier(Supplier<JepConfig> configSupplier) {
        this.configSupplier = configSupplier;
        return this;
    }

    public String getName() {
        return name;
    }

    public JepPerformerContainer setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets custom supplier of JEP interpreter. In this case, the kind is ignored.
     *
     * @param customContextSupplier custom supplier.
     * @param name                  name of created interpreter (used in toString and logging).
     * @return reference to this object.
     */
    public JepPerformerContainer setContextSupplier(Supplier<Interpreter> customContextSupplier, String name) {
        Objects.requireNonNull(customContextSupplier, "Null customContextSupplier");
        this.contextSupplier = customContextSupplier;
        this.name = name;
        return this;
    }

    public JepPerformerContainer setContextSupplier(Supplier<Interpreter> customInterpreterSupplier) {
        return setContextSupplier(customInterpreterSupplier, null);
    }

    public Supplier<Interpreter> getContextSupplier() {
        return contextSupplier;
    }

    public JepPerformer performer() {
        // Maybe, in future we will be reworked: https://github.com/ninia/jep/issues/411
        JepPerformer performer;
        boolean created = false;
        synchronized (lock) {
            performer = this.performer;
            if (performer == null) {
                this.performer = performer = JepPerformer.newPerformer(
                        JepSingleThreadInterpreter.newInstance(contextSupplier, name));
                created = true;
            }
        }
        if (created) {
            JepPerformer.LOG.log(System.Logger.Level.DEBUG, "Created new " + performer);
        }
        return performer;
    }

    public JepConfig config() {
        return configSupplier == null ? null : configSupplier.get();
    }

    /**
     * @apiNote Will be automatically revived while usage after closing.
     */
    public void close() {
        String message = null;
        synchronized (lock) {
            if (performer != null) {
                message = performer.toString();
                performer.close();
                performer = null;
            }
        }
        if (message != null) {
            JepPerformer.LOG.log(System.Logger.Level.DEBUG, "Closed " + message);
        }
    }

    private void resetSupplier() {
        contextSupplier = () -> kind.newInterpreter(config());
    }
}
