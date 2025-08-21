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

package net.algart.executors.api.python;

import net.algart.executors.api.jep.JepAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.AtomicPyCallable;
import net.algart.jep.additions.AtomicPyObject;
import net.algart.jep.additions.JepInterpretation;
import net.algart.jep.additions.JepType;

import java.util.Objects;

public final class JepCaller implements Cloneable, AutoCloseable {
    private static final boolean REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES = true;
    // Should be true. We could create a new container for every instance, but we prefer reusing the same one
    // with the same SharedInterpreter and the same single-thread pool.
    // Thus, we can be sure that the number of such thread pools in a multi-chain system
    // will not be greater than the number of DIFFERENT Python executors.
    // Another reason: different instances of SharedInterpreter still use the same global variables,
    // and there are no any ways to provide correct access to them in Python from parallel threads
    // (which become possible when using different containers with different single-thread pools).

    private static final boolean DEBUG_SLEEP_FOR_PARALLEL_EXECUTION = false;
    // Must be false.

    private final PythonSpecification specification;
    private final PythonSpecification.Python python;
    private volatile JepPerformerContainer container;
    // volatile is not necessary, but COULD become necessary if we will not use synchronization
    // it is not "final" because of clone() method, so JVM do not provide the same guarantees as for "final"
    private final JepType type;
    private final JepAPI jepAPI = JepAPI.getInstance();
    private volatile AtomicPyObject pythonClassInstance = null;

    private final Object lock = new Object();
    // - Note: copied while cloning! This lock little simplifies understanding logic in a multithreaded environment.
    // It is also important for stable access to pythonClassInstance

    private JepCaller(PythonSpecification specification) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.python = specification.getPython();
        if (python == null) {
            final var file = specification.getSpecificationFile();
            throw new IllegalArgumentException(
                    "JSON" + (file == null ? "" : " " + file)
                    + " is not a Python executor configuration: no \"python\" section");
        }
        final JepType type = this.python.getJepType();
        if (type.isPure()) {
            throw new IllegalArgumentException("Pure interpreter (" + type + "is not allowed");
        }
        this.type = type;
        this.container = JepAPI.newContainer(type);
    }

    public static JepCaller of(PythonSpecification specification) {
        return new JepCaller(specification);
    }

    public PythonSpecification specification() {
        return specification;
    }

    public String executorId() {
        return specification.getId();
    }

    public String name() {
        return specification.getName();
    }

    public String platformId() {
        return specification.getPlatformId();
    }

    public JepType type() {
        return type;
    }

    public boolean isGlobalSynchronizationRequired() {
        return type.isJVMGlobal();
    }

    public JepPerformer performer() {
        synchronized (lock) {
            return container.performer();
        }
    }

    public AtomicPyObject pythonClassInstance() {
        synchronized (lock) {
            return pythonClassInstance;
        }
    }

    /**
     * Executes a sequence of actions under the internal synchronization lock of this object.
     * This is necessary if the methods of this object are called in a multithreaded environment.
     * Here:
     * <ul>
     *     <li><code>initializing</code>:
     *     initialized python function (usually {@link #initialize(Executor)};</li>
     *     <li><code>processing</code>: performs the main logic, typically using the {@link #loadParameters(Executor)},
     *     {@link #readInputPorts(Executor)}, {@link #writeOutputPorts(Executor, AtomicPyObject)} and
     *     {@link #callPython(AtomicPyObject, AtomicPyObject, AtomicPyObject)};</li>
     * </ul>
     *
     * <p>This method uses <code>synchronized (lock) {...}</code> operators
     * for executing all 2 stages.
     *
     * @param initializing create and initialize the interpreter.
     * @param processing   process Python operations.
     * @throws NullPointerException if any of arguments is <code>null</code>.
     */
    public void executeWithLock(Runnable initializing, Runnable processing) {
        Objects.requireNonNull(initializing, "Null initializing");
        Objects.requireNonNull(processing, "Null processing");
        synchronized (lock) {
            initializing.run();
            processing.run();
        }
    }

    public void initialize(Executor executor) {
        synchronized (lock) {
            @SuppressWarnings("resource") final JepPerformer performer = performer();
//         jepAPI.initializedGlobalEnvironment(performer, executor, null);
            // - We do not call initialize the global environment (the previous commented line):
            // if the same JepCaller in another InterpretPython instance reuses the same SharedInterpreter,
            // it can lead to invalid value of the global variable _env
            if (python.isClassMethod()) {
                final String className = python.getClassName();
                performer.perform(JepInterpretation.importCode(python.getModule(), className));
                if (pythonClassInstance == null) {
                    pythonClassInstance = performer.newObject(className);
                }
            } else {
                performer.perform(JepInterpretation.importCode(python.getModule(), python.getFunction()));
            }
        }
        if (DEBUG_SLEEP_FOR_PARALLEL_EXECUTION) {
            System.out.println("~~~ Sleeping 10 seconds for " + container);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
            System.out.println("~~~ ...finish sleeping for " + container);
        }
    }

    public AtomicPyObject loadParameters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final JepPerformer performer = performer();
        AtomicPyObject parameters = jepAPI.newAPIObject(performer, python.getParametersClass());
        jepAPI.loadSystemParameters(executor, parameters, null);
        jepAPI.loadParameters(executor, parameters);
        return parameters;
    }

    public AtomicPyObject readInputPorts(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final JepPerformer performer = performer();
        AtomicPyObject inputs = jepAPI.newAPIObject(performer, python.getInputsClass());
        jepAPI.readInputPorts(performer, executor.inputPorts(), inputs);
        return inputs;
    }

    public AtomicPyObject createOutputs() {
        return jepAPI.newAPIObject(performer(), python.getOutputsClass());
    }

    public void writeOutputPorts(Executor executor, AtomicPyObject outputs) {
        Objects.requireNonNull(executor, "Null executor");
        jepAPI.writeOutputPorts(performer(), executor.outputPorts(), outputs);
    }

    public void writeOptionalOutputPort(Executor executor, String portName, Object value, boolean preserveExisting) {
        Objects.requireNonNull(executor, "Null executor");
        if (value != null) {
            final Port outputPort = executor.getOutputPort(portName);
            if (outputPort != null) {
                jepAPI.writeOutputPort(performer(), outputPort, value, preserveExisting);
            }
        }
    }

    public Object callPython(AtomicPyObject parameters, AtomicPyObject inputs, AtomicPyObject outputs) {
        if (python.isClassMethod()) {
            @SuppressWarnings("resource") final AtomicPyObject instance = pythonClassInstance();
            if (instance == null) {
                throw new IllegalStateException("initialize() was not called correctly");
            }
            try (final AtomicPyCallable method = instance.getAtomicCallable(python.getFunction())) {
                return method.callAs(
                        Object.class,
                        parameters.pyObject(),
                        inputs.pyObject(),
                        outputs.pyObject());
            }
        } else {
            @SuppressWarnings("resource") final JepPerformer performer = performer();
            return performer.invokeFunction(python.getFunction(),
                    parameters.pyObject(),
                    inputs.pyObject(),
                    outputs.pyObject());
        }
    }

    @Override
    public JepCaller clone() {
        try {
            JepCaller clone = (JepCaller) super.clone();
            if (!REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES) {
                clone.container = JepAPI.newContainer(type);
            }
            return clone;

        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (pythonClassInstance != null) {
                pythonClassInstance.close();
                pythonClassInstance = null;
            }
            container.close();
//            System.out.println("!!! Closing " + container);
        }
    }
}
