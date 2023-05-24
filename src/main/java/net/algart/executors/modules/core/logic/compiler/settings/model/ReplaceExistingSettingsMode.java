/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.compiler.settings.model;

import net.algart.json.Jsons;

import jakarta.json.JsonObject;

public enum ReplaceExistingSettingsMode {
    SIMPLE() {
        @Override
        public JsonObject replace(JsonObject existingSubSettings, JsonObject newSubSettings) {
            return newSubSettings;
        }
    },
    ADD_NEW() {
        @Override
        public JsonObject replace(JsonObject existingSubSettings, JsonObject newSubSettings) {
            return existingSubSettings == null ?
                    newSubSettings :
                    Jsons.addNonExistingEntries(existingSubSettings, newSubSettings);
        }
    },
    OVERRIDE() {
        @Override
        public JsonObject replace(JsonObject existingSubSettings, JsonObject newSubSettings) {
            return existingSubSettings == null ?
                    newSubSettings :
                    Jsons.overrideEntries(existingSubSettings, newSubSettings);
        }
    },
    OVERRIDE_ONLY_EXISTING_IN_BOTH() {
        @Override
        public JsonObject replace(JsonObject existingSubSettings, JsonObject newSubSettings) {
            return existingSubSettings == null ?
                    newSubSettings :
                    Jsons.overrideOnlyExistingInBoth(existingSubSettings, newSubSettings);
        }
    };

    public abstract JsonObject replace(JsonObject existingSubSettings, JsonObject newSubSettings);
}
