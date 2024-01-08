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

import jep.Interpreter;
import jep.JepConfig;
import jep.JepException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class JepExtendedConfig extends JepConfig {
    @FunctionalInterface
    public interface Verifier {
        /**
         * Executed once to check possible installation problems. Should throw an exception in a case of problems
         *
         * @param jepInterpreter JEP interpreter; can be not used (if this code creates its own interpreter)
         * @param config JEP configuration
         */
        void verify(Interpreter jepInterpreter, JepConfig config) throws JepException;
    }

    private List<String> startupCode = Collections.emptyList();
    private Verifier verifier = null;

    public List<String> getStartupCode() {
        return Collections.unmodifiableList(startupCode);
    }

    public JepExtendedConfig setStartupCode(List<String> startupCode) {
        Objects.requireNonNull(startupCode, "Null startupCode");
        this.startupCode = new ArrayList<>(startupCode);
        return this;
    }

    public boolean hasVerifier() {
        return verifier != null;
    }

    public Verifier getVerifier() {
        return verifier;
    }

    public JepExtendedConfig setVerifier(Verifier verifier) {
        this.verifier = verifier;
        return this;
    }
}
