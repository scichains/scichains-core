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

import jakarta.json.JsonException;
import net.algart.executors.api.model.ExecutorSpecification;
import net.algart.executors.api.model.ExecutorSpecificationSet;
import net.algart.executors.api.model.ExecutorNotFoundException;

import java.util.Objects;

// Note: executing some loading-stage blocks can add here new IDs for further loading-stage blocks.
public class StandardExecutorFactory implements ExecutorFactory {
    private final ExecutorSpecificationSet staticExecutors;
    private final ExecutorSpecificationSet dynamicExecutorsCache = ExecutorSpecificationSet.newInstance();
    // - this set is dynamically extended in specification() method via ExecutionBlock.getExecutorSpecification
    private final String sessionId;
    private final Object lock = new Object();

    private StandardExecutorFactory(ExecutorSpecificationSet staticExecutors, String sessionId) {
        this.staticExecutors = Objects.requireNonNull(staticExecutors, "Null static executors set");
        this.sessionId = Objects.requireNonNull(sessionId, "Null sessionId");
    }

    public static StandardExecutorFactory newInstance(ExecutorSpecificationSet staticExecutors, String sessionId) {
        return new StandardExecutorFactory(staticExecutors, sessionId);
    }

    @Override
    public ExecutorSpecification specification(String executorId) {
        synchronized (lock) {
            ExecutorSpecification executorSpecification = staticExecutors.get(executorId);
            if (executorSpecification != null) {
                return executorSpecification;
            }
            executorSpecification = dynamicExecutorsCache.get(executorId);
            if (executorSpecification != null) {
                return executorSpecification;
                // - Caching: we suppose that non-null executor models cannot change.
                // It is not absolutely correct, when the programmer is developing new dynamic executors,
                // but the usage of this model by this package is very pure: we prefer to provide
                // maximal performance here (note that the following operators require conversion JSON <-> String
                // and are relatively slow). In any case, the developer can restart the server at any time.
                // We DO NOT TRY to cache null specification: it MAY become non-null as a result of registering
                // new dynamic executors.
            }
            final String specification = ExecutionBlock.getExecutorSpecification(sessionId, executorId);
            if (specification == null) {
                // - It will be null, when there is no available executor: for example, it is a dynamic executor
                // (which was not created yet by the corresponding static executor),
                // or it is not a Java executor (but we loaded Java only).
                // The typical example is creating/initializing new Chain in UseSubChain static executor.
                // This process consists of 3 stages (see UseSubChain.use(ChainJson) method):
                //      A) we create a Chain instance with all its blocks (ChainBlock);
                //      B) we execute all its static executors, like UseSettings, UseMapping etc.
                // (executeLoadingTimeBlocksWithoutInputs method);
                //      C) we finish creating the chain model (ExecutorSpecification) by
                // buildSubChainModelAndExecuteLoadingTimeWithoutInputs method and register it.
                // Let this chain contain some dynamic executors like CombineSettings or InterpretMultiChain,
                // together with necessary static executors, which create them (UseSettings or UseMultiChain).
                // Every ChainBlock tries to get ExecutorSpecification for its executor already at the stage A)
                // while its creation (ChainBlock.valueOf) - this is not obligatory, but helps while diagnostic
                // possible errors. However, the actual model for them will become known only at the stage B),
                // while executing corresponding static executors.
                // Of course, when we want to EXECUTE dynamic executor, we will call newExecutor method
                // below, which REQUIRES the existence of a ready model.
                return null;
                // - No sense to add null to dynamicExecutorsCache;
                // moreover, it is prohibited ("add" method will throw an exception)
            } else {
                try {
                    executorSpecification = ExecutorSpecification.valueOf(specification);
//                    System.out.println("Building executor: " + executorSpecification.getName());
                } catch (JsonException e) {
                    throw new IllegalStateException("Standard executor factory cannot be used with executor "
                            + executorId + ": it is registered with unsupported format of executor specification", e);
                }
            }
            dynamicExecutorsCache.add(executorId, executorSpecification);
            return executorSpecification;
        }
    }

    @Override
    public ExecutionBlock newExecutor(String executorId) throws ClassNotFoundException, ExecutorNotFoundException {
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (lock) {
            final ExecutorSpecification executorSpecification = specification(executorId);
            if (executorSpecification == null) {
                throw new ExecutorNotFoundException("Cannot create executor: non-registered ID " + executorId);
            }
            return ExecutionBlock.newExecutor(sessionId, executorId, executorSpecification);
        }
    }
}
