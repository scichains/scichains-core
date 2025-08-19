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

package net.algart.jep.additions;

import jep.Interpreter;
import jep.JepConfig;
import jep.JepException;
import jep.python.PyCallable;
import jep.python.PyObject;

import java.lang.System.Logger;
import java.lang.ref.Cleaner;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class JepSingleThreadInterpreter implements Interpreter {
    static final Logger LOG = System.getLogger(JepSingleThreadInterpreter.class.getName());

    private static final Cleaner CLEANER = Cleaner.create();

    private final JepType type;
    private final ExpensiveState state;
    private final Cleaner.Cleanable cleanable;

    private JepSingleThreadInterpreter(JepType type, Supplier<JepConfig> configurationSupplier) {
        Objects.requireNonNull(type, "Null JEP interpretation type");
        this.type = type;
        final ExpensiveCleanableState cleanableState = new ExpensiveCleanableState(type, configurationSupplier);
        this.state = cleanableState;
        this.cleanable = CLEANER.register(this, cleanableState);
        checkStateAlive();
    }

    public static Object getGlobalLock() {
        return GLOBAL_LOCK;
    }

    public static JepSingleThreadInterpreter newInstance(JepType type, Supplier<JepConfig> configurationSupplier) {
        Objects.requireNonNull(type, "Null JEP interpretation type");
        return new JepSingleThreadInterpreter(type, configurationSupplier);
    }

    public JepType type() {
        return type;
    }

    public JepConfig configuration() {
        checkStateAlive();
        return state.configuredInterpreter.configuration();
    }

    public <T> T executeInSingleThread(Callable<T> task) {
        Objects.requireNonNull(task, "Null task");
        synchronized (state.lock) {
            checkStateAlive();
            checkClosed();
            try {
                return state.singleThreadPool.submit(task).get();
            } catch (InterruptedException | ExecutionException e) {
                throw translateException(e);
            }
        }
    }

    public void executeInSingleThread(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        synchronized (state.lock) {
            checkStateAlive();
            checkClosed();
            try {
                state.singleThreadPool.submit(task).get();
            } catch (InterruptedException | ExecutionException e) {
                throw translateException(e);
            }
        }
    }

    public AtomicPyObject wrapObject(PyObject pyObject) {
        return new AtomicPyObject(this, pyObject);
    }

    public AtomicPyCallable wrapCallable(PyCallable pyCallable) {
        return new AtomicPyCallable(this, pyCallable);
    }

    @Override
    public Object invoke(String name, Object... args) throws JepException {
        Objects.requireNonNull(name, "Null name");
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().invoke(name, args));
    }

    @Override
    public Object invoke(String name, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(name, "Null name");
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().invoke(name, kwargs));
    }

    @Override
    public Object invoke(String name, Object[] args, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(name, "Null name");
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().invoke(name, args, kwargs));
    }

    @Override
    public boolean eval(String str) throws JepException {
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().eval(str));
    }

    @Override
    public void exec(String str) throws JepException {
        checkStateAlive();
        executeInSingleThread(() -> state.configuredInterpreter.interpreter().exec(str));
    }

    @Override
    public void runScript(String script) throws JepException {
        checkStateAlive();
        executeInSingleThread(() -> state.configuredInterpreter.interpreter().runScript(script));
    }

    @Override
    public Object getValue(String name) throws JepException {
        Objects.requireNonNull(name, "Null name");
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().getValue(name));
    }

    @Override
    public <T> T getValue(String name, Class<T> clazz) throws JepException {
        Objects.requireNonNull(name, "Null name");
        Objects.requireNonNull(clazz, "Null class");
        checkStateAlive();
        return executeInSingleThread(() -> state.configuredInterpreter.interpreter().getValue(name, clazz));
    }

    @Override
    public void set(String name, Object v) throws JepException {
        Objects.requireNonNull(name, "Null name");
        checkStateAlive();
        executeInSingleThread(() -> state.configuredInterpreter.interpreter().set(name, v));
    }

    public boolean isClosed() {
        return state.isReallyClosed();
    }

    @Override
    public void close() throws JepException {
        state.normallyClosed = true;
        cleanable.clean();
    }

    @Override
    public String toString() {
        return state.toBriefString();
    }

    private void checkStateAlive() {
        if (state.configuredInterpreter == null) {
            throw new IllegalStateException("Abnormal situation: interpreter is closed");
            // - should not occur while normal usage
        }
    }

    private void checkClosed() {
        if (state.normallyClosed || state.isReallyClosed()) {
            // - will be true even WHILE executing cleanable.clean
            throw new IllegalStateException("Cannot use " + this);
        }
    }


    private static AssertionError translateException(Throwable exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            throw new JepException("Interruption of JEP operation", exception);
        }
        if (exception instanceof ExecutionException) {
            final Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException && !(cause instanceof JepException)) {
                final String message = cause.getMessage();
                throw new JepException(
                        "Exception in JEP thread: " + (message == null ? cause : message),
                        cause);
            }
            throwUncheckedException(cause);
        }
        throwUncheckedException(exception);
        return new AssertionError("Cannot occur");
    }

    private static void throwUncheckedException(Throwable exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
        throw new AssertionError("Impossible checked exception: " + exception);
    }

    private static class ExpensiveState {
        private final String name;
        private final boolean globalThreadPool;
        private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
        private volatile ThreadPoolExecutor singleThreadPool;
        private volatile ConfiguredInterpreter configuredInterpreter;
        private volatile boolean normallyClosed = false;
        private final Object lock = new Object();

        ExpensiveState(JepType type, Supplier<JepConfig> configurationSupplier) {
            this(() -> type.newLowLevelInterpreter(configurationSupplier),
                    type.typeName(),
                    type.isJVMGlobal());
        }

        private ExpensiveState(
                Supplier<ConfiguredInterpreter> configuredInterpreterSupplier,
                String name,
                boolean globalThreadPool) {
            if (name == null) {
                name = "unknown";
            }
            this.name = name;
            this.globalThreadPool = globalThreadPool;
            if (globalThreadPool) {
                this.singleThreadPool = JVMGlobalThreadPoolHolder.INSTANCE.getSingleThreadPool();
            } else {
                String threadName = "JEP " + name + " execution thread #" + THREAD_COUNT.getAndIncrement();
                LOG.log(Logger.Level.DEBUG, () -> "Creating " + threadName);
                this.singleThreadPool = newSingleThreadPool(threadName);
            }
            try {
                LOG.log(Logger.Level.DEBUG, () -> "Creating JEP " + this.name + " interpreter");
                this.configuredInterpreter = this.singleThreadPool.submit(configuredInterpreterSupplier::get).get();
                if (configuredInterpreter == null) {
                    throw new IllegalArgumentException("Illegal configuredInterpreterSupplier: created null result");
                }
            } catch (Throwable e) {
                shutdownLocalThreadPool(e);
                throw translateException(e);
            }
            LOG.log(Logger.Level.DEBUG, () -> "Created " + toBriefString());
            assert configuredInterpreter != null : "already checked above: configuredInterpreter == null";
            assert singleThreadPool != null : "created via new operation above: singleThreadPool == null";
        }

        void closeInterpreter() {
            synchronized (this.lock) {
                if (isReallyClosed()) {
                    return;
                }
                if (normallyClosed) {
                    LOG.log(Logger.Level.TRACE, () -> "Normal closing " + this);
                } else {
                    LOG.log(Logger.Level.INFO, () -> "CLEANING " + toBriefString());
                }
                try {
                    singleThreadPool.submit(configuredInterpreter::close).get();
                    LOG.log(Logger.Level.DEBUG, () -> "Closed JEP " + this.name + " interpreter");
                } catch (InterruptedException | ExecutionException e) {
                    throw translateException(e);
                }
                shutdownLocalThreadPool(null);
                configuredInterpreter = null;
            }
        }

        private void shutdownLocalThreadPool(Throwable exception) {
            assert singleThreadPool != null :
                    "shutdownLocalThreadPool() was called twice or without normal creation: singleThreadPool == null";
            if (!globalThreadPool) {
                LOG.log(Logger.Level.DEBUG, () -> "Shutting down JEP " + name + " thread pool" +
                        (exception == null ? "" : " (because of " + exception + ")"));
                singleThreadPool.shutdownNow();
                // - not just shutdown(): no sense to continue executing waiting tasks,
                // because jepInterpreter is already closed and will not be able to process anything
            }
            singleThreadPool = null;
        }

        public String toBriefString() {
            return (!isReallyClosed() ? "" : normallyClosed ? "closed " : "abnormally CLEANED ")
                    + "JEP single-thread " + name + " interpreter";
        }

        public String toString() {
            return toBriefString() + " in " + singleThreadPool + " (" + configuredInterpreter + ")";
        }


        private void checkClosed(String name) {
            if (normallyClosed || isReallyClosed()) {
                // - will be true even WHILE executing cleanable.clean
                throw new IllegalStateException("Cannot use " + name);
            }
        }

        static ThreadPoolExecutor newSingleThreadPool(String threadName) {
            return new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        final Thread t = new Thread(r, threadName);
                        t.setDaemon(true);
                        return t;
                    });
            // Note: we could make this pool more flexible with calls
            //      result.allowCoreThreadTimeOut(true);
            //      result.setKeepAliveTime(60, TimeUnit.SECONDS);
            // But in any case we MUST ensure that the same ShareInterpreter is running inside the same thread,
            // so we can allow the timeout only AFTER closing the ShareInterpreter and
            // should recreate if after restarting a new thread.
            // Instead, we prefer just to shut down this pool together with closing the ShareInterpreter
            // (closeInterpreter() method called from JepSingleThreadInterpreter.close())
        }

        private boolean isReallyClosed() {
            return configuredInterpreter == null;
        }
    }

    private static class ExpensiveCleanableState extends ExpensiveState implements Runnable {
        public ExpensiveCleanableState(JepType type, Supplier<JepConfig> configurationSupplier) {
            super(type, configurationSupplier);
        }

        // This method is called by JepSingleThreadInterpreter.CLEANER object
        @Override
        public void run() {
            closeInterpreter();
        }
    }

    private final static Object GLOBAL_LOCK = new Object();
    private static class JVMGlobalThreadPoolHolder {
        static final JVMGlobalThreadPoolHolder INSTANCE = new JVMGlobalThreadPoolHolder();

        private final ThreadPoolExecutor singleThreadPool;

        private JVMGlobalThreadPoolHolder() {
            final String threadName = "JEP JVM-global execution thread";
            LOG.log(Logger.Level.INFO, () -> "Creating " + threadName);
            this.singleThreadPool = ExpensiveState.newSingleThreadPool(threadName);
        }

        public ThreadPoolExecutor getSingleThreadPool() {
            return singleThreadPool;
        }
    }
}
