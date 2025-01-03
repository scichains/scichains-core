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

package net.algart.executors.api.system;

import jakarta.json.JsonException;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.ExecutorLoader;

import java.util.*;

public final class ExecutorLoaderSet {
    private final List<ExecutorLoader> loaders = new ArrayList<>();

    public void register(ExecutorLoader loader) {
        synchronized (loaders) {
            loaders.add(loader);
        }
    }

    public List<ExecutorLoader> loaders() {
        synchronized (loaders) {
            return new ArrayList<>(loaders);
        }
    }

    public ExecutionBlock newExecutor(String sessionId, String executorId, ExecutorSpecification specification)
            throws ClassNotFoundException {
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(specification, "Null specification");
        final List<ExecutorLoader> loaders = loaders();
        for (int k = loaders.size() - 1; k >= 0; k--) {
            // Last registered loaders override previous
            final ExecutionBlock executor = loaders.get(k).newExecutor(sessionId, executorId, specification);
            if (executor != null) {
                return executor;
            }
        }
        throw new IllegalArgumentException("Cannot load executor with ID " + executorId
                + ": unknown executor specification");
    }

    public Map<String, String> availableSpecifications(String sessionId) {
        return availableSpecifications(sessionId, true);
    }

    /**
     * Returns executor specifications for all executors, registered for the given session
     * and, if the second argument is <code>true</code>,
     * for the global session {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.
     * Keys in the result are ID of every executor, values are descriptions.
     * This may be used for the user interface to show information for available executors.
     *
     * @param sessionId            unique ID of current session; may be <code>null</code>.
     * @param includeGlobalSession if <code>true</code>, the result includes the executors,
     *                             registered in the global session.
     * @return all available executor specifications for this and the global session.
     */
    public Map<String, String> availableSpecifications(String sessionId, boolean includeGlobalSession) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (ExecutorLoader loader : loaders()) {
            if (includeGlobalSession) {
                result.putAll(loader.availableSpecifications(ExecutionBlock.GLOBAL_SHARED_SESSION_ID));
            }
            if (sessionId != null) {
                result.putAll(loader.availableSpecifications(sessionId));
            }
//            System.out.println("!!! " + loader + ": " + result.size() + " executors");
        }
        return result;
    }

    public String getSpecification(String sessionId, String executorId) {
        return getSpecification(sessionId, executorId, true);
    }

    /**
     * Equivalent to <code>{@link #availableSpecifications(String, boolean)
     * availableExecutorSpecifications}(sessionId, includeGlobalSession).get(executorId)</code>,
     * but works quickly (without creating a new map).
     *
     * @param sessionId  unique ID of current session; may be <code>null</code>, than only global session will be
     *                   checked.
     * @param executorId unique ID of this executor in the system.
     * @return description of this dynamic executor (probably JSON) or <code>null</code> if there is not such executor.
     * @throws NullPointerException if <code>executorId</code>> arguments is <code>null</code>.
     */
    public String getSpecification(String sessionId, String executorId, boolean includeGlobalSession) {
        synchronized (loaders) {
            for (ExecutorLoader loader : loaders) {
                if (includeGlobalSession) {
                    final String result = loader.getSpecification(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, executorId);
                    if (result != null) {
                        return result;
                    }
                }
                if (sessionId != null) {
                    final String result = loader.getSpecification(sessionId, executorId);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    public ExecutorSpecification parseSpecification(String sessionId, String executorId) throws JsonException {
        final String specification = getSpecification(sessionId, executorId);
        if (specification == null) {
            return null;
        }
        return ExecutorSpecification.valueOf(specification);
    }

    public void clearSession(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        for (ExecutorLoader loader : loaders()) {
            loader.clearSession(sessionId);
        }
    }
}
