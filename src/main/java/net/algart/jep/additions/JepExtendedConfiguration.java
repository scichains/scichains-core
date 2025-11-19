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

import jep.Interpreter;
import jep.JepConfig;
import jep.JepException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * <p>An extended configuration object for constructing a Jep instance, corresponding to the
 * configuration of the particular Python sub-interpreter. </p>
 */
public class JepExtendedConfiguration extends JepConfig {
    @FunctionalInterface
    public interface Verifier {
        /**
         * Executed once to check possible installation problems.
         * Should throw an exception or log some message in case of a problem.
         *
         * <p>The result of this method is stored in the configuration using
         * {@link #setVerificationStatus(Object)} method.</p>
         *
         * @param jepInterpreter JEP interpreter; can be not used (if this code creates its own interpreter).
         * @param configuration  JEP configuration.
         */
        Object verify(Interpreter jepInterpreter, JepExtendedConfiguration configuration) throws JepException;
    }

    private List<String> startupCode = Collections.emptyList();
    private Verifier verifier = null;
    private Object verificationStatus = null;

    public List<String> getStartupCode() {
        return Collections.unmodifiableList(startupCode);
    }

    public JepExtendedConfiguration setStartupCode(List<String> startupCode) {
        Objects.requireNonNull(startupCode, "Null startupCode");
        final ArrayList<String> clone = new ArrayList<>(startupCode);
        clone.forEach(s -> Objects.requireNonNull(s, "Null element of startupCode"));
        this.startupCode = clone;
        return this;
    }

    public boolean hasVerifier() {
        return verifier != null;
    }

    public Verifier getVerifier() {
        return verifier;
    }

    /**
     * Sets some additional verifier that is called after the {@link #setStartupCode(List) start-up code}
     * and throws exception in the case of any possible problems.
     *
     * @param verifier the new verifier.
     * @return a reference to this object.
     */
    public JepExtendedConfiguration setVerifier(Verifier verifier) {
        this.verifier = verifier;
        return this;
    }

    public Object getVerificationStatus() {
        return verificationStatus;
    }

    public JepExtendedConfiguration setVerificationStatus(Object verificationStatus) {
        this.verificationStatus = verificationStatus;
        return this;
    }

    public String getIncludePath() {
        return includePath == null? null : includePath.toString();
    }

    @Override
    public String toString() {
        return "JepExtendedConfiguration with verificationStatus=" + verificationStatus + " " + super.toString();
    }
}
