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

import java.util.*;
import java.util.function.Supplier;

public class SmartSearchSettings {
    private static final String TESTED_SUBSTRING = "\"" + ExecutorSpecification.SETTINGS + "\"";
    private static final System.Logger LOG = System.getLogger(SettingsTree.class.getName());

    private final ExecutorSpecificationFactory factory;
    private final Supplier<? extends Collection<String>> allSettingsIds;

    private System.Logger.Level logLevel = System.Logger.Level.DEBUG;

    private volatile boolean ready = false;
    private volatile Map<String, ExecutorSpecification> allSettings = null;
    private volatile boolean complete = false;

    private SmartSearchSettings(
            ExecutorSpecificationFactory factory,
            Supplier<? extends Collection<String>> allSettingsIds) {
        this.factory = Objects.requireNonNull(factory, "Null factory");
        this.allSettingsIds = Objects.requireNonNull(allSettingsIds, "Null allSettingsIds");
    }

    public static SmartSearchSettings of(
            ExecutorSpecificationFactory factory,
            Supplier<? extends Collection<String>> allExecutorsIdsToCheck) {
        return new SmartSearchSettings(factory, allExecutorsIdsToCheck);
    }

    public static SmartSearchSettings of(
            ExecutorSpecificationFactory factory,
            ExecutorLoaderSet executorLoaderSet,
            String sessionId) {
        Objects.requireNonNull(factory, "Null factory");
        Objects.requireNonNull(executorLoaderSet, "Null executor loader set");
        return of(factory, () -> probableSettingsIds(executorLoaderSet, sessionId));
    }

    public ExecutorSpecificationFactory factory() {
        return factory;
    }

    public System.Logger.Level getLogLevel() {
        return logLevel;
    }

    public SmartSearchSettings setLogLevel(System.Logger.Level logLevel) {
        this.logLevel = Objects.requireNonNull(logLevel, "Null logLevel");
        return this;
    }

    public Map<String, ExecutorSpecification> allSettings() {
        checkReady();
        return allSettings;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean search() {
        if (ready) {
            return complete;
        }
        this.allSettings = makeAllSettings();
        boolean complete = true;
        for (ExecutorSpecification specification : allSettings.values()) {
            final Map<String, ExecutorSpecification.ControlConf> controls = specification.getControls();
            // - note: getControls() is synchronized
            for (var entry : controls.entrySet()) {
                final String name = entry.getKey();
                final ExecutorSpecification.ControlConf control = entry.getValue();
                if (control.isSubSettings()) {
                    String settingsId = control.getSettingsId();
                    if (settingsId == null) {
                        settingsId = tryToFindSettings(control.getValueClassName(), name);
                        if (settingsId != null) {
                            specification.updateControlSettingsId(name, settingsId);
                            // - note: updateControlSettingsId() is synchronized
                        } else {
                            complete = false;
                            LOG.log(logLevel, () -> "Cannot find sub-settings for control \"" + name +
                                    "\" of the executor " +
                                    "\"" + specification.canonicalName() + "\" (" + specification.getId() + ")");
                        }
                    }
                }
            }
        }
        this.complete = complete;
        this.ready = true;
        return complete;
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


    private Map<String, ExecutorSpecification> makeAllSettings() {
        final Map<String, ExecutorSpecification> result = new LinkedHashMap<>();
        for (String settingsId : this.allSettingsIds.get()) {
            final ExecutorSpecification specification = factory.getSpecification(settingsId);
            if (specification != null && specification.isRoleSettings()) {
                result.put(settingsId, specification);
            }
        }
        return result;
    }

    private String tryToFindSettings(String valueClassName, String controlName) {
        for (Map.Entry<String, ExecutorSpecification> entry : this.allSettings.entrySet()) {
            final ExecutorSpecification specification = entry.getValue();
            final ExecutorSpecification.Options.Role role = specification.getRole();
            if (role == null) {
                // - improbable: possible only due to modification from parallel thread
                continue;
            }
            if (role.matchesClass(valueClassName) || role.matchesClass(controlName)) {
                return specification.getId();
            }
        }
        return null;
    }

    private void checkReady() {
        if (!ready) {
            throw new IllegalStateException("Smart search has not been performed yet");
        }
    }
}
