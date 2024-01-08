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

package net.algart.bridges.standard;

import javax.script.*;
import java.util.Objects;
import java.lang.System.Logger;

public final class JavaScriptPerformer {
    private static final boolean OPTIMIZE_SEVERAL_COMPILATION_OF_SAME_FORMULA = true;
    static final Logger LOG = System.getLogger(JavaScriptPerformer.class.getName());

    private final String script;
    private final CompiledScript compiledScript;
    private final ScriptEngine context;

    private JavaScriptPerformer(String script, ScriptEngine context) {
        this.script = Objects.requireNonNull(script, "Null script");
        this.context = context == null ? ScriptEngineTools.newEngine() : context;
        try {
            if (this.context instanceof Compilable compilable) {
                this.compiledScript = compilable.compile(script);
            } else {
                this.compiledScript = null;
            }
        } catch (ScriptException e) {
            throw JavaScriptException.wrap(e, script);
        }
    }

    public static JavaScriptPerformer newInstance(String script) {
        return new JavaScriptPerformer(script, null);
    }

    public static JavaScriptPerformer newInstance(String script, ScriptEngine context) {
        return new JavaScriptPerformer(script, context);
    }

    public static JavaScriptPerformer newInstanceIfChanged(String script, JavaScriptPerformer previousInstance) {
        return newInstanceIfChanged(script, previousInstance, null);
    }

    public static JavaScriptPerformer newInstanceIfChanged(
            String script,
            JavaScriptPerformer previousInstance,
            ScriptEngine context) {
        Objects.requireNonNull(script, "Null script");
        return OPTIMIZE_SEVERAL_COMPILATION_OF_SAME_FORMULA
                && previousInstance != null && previousInstance.script.equals(script)
                && (context == null || context == previousInstance.context) ?
                previousInstance :
                new JavaScriptPerformer(script, context);
    }

    public String script() {
        return script;
    }

    public ScriptEngine context() {
        return context;
    }

    public void putVariable(String variableName, Object value) {
        context.put(variableName, value);
    }

    public boolean compilable() {
        return compiledScript != null;
    }

    public Object perform() {
        try {
            return compiledScript == null ?
                    context.eval(script) :
                    compiledScript.eval();
        } catch (ScriptException e) {
            throw JavaScriptException.wrap(e, script);
        }
    }

    public boolean calculateBoolean() {
        final Object result = perform();
        if (result == null || result.equals("")) {
            return false;
        }
        return Boolean.parseBoolean(result.toString());
    }

    public double calculateDouble() {
        final Object result = perform();
        if (result == null) {
            return Double.NaN;
        }
        return Double.parseDouble(result.toString());
    }

    public String calculateStringOrNumber() {
        Object result = perform();
        if (result == null) {
            return null;
        }
        if (result instanceof Double || result instanceof Float) {
            final double v = ((Number) result).doubleValue();
            if (v == (int) v) {
                result = (int) v;
            }
        }
        return result.toString();
    }

    public boolean isInterfaceSupported() {
        return context instanceof Invocable;
    }

    public <T> T getInterface(Class<T> clazz, String javaScriptFunction) {
        try {
            context.eval(javaScriptFunction);
        } catch (ScriptException e) {
            throw JavaScriptException.wrap(e, javaScriptFunction);
        }
        final T result = ((Invocable) context).getInterface(clazz);
        if (result == null) {
            throw new IllegalArgumentException("JavaScript code \""
                    + javaScriptFunction + "\" does not implement interface " + clazz.getName());
        }
        return result;
    }
}
