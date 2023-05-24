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

package net.algart.bridges.jep;

import jep.python.PyCallable;
import jep.python.PyObject;
import net.algart.bridges.jep.additions.AtomicPyCallable;
import net.algart.bridges.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.additions.JepSingleThreadInterpreter;

import java.util.Objects;
import java.lang.System.Logger;

public final class JepPerformer implements AutoCloseable {
    static final Logger LOG = System.getLogger(JepPerformer.class.getName());

    final JepSingleThreadInterpreter context;

    private JepPerformer(JepSingleThreadInterpreter context) {
        this.context = Objects.requireNonNull(context, "Null JEP context");
    }

    public static JepPerformer newPerformer(JepSingleThreadInterpreter context) {
        return new JepPerformer(context);
    }

    public JepSingleThreadInterpreter context() {
        return context;
    }

    public void perform(String code) {
        Objects.requireNonNull(code, "Null code");
        context.exec(code);
    }

    public void performFile(String path) {
        Objects.requireNonNull(path, "Null path");
        context.runScript(path);
    }

    public void putValue(String valueName, Object value) {
        Objects.requireNonNull(valueName, "Null valueName");
        context.set(valueName, value);
    }

    public <T> T getValueAs(String valueName, Class<T> valueClass) {
        Objects.requireNonNull(valueName, "Null valueName");
        Objects.requireNonNull(valueClass, "Null valueClass");
        return context.getValue(valueName, valueClass);
    }

    public AtomicPyCallable getCallable(String valueName) {
        return context.wrapCallable(getValueAs(valueName, PyCallable.class));
    }

    public Object invokeFunction(String functionName, Object... args) {
        Objects.requireNonNull(functionName, "Null functionName");
        return context.invoke(functionName, args);
    }

    public AtomicPyObject newObject(String className, Object... args) {
        Objects.requireNonNull(className, "Null Python class name");
        try (final AtomicPyCallable callable = getCallable(className)) {
            return callable.callAsObject(args);
        }
    }

    public AtomicPyObject wrapObject(PyObject pyObject) {
        return context.wrapObject(pyObject);
    }

    public AtomicPyCallable wrapCallable(PyCallable pyCallable) {
        return context.wrapCallable(pyCallable);
    }

    @Override
    public void close() {
        context.close();
    }

    @Override
    public String toString() {
        return "Python performer of " + context;
    }

    public static String importCode(String from, String what) {
        Objects.requireNonNull(from, "Null from");
        Objects.requireNonNull(what, "Null what");
        return "from " + from + " import " + what + "\n";
    }
}
