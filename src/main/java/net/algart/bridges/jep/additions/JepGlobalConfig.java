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

package net.algart.bridges.jep.additions;

import jep.MainInterpreter;
import jep.PyConfig;

import java.nio.file.Files;
import java.nio.file.Paths;

public class JepGlobalConfig extends PyConfig {
    public record PythonHomeInformation(
            String pythonHome,
            boolean used,
            boolean systemEnvironmentDisabled) {

        public boolean unknown() {
            return pythonHome == null;
        }

        public boolean exists() {
            if (pythonHome == null) {
                return false;
            }
            try {
                return Files.exists(Paths.get(pythonHome));
            } catch (Exception ignoredExceptionWhileParsingPath) {
                return false;
            }
        }

        public boolean systemEnvironmentUsed() {
            if (!used) {
                return true;
            }
            if (pythonHome == null) {
                assert systemEnvironmentDisabled;
                // - in another case, we should make this record with !used
                return true;
            }
            return false;
        }
    }

    public static final String JEP_CONFIG_PROPERTY_PREFIX = "jep.config.";

    public static final JepGlobalConfig INSTANCE = new JepGlobalConfig();

    private boolean used = false;
    private final Object lock = new Object();

    private JepGlobalConfig() {
    }

    public int getNoSiteFlag() {
        synchronized (lock) {
            return noSiteFlag;
        }
    }

    @Override
    public JepGlobalConfig setNoSiteFlag(int noSiteFlag) {
        synchronized (lock) {
            super.setNoSiteFlag(noSiteFlag);
            return this;
        }
    }

    public int getNoUserSiteDirectory() {
        synchronized (lock) {
            return noUserSiteDirectory;
        }
    }

    @Override
    public JepGlobalConfig setNoUserSiteDirectory(int noUserSiteDirectory) {
        synchronized (lock) {
            super.setNoUserSiteDirectory(noUserSiteDirectory);
            return this;
        }
    }

    public int getIgnoreEnvironmentFlag() {
        synchronized (lock) {
            return ignoreEnvironmentFlag;
        }
    }

    @Override
    public JepGlobalConfig setIgnoreEnvironmentFlag(int ignoreEnvironmentFlag) {
        synchronized (lock) {
            super.setIgnoreEnvironmentFlag(ignoreEnvironmentFlag);
            return this;
        }
    }

    public int getVerboseFlag() {
        synchronized (lock) {
            return verboseFlag;
        }
    }

    @Override
    public JepGlobalConfig setVerboseFlag(int verboseFlag) {
        synchronized (lock) {
            super.setVerboseFlag(verboseFlag);
            return this;
        }
    }

    public int getOptimizeFlag() {
        synchronized (lock) {
            return optimizeFlag;
        }
    }

    @Override
    public JepGlobalConfig setOptimizeFlag(int optimizeFlag) {
        synchronized (lock) {
            super.setOptimizeFlag(optimizeFlag);
            return this;
        }
    }

    public int getDontWriteBytecodeFlag() {
        synchronized (lock) {
            return dontWriteBytecodeFlag;
        }
    }

    @Override
    public JepGlobalConfig setDontWriteBytecodeFlag(int dontWriteBytecodeFlag) {
        synchronized (lock) {
            super.setDontWriteBytecodeFlag(dontWriteBytecodeFlag);
            return this;
        }
    }

    public int getHashRandomizationFlag() {
        synchronized (lock) {
            return hashRandomizationFlag;
        }
    }

    @Override
    public JepGlobalConfig setHashRandomizationFlag(int hashRandomizationFlag) {
        synchronized (lock) {
            super.setHashRandomizationFlag(hashRandomizationFlag);
            return this;
        }
    }

    public String getPythonHome() {
        synchronized (lock) {
            return pythonHome;
        }
    }

    @Override
    public JepGlobalConfig setPythonHome(String pythonHome) {
        synchronized (lock) {
            super.setPythonHome(pythonHome);
            return this;
        }
    }

    public PythonHomeInformation pythonHomeInformation() {
        synchronized (lock) {
            if (isUsed()) {
                final String pythonHome = getPythonHome();
                boolean ignoreEnvironment = getIgnoreEnvironmentFlag() > 0;
                if (pythonHome != null || ignoreEnvironment) {
                    return new PythonHomeInformation(pythonHome, true, ignoreEnvironment);
                }
            }
            return new PythonHomeInformation(getEnv("PYTHONHOME"), false, false);
        }
    }

    public String actualPythonHome() {
        return pythonHomeInformation().pythonHome();
    }

    public JepGlobalConfig loadFromSystemProperties() {
        synchronized (lock) {
            setNoSiteFlag(getPrefixedInt("noSiteFlag", noSiteFlag));
            setNoUserSiteDirectory(getPrefixedInt("noUserSiteDirectory", noUserSiteDirectory));
            setIgnoreEnvironmentFlag(getPrefixedInt("ignoreEnvironmentFlag", ignoreEnvironmentFlag));
            setVerboseFlag(getPrefixedInt("verboseFlag", verboseFlag));
            setOptimizeFlag(getPrefixedInt("optimizeFlag", optimizeFlag));
            setDontWriteBytecodeFlag(getPrefixedInt("dontWriteBytecodeFlag", dontWriteBytecodeFlag));
            setHashRandomizationFlag(getPrefixedInt("hashRandomizationFlag", hashRandomizationFlag));
            setPythonHome(getPrefixedString("pythonHome", pythonHome));
            // - note: we preserve previous pythonHome if there is no system property
            return this;
        }
    }

    public JepGlobalConfig useForJep() {
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

    private static int getPrefixedInt(String propertyName, int defaultValue) {
        try {
            final String property = System.getProperty(JEP_CONFIG_PROPERTY_PREFIX + propertyName);
            return property == null ? defaultValue : Integer.parseInt(property);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String getPrefixedString(String propertyName, String defaultValue) {
        try {
            final String property = System.getProperty(JEP_CONFIG_PROPERTY_PREFIX + propertyName);
            return property == null ? defaultValue : property;
        } catch (Exception e) {
            // for a case of SecurityException
            return defaultValue;
        }
    }

    private static String getEnv(String envName) {
        try {
            return System.getenv(envName);
        } catch (Exception e) {
            // for a case of SecurityException
            return null;
        }
    }
}
