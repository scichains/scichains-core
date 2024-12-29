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

import net.algart.executors.api.model.ExecutorJson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Actually, we do not override any methods in most cases.
 * This loader is used only for adding model descriptions (registerWorker method);
 * standard execution block loader (for usual Java classes) is used for loading workers.
 *
 * @param <W> some object ("worker") that actually perform all work of the executor;
 *            should implement {@link AutoCloseable}, if it has some resources that must be freed after usage.
 */
public class SimpleExecutorLoader<W> extends ExecutorLoader {
    public SimpleExecutorLoader(String name) {
        super(name);
    }

    private final Map<String, W> idToWorkerMap = new LinkedHashMap<>();
    private final Object lock = new Object();

    @Override
    public ExecutionBlock loadExecutor(String sessionId, String executorId, ExecutorJson specification) {
        return null;
        // - the same behavior as in the superclass: just skip loading and pass executorId to the standard loader
    }

    public boolean registerWorker(String sessionId, String id, W worker, ExecutorJson specification) {
        if (id == null) {
            return false;
        }
        Objects.requireNonNull(worker, "Null worker for non-null ID=" + id);
        synchronized (lock) {
            setSpecification(sessionId, id, specification.toJson().toString());
            final W previosWorker = idToWorkerMap.put(id, worker);
            try {
                if (previosWorker instanceof AutoCloseable) {
                    ((AutoCloseable) previosWorker).close();
                }
            } catch (Exception ignored) {
            }
            return previosWorker != null;
        }
    }

    public W reqRegisteredWorker(String id) {
        synchronized (lock) {
            final W result = idToWorkerMap.get(id);
            Objects.requireNonNull(result, "Cannot find registered worker with id=" + id);
            return result;
        }
    }
}
