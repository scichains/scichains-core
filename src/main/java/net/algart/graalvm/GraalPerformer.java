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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.lang.System.Logger;
import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GraalPerformer implements AutoCloseable {
    private static final Logger LOG = System.getLogger(GraalPerformer.class.getName());

    private static final Cleaner CLEANER = Cleaner.create();

    private final ExpensiveCleanableState state;
    private final Cleaner.Cleanable cleanable;

    private GraalPerformer(Context context, String autoBindingLanguage) {
        this.state = new ExpensiveCleanableState(context, autoBindingLanguage);
        this.cleanable = CLEANER.register(this, state);
    }

    public static GraalPerformer newPerformer(Context context) {
        return newPerformer(context, null);
    }

    public static GraalPerformer newPerformer(Context context, String autoBindingLanguage) {
        return new GraalPerformer(context, autoBindingLanguage);
    }

    public Context context() {
        checkClosed();
        return state.context;
    }

    public String autoBindingLanguage() {
        return state.autoBindingLanguage;
    }

    public boolean hasAutoBindings() {
        return state.autoBindings != null;
    }

    public Value autoBindings() {
        checkClosed();
        return state.autoBindings;
    }

    public Value bindingsJS() {
        return bindings(GraalSourceContainer.JAVASCRIPT_LANGUAGE);
    }

    public Value bindings(String language) {
        Objects.requireNonNull(language, "Null language for binding");
        checkClosed();
        return language.equals(state.autoBindingLanguage) ? autoBindings() : state.context.getBindings(language);
    }

    public Map<String, Object> properties() {
        checkClosed();
        return Collections.unmodifiableMap(state.properties);
    }

    public <T> T getProperty(String key, Class<T> propertyClass) {
        Objects.requireNonNull(key, "Null key");
        Objects.requireNonNull(propertyClass, "Null propertyClass");
        checkClosed();
        final Object object = state.properties.get(key);
        if (object != null && !propertyClass.isInstance(object)) {
            throw new IllegalStateException("Performer property \"" + key + "\" has invalid class "
                    + object.getClass().getName() + ": " + propertyClass + " is expected");
        }
        return propertyClass.cast(object);
    }

    public Object putProperty(String key, Object value) {
        Objects.requireNonNull(key, "Null key");
        Objects.requireNonNull(value, "Null value");
        checkClosed();
        return state.properties.put(key, value);
    }

    public Object putPropertyIfAbsent(String key, Object value) {
        Objects.requireNonNull(key, "Null key");
        Objects.requireNonNull(value, "Null value");
        checkClosed();
        return state.properties.putIfAbsent(key, value);
    }

    public Value performJS(CharSequence source) {
        return perform(GraalSourceContainer.JAVASCRIPT_LANGUAGE, source);
    }

    public Value perform(String language, CharSequence source) {
        checkClosed();
        return state.context.eval(language, source);
    }

    public Value perform(Source source) {
        checkClosed();
        return state.context.eval(source);
    }

    public Value perform(GraalSourceContainer sourceContainer) {
        return perform(sourceContainer.source());
    }

    public boolean isClosed() {
        return state.isReallyClosed();
    }

    @Override
    public void close() {
        state.normallyClosed = true;
        cleanable.clean();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    void setCustomizerInfo(String customizerToString) {
        state.customizerToString = customizerToString;
    }

    void setContextIdInfo(String contextId) {
        state.contextId = contextId;
    }

    private void checkClosed() {
        if (state.normallyClosed || state.isReallyClosed()) {
            // - will be true even WHILE executing cleanable.clean
            throw new IllegalStateException("Cannot use " + this);
        }
    }

    private static class ExpensiveCleanableState implements Runnable {
        private final String contextString;
        private final String autoBindingLanguage;
        private volatile boolean normallyClosed = false;
        private volatile Context context;
        private Value autoBindings;
        private Map<String, Object> properties;
        private String contextId = null;
        private String customizerToString = null;
        // - last 2 fields are only for better toString

        ExpensiveCleanableState(Context context, String autoBindingLanguage) {
            this.context = Objects.requireNonNull(context, "Null context");
            this.contextString = context.toString();
            this.autoBindingLanguage = autoBindingLanguage;
            this.autoBindings = autoBindingLanguage == null ? null : context.getBindings(autoBindingLanguage);
            this.properties = new LinkedHashMap<>();
        }

        @Override
        public void run() {
            if (isReallyClosed()) {
                return;
            }
            if (!normallyClosed) {
                LOG.log(System.Logger.Level.WARNING, () -> "CLEANING " + toBriefString());
            }
            context.close();
            context = null;
            autoBindings = null;
            properties = null;
        }

        @Override
        public String toString() {
            return (!isReallyClosed() ? "" : normallyClosed ? "closed " : "abnormally CLEANED ") + toBriefString();
        }

        private String toBriefString() {
            return "Graal performer " +
                    "of " + contextString +
                    (customizerToString == null ? "" : " (customization: " + customizerToString) + ")" +
                    (contextId == null ? "" : " in context #" + contextId);
        }

        private boolean isReallyClosed() {
            return context == null;
        }
    }
}
