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
 * Default standard implementation of executor factory, based on the {@link ExecutorLoaderSet executor loader set}.
 */
public class DefaultExecutorFactory implements ExecutorFactory {
    private final ExecutorLoaderSet loaderSet;

    private final ExecutorSpecificationSet preloadedSpecifications;
    private final ExecutorSpecificationSet dynamicSpecificationsCache = ExecutorSpecificationSet.newInstance();
    // - this set is dynamically extended in specification() method via ExecutorLoaderSet.getSpecification
    private final String sessionId;
    private final Object lock = new Object();

    /**
     * Creates a new executor factory based on the specified <code>loaderSet</code>:
     * methods of this interface call the corresponding methods of this loader set.
     *
     * <p>The <code>sessionId</code> is a unique ID of the session in which all executors will be initialized
     * by teh {@link ExecutionBlock#setSessionId(String)} method.
     * It is passed as the first argument to the loader set methods.</p>
     *
     * <p>This is important if you want to use executors like sub-chains that dynamically create other executors,
     * possibly having the same executor IDs.
     * Executor factories with different <code>sessionID</code> are isolated from each other
     * and can be used simultaneously.
     * If you want to work with executors shared across all sessions, please use
     * {@link ExecutionBlock#GLOBAL_SHARED_SESSION_ID}.</p>
     *
     * <p>The set of specifications <code>preloadedSpecifications</code> changes the behavior
     * of the main method {@link #getSpecification(String)}. If any executor ID is found in this set,
     * it is returned without any additional checks of the loader set.
     * This can be used for optimization.
     * Typical variants of this parameter are {@link ExecutorSpecificationSet#allBuiltIn()}
     * and {@link ExecutorSpecificationSet#newInstance()}.</p>
     *
     * @param loaderSet               set of executor loaders, used to search for specifications and create new
     *                                executors.
     * @param sessionId               unique session ID.
     * @param preloadedSpecifications executor specifications, which will always be used before checking the loaders.
     * @see ExecutionBlock#getSessionId()
     * @see ExecutorLoaderSet#newExecutor
     */
    public DefaultExecutorFactory(
            ExecutorLoaderSet loaderSet,
            String sessionId,
            ExecutorSpecificationSet preloadedSpecifications) {
        this.loaderSet = Objects.requireNonNull(loaderSet);
        this.sessionId = Objects.requireNonNull(sessionId, "Null sessionId");
        this.preloadedSpecifications = Objects.requireNonNull(preloadedSpecifications, "Null static executors set");
    }

    /**
     * Returns the executor loader set, specified in the constructor.
     *
     * @return the executor loader set, used by this factory.
     */
    public final ExecutorLoaderSet loaderSet() {
        return loaderSet;
    }

    /**
     * Returns the session ID, specified in the constructor.
     *
     * @return the session ID of this factory.
     */
    public final String sessionId() {
        return sessionId;
    }

    /**
     * This implementation works like
     * <pre>
     *      {@link #loaderSet() loaderSet()}.{@link ExecutorLoaderSet#getSpecification
     *      getSpecification}({@link #sessionId() sessionId()}, executorId, true);
     * </pre>
     *
     * <p>Unlike this, this method has optimizations:</p>
     * <ul>
     *     <li>the specification is searched for in the preloaded specification set specified in the constructor;</li>
     *     <li>the parsed specifications are cached.</li>
     * </ul>
     *
     * @param executorId unique executor ID.
     * @return executor specification for creating new executor.
     */
    @Override
    public ExecutorSpecification getSpecification(String executorId) {
        synchronized (lock) {
            ExecutorSpecification specification = preloadedSpecifications.get(executorId);
            if (specification != null) {
                return specification;
            }
            specification = dynamicSpecificationsCache.get(executorId);
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
            specification = loaderSet.getSpecification(sessionId, executorId, true);
            if (specification == null) {
                // - It will be null when there is no available executor: for example, it is a dynamic executor
                // (which was not created yet by the corresponding static executor),
                // or it is not a Java executor (but we have loaded Java only).
                // The typical example is creating/initializing a new Chain in UseChain static executor.
                // This process consists of 3 stages (see UseChain.use(ChainSpecification) method):
                //      A) we create a Chain instance with all its blocks (ChainBlock);
                //      B) we execute all its static executors, like UseSettings, UseMapping etc.
                // (executeLoadingTimeBlocksWithoutInputs method);
                //      C) we finish creating the chain executor specification by
                // buildSubChainSpecificationAndExecuteLoadingTimeWithoutInputs method and register it.
                // Let this chain contain some dynamic executors like CombineSettings or InterpretMultiChain,
                // together with necessary static executors, which create them (UseSettings or UseMultiChain).
                // Every ChainBlock tries to get ExecutorSpecification for its executor already at stage A,
                // while its creation (ChainBlock.of) - this is not obligatory, but helps in the case of
                // possible diagnostic errors.
                // However, the actual specification for them will become known only at stage B,
                // while executing corresponding static executors.
                // Of course, when we want to EXECUTE dynamic executor, we will call newExecutor method
                // below, which REQUIRES the existence of a ready specification.
                return null;
                // - No sense to add null to dynamicExecutorsCache;
                // moreover, it is prohibited ("add" method will throw an exception)
            }
//            System.out.println("!!! Add specification for executor " + executorId + ": " + specification.getName());
            dynamicSpecificationsCache.add(executorId, specification);
            return specification;
        }
    }

    /**
     * <p>Creates new instance of {@link ExecutionBlock} on the base of its {@link #getSpecification(String)
     * specification} with help of the following call:
     * <pre>
     *      {@link #loaderSet() loaderSet()}.{@link ExecutorLoaderSet#newExecutor
     *      newExecutor}({@link #sessionId() sessionId()}, specification, createMode);
     * </pre>
     *
     * @param executorId unique ID of the executor.
     * @param createMode what should we do after successful instantiating the executor?
     * @return newly created executor.
     * @throws ClassNotFoundException    if Java class, required for creating the executor,
     *                                   is not available in the current <code>classpath</code> environment.
     * @throws ExecutorExpectedException if there is no requested executor.
     * @throws NullPointerException      if <code>executorId==null</code> or <code>createMode==null</code>.
     */
    @Override
    public ExecutionBlock newExecutor(String executorId, CreateMode createMode)
            throws ClassNotFoundException, ExecutorExpectedException {
        Objects.requireNonNull(executorId, "Null executorId");
        Objects.requireNonNull(createMode, "Null createMode");
        synchronized (lock) {
            final ExecutorSpecification specification = getSpecification(executorId);
            if (specification == null) {
                throw new ExecutorExpectedException("Cannot create executor: non-registered ID \"" + executorId + "\"");
            }
            return loaderSet.newExecutor(sessionId, specification, createMode);
        }
    }

    @Override
    public String toString() {
        return "default executor factory for session \"" + sessionId + "\"";
    }
}
