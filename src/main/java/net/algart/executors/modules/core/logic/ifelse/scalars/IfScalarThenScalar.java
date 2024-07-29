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

package net.algart.executors.modules.core.logic.ifelse.scalars;

import net.algart.executors.api.Executor;
import net.algart.executors.api.Port;
import net.algart.executors.modules.core.logic.ConditionStyle;

public final class IfScalarThenScalar extends Executor {
    public static final String INPUT_CONDITION = "if";
    public static final String INPUT_PORT_FALSE = "false";
    public static final String INPUT_PORT_TRUE = "true";
    public static final String OUTPUT_CONDITION = "if";

    private ConditionStyle conditionStyle = ConditionStyle.JAVA_LIKE;
    private boolean defaultCondition = false;
    private String defaultFalse = "";
    private String defaultTrue = "";
    private boolean requireInput = true;

    public IfScalarThenScalar() {
        addInputScalar(INPUT_CONDITION);
        addInputScalar(INPUT_PORT_FALSE);
        addInputScalar(INPUT_PORT_TRUE);
        addOutputScalar(OUTPUT_CONDITION);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public ConditionStyle getConditionStyle() {
        return conditionStyle;
    }

    public IfScalarThenScalar setConditionStyle(ConditionStyle conditionStyle) {
        this.conditionStyle = nonNull(conditionStyle);
        return this;
    }

    public boolean isDefaultCondition() {
        return defaultCondition;
    }

    public IfScalarThenScalar setDefaultCondition(boolean defaultCondition) {
        this.defaultCondition = defaultCondition;
        return this;
    }

    public String getDefaultFalse() {
        return defaultFalse;
    }

    public IfScalarThenScalar setDefaultFalse(String defaultFalse) {
        this.defaultFalse = nonNull(defaultFalse);
        return this;
    }

    public String getDefaultTrue() {
        return defaultTrue;
    }

    public IfScalarThenScalar setDefaultTrue(String defaultTrue) {
        this.defaultTrue = nonNull(defaultTrue);
        return this;
    }

    public boolean isRequireInput() {
        return requireInput;
    }

    public IfScalarThenScalar setRequireInput(boolean requireInput) {
        this.requireInput = requireInput;
        return this;
    }

    @Override
    public void process() {
        final boolean condition = condition();
        final String trueOrFalse = getInputScalar(portName(condition), !requireInput).getValue();
        final String defaultValue = condition ? defaultTrue : defaultFalse;
        getScalar().setTo(trueOrFalse != null ? trueOrFalse : !defaultValue.isEmpty() ? defaultValue : null);
        getScalar(OUTPUT_CONDITION).setTo(condition);
    }

    @Override
    public Boolean checkInputNecessary(Port inputPort) {
        if (inputPort == null) {
            return null;
        }
        final String inputPortName = inputPort.getName();
        if (INPUT_CONDITION.equals(inputPortName)) {
            return null;
        }
        return portName(condition()).equals(inputPortName);
    }

    public boolean condition() {
        final String conditionString = getInputScalar(INPUT_CONDITION, true).getValue();
        return conditionStyle.toBoolean(conditionString, defaultCondition);
    }

    private static String portName(boolean condition) {
        return condition ? INPUT_PORT_TRUE : INPUT_PORT_FALSE;
    }
}
