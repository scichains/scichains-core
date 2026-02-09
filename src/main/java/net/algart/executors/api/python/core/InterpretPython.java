/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.api.python.core;

import net.algart.arrays.Arrays;
import net.algart.executors.api.python.JepCaller;
import net.algart.jep.additions.AtomicPyObject;
import net.algart.executors.api.jep.JepPlatforms;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.jep.additions.JepInterpretation;

import java.util.Locale;

public class InterpretPython extends Executor implements ReadOnlyExecutionInput {
    private static final boolean ENFORCE_SHUTDOWN_ON_CLOSE = Arrays.SystemSettings.getBooleanProperty(
            "net.algart.executors.api.python.enforceShutdownOnClose", true);
    // Should be set to true, which ensures that JepCaller is closed in the close() method.
    // If we will set it to false, it will lead to staying Python threads forever:
    // the original JepCaller is registered by registeredWorker in the global space.
    // Strictly speaking, closing JepCaller is not an entirely correct operation,
    // just like we never close the global running thread, if isGlobalSynchronizationRequired()
    // (we only close SharedInterpreter instances).
    // We suppose that this JepCaller will be reused in all instances of the same Python class,
    // just like JVM reuses the same Java class and never destroys it.
    // But JepCaller is a very heavy object: it runs an OS thread in a single-thread pool,
    // and we SHOULD close it sometimes.
    // Closing this SciChains executor (InterpretPython) is a possible reason.
    // Typically, this happens when the user closes a chain, and even if we have other open windows
    // with instances of the same executor, they will be automatically revived.
    // If this becomes a serious performance issue in the future (hundreds of users on the same server),
    // we will be able to improve this solution, for example, checking 10-second delay without access
    // before actually freeing resources.

    private volatile JepCaller jepCaller = null;

    public InterpretPython() {
        useVisibleResultParameter();
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void initialize() {
        // We could call here initializePython only  if JepCaller.REUSE_SINGLE_THREAD_FOR_ALL_INSTANCES=false.
        // When reusing the thread, we MUST execute initialize+process in a synchronized block
        // because another SciChains window may close this object at the same time.
    }

    @Override
    public void process() {
        @SuppressWarnings("resource") final JepCaller jepCaller = jepCaller();
        if (jepCaller.isGlobalSynchronizationRequired()) {
            JepInterpretation.executeWithJVMGlobalLock(
                    () -> initializePython(jepCaller),
                    () -> processPython(jepCaller),
                    () -> closePython(true));

        } else {
            jepCaller.executeWithLock(
                    () -> initializePython(jepCaller),
                    () -> processPython(jepCaller)
                    // But we do not close it!
                    // Thus, when all chains are loaded and "warmed up", there should be no lack in performance.
            );
        }
    }

    @Override
    public void close() {
        closePython(ENFORCE_SHUTDOWN_ON_CLOSE);
        // - not a problem to close again even if was closed by process() in the global mode
        super.close();
    }

    public JepCaller jepCaller() {
        final String sessionId = getSessionId();
        final String executorId = getExecutorId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot find Python worker: session ID is not set");
        }
        if (executorId == null) {
            throw new IllegalStateException("Cannot find Python worker: executor ID is not set");
        }
        JepCaller jepCaller = this.jepCaller;
        if (jepCaller == null) {
            jepCaller = UsingPython.jepCallerLoader().registeredWorker(sessionId, executorId);
            jepCaller = jepCaller.clone();
            // - we return a clone!
            this.jepCaller = jepCaller;
            // - the order is important for multithreading: local jepCaller is assigned first,
            // this.jepCaller is assigned to it;
            // cloning is not necessary in the current version, but added for possible future extensions
        }
        return jepCaller;
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private void initializePython(JepCaller jepCaller) {
        long t1 = debugTime();
        jepCaller.initialize(this);
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Python module \"%s\" (%s) initialized in %.3f ms",
                jepCaller.name(), jepCaller.type(),
                (t2 - t1) * 1e-6));
    }

    private void processPython(JepCaller jepCaller) {
        long t1 = debugTime(), t2, t3, t4;
        try (AtomicPyObject parameters = jepCaller.loadParameters(this);
             AtomicPyObject inputs = jepCaller.readInputPorts(this);
             AtomicPyObject outputs = jepCaller.createOutputs()) {
            t2 = debugTime();
            final Object result = jepCaller.callPython(parameters, inputs, outputs);
            t3 = debugTime();
            jepCaller.writeOutputPorts(this, outputs);
            jepCaller.writeOptionalOutputPort(this, DEFAULT_OUTPUT_PORT, result, true);
            // - note: direct assignment "outputs.output = xxx" overrides simple returning result
            t4 = debugTime();
        }
        setSystemOutputs();
        logDebug(() -> String.format(Locale.US,
                "Python module \"%s\" (%s) executed in %.5f ms:"
                        + " %.6f ms loading inputs + %.6f ms calling + %.6f ms returning outputs",
                jepCaller.name(), jepCaller.type(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    private void closePython(boolean doClose) {
        JepCaller jepCaller = this.jepCaller;
        if (jepCaller != null) {
            this.jepCaller = null;
            if (doClose) {
                jepCaller.close();
            }
        }
    }

    private void setSystemOutputs() {
        if (isOutputNecessary(UsingPython.SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME)) {
            getScalar(UsingPython.SUPPLIED_PYTHON_ROOTS_OUTPUT_NAME).setTo(
                    String.join(String.format("%n"), JepPlatforms.pythonRootFolders()));
        }
        if (isOutputNecessary(UsingPython.SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME)) {
            getScalar(UsingPython.SUPPLIED_PYTHON_SPECIFICATIONS_OUTPUT_NAME).setTo(
                    String.join(String.format("%n"),
                            JepPlatforms.pythonPlatforms().installedSpecificationFolders()));
        }
    }
}
