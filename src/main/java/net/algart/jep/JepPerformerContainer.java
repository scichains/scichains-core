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

package net.algart.jep;

import jep.JepConfig;
import net.algart.jep.additions.JepSingleThreadInterpreter;
import net.algart.jep.additions.JepType;

import java.util.Objects;
import java.util.function.Supplier;

public final class JepPerformerContainer implements AutoCloseable {
    private Supplier<JepConfig> configurationSupplier = null;
    private final JepType type;

    private volatile JepPerformer performer = null;
    private final Object lock = new Object();

    private JepPerformerContainer(JepType type) {
        this.type = Objects.requireNonNull(type, "Null JEP interpretation type");
    }

    public static JepPerformerContainer newContainer(JepType type) {
        return new JepPerformerContainer(type);
    }

    public JepType type() {
        return type;
    }

    public Supplier<JepConfig> getConfigurationSupplier() {
        return configurationSupplier;
    }

    /**
     * Sets the supplier used to create a {@link JepConfig JEP cofi} object, which will be
     * used when creating a {@link JepPerformer} by {@link #performer()} method.
     *
     * <p>This method <b>must</b> be called before {@link #performer()}.
     * The reason for this requirement is that unconfigured performers created by a mistake
     * can lead to unpredictable results.</p>
     *
     * <p>Note: using a supplier instead of an explicit configuration object (stored inside this container)
     * helps to avoid long-time and possibly problematic operations while customizing this container object.
     * For example, you may need to compute include paths or perform disk lookups
     * before creating a {@link JepConfig}.</p>
     *
     * @param configurationSupplier configuration supplier.
     * @return reference to this object.
     * @throws NullPointerException if argument is {@code null}.
     */
    public JepPerformerContainer setConfigurationSupplier(Supplier<JepConfig> configurationSupplier) {
        Objects.requireNonNull(configurationSupplier, "Null configuration supplier");
        synchronized (lock) {
//            System.out.println(">>> Setting configuration supplier in " + System.identityHashCode(this) +
//                    ": " + configurationSupplier);
            this.configurationSupplier = configurationSupplier;
        }
        return this;
    }

    /**
     * Sets a default configuration supplier that simply creates a new {@link JepConfig}
     * with no additional customization.
     *
     * <p>This method allows the container to be used without providing an explicit
     * configuration. However, in most real scenarios this is not enough:
     * typically, the {@link JepConfig} must be customized, for example, by setting Python include paths.</p>
     *
     * @return reference to this object.
     */
    public JepPerformerContainer noConfiguration() {
        return setConfigurationSupplier(JepConfig::new);
    }

    /**
     * Returns the {@link JepPerformer} stored in this container.
     * If the performer does not yet exist (on the first call), it is created in a thread-safe manner.
     *
     * @return the existing or newly created performer.
     * @throws IllegalStateException if the configuration supplier has not been set.
     * @see #setConfigurationSupplier(Supplier)
     */
    public JepPerformer performer() {
        // Maybe in the future this will be reworked: https://github.com/ninia/jep/issues/411
        JepPerformer performer;
        boolean created = false;
        synchronized (lock) {
            if (configurationSupplier == null) {
                throw new IllegalStateException(
                        "Creating a new performer is not allowed: the configuration supplier has not been set.");
            }
            performer = this.performer;
            if (performer == null) {
//                System.out.println("!!! Requesting new performer in" + this);
                this.performer = performer = JepPerformer.newPerformer(
                        JepSingleThreadInterpreter.newInstance(type, configurationSupplier));
                created = true;
            }
        }
        if (created) {
            JepPerformer.LOG.log(System.Logger.Level.DEBUG, "Created new " + performer);
        }
        return performer;
    }

    public boolean isEmpty() {
        return performer == null;
    }

    @Override
    public String toString() {
        final JepPerformer performer = this.performer;
        return "JEP performer container (" + type + "), " +
                (performer == null ? "EMPTY" : performer.toString()) + ", " +
                "identity 0x" + System.identityHashCode(this);
    }

    /**
     * Will be automatically revived while usage after closing.
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
}
