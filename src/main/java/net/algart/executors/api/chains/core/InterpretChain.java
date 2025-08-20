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

package net.algart.executors.api.chains.core;

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.ChainBlock;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.settings.*;
import net.algart.executors.api.settings.core.UseSettings;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.FunctionTiming;
import net.algart.executors.modules.core.common.TimingStatistics;
import net.algart.json.Jsons;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;

public class InterpretChain extends ChainExecutor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;

    private final FunctionTiming timing = FunctionTiming.newDisabledInstance();

    public InterpretChain() {
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        long t1 = System.nanoTime(), t2, t3, t4, t5, t6, t7, t8;
        final boolean doAction = parameters().getBoolean(UseChain.DO_ACTION_NAME, true);
        if (!doAction) {
            copyInputToOutput(this);
            return;
        }
        final Chain chain = chain();
        t2 = System.nanoTime();
        status().setExecutorSimpleClassName(chain.name() == null ? "chain" : chain.name());
        final JsonObject inputSettings = !hasInputPort(SETTINGS) ? Jsons.newEmptyJson() :
                Jsons.toJson(getInputScalar(SETTINGS, true).getValue(), true);
        chain.reinitializeAll();
        chain.setCaller(this);
        final Level timingLogLevel = ofLogLevel(parameters().getString(
                UseChain.TIMING_LOG_LEVEL_NAME, UseChain.TIMING_LOG_LEVEL_DEFAULT));
        final int timingNumberOfCalls = LOG.isLoggable(timingLogLevel) ?
                parameters().getInteger(
                        UseChain.TIMING_NUMBER_OF_CALLS_NAME, UseChain.TIMING_NUMBER_OF_CALLS_DEFAULT) :
                0;
        final int timingNumberOfPercentiles = parameters().getInteger(
                UseChain.TIMING_NUMBER_OF_PERCENTILES_NAME, UseChain.TIMING_NUMBER_OF_PERCENTILES_DEFAULT);
        final TimingStatistics.Settings timingConfiguration = new TimingStatistics.Settings();
        timingConfiguration.setUniformPercentileLevels(timingNumberOfPercentiles);
        chain.setTimingSettings(timingNumberOfCalls, timingConfiguration);
        timing.setSettings(timingNumberOfCalls, timingConfiguration);
        try {
            // Instead of overriding onChangeParameter, we should directly load
            // all information from parameters() here: we have no containers
            // (like class fields) for storing parameters between the calls
            // (the created chain cannot be a container: it is freed every time)
            chain.setParameters(parameters());
            t3 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            chain.readInputPortsFromExecutor(this);
            t4 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            setChainSettings(chain, inputSettings);
            t5 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            chain.executeNecessary(this);
            t6 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
            chain.writeOutputPortsToExecutor(this);
            t7 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
        } finally {
            chain.freeData();
        }
        t8 = timingNumberOfCalls > 0 ? System.nanoTime() : 0;
        timing.updatePassingData(t4 - t3 + t7 - t6);
        timing.updateExecution(t6 - t5);
        timing.updateSummary(t8 - t1);
        if (timingNumberOfCalls > 0 &&
                parameters().getBoolean(UseChain.LOG_TIMING_NAME, UseChain.LOG_TIMING_DEFAULT)) {
            final String name = chain.name() == null ? "" : " \"" + chain.name() + "\"";
            timing.analyse();
            final Path file = chain.chainSpecificationPath();
            LOG.log(timingLogLevel, () -> String.format(Locale.US,
                    "Chain%s executed%s%s in %.3f ms:%n" +
                            "  %.3f mcs getting chain, " +
                            "%.3f mcs setting parameters, %.3f mcs loading inputs, %.3f mcs set chain settings, " +
                            "%.3f mcs process, " +
                            "%.3f mcs returning outputs, %.3f mcs freeing%n" +
                            "  Chain ID: %s (identity %X)%n" +
                            "  Chain specification file: %s%n%s" +
                            "  All%s, %s",
                    name,
                    chain.isMultithreading() ? " (multithreading mode)" : " (single-thread mode)",
                    chain.isExecuteAll() ? " (ALL blocks)" : "",
                    (t8 - t1) * 1e-6,
                    (t2 - t1) * 1e-3,
                    (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, (t5 - t4) * 1e-3,
                    (t6 - t5) * 1e-3,
                    (t7 - t6) * 1e-3, (t8 - t7) * 1e-3,
                    chain.contextId(), System.identityHashCode(chain),
                    file == null ? "n/a" : "\"" + file + "\"",
                    chain.timingInfo(),
                    name, timing));
        }
    }

    @Override
    public String visibleOutputPortName() {
        String result = parameters().getString(UseChain.VISIBLE_RESULT_PARAMETER_NAME, null);
        if (result == null) {
            final Collection<Port> outputPorts = outputPorts();
            if (!outputPorts.isEmpty()) {
                result = outputPorts.iterator().next().getName();
                // - this situation is normal when there is only 1 output port,
                // but can occur also for old saved chains, that do not contain this property
            }
        }
        return result;
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private void setChainSettings(Chain chain, JsonObject parentSettings) {
        final SettingsBuilder settingsBuilder = chain.getSettingsBuilder();
        if (settingsBuilder == null) {
            return;
        }
        final boolean absolutePaths = parameters().getBoolean(
                UseSettings.ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                SettingsBuilder.ABSOLUTE_PATHS_DEFAULT_VALUE);
        final boolean extractSubSettings = parameters().getBoolean(
                UseSettings.EXTRACT_SUB_SETTINGS_PARAMETER_NAME,
                UseSettings.EXTRACT_SUB_SETTINGS_PARAMETER_FOR_CHAIN_DEFAULT);
        final boolean ignoreInputParameters = parameters().getBoolean(
                UseSettings.IGNORE_PARAMETERS_PARAMETER_NAME,
                UseSettings.IGNORE_PARAMETERS_PARAMETER_DEFAULT);
        final boolean logSettings = parameters().getBoolean(
                UseSettings.LOG_SETTINGS_PARAMETER_NAME,
                false);
        settingsBuilder.setAbsolutePaths(absolutePaths);
        settingsBuilder.setExtractSubSettings(extractSubSettings);
        final JsonObject executorSettings = ignoreInputParameters ?
                Jsons.newEmptyJson() :
                settingsBuilder.build(this);
        final JsonObject overriddenSettings = settingsBuilder.overrideSettings(executorSettings, parentSettings);

        final ChainBlock settingsBlock = chain.getBlock(chain.getMainSettingsBlockId());
        // Note: the chain is a cleanCopy() of the original chain, so, we need
        // to find the block with CombineSettings again by its ID
        if (settingsBlock == null)
            throw new AssertionError("Dynamic executor '"
                    + chain.getMainSettingsBlockId() + "' not found in the chain " + chain);
        final ExecutorSpecification settingsSpecification = settingsBlock.getExecutorSpecification();
        if (settingsSpecification != null) {
            // - In the current version, settingsSpecification will usually be null.
            // We build every ChainBlock at the stage of loading a chain, BEFORE executing its loading-time
            // functions; at this stage, settings are not registered yet, and we have no correct JSON.
            if (!settingsSpecification.isRoleSettings()) {
                throw new IllegalArgumentException("Incorrect main chain settings block: it doesn't have " +
                        "a correct role \"settings\" (its options are " +
                        settingsSpecification.getOptions() + ")");
                // Note: this role MAY be not a main role if we loaded these settings not only with a correct
                // function UseChainSettings, but also with a simple UseSettings
            }
        }
//        final var settingsExecutor = settingsBlock.getExecutor();
//        if (!(settingsExecutor instanceof CombineSettings)) {
//            throw new AssertionError("Dynamic executor '" + settingsExecutor.getExecutorId()
//                    + "' must be an instance of CombineSettings, but it is " + settingsExecutor);
//        }
        final String settingsString = Jsons.toPrettyString(overriddenSettings);
        settingsBlock.setActualInputData(SETTINGS, SScalar.of(settingsString));
        if (hasOutputPort(SETTINGS)) {
            // - we check the port to be on the safe side; in a correctly created chain, it must exist
            getScalar(SETTINGS).setTo(settingsString);
        }
        if (hasOutputPort(UseSettings.SETTINGS_ID_OUTPUT_NAME)) {
            // - we check the port to be on the safe side; in a correctly created chain, it must exist
            getScalar(UseSettings.SETTINGS_ID_OUTPUT_NAME).setTo(settingsBuilder.id());
        }
        final Level level = logSettings ? Level.INFO : Level.DEBUG;
        if (!parentSettings.isEmpty()) {
            LOG.log(level, () -> String.format(Locale.US,
                    "Customizing chain \"%s\" with help of %s \"%s\" (called from %s):\n%s%s",
                    chain.name(),
                    extractSubSettings ? "extracted sub-settings" : "json-settings",
                    settingsBuilder.name(),
                    quoteContextName(this),
                    settingsString,
                    LOGGABLE_TRACE ?
                            "\nOriginal settings (from parameters):\n"
                                    + Jsons.toPrettyString(executorSettings)
                                    + "\nInput settings (that override parameters):\n"
                                    + Jsons.toPrettyString(parentSettings) :
                            ""));
        } else {
            LOG.log(level, () -> String.format(Locale.US,
                    "Customizing chain \"%s\" directly from parameters (settings \"%s\", "
                            + "called from %s):\n%s",
                    chain.name(),
                    settingsBuilder.name(),
                    quoteContextName(this),
                    settingsString));
        }
    }

    private static String quoteContextName(Executor e) {
        final String contextName = e.getContextName();
        return contextName == null ? "unnamed context #" + e.getContextId() : "\"" + contextName + "\"";
    }
}
