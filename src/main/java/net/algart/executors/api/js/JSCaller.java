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

package net.algart.executors.api.js;

import net.algart.graalvm.GraalPerformer;
import net.algart.graalvm.GraalPerformerContainer;
import net.algart.graalvm.GraalSourceContainer;
import net.algart.executors.api.graalvm.GraalAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.Objects;

public final class JSCaller implements Cloneable, AutoCloseable {
    private static final boolean REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES = false;
    // Should be false: otherwise, we will need strict synchronization to avoid parallel
    // call of the same JSCaller from other SciChains windows.

    private final JSCallerSpecification specification;
    private final Path workingDirectory;
    private final JSCallerSpecification.JS js;
    private volatile GraalPerformerContainer.Local performerContainer;
    // volatile is not necessary, but COULD become necessary if we will not use synchronization:
    // it is not "final" because of clone() method, so JVM do not provide the same guarantees as for "final"
    private final GraalAPI graalAPI = GraalAPI.getInstance()
            .setConvertInputScalarToNumber(false)
            .setConvertInputNumbersToArray(false)
            .setConvertOutputIntegersToBriefForm(true);
    private final GraalSourceContainer importCode = GraalSourceContainer.newLiteral();
    private volatile Value mainFunction = null;
    private volatile Value createEmptyObjectFunction = null;

    private final Object lock = new Object();
    // - Note: copied while cloning! This lock little simplifies understanding logic in a multithreaded environment.

    private JSCaller(JSCallerSpecification specification, Path workingDirectory) {
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "Null workingDirectory");
        this.js = specification.getJS();
        if (js == null) {
            final Path file = specification.getSpecificationFile();
            throw new IllegalArgumentException("JSON" + (file == null ? "" : " " + file)
                    + " is not a JS executor configuration: no \"JS\" section");
        }
        createPerformerContainer();
    }

    public static JSCaller of(JSCallerSpecification specification, Path workingDirectory) {
        return new JSCaller(specification, workingDirectory);
    }

    public JSCallerSpecification specification() {
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

    public GraalPerformer performer() {
        synchronized (lock) {
            return performerContainer.performer();
        }
    }

    public void initialize() {
        synchronized (lock) {
            importCode.setModuleJS(GraalPerformer.importAndReturnJSFunction(js.getModule(), js.getFunction()),
                    "importing");
            // - name "importing" is not important: we will not use share this performer (Graal context)
            // Note: no sense to check importCode.changed(), because it cannot change until reloading the entire chain.
            final GraalPerformer performer = performer();
            mainFunction = performer.perform(importCode);
            createEmptyObjectFunction = GraalAPI.storedCreateEmptyObjectJSFunction(performer);
        }
    }

    public Value loadParameters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Value parameters = createEmptyObjectFunction.execute();
        graalAPI.loadSystemParameters(executor, parameters, null);
        graalAPI.loadParameters(executor, parameters);
        return parameters;
    }

    public Value readInputPorts(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Value inputs = createEmptyObjectFunction.execute();
        graalAPI.readInputPorts(executor.inputPorts(), inputs);
        return inputs;
    }

    public Value createOutputs() {
        return createEmptyObjectFunction.execute();
    }

    public void writeOutputPorts(Executor executor, Value outputs) {
        Objects.requireNonNull(executor, "Null executor");
        graalAPI.writeOutputPorts(executor.outputPorts(), outputs);
    }

    public void writeOptionalOutputPort(Executor executor, String portName, Value value, boolean preserveExisting) {
        Objects.requireNonNull(executor, "Null executor");
        if (value != null) {
            final Port outputPort = executor.getOutputPort(portName);
            if (outputPort != null) {
                graalAPI.writeOutputPort(outputPort, value, preserveExisting);
            }
        }
    }

    public Value callJS(Value parameters, Value inputs, Value outputs) {
        return mainFunction.execute(parameters, inputs, outputs);
    }

    @Override
    public JSCaller clone() {
        try {
            JSCaller clone = (JSCaller) super.clone();
            if (!REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES) {
                clone.createPerformerContainer();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closePerformerContainer();
        }
    }

    private void closePerformerContainer() {
        this.mainFunction = null;
        // - enforce re-creating this function by perform()
        this.performerContainer.freeResources();
    }

    private void createPerformerContainer() {
        this.performerContainer = GraalPerformerContainer.getLocalAllAccess(workingDirectory);
        GraalAPI.initializeJS(this.performerContainer);
    }
}
