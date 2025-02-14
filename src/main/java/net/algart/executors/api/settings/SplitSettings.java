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
import net.algart.json.Jsons;

import java.util.Locale;

public class SplitSettings extends SettingsExecutor implements ReadOnlyExecutionInput {
    public SplitSettings() {
        setDefaultInputScalar(CombineSettings.SETTINGS);
        setDefaultOutputScalar(CombineSettings.SETTINGS);
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        setSystemOutputs();
        // - important to do this before other operations, for an improbable case
        // when there is user's port with the same name UseSettings.EXECUTOR_JSON_OUTPUT_NAME
        long t1 = debugTime();
        final Settings settings = settings();
        final String s = getInputScalar(CombineSettings.SETTINGS, true)
                .getValueOrDefault("").trim();
        JsonObject inputSettings = s.isEmpty() ? Jsons.newEmptyJson() : Jsons.toJson(s);
        settings.splitSettings(this, inputSettings);
        inputSettings = Jsons.overrideEntries(settings.createSettings(this), inputSettings);
        // - provide default values for keys, absent in the source JSON
        getScalar(CombineSettings.SETTINGS).setTo(Jsons.toPrettyString(inputSettings));
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Splitting settings \"%s\": %.3f ms",
                settings.splitName(),
                (t2 - t1) * 1e-6));
    }

    @Override
    public String toString() {
        return "Split " + (settings != null ? settings : "some non-initialized settings");
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }
}
