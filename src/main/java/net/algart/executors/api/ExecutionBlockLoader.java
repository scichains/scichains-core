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

package net.algart.executors.api;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExecutorJsonSet;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.System.Logger;

/**
 * Loader of {@link ExecutionBlock executors}. Every kinds of executors, like sub-chains, settings combiners,
 * Python function use their own loaders.
 *
 * <p>In current version, all they are instances of {@link SimpleExecutionBlockLoader}.
 */
public class ExecutionBlockLoader {
    private static final boolean REGISTER_BUILT_IN_EXECUTORS = true;
    // - Must be true since 15 Aug 2022. It helps external system to see modification,
    // made by Java in standard built-in executor JSONs.

    private static final Logger LOG = System.getLogger(ExecutionBlockLoader.class.getName());

    final Map<String, Map<String, String>> allModels = new LinkedHashMap<>();
    // - sessionId -> Map(executorId -> executorJson)

    private final String name;

    public ExecutionBlockLoader(String name) {
        this.name = Objects.requireNonNull(name, "Null loader name");
    }

    public String name() {
        return name;
    }

    /**
     * Actually loads and instantiate new execution block.
     * Default implementation returns <code>null</code>.
     *
     * <p>Note: if this loader use sessionId, if MUST always check also
     * {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.
     *
     * @param sessionId             see the same argument of {@link ExecutionBlock#newExecutionBlock};
     *                              may be ignored.
     * @param executorId            see the same argument of {@link ExecutionBlock#newExecutionBlock}.
     * @param executorSpecification see the same argument of {@link ExecutionBlock#newExecutionBlock}.
     * @return newly created executor or <code>null</code> if this loader does not "understand" such JSON.
     * @throws ClassNotFoundException if Java class, required for creating executing block,
     *                                is not available in the current <code>classpath</code> environment.
     */
    public ExecutionBlock newExecutionBlock(String sessionId, String executorId, String executorSpecification)
            throws ClassNotFoundException {
        return null;
    }

    /**
     * Removes all executors, dynamically created for the given session,
     * probably stored in some tables by this loaders for dynamic usage,
     * and frees some caches, necessary for such executors.
     * Default implementation calls {@link #removeSessionExecutorModels(String)}.
     *
     * @param sessionId unique ID of current session.
     * @throws NullPointerException if <code>sessionId==null</code>.
     */
    public void clearSession(String sessionId) {
        removeSessionExecutorModels(sessionId);
    }

