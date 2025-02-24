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

package net.algart.executors.api.multichains;

import net.algart.executors.api.settings.*;
import net.algart.executors.api.system.ExecutorSpecification;

// Note: it is not an executor, it is an internal class for usage inside UseMultiChain and MultiChain
public final class UseMultiChainSettings extends UseSettings {
    private volatile MultiChain multiChain = null;

    public MultiChain getMultiChain() {
        return multiChain;
    }

    public void setMultiChain(MultiChain multiChain) {
        this.multiChain = multiChain;
    }

    @Override
    public ExecutorSpecification buildCombineSpecification(SettingsBuilder settingsBuilder) {
        if (multiChain == null) {
            throw new IllegalStateException("MultiChain is not set: UseMultiChainSettings cannot be used");
        }
        final ExecutorSpecification result = super.buildCombineSpecification(settingsBuilder);
        result.createOptionsIfAbsent().createControllingIfAbsent()
                .setGrouping(true)
                .setGroupSelector(multiChain.selectedChainParameter());
        return result;
    }

    @Override
    protected boolean isNeedToAddOwner() {
        return true;
    }

    @Override
    protected SettingsBuilder newSettings(SettingsSpecification settingsSpecification) {
        if (multiChain == null) {
            throw new IllegalStateException("multiChain is not set: newSettings cannot be used");
        }
        return new MultiChainSettingsBuilder(settingsSpecification, multiChain);
    }

    @Override
    protected CombineSettings newCombineSettings() {
        return new CombineMultiChainSettings();
    }

    @Override
    protected SplitSettings newSplitSettings() {
        throw new UnsupportedOperationException("This method should not be called: " + getClass()
                + " must be used only for creating multi-chain settings, which has no split_id");
    }

    @Override
    protected GetNamesOfSettings newGetNamesOfSettings() {
        throw new UnsupportedOperationException("This method should not be called: " + getClass()
                + " must be used only for creating multi-chain settings, which has no get_names_id");
    }
}
