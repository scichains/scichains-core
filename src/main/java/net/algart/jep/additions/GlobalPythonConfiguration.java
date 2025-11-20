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

package net.algart.jep.additions;

import jep.MainInterpreter;
import jep.PyConfig;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class GlobalPythonConfiguration extends PyConfig {
    public record PythonHome(
            String home,
            boolean used,
            boolean systemEnvironmentDisabled) {

        public boolean unknown() {
            return home == null;
        }

        public boolean exists() {
            if (home == null) {
                return false;
            }
            try {
                return Files.exists(Paths.get(home));
            } catch (Exception ignoredExceptionWhileParsingPath) {
                return false;
            }
        }

        public boolean systemEnvironmentUsed() {
            if (!used) {
                return true;
            }
            if (home == null) {
                assert systemEnvironmentDisabled;
                // - in another case, we should make this record with !used
                return true;
            }
            return false;
        }
    }

    public static final String JEP_CONFIG_PROPERTY_PREFIX = "jep.config.";

    public static final GlobalPythonConfiguration INSTANCE = new GlobalPythonConfiguration(false);

    private boolean used = false;
    private final Object lock = new Object();

    public GlobalPythonConfiguration(boolean isolated) {
        super(isolated);
    }

    public String getHome() {
        synchronized (lock) {
            return home;
        }
    }

    @Override
    public GlobalPythonConfiguration setHome(String home) {
        synchronized (lock) {
            super.setHome(home);
            return this;
        }
    }

    public int getOptimizationLevel() {
        synchronized (lock) {
            return optimizationLevel;
        }
    }

    @Override
    public GlobalPythonConfiguration setOptimizationLevel(int optimizationLevel) {
        synchronized (lock) {
            super.setOptimizationLevel(optimizationLevel);
            return this;
        }
    }

    public Boolean getSiteImport() {
        synchronized (lock) {
            return siteImport < 0 ? null : siteImport > 0;
        }
    }

    @Override
    public GlobalPythonConfiguration setSiteImport(boolean siteImport) {
        synchronized (lock) {
            super.setSiteImport(siteImport);
            return this;
        }
    }

    public boolean isIgnoreEnvironment() {
        synchronized (lock) {
            return useEnvironment == 0;
        }
    }

    public Boolean getUseEnvironment() {
        synchronized (lock) {
            return useEnvironment < 0 ? null : useEnvironment > 0;
        }
    }

    @Override
    public GlobalPythonConfiguration setUseEnvironment(boolean useEnvironment) {
        synchronized (lock) {
            super.setUseEnvironment(useEnvironment);
            return this;
        }
    }

    public Boolean getUserSiteDirectory() {
        synchronized (lock) {
            return userSiteDirectory < 0 ? null : userSiteDirectory > 0;
        }
    }

    @Override
    public GlobalPythonConfiguration setUserSiteDirectory(boolean userSiteDirectory) {
        synchronized (lock) {
            super.setUserSiteDirectory(userSiteDirectory);
            return this;
        }
    }

    public int getVerbose() {
        synchronized (lock) {
            return verbose;
        }
    }

    @Override
    public GlobalPythonConfiguration setVerbose(int verbose) {
        synchronized (lock) {
            super.setVerbose(verbose);
            return this;
        }
    }

    public Boolean getWriteBytecode() {
        synchronized (lock) {
            return writeBytecode < 0 ? null : writeBytecode > 0;
        }
    }

    @Override
    public GlobalPythonConfiguration setWriteBytecode(boolean writeBytecode) {
        synchronized (lock) {
            super.setWriteBytecode(writeBytecode);
            return this;
        }
    }

    public PythonHome pythonHome() {
        synchronized (lock) {
            if (isUsed()) {
                final String pythonHome = getHome();
                final boolean ignoreEnvironment = isIgnoreEnvironment();
                if (pythonHome != null || ignoreEnvironment) {
                    return new PythonHome(pythonHome, true, ignoreEnvironment);
                }
            }
            // - This is based on the following fragment of JEP source code (LibraryLocator class):
            // private LibraryLocator(PyConfig pyConfig) {
            //     String pythonHome;
            //     if (pyConfig != null) {
            //         ignoreEnv = pyConfig.useEnvironment == 0;
            //         noSite = pyConfig.siteImport == 0;
            //         noUserSite = pyConfig.userSiteDirectory == 0;
            //         pythonHome = pyConfig.home;
            //     } else {
            //         ignoreEnv = false;
            //         noSite = false;
            //         noUserSite = false;
            //         pythonHome = null;
            //     }
            //     if (pythonHome == null && !ignoreEnv) {
            //         pythonHome = System.getenv("PYTHONHOME");
            //         if (pythonHome == null) {
            //             pythonHome = System.getenv("VIRTUAL_ENV");
            //         }
            //     }
            //     ...
            return new PythonHome(getEnv("PYTHONHOME"), false, false);
        }
    }

    public String pythonHomeDirectory() {
        return pythonHome().home();
    }

    public GlobalPythonConfiguration loadFromSystemProperties() {
        synchronized (lock) {
            setPrefixedInt("optimizationLevel", this::setOptimizationLevel);
            setPrefixedBoolean("siteImport", this::setSiteImport);
            setPrefixedBoolean("useEnvironment", this::setUseEnvironment);
            setPrefixedBoolean("userSiteDirectory", this::setUserSiteDirectory);
            setPrefixedInt("verbose", this::setVerbose);
            setPrefixedBoolean("writeBytecode", this::setWriteBytecode);
            setHome(getNonEmptyString("python.home", home));
            // - note: we preserve the previous this.pythonHome if there is no system property or if it is ""
            return this;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public GlobalPythonConfiguration useForJep() {
        synchronized (lock) {
            MainInterpreter.setInitParams(this);
            this.used = true;
            return this;
        }
    }

    public boolean isUsed() {
        synchronized (lock) {
            return used;
        }
    }

    private void setPrefixedInt(String propertyName, Consumer<Integer> setter) {
        Integer value = getPrefixedInt(propertyName);
        if (value != null) {
            setter.accept(value);
        }
    }

    private static Integer getPrefixedInt(String propertyName) {
        try {
            final String property = System.getProperty(JEP_CONFIG_PROPERTY_PREFIX + propertyName);
            return property == null ? null : Integer.parseInt(property);
        } catch (Exception ex) {
            return null;
        }
    }

    private void setPrefixedBoolean(String propertyName, Consumer<Boolean> setter) {
        Boolean value = getPrefixedBoolean(propertyName);
        if (value != null) {
            setter.accept(value);
        }
    }

    private static Boolean getPrefixedBoolean(String propertyName) {
        try {
            final String property = System.getProperty(JEP_CONFIG_PROPERTY_PREFIX + propertyName);
            return property == null ? null : "true".equalsIgnoreCase(property);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String getNonEmptyString(String propertyName, String defaultValue) {
        try {
            final String property = System.getProperty(propertyName);
            return property == null || property.isEmpty() ? defaultValue : property;
        } catch (Exception e) {
            // for a case of SecurityException
            return defaultValue;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String getEnv(String envName) {
        try {
            return System.getenv(envName);
        } catch (Exception e) {
            // for a case of SecurityException
            return null;
        }
    }
}
