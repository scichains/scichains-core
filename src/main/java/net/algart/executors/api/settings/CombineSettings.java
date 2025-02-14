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
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SScalar;
import net.algart.json.Jsons;

import java.util.Locale;

public class CombineSettings extends SettingsExecutor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;

    public CombineSettings() {
        addInputScalar(SETTINGS);
        setDefaultOutputScalar(SETTINGS);
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        setSystemOutputs();
        // - important to do this before other operations, for an improbable case
        // when there is a user's port with the same name UseSettings.EXECUTOR_JSON_OUTPUT_NAME
        long t1 = debugTime();
        final Settings settings = settings();
        final SScalar inputSettings = getInputScalar(SETTINGS, true);
        settings.setAbsolutePaths(parameters().getBoolean(
                UseSettings.ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                Settings.ABSOLUTE_PATHS_DEFAULT_VALUE));
// We decided not to add this information: the settings are usually created by external dashboard,
// direct using settings is not a typical case
//        settings.setAddSettingsClass(properties().getBoolean(
//                UseSettings.ADD_SETTINGS_CLASS_PARAMETER_NAME, true));
        final boolean extractSubSettings = parameters().getBoolean(
                UseSettings.EXTRACT_SUB_SETTINGS_PARAMETER_NAME, false);
        settings.setExtractSubSettings(extractSubSettings);
        final String allSettings = !inputSettings.isInitialized() ?
                parameters().getString(UseSettings.ALL_SETTINGS_PARAMETER_NAME, "").trim() :
                inputSettings.getValue();
        final JsonObject executorSettings = settings.createSettings(this);
        final JsonObject parentSettings = Jsons.toJson(allSettings, true);
        final JsonObject overriddenSettings = settings.overrideSettings(executorSettings, parentSettings);
        final JsonObject resultSettings = correctSettings(overriddenSettings, settings);
        settings.splitSettings(this, resultSettings);
        final String settingsString = Jsons.toPrettyString(resultSettings);
        getScalar(SETTINGS).setTo(settingsString);
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Combining%s %ssettings \"%s\": %.3f ms%s",
                !inputSettings.isInitialized() ? "" :
                        " and overriding" + (extractSubSettings ? " extracted " : ""),
                this instanceof CombineChainSettings ? "sub-chain " : "",
                settings.name(),
                (t2 - t1) * 1e-6,
                LOGGABLE_TRACE ?
                        "\n" + settingsString + (!inputSettings.isInitialized() ? ""
                                : "\nOriginal settings (from parameters):\n" + Jsons.toPrettyString(executorSettings)
                                + "\nInput settings (that override parameters):\n" + inputSettings.getValue())
                        : ""));
    }

    @Override
    public String toString() {
        return "Combine " + (settings != null ? settings : "some non-initialized settings");
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    protected JsonObject correctSettings(JsonObject settings, Settings combiner) {
        return settings;
    }
}
