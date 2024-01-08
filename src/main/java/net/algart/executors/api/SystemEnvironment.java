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

package net.algart.executors.api;

public class SystemEnvironment {
    private SystemEnvironment() {
    }

    /**
     * Name of environment variable (optional), that may refer to the home directory of the full product.
     * Should <b>not</b> be used in the executors or execution system, excepting
     * {@link net.algart.executors.api.model.InstalledExtensions#EXTENSIONS_PATH_PROPERTY} and
     * {@link net.algart.executors.api.model.InstalledExtensions#EXTENSIONS_ROOT_PROPERTY}.
     * But it may be used, for example, in some tests.
     */
    public static final String EXECUTORS_HOME_ENV_NAME = "EXECUTORS_HOME";
    /**
     * Value of the environment variable {@link #EXECUTORS_HOME_ENV_NAME} or <tt>null</tt> if it is not specified.
     */
    public static final String EXECUTORS_HOME;
    static {
        String executorsHome = null;
        try {
            executorsHome = System.getenv(EXECUTORS_HOME_ENV_NAME);
        } catch (Exception ignored) {
            // for a case of SecurityException
        }
        EXECUTORS_HOME = executorsHome;
    }

    /**
     * This string {@value} is replaced with {@link #EXECUTORS_HOME} by
     * {@link #replaceHomeEnvironmentVariable(String)} function.
     */
    public static final String EXECUTORS_HOME_PATTERN_STRING = "${home}";

    public static String replaceHomeEnvironmentVariable(String path) {
        if (path == null) {
            return null;
        }
        if (path.contains(EXECUTORS_HOME_PATTERN_STRING)) {
            if (EXECUTORS_HOME == null) {
                throw new IllegalStateException("Path \"" + path + "\" contains \"" + EXECUTORS_HOME_PATTERN_STRING +
                        "\", but the environment variable " + EXECUTORS_HOME_ENV_NAME + " is not set");
            }
            return path.replace(EXECUTORS_HOME_PATTERN_STRING, EXECUTORS_HOME);
        } else {
            return path;
        }
    }

    public static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        try {
            if (defaultValue)
                return !"false".equalsIgnoreCase(System.getProperty(propertyName));
            else {
                return Boolean.getBoolean(propertyName);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getStringProperty(String propertyName) {
        try {
            return System.getProperty(propertyName);
        } catch (Exception e) {
            // for a case of SecurityException
            return null;
        }
    }

    public static String getStringProperty(String propertyName, String defaultValue) {
        final String result = getStringProperty(propertyName);
        return result != null ? result : defaultValue;
    }
}
