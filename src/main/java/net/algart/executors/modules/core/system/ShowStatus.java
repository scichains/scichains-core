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

package net.algart.executors.modules.core.system;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.ExecutionStatus;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;

public final class ShowStatus extends ScalarFilter {
    public static final String OUTPUT_STATUS_JSON = "status_json";
    public static final String OUTPUT_ROOT_STATUS_JSON = "root_status_json";
    public static final String S1 = "s";
    public static final String X1 = "x";
    public static final String M1 = "m";
    public static final String S2 = "s2";
    public static final String X2 = "x2";
    public static final String M2 = "m2";
    public static final String S3 = "s3";
    public static final String X3 = "x3";
    public static final String M3 = "m3";

    public static final String SCALAR_PATTERN = "$$$";

    private static final int MAX_RESULT_LENGTH = 50000;

    private boolean doAction = true;
    private String pattern = SCALAR_PATTERN;
    private boolean modifyCallerStatus = true;

    public ShowStatus() {
        addOutputScalar(OUTPUT_STATUS_JSON);
        addOutputScalar(OUTPUT_ROOT_STATUS_JSON);
        addInputScalar(S1);
        addInputNumbers(X1);
        addInputMat(M1);
        addInputScalar(S2);
        addInputNumbers(X2);
        addInputMat(M2);
        addInputScalar(S3);
        addInputNumbers(X3);
        addInputMat(M3);
        addOutputScalar(S1);
        addOutputNumbers(X1);
        addOutputMat(M1);
        addOutputScalar(S2);
        addOutputNumbers(X2);
        addOutputMat(M2);
        addOutputScalar(S3);
        addOutputNumbers(X3);
        addOutputMat(M3);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public ShowStatus setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public ShowStatus setPattern(String pattern) {
        this.pattern = nonNull(pattern);
        return this;
    }

    public boolean isModifyCallerStatus() {
        return modifyCallerStatus;
    }

    public ShowStatus setModifyCallerStatus(boolean modifyCallerStatus) {
        this.modifyCallerStatus = modifyCallerStatus;
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        getScalar(S1).exchange(getInputScalar(S1, true));
        getScalar(S2).exchange(getInputScalar(S2, true));
        getScalar(S3).exchange(getInputScalar(S3, true));
        getNumbers(X1).exchange(getInputNumbers(X1, true));
        getNumbers(X2).exchange(getInputNumbers(X2, true));
        getNumbers(X3).exchange(getInputNumbers(X3, true));
        getMat(M1).exchange(getInputMat(M1, true));
        getMat(M2).exchange(getInputMat(M2, true));
        getMat(M3).exchange(getInputMat(M3, true));
        final String result = show(source.getValue());
        return SScalar.valueOf(result);
    }

    public String show(String s) {
        if (s != null && s.length() > MAX_RESULT_LENGTH) {
            s = s.substring(0, MAX_RESULT_LENGTH - 3) + "...";
        }
        String message = pattern
                .replace("\\n", "\n")
                .replace("\\r", "\r");
        if (s != null) {
            message = message.replace(SCALAR_PATTERN, s);
        }
        if (!doAction) {
            return message;
        }
        final ExecutionBlock executor = modifyCallerStatus ? getCaller() : this;
        if (executor instanceof Executor) {
            final ExecutionStatus status = ((Executor) executor).status();
            status.setMessageString(message);
            if (status.isOpened()) {
                if (isOutputNecessary(OUTPUT_STATUS_JSON)) {
                    getScalar(OUTPUT_STATUS_JSON).setTo(executor.statusData(ExecutionStatus.DataKind.JSON.code()));
                }
                if (isOutputNecessary(OUTPUT_ROOT_STATUS_JSON)) {
                    getScalar(OUTPUT_ROOT_STATUS_JSON).setTo(status.root().toJsonString());
                }
            }
        }
        return message;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
