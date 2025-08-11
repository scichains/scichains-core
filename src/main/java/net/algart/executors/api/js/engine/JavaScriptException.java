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

package net.algart.executors.api.js.engine;

import javax.script.ScriptException;
import java.io.Serial;

public final class JavaScriptException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1909706924795350246L;

    public JavaScriptException(String message, ScriptException cause) {
        super(message, cause);
    }

    public static RuntimeException wrap(ScriptException scriptException, String javaScript) {
        Throwable cause = scriptException.getCause();
        if (cause != null) {
            cause = cause.getCause();
        }
        if (cause instanceof NullPointerException
                || cause instanceof IllegalArgumentException
                || cause instanceof IllegalStateException
                || cause instanceof UnsupportedOperationException) {
            // - typical exceptions, that can be thrown in user's JavaScript code via
            // "throw new java.lang.IllegalArgumentException(...)" or similar JavaScript operatorJ
            return (RuntimeException) cause;
        } else {
            return new JavaScriptException(scriptException.getMessage()
                    + "\nOccurred while attempt to execute JavaScript code:\n"
                    + reduce(javaScript), scriptException);
        }
    }

    private static String reduce(String value) {
        return value == null ? "" : value.length() > 1024 ? value.substring(0, 1024) + "..." : value;
    }

}
