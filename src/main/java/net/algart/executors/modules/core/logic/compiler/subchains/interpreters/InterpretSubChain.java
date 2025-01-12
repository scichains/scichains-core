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

package net.algart.executors.modules.core.logic.compiler.subchains.interpreters;

import jakarta.json.JsonObject;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.ChainBlock;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.executors.modules.core.common.FunctionTiming;
import net.algart.executors.modules.core.common.TimingStatistics;
import net.algart.executors.modules.core.logic.compiler.settings.UseSettings;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.CombineSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsSpecification;
import net.algart.executors.modules.core.logic.compiler.subchains.MainChainSettingsInformation;
import net.algart.executors.modules.core.logic.compiler.subchains.UseSubChain;
import net.algart.json.Jsons;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public final class InterpretSubChain extends Executor implements ReadOnlyExecutionInput {
    public static final String SETTINGS = SettingsSpecification.SETTINGS;

    private volatile Chain chain = null;
    private final FunctionTiming timing = FunctionTiming.newDisabledInstance();

    public InterpretSubChain() {
        disableOnChangeParametersAutomatic();
    }

    @Override
    public void process() {
        // UseSubChain.useSystemSubChainsPath(getSessionId(), true);
        // - it was incorrect solution
        long t1 = System.nanoTime(), t2, t3, t4, t5, t6, t7, t8;
        final boolean doAction = parameters().getBoolean(UseSubChain.DO_ACTION_NAME, true);
        if (!doAction) {
            skipAction(this);
            return;
        }
        final Chain chain = chain();
        t2 = System.nanoTime();
        status().setExecutorSimpleClassName(chain.name() == null ? "sub-chain" : chain.name());
        final JsonObject inputSettings = !hasInputPort(SETTINGS) ? Jsons.newEmptyJson() :
                Jsons.toJson(getInputScalar(SETTINGS, true).getValue(), true);
        chain.reinitializeAll();
        chain.setCaller(this);
        final Level timingLogLevel = valueOfLogLevel(parameters().getString(
                UseSubChain.TIMING_LOG_LEVEL_NAME, UseSubChain.TIMING_LOG_LEVEL_DEFAULT));
        final int timingNumberOfCalls = LOG.isLoggable(timingLogLevel) ?
                parameters().getInteger(
                        UseSubChain.TIMING_NUMBER_OF_CALLS_NAME, UseSubChain.TIMING_NUMBER_OF_CALLS_DEFAULT) :
                0;
        final int timingNumberOfPercentiles = parameters().getInteger(
                UseSubChain.TIMING_NUMBER_OF_PERCENTILES_NAME, UseSubChain.TIMING_NUMBER_OF_PERCENTILES_DEFAULT);
        final TimingStatistics.Settings settings = new TimingStatistics.Settings();
        settings.setUniformPercentileLevels(timingNumberOfPercentiles);
        chain.setTimingSettings(timingNumberOfCalls, settings);
        timing.setSettings(timingNumberOfCalls, settings);
        try {
            // Instead of overriding updateProperties, we should directly load
            // all information from properties() here: we have no containers
            // (like class fields) for storing properties between the calls
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
                parameters().getBoolean(UseSubChain.LOG_TIMING_NAME, UseSubChain.LOG_TIMING_DEFAULT)) {
            final String name = chain.name() == null ? "" : " \"" + chain.name() + "\"";
            timing.analyse();
            final Path file = chain.chainSpecificationPath();
            LOG.log(timingLogLevel, () -> String.format(Locale.US,
                    "Sub-chain%s executed%s%s in %.3f ms:%n" +
                            "  %.3f mcs getting chain, " +
                            "%.3f mcs setting parameters, %.3f mcs loading inputs, %.3f mcs set chain settings, " +
                            "%.3f mcs process, " +
                            "%.3f mcs returning outputs, %.3f mcs freeing%n" +
                            "  Sub-chain ID: %s (identity %X)%n" +
                            "  Sub-chain specification file: %s%n%s" +
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
    public void close() {
        Chain chain = this.chain;
        if (chain != null) {
            this.chain = null;
            // - for a case of recursive calls
            chain.freeResources();
        }
        super.close();
    }

    @Override
    public String visibleOutputPortName() {
        String result = parameters().getString(UseSubChain.VISIBLE_RESULT_PARAMETER_NAME, null);
        if (result == null) {
            final Collection<Port> outputPorts = allOutputPorts();
            if (!outputPorts.isEmpty()) {
                result = outputPorts.iterator().next().getName();
                // - this situation is normal when there is only 1 output port,
                // but can occur also for old saved chains, that do not contain this property
            }
        }
        return result;
    }

    public Chain chain() {
        Chain chain = this.chain;
        if (chain == null) {
            chain = registeredChain(getSessionId(), getExecutorId());
            this.chain = chain;
            // - the order is important for multithreading: local chain is assigned first, this.chain is assigned to it
        }
        return chain;
    }

    public static Chain registeredChain(String sessionId, String executorId) {
        Objects.requireNonNull(sessionId, "Cannot find sub-chain worker: session ID is not set");
        Objects.requireNonNull(executorId, "Cannot find sub-chain worker: executor ID is not set");
        Chain chain = UseSubChain.subChainLoader().registeredWorker(sessionId, executorId);
        chain = chain.cleanCopy();
        // - every instance of this executor has its own space for data, like activates for usual procedures
        // (necessary for recursion)
        return chain;
    }

    @Override
    protected boolean skipStandardAutomaticParameters() {
        return true;
    }

    private void setChainSettings(Chain chain, JsonObject parentSettings) {
        final MainChainSettingsInformation settingsInformation = UseSubChain.getMainChainSettingsInformation(chain);
        if (settingsInformation == null) {
            return;
        }
        final SettingsCombiner combiner = settingsInformation.chainSettingsCombiner();
        final ChainBlock settingsBlock = chain.getBlock(settingsInformation.chainCombineSettingsBlockId());
        // Note: the chain is a cleanCopy() of the original chain, so, we need
        // to find CombineSettings block again by its ID
        if (settingsBlock == null)
            throw new AssertionError("Dynamic executor '"
                    + settingsInformation.chainCombineSettingsBlockId() + "' not found in the chain " + chain);
        final ExecutorSpecification settingsSpecification = settingsBlock.getExecutorSpecification();
        if (settingsSpecification != null) {
            // - In the current version, settingsSpecification will usually be null.
            // We build every ChainBlock at the stage of loading sub-chain, BEFORE executing its loading-time
            // functions; at this stage, settings combiners are not registered yet, and we have no correct JSON.
            if (settingsSpecification.getOptions() == null
                    || settingsSpecification.getOptions().getRole() == null
                    || !settingsSpecification.getOptions().getRole().isSettings()) {
                throw new IllegalArgumentException("Incorrect main chain settings block: it doesn't have " +
                        "a correct role \"settings\" (its options are " +
                        settingsSpecification.getOptions() + ")");
                // Note: this role MAY be not main, if we loaded this combiner not only with a correct
                // function UseChainSettings, but also h simple UseSettings
            }
        }
        final var settingsExecutor = settingsBlock.getExecutor();
        if (!(settingsExecutor instanceof CombineSettings)) {
            throw new AssertionError("Dynamic executor '" + settingsExecutor.getExecutorId()
                    + "' must be an instance of CombineSettings, but it is " + settingsExecutor);
        }
        final boolean absolutePaths = parameters().getBoolean(
                UseSettings.ABSOLUTE_PATHS_NAME_PARAMETER_NAME,
                SettingsCombiner.ABSOLUTE_PATHS_DEFAULT_VALUE);
        final boolean extractSubSettings = parameters().getBoolean(
                UseSettings.EXTRACT_SUB_SETTINGS_PARAMETER_NAME,
                UseSettings.EXTRACT_SUB_SETTINGS_PARAMETER_FOR_SUB_CHAIN_DEFAULT);
        final boolean ignoreInputParameters = parameters().getBoolean(
                UseSettings.IGNORE_PARAMETERS_PARAMETER_NAME,
                UseSettings.IGNORE_PARAMETERS_PARAMETER_DEFAULT);
        final boolean logSettings = parameters().getBoolean(
                UseSettings.LOG_SETTINGS_PARAMETER_NAME,
                false);
        combiner.setAbsolutePaths(absolutePaths);
        combiner.setExtractSubSettings(extractSubSettings);
        final JsonObject executorSettings = ignoreInputParameters ?
                Jsons.newEmptyJson() :
                combiner.createSettings(this);
        final JsonObject overriddenSettings = combiner.overrideSettings(executorSettings, parentSettings);
        final String settingsString = Jsons.toPrettyString(overriddenSettings);
        settingsBlock.setActualInputData(CombineSettings.SETTINGS, SScalar.valueOf(settingsString));
        if (hasOutputPort(CombineSettings.SETTINGS)) {
            // - we check the port to be on the safe side; in correctly created chain, it must exist
            getScalar(CombineSettings.SETTINGS).setTo(settingsString);
        }
        final Level level = logSettings ? Level.WARNING : Level.DEBUG;
        if (!parentSettings.isEmpty()) {
            LOG.log(level, () -> String.format(Locale.US,
                    "Customizing chain \"%s\" with help of %s \"%s\" (called from %s):\n%s%s",
                    chain.name(),
                    extractSubSettings ? "extracted sub-settings" : "json-settings",
                    combiner.name(),
                    quote(getContextName()),
                    settingsString,
                    LOGGABLE_TRACE ?
                            "\nOriginal settings (from parameters):\n"
                                    + Jsons.toPrettyString(executorSettings)
                                    + "\nInput settings (that override parameters):\n"
                                    + Jsons.toPrettyString(parentSettings) :
                            ""));
        } else {
            LOG.log(level, () -> String.format(Locale.US,
                    "Customizing chain \"%s\" directly from parameters (combiner \"%s\", "
                            + "called from %s):\n%s",
                    chain.name(),
                    combiner.name(),
                    quote(getContextName()),
                    settingsString));
        }
    }

    static String quote(String s) {
        return s == null ? "N/A" : "\"" + s + "\"";
    }

    static void skipAction(Executor executor) {
        for (Port input : executor.allInputPorts()) {
            final Port outputPort = executor.getOutputPort(input.getName());
            if (outputPort != null && outputPort.getDataType() == input.getDataType()) {
                outputPort.getData().setTo(input.getData());
                // Not exchange! This class implements ReadOnlyExecutionInput!
            }
        }
    }
}
