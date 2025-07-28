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

import net.algart.jep.JepPerformer;
import net.algart.jep.JepPerformerContainer;
import net.algart.jep.additions.AtomicPyCallable;
import net.algart.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.api.JepAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;

import java.util.Objects;

public final class PythonCaller implements Cloneable, AutoCloseable {
    private final PythonCallerSpecification specification;
    private final PythonCallerSpecification.Python python;
    private final JepPerformerContainer container;
    private final JepAPI jepAPI = JepAPI.getInstance();
    private volatile AtomicPyObject instance = null;

    private final Object lock = new Object();

    private PythonCaller(PythonCallerSpecification specification) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.python = specification.getPython();
        if (python == null) {
            final var file = specification.getSpecificationFile();
            throw new IllegalArgumentException("JSON" + (file == null ? "" : " " + file)
                    + " is not a Python executor configuration: no \"python\" section");
        }
        this.container = JepAPI.getContainer();
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

    public JepPerformer performer() {
        return container.performer();
    }

    public AtomicPyObject pythonInstance() {
        synchronized (lock) {
            return instance;
        }
    }

    public void initialize(Executor executor) {
        final JepPerformer performer = performer();
        jepAPI.initializedGlobalEnvironment(performer, executor);
        if (python.isClassMethod()) {
            final String className = python.getClassName();
            performer.perform(JepPerformer.importCode(python.getModule(), className));
            synchronized (lock) {
                if (instance == null) {
                    instance = performer.newObject(className);
                }
            }
        } else {
            performer.perform(JepPerformer.importCode(python.getModule(), python.getFunction()));
        }
    }

    public AtomicPyObject loadParameters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        AtomicPyObject params = jepAPI.newAPIObject(performer(), python.getParamsClass());
        jepAPI.loadParameters(executor, params);
        return params;
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

    public Object callPython(AtomicPyObject params, AtomicPyObject inputs, AtomicPyObject outputs) {
        if (python.isClassMethod()) {
            @SuppressWarnings("resource") final AtomicPyObject instance = pythonInstance();
            if (instance == null) {
                throw new IllegalStateException("initialize() was not called correcly");
            }
            try (final AtomicPyCallable method = instance.getCallable(python.getFunction())) {
                return method.callAs(
                        Object.class,
                        params.pyObject(),
                        inputs.pyObject(),
                        outputs.pyObject());
            }
        } else {
            @SuppressWarnings("resource") final JepPerformer performer = performer();
            return performer.invokeFunction(python.getFunction(),
                    params.pyObject(),
                    inputs.pyObject(),
                    outputs.pyObject());
        }
    }

    @Override
    public PythonCaller clone() {
        try {
            return (PythonCaller) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
        container.close();
    }
}
