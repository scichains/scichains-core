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

package net.algart.executors.api.chains;

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.settings.CombineChainSettings;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.SettingsExecutor;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.json.Jsons;

import java.util.Objects;

public abstract class ChainExecutor extends Executor {
    private volatile Chain chain = null;

    public Chain chain() {
        Chain chain = this.chain;
        if (chain == null) {
            chain = registeredChain(getSessionId(), getExecutorId());
            this.chain = chain;
            // - the order is important for multithreading
        }
        return chain;
    }
    public ExecutorFactory executorFactory() {
        //noinspection resource
        return chain().executorFactory();
    }

    public SettingsBuilder settingsBuilder() {
        //noinspection resource
        return chain().settingsBuilder();
    }

    public CombineChainSettings newCombine() {
        //noinspection resource
        return chain().newCombine();
    }

    public ChainExecutor putSettingsJson(JsonObject settings) {
        return putSettings(settings == null ? null : Jsons.toPrettyString(settings));
    }

    public ChainExecutor putSettings(String settings) {
        putStringScalar(SettingsExecutor.SETTINGS, settings);
        return this;
    }

    @Override
    public void close() {
        Chain chain = this.chain;
        if (chain != null) {
            this.chain = null;
            // - for a case of recursive calls
            chain.freeResources();
        }
        super.close();
    }

    @Override
    public String toString() {
        return "Executor of " + (chain != null ? chain : "some non-initialized chain");
    }

    public static Chain registeredChain(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Cannot find sub-chain worker: session ID is not set");
        Objects.requireNonNull(executorId, "Cannot find sub-chain worker: executor ID is not set");
        Chain chain = UseSubChain.subChainLoader().registeredWorker(sessionId, executorId);
        chain = chain.cleanCopy();
        // - every instance of this executor has its own space for data, like activates for usual procedures
        // (necessary for recursion)
        return chain;
    }

}
