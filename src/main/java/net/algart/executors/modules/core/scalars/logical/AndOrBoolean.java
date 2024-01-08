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

package net.algart.executors.modules.core.scalars.logical;

import net.algart.executors.modules.core.logic.ConditionStyle;
import net.algart.executors.api.Executor;

abstract class AndOrBoolean extends Executor {
    public static final String INPUT_PORT_PREFIX = "x";

    private final boolean initialAccumulator;

    private boolean invert = false;
    private ConditionStyle booleanStyle = ConditionStyle.JAVA_LIKE;
    private boolean commonInputStyle = true;

    public AndOrBoolean(boolean initialAccumulator) {
        this.initialAccumulator = initialAccumulator;
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public boolean isInvert() {
        return invert;
    }

    public AndOrBoolean setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    public ConditionStyle getBooleanStyle() {
        return booleanStyle;
    }

    public AndOrBoolean setBooleanStyle(ConditionStyle booleanStyle) {
        this.booleanStyle = nonNull(booleanStyle);
        return this;
    }

    public boolean isCommonInputStyle() {
        return commonInputStyle;
    }

    public AndOrBoolean setCommonInputStyle(boolean commonInputStyle) {
        this.commonInputStyle = commonInputStyle;
        return this;
    }

    @Override
    public void process() {
        boolean result = initialAccumulator;
        for (int k = 0; ; k++) {
            final String portName = INPUT_PORT_PREFIX + (k + 1);
            if (!hasInputPort(portName)) {
                break;
            }
            final String input = getInputScalar(portName,true).getValue();
            result = combine(result, toBoolean(input));
        }
        if (invert) {
            result = !result;
        }
        booleanStyle.setScalar(getScalar(), result);
    }

    public boolean toBoolean(String inputString) {
        return commonInputStyle ?
                ConditionStyle.toCommonBoolean(inputString, initialAccumulator) :
                booleanStyle.toBoolean(inputString, initialAccumulator);
    }

    abstract boolean combine(boolean accumulator, boolean value);
}
