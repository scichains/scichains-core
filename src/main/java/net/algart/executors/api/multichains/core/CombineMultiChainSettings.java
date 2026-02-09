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

package net.algart.executors.api.multichains.core;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.multichains.MultiChain;
import net.algart.executors.api.multichains.MultiChainSettingsBuilder;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.core.CombineChainSettings;
import net.algart.json.Jsons;

import java.util.Objects;

// Must be public with public constructor without arguments to be created by external systems.
public class CombineMultiChainSettings extends CombineChainSettings {
    public CombineMultiChainSettings() {
    }

    public MultiChain multiChain() {
        final SettingsBuilder settingsBuilder = settingsBuilder();
        if (!(settingsBuilder instanceof MultiChainSettingsBuilder multiChainSettingsBuilder)) {
            throw new IllegalStateException("Invalid usage of " + this
                    + ": settings object is not MultiChainSettingsBuilder");
        }
        return multiChainSettingsBuilder.multiChain();
    }

    public void selectChainVariant(String variant) {
        Objects.requireNonNull(variant, "Null variant");
        //noinspection resource
        setStringParameter(multiChain().selectedChainParameter(), variant);
    }


    /**
     * This implementation adds to the <code>settingsJson</code>
     * {@value MultiChain#SELECTED_CHAIN_NAME} field in the case,
     * when the <code>settingsJson</code> contain only {@value MultiChain#SELECTED_CHAIN_ID}.
     * This is done only for better readability of the resulting JSON.
     */
    @Override
    protected JsonObject correctSettings(JsonObject settingsJson, SettingsBuilder settingsBuilder) {
        if (!(settingsBuilder instanceof MultiChainSettingsBuilder multiChainSettingsBuilder)) {
            throw new IllegalArgumentException("Invalid usage of " + this
                    + ": settings object is not MultiChainSettingsBuilder");
        }
        @SuppressWarnings("resource") final MultiChain multiChain = multiChainSettingsBuilder.multiChain();
        final String chainId = settingsJson.getString(MultiChain.SELECTED_CHAIN_ID, null);
        if (chainId == null) {
            // - selection by name is used, no sense to add anything
            return settingsJson;
        }
//        System.out.println("!!!" + chainId + "; " + multiChain);
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (!settingsJson.containsKey(MultiChain.SELECTED_CHAIN_NAME)) {
            for (ChainSpecification specification : multiChain.chainSpecifications()) {
                if (chainId.equals(specification.chainId())) {
                    builder.add(MultiChain.SELECTED_CHAIN_NAME, specification.chainName());
                    break;
                }
            }
        }
        Jsons.addAllJson(builder, settingsJson);
        return builder.build();
    }
}
