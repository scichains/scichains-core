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

package net.algart.executors.api.model;

import net.algart.executors.api.ExecutionSystemConfigurationException;
import net.algart.executors.api.SystemEnvironment;
import net.algart.external.UsedByNativeCode;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class InstalledExtensions {
    public static String CORE_PLATFORM_ID = "b58342ef-eaf4-4645-b672-d001f327293f";

    private InstalledExtensions() {
    }

    /**
     * System property {@value}
     * must contain a root path to all extensions, where they will be searched recursively
     * (by a criteria that the subdirectory contains {@value ExtensionJson#DEFAULT_EXTENSION_FILE_NAME} file).
     * Used only if {@value #EXTENSIONS_PATH_PROPERTY} property is not defined,
     * in other case it is ignored.
     */
    @UsedByNativeCode
    public static final String EXTENSIONS_ROOT_PROPERTY = "net.algart.executors.root";
    /**
     * System property {@value}
     * must contain a list of paths to all extensions, separated by the path separator
     * (usually ";").
     * May be skipped if {@value #EXTENSIONS_ROOT_PROPERTY} property is defined.
     */
    @UsedByNativeCode
    public static final String EXTENSIONS_PATH_PROPERTY = "net.algart.executors.path";

    /**
     * Boolean value of the system property "net.algart.executors.pathReplacementAllowed".
     * If it is <tt>true</tt>, {@link #EXTENSIONS_ROOT_PROPERTY} and {@link #EXTENSIONS_PATH_PROPERTY} may
     * contain substring {@value SystemEnvironment#EXECUTORS_HOME_PATTERN_STRING}, which will be automatically
     * replaced with the value of {@link SystemEnvironment#EXECUTORS_HOME_ENV_NAME} environment variable.
     * Default value is <tt>false</tt>.
     */
    public static final boolean ENABLE_REPLACEMENT_IN_EXTENSIONS_PROPERTIES = SystemEnvironment.getBooleanProperty(
            "net.algart.executors.pathReplacementAllowed", false);

    public static final String EXTENSIONS_ROOT = replaceHome(
            SystemEnvironment.getStringProperty(EXTENSIONS_ROOT_PROPERTY));
    public static final String EXTENSIONS_PATH = replaceHome(
            SystemEnvironment.getStringProperty(EXTENSIONS_PATH_PROPERTY));


    public static Collection<Path> installedExtensionsPaths() {
        try {
            if (EXTENSIONS_PATH != null) {
                final String[] split = EXTENSIONS_PATH.split("\\" + File.pathSeparator);
                return Arrays.stream(split).map(Paths::get).toList();
            }
            if (EXTENSIONS_ROOT != null) {
                return ExtensionJson.allExtensionFolders(Paths.get(EXTENSIONS_ROOT));
            }
        } catch (IOException | InvalidPathException e) {
            throw new IllegalStateException("Installed extensions root path " +
                    EXTENSIONS_ROOT + ", defined in \"" +
                    EXTENSIONS_ROOT_PROPERTY + "\" system.property, " +
                    "is problematic: scanning it leads to I/O exception " + e, e);
        }
        throw new IllegalStateException("Installed extensions paths are not defined: they should be set " +
                "either via the system property \"" + EXTENSIONS_PATH_PROPERTY + "\" (separated by \"" +
                File.pathSeparator + "\") " +
                "or via the system property \"" + EXTENSIONS_ROOT_PROPERTY
                + "\" (root folder of all extensions, which will be scanned recursively)");
    }

    public static List<ExtensionJson> allInstalledExtensions() {
        return InstalledPlatformsHolder.installedExtensions();
    }

    public static List<ExtensionJson.Platform> allInstalledPlatforms() {
        return InstalledPlatformsHolder.installedPlatforms();
    }

    public static Map<String, ExtensionJson.Platform> allInstalledPlatformsMap() {
        return InstalledPlatformsHolder.installedPlatformsMap();
    }

    public static ExtensionJson.Platform installedPlatform(String id) {
        Objects.requireNonNull(id, "Null platform ID");
        ExtensionJson.Platform platform = allInstalledPlatformsMap().get(id);
        if (platform == null) {
            throw new IllegalArgumentException("Platform with ID \"" + id + "\" is not installed");
        }
        return platform;
    }

    public static void checkDependencies(Map<String, ExtensionJson.Platform> platformMap) {
        Objects.requireNonNull(platformMap, "Null platformMap");
        if (!platformMap.isEmpty() && !platformMap.containsKey(CORE_PLATFORM_ID)) {
            // - however, empty configuration is also allowed, even without the core platform
            throw new ExecutionSystemConfigurationException("No core platform installed (ID \""
                    + CORE_PLATFORM_ID + "\")");
        }
        for (ExtensionJson.Platform platform : platformMap.values()) {
            Objects.requireNonNull(platform, "Null platform inside platformMap");
            final List<ExtensionJson.Platform.Dependency> dependencies = platform.getDependencies();
            for (ExtensionJson.Platform.Dependency dependency : dependencies) {
                if (!platformMap.containsKey(dependency.getId())) {
                    final String id = dependency.getId();
                    assert id != null;
                    final String name = dependency.getName();
                    final String description = dependency.getDescription();
                    throw new ExecutionSystemConfigurationException("Platform \"" + platform.getName() +
                            "\", ID \"" + platform.getId() + "\" depends on non-existing platform" +
                            (name == null ? "" : " \"" + name + "\"") +
                            ", ID \"" + id + "\"" +
                            (description == null ? "" : " (" + description + ")"));
                }
            }
        }
    }

    private static String replaceHome(String path) {
        return ENABLE_REPLACEMENT_IN_EXTENSIONS_PROPERTIES ?
                SystemEnvironment.replaceHomeEnvironmentVariable(path) :
                path;
    }

    private static class InstalledPlatformsHolder {
        private static List<ExtensionJson> installedExtensions = null;
        private static List<ExtensionJson.Platform> installedPlatforms = null;
        private static Map<String, ExtensionJson.Platform> installedPlatformsMap = null;

        private static synchronized List<ExtensionJson> installedExtensions() {
            load();
            return installedExtensions;
        }

        private static synchronized List<ExtensionJson.Platform> installedPlatforms() {
            load();
            return installedPlatforms;
        }

        private static synchronized Map<String, ExtensionJson.Platform> installedPlatformsMap() {
            load();
            return installedPlatformsMap;
        }

        private static void load() {
            // It is better than static initialization: this solution allows to see possible exceptions
            // (static initialization will lead to very "strange" exceptions like NoClassDefFound error,
            // because this class will stay not initialized)
            if (installedExtensions == null) {
                try {
                    final List<ExtensionJson> extensions = new ArrayList<>();
                    final List<ExtensionJson.Platform> platforms = new ArrayList<>();
                    final Map<String, ExtensionJson.Platform> platformsMap = new LinkedHashMap<>();
                    for (Path path : installedExtensionsPaths()) {
                        final ExtensionJson extension = ExtensionJson.readFromFolder(path);
                        extensions.add(extension);
                        for (ExtensionJson.Platform platform : extension.getPlatforms()) {
                            platform.setImmutable();
                            final ExtensionJson.Platform existing = platformsMap.put(platform.getId(), platform);
                            if (existing != null) {
                                throw new IOException("Invalid set of extensions: duplicate id \""
                                        + platform.getId() + "\" for platform, named \"" + platform.getName()
                                        + "\", in folder " + path
                                        + " (there is another platform with this id, named \""
                                        + existing.getName() + "\")");
                            }
                            platforms.add(platform);
                        }
                    }
                    // - exceptions possible, then installedExtensions/installedPlatforms will stay be null
                    installedExtensions = Collections.unmodifiableList(extensions);
                    installedPlatforms = Collections.unmodifiableList(platforms);
                    installedPlatformsMap = Collections.unmodifiableMap(platformsMap);
                    checkDependencies(installedPlatformsMap);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
    }
}
