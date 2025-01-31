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

package net.algart.executors.api.system;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.json.Jsons;

import java.util.*;

/**
 * Specification tree of executors, playing a role of settings:
 * real {@link SettingsSpecification settings}, mappings and so on.
 */
public final class SettingsTree {
    private static final System.Logger LOG = System.getLogger(SettingsTree.class.getName());

    private final SmartSearchSettings smartSearch;
    private final ExecutorSpecificationFactory factory;
    private final ExecutorSpecification specification;
    private final Map<String, SettingsTree> children = new LinkedHashMap<>();
    private int numberOfSubTrees;
    private final SettingsTree parent;
    private final boolean complete;

    private SettingsTree(
            SmartSearchSettings smartSearch,
            ExecutorSpecificationFactory factory,
            ExecutorSpecification specification,
            SettingsTree parent,
            Set<String> stackForDetectingRecursion) {
        this.smartSearch = smartSearch;
        this.factory = Objects.requireNonNull(factory, "Null specification factory");
        assert smartSearch == null || smartSearch.factory() == factory;
        this.specification = Objects.requireNonNull(specification, "Null settings specification");
        this.parent = parent;
        this.complete = buildTree(stackForDetectingRecursion);
    }

    public static SettingsTree of(SmartSearchSettings smartSearch, ExecutorSpecification specification) {
        return new SettingsTree(smartSearch, smartSearch.factory(), specification, null, new HashSet<>());
    }

    public static SettingsTree of(ExecutorSpecificationFactory factory, ExecutorSpecification specification) {
        return new SettingsTree(null, factory, specification, null, new HashSet<>());
    }

    public SettingsTree parent() {
        return parent;
    }

    public ExecutorSpecificationFactory factory() {
        return factory;
    }

    public ExecutorSpecification specification() {
        return specification;
    }

    public Map<String, SettingsTree> children() {
        return Collections.unmodifiableMap(children);
    }

    public int numberOfSubTrees() {
        return numberOfSubTrees;
    }

    public String id() {
        return specification.getId();
    }

    /**
     * Returns <code>true</code> if all children are successfully found by
     * {@link ExecutorSpecification.ControlConf#getSettingsId() settings ID} and
     * all subtrees are also complete.
     *
     * @return whether this tree is complete.
     */
    public boolean isComplete() {
        return complete;
    }

    public JsonObject specificationJson() {
        return specificationJson(ExecutorSpecification.JsonMode.MEDIUM);
        // - by default, there are no reasons to duplicate settings information in the tree
    }

    public JsonObject specificationJson(ExecutorSpecification.JsonMode mode) {
        specification.checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        specification.buildJson(builder, mode, name -> childJsonTree(name, mode));
        return builder.build();
    }

    public JsonObject defaultSettingsJson() {
        specification.checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        specification.buildDefaultSettingsJson(builder, this::childDefaultSettingsJsonTree);
        return builder.build();
    }

    public String jsonString() {
        return Jsons.toPrettyString(specificationJson());
    }

    public String jsonString(ExecutorSpecification.JsonMode mode) {
        return Jsons.toPrettyString(specificationJson(mode));
    }

    public String defaultSettingsJsonString() {
        return Jsons.toPrettyString(defaultSettingsJson());
    }

    @Override
    public String toString() {
        return "settings tree" + (smartSearch != null ? " (smart)" : "") + " for executor " +
                "\"" + specification.canonicalName() + "\" (" + specification.getId() + ")" +
                ": " + children.size() + " children";
    }

    private boolean buildTree(Set<String> stackForDetectingRecursion) {
        Map<String, SettingsTree> children = new LinkedHashMap<>();
        boolean complete = true;
        int count = 0;
        stackForDetectingRecursion.add(specification.getId());
        try {
            for (var entry : specification.getControls().entrySet()) {
                final String name = entry.getKey();
                final ExecutorSpecification.ControlConf control = entry.getValue();
                if (control.isSubSettings()) {
                    String settingsId = control.getSettingsId();
                    if (settingsId == null && smartSearch != null) {
                        smartSearch.search();
                        // ...and try again!
                        settingsId = control.getSettingsId();
                    }
                    final boolean found = settingsId != null;
                    complete &= found;
                    if (found) {
                        final ExecutorSpecification childSettings = factory.getSpecification(settingsId);
                        if (childSettings == null) {
                            throw new IllegalStateException("Child settings with ID \"" + settingsId +
                                    "\" (child control \"" + name + "\") is not found");
                        }
                        if (stackForDetectingRecursion.contains(settingsId)) {
                            throw new IllegalStateException("Cannot build tree due to recursive link to " +
                                    "settings ID \"" + settingsId +
                                    "\" (child control \"" + name +
                                    "\") detected in the settings with ID \"" + specification.getId() +
                                    "\" (\"" + specification.getName() +
                                    "\" in category \"" + specification.getCategory() + "\")");
                        }
                        final SettingsTree child = new SettingsTree(
                                smartSearch,
                                factory,
                                childSettings,
                                this,
                                stackForDetectingRecursion);
                        complete &= child.complete;
                        count += child.numberOfSubTrees() + 1;
                        children.put(name, child);
                    } else {
                        LOG.log(System.Logger.Level.DEBUG, () -> "Sub-settings " + name + " not found");
                    }
                }
            }
        } finally {
            stackForDetectingRecursion.remove(specification.getId());
        }
        // Note: we did not modify this object before this moment to make this method atomic regarding a failure
        this.children.clear();
        this.children.putAll(children);
        this.numberOfSubTrees = count;
        return complete;
    }

    private JsonObject childJsonTree(String name, ExecutorSpecification.JsonMode mode) {
        return children.containsKey(name) ? children.get(name).specificationJson(mode) : null;
    }

    private JsonObject childDefaultSettingsJsonTree(String name) {
        return children.containsKey(name) ? children.get(name).defaultSettingsJson() : null;
    }
}
