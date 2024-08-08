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

package net.algart.executors.modules.core.logic.control;

import net.algart.executors.api.Executor;
import net.algart.executors.api.HighLevelException;
import net.algart.executors.modules.core.logic.ConditionStyle;

import java.util.function.Function;

public class ThrowException extends Executor {
    public static final String INPUT_CONDITION = "if";
    public static final String INPUT_REASON = "reason";
    public static final String S = "s";
    public static final String X = "x";
    public static final String M = "m";

    public enum ExceptionKind {
        NULL_POINTER_EXCEPTION(NullPointerException::new),
        ILLEGAL_ARGUMENT_EXCEPTION(IllegalArgumentException::new),
        ILLEGAL_STATE_EXCEPTION(IllegalStateException::new),
        INDEX_OUT_OF_BOUNDS_EXCEPTION(IndexOutOfBoundsException::new),
        UNSUPPORTED_OPERATION_EXCEPTION(UnsupportedOperationException::new),
        ASSERTION_ERROR(AssertionError::new);

        private final Function<String, Throwable> exception;

        ExceptionKind(Function<String, Throwable> exception) {
            this.exception = exception;
            final Throwable test = exception.apply("");
            assert test instanceof RuntimeException || test instanceof Error : "Invalid exception in enum";
        }

        public void throwException(String message) {
            throw new HighLevelException(exception.apply(message));
        }
    }

    private ConditionStyle conditionStyle = ConditionStyle.JAVA_LIKE;
    private boolean invert = false;
    private ExceptionKind exceptionKind = ExceptionKind.ASSERTION_ERROR;
    private String message = "Some problem occurred";

    public ThrowException() {
        addInputScalar(INPUT_CONDITION);
        addInputScalar(INPUT_REASON);
        addInputScalar(S);
        addInputNumbers(X);
        addInputMat(M);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(S);
        addOutputNumbers(X);
        addOutputMat(M);
    }

    public ConditionStyle getConditionStyle() {
        return conditionStyle;
    }

    public ThrowException setConditionStyle(ConditionStyle conditionStyle) {
        this.conditionStyle = conditionStyle;
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public ExceptionKind getExceptionKind() {
        return exceptionKind;
    }

    public ThrowException setExceptionKind(ExceptionKind exceptionKind) {
        this.exceptionKind = nonNull(exceptionKind);
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ThrowException setMessage(String message) {
        this.message = nonNull(message);
        return this;
    }

    @Override
    public void process() {
        getScalar(S).exchange(getInputScalar(S, true));
        getNumbers(X).exchange(getInputNumbers(X, true));
        getMat(M).exchange(getInputMat(M, true));
        if (condition()) {
            getScalar().remove();
            final String additionalInformation = getInputScalar(INPUT_REASON, true).getValue();
            final String message = this.message + (additionalInformation != null ? additionalInformation : "");
            exceptionKind.throwException(message);
        } else {
            getScalar().setTo("O'k");
        }
    }

    public boolean condition() {
        final String conditionString = getInputScalar(INPUT_CONDITION).getValue();
        return conditionStyle.toBoolean(conditionString, false) != invert;
    }
}
