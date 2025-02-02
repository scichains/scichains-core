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

package net.algart.bridges.jep.additions;

import jep.Interpreter;
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

    private final ExpensiveCleanableState state;
    private final Cleaner.Cleanable cleanable;

    private JepSingleThreadInterpreter(Supplier<Interpreter> interpreterSupplier, String name) {
        Objects.requireNonNull(interpreterSupplier, "Null interpreterSupplier");
        this.state = new ExpensiveCleanableState(interpreterSupplier, name);
        this.cleanable = CLEANER.register(this, state);
    }

    public static JepSingleThreadInterpreter newInstance(Supplier<Interpreter> interpreterSupplier, String name) {
        return new JepSingleThreadInterpreter(interpreterSupplier, name);
    }

    public static JepSingleThreadInterpreter newInstance(JepInterpreterKind jepInterpreterKind) {
        Objects.requireNonNull(jepInterpreterKind, "Null jepInterpreterKind");
        return new JepSingleThreadInterpreter(
                jepInterpreterKind::newInterpreter, jepInterpreterKind.kindName());
    }

    public <T> T executeInSingleThread(Callable<T> task) {
        Objects.requireNonNull(task, "Null task");
        synchronized (state.lock) {
            checkClosed();
            try {
                return state.singleThreadPool.submit(task).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw translateException(e);
            } catch (ExecutionException e) {
                throw translateException(e);
            }
        }
    }

    public void executeInSingleThread(Runnable task) {
        Objects.requireNonNull(task, "Null task");
        synchronized (state.lock) {
            checkClosed();
            try {
                state.singleThreadPool.submit(task).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw translateException(e);
            } catch (ExecutionException e) {
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
        return executeInSingleThread(() -> state.jepInterpreter.invoke(name, args));
    }

    @Override
    public Object invoke(String name, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(name, "Null name");
        return executeInSingleThread(() -> state.jepInterpreter.invoke(name, kwargs));
    }

    @Override
    public Object invoke(String name, Object[] args, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(name, "Null name");
        return executeInSingleThread(() -> state.jepInterpreter.invoke(name, args, kwargs));
    }

    @Override
    public boolean eval(String str) throws JepException {
        return executeInSingleThread(() -> state.jepInterpreter.eval(str));
    }

    @Override
    public void exec(String str) throws JepException {
        executeInSingleThread(() -> state.jepInterpreter.exec(str));
    }

    @Override
    public void runScript(String script) throws JepException {
        executeInSingleThread(() -> state.jepInterpreter.runScript(script));
    }

    @Override
    public Object getValue(String name) throws JepException {
        Objects.requireNonNull(name, "Null name");
        return executeInSingleThread(() -> state.jepInterpreter.getValue(name));
    }

    @Override
    public <T> T getValue(String name, Class<T> clazz) throws JepException {
        Objects.requireNonNull(name, "Null name");
        Objects.requireNonNull(clazz, "Null class");
        return executeInSingleThread(() -> state.jepInterpreter.getValue(name, clazz));
    }

    @Override
    public void set(String name, Object v) throws JepException {
        Objects.requireNonNull(name, "Null name");
        executeInSingleThread(() -> state.jepInterpreter.set(name, v));
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

    private void checkClosed() {
        if (state.normallyClosed || state.isReallyClosed()) {
            // - will be true even WHILE executing cleanable.clean
            throw new IllegalStateException("Cannot use " + this);
        }
    }


    private static AssertionError translateException(Throwable exception) {
        if (exception instanceof InterruptedException) {
            throw new JepException("Interruption of Jep operation", exception);
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

    private static class ExpensiveCleanableState implements Runnable {
        private final String name;
        private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
        private volatile ThreadPoolExecutor singleThreadPool = null;
        private volatile Interpreter jepInterpreter = null;
        private volatile boolean normallyClosed = false;
        private final Object lock = new Object();

        public ExpensiveCleanableState(Supplier<Interpreter> interpreterSupplier, String name) {
            if (name == null) {
                name = "unknown";
            }
            this.name = name;
            String threadName = "JEP " + name + " single-thread wrapping thread #" + THREAD_COUNT.getAndIncrement();
            LOG.log(System.Logger.Level.TRACE,
                    () -> "Creating thread " + threadName + "; number of active threads was " + Thread.activeCount());
            this.singleThreadPool = new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        final Thread t = new Thread(r, threadName);
                        t.setDaemon(true);
                        return t;
                    });
            try {
                this.jepInterpreter = this.singleThreadPool.submit(interpreterSupplier::get).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.singleThreadPool.shutdownNow();
                throw translateException(e);
            } catch (Throwable e) {
                this.singleThreadPool.shutdownNow();
                throw translateException(e);
            }
            LOG.log(Logger.Level.DEBUG,
                    () -> "Created " + this + "; number of active threads is " + Thread.activeCount());
        }

        @Override
        public void run() {
            synchronized (this.lock) {
                if (isReallyClosed()) {
                    return;
                }
                if (normallyClosed) {
                    LOG.log(System.Logger.Level.TRACE, () -> "Normal closing " + this);
                } else {
                    LOG.log(System.Logger.Level.WARNING, () -> "CLEANING forgotten " + this);
                }
                try {
                    singleThreadPool.submit(jepInterpreter::close).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw translateException(e);
                } catch (ExecutionException e) {
                    throw translateException(e);
                }
                singleThreadPool.shutdownNow();
                // - not just shutdown(): no sense to continue executing waiting tasks,
                // because jepInterpreter is already closed and will not be able to process anything
                singleThreadPool = null;
                jepInterpreter = null;
            }
        }

        public String toBriefString() {
            return (!isReallyClosed() ? "" : normallyClosed ? "closed " : "abnormally CLEANED ")
                    + "JEP single-thread " + name + " interpreter";
        }

        public String toString() {
            return toBriefString() + " in " + singleThreadPool + " (" + jepInterpreter + ")";
        }


        private void checkClosed(String name) {
            if (normallyClosed || isReallyClosed()) {
                // - will be true even WHILE executing cleanable.clean
                throw new IllegalStateException("Cannot use " + name);
            }
        }

        private boolean isReallyClosed() {
            return singleThreadPool == null;
        }
    }
}
