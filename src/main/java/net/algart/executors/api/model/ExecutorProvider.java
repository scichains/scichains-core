/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.executors.api.ExecutionBlockLoader;

/**
 * Provider of {@link ExecutionBlock executors}. Now we have only one implementation,
 * {@link StandardExecutorProvider}, which just calls
 * {@link ExecutionBlock#newExecutionBlock} function with passing their
 * minimal simplified JSON string, necessary for loading Java class of executor.
 * In turn, {@link ExecutionBlock#newExecutionBlock} uses one of registered
 * instanced of {@link ExecutionBlockLoader} to actually create Java class {@link ExecutionBlock}.
 *
 * <p>This interface is necessary, for example, to execute block in a {@link Chain} or
 * to call some executor by its ID from JavaScript.
 */
public interface ExecutorProvider {
    /**
     * Returns JSON model of the given executor.
     *
     * <p>Note that the main goal of this function
     * is only to return minimal description, enough for building new executor by {@link #newExecutor(String)} method;
     * in current it is enough to return {@link ExecutorJson#minimalConfigurationJsonString()}.
     * But this function <b>may</b> return full JSON; it is used, in particular, by {@link Chain} class.
     *
     * <p>The main source of information about all JSON models is another:
     * {@link ExecutionBlock#availableExecutorModelDescriptions(String)}.
     *
     * @param executorId unique executor ID.
     * @return minimal JSON model, enough for creating Java class {@link ExecutionBlock}.
     */
    ExecutorJson executorJson(String executorId);

    ExecutionBlock newExecutor(String executorId) throws ClassNotFoundException, ExecutorNotFoundException;

    static ExecutorProvider newStandardInstance(String sessionId) {
        return StandardExecutorProvider.newInstance(ExecutorJsonSet.allBuiltIn(), sessionId);
    }
}
