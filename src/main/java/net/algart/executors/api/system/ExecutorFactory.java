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
     * <p>Creates new instance of {@link ExecutionBlock} on the base of its specification,
     * returned by {@link #getSpecification(String)} method.
     * This method also performs some initialization of the newly created object
     * according the <code>instantiationMode</code> argument.</p>
     *
     * <p>If {@link #getSpecification(String)} method returns <code>null</code>, this method
     * throws {@link ExecutorNotFoundException}; this method never returns <code>null</code>.</p>
     *
     * @param executorId        unique ID of the executor.
     * @param instantiationMode how to initialize a newly created instance?
     * @return newly created executor.
     * @throws ClassNotFoundException    if Java class, required for creating the executor,
     *                                   is not available in the current <code>classpath</code> environment.
     * @throws ExecutorNotFoundException if there is no requested executor.
     * @throws NullPointerException      if <code>executorId==null</code> or <code>instantiationMode==null</code>.
     */
    ExecutionBlock newExecutor(String executorId, InstantiationMode instantiationMode) throws
            ClassNotFoundException, ExecutorNotFoundException;

    static ExecutorFactory newDefaultInstance(String sessionId) {
        return ExecutionBlock.globalLoaders().newFactory(sessionId);
    }
}
