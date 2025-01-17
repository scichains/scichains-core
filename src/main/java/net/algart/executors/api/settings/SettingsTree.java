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
import net.algart.executors.api.system.ExecutorLoaderSet;
import net.algart.executors.api.system.ExecutorSpecification;

import java.util.*;
import java.util.function.Supplier;

//TODO!!
public class SettingsTree {
    private final SettingsSpecification settingsSpecification;
    private final Map<String, SettingsTree> children = new LinkedHashMap<>();
    private SettingsTree parent = null;
    private boolean complete = false;

    private SettingsTree(SettingsSpecification settingsSpecification) {
        this.settingsSpecification =
                Objects.requireNonNull(settingsSpecification, "Null settings specification");
    }

    public static SettingsTree getInstance(SettingsSpecification settingsSpecification) {
        return new SettingsTree(settingsSpecification);
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

    public SettingsTree buildTree(SettingsSpecificationFactory specificationFactory) {
        //TODO!! just analyse settingsId, build links
        //TODO!! atomic for failure! call for new children map, only then copy into this.children
        return this;
    }

    public JsonObject toJson() {
        //TODO!! without build, works like settingsSpecification.toJson();
        return settingsSpecification.toJson();
    }

    public JsonObject defaultsJson() {
        //TODO!!
        throw new UnsupportedOperationException();
    }

    public static class SmartSearch {
        private final Supplier<? extends Collection<String>> allSettingsIds;
        private final SettingsSpecificationFactory settingsSpecificationFactory;

        private boolean complete = false;
        private boolean hasDuplicates = false;

        private SmartSearch(
                Supplier<? extends Collection<String>> allSettingsIds,
                SettingsSpecificationFactory settingsSpecificationFactory) {
            this.allSettingsIds = Objects.requireNonNull(allSettingsIds, "Null allSettingsIds");
            this.settingsSpecificationFactory = Objects.requireNonNull(
                    settingsSpecificationFactory, "Null settingsSpecificationFactory");
        }

        public static SmartSearch getInstance(
                Supplier<? extends Collection<String>> allSettingsIds,
                SettingsSpecificationFactory settingsSpecificationFactory) {
            return new SmartSearch(allSettingsIds, settingsSpecificationFactory);
        }

        public static SmartSearch getInstance(ExecutorLoaderSet executorLoaderSet, String sessionId) {
            Objects.requireNonNull(executorLoaderSet, "Null executor loader set");
            return getInstance(
                    () -> executorLoaderSet.allSessionExecutorIds(sessionId, true),
                    executorLoaderSet.newFactory(sessionId));
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
