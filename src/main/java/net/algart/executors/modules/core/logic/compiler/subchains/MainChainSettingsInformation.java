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

package net.algart.executors.modules.core.logic.compiler.subchains;

import net.algart.executors.api.system.Chain;
import net.algart.executors.api.system.ChainBlock;
import net.algart.executors.api.system.ChainSpecification;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;

import java.util.Objects;

public final class MainChainSettingsInformation {
    private final String chainCombineSettingsBlockId;
    private final SettingsCombiner chainSettingsCombiner;

    MainChainSettingsInformation(Chain chain, SettingsCombiner chainSettingsCombiner) {
        Objects.requireNonNull(chain, "Null chain");
        Objects.requireNonNull(chainSettingsCombiner, "Null chainSettingsCombiner");
        this.chainCombineSettingsBlockId = findCombineSettings(chain, chainSettingsCombiner).getId();
        assert this.chainCombineSettingsBlockId != null;
        this.chainSettingsCombiner = chainSettingsCombiner;
    }

    public String chainCombineSettingsBlockId() {
        return chainCombineSettingsBlockId;
    }

    public SettingsCombiner chainSettingsCombiner() {
        return chainSettingsCombiner;
    }

    private static ChainBlock findCombineSettings(Chain chain, SettingsCombiner combiner) {
        final String id = combiner.id();
        assert id != null;
        for (ChainBlock block : chain.getAllBlocks().values()) {
            final ChainSpecification.ChainBlockConf blockConfJson = block.getBlockConfJson();
            assert blockConfJson != null;
            final String blockExecutorId = blockConfJson.getExecutorId();
            assert blockExecutorId != null;
            if (id.equals(blockExecutorId)) {
                return block;
            }
        }
        throw new IllegalStateException("Sub-chain " + chain.category() + "." + chain.name()
                + " does not contain dynamic executor '" + id
                + "', created by its UseChainSettings function");
    }
}
