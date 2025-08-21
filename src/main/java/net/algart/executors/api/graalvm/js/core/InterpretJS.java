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

package net.algart.executors.api.graalvm.js.core;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.graalvm.js.JSCaller;
import org.graalvm.polyglot.Value;

import java.util.Locale;

public class InterpretJS extends Executor implements ReadOnlyExecutionInput {
    private volatile JSCaller jsCaller = null;

    public InterpretJS() {
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void initialize() {
        useVisibleResultParameter();
        //noinspection resource
        jsCaller().initialize();
    }

    @Override
    public void process() {
        long t1 = System.nanoTime(), t2, t3, t4;
        @SuppressWarnings("resource") final JSCaller jsCaller = jsCaller();
        Value parameters = jsCaller.loadParameters(this);
        Value inputs = jsCaller.readInputPorts(this);
        Value outputs = jsCaller.createOutputs();
        t2 = debugTime();
        final Value result = jsCaller.callJS(parameters, inputs, outputs);
        t3 = debugTime();
        jsCaller.writeOutputPorts(this, outputs);
        jsCaller.writeOptionalOutputPort(this, DEFAULT_OUTPUT_PORT, result, true);
        // - note: direct assignment "outputs.output = xxx" overrides simple returning result
        t4 = debugTime();
        setSystemOutputs();
        logDebug(() -> String.format(Locale.US,
                "JS \"%s\" executed in %.5f ms:"
                        + " %.6f ms loading inputs + %.6f ms calling + %.6f ms returning outputs",
                jsCaller.name(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    public JSCaller jsCaller() {
        final String sessionId = getSessionId();
        final String executorId = getExecutorId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot find JavaScript worker: session ID is not set");
        }
        if (executorId == null) {
            throw new IllegalStateException("Cannot find JavaScript worker: executor ID is not set");
        }
        JSCaller jsCaller = this.jsCaller;
        if (jsCaller == null) {
            jsCaller = UseJS.jsCallerLoader().registeredWorker(sessionId, executorId);
            jsCaller = jsCaller.clone();
            // - we return a clone!
            this.jsCaller = jsCaller;
            // - the order is important for multithreading: local jsCaller is assigned first,
            // this.jsCaller is assigned to it;


            // cloning is not necessary in the current version, but added for possible future extensions
        }
        return jsCaller;
    }

    @Override
    public void close() {
        JSCaller jsCaller = this.jsCaller;
        if (jsCaller != null) {
            this.jsCaller = null;
            jsCaller.close();
        }
        super.close();
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private void setSystemOutputs() {
        // nothing in the current version
    }
}
