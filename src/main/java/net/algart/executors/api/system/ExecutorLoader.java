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
 * Loader of {@link ExecutionBlock executors}. Every kind of executors, like sub-chains, settings,
 * Python function uses their own loaders.
 *
 * <p>This class registers new executor specifications by
 * {@link #setSpecification(String, ExecutorSpecification)} method.
 * The registered specifications are stored inside an internal two-level table:
 * a map of sessions, where every session is a map of executor specifications.
 * Specifications are stored in some immutable serialized form, probable strings:
 * this helps to avoid problems while concurrent accessing from different chains,
 * executed in parallel threads.
 *
 * <p>In the current version, all they are instances of {@link DefaultExecutorLoader}.</p>
 *
 * @see ExecutorLoaderSet
 * @see ExecutionBlock#globalLoaders()
 */
public abstract class ExecutorLoader {
    private static final boolean REGISTER_BUILT_IN_EXECUTORS = true;
    // - Must be true since 15 Aug 2022. It helps external system to see modification,
    // made by Java in standard built-in executor JSONs.

    private static final Logger LOG = System.getLogger(ExecutorLoader.class.getName());

    private final Map<String, Map<String, String>> allSpecifications = new LinkedHashMap<>();
    // - This map is: sessionId -> Map(executorId -> executorSpecification)
    private final Map<String, Executable> newInstanceMakers = new HashMap<>();

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
     * <p>Note: <code>sessionId</code> is typically used only to store it into the returned executor
     * according {@link InstantiationMode}. Usually this does not affect creating a class instance.
     *
     * @param sessionId         unique non-empty ID of current session while multi-session usage;
     *                          may be <code>null</code> while simple usage.
     * @param specification     specification of the executor.
     * @param instantiationMode what should we do after successful instantiating the executor?
     * @return newly created executor or <code>null</code> if this loader does not "understand" this specification.
     * @throws NullPointerException   if <code>specification</code> or <code>instantiationMode</code> is
     *                                <code>null</code>.
     * @throws ClassNotFoundException if Java class, required for creating the executor,
     *                                is not available in the current <code>classpath</code> environment.
     */
    public ExecutionBlock loadExecutor(
            String sessionId,
            ExecutorSpecification specification,
            InstantiationMode instantiationMode)
            throws ClassNotFoundException {
        Objects.requireNonNull(specification, "Null specification");
        Objects.requireNonNull(instantiationMode, "Null instantiationMode)");
        final ExecutionBlock executor = loadExecutor(sessionId, specification);
        if (executor != null) {
            instantiationMode.customizeExecutor(executor, sessionId, specification);
        }
        return executor;
    }

    protected abstract ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification)
            throws ClassNotFoundException;

    protected final ExecutionBlock loadStandardJavaExecutor(
            String ignoredSessionId,
            ExecutorSpecification specification)
            throws ClassNotFoundException {
        Objects.requireNonNull(specification, "Null specification");
        final Executable newInstance = findNewInstance(specification);
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
            throw new IllegalStateException("Object, created by "
                    + (newInstance instanceof Method ? newInstance.getName() + "() method" : "constructor")
                    + " of " + newInstance.getDeclaringClass()
                    + ", is NOT AN EXECUTOR\n    (it is "
                    + (result == null ? "null" : result.getClass().getName())
                    + " and does not extend ExecutionBlock class)\n    <<<" + specification + ">>>");
        }
        return (ExecutionBlock) result;
    }


    /**
     * Removes all executors, dynamically created for the given session,
     * probably stored in some tables by this loader for dynamic usage,
     * and frees some caches, necessary for such executors.
     * Default implementation calls {@link #removeSpecifications(String)}.
     *
     * <p>Note: <code>sessionId</code> can be <code>null</code> or an empty string;
     * in these cases, this function does nothing (such sessions never exist).</p>
     *
     * @param sessionId unique ID of current session.
     */
    public void clearSession(String sessionId) {
        if (sessionId != null) {
            removeSpecifications(sessionId);
        }
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
    public final Map<String, String> allSerializedSpecifications(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allSpecifications) {
            // Note: Collections.unmodifiableMap is not enough, it will not be synchronized well
            final Map<String, String> session = allSpecifications.get(sessionId);
            return session == null ? new LinkedHashMap<>() : new LinkedHashMap<>(session);
        }
    }

    public Set<String> allExecutorIds(String sessionId) {
        return allSerializedSpecifications(sessionId).keySet();
    }

    public Set<String> allSessionIds() {
        synchronized (allSpecifications) {
            return new LinkedHashSet<>(allSpecifications.keySet());
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
            return ExecutorSpecification.of(serialized);
        } catch (JsonException e) {
            throw new AssertionError("Very strange: all registered specification " +
                    "were serialized via toJson().toString()!", e);
        }
    }

    public final String serializedSpecification(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (allSpecifications) {
            final Map<String, String> session = allSpecifications.get(sessionId);
            return session == null ? null : session.get(executorId);
        }
    }

    /**
     * Registers new executor according to the specification with its ID
     * {@link ExecutorSpecification#getId()}.
     * If this specification is already present, this method overrides it with the new value.
     *
     * <p>This method is called from {@link DefaultExecutorLoader#registerWorker} method.</p>
     *
     * @param sessionId     unique non-empty ID of current session.
     * @param specification new specification.
     */
    public final void setSpecification(String sessionId, ExecutorSpecification specification) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(specification, "Null specification");
        checkEmptySessionId(sessionId);
        final String serialized = specification.toJson().toString().intern();
        synchronized (allSpecifications) {
            allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>())
                    .put(specification.getId(), serialized);
        }
    }

    public final void setSpecifications(String sessionId, Collection<ExecutorSpecification> specifications) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(specifications, "Null specifications");
        checkEmptySessionId(sessionId);
        synchronized (allSpecifications) {
            final var serialized = allSpecifications.computeIfAbsent(sessionId, k -> new LinkedHashMap<>());
            for (ExecutorSpecification specification : specifications) {
                Objects.requireNonNull(specification, "Null specification in the collection");
                serialized.put(specification.getId(), specification.toJson().toString().intern());
                // - in fact, intern() method is not necessary here: toJson() creates interned strings;
                // but an explicitly calling intern() provides the guarantee
            }
        }
    }

    public final void addAllStandardJavaExecutorSpecifications() {
        if (REGISTER_BUILT_IN_EXECUTORS) {
            final long t1 = System.nanoTime();
            final Collection<ExecutorSpecification> allStandard = ExecutorSpecificationSet.allBuiltIn().all();
            final long t2 = System.nanoTime();
            LOG.log(Logger.Level.INFO, () -> String.format(Locale.US,
                    "Storing descriptions of %d installed built-in executor specifications: %.3f ms",
                    allStandard.size(), (t2 - t1) * 1e-6));
            setSpecifications(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, allStandard);
        }
    }

    public final boolean removeSpecification(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        checkEmptySessionId(sessionId);
        synchronized (allSpecifications) {
            final Map<String, String> session = allSpecifications.get(sessionId);
            if (session != null) {
                return session.remove(executorId) != null;
            } else {
                return false;
            }
        }
    }

    public final void removeSpecifications(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        synchronized (allSpecifications) {
            allSpecifications.remove(sessionId);
        }
    }

    private Executable findNewInstance(ExecutorSpecification specification) throws ClassNotFoundException {
        final String executorId = specification.getId();
        synchronized (newInstanceMakers) {
            if (newInstanceMakers.containsKey(executorId)) {
                return newInstanceMakers.get(executorId);
            }
            Executable executable = getNewInstance(specification);
            newInstanceMakers.put(executorId, executable);
            return executable;
        }
    }

    private static void checkEmptySessionId(String sessionId) {
        if (sessionId.isEmpty()) {
            throw new IllegalArgumentException("Empty sessionId");
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

    @Override
    public String toString() {
        return name + " (" + allSpecifications.size() + " sessions" +
                (allSpecifications.containsKey(ExecutionBlock.GLOBAL_SHARED_SESSION_ID) ?
                        ", including global" :
                        "") +
                ")";
    }

    // Note: this loader is usually enough for actual creating executors,
    // because non-Java executors are usually implemented via standard Java executor, which performs their tasks.
    // This loader DOES NOT use sessionId (Java classes are shared among all JVM).
    private static class StandardJavaExecutorLoader extends ExecutorLoader {

        StandardJavaExecutorLoader() {
            super("standard Java executors loader");
        }

        @Override
        public ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification)
                throws ClassNotFoundException {
            return loadStandardJavaExecutor(sessionId, specification);
        }

        @Override
        public void clearSession(String sessionId) {
            // No sense to free the cache, because it corresponds to system class loader and cannot become obsolete.
        }
    }
}
