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

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.Chain;

import java.util.Objects;

/**
 * Factory of {@link ExecutionBlock executors}.
 *
 * <p>Now we have only one implementation, {@link DefaultExecutorFactory}, which just calls
 * {@link ExecutionBlock#newExecutor} function with passing their
 * specification ({@link ExecutorSpecification}), necessary for loading Java class of executor.
 * In turn, {@link ExecutionBlock#newExecutor} uses one of registered
 * instanced of {@link ExecutorLoader} to actually create Java class {@link ExecutionBlock}.
 *
 * <p>This interface is necessary, for example, to execute block in a {@link Chain} or
 * to call some executor by its ID from JavaScript.
 *
 * <p>Usually there are many executor factories, created for different needs: in the chain interpreter,
 * in JavaScript interpreter, etc.
 */
public interface ExecutorFactory extends ExecutorSpecificationFactory {
    /**
     * Returns the session ID for which this factory creates executors.
     *
     * @return the session ID of this factory.
     */
    String sessionId();

    /**
     * <p>Creates new instance of {@link ExecutionBlock} on the base of its specification,
     * returned by {@link #getSpecification(String)} method.
     * This method also performs some initialization of the newly created object
     * according the <code>createMode</code> argument.</p>
     *
     * <p>If {@link #getSpecification(String)} method returns <code>null</code>, this method
     * throws {@link ExecutorExpectedException}; this method never returns <code>null</code>.</p>
     *
     * @param executorId        unique ID of the executor.
     * @param createMode how to initialize a newly created instance?
     * @return newly created executor.
     * @throws ClassNotFoundException    if Java class, required for creating the executor,
     *                                   is not available in the current <code>classpath</code> environment.
     * @throws ExecutorExpectedException if there is no requested executor.
     * @throws NullPointerException      if <code>executorId==null</code> or <code>createMode==null</code>.
     */
    ExecutionBlock newExecutor(String executorId, CreateMode createMode)
            throws ClassNotFoundException, ExecutorExpectedException;

    default <T extends ExecutionBlock> T newExecutor(Class<T> expectedClass, String executorId) {
        return newExecutor(expectedClass, executorId, CreateMode.REQUEST_DEFAULT);
        // - this function is universal, so we cannot provide a solution that may be inefficient (REQUEST_ALL);
        // on the other hand, requesting at least one port is almost always necessary
    }

    default <T extends ExecutionBlock> T newExecutor(
            Class<T> expectedClass,
            String executorId,
            CreateMode createMode) {
        Objects.requireNonNull(expectedClass, "Null expectedClass");
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(createMode, "Null createMode");
        ExecutionBlock result;
        try {
            result = newExecutor(executorId, createMode);
        } catch (ClassNotFoundException e) {
            throw new ExecutorExpectedException("Executor with ID \"" + executorId +
                    "\" probably was not successfully registered - Java class not found: " + e.getMessage(), e);
        }
        if (!expectedClass.isInstance(result)) {
            throw new ExecutorExpectedException("Executor with ID \"" + executorId +
                    "\" has unexpected " +
                    (result == null ? "null value" : "type " + result.getClass().getName()) +
                    ": it is not an instance of " + expectedClass);
        }
        return expectedClass.cast(result);
    }

    static ExecutorFactory newSharedFactory() {
        return newSharedFactory(ExecutionBlock.globalLoaders());
    }

    static ExecutorFactory newSharedFactory(ExecutorLoaderSet loaders) {
        return newFactory(loaders, ExecutionBlock.GLOBAL_SHARED_SESSION_ID);
    }

    static ExecutorFactory newFactory(String sessionId) {
        return newFactory(ExecutionBlock.globalLoaders(), sessionId);
    }

    static ExecutorFactory newFactory(ExecutorLoaderSet loaders, String sessionId) {
        Objects.requireNonNull(loaders, "Null loader set");
        Objects.requireNonNull(sessionId, "Null sessionId");
        return loaders.newFactory(sessionId);
    }
}
