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

package net.algart.executors.modules.core.logic.compiler.subchains.model;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import net.algart.executors.api.data.ParameterValueType;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.model.*;
import net.algart.executors.modules.core.logic.compiler.settings.interpreters.CombineSettings;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombiner;
import net.algart.executors.modules.core.logic.compiler.settings.model.SettingsCombinerJson;
import net.algart.executors.modules.core.logic.compiler.subchains.MainChainSettingsInformation;
import net.algart.executors.modules.core.logic.compiler.subchains.UseMultiChainSettings;
import net.algart.executors.modules.core.logic.compiler.subchains.UseSubChain;
import net.algart.executors.modules.core.logic.compiler.subchains.interpreters.InterpretSubChain;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class MultiChain implements Cloneable, AutoCloseable {
    public static final String SELECTED_CHAIN_ID_PARAMETER_NAME = "___selectedChainId";
    public static final String SELECTED_CHAIN_NAME_JSON_KEY = "___selectedChainName";
    // - unlike chain ID, it is not a parameter of combiner, but only additional information inside result JSON
    public static final String SELECTED_CHAIN_ID_PARAMETER_CAPTION = "Selected chain";

    private static final AtomicLong CURRENT_CONTEXT_ID = new AtomicLong(109099000000000L);
    // - Some magic value helps to reduce the chance of accidental coincidence with other contextIDs,
    // probable used in the system in other ways (109, 99 are ASCII-codes of letters 'mc').

    private volatile long contextId;
    // - Unique ID for every multichain. Unlike sub-chains, it is almost not used: multichain is not an environment
    // for executing anything; but it is used as a context ID for multichain settings combiner.
    private final MultiChainJson model;
    private final List<ChainJson> chainModels;
    private final List<ChainJson> blockedChainModels;
    private final Set<String> blockedChainModelNames;
    private final List<ExecutorJson> loadedChainExecutorSpecifications;
    private final String defaultChainVariantId;
    private final SettingsCombiner multiChainOnlyCommonSettingsCombiner;
    // - note: this combiner is not registered, it is used for building multi-chain model only in UseMultiChain
    private final SettingsCombiner multiChainSettingsCombiner;

    // Note: unlike Chain, currentDirectory is not actual here: loading without files is senseless here.
    private volatile Map<String, Chain> chainMap = null;

    private boolean extractSubSettings = false;

    private MultiChain(MultiChainJson model, UseSubChain chainFactory, UseMultiChainSettings settingsFactory)
            throws IOException {
        renewContextId();
        this.model = Objects.requireNonNull(model, "Null json model");
        Objects.requireNonNull(chainFactory, "Null chainFactory");
        Objects.requireNonNull(settingsFactory, "Null settingsFactory");
        this.model.checkCompleteness();
        this.chainModels = model.readChainVariants();
        this.blockedChainModels = new ArrayList<>();
        this.blockedChainModelNames = new LinkedHashSet<>();
        this.loadedChainExecutorSpecifications = new ArrayList<>();
        assert !this.chainModels.isEmpty();
        final Map<String, Chain> partialChainMap = new HashMap<>();
        String firstChainId = null;
        for (ChainJson chainModel : this.chainModels) {
            if (firstChainId == null) {
                firstChainId = chainModel.chainId();
            }
            final Optional<Chain> optionalChain;
            try {
                optionalChain = chainFactory.useIfNonRecursive(chainModel);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ChainRunningException("Cannot initialize sub-chain "
                        + chainModel.getChainJsonFile() + ", variant of multichain "
                        + model.getMultiChainJsonFile(), e);
            }
            if (optionalChain.isPresent()) {
                final ExecutorJson implementationModel = chainFactory.chainExecutorSpecification();
                assert implementationModel != null : "chainExecutorModel cannot be null if use() returns some result";
                this.loadedChainExecutorSpecifications.add(implementationModel);
                partialChainMap.put(optionalChain.get().id(), optionalChain.get());
            } else {
                blockedChainModels.add(chainModel);
                blockedChainModelNames.add(chainModel.chainName());
            }
            // - Note: if loading chain was blocked due to recursion, it means that it is in the process
            // of registering and not available yet even via ExecutionBlock.getExecutorModelDescription.
            // We will not be able to check it at loading stage by checkImplementationCompatibility()
            // and will not be able to use help from partialChainMap,
            // but it is not too serious problem: such a recursion is a very rare case.
        }
        final String defaultChainVariantId = model.getDefaultChainVariantId();
        this.defaultChainVariantId = defaultChainVariantId != null ? defaultChainVariantId : firstChainId;
        settingsFactory.setOwnerId(model.getId());
        settingsFactory.setContextId(contextId);
        settingsFactory.setContextName(model.getName());
        // - this information, set by previous operators, will be used only in the following operators,
        // to make a reference to this MultiChain and to set correct owner information
        // inside newly created settings combiner
        this.multiChainOnlyCommonSettingsCombiner = SettingsCombiner.valueOf(
                buildMultiChainSettingsModel(false, partialChainMap));
        // - this (internally used) combiner does not contain advanced multi-line controls
        // for settings of the chain variants; it is used in UseMultiChain.buildMultiChainModel()
        settingsFactory.setMultiChain(this);
        // - this reference will be necessary in CombineMultiChainSettings.correctSettings
        this.multiChainSettingsCombiner = settingsFactory.use(
                buildMultiChainSettingsModel(true, partialChainMap));
    }

    public static MultiChain valueOf(
            MultiChainJson model,
            UseSubChain chainFactory,
            UseMultiChainSettings settingsFactory)
            throws IOException {
        return new MultiChain(model, chainFactory, settingsFactory);
    }

    public boolean isExtractSubSettings() {
        return extractSubSettings;
    }

    public MultiChain setExtractSubSettings(boolean extractSubSettings) {
        this.extractSubSettings = extractSubSettings;
        return this;
    }

    public MultiChainJson model() {
        return model;
    }

    public List<ChainJson> chainModels() {
        return Collections.unmodifiableList(chainModels);
    }

    public List<ChainJson> blockedChainModels() {
        return Collections.unmodifiableList(blockedChainModels);
    }

    public Set<String> blockedChainModelNames() {
        return Collections.unmodifiableSet(blockedChainModelNames);
    }

    public String defaultChainVariantId() {
        return defaultChainVariantId;
    }

    public SettingsCombiner multiChainOnlyCommonSettingsCombiner() {
        return multiChainOnlyCommonSettingsCombiner;
    }

    public SettingsCombiner multiChainSettingsCombiner() {
        return multiChainSettingsCombiner;
    }

    public long contextId() {
        // Note: it will be another for result of clone!
        return contextId;
    }

    public Path multiChainJsonFile() {
        return model.getMultiChainJsonFile();
    }

    public String id() {
        return model.getId();
    }

    public String category() {
        return model.getCategory();
    }

    public String name() {
        return model.getName();
    }

    public String description() {
        return model.getDescription();
    }

    public void checkImplementationCompatibility() {
        for (ExecutorJson implementationModel : loadedChainExecutorSpecifications) {
            model.checkImplementationCompatibility(implementationModel);
        }
    }

    // Note: it is not too good idea to create it only inside the constructor.
    // Every chain can require a lot of resources, and different clones of multichain (see clone())
    // should have different chains.
    public Map<String, Chain> chainMap() {
        Map<String, Chain> chainMap = this.chainMap;
        if (chainMap == null) {
            this.chainMap = chainMap = createChainMap();
            // - the order is important for multithreading
        }
        return Collections.unmodifiableMap(chainMap);
    }

    public String findSelectedChainId(JsonObject parentSettings, String defaultChainId) {
        Objects.requireNonNull(parentSettings, "Null parentSettings");
        Objects.requireNonNull(defaultChainId, "Null defaultChainId");
        String result = defaultChainId;
        if (parentSettings.isEmpty()) {
            return result;
        }
        if (extractSubSettings) {
            final JsonObject multiChainSubSettings = SettingsCombiner.getSubSettingsByName(parentSettings, name());
            if (multiChainSubSettings != null) {
                result = multiChainSubSettings.getString(SELECTED_CHAIN_ID_PARAMETER_NAME, result);
            }
        }
        return parentSettings.getString(SELECTED_CHAIN_ID_PARAMETER_NAME, result);
        // - overriding by the same parameter in the parent JSON, if exists
    }

    public JsonObject findSelectedChainSettings(
            JsonObject executorSettings,
            JsonObject parentSettings,
            Chain selectedChain) {
        Objects.requireNonNull(executorSettings, "Null executorSettings");
        Objects.requireNonNull(parentSettings, "Null parentSettings");
        Objects.requireNonNull(selectedChain, "Null selectedChain");
        final MainChainSettingsInformation settingsInformation =
                UseSubChain.getMainChainSettingsInformation(selectedChain);
        if (settingsInformation == null) {
            // - should not occur while normal usage (selecting chain from chainMap() result)
            throw new IllegalArgumentException("Selected chain has no built-in settings: " + selectedChain);
        }

        // - no needs to customize
        JsonObject result = executorSettings;
        if (parentSettings.isEmpty()) {
            return result;
            // - micro-optimization
        }

        if (!extractSubSettings) {
            return Jsons.overrideEntries(result, parentSettings);
        }

        final String multiChainName = name();
        final String selectedChainName = selectedChain.name();
        final Set<String> selectedSubChainActualKeys = settingsInformation.chainSettingsCombiner()
                .settingsJsonKeySet();

        final JsonObject multiSettings = SettingsCombiner.getSubSettingsByName(parentSettings, multiChainName);
        final JsonObject parentSubSettings = SettingsCombiner.getSubSettingsByName(parentSettings, selectedChainName);
        final JsonObject selectedSubSettings = multiSettings != null
                && (parentSubSettings == null || !multiSettings.isEmpty()) ?
                SettingsCombiner.getSubSettingsByName(multiSettings, selectedChainName) :
                parentSubSettings;
        // - Normally, settings for chain variant are stored inside multi-settings;
        // but, if there is no multi-settings, then we allow to store it directly in the parent.
        // If both sections are present, we choose non-empty from them;
        // if both are non-empty, we choose more standard way: multiSettings.
        // It is convenient to allow to set directly settings of a concrete variant,
        // if we don't want to allow choosing from variants.

        // 1st overriding: by section @sss, sss is the name of selected chain
        if (selectedSubSettings != null) {
            final JsonObject onlyActual = Jsons.filterJson(selectedSubSettings, selectedSubChainActualKeys);
            // - only actual: maybe this sub-settings contains a lot of other information (for deeper levels)
            result = Jsons.overrideEntries(result, onlyActual);
        }

        // 2nd overriding: by section @MMM (when extractSubSettings), MMM is the name of multichain,
        if (multiSettings != null) {
            final JsonObject onlyActual = Jsons.filterJson(multiSettings, selectedSubChainActualKeys);
            // - only actual: maybe this sub-settings contains a lot of other information (for other variants)
            result = Jsons.overrideEntries(result, onlyActual);
            // - Note: we use THE SAME actual keys, not multichain actual keys!
            // We need to return settings for selected CHAIN, that does not understand
            // any multichain parameters, excepting ones that are identical with (and also actual for)
            // CHAIN parameters. In particular, we must NEVER return here SELECTED_CHAIN_ID_PARAMETER_NAME:
            // it must not appear on the top level to avoid possible problems.
        }

        // 3rd overriding: by parent (both are performed in multiChainCombiner.overrideSettings method)
        return SettingsCombiner.overrideEntriesExceptingGivenSettings(
                result, parentSettings, multiChainName, selectedChainName);
    }

    public static void setSettings(String selectedChainSettingsString, Chain selectedChain) {
        Objects.requireNonNull(selectedChainSettingsString, "Null selectedChainSettingsString");
        Objects.requireNonNull(selectedChain, "Null selectedChain");
        final MainChainSettingsInformation settingsInformation =
                UseSubChain.getMainChainSettingsInformation(selectedChain);
        if (settingsInformation == null) {
            // - should not occur while normal usage (selecting chain from chainMap() result)
            throw new IllegalArgumentException("Selected chain has no built-in settings: " + selectedChain);
        }
        final ChainBlock settingsBlock = selectedChain.getBlock(settingsInformation.chainCombineSettingsBlockId());
        if (settingsBlock == null)
            throw new AssertionError("Dynamic executor '"
                    + settingsInformation.chainCombineSettingsBlockId() + "' not found in the chain " + selectedChain);
        settingsBlock.setActualInputData(CombineSettings.SETTINGS, SScalar.valueOf(selectedChainSettingsString));
    }

    public void freeResources() {
        Map<String, Chain> chainMap = this.chainMap;
        if (chainMap != null) {
            this.chainMap = null;
            // - to be on the safe side (recursive calls)
            for (Chain chain : chainMap.values()) {
                chain.freeResources();
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("multichain \"" + category()
                + ChainJson.CATEGORY_SEPARATOR + name()
                + "\", containing " + chainModels.size() + " chains:\n");
        for (int i = 0, n = chainModels.size(); i < n; i++) {
            ChainJson chainModel = chainModels.get(i);
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("   \"").append(chainModel.canonicalName())
                    .append("\", ID '").append(chainModel.chainId()).append("'");
        }
        return sb.toString();
    }

    @Override
    public MultiChain clone() {
        final MultiChain clone;
        try {
            clone = (MultiChain) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        clone.renewContextId();
        clone.chainMap = null;
        return clone;
    }

    @Override
    public void close() {
        freeResources();
    }

    // Note: this.chainModels must be already built
    private SettingsCombinerJson buildMultiChainSettingsModel(
            boolean addSubSettingsForVariants,
            Map<String, Chain> helpingChainMap) {
        final SettingsCombinerJson result = new SettingsCombinerJson();
        result.setId(model.getSettingsId());
        result.setName(model.getName());
        result.setCombineName(model.getSettingsName());
        result.setCategory(model.getSettingsCategory());
        final Map<String, ExecutorJson.ControlConf> controls = new LinkedHashMap<>();
        final ExecutorJson.ControlConf currentChainIdControl = createCurrentChainIdControl();
        controls.put(currentChainIdControl.getName(), currentChainIdControl);
        controls.putAll(model.getControls());
        if (addSubSettingsForVariants) {
            final String multiChainJsonFileMessage = model.getMultiChainJsonFile() == null ? "" :
                    " (problem occurred in multichain, loaded from the file " + model.getMultiChainJsonFile() + ")";
            for (ChainJson chainModel : chainModels) {
                final ChainJson.Executor executor = chainModel.getExecutor();
                final String name = executor.getName();
                try {
                    SettingsCombinerJson.checkParameterName(name, null);
                } catch (JsonException e) {
                    throw new IllegalArgumentException("Chain variant name \"" + name + "\" is invalid name: "
                            + "it is not allowed as a parameter name in the settings" + multiChainJsonFileMessage, e);
                }
                final ExecutorJson.ControlConf settingsControlConf = new ExecutorJson.ControlConf()
                        .setValueType(ParameterValueType.SETTINGS)
                        .setName(name)
                        .setDescription(executor.getDescription())
                        .setEditionType(ControlEditionType.VALUE)
                        .setAdvanced(true)
                        .setMultiline(true);
                final Chain chain = helpingChainMap.get(chainModel.chainId());
                if (chain != null) {
                    settingsControlConf.setGroupId(chain.id());
                    final String combinerId = UseSubChain.getMainChainSettingsCombinerId(chain);
                    if (combinerId != null) {
                        // - to be on the safe side (should not occur for normal multichain)
                        settingsControlConf.setBuilderId(combinerId);
                    }
                }
                if (controls.put(name, settingsControlConf) != null) {
                    throw new IllegalArgumentException("Chain variant name \"" + name + "\" has a name, identical "
                            + "to one of multichain parameters; it is not allowed" + multiChainJsonFileMessage);
                }
            }
        }
        result.setControls(controls);
        result.checkCompleteness();
        // - to be on the safe side
        return result;
    }

    private ExecutorJson.ControlConf createCurrentChainIdControl() {
        final List<ExecutorJson.ControlConf.EnumItem> items = new ArrayList<>();
        for (ChainJson chainModel : chainModels) {
            final String chainId = chainModel.chainId();
            items.add(new ExecutorJson.ControlConf.EnumItem(chainId).setCaption(chainModel.chainName()));
        }
        ExecutorJson.ControlConf result = new ExecutorJson.ControlConf();
        result.setName(SELECTED_CHAIN_ID_PARAMETER_NAME);
        result.setCaption(SELECTED_CHAIN_ID_PARAMETER_CAPTION);
        result.setValueType(ParameterValueType.ENUM_STRING);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(items);
        result.setDefaultStringValue(defaultChainVariantId);
        return result;
    }

    private Map<String, Chain> createChainMap() {
        Map<String, Chain> result = new LinkedHashMap<>();
        for (ChainJson chainModel : this.chainModels) {
            final String executorId = chainModel.chainId();
            final Chain chain = registeredChain(executorId);
            result.put(executorId, chain);
        }
        return result;
    }

    private Chain registeredChain(String executorId) {
        final Chain chain = InterpretSubChain.registeredChain(executorId);
        if (UseSubChain.getMainChainSettingsInformation(chain) == null) {
            throw new IllegalStateException("Chain \"" + chain.name()
                    + " \" (ID \"" + chain.id() + "\") of multichain \"" + name()
                    + "\" (ID \"" + id() + "\") has no built-in settings; "
                    + "it is not allowed inside multichains");
        }
        return chain;
    }

    private void renewContextId() {
        this.contextId = CURRENT_CONTEXT_ID.getAndIncrement();
    }
}
