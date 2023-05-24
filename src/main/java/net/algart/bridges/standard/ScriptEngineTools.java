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

package net.algart.bridges.standard;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

// This class does not depend on Graal directly, but customizes ScriptEngine according Graal requirements
class ScriptEngineTools {
    private static final String DEFAULT_ENGINE_NAME = getStringProperty(
            "net.algart.bridges.standard", "javascript");
    // - we don't use "graal.js" by default: "javascript" is more flexible

    private static final Set<String> SAFE_CLASSES = new HashSet<>(Arrays.asList(
            String.class.getCanonicalName(),
            Locale.class.getCanonicalName(),
            Float.class.getCanonicalName(),
            Double.class.getCanonicalName(),
            // - but not Integer/Long: they have "getInteger"/"getLong" methods,
            // allowing to read some system properties (it is not secure operation)
            Math.class.getCanonicalName(),
            StrictMath.class.getCanonicalName(),
            Arrays.class.getCanonicalName(),
            char.class.getCanonicalName(),
            boolean.class.getCanonicalName(),
            byte.class.getCanonicalName(),
            short.class.getCanonicalName(),
            int.class.getCanonicalName(),
            long.class.getCanonicalName(),
            float.class.getCanonicalName(),
            double.class.getCanonicalName(),
            char[].class.getCanonicalName(),
            boolean[].class.getCanonicalName(),
            byte[].class.getCanonicalName(),
            short[].class.getCanonicalName(),
            int[].class.getCanonicalName(),
            long[].class.getCanonicalName(),
            float[].class.getCanonicalName(),
            double[].class.getCanonicalName()
            // - previous types are necessary for creating primitive Java arrays from JavaScript,
            // like in the following code:
            //      var IntsC = Java.type("int[]");
            //      var ja = new IntsC(100);
    ));

    static ScriptEngine newEngine() {
        // Actually this function is equivalent to doNewEngine(), but outputs a log message.
        final ScriptEngine engine = doNewEngine();
        JavaScriptPerformer.LOG.log(System.Logger.Level.DEBUG,
                "Creating new local JavaScript engine: " + engine.getClass());
        return engine;
    }

    static ScriptEngine doNewEngine() {
        final ScriptEngine engine = new ScriptEngineManager(JavaScriptPerformer.class.getClassLoader())
                .getEngineByName(DEFAULT_ENGINE_NAME);
        // - without explicit argument JavaScriptContextContainer.class.getClassLoader(), we will have a problem
        // in ScriptEngineManager: it will use getContextClassLoader, but it can be null while calling from JNI
        if (engine == null) {
            throw new UnsupportedOperationException("Default script engine \""
                    + DEFAULT_ENGINE_NAME + "\" is not supported");
        }
        final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) SAFE_CLASSES::contains);
        // - allows access to several safe classes only (graal,js)
        engine.put("LOGGER", JavaScriptPerformer.LOG);
        return engine;
    }

    private static String getStringProperty(String propertyName) {
        try {
            return System.getProperty(propertyName);
        } catch (Exception e) {
            // for a case of SecurityException
            return null;
        }
    }

    private static String getStringProperty(String propertyName, String defaultValue) {
        final String result = getStringProperty(propertyName);
        return result != null ? result : defaultValue;
    }

}
