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

import net.algart.executors.api.js.scriptengine.JavaScriptContextContainer;
import net.algart.executors.api.js.scriptengine.JavaScriptPerformer;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;

import javax.script.ScriptEngine;

@Deprecated
public final class RepeatJSOld extends Executor {
    public static final String FIRST_ITERATION_VARIABLE = "isFirst";
    public static final String INPUT_A = "a";
    public static final String INPUT_B = "b";
    public static final String INPUT_C = "c";
    public static final String INPUT_D = "d";
    public static final String INPUT_E = "e";
    public static final String INPUT_F = "f";
    public static final String INPUT_X1 = "x1";
    public static final String X1_VARIABLE_ALT = "x";
    public static final String INPUT_X2 = "x2";
    public static final String INPUT_X3 = "x3";
    public static final String OUTPUT_IS_FIRST = "is_first";
    public static final String OUTPUT_IS_LAST = "is_last";
    public static final String OUTPUT_IS_NOT_FIRST = "is_not_first";
    public static final String OUTPUT_IS_NOT_LAST = "is_not_last";
    public static final String OUTPUT_A = "a";
    public static final String OUTPUT_B = "b";
    public static final String OUTPUT_C = "c";
    public static final String OUTPUT_D = "d";
    public static final String OUTPUT_I = "i";
    public static final String OUTPUT_J = "j";
    public static final String OUTPUT_K = "k";
    public static final String OUTPUT_X1 = "x1";
    public static final String OUTPUT_X2 = "x2";
    public static final String OUTPUT_X3 = "x3";

    private String initializingOperator = "var i = 0;";
    private String resultA = "";
    private String resultB = "";
    private String resultC = "";
    private String resultD = "";
    private String resultI = "i";
    private String resultJ = "";
    private String resultK = "";
    private String resultX1 = "";
    private String resultX2 = "";
    private String resultX3 = "";
    private int resultX1BlockLength = 1;
    private int resultX2BlockLength = 1;
    private int resultX3BlockLength = 1;
    private String status = "";
    private String doOperator = "i++";
    private String whileCondition = "i < 10";
    private boolean convertInputArraysToDouble = false;
    private boolean shareNamespace = true;

    private final JavaScriptContextContainer contextContainer = JavaScriptContextContainer.getInstance();
    private JavaScriptPerformer javaScriptInitializingOperator = null;
    private JavaScriptPerformer javaScriptResultA = null;
    private JavaScriptPerformer javaScriptResultB = null;
    private JavaScriptPerformer javaScriptResultC = null;
    private JavaScriptPerformer javaScriptResultD = null;
    private JavaScriptPerformer javaScriptResultI = null;
    private JavaScriptPerformer javaScriptResultJ = null;
    private JavaScriptPerformer javaScriptResultK = null;
    private JavaScriptPerformer javaScriptResultX1 = null;
    private JavaScriptPerformer javaScriptResultX2 = null;
    private JavaScriptPerformer javaScriptResultX3 = null;
    private JavaScriptPerformer javaScriptStatus = null;
    private JavaScriptPerformer javaScriptDoOperator = null;
    private JavaScriptPerformer javaScriptWhileCondition = null;
    private boolean isFirstIteration = false;
    private boolean isLastIteration = false;

