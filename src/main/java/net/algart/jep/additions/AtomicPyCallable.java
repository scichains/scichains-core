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

import jep.JepException;
import jep.python.PyCallable;
import jep.python.PyObject;

import java.util.Map;
import java.util.Objects;

public class AtomicPyCallable extends AtomicPyObject {
    private final PyCallable pyCallable;

    AtomicPyCallable(JepSingleThreadInterpreter interpreter, PyCallable pyCallable) {
        super(interpreter, Objects.requireNonNull(pyCallable, "Null pyCallable"));
        this.pyCallable = pyCallable;
    }

    public AtomicPyObject callAsObject(Object... args) throws JepException {
        return i.wrapObject(callAs(PyObject.class, args));
    }

    public Object callRaw(Object... args) throws JepException {
        return i.executeInSingleThread(() -> pyCallable.call(args));
    }

    public <T> T callAs(Class<T> expectedType, Object... args) throws JepException {
        Objects.requireNonNull(expectedType, "Null expectedType");
        return i.executeInSingleThread(() -> pyCallable.callAs(expectedType, args));
    }

    public <T> T callAs(Class<T> expectedType, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(expectedType, "Null expectedType");
        Objects.requireNonNull(kwargs, "Null kwargs");
        return i.executeInSingleThread(() -> pyCallable.callAs(expectedType, kwargs));
    }

    public <T> T callAs(Class<T> expectedType, Object[] args, Map<String, Object> kwargs) throws JepException {
        Objects.requireNonNull(expectedType, "Null expectedType");
        Objects.requireNonNull(kwargs, "Null kwargs");
        return i.executeInSingleThread(() -> pyCallable.callAs(expectedType, args, kwargs));
    }
}
