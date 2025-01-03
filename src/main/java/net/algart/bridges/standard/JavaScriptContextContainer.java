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

package net.algart.bridges.standard;

import javax.script.ScriptEngine;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class JavaScriptContextContainer {
    // In the future, this class may be reworked to direct using GraalVM Context instead of ScriptEngine

    private volatile ContextKey contextKey = null;
    private volatile ScriptEngine context = null;

    private JavaScriptContextContainer() {
    }

    public static JavaScriptContextContainer getInstance() {
        return new JavaScriptContextContainer();
    }

    public ScriptEngine getContext(boolean shareNamespace, Object contextId) {
        if (shareNamespace) {
            if (contextKey == null) {
                contextKey = new ContextKey(contextId);
            }
            return contextKey.getContext();
        } else {
            return getLocalContext();
        }
    }

    public ScriptEngine getLocalContext() {
        if (context == null) {
            context = ScriptEngineTools.newEngine();
        }
        contextKey = null;
        // - necessary to allow to free unnecessary script engines
        return context;
    }

    public static int numberOfStoredScriptEngines() {
        return ContextKey.numberOfStoredContexts();
    }

    private static final class ContextKey {
        private static final Map<ContextKey, ScriptEngine> contexts = new WeakHashMap<>();

        private final Object contextId;

        ContextKey(Object contextId) {
            this.contextId = Objects.requireNonNull(contextId, "Null JavaScript context ID");
        }

        ScriptEngine getContext() {
            synchronized (contexts) {
                return contexts.computeIfAbsent(this, k -> {
                    final ScriptEngine context = ScriptEngineTools.doNewEngine();
                    JavaScriptPerformer.LOG.log(System.Logger.Level.INFO,
                            "Creating new shared JavaScript engine for context #"
                            + contextId + ": " + context.getClass());
                    return context;
                });
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
            return Objects.equals(contextId, that.contextId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextId);
        }

        private static int numberOfStoredContexts() {
            synchronized (contexts) {
                return contexts.size();
            }
        }

    }
}
