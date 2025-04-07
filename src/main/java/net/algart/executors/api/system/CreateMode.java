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

package net.algart.executors.api.system;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.UseSubChain;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.parameters.Parameters;

import java.util.Map;

/**
 * Initialization mode for executor, created by {@link ExecutorFactory#newExecutor(String, CreateMode)}
 * and similar methods.
 */
public enum CreateMode {
    /**
     * The executor is created by its constructor or an equivalent instantiation method.
     * No additional initialization is performed.
     * Note that some executors can be unable to operate normally in this mode: for example, a
     * {@link UseSubChain#newExecutor}
     * requires information about its executor ID.
     */
    NO_ACTIONS {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
        }
    },
    /**
     * Minimal initialization, sufficient for most needs.
     * The new executor is configured by {@link ExecutionBlock#setSessionId(String)}
     * and {@link ExecutionBlock#setSpecification(ExecutorSpecification)} methods.
     * So, the {@link ExecutionBlock#getExecutorId() executor ID},
     * {@link ExecutionBlock#getPlatformId() platform ID} and other specification details will be available
     * to the executor implementation.
     */
    MINIMAL {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            result.setSessionId(sessionId);
            result.setSpecification(specification);
        }
    },
    /**
     * In addition to {@link #MINIMAL} mode, this initialization procedure creates all input and
     * output ports, listed in the specification, and fills all {@link ExecutionBlock#parameters() parameters}
     * with the default values from the specification.
     * This is convenient for usage.
     * <p>Note that this mode can theoretically lead to another behavior from {@link #MINIMAL}
     * in some clients if they depend on the ports' existence or on default parameter values.
     */
    NO_REQUEST {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            MINIMAL.customizeExecutor(result, sessionId, specification);
            final Parameters parameters = result.parameters();
            for (Map.Entry<String, ControlSpecification> e : specification.controls.entrySet()) {
                final String name = e.getKey();
                final ControlSpecification controlSpecification = e.getValue();
                final Object defaultValue = controlSpecification.getDefaultValue();
                if (defaultValue != null) {
                    // - we MUST NOT add parameters with non-existing default values:
                    // null is not an allowed value for most parameter types
                    parameters.put(name, defaultValue);
                }
            }
            for (Map.Entry<String, PortSpecification> e : specification.getInputPorts().entrySet()) {
                result.addPort(Port.newInput(e.getKey(), e.getValue().getValueType()));
            }
            for (Map.Entry<String, PortSpecification> e : specification.getOutputPorts().entrySet()) {
                result.addPort(Port.newOutput(e.getKey(), e.getValue().getValueType()));
            }
        }
    },

    /**
     * The same as {@link #NO_REQUEST}, but in addition this mode calls
     * {@link ExecutionBlock#requestDefaultOutput() executor.requestDefaultOutput()}.
     * Usually this is a good balanced choice, when the executor has only one resulting port.
     * If it has several output ports, we recommend using the {@link #REQUEST_ALL} option or
     * directly selecting the required ports with help of {@link ExecutionBlock#requestOutput(String...)} method.
     */
    REQUEST_DEFAULT {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            NO_REQUEST.customizeExecutor(result, sessionId, specification);
            result.requestDefaultOutput();
        }
    },
    /**
     * The same as {@link #REQUEST_DEFAULT}, but in addition this mode calls
     * {@link ExecutionBlock#setAllOutputsNecessary(boolean) executor.setAllOutputsNecessary(true)}.
     * Usually this is the desired behavior, excepting some complex cases when the executor has
     * several resulting ports, and we need to calculate only part from them for saving executing time.
     */
    REQUEST_ALL {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            NO_REQUEST.customizeExecutor(result, sessionId, specification);
            result.setAllOutputsNecessary(true);
        }
    };


    abstract void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification);
}
