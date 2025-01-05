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
import net.algart.executors.api.Port;
import net.algart.executors.api.parameters.Parameters;

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

    public ExecutorFactory newFactory(ExecutorSpecificationSet staticExecutors, String sessionId) {
        return new DefaultExecutorFactory(this, staticExecutors, sessionId);
    }

    public ExecutorFactory newFactory(String sessionId) {
        return newFactory(ExecutorSpecificationSet.allBuiltIn(), sessionId);
    }

    public ExecutionBlock newExecutor(String sessionId, ExecutorSpecification specification)
            throws ClassNotFoundException {
        final ExecutionBlock executor = loadExecutor(sessionId, specification);
        executor.setSessionId(sessionId);
        executor.setExecutorSpecification(specification);
        final Parameters parameters = executor.parameters();
        for (var e : specification.getControls().entrySet()) {
            final String name = e.getKey();
            final ExecutorSpecification.ControlConf controlConf = e.getValue();
            parameters.put(name, controlConf.getDefaultValue());
        }
        for (var e : specification.getInPorts().entrySet()) {
            executor.addPort(Port.newInput(e.getKey(), e.getValue().getValueType()));
        }
        for (var e : specification.getOutPorts().entrySet()) {
            executor.addPort(Port.newOutput(e.getKey(), e.getValue().getValueType()));
        }
//                System.out.println("Specification: " + specification);
        return executor;

    }

    public ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification)
            throws ClassNotFoundException {
        Objects.requireNonNull(specification, "Null specification");
        final List<ExecutorLoader> loaders = loaders();
        for (int k = loaders.size() - 1; k >= 0; k--) {
            // Last registered loaders override previous
            final ExecutionBlock executor = loaders.get(k).loadExecutor(sessionId, specification);
            if (executor != null) {
                return executor;
            }
        }
        throw new IllegalArgumentException("Cannot load executor with ID " + specification.getExecutorId()
                + ": unknown executor specification");
    }

    public ExecutorSpecification getSpecification(String sessionId, String executorId) {
        return getSpecification(sessionId, executorId, true);
    }

    /**
     * Returns specification Equivalent to <code>{@link #serializedSessionSpecifications(String, boolean)
     * availableExecutorSpecifications}(sessionId, includeGlobalSession).get(executorId)</code>,
     * but works quickly (without creating a new map).
     *
     * @param sessionId  unique ID of current session; may be <code>null</code>, than only global session will be
     *                   checked.
     * @param executorId unique ID of this executor in the system.
     * @return description of this dynamic executor (probably JSON) or <code>null</code> if there is not such executor.
     * @throws NullPointerException if <code>executorId</code>> arguments is <code>null</code>.
     */
    public ExecutorSpecification getSpecification(String sessionId, String executorId, boolean includeGlobalSession)
            throws JsonException {
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


    public Map<String, String> serializedSessionSpecifications(String sessionId) {
        return serializedSessionSpecifications(sessionId, true);
    }

    /**
     * Returns executor specifications for all executors, registered for the given session
     * and, if the second argument is <code>true</code>,
     * for the global session {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID},
     * in a serialized (string) form.
     * Keys in the result are IDs of executors, values are serialized specifications.
     * This may be used for the user interface to show information for available executors.
     *
     * @param sessionId            unique ID of current session; may be <code>null</code>.
     * @param includeGlobalSession if <code>true</code>, the result includes the executors,
     *                             registered in the global session.
     * @return all available executor specifications for this and (possibly) the global session.
     */
    public Map<String, String> serializedSessionSpecifications(String sessionId, boolean includeGlobalSession) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (ExecutorLoader loader : loaders()) {
            if (includeGlobalSession) {
                result.putAll(loader.serializedSessionSpecifications(ExecutionBlock.GLOBAL_SHARED_SESSION_ID));
            }
            if (sessionId != null) {
                result.putAll(loader.serializedSessionSpecifications(sessionId));
            }
//            System.out.println("!!! " + loader + ": " + result.size() + " executors");
        }
        return result;
    }

    public void clearSession(String sessionId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        for (ExecutorLoader loader : loaders()) {
            loader.clearSession(sessionId);
        }
    }
}
