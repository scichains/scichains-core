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

package net.algart.executors.api.settings;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;

public abstract class SettingsExecutor extends Executor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;
    volatile Settings settings = null;

    public Settings settings() {
        final String sessionId = getSessionId();
        final String executorId = getExecutorId();
        if (sessionId == null) {
            throw new IllegalStateException("Cannot find settings: session ID is not set");
        }
        if (executorId == null) {
            throw new IllegalStateException("Cannot find settings: executor ID is not set");
        }
        Settings settings = this.settings;
        if (settings == null) {
            settings = UseSettings.settingsLoader().registeredWorker(sessionId, executorId);
            this.settings = settings.clone();
            // - the order is important for multithreading: local settings are assigned first,
            // this.settings is assigned to it;
            // cloning is necessary because Settings class can be little customized
        }
        return settings;
    }

    void setSystemOutputs() {
        final Settings settings = settings();
        if (hasOutputPort(UseSettings.SETTINGS_NAME_OUTPUT_NAME)) {
            getScalar(UseSettings.SETTINGS_NAME_OUTPUT_NAME).setTo(settings.name());
        }
    }
}