    public RepeatJSOld() {
        addInputScalar(INPUT_A);
        addInputScalar(INPUT_B);
        addInputScalar(INPUT_C);
        addInputScalar(INPUT_D);
        addInputScalar(INPUT_E);
        addInputScalar(INPUT_F);
        addInputNumbers(INPUT_X1);
        addInputNumbers(INPUT_X2);
        addInputNumbers(INPUT_X3);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_IS_FIRST);
        addOutputScalar(OUTPUT_IS_LAST);
        addOutputScalar(OUTPUT_IS_NOT_FIRST);
        addOutputScalar(OUTPUT_IS_NOT_LAST);
        addOutputScalar(OUTPUT_A);
        addOutputScalar(OUTPUT_B);
        addOutputScalar(OUTPUT_C);
        addOutputScalar(OUTPUT_D);
        addOutputScalar(OUTPUT_I);
        addOutputScalar(OUTPUT_J);
        addOutputScalar(OUTPUT_K);
        addOutputNumbers(OUTPUT_X1);
        addOutputNumbers(OUTPUT_X2);
        addOutputNumbers(OUTPUT_X3);
    }

    public String getInitializingOperator() {
        return initializingOperator;
    }

    public RepeatJSOld setInitializingOperator(String initializingOperator) {
        this.initializingOperator = nonNull(initializingOperator);
        return this;
    }

    public String getResultA() {
        return resultA;
    }

    public RepeatJSOld setResultA(String resultA) {
        this.resultA = nonNull(resultA);
        return this;
    }

    public String getResultB() {
        return resultB;
    }

    public RepeatJSOld setResultB(String resultB) {
        this.resultB = nonNull(resultB);
        return this;
    }

    public String getResultC() {
        return resultC;
    }

    public RepeatJSOld setResultC(String resultC) {
        this.resultC = nonNull(resultC);
        return this;
    }

    public String getResultD() {
        return resultD;
    }

    public RepeatJSOld setResultD(String resultD) {
        this.resultD = nonNull(resultD);
        return this;
    }

    public String getResultI() {
        return resultI;
    }

    public RepeatJSOld setResultI(String resultI) {
        this.resultI = nonNull(resultI);
        return this;
    }

    public String getResultJ() {
        return resultJ;
    }

    public RepeatJSOld setResultJ(String resultJ) {
        this.resultJ = nonNull(resultJ);
        return this;
    }

    public String getResultK() {
        return resultK;
    }

    public RepeatJSOld setResultK(String resultK) {
        this.resultK = nonNull(resultK);
        return this;
    }

    public String getResultX1() {
        return resultX1;
    }

    public RepeatJSOld setResultX1(String resultX1) {
        this.resultX1 = nonNull(resultX1);
        return this;
    }

    public String getResultX2() {
        return resultX2;
    }

    public RepeatJSOld setResultX2(String resultX2) {
        this.resultX2 = nonNull(resultX2);
        return this;
    }

    public String getResultX3() {
        return resultX3;
    }

    public RepeatJSOld setResultX3(String resultX3) {
        this.resultX3 = nonNull(resultX3);
        return this;
    }

    public int getResultX1BlockLength() {
        return resultX1BlockLength;
    }

    public RepeatJSOld setResultX1BlockLength(int resultX1BlockLength) {
        this.resultX1BlockLength = positive(resultX1BlockLength);
        return this;
    }

    public int getResultX2BlockLength() {
        return resultX2BlockLength;
    }

    public RepeatJSOld setResultX2BlockLength(int resultX2BlockLength) {
        this.resultX2BlockLength = positive(resultX2BlockLength);
        return this;
    }

    public int getResultX3BlockLength() {
        return resultX3BlockLength;
    }

    public RepeatJSOld setResultX3BlockLength(int resultX3BlockLength) {
        this.resultX3BlockLength = positive(resultX3BlockLength);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public RepeatJSOld setStatus(String status) {
        this.status = nonNull(status);
        return this;
    }

    public String getDoOperator() {
        return doOperator;
    }

    public RepeatJSOld setDoOperator(String doOperator) {
        this.doOperator = nonNull(doOperator);
        return this;
    }

    public String getWhileCondition() {
        return whileCondition;
    }

    public RepeatJSOld setWhileCondition(String whileCondition) {
        this.whileCondition = nonNull(whileCondition);
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return convertInputArraysToDouble;
    }

    public RepeatJSOld setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        this.convertInputArraysToDouble = convertInputArraysToDouble;
        return this;
    }

    public boolean isShareNamespace() {
        return shareNamespace;
    }

    public RepeatJSOld setShareNamespace(boolean shareNamespace) {
        this.shareNamespace = shareNamespace;
        return this;
    }

    @Override
    public void initialize() {
        isFirstIteration = true;
        final ScriptEngine context = contextContainer.getContext(shareNamespace, getContextId());
        javaScriptInitializingOperator = getScript(initializingOperator, javaScriptInitializingOperator, context);
        putAllInputs(context, true);
        javaScriptInitializingOperator.perform();
    }

    @Override
    public void process() {
        final ScriptEngine context = contextContainer.getContext(shareNamespace, getContextId());
        putAllInputs(context, false);
        javaScriptResultA = getScript(resultA, javaScriptResultA, context);
        javaScriptResultB = getScript(resultB, javaScriptResultB, context);
        javaScriptResultC = getScript(resultC, javaScriptResultC, context);
        javaScriptResultD = getScript(resultD, javaScriptResultD, context);
        javaScriptResultI = getScript(resultI, javaScriptResultI, context);
        javaScriptResultJ = getScript(resultJ, javaScriptResultJ, context);
        javaScriptResultK = getScript(resultK, javaScriptResultK, context);
        javaScriptResultX1 = getScript(resultX1, javaScriptResultX1, context);
        javaScriptResultX2 = getScript(resultX2, javaScriptResultX2, context);
        javaScriptResultX3 = getScript(resultX3, javaScriptResultX3, context);
        javaScriptStatus = getScript(status, javaScriptStatus, context);
        javaScriptDoOperator = getScript(doOperator, javaScriptDoOperator, context);
        javaScriptWhileCondition = getScript(whileCondition, javaScriptWhileCondition, context);
        final String result = javaScriptDoOperator.calculateStringOrNumber();
        getScalar().setTo(result);
        getScalar(OUTPUT_A).setTo(javaScriptResultA.calculateStringOrNumber());
        getScalar(OUTPUT_B).setTo(javaScriptResultB.calculateStringOrNumber());
        getScalar(OUTPUT_C).setTo(javaScriptResultC.calculateStringOrNumber());
        getScalar(OUTPUT_D).setTo(javaScriptResultD.calculateStringOrNumber());
        getScalar(OUTPUT_I).setTo(javaScriptResultI.calculateDouble());
        getScalar(OUTPUT_J).setTo(javaScriptResultJ.calculateDouble());
        getScalar(OUTPUT_K).setTo(javaScriptResultK.calculateDouble());
        if (!status.isBlank()) {
            uploadStatus(javaScriptStatus);
        }
        uploadOutputNumbers(OUTPUT_X1, javaScriptResultX1, resultX1BlockLength);
        uploadOutputNumbers(OUTPUT_X2, javaScriptResultX2, resultX2BlockLength);
        uploadOutputNumbers(OUTPUT_X3, javaScriptResultX3, resultX3BlockLength);
        final boolean whileCondition = javaScriptWhileCondition.calculateBoolean();
        isLastIteration = !whileCondition;
        getScalar(OUTPUT_IS_FIRST).setTo(isFirstIteration);
        getScalar(OUTPUT_IS_LAST).setTo(isLastIteration);
        getScalar(OUTPUT_IS_NOT_FIRST).setTo(!isFirstIteration);
        getScalar(OUTPUT_IS_NOT_LAST).setTo(!isLastIteration);
        isFirstIteration = false;
    }

    @Override
    public boolean needToRepeat() {
        logDebug(() -> (!isLastIteration ? "Repeating loop" : "FINISHING loop")
                + " according formula \"" + whileCondition + "\"");
        return !isLastIteration;
    }

    private void putAllInputs(ScriptEngine context, boolean alsoUninitialized) {
        context.put(FIRST_ITERATION_VARIABLE, isFirstIteration);
        // - but there is no sense to add variable for lastIteration: it is
        // usually calculated IN A PROCESS of executing main script
        putInputScalar(context, INPUT_A);
        putInputScalar(context, INPUT_B);
        putInputScalar(context, INPUT_C);
        putInputScalar(context, INPUT_D);
        putInputScalar(context, INPUT_E);
        putInputScalar(context, INPUT_F);
        putInputNumbers(context, INPUT_X1, X1_VARIABLE_ALT, alsoUninitialized);
        putInputNumbers(context, INPUT_X2, null, alsoUninitialized);
        putInputNumbers(context, INPUT_X3, null, alsoUninitialized);
    }

    private void putInputScalar(ScriptEngine context, String inputPortName) {
        SScalar scalar = getInputScalar(inputPortName, true);
        if (scalar.isInitialized()) {
            context.put(inputPortName, scalar.getValue());
        }
    }

    private void putInputNumbers(
            ScriptEngine context,
            String inputPortName,
            String altName,
            boolean alsoUninitialized) {
        SNumbers numbers = getInputNumbers(inputPortName, true);
        final Object value = convertInputArraysToDouble ? numbers.toDoubleArray() : numbers.getArray();
        if (alsoUninitialized || value != null) {
            context.put(inputPortName, value);
            if (altName != null) {
                context.put(altName, value);
            }
        }
    }

    private void uploadStatus(JavaScriptPerformer statusFormula) {
        final ExecutionBlock executor = getCaller();
        if (!(executor instanceof Executor)) {
            return;
        }
        final Object status = statusFormula.perform();
        if (status == null) {
            return;
        }
        ((Executor) executor).status().setMessageString(status.toString());
    }

    private void uploadOutputNumbers(String outputPortName, JavaScriptPerformer resultFormula, int blockLength) {
        final Object result = resultFormula.perform();
        if (result != null) {
            if (result instanceof SNumbers) {
                getNumbers(outputPortName).setTo((SNumbers) result);
            } else {
                getNumbers(outputPortName).setToArray(result, blockLength);
            }
        }
    }

    private static JavaScriptPerformer getScript(
            String formula,
            JavaScriptPerformer previous,
            ScriptEngine context) {
        return JavaScriptPerformer.newInstanceIfChanged(formula, previous, context);
    }
}
