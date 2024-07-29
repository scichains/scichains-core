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

package net.algart.executors.modules.core.logic.compiler.subchains.interpreters;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.model.ChainJson;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.CombineChainSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.subchains.model.MultiChain;
import net.algart.json.Jsons;

// Must be public with public constructor without arguments to be created by external systems.
public class CombineMultiChainSettings extends CombineChainSettings {
    public CombineMultiChainSettings() {
    }

    @Override
    protected JsonObject correctSettings(JsonObject settings, SettingsCombiner combiner) {
        final Object customSettingsInformation = combiner.getCustomSettingsInformation();
        if (!(customSettingsInformation instanceof MultiChain)) {
            throw new AssertionError("Invalid usage of " + this
                    + ": combiner does not contain parent MultiChain");
        }
        final MultiChain multiChain = (MultiChain) customSettingsInformation;
        final String chainId = settings.getString(MultiChain.SELECTED_CHAIN_ID_PARAMETER_NAME, null);
        if (chainId == null) {
            throw new AssertionError("Invalid usage of " + this + ": no "
                    + MultiChain.SELECTED_CHAIN_ID_PARAMETER_NAME + " parameter");
        }
//        System.out.println("!!!" + chainId + "; " + multiChain);
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (!settings.containsKey(MultiChain.SELECTED_CHAIN_NAME_JSON_KEY)) {
            for (ChainJson model : multiChain.chainModels()) {
                if (chainId.equals(model.chainId())) {
                    builder.add(MultiChain.SELECTED_CHAIN_NAME_JSON_KEY, model.chainName());
                    break;
                }
            }
        }
        Jsons.addAllJson(builder, settings);
        return builder.build();
    }
}
