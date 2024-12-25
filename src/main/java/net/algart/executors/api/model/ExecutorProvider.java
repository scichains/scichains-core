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

package net.algart.executors.api.model;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.ExecutorLoader;

/**
 * Provider of {@link ExecutionBlock executors}.
 * Now we have only one implementation, {@link StandardExecutorProvider}, which just calls
 * {@link ExecutionBlock#newExecutor} function with passing their
 * specification ({@link ExecutorJson}), necessary for loading Java class of executor.
 * In turn, {@link ExecutionBlock#newExecutor} uses one of registered
 * instanced of {@link ExecutorLoader} to actually create Java class {@link ExecutionBlock}.
 *
 * <p>This interface is necessary, for example, to execute block in a {@link Chain} or
 * to call some executor by its ID from JavaScript.
 *
 * <p>Usually there are many executor providers, created for different needs: in the chain interpreter,
 * in JavaScript interpreter, etc.
 */
public interface ExecutorProvider {
    /**
     * Returns JSON model of the given executor.
     *
     * <p>Note that the main goal of this function
     * is only to return minimal description, enough for building new executor by {@link #newExecutor(String)} method.
     * However, this function usually returns full specification.
     *
     * <p>The main source of information about all JSON models is another:
     * {@link ExecutionBlock#availableExecutorSpecifications(String)}.
     *
     * @param executorId unique executor ID.
     * @return minimal JSON model, enough for creating Java class {@link ExecutionBlock}.
     */
    ExecutorJson specification(String executorId);

    ExecutionBlock newExecutor(String executorId) throws ClassNotFoundException, ExecutorNotFoundException;

    /**
     * Creates new executor provider with standard behavior, based on the executor set
     * {@link ExecutorJsonSet#allBuiltIn()}.
     *
     * <p>The <code>sessionId</code> is the unique ID of the session, where all executors will be initialized:
     * see {@link ExecutionBlock#setSessionId(String)} method. This is important if you want
     * to execute executors like sub-chains, which dynamically create other executors,
     * probably having equal executor IDs. Executor providers with different <code>sessionID</code>
     * are isolated from each other and can be used simultaneously.</p>
     *
     * <p>If you want to work with executors shared across all sessions, please use
     * {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.</p>
     *
     * <p>If you need only one set of executors, you can specify any <code>sessionID</code> like
     * <code>"MySession"</code>.</p>
     *
     * @param sessionId unique session ID (1st argument of
     * {@link ExecutionBlock#newExecutionBlock(String, String, String)}).
     * @return a new standard executor provider.
     *
     * @see ExecutionBlock#setSessionId(String)
     * @see ExecutionBlock#newExecutionBlock(String, String, String)
     */
    static ExecutorProvider newStandardInstance(String sessionId) {
        return StandardExecutorProvider.newInstance(ExecutorJsonSet.allBuiltIn(), sessionId);
    }
}
