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
import net.algart.executors.api.data.Port;
import net.algart.executors.api.parameters.Parameters;

public enum InstantiationMode {
    CONSTRUCTOR_ONLY {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
        }
    },
    MINIMAL {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            result.setSessionId(sessionId);
            result.setExecutorSpecification(specification);
        }
    },
    NORMAL {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            MINIMAL.customizeExecutor(result, sessionId, specification);
            final Parameters parameters = result.parameters();
            for (var e : specification.getControls().entrySet()) {
                final String name = e.getKey();
                final ExecutorSpecification.ControlConf controlConf = e.getValue();
                parameters.put(name, controlConf.getDefaultValue());
            }
            for (var e : specification.getInPorts().entrySet()) {
                result.addPort(Port.newInput(e.getKey(), e.getValue().getValueType()));
            }
            for (var e : specification.getOutPorts().entrySet()) {
                result.addPort(Port.newOutput(e.getKey(), e.getValue().getValueType()));
            }
        }
    },
    REQUEST_OUTPUT {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            NORMAL.customizeExecutor(result, sessionId, specification);
            result.requestDefaultOutput();
        }
    },
    REQUEST_ALL_OUTPUTS {
        @Override
        void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification) {
            NORMAL.customizeExecutor(result, sessionId, specification);
            result.setAllOutputsNecessary(true);
        }
    };


    abstract void customizeExecutor(ExecutionBlock result, String sessionId, ExecutorSpecification specification);
}
