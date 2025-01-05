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

package net.algart.executors.api.system;

import jakarta.json.JsonException;
import net.algart.executors.api.ExecutionBlock;

import java.lang.System.Logger;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Loader of {@link ExecutionBlock executors}. Every kind of executors, like sub-chains, settings combiners,
 * Python function uses their own loaders.
 *
 * <p>In current version, all they are instances of {@link DefaultExecutorLoader}.
 *
 * @see ExecutorLoaderSet#globalExecutorLoaders()
 */
public abstract class ExecutorLoader {
    private static final boolean REGISTER_BUILT_IN_EXECUTORS = true;
    // - Must be true since 15 Aug 2022. It helps external system to see modification,
    // made by Java in standard built-in executor JSONs.

    private static final Logger LOG = System.getLogger(ExecutorLoader.class.getName());

    final Map<String, Map<String, String>> allSpecifications = new LinkedHashMap<>();
    // - sessionId -> Map(executorId -> executorSpecification)

    private final String name;

    public ExecutorLoader(String name) {
        this.name = Objects.requireNonNull(name, "Null loader name");
    }

    public static ExecutorLoader getStandardJavaExecutorLoader() {
        return new StandardJavaExecutorLoader();
    }

    public String name() {
        return name;
    }

    /**
     * Actually loads and instantiate new execution block.
     *
     * <p>If this method returns <code>null</code>, the system will ignore this loader and try another one.
     *
     * <p>Note: if this loader uses sessionId, if MUST always check also
     * {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.
     *
     * @param sessionId     see the same argument of {@link ExecutionBlock#newExecutor};
     *                      may be ignored.
     * @param executorId    see the same argument of {@link ExecutionBlock#newExecutor}.
     * @param specification see the same argument of {@link ExecutionBlock#newExecutor}.
     * @return newly created executor or <code>null</code> if this loader does not "understand" this specification.
     * @throws ClassNotFoundException if Java class, required for creating executing block,
     *                                is not available in the current <code>classpath</code> environment.
     */
    public abstract ExecutionBlock loadExecutor(
            String sessionId, String executorId, ExecutorSpecification specification)
            throws ClassNotFoundException;

    /**
     * Removes all executors, dynamically created for the given session,
     * probably stored in some tables by this loader for dynamic usage,
     * and frees some caches, necessary for such executors.
     * Default implementation calls {@link #removeSessionSpecifications(String)}.
     *
     * @param sessionId unique ID of current session.
     * @throws NullPointerException if <code>sessionId==null</code>.
     */
    public void clearSession(String sessionId) {
        removeSessionSpecifications(sessionId);
    }

