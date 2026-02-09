/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import jep.JepException;
import jep.python.PyCallable;
import jep.python.PyObject;

import java.util.Objects;

public class AtomicPyObject implements AutoCloseable {
    final JepSingleThreadInterpreter i;
    private final PyObject pyObject;

    AtomicPyObject(JepSingleThreadInterpreter interpreter, PyObject pyObject) {
        this.i = Objects.requireNonNull(interpreter, "Null interpreter");
        this.pyObject = Objects.requireNonNull(pyObject, "Null pyObject");
    }

    public PyObject pyObject() {
        return pyObject;
    }

    public AtomicPyObject getAtomic(String name) throws JepException {
        return i.wrapObject(getAttributeAs(name, PyObject.class));
    }

    public AtomicPyCallable getAtomicCallable(String name) throws JepException {
        return i.wrapCallable(getAttributeAs(name, PyCallable.class));
    }

    public Object getAttribute(String name) throws JepException {
        return getAttributeAs(name, Object.class);
    }

    public Object getAttributeOrNull(String name) throws JepException {
        return i.executeInSingleThread(() -> {
            try {
                return pyObject.getAttr(name);
            } catch (JepException e) {
                return null;
            }
        });
    }

    public <T> T getAttributeAs(String name, Class<T> resultClass) throws JepException {
        Objects.requireNonNull(resultClass, "Null resultClass");
        return i.executeInSingleThread(() -> pyObject.getAttr(name, resultClass));
    }

    public void setAttribute(String name, Object value) throws JepException {
        Objects.requireNonNull(name, "Null name");
        i.executeInSingleThread(() -> pyObject.setAttr(name, value));
    }

    public void removeAttribute(String name) throws JepException {
        Objects.requireNonNull(name, "Null name");
        i.executeInSingleThread(() -> pyObject.delAttr(name));
    }

    public AtomicPyObject invokeAsObject(String methodName, Object... args) {
        return i.wrapObject(invokeAs(methodName, PyObject.class, args));
    }

    public Object invoke(String methodName, Object... args) {
        return invokeAs(methodName, Object.class, args);
    }

    public <T> T invokeAs(String methodName, Class<T> resultClass, Object... args) {
        Objects.requireNonNull(methodName, "Null methodName");
        Objects.requireNonNull(resultClass, "Null resultClass");
        try (final AtomicPyCallable callable = getAtomicCallable(methodName)) {
            return callable.callAs(resultClass, args);
        }
    }

    public <T> T as(Class<T> expectedType) throws JepException {
        Objects.requireNonNull(expectedType, "Null expectedType");
        return i.executeInSingleThread(() -> pyObject.as(expectedType));
    }

    @Override
    public void close() {
        i.executeInSingleThread(pyObject::close);
    }

    @Override
    public String toString() {
        return "atomic wrapper for " + i.executeInSingleThread(pyObject::toString);
    }
}
