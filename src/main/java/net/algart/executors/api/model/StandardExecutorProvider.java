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

import jakarta.json.JsonException;
import net.algart.executors.api.ExecutionBlock;

import java.util.Objects;

// Note: executing some loading-stage blocks can add here new IDs for further loading-stage blocks.
public class StandardExecutorProvider implements ExecutorProvider {
    private final ExecutorJsonSet staticExecutors;
    private final ExecutorJsonSet dynamicExecutorsCache = ExecutorJsonSet.newInstance();
    // - this set is dynamically extended in executorJson method via ExecutionBlock.getExecutorModelDescription
    private final String sessionId;
    private final Object lock = new Object();

    private StandardExecutorProvider(ExecutorJsonSet staticExecutors, String sessionId) {
        this.staticExecutors = Objects.requireNonNull(staticExecutors, "Null static executors set");
        this.sessionId = Objects.requireNonNull(sessionId, "Null sessionId");
    }

    public static StandardExecutorProvider newInstance(ExecutorJsonSet staticExecutors, String sessionId) {
        return new StandardExecutorProvider(staticExecutors, sessionId);
    }

    @Override
    public ExecutorJson executorJson(String executorId) {
        synchronized (lock) {
            ExecutorJson model = staticExecutors.get(executorId);
            if (model != null) {
                return model;
            }
            model = dynamicExecutorsCache.get(executorId);
            if (model != null) {
                return model;
                // - Caching: we suppose that non-null executor models cannot change.
                // It is not absolutely correct, when the programmer is developing new dynamic executors,
                // but the usage of this model by this package is very pure: we prefer to provide
                // maximal performance here (note that the following operators require conversion JSON <-> String
                // and are relatively slow). In any case, the developer can restart the server at any time.
                // We DO NOT TRY to cache null model: it MAY become non-null as a result of registering
                // new dynamic executors.
            }
            final String modelDescription = ExecutionBlock.getExecutorModelDescription(sessionId, executorId);
            if (modelDescription == null) {
                // - Will be null, when there is no available executor: for example, it is a dynamic executor
                // (which was not created yet by the corresponding static executor),
                // or it is not a Java executor (but we loaded Java only).
                // Typical example is creating/initializing new Chain in UseSubChain static executor.
                // This process consists of 3 stage (see UseSubChain.use(ChainJson) method):
                //      A) we create a Chain instance with all its blocks (ChainBlock);
                //      B) we execute all its static executors, like UseSettings, UseMapping etc.
                // (executeLoadingTimeBlocksWithoutInputs method);
                //      C) we finish creating the chain model (ExecutorJson) by
                // buildSubChainModelAndExecuteLoadingTimeWithoutInputs method and register it.
                // Let this chain contain some dynamic executors like CombineSettings or InterpretMultiChain,
                // together with necessary static executors, which create them (UseSettings or UseMultiChain).
                // Every ChainBlock tries to get ExecutorJson for its executor already at the stage A)
                // while its creation (ChainBlock.valueOf) - this is not obligatory, but helps while diagnostic
                // possible errors. However, the actual model for them will become known only at the stage B),
                // while executing corresponding static executors.
                // Of course, when we will want to EXECUTE dynamic executor, we will call newExecutor method
                // below, which REQUIRES existing a ready model.
                return null;
                // - No sense to add null to dynamicExecutorsCache;
                // moreover, it is prohibited ("add" method will throw an exception)
            } else {
                try {
                    model = ExecutorJson.valueOf(modelDescription);
                } catch (JsonException e) {
                    throw new IllegalStateException("Standard executor provider cannot be used with executor "
                            + executorId + ": it is registered with unsupported format of model description", e);
                }
            }
            dynamicExecutorsCache.add(executorId, model);
            return model;
        }
    }

    @Override
    public ExecutionBlock newExecutor(String executorId) throws ClassNotFoundException, ExecutorNotFoundException {
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (lock) {
            final ExecutorJson executorJson = executorJson(executorId);
            if (executorJson == null) {
                throw new ExecutorNotFoundException("Cannot create executor: non-registered ID " + executorId);
            }
            return ExecutionBlock.newExecutionBlock(
                    sessionId,
                    executorId,
                    executorJson.minimalConfigurationJsonString());
        }
    }
}
