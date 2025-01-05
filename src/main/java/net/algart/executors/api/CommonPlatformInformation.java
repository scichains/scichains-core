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

package net.algart.executors.api;

import net.algart.executors.api.system.ExtensionSpecification;
import net.algart.executors.api.system.InstalledExtensions;

import java.util.List;

public class CommonPlatformInformation extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_ID = "id";
    public static final String OUTPUT_NAME = "name";
    public static final String OUTPUT_DESCRIPTION = "description";
    public static final String OUTPUT_TECHNOLOGY = "technology";
    public static final String OUTPUT_LANGUAGE = "language";
    public static final String OUTPUT_ROOT_FOLDER = "root_folder";
    public static final String OUTPUT_SPECIFICATIONS_FOLDER = "specifications_folder";
    public static final String OUTPUT_MODULES_FOLDER = "modules_folder";
    public static final String OUTPUT_LIBRARIES_FOLDER = "libraries_folder";
    public static final String OUTPUT_RESOURCES_FOLDER = "resources_folder";

    private static final List<String> ALL_OUTPUT_PORTS = List.of(
            DEFAULT_OUTPUT_PORT,
            OUTPUT_ID,
            OUTPUT_NAME,
            OUTPUT_DESCRIPTION,
            OUTPUT_TECHNOLOGY,
            OUTPUT_LANGUAGE,
            OUTPUT_ROOT_FOLDER,
            OUTPUT_SPECIFICATIONS_FOLDER,
            OUTPUT_MODULES_FOLDER,
            OUTPUT_LIBRARIES_FOLDER,
            OUTPUT_RESOURCES_FOLDER);

    public CommonPlatformInformation() {
        ALL_OUTPUT_PORTS.forEach(this::addOutputScalar);
    }

    @Override
    public void process() {
        String id = platformId();
        final ExtensionSpecification.Platform platform = InstalledExtensions.allInstalledPlatformsMap().get(id);
        if (platform == null) {
            ALL_OUTPUT_PORTS.forEach(s -> getScalar(s).remove());
            getScalar().setTo("Platform \"" + id + "\" not found");
        } else {
            getScalar().setTo(platform.jsonString());
            getScalar(OUTPUT_ID).setTo(platform.getId());
            getScalar(OUTPUT_NAME).setTo(platform.getName());
            getScalar(OUTPUT_DESCRIPTION).setTo(platform.getDescription());
            getScalar(OUTPUT_TECHNOLOGY).setTo(platform.getTechnology());
            getScalar(OUTPUT_LANGUAGE).setTo(platform.getLanguage());
            getScalar(OUTPUT_ROOT_FOLDER).setTo(platform.getFolders().getRoot());
            getScalar(OUTPUT_SPECIFICATIONS_FOLDER).setTo(platform.specificationsFolderOrNull());
            getScalar(OUTPUT_MODULES_FOLDER).setTo(platform.modulesFolderOrNull());
            getScalar(OUTPUT_LIBRARIES_FOLDER).setTo(platform.librariesFolderOrNull());
            getScalar(OUTPUT_RESOURCES_FOLDER).setTo(platform.resourcesFolderOrNull());
        }
    }

    protected String platformId() {
        return getPlatformId();
    }
}
