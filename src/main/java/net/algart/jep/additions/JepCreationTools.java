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

import jep.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class JepCreationTools {
    private static final AtomicBoolean SUB_INTERPRETER_CREATED = new AtomicBoolean(false);
    private static final AtomicBoolean SHARED_INTERPRETER_CREATED = new AtomicBoolean(false);
    private static final Pattern IMPORT_MATCHER = Pattern.compile("^import\\s+(\\w+)");

    private static final System.Logger LOG = System.getLogger(JepCreationTools.class.getName());

    static SubInterpreter newSubInterpreter(JepConfig configuration, JepInterpreterKind kind) {
        Objects.requireNonNull(configuration, "Null configuration");
        Objects.requireNonNull(kind, "Null kind");
        final SubInterpreter result = doCreate(() -> new SubInterpreter(configuration));
        SUB_INTERPRETER_CREATED.set(true);
        performStartupCodeForExtended(result, configuration, kind);
        return result;
    }

    static SharedInterpreter newSharedInterpreter(JepConfig configuration, JepInterpreterKind kind) {
        Objects.requireNonNull(configuration, "Null configuration");
        Objects.requireNonNull(kind, "Null kind");
        if (!SHARED_INTERPRETER_CREATED.getAndSet(true)) {
            SharedInterpreter.setConfig(configuration);
        }
        final SharedInterpreter result = doCreate(SharedInterpreter::new);
        performStartupCodeForExtended(result, configuration, kind);
        return result;
    }

    static boolean wasSubInterpreterCreated() {
        return SUB_INTERPRETER_CREATED.get();
    }

    private static <T extends Interpreter> T doCreate(Supplier<T> constructor) {
        try {
            return constructor.get();
        } catch (UnsatisfiedLinkError | JepException e) {
            throw new JepException("Cannot find Python: \"jep\" module (Java Embedded Python) " +
                    "is not properly loaded (" +
                    e.getClass().getSimpleName() +
                    ").\nProbably " + unsatisfiedLinkDiagnostics(), e);
        }
    }

    private static String unsatisfiedLinkDiagnostics() {
        final var homeInformation = GlobalPythonConfiguration.INSTANCE.pythonHomeInformation();
        assert homeInformation != null;
        if (homeInformation.unknown()) {
            return homeInformation.systemEnvironmentDisabled() ?
                    "usage of PYTHONHOME environment variable is disabled, " +
                            "and Python home directory is not set properly" :
                    "Python is not installed, or the path to your Python installation " +
                            "is not specified properly\n" +
                            "(for example via the PYTHONHOME system environment variable)";
        }
        final String messageHome = "\"" + homeInformation.pythonHome() + "\" " +
                (homeInformation.systemEnvironmentUsed() ?
                        "(value of PYTHONHOME environment variable) " :
                        "");
        if (homeInformation.exists()) {
            return "Python is not correctly installed at " + messageHome +
                    "or \"jep\" module is not properly installed.\n" + GlobalPythonConfiguration.JEP_INSTALLATION_HINTS;
        } else {
            return "Python home " + messageHome + "is not an existing Python directory";
        }
    }

    private static void performStartupCodeForExtended(
            Interpreter jepInterpreter,
            JepConfig configuration,
            JepInterpreterKind kind) {
        Objects.requireNonNull(jepInterpreter, "Null jepInterpreter");
        if (configuration instanceof final JepExtendedConfiguration extendedConfiguration) {
            final List<String> startupCode = extendedConfiguration.getStartupCode();
            LOG.log(System.Logger.Level.DEBUG,
                    () -> "Executing JEP start-up code for %s Python interpreter in a thread \"%s\":%n%s".formatted(
                            kind,
                            Thread.currentThread().getName(),
                            startupCode.stream().map(s -> "<<" + s + ">>")
                                    .collect(Collectors.joining("\n"))));
            // - note: even for GLOBAL kind, this code is executed many times - before creating SharedInterpreter
            for (String codeSnippet : startupCode) {
                assert codeSnippet != null : "setStartupCode did not check null elements";
                final boolean probablyNumpy = codeSnippet.contains("numpy");
                if (kind == JepInterpreterKind.SUB_INTERPRETER && probablyNumpy) {
                    throw new JepException("cannot execute startup Python code: \"" + codeSnippet.trim() +
                            "\", because it works with NumPy, which is strictly forbidden " +
                            "for Python sub-interpreters (interpreter kind " + kind +
                            ") and may crash the entire application");
                }
                try {
                    jepInterpreter.exec(codeSnippet);
                } catch (JepException e) {
                    if (e.getMessage() != null && e.getMessage().contains("nterpreter change detected")) {
                        // - it seems to be another problem inside numpy
                        throw e;
                    }
                    throw new JepException("cannot execute startup Python code: \"" + codeSnippet.trim() +
                            "\".\nThe necessary Python package" + importedPackage(codeSnippet) +
                            " was probably not installed correctly in Python" +
                            (probablyNumpy && wasSubInterpreterCreated() ?
                                    ",\nor you already used JEP SubInterpreter, which is completely " +
                                            "incompatible with NumPy even if was used once." :
                                    ".\n(Python error message: " + e.getMessage() + ").\n" +
                                            GlobalPythonConfiguration.JEP_INSTALLATION_HINTS), e);
                }
            }
            if (extendedConfiguration.hasVerifier()) {
                Object status = extendedConfiguration.getVerifier().verify(jepInterpreter, extendedConfiguration);
                extendedConfiguration.setVerificationStatus(status);
            }
        }
    }

    private static String importedPackage(String code) {
        final Matcher matcher = IMPORT_MATCHER.matcher(code.trim());
        return matcher.find() ? " \"" + matcher.group(1) + "\"" : "";
    }
}
