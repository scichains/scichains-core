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

import jep.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JepCreationTools {
    private static final AtomicBoolean SHARED_CREATED = new AtomicBoolean(false);
    private static final Pattern IMPORT_MATCHER = Pattern.compile("^import\\s+(\\w+)");

    static SubInterpreter newSubInterpreter(JepConfig config) {
        Objects.requireNonNull(config, "Null config");
        final SubInterpreter result = doCreate(() -> new SubInterpreter(config));
        performStartupCodeForExtended(result, config, JepInterpreterKind.LOCAL);
        return result;
    }

    static SharedInterpreter newSharedInterpreter(JepConfig config) {
        Objects.requireNonNull(config, "Null config");
        if (!SHARED_CREATED.getAndSet(true)) {
            SharedInterpreter.setConfig(config);
        }
        final SharedInterpreter result = doCreate(SharedInterpreter::new);
        performStartupCodeForExtended(result, config, JepInterpreterKind.SHARED);
        return result;
    }

    private static <T extends Interpreter> T doCreate(Supplier<T> constructor) {
        try {
            return constructor.get();
        } catch (UnsatisfiedLinkError | JepException e) {
            throw new JepException("Cannot load JEP (Java Embedded Python) due to " +
                    e.getClass().getSimpleName() +
                    "; probably " + unsatisfiedLinkDiagnostics(), e);
        }
    }

    private static String unsatisfiedLinkDiagnostics() {
        final var homeInformation = JepGlobalConfig.INSTANCE.pythonHomeInformation();
        assert homeInformation != null;
        if (homeInformation.unknown()) {
            return homeInformation.systemEnvironmentDisabled() ?
                    "usage of PYTHONHOME environment variable is disabled, " +
                            "and Python home directory is not set properly" :
                    "Python is not installed or PYTHONHOME environment variable is not set to Python home directory";
        }
        final String messageHome = "\"" + homeInformation.pythonHome() + "\" " +
                (homeInformation.systemEnvironmentUsed() ?
                        "(value of PYTHONHOME environment variable) " :
                        "");
        if (homeInformation.exists()) {
            return "Python is not correctly installed at " + messageHome +
                    "or JEP module is not available; JEP should be installed by commands: " +
                    "\"pip install numpy\" (MUST be installed before JEP) and then \"pip install jep\"";
        } else {
            return "Python home " + messageHome + "is not an existing Python directory";
        }
    }

    private static void performStartupCodeForExtended(
            Interpreter jepInterpreter,
            JepConfig config,
            JepInterpreterKind kind) {
        Objects.requireNonNull(jepInterpreter, "Null jepInterpreter");
        if (config instanceof final JepExtendedConfig extended) {
            final List<String> startupCode = extended.getStartupCode();
            for (String codeSnippet : startupCode) {
                try {
                    JepSingleThreadInterpreter.LOG.log(System.Logger.Level.TRACE,
                            () -> "Executing " + kind.kindName() +
                                    " JEP start-up code:\n" + String.join("", startupCode));
                    jepInterpreter.exec(codeSnippet);
                } catch (JepException e) {
                    if (e.getMessage() != null && e.getMessage().contains("nterpreter change detected")) {
                        // - it seems to be another problem inside numpy
                        throw e;
                    }
                    throw new JepException("cannot execute start-up Python code: \"" + codeSnippet.trim() +
                            "\"; probably necessary Python package" + importedPackage(codeSnippet) +
                            " is not installed (Python message: " + e.getMessage() + ")", e);
                }
            }
            if (extended.hasVerifier()) {
                extended.getVerifier().verify(jepInterpreter, config);
            }
        }
    }

    private static String importedPackage(String code) {
        final Matcher matcher = IMPORT_MATCHER.matcher(code.trim());
        return matcher.find() ? " \"" + matcher.group(1) + "\"" : "";
    }
}
