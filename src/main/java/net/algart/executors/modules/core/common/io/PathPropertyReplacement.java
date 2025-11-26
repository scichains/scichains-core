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

package net.algart.executors.modules.core.common.io;

import net.algart.executors.api.Executor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathPropertyReplacement {
    public static final String TMP_DIR_PREFIX = "%TEMP%";

    private static final Pattern PROPERTY_NAME_PATTERN = Pattern.compile("\\$\\{([%\\w\\.\\,\\-]+)\\}");
    private static final Pattern PROBABLE_PROPERTY_NAME_PATTERN = Pattern.compile("(%|\\$\\{|\\$\\$)");
    private static final String TMP_DIR_PREFIX_COMMON_START = "%TEMP";
    private static final String[] TMP_DIR_PREFIXES = {
            TMP_DIR_PREFIX + "/", TMP_DIR_PREFIX + File.separator, TMP_DIR_PREFIX
    };

    public enum Property {
        FILE_PATH("path.name", path -> removeExtension(path.toAbsolutePath()), Executor::contextPath),
        FILE_PATH_EXT("path.name.ext", Path::toAbsolutePath, Executor::contextPath),
        FILE_NAME("file.name", path -> removeExtension(path.getFileName()), Executor::contextPath),
        FILE_NAME_EXT("file.name.ext", Path::getFileName, Executor::contextPath),
        RESOURCE_FOLDER("resources", Path::toAbsolutePath, Executor::executorResourceFolder);

        private static final Map<String, Property> ALL_PROPERTIES = Stream.of(values()).collect(
                Collectors.toMap(Property::propertyName, e -> e));

        private final String propertyName;
        private final Function<Path, Path> replacePath;
        private final Function<Executor, Path> getPath;

        Property(String propertyName, Function<Path, Path> replacePath, Function<Executor, Path> getPath) {
            this.propertyName = Objects.requireNonNull(propertyName);
            this.replacePath = replacePath;
            this.getPath = getPath;
        }

        public String propertyName() {
            return propertyName;
        }

        /**
         * Returns an {@link Optional} containing the {@link Property} with the given {@link #propertyName()}.
         * <p>If no property with the specified name exists or if the argument is {@code null},
         * an empty optional is returned.
         *
         * @param propertyName the property name; may be {@code null}.
         * @return an optional property.
         */
        public static Optional<Property> fromPropertyName(String propertyName) {
            return Optional.ofNullable(ALL_PROPERTIES.get(propertyName));
        }

        String replacement(Path path, Executor executor, String stringForException) {
            if (executor != null) {
                path = getPath.apply(executor);
            }
            return replacement(path, stringForException);
        }

        private String replacement(Path path, String stringForException) {
            if (path == null) {
                throw new IllegalArgumentException("String \"" + stringForException
                        + "\" contains special path property \""
                        + propertyName + "\" and cannot be resolved, because "
                        + "there is no any path for replacement (it is null)");
            }
            final Path result = replacePath.apply(path);
            if (result == null) {
                throw new IllegalArgumentException("Cannot find required replacement for property \"" + propertyName
                        + "\" in the path \"" + path + "\"; probably this path is empty");
            }
            return result.toString();
        }
    }

    private PathPropertyReplacement() {
    }

    public static boolean hasProperties(String s) {
        Objects.requireNonNull(s, "Null string");
        return PROPERTY_NAME_PATTERN.matcher(s).find();
    }

    /**
     * Whether the given string contains probable properties, that will be probably detected in future versions
     * or in other system. Returns <code>true</code> if the string contains "%" character or "${" combination.
     *
     * @param s some string.
     * @return <code>true</code> if this string, probably, contain a property or something like this.
     */
    public static boolean hasProbableProperties(String s) {
        Objects.requireNonNull(s, "Null string");
        return PROBABLE_PROPERTY_NAME_PATTERN.matcher(s).find();
    }

    public static void checkProbableProperties(String path) {
        Objects.requireNonNull(path, "Null path");
        if (hasProbableProperties(path)) {
            throw new SecurityException("Path probably contains some properties"
                    + " for possible replacements, it is dangerous and prohibited: \""
                    + path + "\" (it contains suspicious characters/substrings like \"%\" or \"${\")");
        }
    }

    public static void checkAbsolute(Path path) {
        Objects.requireNonNull(path, "Null path");
        if (!path.isAbsolute()) {
            throw new SecurityException("Path must be absolute, but it is not: \"" + path + "\"");
        }
    }

    public static Optional<Property> firstPathProperty(String s) {
        final Matcher matcher = PROPERTY_NAME_PATTERN.matcher(s.trim());
        while (matcher.find()) {
            final String propertyName = matcher.group(1);
            final Optional<Property> property = Property.fromPropertyName(propertyName);
            if (property.isPresent()) {
                return property;
            }
        }
        return Optional.empty();
    }

    public static String translatePathProperties(String s, Executor executor) {
        return translatePathProperties(s, null, executor);
    }

    public static String translatePathProperties(String s, Path path) {
        return translatePathProperties(s, path, null);
    }

    public static String translateTmpDir(String s) {
        Objects.requireNonNull(s, "Null string");
        if (s.startsWith(TMP_DIR_PREFIX_COMMON_START)) {
            for (String prefix : TMP_DIR_PREFIXES) {
                if (s.startsWith(prefix)) {
                    final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
                    // - note: it is correct in both cases "C:\TMP" and "C:\TMP\"
                    return tmpDir.resolve(s.substring(prefix.length())).toString();
                }
            }
            throw new IllegalArgumentException("Path \"" + s + "\" starts with " + TMP_DIR_PREFIX_COMMON_START
                    + ", but this keyword is not finished properly: one of prefixes "
                    + String.join(", ", TMP_DIR_PREFIXES) + " expected");
        }
        return s;
    }

    public static String translateSystemProperties(String s) {
        Objects.requireNonNull(s, "Null string");
        s = s.trim();
        final Matcher matcher = PROPERTY_NAME_PATTERN.matcher(s);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String propertyName = matcher.group(1);
            /* - The following code is obsolete: it increases risk of unsecure operation and actually does not help
            String property;
            if (propertyName.startsWith("%") && propertyName.endsWith("%")) {
                if (propertyName.length() < 3) {
                    throw new IllegalArgumentException("Invalid system environment variable name \""
                            + propertyName + "\": in must contain at least 1 character between % and %");
                }
                propertyName = propertyName.substring(1, propertyName.length() - 1);
                property = System.getenv(propertyName);
                if (property == null) {
                    throw new IllegalArgumentException("Non-existing environment variable \"" + propertyName
                            + "\" in the string \"" + s + "\"");
                }
            } else {
            */
            String property = System.getProperty(propertyName);
            if (property == null) {
                throw new IllegalArgumentException("Non-existing property \"" + propertyName
                        + "\" in the string \"" + s + "\"");
            }
            property = correctDirSystemProperty(propertyName, property);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(property));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // Work with String, not Path: allows to process, for example, several paths separated by ";"
    public static String translateProperties(String s, Path path) {
        s = translatePathProperties(s, path);
        s = translateSystemProperties(s);
        s = translateTmpDir(s);
        return s;
    }

    public static Path translatePropertiesAndCurrentDirectory(String s, Executor executor) {
        Objects.requireNonNull(executor, "Null executor");
        s = translatePathProperties(s, executor);
        s = translateSystemProperties(s);
        s = translateTmpDir(s);
        return executor.translateCurrentDirectory(Paths.get(s));
    }

    private static Path removeExtension(Path path) {
        if (path == null) {
            return null;
        }
        final String fileName = path.toString();
        final int p = fileName.lastIndexOf('.');
        return p == -1 ? path : Paths.get(fileName.substring(0, p));
    }

    private static String translatePathProperties(String s, Path path, Executor executor) {
        Objects.requireNonNull(s, "Null string");
        final Matcher matcher = PROPERTY_NAME_PATTERN.matcher(s.trim());
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String propertyName = matcher.group(1);
            final Optional<Property> property = Property.fromPropertyName(propertyName);
            final String propertyValue;
            propertyValue = property
                    .map(value -> value.replacement(path, executor, s))
                    .orElseGet(matcher::group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(propertyValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String correctDirSystemProperty(String propertyName, String property) {
        if (propertyName.equals("java.io.tmpdir")
                || propertyName.equals("user.home")
                || propertyName.equals("user.dir")) {
            // Some of these properties adds / to the end, some do not this: it is system-dependent.
            // For example, on my Windows, user.home does not contain final slash, but java.io.tmpdir contains it.
            if (!property.endsWith("/") && !property.endsWith(File.separator)) {
                return property + File.separator;
            }
        }
        return property;
    }
}
