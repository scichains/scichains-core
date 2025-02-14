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

package net.algart.executors.api.multichains;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.*;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.settings.CombineSettings;
import net.algart.executors.api.settings.Settings;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.system.*;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class MultiChain implements Cloneable, AutoCloseable {
    public static final String SELECTED_CHAIN_ID = "___selectedChainId";
    public static final String SELECTED_CHAIN_NAME = "___selectedChainName";
    // - unlike chain ID, it is not a parameter of combiner, but only additional information inside result JSON
    public static final String SELECTED_CHAIN_ID_PARAMETER_CAPTION = "Selected chain";

    private static final boolean DEBUG_ALWAYS_SELECTION_BY_ID = false;
    // - for debugging: imitate situation when we must select by ID instead of name due to name collisions

    private static final AtomicLong CURRENT_CONTEXT_ID = new AtomicLong(109099000000000L);
    // - Some magic value helps to reduce the chance of accidental coincidence with other contextIDs,
    // probably used in the system in other ways (109, 99 are ASCII codes of letters 'mc').

    private volatile long contextId;
    // - Unique ID for every multi-chain. Unlike sub-chains, it is almost not used: a multi-chain is not an environment
    // for executing anything; but it is used as a context ID for multi-chain settings combiner.
    private final MultiChainSpecification specification;
    private final UseSubChain chainFactory;
    private final List<ChainSpecification> chainSpecifications;
    private final List<ChainSpecification> blockedChainSpecifications;
    private final Set<String> blockedChainSpecificationNames;
    private final List<ExecutorSpecification> loadedChainExecutorSpecifications;
    private final boolean selectionById;
    private final String firstChainId;
    private final String firstChainName;
    private String defaultChainIdOrName;
    // - filled in createSelectedChainIdControl()
    private final Settings multiChainOnlyCommonSettings;
    // - note: this combiner is not registered, it is used for building a multi-chain executor only in UseMultiChain
    private final Settings multiChainSettings;

    // Note: unlike Chain, currentDirectory is not actual here: loading without files is senseless here.
    private volatile Map<String, Chain> chainMap = null;

    private boolean extractSubSettings = false;

    private MultiChain(
            MultiChainSpecification specification,
            UseSubChain chainFactory,
            UseMultiChainSettings settingsFactory)
            throws IOException {
        renewContextId();
        this.specification = Objects.requireNonNull(specification, "Null specification");
        this.chainFactory = Objects.requireNonNull(chainFactory, "Null chainFactory");
        Objects.requireNonNull(settingsFactory, "Null settingsFactory");
        this.specification.checkCompleteness();
        this.chainSpecifications = specification.readChainVariants();
        this.blockedChainSpecifications = new ArrayList<>();
        this.blockedChainSpecificationNames = new LinkedHashSet<>();
        this.loadedChainExecutorSpecifications = new ArrayList<>();
        assert !this.chainSpecifications.isEmpty();
        final Map<String, Chain> nonRecursiveChainMap = new HashMap<>();
        String firstChainId = null;
        String firstChainName = null;
        for (ChainSpecification chainSpecification : this.chainSpecifications) {
            if (firstChainId == null) {
                firstChainId = chainSpecification.chainId();
                firstChainName = chainSpecification.chainName();
            }
            final Optional<Chain> optionalChain;
            try {
                optionalChain = chainFactory.useIfNonRecursive(chainSpecification);
            } catch (ChainLoadingException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ChainRunningException("Cannot initialize sub-chain "
                        + chainSpecification.getChainSpecificationFile() + ", variant of multi-chain "
                        + specification.getMultiChainSpecificationFile(), e);
            }
            if (optionalChain.isPresent()) {
                final ExecutorSpecification implementationSpecification = chainFactory.chainExecutorSpecification();
                assert implementationSpecification != null :
                        "chainExecutorSpecification cannot be null if use() returns some result";
                this.loadedChainExecutorSpecifications.add(implementationSpecification);
                nonRecursiveChainMap.put(optionalChain.get().id(), optionalChain.get());
            } else {
                blockedChainSpecifications.add(chainSpecification);
                blockedChainSpecificationNames.add(chainSpecification.chainName());
            }
            // - Note: if the process of loading chain was blocked due to recursion, it means that it is in
            // the process of registering and not available yet even via ExecutionBlock.getExecutorSpecification.
            // We will not be able to check it at loading stage by checkImplementationCompatibility()
            // and will not be able to use help from nonRecursiveChainMap,
            // but it is not-too-serious problem: such a recursion is a very rare case.
        }
        if (firstChainId == null) {
            throw new AssertionError("Cannot find the first chain in non-empty collection");
        }
        this.selectionById = specification.isBehaviourPreferSelectionById()
                || detectNecessityOfSelectionById(nonRecursiveChainMap.values());
        this.firstChainId = firstChainId;
        this.firstChainName = firstChainName;
        settingsFactory.setOwnerId(specification.getId());
        settingsFactory.setContextId(contextId);
        settingsFactory.setContextName(specification.getName());
        // - this information, set by previous operators, will be used only in the following operators,
        // to make a reference to this MultiChain and to set the correct owner information
        // inside newly created settings combiner
        this.multiChainOnlyCommonSettings = Settings.of(
                buildMultiChainSettingsSpecification(false, nonRecursiveChainMap));
        // - this (internally used) combiner does not contain advanced multi-line controls
        // for settings of the chain variants; it is used in UseMultiChain.buildMultiChainSpecification()
        assert this.defaultChainIdOrName != null :
                "defaultChainIdOrName must be filled in createSelectedChainIdControl()";
        settingsFactory.setMultiChain(this);
        // - this reference will be necessary in CombineMultiChainSettings.correctSettings
        this.multiChainSettings = settingsFactory.use(
                buildMultiChainSettingsSpecification(true, nonRecursiveChainMap));
    }

    public static MultiChain of(
            MultiChainSpecification specification,
            UseSubChain chainFactory,
            UseMultiChainSettings settingsFactory)
            throws IOException {
        return new MultiChain(specification, chainFactory, settingsFactory);
    }

    public boolean isExtractSubSettings() {
        return extractSubSettings;
    }

    public MultiChain setExtractSubSettings(boolean extractSubSettings) {
        this.extractSubSettings = extractSubSettings;
        return this;
    }

    public MultiChainSpecification specification() {
        return specification;
    }

    public List<ChainSpecification> chainSpecifications() {
        return Collections.unmodifiableList(chainSpecifications);
    }

    public List<ChainSpecification> blockedChainSpecifications() {
        return Collections.unmodifiableList(blockedChainSpecifications);
    }

    public Set<String> blockedChainSpecificationNames() {
        return Collections.unmodifiableSet(blockedChainSpecificationNames);
    }

    public String defaultChainVariant() {
        return defaultChainIdOrName;
    }

    public Settings multiChainOnlyCommonSettingsCombiner() {
        return multiChainOnlyCommonSettings;
    }

    public Settings multiChainSettingsCombiner() {
        return multiChainSettings;
    }

    public long contextId() {
        // Note: it will be another for the result of clone!
        return contextId;
    }

    public Path multiChainSpecificationFile() {
        return specification.getMultiChainSpecificationFile();
    }

    public String id() {
        return specification.getId();
    }

    public String category() {
        return specification.getCategory();
    }

    public String name() {
        return specification.getName();
    }

    public String description() {
        return specification.getDescription();
    }

    public String settingsId() {
        return specification.getSettingsId();
    }

    public void checkImplementationCompatibility() {
        for (ExecutorSpecification implementation : loadedChainExecutorSpecifications) {
            specification.checkImplementationCompatibility(implementation);
        }
    }

    public UseSubChain сhainFactory() {
        return chainFactory;
    }

    // Note: it is not-too-good idea to create it only inside the constructor.
    // Every chain can require a lot of resources, and different clones of multi-chain (see clone())
    // should have different chains.
    public Map<String, Chain> chainMap() {
        Map<String, Chain> chainMap = this.chainMap;
        if (chainMap == null) {
            this.chainMap = chainMap = createChainMap();
            // - the order is important for multithreading
        }
        return Collections.unmodifiableMap(chainMap);
    }

    public String getSelectedChainVariant(JsonObject parentSettings, String defaultChainVariant) {
        Objects.requireNonNull(parentSettings, "Null parentSettings");
        Objects.requireNonNull(defaultChainVariant, "Null defaultChainVariant");
        String result = defaultChainVariant;
        if (parentSettings.isEmpty()) {
            return result;
        }
        if (extractSubSettings) {
            final JsonObject multiChainSubSettings = Settings.getSubSettingsByName(parentSettings, name());
            if (multiChainSubSettings != null) {
                result = multiChainSubSettings.getString(selectedChainParameter(), result);
            }
        }
        return parentSettings.getString(selectedChainParameter(), result);
        // - overriding by the same parameter in the parent JSON, if exists
    }

    public Chain findSelectedChain(String selectedChainVariant) {
        Objects.requireNonNull(selectedChainVariant, "Null selectedChainVariant");
        final Map<String, Chain> chains = chainMap();
        Chain selectedChain = chains.get(selectedChainVariant);
        if (selectedChain == null) {
            for (ChainSpecification specification : chainSpecifications) {
                if (specification.chainName().equals(selectedChainVariant)) {
                    selectedChain = chains.get(specification.chainId());
                }
            }
        }
        if (selectedChain == null) {
            throw new IllegalArgumentException("Cannot find the selected chain by its ID or name: \"" +
                    selectedChainVariant + "\"" + "; " +
                    "there is no chain variant with this ID/name among all elements of this multi-chain " + this);
        }
        return selectedChain;
    }

    public JsonObject selectedChainSettings(
            JsonObject executorSettings,
            JsonObject parentSettings,
            Chain selectedChain) {
        Objects.requireNonNull(executorSettings, "Null executorSettings");
        Objects.requireNonNull(parentSettings, "Null parentSettings");
        Objects.requireNonNull(selectedChain, "Null selectedChain");
        final Settings mainSettings = selectedChain.getMainSettingsCombiner();
        if (mainSettings == null) {
            return null;
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
        final Set<String> selectedSubChainActualKeys = mainSettings.settingsKeySet();

        final JsonObject multiSettings = Settings.getSubSettingsByName(parentSettings, multiChainName);
        final JsonObject parentSubSettings = Settings.getSubSettingsByName(parentSettings, selectedChainName);
        final JsonObject selectedSubSettings = multiSettings != null
                && (parentSubSettings == null || !multiSettings.isEmpty()) ?
                Settings.getSubSettingsByName(multiSettings, selectedChainName) :
                parentSubSettings;
        // - Normally, settings for chain variant are stored inside multi-settings;
        // but, if there are no multi-settings, then we allow to store it directly in the parent.
        // If both sections are present, we choose non-empty from them;
        // if both are non-empty, we choose more standard way: multiSettings.
        // It is convenient to allow setting directly settings of a concrete variant
        // if we don't want to allow choosing from variants.

        // 1st overriding: by section @sss, sss is the name of selected chain
        if (selectedSubSettings != null) {
            final JsonObject onlyActual = Jsons.filterJson(selectedSubSettings, selectedSubChainActualKeys);
            // - only actual: maybe this sub-settings contains a lot of other information (for deeper levels)
            result = Jsons.overrideEntries(result, onlyActual);
        }

        // 2nd overriding: by section @MMM (when extractSubSettings), MMM is the name of multi-chain
        if (multiSettings != null) {
            final JsonObject onlyActual = Jsons.filterJson(multiSettings, selectedSubChainActualKeys);
            // - only actual: maybe this sub-settings contains a lot of other information (for other variants)
            result = Jsons.overrideEntries(result, onlyActual);
            // - Note: we use THE SAME actual keys, not multi-chain actual keys!
            // We need to return settings for selected CHAIN, that does not understand
            // any multi-chain parameters, excepting ones that are identical with (and also actual for)
            // CHAIN parameters. In particular, we must NEVER return here SELECTED_CHAIN_ID_PARAMETER_NAME:
            // it must not appear on the top level to avoid possible problems.
        }

        // 3rd overriding: by parent (both are performed in multiChainCombiner.overrideSettings method)
        return Settings.overrideEntriesExceptingGivenSettings(
                result, parentSettings, multiChainName, selectedChainName);
    }

    public static void setSettings(String selectedChainSettingsString, Chain selectedChain) {
        if (selectedChainSettingsString == null) {
            return;
        }
        Objects.requireNonNull(selectedChain, "Null selectedChain");
        final String mainSettingsBlockId = selectedChain.getMainSettingsBlockId();
        if (mainSettingsBlockId == null) {
            // - should not occur while normal usage (selectedChainSettings is not null)
            throw new IllegalArgumentException("Selected chain has no built-in settings: " + selectedChain);
        }
        final ChainBlock settingsBlock = selectedChain.getBlock(mainSettingsBlockId);
        if (settingsBlock == null)
            throw new AssertionError("Main settings block  '"
                    + mainSettingsBlockId + "' is not found in the chain " + selectedChain);
        settingsBlock.setActualInputData(
                CombineSettings.SETTINGS, SScalar.of(selectedChainSettingsString));
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

    public MultiChainExecutor toExecutor(InstantiationMode instantiationMode) {
        final ExecutorFactory executorFactory = chainFactory.executorFactory();
        ExecutionBlock result;
        try {
            result = executorFactory.newExecutor(id(), instantiationMode);
            // - we suppose that someone has a registered executor, which execute this chain
        } catch (ClassNotFoundException | ExecutorNotFoundException e) {
            throw new IllegalStateException("Multi-chain with ID " + id() + " was not successfully registered", e);
        }
        if (!(result instanceof MultiChainExecutor)) {
            throw new IllegalStateException("Multi-chain with ID " + id() + " is executed by some non-standard way: "
                    + "its executor is not an instance of " + MultiChainExecutor.class);
        }
        return (MultiChainExecutor) result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("multi-chain \""
                + ExecutorSpecification.className(category(), name())
                + "\", containing " + chainSpecifications.size() + " chains:\n");
        for (int i = 0, n = chainSpecifications.size(); i < n; i++) {
            ChainSpecification chainchainSpecification = chainSpecifications.get(i);
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("   \"").append(chainchainSpecification.canonicalName())
                    .append("\", ID '").append(chainchainSpecification.chainId()).append("'");
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

    public String selectedChainParameter() {
        return specification.isBehaviourPreferSelectionById() ? SELECTED_CHAIN_ID : SELECTED_CHAIN_NAME;
        // - Important that we must use here the specification flag, not an effective selectionById field:
        // otherwise, the user will not be able to understand, which parameter he must use to select the variant
    }

    // Note: the result of this method is also used for building ports/controls of the multi-chain executor
    // in the UseMultiChain.buildMultiChainSpecification method.
    private SettingsSpecification buildMultiChainSettingsSpecification(
            boolean addSubSettingsForSelectedChainVariants,
            Map<String, Chain> helpingChainMap) {
        final SettingsSpecification result = new SettingsSpecification();
        result.setId(specification.getSettingsId());
        result.setName(specification.getName(), specification.isAutogeneratedName());
        result.setCombineName(specification.getSettingsName());
        result.setCategory(specification.getSettingsCategory(), specification.isAutogeneratedSettingsCategory());
        final Map<String, ExecutorSpecification.ControlConf> controls = new LinkedHashMap<>();
        final ExecutorSpecification.ControlConf currentChainIdControl = createSelectedChainIdControl();
        controls.put(currentChainIdControl.getName(), currentChainIdControl);
        controls.putAll(specification.getControls());
        if (addSubSettingsForSelectedChainVariants) {
            final String multiChainSpecificationFileMessage =
                    specification.getMultiChainSpecificationFile() == null ? "" :
                            " (problem occurred in multi-chain, loaded from the file " +
                                    specification.getMultiChainSpecificationFile() + ")";
            for (ChainSpecification chainSpecification : chainSpecifications) {
                final ChainSpecification.Executor executor = chainSpecification.getExecutor();
                final String name = executor.getName();
                try {
                    SettingsSpecification.checkParameterName(name, null);
                } catch (JsonException e) {
                    throw new IllegalArgumentException("Chain variant name \"" + name + "\" is invalid name, "
                            + "not allowed as a parameter name in the settings" +
                            multiChainSpecificationFileMessage, e);
                }
                final ExecutorSpecification.ControlConf settingsControlConf = new ExecutorSpecification.ControlConf()
                        .setValueType(ParameterValueType.SETTINGS)
                        .setName(name)
                        .setDescription(executor.getDescription())
                        .setEditionType(ControlEditionType.VALUE)
                        .setAdvanced(true)
                        .setMultiline(true);
                final Chain chain = helpingChainMap.get(chainSpecification.chainId());
                if (chain != null) {
                    final Settings mainSettings = chain.getMainSettingsCombiner();
                    if (mainSettings != null) {
                        final SettingsSpecification specification = mainSettings.specification();
                        settingsControlConf.setSettingsId(specification.getId());
                        settingsControlConf.setValueClassName(specification.className());
//                        System.out.printf("Variant %s -> %s%n", specification.getName(), specification.className());
                    }
                }
                if (controls.put(name, settingsControlConf) != null) {
                    throw new IllegalArgumentException("Chain variant name \"" + name + "\" has a name, identical "
                            + "to one of multi-chain parameters; this is not allowed" +
                            multiChainSpecificationFileMessage);
                }
            }
        }
        result.setControls(controls);
        result.checkCompleteness();
        // - to be on the safe side
        return result;
    }

    private ExecutorSpecification.ControlConf createSelectedChainIdControl() {
        final List<ExecutorSpecification.ControlConf.EnumItem> items = new ArrayList<>();
        final String defaultChainVariantId = specification.getDefaultChainVariantId();
        final String defaultChainVariantName = specification.getDefaultChainVariantName();
        String defaultValue = selectionById ? defaultChainVariantId : defaultChainVariantName;
        for (ChainSpecification sp : chainSpecifications) {
            final String itemValue, itemCaption;
            final boolean probablyDefault;
            if (selectionById) {
                itemValue = sp.chainId();
                itemCaption = sp.chainName() + " [" + sp.chainId() + "]";
                probablyDefault = sp.chainName().equals(defaultChainVariantName);
            } else {
                itemValue = sp.chainName();
                itemCaption = sp.chainName();
                probablyDefault = sp.chainId().equals(defaultChainVariantId);
            }
            if (defaultValue == null && probablyDefault) {
                defaultValue = itemValue;
            }
            items.add(new ExecutorSpecification.ControlConf.EnumItem(itemValue).setCaption(itemCaption));
        }
        if (defaultValue == null) {
            defaultValue = selectionById ? firstChainId : firstChainName;
        }
        this.defaultChainIdOrName = defaultValue;
        ExecutorSpecification.ControlConf result = new ExecutorSpecification.ControlConf();
        result.setName(selectedChainParameter());
        result.setCaption(SELECTED_CHAIN_ID_PARAMETER_CAPTION);
        result.setValueType(ParameterValueType.ENUM_STRING);
        result.setEditionType(ControlEditionType.ENUM);
        result.setItems(items);
        result.setDefaultStringValue(defaultValue);
        return result;
    }

    private Map<String, Chain> createChainMap() {
        Map<String, Chain> result = new LinkedHashMap<>();
        for (ChainSpecification specification : this.chainSpecifications) {
            final String executorId = specification.chainId();
            final Chain chain = InterpretSubChain.registeredChain(chainFactory.getSessionId(), executorId);
            result.put(executorId, chain);
        }
        return result;
    }

    /*
    private Chain registeredChain(String sessionId, String executorId) {
        final Chain chain = InterpretSubChain.registeredChain(sessionId, executorId);
        if (chain.getMainSettingsCombiner() == null) {
            throw new IllegalStateException("Chain \"" + chain.name()
                    + " \" (ID \"" + chain.id() + "\") of multi-chain \"" + name()
                    + "\" (ID \"" + id() + "\") has no built-in main settings; "
                    + "this is not allowed inside multi-chains");
        }
        return chain;
    }
    */

    private void renewContextId() {
        this.contextId = CURRENT_CONTEXT_ID.getAndIncrement();
    }

    private static boolean detectNecessityOfSelectionById(Collection<Chain> chainVariants) {
        if (DEBUG_ALWAYS_SELECTION_BY_ID) {
            return true;
        }
        final Set<String> uniqueNames = new HashSet<>();
        for (Chain chain : chainVariants) {
            if (!uniqueNames.add(chain.id()) || !uniqueNames.add(chain.name())) {
                // - All IDs and names must be different!
                // Note that a problem is very improbable here:
                // identical names or identical IDs were already checked in the constructor
                // (see readChainVariants() method).
                return true;
            }
        }
        return false;
    }

}
