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

package net.algart.jep;

import jep.JepConfig;
import jep.python.PyCallable;
import jep.python.PyObject;
import net.algart.jep.additions.*;

import java.lang.System.Logger;
import java.util.Objects;

public final class JepPerformer implements AutoCloseable {
    static final Logger LOG = System.getLogger(JepPerformer.class.getName());

    final JepSingleThreadInterpreter context;
    private final JepType type;
    private final JepConfig configuration;

    private JepPerformer(JepSingleThreadInterpreter context) {
        this.context = Objects.requireNonNull(context, "Null JEP context");
        this.type = context.type();
        this.configuration = context.configuration();
    }

    public static JepPerformer newPerformer(JepSingleThreadInterpreter context) {
        return new JepPerformer(context);
    }

    public JepSingleThreadInterpreter context() {
        return context;
    }

    public JepType type() {
        return type;
    }

    /**
     * Returns JEP configuration used while creating this object.
     *
     * @return current JEP configuration; may be <code>null</code>.
     */
    public JepConfig configuration() {
        return configuration;
    }

    public Object verificationStatus() {
        JepConfig configuration = context.configuration();
        return configuration instanceof JepExtendedConfiguration extendedConfiguration ?
                extendedConfiguration.getVerificationStatus() :
                null;
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

    public Object getRawValue(String valueName) {
        Objects.requireNonNull(valueName, "Null valueName");
        return context.getValue(valueName);
    }

    public <T> T getValueAs(String valueName, Class<T> valueClass) {
        Objects.requireNonNull(valueName, "Null valueName");
        Objects.requireNonNull(valueClass, "Null valueClass");
        return context.getValue(valueName, valueClass);
    }

    public AtomicPyObject getObject(String valueName) {
        return context.wrapObject(getValueAs(valueName, PyObject.class));
    }

    public AtomicPyCallable getCallable(String valueName) {
        final PyCallable callable = type.isSubInterpreter() ?
                (PyCallable) getRawValue(valueName) :
                getValueAs(valueName, PyCallable.class);
        return context.wrapCallable(callable);
    }

    public Object invokeFunction(String functionName, Object... args) {
        Objects.requireNonNull(functionName, "Null functionName");
        return context.invoke(functionName, args);
    }

    public AtomicPyObject newObject(String className, Object... args) {
        Objects.requireNonNull(className, "Null Python class name");
        try (final AtomicPyCallable callable = getCallable(className)) {
            return type.isSubInterpreter() ?
                    callable.callRawAtomic(args) :
                    callable.callAsAtomic(args);
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

}
