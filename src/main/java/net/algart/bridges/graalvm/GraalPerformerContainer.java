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

package net.algart.bridges.graalvm;

import org.graalvm.polyglot.Context;

import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public abstract class GraalPerformerContainer {
    public enum ActionOnChangeContextId {
        NONE,
        CLOSE_PREVIOUS() {
            @Override
            void onChangeId(ContextKey previousContextKey, Object newContextId) {
                previousContextKey.close();
            }
        },
        /**
         * Default behaviour: disable changing context ID inside the same container.
         */
        THROW_EXCEPTION() {
            @Override
            void onChangeId(ContextKey previousContextKey, Object newContextId) {
                throw new IllegalStateException("Attempt to use the same context container " +
                        "with another context ID: old ID is \"" + previousContextKey.contextId +
                        "\", new ID is \"" + newContextId + "\"");
            }
        };

        void onChangeId(ContextKey previousContextKey, Object newContextId) {
        }
    }

    static final Logger LOG = System.getLogger(GraalPerformerContainer.class.getName());

    private Supplier<Context> contextSupplier;
    private String[] permittedLanguages = new String[0];
    private GraalContextCustomizer customizer = GraalContextCustomizer.DEFAULT;
    private Path workingDirectory = null;
    private String autoBindingLanguage = null;
    private GraalPerformerConfigurator configurator = performer -> {
    };

    GraalPerformerContainer() {
        resetSupplier();
    }

    public static GraalPerformerContainer getContainer(boolean shared) {
        return shared ? new Shared() : new Local();
    }

    public static GraalPerformerContainer getContainer(boolean shared, GraalContextCustomizer customizer) {
        Objects.requireNonNull(customizer, "Mull customizer");
        return getContainer(shared).setCustomizer(customizer);
    }

    public static Local getLocalPure() {
        return (Local) getContainer(false);
    }

    public static Local getLocalAllAccess() {
        return getLocalPure().setCustomizer(GraalContextCustomizer.ALL_ACCESS);
    }

    public static Local getLocal(GraalContextCustomizer customizer) {
        Objects.requireNonNull(customizer, "Mull customizer");
        return (Local) getContainer(false).setCustomizer(customizer);
    }

    public static Shared getSharedPure() {
        return (Shared) getContainer(true);
    }

    public static Shared getShared(GraalContextCustomizer customizer) {
        Objects.requireNonNull(customizer, "Mull customizer");
        return getSharedPure().setCustomizer(customizer);
    }

    public static Shared getShared(
            GraalContextCustomizer customizer,
            ActionOnChangeContextId actionOnChangeContextId) {
        return getShared(customizer).setActionOnChangeContextId(actionOnChangeContextId);
    }

    public String[] getPermittedLanguages() {
        return permittedLanguages.clone();
    }

    public GraalPerformerContainer setPermittedLanguages(String... permittedLanguages) {
        Objects.requireNonNull(permittedLanguages, "Null permittedLanguages");
        this.permittedLanguages = permittedLanguages.clone();
        return this;
    }

    public GraalContextCustomizer getCustomizer() {
        return customizer;
    }

    public GraalPerformerContainer setCustomizer(GraalContextCustomizer customizer) {
        this.customizer = Objects.requireNonNull(customizer, "Null customizer");
        return this;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public GraalPerformerContainer setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public String getAutoBindingLanguage() {
        return autoBindingLanguage;
    }

    /**
     * Sets language for bindings. Should be set, if you want to use some predefined bindings.
     *
     * @param autoBindingLanguage new current language; <code>null</code> by default.
     * @return reference to this object.
     */
    public GraalPerformerContainer setAutoBindingLanguage(String autoBindingLanguage) {
        this.autoBindingLanguage = autoBindingLanguage;
        return this;
    }

    public GraalPerformerContainer setAutoBindingJS() {
        return setAutoBindingLanguage(GraalSourceContainer.JAVASCRIPT_LANGUAGE);
    }

    public GraalPerformerConfigurator getConfigurator() {
        return configurator;
    }

    public GraalPerformerContainer setConfigurator(GraalPerformerConfigurator configurator) {
        this.configurator = Objects.requireNonNull(configurator, "Null configurator");
        return this;
    }

    public Supplier<Context> getContextSupplier() {
        return contextSupplier;
    }

    /**
     * Sets custom supplier of JEP interpreter. In this case, the parameters
     * {@link #setPermittedLanguages(String...) permittedLanguages},
     * {@link #setCustomizer(GraalContextCustomizer) customizer},
     * {@link #setWorkingDirectory(Path) workingDirectory} will be ignored.
     *
     * @param customContextSupplier custom supplier.
     * @return reference to this object.
     */
    public GraalPerformerContainer setContextSupplier(Supplier<Context> customContextSupplier) {
        this.contextSupplier = Objects.requireNonNull(customContextSupplier, "Null customContextSupplier");
        return this;
    }

    /**
     * Returns performer, stored in this container for the given context ID.
     *
     * <p><b>Important:</b> usually you <b>should not</b> close the returned performed, for example,
     * via <code>try-with-resources</code> statement. Instead, you should use {@link #freeResources(boolean)} method
     * of this container.</p>
     *
     * <p>Note: this performer will be shared (under the same ID) among <b>all</b> containers, created in this JVM.
     * If there is a performer with this ID, already created by any container, it will be returned.
     * So, we recommend guaranteeing (by the external logic) that all containers that create performers
     * by this method with the same ID will have identical settings
     * (like {@link #setCustomizer(GraalContextCustomizer) customizer}).
     *
     * <p>Note: for local container the argument will be ignored.
     *
     *
     * @param contextId ID of the context.
     * @return stored performer.
     */
    public abstract GraalPerformer performer(Object contextId);

    @Override
    public String toString() {
        return "Graal " + typeName() + " performer container, customization: " + customizer;
    }

    /**
     * Frees the resources.
     *
     * <p>For {@link Local} container, it just frees the only contained performer
     * by the call {@link GraalPerformer#close()}.
     *
     * <p>For {@link Shared} container, behavior depends on the argument.
     * If it is <code>false</code>, it does nothing;
     * this may be important, because the same performer may be shared with other containers.
     * Then performers will be automatically closed by Java cleaner in {@link GraalPerformer}.
     * If it is <code>true</code>, this method frees the <b>last</b> performer accessed
     * by {@link #performer(Object)} method.
     * It may be suitable if you are sure that after this moment no one can need the same shared performer.
     */
    public abstract void freeResources(boolean freeSharedContexts);

    public static int numberOfStoredPerformers() {
        return ContextKey.numberOfStoredPerformers();
    }

    abstract String typeName();

    private void resetSupplier() {
        this.contextSupplier = this::supplyDefaultContext;
    }

    GraalPerformer createAndInitialize(Object contextId) {
        final Context context = contextSupplier.get();
        final GraalPerformer result = GraalPerformer.newPerformer(context, autoBindingLanguage);
        assert customizer != null;
        result.setCustomizerInfo(customizer.toString());
        if (contextId != null) {
            result.setContextIdInfo(contextId.toString());
            // - passing only String objects: they are absolutely safe regarding garbage collection
        }
        configurator.configure(result);
        return result;
    }

    private Context supplyDefaultContext() {
        final Context.Builder builder = customizer.newBuilder(permittedLanguages);
        if (workingDirectory != null) {
            builder.currentWorkingDirectory(workingDirectory);
        }
        return builder.build();
    }

    public static class Local extends GraalPerformerContainer implements AutoCloseable {
        private volatile GraalPerformer performer = null;

        Local() {
            super();
        }

        @Override
        public Local setPermittedLanguages(String... permittedLanguages) {
            return (Local) super.setPermittedLanguages(permittedLanguages);
        }

        @Override
        public Local setCustomizer(GraalContextCustomizer customizer) {
            return (Local) super.setCustomizer(customizer);
        }

        @Override
        public Local setWorkingDirectory(Path workingDirectory) {
            return (Local) super.setWorkingDirectory(workingDirectory);
        }

        @Override
        public Local setAutoBindingLanguage(String autoBindingLanguage) {
            return (Local) super.setAutoBindingLanguage(autoBindingLanguage);
        }

        @Override
        public Local setAutoBindingJS() {
            return (Local) super.setAutoBindingJS();
        }

        @Override
        public Local setConfigurator(GraalPerformerConfigurator configurator) {
            return (Local) super.setConfigurator(configurator);
        }

        @Override
        public Local setContextSupplier(Supplier<Context> customContextSupplier) {
            return (Local) super.setContextSupplier(customContextSupplier);
        }

        @Override
        public GraalPerformer performer(Object contextId) {
            return performer();
        }

        public GraalPerformer performer() {
            GraalPerformer performer = this.performer;
            if (performer == null) {
                this.performer = performer = createAndInitialize(null);
                LOG.log(Logger.Level.DEBUG, "Created new local " + performer);
            }
            return performer;
        }

        @Override
        public void freeResources(boolean freeSharedContexts) {
            close();
        }

        public void freeResources() {
            close();
        }

        @Override
        public void close() {
            String message = null;
            GraalPerformer performer = this.performer;
            if (performer != null) {
                message = performer.toString();
                performer.close();
                this.performer = null;
            }
            if (message != null) {
                LOG.log(Logger.Level.DEBUG, "Closed local " + message);
            }
        }

        @Override
        String typeName() {
            return "local";
        }
    }

    public static class Shared extends GraalPerformerContainer {
        private ActionOnChangeContextId actionOnChangeContextId = ActionOnChangeContextId.THROW_EXCEPTION;
        private ContextKey contextKey = null;

        private final Object lock = new Object();

        Shared() {
            super();
        }

        public ActionOnChangeContextId getActionOnChangeContextId() {
            return actionOnChangeContextId;
        }

        public Shared setActionOnChangeContextId(ActionOnChangeContextId actionOnChangeContextId) {
            this.actionOnChangeContextId = Objects.requireNonNull(
                    actionOnChangeContextId, "Null actionOnChangeContextId");
            return this;
        }

        @Override
        public Shared setCustomizer(GraalContextCustomizer customizer) {
            return (Shared) super.setCustomizer(customizer);
        }

        @Override
        public Shared setPermittedLanguages(String... permittedLanguages) {
            return (Shared) super.setPermittedLanguages(permittedLanguages);
        }

        @Override
        public Shared setWorkingDirectory(Path workingDirectory) {
            return (Shared) super.setWorkingDirectory(workingDirectory);
        }

        @Override
        public Shared setAutoBindingLanguage(String autoBindingLanguage) {
            return (Shared) super.setAutoBindingLanguage(autoBindingLanguage);
        }

        @Override
        public Shared setAutoBindingJS() {
            return (Shared) super.setAutoBindingJS();
        }

        @Override
        public Shared setConfigurator(GraalPerformerConfigurator configurator) {
            return (Shared) super.setConfigurator(configurator);
        }

        @Override
        public Shared setContextSupplier(Supplier<Context> customContextSupplier) {
            return (Shared) super.setContextSupplier(customContextSupplier);
        }

        @Override
        public GraalPerformer performer(Object contextId) {
            Objects.requireNonNull(contextId, "Null context ID");
            synchronized (lock) {
                if (contextKey == null || !contextId.equals(contextKey.contextId)) {
                    if (contextKey != null) {
                        actionOnChangeContextId.onChangeId(contextKey, contextId);
                    }
                    contextKey = new ContextKey(contextId);
                }
                return contextKey.getPerformer();
            }
        }

        /**
         * @apiNote Will be automatically revived while usage after closing.
         */

        @Override
        public void freeResources(boolean freeSharedContexts) {
            if (freeSharedContexts) {
                synchronized (lock) {
                    if (contextKey != null) {
                        contextKey.close();
                        contextKey = null;
                    }
                }
            }
        }

        @Override
        String typeName() {
            return "shared";
        }
    }

    // This "extra" trivial class is necessary for correct work of WeakHashMap:
    // its keys cannot be contextId itself, they should be something like ContextKey,
    // which will be removed by garbage collector together with container.
    private final class ContextKey {
        private static final Map<ContextKey, GraalPerformer> performers = new WeakHashMap<>();

        private final Object contextId;

        ContextKey(Object contextId) {
            this.contextId = Objects.requireNonNull(contextId, "Null JavaScript context ID");
        }

        GraalPerformer getPerformer() {
            GraalPerformer newPerformer;
            synchronized (performers) {
                // We don't use computeIfAbsent to avoid logging inside synchronized section
                final GraalPerformer performer = performers.get(this);
                if (performer != null) {
                    return performer;
                }
                newPerformer = createAndInitialize(contextId);
                performers.put(this, newPerformer);
            }
            LOG.log(Logger.Level.INFO, "Created new shared " + newPerformer);
            return newPerformer;
        }

        private void close() {
            String message = null;
            synchronized (performers) {
                final GraalPerformer performer = performers.remove(this);
                if (performer != null) {
                    message = performer.toString();
                    performer.close();
                }
            }
            if (message != null) {
                LOG.log(Logger.Level.INFO, "Closed shared " + message);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ContextKey that = (ContextKey) o;
            return contextId.equals(that.contextId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextId);
        }

        private static int numberOfStoredPerformers() {
            synchronized (performers) {
                return performers.size();
            }
        }
    }
}
