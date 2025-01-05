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
 * This loader is used only for adding model specifications (performed inside {@link #registerWorker} method).
 * The {@link ExecutorLoader#getStandardJavaExecutorLoader() standard executor loader}
 * (for usual Java classes) is used for loading the executor, but it delegates all work
 * to the "worker", registered for this executor ID.
 *
 * @param <W> some object ("worker") that actually perform all work of the executor;
 *            should implement {@link AutoCloseable}, if it has some resources that must be freed after usage.
 */
public class DefaultExecutorLoader<W> extends ExecutorLoader {
    public DefaultExecutorLoader(String name) {
        super(name);
    }

    private final Map<String, W> idToWorkerMap = new LinkedHashMap<>();
    private final Object lock = new Object();

    @Override
    public ExecutionBlock loadExecutor(String sessionId, ExecutorSpecification specification) {
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
            final W previosWorker = idToWorkerMap.put(specification.getExecutorId(), worker);
            try {
                if (previosWorker instanceof AutoCloseable) {
                    ((AutoCloseable) previosWorker).close();
                }
            } catch (Exception ignored) {
            }
            return previosWorker != null;
        }
    }

    public W registeredWorker(String id) {
        synchronized (lock) {
            final W result = idToWorkerMap.get(id);
            Objects.requireNonNull(result, "Cannot find registered worker with id=" + id);
            return result;
        }
    }
}
