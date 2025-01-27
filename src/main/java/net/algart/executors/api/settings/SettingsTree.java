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
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.system.ExecutorLoaderSet;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;

import java.util.*;
import java.util.function.Supplier;

public final class SettingsTree extends AbstractConvertibleToJson {
    private final SettingsSpecificationFactory factory;
    private final SettingsSpecification specification;
    private final Map<String, SettingsTree> children = new LinkedHashMap<>();
    private final SettingsTree parent;
    private final boolean complete;

    SettingsTree(SettingsSpecificationFactory factory, SettingsSpecification specification) {
        this(factory, specification, null, new HashSet<>());
    }

    private SettingsTree(
            SettingsSpecificationFactory factory,
            SettingsSpecification specification,
            SettingsTree parent,
            Set<String> stackForDetectingRecursion) {
        this.factory = Objects.requireNonNull(factory, "Null specification factory");
        this.specification = Objects.requireNonNull(specification, "Null settings specification");
        this.parent = parent;
        this.complete = buildTree(stackForDetectingRecursion);
    }

    public SettingsTree parent() {
        return parent;
    }

    public SettingsSpecificationFactory factory() {
        return factory;
    }

    public SettingsSpecification specification() {
        return specification;
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

    private boolean buildTree(Set<String> stackForDetectingRecursion) {
        Map<String, SettingsTree> children = new LinkedHashMap<>();
        boolean complete = true;
        stackForDetectingRecursion.add(specification.getId());
        try {
            for (var entry : specification.getControls().entrySet()) {
                final String name = entry.getKey();
                final ExecutorSpecification.ControlConf control = entry.getValue();
                if (control.getValueType().isSettings()) {
                    final String settingsId = control.getSettingsId();
                    final boolean found = settingsId != null;
                    complete &= found;
//                    if (!found) System.out.printf("%s: not found%n", name);
                    if (found) {
                        final SettingsSpecification childSettings = factory.getSettingsSpecification(settingsId);
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
                                factory, childSettings, this, stackForDetectingRecursion);
                        complete &= child.complete;
                        children.put(name, child);
                    }
                }
            }
        } finally {
            stackForDetectingRecursion.remove(specification.getId());
        }
        // Note: we did not modify this object before this moment to make this method atomic regarding a failure
        this.children.clear();
        this.children.putAll(children);
        return complete;
    }

    public void buildJson(JsonObjectBuilder builder) {
        specification.buildJson(builder, this::childJsonTree);
    }

    public JsonObject defaultValuesJson() {
        //TODO!! recursive JSON with all default settings
        throw new UnsupportedOperationException();
    }

    private JsonObject childJsonTree(String name) {
        return children.containsKey(name) ? children.get(name).toJson() : null;
    }

    public static class SmartSearch {
        private final SettingsSpecificationFactory settingsSpecificationFactory;
        private final Supplier<? extends Collection<String>> allSettingsIds;

        private boolean complete = false;
        private boolean hasDuplicates = false;

        private SmartSearch(
                SettingsSpecificationFactory settingsSpecificationFactory,
                Supplier<? extends Collection<String>> allSettingsIds) {
            this.settingsSpecificationFactory = Objects.requireNonNull(
                    settingsSpecificationFactory, "Null settingsSpecificationFactory");
            this.allSettingsIds = Objects.requireNonNull(allSettingsIds, "Null allSettingsIds");
        }

        public static SmartSearch getInstance(
                SettingsSpecificationFactory settingsSpecificationFactory,
                Supplier<? extends Collection<String>> allSettingsIds) {
            return new SmartSearch(settingsSpecificationFactory, allSettingsIds);
        }

        public static SmartSearch getInstance(Map<String, SettingsSpecification> settingsSpecifications) {
            Objects.requireNonNull(settingsSpecifications, "Null settingsSpecifications");
            return new SmartSearch(settingsSpecifications::get, settingsSpecifications::keySet);
        }

        public static SmartSearch getInstance(ExecutorLoaderSet executorLoaderSet, String sessionId) {
            Objects.requireNonNull(executorLoaderSet, "Null executor loader set");
            return getInstance(
                    executorLoaderSet.newFactory(sessionId),
                    () -> executorLoaderSet.allSessionExecutorIds(sessionId, true)
            );
        }

        public void findChildren() {
            //TODO!! fill complete, hasDuplicates (??)
            //TODO!! don't forget about synchronization by SettingsSpecification.controlsLock
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean hasDuplicates() {
            return hasDuplicates;
        }
    }
}
