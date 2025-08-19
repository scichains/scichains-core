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

package net.algart.executors.api.multichains.core;

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.core.ChainExecutor;
import net.algart.executors.api.chains.core.UseSubChain;
import net.algart.executors.api.multichains.MultiChain;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.settings.core.UseSettings;
import net.algart.executors.modules.core.common.FunctionTiming;
import net.algart.executors.modules.core.common.TimingStatistics;
import net.algart.json.Jsons;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Locale;

public class InterpretMultiChain extends MultiChainExecutor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;

    private final FunctionTiming timing = FunctionTiming.newDisabledInstance();

    public InterpretMultiChain() {
        addInputScalar(SETTINGS);
        addOutputScalar(SETTINGS);
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        long t1 = System.nanoTime(), t2, t3, t4, t5, t6;
        final boolean doAction = parameters().getBoolean(UseMultiChain.DO_ACTION_NAME, true);
        if (!doAction) {
            ChainExecutor.copyInputToOutput(this);
            return;
        }
        final JsonObject inputSettings = Jsons.toJson(
                getInputScalar(SETTINGS, true).getValue(), true);
        final boolean absolutePaths = parameters().getBoolean(
                UseSettings.ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                SettingsBuilder.ABSOLUTE_PATHS_DEFAULT_VALUE);
        final boolean extractSubSettings = parameters().getBoolean(
                UseMultiChain.EXTRACT_SUB_SETTINGS_PARAMETER_NAME,
                UseMultiChain.EXTRACT_SUB_SETTINGS_PARAMETER_DEFAULT);
        final boolean ignoreParameters = parameters().getBoolean(
                UseMultiChain.IGNORE_PARAMETERS_PARAMETER_NAME,
                UseMultiChain.IGNORE_PARAMETERS_PARAMETER_DEFAULT);
        @SuppressWarnings("resource") final MultiChain multiChain = multiChain();
        multiChain.setExtractSubSettings(extractSubSettings);
        final SettingsBuilder multiChainSettingsBuilder = multiChain.settingsBuilder();
        multiChainSettingsBuilder.setAbsolutePaths(absolutePaths);
        final String defaultChainVariant = ignoreParameters ?
                multiChain.defaultChainVariant() :
                parameters().getString(multiChain.selectedChainParameter());
        final String selectedChainVariant = multiChain.getSelectedChainVariant(inputSettings, defaultChainVariant);
        final JsonObject executorSettings = ignoreParameters ?
                Jsons.newEmptyJson() :
                multiChainSettingsBuilder.build(this);
        Chain selectedChain = multiChain.findSelectedChain(selectedChainVariant);
        status().setExecutorSimpleClassName(multiChain.name() + ":"
                + (selectedChain.name() == null ? "" : selectedChain.name()));
        final JsonObject multiChainSettings = multiChain.multiChainSettings(inputSettings);
        final JsonObject selectedChainSettings =
                multiChain.selectedChainSettings(executorSettings, inputSettings, selectedChain);
        selectedChain.reinitializeAll();
        selectedChain.setCaller(this);
        final Level timingLogLevel = ofLogLevel(parameters().getString(
                UseMultiChain.TIMING_LOG_LEVEL_NAME, UseSubChain.TIMING_LOG_LEVEL_DEFAULT));
        final int timingNumberOfCalls = LOG.isLoggable(timingLogLevel) ?
                parameters().getInteger(
                        UseMultiChain.TIMING_NUMBER_OF_CALLS_NAME, UseSubChain.TIMING_NUMBER_OF_CALLS_DEFAULT) :
                0;
        final int timingNumberOfPercentiles = parameters().getInteger(
                UseMultiChain.TIMING_NUMBER_OF_PERCENTILES_NAME, UseSubChain.TIMING_NUMBER_OF_PERCENTILES_DEFAULT);
        final TimingStatistics.Settings timingConfiguration = new TimingStatistics.Settings();
        timingConfiguration.setUniformPercentileLevels(timingNumberOfPercentiles);
        selectedChain.setTimingSettings(timingNumberOfCalls, timingConfiguration);
        timing.setSettings(timingNumberOfCalls, timingConfiguration);
        final String selectedChainSettingsString;
        try {
            Parameters parametersCopy = new Parameters(parameters());
            multiChainSettingsBuilder.parseSettingsToParameters(parametersCopy, multiChainSettings);
            selectedChain.setParameters(parametersCopy);
            // - if a chain has direct parameters, sets also them in addition to copying into selectedChainSettings
            t2 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            selectedChain.readInputPortsFromExecutor(this);
            t3 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            selectedChainSettingsString = MultiChain.setSettings(selectedChain, selectedChainSettings);
            // - used if this chain variant has its own settings block for detailed customization
            t4 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            selectedChain.executeNecessary(this);
            t5 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            selectedChain.writeOutputPortsToExecutor(this);
            t6 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
        } finally {
            selectedChain.freeData();
        }
        long t7 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
        timing.updatePassingData(t3 - t2 + t6 - t5);
        timing.updateExecution(t5 - t4);
        timing.updateSummary(t7 - t1);
        if (timingNumberOfCalls > 0 &&
                parameters().getBoolean(UseMultiChain.LOG_TIMING_NAME, UseSubChain.LOG_TIMING_DEFAULT)) {
            timing.analyse();
            final Path file = multiChain.specification().getSpecificationFile();
            LOG.log(timingLogLevel, () -> String.format(Locale.US,
                    "Multi-chain \"%s\", variant \"%s\" executed%s%s in %.3f ms:%n" +
                            "  %.5f ms initializing, %.5f ms loading inputs, %.5f ms set chain settings, " +
                            "%.5f ms process, %.5f ms returning outputs, %.5f ms freeing%n" +
                            "  multi-chain specification file: %s%n%s" +
                            "  All multi-chain \"%s\", %s",
                    multiChain.name(),
                    selectedChain.name(),
                    selectedChain.isMultithreading() ? " (multithreading mode)" : " (single-thread mode)",
                    selectedChain.isExecuteAll() ? " (ALL blocks)" : "",
                    (t7 - t1) * 1e-6,
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6,
                    (t5 - t4) * 1e-6, (t6 - t5) * 1e-6, (t7 - t6) * 1e-6,
                    file == null ? "n/a" : "\"" + file + "\"",
                    selectedChain.timingInfo(),
                    multiChain.name(), timing));
        }
        final Level settingsLogLevel = parameters().getBoolean(
                UseMultiChain.LOG_SETTINGS_PARAMETER_NAME, false) ?
                Level.WARNING :
                Level.DEBUG;
        LOG.log(settingsLogLevel, () -> String.format(Locale.US,
                "Customizing multi-chain \"%s\", variant \"%s\" with help of %s (called from %s):\n%s",
                multiChain.name(),
                selectedChain.name(),
                extractSubSettings ? "extracted sub-settings" : "json-settings",
                quoteContextName(this),
                selectedChainSettings == null ? "    [this sub-chain has no settings]" : selectedChainSettingsString));
        setOutputScalar(SETTINGS, selectedChainSettingsString);
        setOutputScalar(UseSettings.SETTINGS_ID_OUTPUT_NAME, multiChain.settingsId());
    }

    @Override
    public String visibleOutputPortName() {
        return parameters().getString(UseMultiChain.VISIBLE_RESULT_PARAMETER_NAME, defaultOutputPortName());
        // - default value is necessary for saved chains, that do not contain this property
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private static String quoteContextName(Executor e) {
        final String contextName = e.getContextName();
        return contextName == null ? "unnamed context #" + e.getContextId() : "\"" + contextName + "\"";
    }
}
