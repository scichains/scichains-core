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

package net.algart.executors.modules.core.logic.compiler.python.interpreters;

import net.algart.bridges.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.executors.modules.core.logic.compiler.python.UsingPython;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.logic.compiler.python.model.PythonCaller;

import java.util.Locale;

public class InterpretPython extends Executor implements ReadOnlyExecutionInput {
    private volatile PythonCaller pythonCaller = null;

    public InterpretPython() {
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void initialize() {
        //noinspection resource
        pythonCaller().initialize();
    }

    @Override
    public void process() {
        long t1 = System.nanoTime(), t2, t3, t4;
        @SuppressWarnings("resource")
        final PythonCaller pythonCaller = pythonCaller();
        try (AtomicPyObject params = pythonCaller.loadParameters(this);
             AtomicPyObject inputs = pythonCaller.readInputPorts(this);
             AtomicPyObject outputs = pythonCaller.createOutputs()) {
            t2 = debugTime();
            final Object result = pythonCaller.callPython(params, inputs, outputs);
            t3 = debugTime();
            pythonCaller.writeOutputPorts(this, outputs);
            pythonCaller.writeOptionalOutputPort(this, DEFAULT_OUTPUT_PORT, result, true);
            // - note: direct assignment "outputs.output = xxx" overrides simple returning result
            t4 = debugTime();
        }
        setSystemOutputs();
        logDebug(() -> String.format(Locale.US,
                "Python \"%s\" executed in %.5f ms:"
                        + " %.6f ms loading inputs + %.6f ms calling + %.6f ms returning outputs",
                pythonCaller.name(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    public PythonCaller pythonCaller() {
        PythonCaller pythonCaller = this.pythonCaller;
        if (pythonCaller == null) {
            pythonCaller = UsingPython.pythonCallerLoader().reqRegisteredWorker(getExecutorId());
            pythonCaller = pythonCaller.clone();
            // - we return a clone!
            this.pythonCaller = pythonCaller;
            // - the order is important for multithreading: local pythonCaller is assigned first,
            // this.pythonCaller is assigned to it;
            // cloning is not necessary in current version, but added for possible future extensions
        }
        return pythonCaller;
    }

    @Override
    public void close() {
        PythonCaller pythonCaller = this.pythonCaller;
        if (pythonCaller != null) {
            this.pythonCaller = null;
            pythonCaller.close();
        }
        super.close();
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private void setSystemOutputs() {
        if (isOutputNecessary(UsingPython.SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            getScalar(UsingPython.SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME).setTo(
                    String.join(String.format("%n"), JepPlatforms.pythonRootFolders()));
        }
        if (isOutputNecessary(UsingPython.SUPPLIED_PYTHON_MODELS_OUTPUT_NAME)) {
            getScalar(UsingPython.SUPPLIED_PYTHON_MODELS_OUTPUT_NAME).setTo(
                    String.join(String.format("%n"),
                            JepPlatforms.pythonPlatforms().installedModelFolders()));
        }
    }
}
