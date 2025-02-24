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

package net.algart.executors.api.mappings;

import net.algart.executors.api.Executor;

public abstract class MappingExecutor extends Executor {
    volatile MappingBuilder mappingBuilder = null;

    public MappingBuilder mappingBuilder() {
        final String sessionId = getSessionId();
        final String executorId = getExecutorId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot find mapping worker: session ID is not set");
        }
        if (executorId == null) {
            throw new IllegalStateException("Cannot find mapping worker: executor ID is not set");
        }
        MappingBuilder mappingBuilder = this.mappingBuilder;
        if (mappingBuilder == null) {
            mappingBuilder = UseMapping.mappingLoader().registeredWorker(sessionId, executorId);
            this.mappingBuilder = mappingBuilder.clone();
            // - the order is important for multithreading: local mapping is assigned first,
            // this.mapping is assigned to it;
            // cloning is not necessary in the current version, but added for possible future extensions
        }
        return mappingBuilder;
    }

}