    /**
     * Returns executor specifications for all executors, registered by this loader
     * for the given session, in a serialized (string) form.
     * Keys in the result are IDs of executors, values are serialized specifications.
     *
     * @param sessionId unique ID of current session.
     * @return specifications for all executors, created by this loader.
     * @throws NullPointerException if <code>sessionId==null</code>.
     */
    public final Map<String, String> serializedSessionSpecifications(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allSpecifications) {
            return Collections.unmodifiableMap(
                    allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()));
        }
    }

    /**
     * Returns the executor specification, registered for the specified session ID and executor ID.
     *
     * @param sessionId  unique ID of current session.
     * @param executorId unique ID of this executor in the system.
     * @return specification of the executor or <code>null</code> if there is no such executor.
     * @throws NullPointerException if one of arguments is <code>null</code>.
     */
    public final ExecutorSpecification getSpecification(String sessionId, String executorId) {
        final String serialized = serializedSpecification(sessionId, executorId);
        if (serialized == null) {
            return null;
        }
        try {
            return ExecutorSpecification.valueOf(serialized);
        } catch (JsonException e) {
            throw new AssertionError("Very strange: all registered specification " +
                    "were serialized via toJson().toString()!", e);
        }

    }

    public final String serializedSpecification(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (allSpecifications) {
            return allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>())
                    .get(executorId);
        }
    }

    /**
     * Registers new executor according to the specification with its ID
     * {@link ExecutorSpecification#getExecutorId() specification.getExecutorId()}.
     * If this specification is already present, this method overrides it with the new value.
     *
     * <p>This method is called from {@link DefaultExecutorLoader#registerWorker} method.</p>
     *
     * @param sessionId     unique ID of current session.
     * @param specification new specification.
     */
    public final void setSpecification(String sessionId, ExecutorSpecification specification) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(specification, "Null specification");
        synchronized (allSpecifications) {
            allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>())
                    .put(specification.getExecutorId(), specification.toJson().toString());
        }
    }

    public final void setSpecifications(String sessionId, Collection<ExecutorSpecification> specifications) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(specifications, "Null specifications");
        synchronized (allSpecifications) {
            final var serialized = allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>());
            for (ExecutorSpecification specification : specifications) {
                Objects.requireNonNull(specification, "Null specification in the collection");
                serialized.put(specification.getExecutorId(), specification.toJson().toString());
            }
        }
    }

    public final void addAllStandardJavaExecutorSpecifications() {
        if (REGISTER_BUILT_IN_EXECUTORS) {
            final long t1 = System.nanoTime();
            final var allStandard = ExecutorSpecificationSet.allBuiltIn().all();
            final long t2 = System.nanoTime();
            LOG.log(Logger.Level.INFO, () -> String.format(Locale.US,
                    "Storing descriptions of installed built-in executor models: %.3f ms",
                    (t2 - t1) * 1e-6));
            setSpecifications(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, allStandard);
        }
    }

    public final boolean removeSpecification(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (allSpecifications) {
            return allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>())
                    .remove(executorId) != null;
        }
    }

    public final void removeSessionSpecifications(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allSpecifications) {
            Map<String, String> models = allSpecifications.get(sessionId);
            if (models != null) {
                models.clear();
            }
        }
    }

    @Override
    public String toString() {
        return name + " (" + allSpecifications.size() + " sessions)";
    }

    // Note: this loader is usually enough for actual creating executors,
    // because non-Java executors are usually implemented via standard Java executor, which performs their tasks.
    // This loader DOES NOT use sessionId (Java classes are shared among all JVM).
    private static class StandardJavaExecutorLoader extends ExecutorLoader {
        private final Map<String, Executable> newInstanceMakers = new HashMap<>();

        StandardJavaExecutorLoader() {
            super("standard Java executors loader");
        }

        @Override
        public ExecutionBlock loadExecutor(
                String ignoredSessionId,
                String executorId,
                ExecutorSpecification specification)
                throws ClassNotFoundException {
            Objects.requireNonNull(executorId, "Null executorId");
            Objects.requireNonNull(specification, "Null specification");
            final Executable newInstance = findNewInstance(executorId, specification);
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
                                + " cannot create new instance <<<" + specification + ">>>", e);
            }
            if (!(result instanceof ExecutionBlock)) {
                throw new JsonException("Object, created by "
                        + (newInstance instanceof Method ? newInstance.getName() + "() method" : "constructor")
                        + " of " + newInstance.getDeclaringClass()
                        + ", is NOT AN EXECUTOR\n    (it is "
                        + (result == null ? "null" : result.getClass().getName())
                        + " and does not extend ExecutionBlock class)\n    <<<" + specification + ">>>");
            }
            return (ExecutionBlock) result;
        }

        @Override
        public void clearSession(String sessionId) {
            // No sense to free the cache, because it corresponds to system class loader and cannot become obsolete.
        }

        private Executable findNewInstance(String executorId, ExecutorSpecification specification) throws ClassNotFoundException {
            synchronized (newInstanceMakers) {
                if (newInstanceMakers.containsKey(executorId)) {
                    return newInstanceMakers.get(executorId);
                }
                Executable executable = getNewInstance(specification);
                newInstanceMakers.put(executorId, executable);
                return executable;
            }
        }

        private static Executable getNewInstance(ExecutorSpecification specification) throws ClassNotFoundException {
            // Note: we do not require specification.isJavaExecutor()
            // This allows to minimize requirements to a minimal JSON specification
            ExecutorSpecification.JavaConf javaConf = specification.getJava();
            if (javaConf == null) {
                return null;
            }
            final String className = javaConf.getClassName();
            if (className == null) {
                return null;
            }
            final Class<?> executorClass = Class.forName(className);
            // - We cannot use javaConf.executorClass(): that method catches ClassNotFoundException,
            // but we must not do this here
            final String newInstanceMethodName = javaConf.getNewInstanceMethod();
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
                        + " in class " + className + " <<<" + specification + ">>>", e);
            }
        }
    }
}
