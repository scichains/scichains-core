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

package net.algart.executors.api.system.tests;

import jakarta.json.JsonObject;
import net.algart.executors.api.extensions.InstalledExtensions;
import net.algart.executors.api.parameters.Parameters;
import net.algart.executors.api.settings.SettingsBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.executors.api.settings.UseSettings;
import net.algart.json.Jsons;

import java.io.IOException;
import java.nio.file.Paths;

public class SettingsTest {
    private final static SettingsSpecification SETTINGS_SPECIFICATION = SettingsSpecification.of("""
            {
              "app": "settings",
              "name": "test",
              "id": "TEST_1234",
              "controls": [
                {
                  "name": "a",
                  "value_type": "double",
                  "edition_type": "value",
                  "default": 100
                },
                {
                  "name": "b",
                  "value_type": "double",
                  "edition_type": "value"
                },
                {
                  "name": "path",
                  "value_type": "String",
                  "edition_type": "file",
                  "default": "./"
                },
                {
                  "name": "details",
                  "value_type": "settings",
                  "edition_type": "value"
                }
              ]
            }
            """);

    public static void main(String[] args) throws IOException {
        final SettingsBuilder settingsBuilder = SettingsBuilder.of(SETTINGS_SPECIFICATION);
        settingsBuilder.setAddSettingsClass(true);
        final JsonObject jsonDefault = settingsBuilder.buildDefault();
        System.out.printf("Defaults:%n%s%n", Jsons.toPrettyString(jsonDefault));

        Parameters parameters = new Parameters();
        parameters.setString("a", "2.0");
        parameters.setDouble("b", 3.0);
        parameters.setString("details", "{\"delta\":0.001}");
        parameters.setString("path", "my_file.dat");
        final JsonObject jsonParameters = settingsBuilder.build(parameters);
        System.out.printf("Parameters:%n%s%n", Jsons.toPrettyString(jsonParameters));

        System.setProperty(InstalledExtensions.EXTENSIONS_ROOT_PROPERTY, "build");
        try (var executor = UseSettings.newSharedExecutor(SETTINGS_SPECIFICATION)) {
            executor.setCurrentDirectory(Paths.get(".").toAbsolutePath());
            // - leads to translating "path" parameter
            executor.setParameters(parameters);
            executor.putStringScalar("details", "{\"delta\":0.002}");
            // - overrides the previous call: possible alternative to setStringParameter for a case of settings
            final String result = executor.combine();
            System.out.printf("%s%nResult of executor is%n%s%n", executor, result);
        }
    }
}
