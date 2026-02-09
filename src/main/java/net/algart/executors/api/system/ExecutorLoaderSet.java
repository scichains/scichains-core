/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.*;

public final class ExecutorLoaderSet {
    private final List<ExecutorLoader> loaders = new ArrayList<>();

    public void register(ExecutorLoader loader) {
        synchronized (loaders) {
            loaders.add(loader);
        }
    }

    public List<ExecutorLoader> list() {
        synchronized (loaders) {
            return Collections.unmodifiableList(loaders);
        }
    }

    public ExecutorFactory newFactory(String sessionId, ExecutorSpecificationSet preloadedSpecifications) {
        return new DefaultExecutorFactory(this, sessionId, preloadedSpecifications);
    }

    public ExecutorFactory newFactory(String sessionId) {
        return newFactory(sessionId, ExecutorSpecificationSet.allBuiltIn());
    }

    public ExecutionBlock newExecutor(String sessionId, ExecutorSpecification specification, CreateMode createMode)
            throws ClassNotFoundException {
        final ExecutionBlock executor = loadExecutor(sessionId, specification, createMode);
        if (executor == null) {
            throw new IllegalArgumentException("Cannot load executor with ID " + specification.getId()
                    + ": unknown executor specification");
        }
//                System.out.println("Specification: " + specification);
        return executor;
    }

    public ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification, CreateMode createMode)
            throws ClassNotFoundException {
        Objects.requireNonNull(specification, "Null specification");
        final List<ExecutorLoader> loaders = list();
        for (int k = loaders.size() - 1; k >= 0; k--) {
            // Last registered loaders override previous
            final ExecutionBlock executor = loaders.get(k).loadExecutor(sessionId, specification, createMode);
            if (executor != null) {
                return executor;
            }
        }
        return null;
    }

    /**
     * Returns specification with the given <code>executorId</code>,
     * that can be found in
     * <pre>{@link #allSerializedSpecifications(String, boolean)
     * serializedSessionSpecifications}(sessionId, includeGlobalSession),</pre>
     * and parses it.
     * This method searches all the registered loaders for the requested executor.
     * This method can be relatively slow because it parses the serialized form of specification.
     *
     * @param sessionId  unique ID of the session; may be <code>null</code>, than only global session will be
     *                   checked.
     * @param executorId unique ID of the executor.
     * @return specification of the executor or <code>null</code> if there is no requested executor.
     * @throws NullPointerException if <code>executorId</code>> arguments is <code>null</code>.
     */
    public ExecutorSpecification getSpecification(String sessionId, String executorId, boolean includeGlobalSession)
            throws JsonException {
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (loaders) {
            for (ExecutorLoader loader : loaders) {
                if (includeGlobalSession) {
                    final var result = loader.getSpecification(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, executorId);
                    if (result != null) {
                        return result;
                    }
                }
                if (sessionId != null) {
                    final var result = loader.getSpecification(sessionId, executorId);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns specification with the given <code>executorId</code>,
     * that can be found in
     * <pre>{@link #allSerializedSpecifications(String, boolean)
     * serializedSessionSpecifications}(sessionId, includeGlobalSession)</pre>
     * This method searches all the registered loaders for the requested executor.
     *
     * @param sessionId  unique ID of the session; may be <code>null</code>, than only global session will be
     *                   checked.
     * @param executorId unique ID of the executor.
     * @return serialized specification of the executor or <code>null</code> if there is not such executor.
     * @throws NullPointerException if <code>executorId</code>> arguments is <code>null</code>.
     */
    public String serializedSpecification(String sessionId, String executorId, boolean includeGlobalSession)
            throws JsonException {
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (loaders) {
            for (ExecutorLoader loader : loaders) {
                if (includeGlobalSession) {
                    final var result = loader.serializedSpecification(
                            ExecutionBlock.GLOBAL_SHARED_SESSION_ID, executorId);
                    if (result != null) {
                        return result;
                    }
                }
                if (sessionId != null) {
                    final var result = loader.serializedSpecification(sessionId, executorId);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns executor specifications for all executors, registered for the given session
     * and, if the second argument is <code>true</code>,
     * for the global session {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID},
     * in a serialized (string) form.
     * Keys in the result are IDs of executors, values are serialized specifications.
     * This may be used for the user interface to show information for available executors.
     *
     * @param sessionId            unique ID of the session;
     *                             may be <code>null</code>, than only global session will be checked.
     * @param includeGlobalSession if <code>true</code>, the result includes the executors,
     *                             registered in the global session.
     * @return all available executor specifications for this and (possibly) the global session.
     */
    public Map<String, String> allSerializedSpecifications(String sessionId, boolean includeGlobalSession) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (ExecutorLoader loader : list()) {
            if (includeGlobalSession) {
                result.putAll(loader.allSerializedSpecifications(ExecutionBlock.GLOBAL_SHARED_SESSION_ID));
            }
            if (sessionId != null) {
                result.putAll(loader.allSerializedSpecifications(sessionId));
            }
//            System.out.println("!!! " + loader + ": " + result.size() + " executors");
        }
        return result;
    }

    public Set<String> allExecutorIds(String sessionId, boolean includeGlobalSession) {
        final Set<String> result = new LinkedHashSet<>();
        for (ExecutorLoader loader : list()) {
            if (includeGlobalSession) {
                result.addAll(loader.allExecutorIds(ExecutionBlock.GLOBAL_SHARED_SESSION_ID));
            }
            if (sessionId != null) {
                result.addAll(loader.allExecutorIds(sessionId));
            }
        }
        return result;
    }

    public Set<String> allSessionIds() {
        final Set<String> result = new LinkedHashSet<>();
        for (ExecutorLoader loader : list()) {
            result.addAll(loader.allSessionIds());
        }
        return result;
    }

    public void clearSession(String sessionId) {
        for (ExecutorLoader loader : list()) {
            loader.clearSession(sessionId);
        }
    }
}
