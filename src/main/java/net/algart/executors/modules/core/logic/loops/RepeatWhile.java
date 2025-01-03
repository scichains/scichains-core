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

package net.algart.executors.modules.core.logic.loops;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.logic.ConditionStyle;

public final class RepeatWhile extends Executor {
    public static final String INPUT_CONDITION = "while";
    public static final String OUTPUT_IS_FIRST = "is_first";
    public static final String OUTPUT_IS_LAST = "is_last";
    public static final String OUTPUT_IS_NOT_FIRST = "is_not_first";
    public static final String OUTPUT_IS_NOT_LAST = "is_not_last";
    public static final String OUTPUT_COUNT = "count";
    public static final String OUTPUT_COUNT_1 = "count_1";
    public static final String S = "s";
    public static final String X = "x";
    public static final String M = "m";

    private ConditionStyle conditionStyle = ConditionStyle.JAVA_LIKE;
    private boolean invertCondition = false;
    private Long maxIterationsCount = null;

    private boolean whileCondition = false;
    private boolean isFirstIteration = false;
    private boolean isLastIteration = false;
    private long counter = 0L;

    public RepeatWhile() {
        addInputScalar(INPUT_CONDITION);
        addOutputScalar(OUTPUT_IS_FIRST);
        addOutputScalar(OUTPUT_IS_LAST);
        addOutputScalar(OUTPUT_IS_NOT_FIRST);
        setDefaultOutputScalar(OUTPUT_IS_NOT_LAST);
        addOutputScalar(OUTPUT_COUNT);
        addOutputScalar(OUTPUT_COUNT_1);
        addInputScalar(S);
        addInputNumbers(X);
        addInputMat(M);
        addOutputScalar(S);
        addOutputNumbers(X);
        addOutputMat(M);
    }

    public ConditionStyle getConditionStyle() {
        return conditionStyle;
    }

    public RepeatWhile setConditionStyle(ConditionStyle conditionStyle) {
        this.conditionStyle = nonNull(conditionStyle);
        return this;
    }

    public boolean isInvertCondition() {
        return invertCondition;
    }

    public RepeatWhile setInvertCondition(boolean invertCondition) {
        this.invertCondition = invertCondition;
        return this;
    }

    public Long getMaxIterationsCount() {
        return maxIterationsCount;
    }

    public RepeatWhile setMaxIterationsCount(Long maxIterationsCount) {
        if (maxIterationsCount != null) {
            positive(maxIterationsCount);
        }
        this.maxIterationsCount = maxIterationsCount;
        return this;
    }

    @Override
    public void initialize() {
        isFirstIteration = true;
        counter = 0;
    }

    @Override
    public void process() {
        getScalar(S).exchange(getInputScalar(S, true));
        getNumbers(X).exchange(getInputNumbers(X, true));
        getMat(M).exchange(getInputMat(M, true));
        final String conditionString = getInputScalar(INPUT_CONDITION, true).getValue();
        if (conditionString == null && maxIterationsCount == null) {
            throw new IllegalArgumentException("Both input condition and maximal iterations count " +
                    "are not specified: infinite loop!");
        }
        whileCondition = conditionStyle.toBoolean(conditionString, true);
        if (invertCondition) {
            whileCondition = !whileCondition;
        }
        getScalar(OUTPUT_COUNT).setTo(counter);
        getScalar(OUTPUT_COUNT_1).setTo(counter == Long.MAX_VALUE ? counter : counter + 1);
        if (counter < Long.MAX_VALUE) {
            // - never overflow: just stop counting instead
            counter++;
        }
        isLastIteration = !whileCondition || (maxIterationsCount != null && counter >= maxIterationsCount);
        getScalar(OUTPUT_IS_FIRST).setTo(isFirstIteration);
        getScalar(OUTPUT_IS_LAST).setTo(isLastIteration);
        getScalar(OUTPUT_IS_NOT_FIRST).setTo(!isFirstIteration);
        getScalar(OUTPUT_IS_NOT_LAST).setTo(whileCondition);
        isFirstIteration = false;
    }

    @Override
    public boolean needToRepeat() {
        logDebug(() -> (!isLastIteration ?
                "Repeating loop" :
                "FINISHING loop according " +
                        (!whileCondition ? "input condition" : "maximal number of iterations")));
        return !isLastIteration;
    }
}
