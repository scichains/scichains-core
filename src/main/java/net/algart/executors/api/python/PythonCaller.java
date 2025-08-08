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

import net.algart.bridges.jep.api.JepAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.AtomicPyCallable;
import net.algart.jep.additions.AtomicPyObject;
import net.algart.jep.additions.JepInterpretation;

import java.util.Objects;

public final class PythonCaller implements Cloneable, AutoCloseable {
    private static final boolean REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES = true;
    // Should be true. We could create a new container for every instance, but we prefer reusing the same one
    // with the same SharedInterpreter and the same single-thread pool.
    // Thus, we can be sure that the number of such thread pools in a multi-chain system
    // will not be greater than the number of DIFFERENT Python executors.

    private final PythonCallerSpecification specification;
    private final PythonCallerSpecification.Python python;
    private volatile JepPerformerContainer container;
    private final JepInterpretation.Mode interpretationMode;
    private final JepAPI jepAPI = JepAPI.getInstance();
    private volatile AtomicPyObject pythonClassInstance = null;

    private final Object lock = new Object();
    // - Note: copied while cloning! This lock little simplifies understanding logic in a multithreaded environment.
    // It is also important for stable access to pythonClassInstance

    private PythonCaller(PythonCallerSpecification specification) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.python = specification.getPython();
        if (python == null) {
            final var file = specification.getSpecificationFile();
            throw new IllegalArgumentException("JSON" + (file == null ? "" : " " + file)
                    + " is not a Python executor configuration: no \"python\" section");
        }
        final JepInterpretation.Mode mode = this.python.getMode();
        if (mode.isPure()) {
            throw new IllegalArgumentException("Pure interpreter (" + mode + "is not allowed");
        }
        this.interpretationMode = mode;
        this.container = JepAPI.newContainer(mode);
    }

    public static PythonCaller of(PythonCallerSpecification specification) {
        return new PythonCaller(specification);
    }

    public PythonCallerSpecification specification() {
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

    public JepInterpretation.Mode interpretationMode() {
        return interpretationMode;
    }

    public boolean isGlobalSynchronizationRequired() {
        return interpretationMode.isJVMGlobal();
    }

    public JepPerformer performer() {
        synchronized (lock) {
//            System.out.println("!!! Opening " + container);
            return container.performer();
        }
    }

    public AtomicPyObject pythonClassInstance() {
        synchronized (lock) {
            return pythonClassInstance;
        }
    }

    public void initialize(Executor executor) {
        synchronized (lock) {
            @SuppressWarnings("resource") final JepPerformer performer = performer();
//         jepAPI.initializedGlobalEnvironment(performer, executor, null);
            // - We do not call initialize the global environment (the previous commented line):
            // if the same PythonCaller reuses the same SharedInterpreter,
            // it can lead to invalid value of the global variable _env
            if (python.isClassMethod()) {
                final String className = python.getClassName();
                performer.perform(JepPerformer.importCode(python.getModule(), className));
                if (pythonClassInstance == null) {
                    pythonClassInstance = performer.newObject(className);
                }
            } else {
                performer.perform(JepPerformer.importCode(python.getModule(), python.getFunction()));
            }
        }
    }

    public AtomicPyObject loadParameters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        AtomicPyObject parameters = jepAPI.newAPIObject(performer(), python.getParametersClass());
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
                throw new IllegalStateException("initialize() was not called correcly");
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
    public PythonCaller clone() {
        try {
            if (REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES) {
                return (PythonCaller) super.clone();
            } else {
                PythonCaller clone = (PythonCaller) super.clone();
                clone.container = JepAPI.newContainer(interpretationMode);
                return clone;
            }
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
