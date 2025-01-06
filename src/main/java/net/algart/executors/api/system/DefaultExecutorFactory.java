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

import java.util.Objects;

/**
 * Default standard implementation of executor factory, based on {@link ExecutorLoaderSet}.
 */
public class DefaultExecutorFactory implements ExecutorFactory {
    private final ExecutorLoaderSet executorLoaderSet;

    private final ExecutorSpecificationSet staticExecutors;
    private final ExecutorSpecificationSet dynamicExecutorsCache = ExecutorSpecificationSet.newInstance();
    // - this set is dynamically extended in specification() method via ExecutorLoaderSet.getSpecification
    private final String sessionId;
    private final Object lock = new Object();

    /**
     * Creates new executor factory with standard behavior, based on the executor specification set
     * {@link ExecutorSpecificationSet#allBuiltIn()}.
     *
     * <p>The <code>sessionId</code> is the unique ID of the session, where all executors will be initialized:
     * see {@link ExecutionBlock#setSessionId(String)} method. This is important if you want
     * to execute executors like sub-chains, which dynamically create other executors,
     * probably having equal executor IDs. Executor factories with different <code>sessionID</code>
     * are isolated from each other and can be used simultaneously.</p>
     *
     * <p>If you want to work with executors shared across all sessions, please use
     * {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.</p>
     *
     * <p>If you need only one set of executors, you can specify any <code>sessionID</code> like
     * <code>"MySession"</code>.</p>
     *
     * @param executorLoaderSet set of executor loaders, used to search for specifications and create new executors.
     * @param sessionId unique session ID (1st argument of
     * {@link ExecutionBlock#newExecutionBlock(String, String, String)}).
     *
     * @see ExecutionBlock#getSessionId()
     * @see ExecutorLoaderSet#newExecutor(String, ExecutorSpecification)
     */
    public DefaultExecutorFactory(
            ExecutorLoaderSet executorLoaderSet,
            ExecutorSpecificationSet staticExecutors,
            String sessionId) {
        this.executorLoaderSet = Objects.requireNonNull(executorLoaderSet);
        this.staticExecutors = Objects.requireNonNull(staticExecutors, "Null static executors set");
        this.sessionId = Objects.requireNonNull(sessionId, "Null sessionId");
    }

    public static DefaultExecutorFactory newInstance(
            ExecutorLoaderSet executorLoaderSet,
            ExecutorSpecificationSet staticExecutors,
            String sessionId) {
        return new DefaultExecutorFactory(executorLoaderSet, staticExecutors, sessionId);
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public ExecutorSpecification getSpecification(String executorId) {
        synchronized (lock) {
            ExecutorSpecification specification = staticExecutors.get(executorId);
            if (specification != null) {
                return specification;
            }
            specification = dynamicExecutorsCache.get(executorId);
            if (specification != null) {
                return specification;
                // - Caching: we suppose that non-null executor specifications cannot change.
                // It is not absolutely correct when the programmer is developing new dynamic executors,
                // but the usage of this specification by this package is very pure: we prefer to provide
                // maximal performance here (note that the following operators require conversion JSON <-> String
                // and are relatively slow). In any case, the developer can restart the server at any time.
                // We DO NOT TRY to cache null specification: it MAY become non-null as a result of registering
                // new dynamic executors.
            }
            specification = executorLoaderSet.getSpecification(sessionId, executorId);
            if (specification == null) {
                // - It will be null, when there is no available executor: for example, it is a dynamic executor
                // (which was not created yet by the corresponding static executor),
                // or it is not a Java executor (but we have loaded Java only).
                // The typical example is creating/initializing new Chain in UseSubChain static executor.
                // This process consists of 3 stages (see UseSubChain.use(ChainSpecification) method):
                //      A) we create a Chain instance with all its blocks (ChainBlock);
                //      B) we execute all its static executors, like UseSettings, UseMapping etc.
                // (executeLoadingTimeBlocksWithoutInputs method);
                //      C) we finish creating the chain executor specification by
                // buildSubChainSpecificationAndExecuteLoadingTimeWithoutInputs method and register it.
                // Let this chain contain some dynamic executors like CombineSettings or InterpretMultiChain,
                // together with necessary static executors, which create them (UseSettings or UseMultiChain).
                // Every ChainBlock tries to get ExecutorSpecification for its executor already at stage A,
                // while its creation (ChainBlock.valueOf) - this is not obligatory, but helps in the case of
                // possible diagnostic errors.
                // However, the actual specification for them will become known only at stage B,
                // while executing corresponding static executors.
                // Of course, when we want to EXECUTE dynamic executor, we will call newExecutor method
                // below, which REQUIRES the existence of a ready specification.
                return null;
                // - No sense to add null to dynamicExecutorsCache;
                // moreover, it is prohibited ("add" method will throw an exception)
            }
            dynamicExecutorsCache.add(executorId, specification);
            return specification;
        }
    }

    @Override
    public ExecutionBlock newExecutor(String executorId) throws ClassNotFoundException, ExecutorNotFoundException {
        Objects.requireNonNull(executorId, "Null executorId");
        synchronized (lock) {
            final ExecutorSpecification executorSpecification = getSpecification(executorId);
            if (executorSpecification == null) {
                throw new ExecutorNotFoundException("Cannot create executor: non-registered ID " + executorId);
            }
            return executorLoaderSet.newExecutor(sessionId, executorSpecification);
        }
    }

    @Override
    public String toString() {
        return "default executor factory for session \"" + sessionId + "\"";
    }
}
