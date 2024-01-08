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

package net.algart.executors.modules.core.scalars.io;

import net.algart.executors.api.LogLevel;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;

import java.util.function.Supplier;

public final class PrintScalar extends ScalarFilter {
    public static final String S = "s";
    public static final String X = "x";
    public static final String M = "m";

    public static final String SCALAR_PATTERN = "$$$";

    private static final int MAX_RESULT_LENGTH = 50000;

    private String pattern = SCALAR_PATTERN;
    private LogLevel logLevel = LogLevel.INFO;
    private String file = FileOperation.DEFAULT_EMPTY_FILE;

    public PrintScalar() {
        addInputScalar(S);
        addInputNumbers(X);
        addInputMat(M);
        addOutputScalar(S);
        addOutputNumbers(X);
        addOutputMat(M);
    }

    public static PrintScalar getInstance() {
        return new PrintScalar();
    }

    public String getPattern() {
        return pattern;
    }

    public PrintScalar setPattern(String pattern) {
        this.pattern = nonNull(pattern);
        return this;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public PrintScalar setLogLevel(LogLevel logLevel) {
        this.logLevel = nonNull(logLevel);
        return this;
    }

    public String getFile() {
        return file;
    }

    public PrintScalar setFile(String file) {
        this.file = nonNull(file);
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        getScalar(S).exchange(getInputScalar(S, true));
        getNumbers(X).exchange(getInputNumbers(X, true));
        getMat(M).exchange(getInputMat(M, true));
        final String result = print(source::getValue, isOutputNecessary(defaultOutputPortName()));
        return SScalar.valueOf(result);
    }

    public String print(Supplier<String> source, boolean resultRequired) {
        if (resultRequired || logLevel.isLoggable()) {
            return print(source.get());
        } else {
            return null;
        }
    }

    public String print(String s) {
        if (s == null) {
            s = "[No input scalar]";
        }
        if (s.length() > MAX_RESULT_LENGTH) {
            s = s.substring(0, MAX_RESULT_LENGTH - 3) + "...";
        }
        final String message = pattern
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace(SCALAR_PATTERN, s);
        logLevel.log(message);
        if (!file.isEmpty()) {
            WriteScalar.getInstance().setFile(file).writeString(message);
        }
        return message;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
