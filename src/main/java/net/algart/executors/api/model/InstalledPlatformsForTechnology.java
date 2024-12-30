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

package net.algart.executors.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InstalledPlatformsForTechnology {
    private final String platformTechnology;

    private boolean ready = false;
    private List<ExtensionSpecification.Platform> installedPlatforms = null;
    private List<String> installedSpecificationFolders = null;
    private List<String> installedImplementationFolders = null;
    private final Object lock = new Object();

    private InstalledPlatformsForTechnology(String platformTechnology) {
        this.platformTechnology = Objects.requireNonNull(platformTechnology, "Null platformTechnology");
    }

    public static InstalledPlatformsForTechnology getInstance(String platformTechnology) {
        return new InstalledPlatformsForTechnology(platformTechnology);
    }

    public List<ExtensionSpecification.Platform> installedPlatforms() {
        synchronized (lock) {
            analysePlatforms();
            return installedPlatforms;
        }
    }

    public List<String> installedSpecificationFolders() {
        synchronized (lock) {
            analysePlatforms();
            return installedSpecificationFolders;
        }
    }

    public List<String> installedImplementationFolders() {
        synchronized (lock) {
            analysePlatforms();
            return installedImplementationFolders;
        }
    }

    private void analysePlatforms() {
        if (!this.ready) {
            final List<ExtensionSpecification.Platform> platforms = new ArrayList<>();
            final List<String> specificationFolders = new ArrayList<>();
            final List<String> implementationFolders = new ArrayList<>();
            for (ExtensionSpecification.Platform platform : InstalledExtensions.allInstalledPlatforms()) {
                if (platform.getTechnology().equals(platformTechnology)) {
                    platforms.add(platform);
                    if (platform.hasModels()) {
                        specificationFolders.add(platform.modelsFolder().toString());
                    }
                    if (platform.hasModules()) {
                        implementationFolders.add(platform.modulesFolder().toString());
                    }
                    if (platform.hasLibraries()) {
                        implementationFolders.add(platform.librariesFolder().toString());
                    }
                }
            }
            this.installedPlatforms = Collections.unmodifiableList(platforms);
            this.installedSpecificationFolders = Collections.unmodifiableList(specificationFolders);
            this.installedImplementationFolders = Collections.unmodifiableList(implementationFolders);
            this.ready = true;
        }
    }
}
