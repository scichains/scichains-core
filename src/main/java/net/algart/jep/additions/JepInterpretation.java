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
import jep.python.PyCallable;
import net.algart.jep.JepPerformerContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class JepInterpretation {
    public static final String JEP_INSTALLATION_HINTS =
            """
                    To install "jep" with all required packages, please use the following command:
                       py -m pip install --upgrade setuptools wheel numpy
                       py -m pip install --no-cache-dir --force-reinstall --no-build-isolation jep
                    Note that "numpy" must be installed BEFORE "jep" \
                    for correct integration between "jep" and "numpy".
                    Before installing "jep", \
                    please set the environment variable JAVA_HOME to a path containing the JDK.""";

    private JepInterpretation() {
    }

    /**
     * Tests whether NumPy integration in the current JEP interpreter is working properly and
     * throws {@link JepNumpyIntegrationException} if this is not.
     *
     * <p>This method calls the specified Python function (provided by name),
     * which must return a NumPy array. If the result of the call is correctly
     * converted into an instance of {@link jep.NDArray} or {@link jep.DirectNDArray},
     * the method returns this object ({@link jep.NDArray} or {@link jep.DirectNDArray}).
     * Otherwise, it throws {@link JepNumpyIntegrationException}, and the type of the actual result
     * can be retrieved by {@link JepNumpyIntegrationException#getActualResultClass()} method.
     *
     * <p>This test helps verify that JEP was properly built with NumPy support in the current Python implementation.
     * To provide correct JEP+NumPy integration, the "numpy" package must be installed
     * in the Python environment <b>before</b> the "jep" package, and "jep" must be installed with
     * the following options:
     * <pre>
     * python -m pip install --no-cache-dir --force-reinstall --no-build-isolation jep
     * </pre>
     *
     * <p><b>Example of the expected Python function:</b>
     * <pre>{@code
     * def returnTestNdArray():
     *     import numpy
     *     return numpy.array([1, 2])
     * }</pre>
     *
     * <p>Make sure that the Python module containing the function is imported in advance.
     *
     * @param jepInterpreter              the JEP interpreter instance used to evaluate the Python function.
     * @param functionReturningNumpyArray the name of a no-argument Python function that returns a NumPy array.
     * @return an object returned as the result of calling the Python function.
     * @throws JepNumpyIntegrationException if the Python function returns an object
     *                                      that is not an instance of {@code NDArray} or {@code DirectNDArray}.
     * @throws JepException                 if an error occurred while attempting to execute the function.
     * @throws NullPointerException         if either argument is {@code null}
     */
    public static Object checkNumpyIntegration(Interpreter jepInterpreter, String functionReturningNumpyArray)
            throws JepNumpyIntegrationException {
        Objects.requireNonNull(jepInterpreter, "Null JEP interpreter");
        Objects.requireNonNull(functionReturningNumpyArray, "Null verification function name");
        final Object array;
        try {
            try (PyCallable creator = jepInterpreter.getValue(functionReturningNumpyArray, PyCallable.class)) {
                array = creator.call();
            }
        } catch (JepException e) {
            throw new JepException("Cannot execute Python verification function \"" +
                    functionReturningNumpyArray +
                    "\"; maybe, the corresponding Python module is not installed correctly", e);
        }
        if (!(array instanceof NDArray<?> || array instanceof DirectNDArray<?>)) {
            throw new JepNumpyIntegrationException(
                    "Integration problem between Python packages \"jep\" and \"numpy\":\n" +
                            "the function that creates numpy.ndarray " +
                            "does not return a correct Java type NDArray/DirectNDArray " +
                            "(it returns " +
                            (array == null ? null : "\"" + array.getClass().getCanonicalName() + "\"") +
                            ").\nThe \"jep\" package was probably not installed correctly in Python.\n" +
                            JEP_INSTALLATION_HINTS, array);
        }
        return array;
    }

    public static Object getJVMGlobalLock() {
        return JepSingleThreadInterpreter.getGlobalLock();
    }

    /**
     * Executes a sequence of actions under the JVM-global synchronization lock: {@link #getJVMGlobalLock()}.
     * Here:
     * <ul>
     *     <li><code>creation</code>:
     *     creates the interpreter (usually {@link JepSingleThreadInterpreter}
     *     or {@link JepPerformerContainer});</li>
     *     <li><code>processing</code>: performs the main logic using the created interpreter;</li>
     *     <li><code>closing</code>: closes the resource (for example, {@link JepPerformerContainer#close()} or
     *     {@link JepPerformerContainer#close()}; guaranteed to be called even in case of exception.
     * </ul>
     *
     * <p>This method uses <code>synchronized (getJVMGlobalLock()) {...}</code> operators
     * for executing all 3 stages.
     *
     * @param creation   create the interpreter.
     * @param processing process Python operations.
     * @param closing    close the interpreter.
     * @throws NullPointerException if any of arguments is <code>null</code>.
     */
    public static void executeWithJVMGlobalLock(Runnable creation, Runnable processing, Runnable closing) {
        Objects.requireNonNull(creation, "Null creation");
        Objects.requireNonNull(processing, "Null processing");
        Objects.requireNonNull(closing, "Null closing");
        synchronized (getJVMGlobalLock()) {
            try {
                creation.run();
                // - should create a single SharedInterpreter
                processing.run();
            } finally {
                closing.run();
                // - should close that SharedInterpreter; must be called even in case of exception!
            }
        }
    }
}
