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

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.ControlSpecification;
import net.algart.json.Jsons;

public final class AddSubSettings extends Executor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;
    public static final String SUB_SETTINGS = "sub-settings";

    private String subSettingsName = "subsettings";
    private ReplaceExistingSettingsMode replaceExistingSettingsMode = ReplaceExistingSettingsMode.OVERRIDE;
    private boolean useSubSettingsPrefix = true;

    public AddSubSettings() {
        setDefaultInputScalar(SETTINGS);
        addInputScalar(SUB_SETTINGS);
        setDefaultOutputScalar(SETTINGS);
    }

    public String getSubSettingsName() {
        return subSettingsName;
    }

    public AddSubSettings setSubSettingsName(String subSettingsName) {
        this.subSettingsName = nonEmpty(subSettingsName);
        return this;
    }

    public ReplaceExistingSettingsMode getReplaceExistingSettingsMode() {
        return replaceExistingSettingsMode;
    }

    public AddSubSettings setReplaceExistingSettingsMode(ReplaceExistingSettingsMode replaceExistingSettingsMode) {
        this.replaceExistingSettingsMode = nonNull(replaceExistingSettingsMode);
        return this;
    }

    public boolean isUseSubSettingsPrefix() {
        return useSubSettingsPrefix;
    }

    public AddSubSettings setUseSubSettingsPrefix(boolean useSubSettingsPrefix) {
        this.useSubSettingsPrefix = useSubSettingsPrefix;
        return this;
    }

    @Override
    public void process() {
        final JsonObject settings = scalarToJson(getInputScalar(SETTINGS, true));
        final JsonObject addedSubSettings = scalarToJson(getInputScalar(SUB_SETTINGS, true));
        final String subSettingsKey = useSubSettingsPrefix ?
                ControlSpecification.settingsKey(subSettingsName) :
                subSettingsName;
        final JsonObject existingSubSettings = settings.getJsonObject(subSettingsKey);
        final JsonObject newSubSettings = replaceExistingSettingsMode.replace(existingSubSettings, addedSubSettings);
        final JsonObject result = Jsons.createObjectBuilder(settings).add(subSettingsKey, newSubSettings).build();
        getScalar(SETTINGS).setTo(Jsons.toPrettyString(result));
    }

    static JsonObject scalarToJson(SScalar scalar) {
        final String s = scalar.getValueOrDefault("").trim();
        return s.isEmpty() ? Jsons.newEmptyJson() : Jsons.toJson(s);
    }
}
