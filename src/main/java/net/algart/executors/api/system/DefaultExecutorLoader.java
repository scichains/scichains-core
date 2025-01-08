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

import net.algart.executors.api.ExecutionBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default executor loader.
 *
 * <p>Actually, we do not override any methods in most cases.
 * This loader is used only for adding executor specifications, performed inside {@link #registerWorker} method.
 * The {@link ExecutorLoader#getStandardJavaExecutorLoader() standard executor loader}
 * (for usual Java classes) is used for loading the executor, but it delegates all work
 * to the "worker", registered for this session and executor ID.
 * Possible example of "worker" is {@link Chain} class.
 *
 * @param <W> some object ("worker") that actually perform all work of the executor;
 *            should implement {@link AutoCloseable}, if it has some resources that must be freed after usage.
 */
public class DefaultExecutorLoader<W> extends ExecutorLoader {
    public DefaultExecutorLoader(String name) {
        super(name);
    }

    private final Map<String, Map<String, W>> idToWorkerMap = new LinkedHashMap<>();
    // - This map is: sessionId -> Map(executorId -> worker)
    // Note that we MUST NOT share workers between sessions!
    // Worker can use session information, for example, for creating its own "child" executors (like Chain class).
    // Usually this is not too important, but may lead to strange results: let's suppose
    // that the executor prints all available executor specifications in its session.

    private final Object lock = new Object();

    @Override
    public ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification)
            throws ClassNotFoundException {
        return null;
        // - Just skip loading and pass executorId to the standard loader.
        // Usually we have a set of executors with different IDs, but with the same Java class like InterpretSubChain,
        // which can be loaded by the standard Java loader.
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean registerWorker(String sessionId, ExecutorSpecification specification, W worker) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(specification, "Null specification");
        Objects.requireNonNull(worker, "Null worker");
        synchronized (lock) {
            setSpecification(sessionId, specification);
            final W previosWorker = setWorker(sessionId, specification.getExecutorId(), worker);
            try {
                if (previosWorker instanceof AutoCloseable) {
                    ((AutoCloseable) previosWorker).close();
                }
            } catch (Exception ignored) {
            }
            return previosWorker != null;
        }
    }

    public W registeredWorker(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Null sessionId");
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (lock) {
            W result = getWorker(ExecutionBlock.GLOBAL_SHARED_SESSION_ID, executorId);
            if (result != null) {
                return result;
            }
            result = getWorker(sessionId, executorId);
            Objects.requireNonNull(result, "Cannot find registered worker with id \"" +
                    executorId + "\" for session \"" + sessionId + "\"");
            return result;
        }
    }

    @Override
    public void clearSession(String sessionId) {
        super.clearSession(sessionId);
        synchronized (lock) {
            idToWorkerMap.remove(sessionId);
//            System.out.println("Clearing session " + sessionId + " in " + this);
        }
    }

    private W getWorker(String sessionId, String executorId) {
        return idToWorkerMap.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()).get(executorId);
    }

    private W setWorker(String sessionId, String executorId, W worker) {
        return idToWorkerMap.computeIfAbsent(sessionId, k -> new LinkedHashMap<>()).put(executorId, worker);
    }

}