    /**
     * Returns executors' descriptions (probable JSONs, like in
     * {@link ExecutorJson})
     * for all executors, dynamically created by this loader for the given session.
     * Keys in the result are ID of every executor, values are descriptions.
     *
     * @param sessionId unique ID of current session.
     * @return executors' descriptions for executors, created by this loader.
     * @throws NullPointerException if <code>sessionId==null</code>.
     */
    public final Map<String, String> availableExecutorModelDescriptions(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allModels) {
            return Collections.unmodifiableMap(allModels.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()));
        }
    }

    /**
     * Returns <tt>{@link #availableExecutorModelDescriptions(String)
     * availableExecutorModelDescriptions}(sessionId).get(executorId)</tt>,
     * but works quickly (without creating new map).
     *
     * @param sessionId  unique ID of current session.
     * @param executorId unique ID of this executor in the system.
     * @return description of this dynamic executor (probably JSON).
     * @throws NullPointerException if one of arguments is <code>null</code>.
     */
    public final String getExecutorModelDescription(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (allModels) {
            return allModels.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()).get(executorId);
        }
    }

    public final void setExecutorModelDescription(String sessionId, String executorId, String modelDescription) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(modelDescription, "Null modelDescription");
        synchronized (allModels) {
            allModels.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()).put(executorId, modelDescription);
        }
    }

    public final boolean removeExecutorModel(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (allModels) {
            return allModels.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()).remove(executorId) != null;
        }
    }

    public final void removeSessionExecutorModels(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allModels) {
            Map<String, String> models = allModels.get(sessionId);
            if (models != null) {
                models.clear();
            }
        }
    }

    @Override
    public String toString() {
        return name + " (" + allModels.size() + " sessions)";
    }

    // Note: this loader is usually enough for actual creating executors,
    // because non-Java executors are usually implemented via standard Java executor, which performs their tasks.
    // This loader DOES NOT use sessionId (Java classes are shared among all JVM).
    static class StandardJavaExecutorLoader extends ExecutionBlockLoader {
        private final Map<String, Executable> newInstanceMakers = new HashMap<>();

        StandardJavaExecutorLoader() {
            super("standard Java executors loader");
        }

        void registerAllStandardModels() {
            if (REGISTER_BUILT_IN_EXECUTORS) {
                final long t1 = System.nanoTime();
                final Map<String, String> allStandardModels = new LinkedHashMap<>();
                for (ExecutorJson executorJson : ExecutorJsonSet.allBuiltIn().all()) {
                    allStandardModels.put(executorJson.getExecutorId(), executorJson.toJson().toString());
                }
                final long t2 = System.nanoTime();
                LOG.log(System.Logger.Level.INFO, () -> String.format(Locale.US,
                        "Storing descriptions of installed built-in executor models: %.3f ms",
                        (t2 - t1) * 1e-6));
                allModels.put(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, allStandardModels);
            }
        }

        @Override
        public ExecutionBlock newExecutionBlock(
                String ignoredSessionId,
                String executorId,
                String executorSpecification)
                throws ClassNotFoundException {
            Objects.requireNonNull(executorId, "Null executorId");
            Objects.requireNonNull(executorSpecification, "Null executorJson");
            final Executable newInstance = findNewInstance(executorId, executorSpecification);
            if (newInstance == null) {
                return null;
            }
            final Object result;
            try {
                if (!(newInstance instanceof Method)) {
                    result = ((Constructor<?>) newInstance).newInstance();
                } else {
                    result = ((Method) newInstance).invoke(null);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new JsonException(
                        (newInstance instanceof Method ? newInstance.getName() + "() method" : "Constructor")
                                + " of " + newInstance.getDeclaringClass()
                                + " cannot create new instance <<<" + executorSpecification + ">>>", e);
            }
            if (!(result instanceof ExecutionBlock)) {
                throw new JsonException("Object, created by "
                        + (newInstance instanceof Method ? newInstance.getName() + "() method" : "constructor")
                        + " of " + newInstance.getDeclaringClass()
                        + ", is NOT AN EXECUTOR\n    (it is "
                        + (result == null ? "null" : result.getClass().getName())
                        + " and does not extend ExecutionBlock class)\n    <<<" + executorSpecification + ">>>");
            }
            return (ExecutionBlock) result;
        }

        @Override
        public void clearSession(String sessionId) {
            // No sense to free the cache, because it corresponds to system class loader and cannot become obsolete.
        }

        private Executable findNewInstance(String executorId, String executorJsonString) throws ClassNotFoundException {
            synchronized (newInstanceMakers) {
                if (newInstanceMakers.containsKey(executorId)) {
                    return newInstanceMakers.get(executorId);
                }
                Executable executable = getNewInstance(executorJsonString);
                newInstanceMakers.put(executorId, executable);
                return executable;
            }
        }

        private static Executable getNewInstance(String executorJsonString) throws ClassNotFoundException {
            try (final JsonReader reader = Json.createReader(new StringReader(executorJsonString))) {
                final JsonObject executorJson = reader.readObject();
                final JsonObject javaConf = executorJson.getJsonObject(ExecutorJson.JavaConf.JAVA_CONF_NAME);
                if (javaConf == null) {
                    return null; // unknown format
                }
                // We prefer not to use ExecutorJson.JavaConf class:
                // this method is very low-level
                final String className = javaConf.getString(
                        ExecutorJson.JavaConf.CLASS_PROPERTY_NAME, null);
                if (className == null) {
                    return null; // unknown format
                }
                final Class<?> executorClass = Class.forName(className);
                final String newInstanceMethodName = javaConf.getString(
                        ExecutorJson.JavaConf.NEW_INSTANCE_METHOD_PROPERTY_NAME, null);
                try {
                    if (newInstanceMethodName != null) {
                        return executorClass.getMethod(newInstanceMethodName);
                    } else {
                        return executorClass.getConstructor();
                    }
                } catch (NoSuchMethodException e) {
                    throw new JsonException("Cannot find "
                            + (newInstanceMethodName != null ?
                            "public static method " + newInstanceMethodName + "() without parameters"
                            : "default public constructor")
                            + " in class " + className + " <<<" + executorJsonString + ">>>", e);
                }
            }
        }
    }
}
