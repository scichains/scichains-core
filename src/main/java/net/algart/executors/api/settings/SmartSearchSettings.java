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

import net.algart.executors.api.system.ExecutorLoaderSet;
import net.algart.executors.api.system.ExecutorSpecification;

import java.util.*;
import java.util.function.Supplier;

public class SmartSearchSettings {
    private static final String TESTED_SUBSTRING = "\"" + ExecutorSpecification.SETTINGS_MAME + "\"";

    private final SettingsSpecificationFactory factory;
    private final Supplier<? extends Collection<String>> allSettingsIds;

    private volatile boolean ready = false;
    private volatile Map<String, SettingsSpecification> allSettings = null;
    private boolean complete = false;
    private boolean hasDuplicates = false;

    private SmartSearchSettings(
            SettingsSpecificationFactory factory,
            Supplier<? extends Collection<String>> allSettingsIds) {
        this.factory = Objects.requireNonNull(
                factory, "Null settingsSpecificationFactory");
        this.allSettingsIds = Objects.requireNonNull(allSettingsIds, "Null allSettingsIds");
    }

    public static SmartSearchSettings getInstance(
            SettingsSpecificationFactory factory,
            Supplier<? extends Collection<String>> allSettingsIds) {
        return new SmartSearchSettings(factory, allSettingsIds);
    }

    public static SmartSearchSettings getInstance(
            SettingsSpecificationFactory factory,
            ExecutorLoaderSet executorLoaderSet,
            String sessionId) {
        Objects.requireNonNull(factory, "Null factory");
        Objects.requireNonNull(executorLoaderSet, "Null executor loader set");
        return getInstance(factory, () -> probableSettingsIds(executorLoaderSet, sessionId));
    }

    public SettingsSpecificationFactory factory() {
        return factory;
    }

    public Map<String, SettingsSpecification> allSettings() {
        checkReady();
        return allSettings;
    }

    public void process() {
        if (!ready) {
            return;
        }
        this.allSettings = makeAllSettings();
        for (SettingsSpecification specification : allSettings.values()) {
            for (ExecutorSpecification.ControlConf control : specification.getControls().values()) {
                // note: getControls() is synchronized
                String settingsId = control.getSettingsId();
                if (settingsId == null) {

                }
            }
        }

        ready = true;
    }

    public static Set<String> probableSettingsIds(ExecutorLoaderSet executorLoaderSet, String sessionId) {
        return probableSerializedSettings(executorLoaderSet, sessionId).keySet();
    }

    public static Map<String, String> probableSerializedSettings(
            ExecutorLoaderSet executorLoaderSet,
            String sessionId) {
        Objects.requireNonNull(executorLoaderSet, "Null executor loader set");
        final Map<String, String> serialized = executorLoaderSet.allSerializedSpecifications(
                sessionId, true);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> stringStringEntry : serialized.entrySet()) {
            if (stringStringEntry.getValue().contains(TESTED_SUBSTRING)) {
                result.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
        }
        return result;
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

    private Map<String, SettingsSpecification> makeAllSettings() {
        final Map<String, SettingsSpecification> result = new LinkedHashMap<>();
        for (String settingsId : this.allSettingsIds.get()) {
            final SettingsSpecification specification = factory.getSettingsSpecification(settingsId);
            if (specification != null) {
                result.put(settingsId, specification);
            }
        }
        return result;
    }

    private String tryToFindSettings(String valueClassName, String controlName) {
        for (Map.Entry<String, SettingsSpecification> entry : this.allSettings.entrySet()) {
            final SettingsSpecification specification = entry.getValue();
//TODO!!
        }
        return null;
    }

    private void checkReady() {
        if (!ready) {
            throw new IllegalStateException("Smart search has not been performed yet");
        }
    }
}
