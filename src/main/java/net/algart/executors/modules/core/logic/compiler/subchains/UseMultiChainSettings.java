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

package net.algart.executors.modules.core.logic.compiler.subchains;

import net.algart.executors.api.chains.MultiChain;
import net.algart.executors.api.settings.SettingsCombiner;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.CombineSettings;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.GetNamesOfSettings;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.SplitSettings;
import net.algart.executors.modules.core.logic.compiler.subchains.interpreters.CombineMultiChainSettings;

// Note: it is not an executor, it is an internal class for usage inside UseMultiChain and MultiChain
public class UseMultiChainSettings extends UseSettings {
    private MultiChain multiChain = null;

    public MultiChain getMultiChain() {
        return multiChain;
    }

    public void setMultiChain(MultiChain multiChain) {
        this.multiChain = multiChain;
    }

    @Override
    public ExecutorSpecification buildCombineSpecification(SettingsCombiner settingsCombiner) {
        final ExecutorSpecification result = super.buildCombineSpecification(settingsCombiner);
        result.createOptionsIfAbsent().createControllingIfAbsent()
                .setGrouping(true)
                .setGroupSelector(MultiChain.SELECTED_CHAIN_ID_PARAMETER_NAME);
        return result;
    }

    @Override
    protected boolean isNeedToAddOwner() {
        return true;
    }

    @Override
    protected CombineSettings newCombineSettings() {
        return new CombineMultiChainSettings();
    }

    @Override
    protected SplitSettings newSplitSettings() {
        throw new UnsupportedOperationException("This method should not be called: " + getClass()
                + " must be used only for creating multi-chain settings combiner, which has no split_id");
    }

    @Override
    protected GetNamesOfSettings newGetNamesOfSettings() {
        throw new UnsupportedOperationException("This method should not be called: " + getClass()
                + " must be used only for creating multi-chain settings combiner, which has no get_names_id");
    }

    @Override
    protected Object customSettingsInformation() {
        return multiChain;
    }

}
