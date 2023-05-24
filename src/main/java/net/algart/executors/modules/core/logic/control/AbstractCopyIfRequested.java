/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.modules.core.logic.ConditionStyle;
import net.algart.executors.api.Port;
import net.algart.executors.api.Executor;

abstract class AbstractCopyIfRequested extends Executor {
    public static final String IF_PREFIX = "if_";
    public static final String IF_S_PREFIX = "if_s";
    public static final String S_PREFIX = "s";
    public static final String IF_X_PREFIX = "if_x";
    public static final String X_PREFIX = "x";
    public static final String IF_M_PREFIX = "if_m";
    public static final String M_PREFIX = "m";

    private ConditionStyle conditionStyle = ConditionStyle.JAVA_LIKE;
    private boolean invert = false;

    protected AbstractCopyIfRequested() {
        useVisibleResultParameter();
    }

    public ConditionStyle getConditionStyle() {
        return conditionStyle;
    }

    public AbstractCopyIfRequested setConditionStyle(ConditionStyle conditionStyle) {
        this.conditionStyle = nonNull(conditionStyle);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public AbstractCopyIfRequested setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    public Boolean checkInputNecessary(Port inputPort) {
        if (inputPort == null) {
            return null;
        }
        final String inputPortName = inputPort.getName();
        if (inputPortName.startsWith(IF_PREFIX)) {
            return null;
        }
        final String ifPortName = IF_PREFIX + inputPortName;
        return condition(ifPortName);
    }

    public boolean condition(String portName) {
        final String conditionString = getInputScalar(portName, true).getValue();
        return conditionStyle.toBoolean(conditionString, false) != invert;
    }

    static String sPortName(int index) {
        return S_PREFIX + (index + 1);
    }

    static String ifSPortName(int index) {
        return IF_S_PREFIX + (index + 1);
    }

    static String xPortName(int index) {
        return X_PREFIX + (index + 1);
    }

    static String ifXPortName(int index) {
        return IF_X_PREFIX + (index + 1);
    }

    static String mPortName(int index) {
        return M_PREFIX + (index + 1);
    }

    static String ifMPortName(int index) {
        return IF_M_PREFIX + (index + 1);
    }
}
