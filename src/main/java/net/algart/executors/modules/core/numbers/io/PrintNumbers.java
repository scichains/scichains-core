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

package net.algart.executors.modules.core.numbers.io;

import net.algart.executors.api.LogLevel;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.scalars.conversions.JoinNumbersToScalar;
import net.algart.executors.modules.core.scalars.io.PrintScalar;

public final class PrintNumbers extends JoinNumbersToScalar {
    public static final String S = "s";
    public static final String X = "x";
    public static final String M = "m";

    private String pattern = PrintScalar.SCALAR_PATTERN;
    private LogLevel logLevel = LogLevel.INFO;
    private String file = FileOperation.DEFAULT_EMPTY_FILE;

    public PrintNumbers() {
        addInputScalar(S);
        addInputNumbers(X);
        addInputMat(M);
        addOutputScalar(S);
        addOutputNumbers(X);
        addOutputMat(M);
    }

    public static PrintNumbers getInstance() {
        return new PrintNumbers();
    }

    public String getPattern() {
        return pattern;
    }

    public PrintNumbers setPattern(String pattern) {
        this.pattern = nonNull(pattern);
        return this;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public PrintNumbers setLogLevel(LogLevel logLevel) {
        this.logLevel = nonNull(logLevel);
        return this;
    }

    public String getFile() {
        return file;
    }

    public PrintNumbers setFile(String file) {
        this.file = nonNull(file);
        return this;
    }

    @Override
    public SScalar analyse(SNumbers source) {
        getScalar(S).exchange(getInputScalar(S, true));
        getNumbers(X).exchange(getInputNumbers(X, true));
        getMat(M).exchange(getInputMat(M, true));
        final String result = PrintScalar.getInstance()
                .setPattern(pattern)
                .setLogLevel(logLevel)
                .setFile(file)
                .print(() -> super.join(source), isOutputNecessary(defaultOutputPortName()));
        return SScalar.valueOf(result);
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

}
