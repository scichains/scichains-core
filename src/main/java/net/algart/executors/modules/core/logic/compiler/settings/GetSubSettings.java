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

package net.algart.executors.modules.core.logic.compiler.settings;

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombinerSpecification;
import net.algart.executors.modules.core.logic.compiler.settings.model.SubSettingsInheritanceMode;
import net.algart.json.Jsons;

public final class GetSubSettings extends Executor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsCombinerSpecification.SETTINGS;
    public static final String SUB_SETTINGS = "sub-settings";

    private String subSettingsName = "subsettings";
    private SubSettingsInheritanceMode subSettingsInheritanceMode = SubSettingsInheritanceMode.NONE;
    private boolean useSubSettingsPrefix = true;

    public GetSubSettings() {
        setDefaultInputScalar(SETTINGS);
        setDefaultOutputScalar(SUB_SETTINGS);
    }

    public String getSubSettingsName() {
        return subSettingsName;
    }

    public GetSubSettings setSubSettingsName(String subSettingsName) {
        this.subSettingsName = nonEmpty(subSettingsName);
        return this;
    }

    public SubSettingsInheritanceMode getSubSettingsInheritanceMode() {
        return subSettingsInheritanceMode;
    }

    public GetSubSettings setSubSettingsInheritanceMode(SubSettingsInheritanceMode subSettingsInheritanceMode) {
        this.subSettingsInheritanceMode = nonNull(subSettingsInheritanceMode);
        return this;
    }

    public boolean isUseSubSettingsPrefix() {
        return useSubSettingsPrefix;
    }

    public GetSubSettings setUseSubSettingsPrefix(boolean useSubSettingsPrefix) {
        this.useSubSettingsPrefix = useSubSettingsPrefix;
        return this;
    }

    @Override
    public void process() {
        final JsonObject settings = AddSubSettings.scalarToJson(getInputScalar(SETTINGS, true));
        final String subSettingsKey = useSubSettingsPrefix ?
                SettingsCombinerSpecification.settingsKey(subSettingsName) :
                subSettingsName;
        JsonObject subSettings = settings.getJsonObject(subSettingsKey);
        if (subSettings == null) {
            subSettings = Jsons.newEmptyJson();
        }
        final JsonObject result = subSettingsInheritanceMode.inherit(settings, subSettings);
        getScalar(SUB_SETTINGS).setTo(Jsons.toPrettyString(result));
    }

}
