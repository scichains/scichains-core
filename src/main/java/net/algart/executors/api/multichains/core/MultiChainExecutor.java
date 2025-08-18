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

package net.algart.executors.api.multichains.core;

import net.algart.executors.api.Executor;
import net.algart.executors.api.multichains.MultiChain;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.system.ExecutorFactory;

import java.util.Objects;

public abstract class MultiChainExecutor extends Executor {
    private volatile MultiChain multiChain = null;

    public MultiChain multiChain() {
        MultiChain multiChain = this.multiChain;
        if (multiChain == null) {
            multiChain = registeredMultiChain(getSessionId(), getExecutorId());
            this.multiChain = multiChain;
            // - the order is important for multithreading: local multiChain is assigned first,
            // this.multiChain is assigned to it
        }
        return multiChain;
    }


    public ExecutorFactory executorFactory() {
        //noinspection resource
        return multiChain().executorFactory();
    }

    public SettingsBuilder settingsBuilder() {
        //noinspection resource
        return multiChain().settingsBuilder();
    }

    public CombineMultiChainSettings newCombine() {
        //noinspection resource
        return multiChain().newCombine();
    }

    public void selectChainVariant(String variant) {
        Objects.requireNonNull(variant, "Null variant");
        //noinspection resource
        setStringParameter(multiChain().selectedChainParameter(), variant);
    }

    @Override
    public void close() {
        MultiChain multiChain = this.multiChain;
        if (multiChain != null) {
            this.multiChain = null;
            // - for a case of recursive calls
            multiChain.freeResources();
        }
        super.close();
    }

    @Override
    public String toString() {
        return "Executor of " + (multiChain != null ? multiChain : "some non-initialized multi-chain");
    }

    public static MultiChain registeredMultiChain(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Cannot find multi-chain worker: session ID is not set");
        Objects.requireNonNull(executorId, "Cannot find multi-chain worker: executor ID is not set");
        @SuppressWarnings("resource")
        MultiChain multiChain = UseMultiChain.multiChainLoader().registeredWorker(sessionId, executorId);
        return multiChain.clone();
    }

}
