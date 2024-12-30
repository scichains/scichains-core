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

package net.algart.executors.modules.core.logic.compiler.js.model;

import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.Port;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.Objects;

public final class JSCaller implements Cloneable, AutoCloseable {
    private final JSCallerSpecification model;
    private final JSCallerSpecification.JSConf jsConf;
    private final GraalPerformerContainer.Local performerContainer;
    private final GraalAPI graalAPI = GraalAPI.getInstance()
            .setConvertInputScalarToNumber(false)
            .setConvertInputNumbersToArray(false)
            .setConvertOutputIntegersToBriefForm(true);
    private final GraalSourceContainer importCode = GraalSourceContainer.newLiteral();
    private volatile Value mainFunction = null;
    private volatile Value createEmptyObjectFunction = null;

    private JSCaller(JSCallerSpecification model, Path workingDirectory) {
        this.model = Objects.requireNonNull(model, "Null model");
        Objects.requireNonNull(workingDirectory, "Null workingDirectory");
        this.jsConf = model.getJS();
        if (jsConf == null) {
            final var file = model.getExecutorSpecificationFile();
            throw new IllegalArgumentException("JSON" + (file == null ? "" : " " + file)
                    + " is not a JS executor configuration: no \"JS\" section");
        }
        this.performerContainer = GraalPerformerContainer.getLocalAllAccess();
        this.performerContainer.setWorkingDirectory(workingDirectory);
        GraalAPI.initializeJS(this.performerContainer);
    }

    public static JSCaller valueOf(JSCallerSpecification model, Path workingDirectory) {
        return new JSCaller(model, workingDirectory);
    }

    public JSCallerSpecification model() {
        return model;
    }

    public String executorId() {
        return model.getExecutorId();
    }

    public String name() {
        return model.getName();
    }

    public String platformId() {
        return model.getPlatformId();
    }

    public GraalPerformer performer() {
        return performerContainer.performer();
    }

    public void initialize() {
        importCode.setModuleJS(GraalPerformer.importAndReturnJSFunction(jsConf.getModule(), jsConf.getFunction()),
                "importing");
        // - name "importing" is not important: we will not use share this performer (Graal context)
        // Note: no sense to check importCode.changed(), because it cannot change until reloading all chain.
        final GraalPerformer performer = performer();
        mainFunction = performer.perform(importCode);
        createEmptyObjectFunction = GraalAPI.storedCreateEmptyObjectJSFunction(performer);
    }

    public Value loadParameters(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Value params = createEmptyObjectFunction.execute();
        graalAPI.loadParameters(executor, params);
        return params;
    }

    public Value readInputPorts(Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        final Value inputs = createEmptyObjectFunction.execute();
        graalAPI.readInputPorts(executor.allInputPorts(), inputs);
        return inputs;
    }

    public Value createOutputs() {
        return createEmptyObjectFunction.execute();
    }

    public void writeOutputPorts(Executor executor, Value outputs) {
        Objects.requireNonNull(executor, "Null executor");
        graalAPI.writeOutputPorts(executor.allOutputPorts(), outputs);
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

    public Value callJS(Value params, Value inputs, Value outputs) {
        return mainFunction.execute(params, inputs, outputs);
    }

    @Override
    public JSCaller clone() {
        try {
            return (JSCaller) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        closePerformerContainer();
    }

    private void closePerformerContainer() {
        this.mainFunction = null;
        // - enforce re-creating this function by perform()
        this.performerContainer.freeResources();
    }
}
