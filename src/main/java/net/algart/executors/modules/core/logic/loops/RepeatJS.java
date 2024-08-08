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

package net.algart.executors.modules.core.logic.loops;

import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.GraalValues;
import net.algart.bridges.graalvm.api.GraalAPI;
import net.algart.bridges.graalvm.api.GraalSafety;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import org.graalvm.polyglot.Value;

public final class RepeatJS extends Executor {
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
    private final GraalAPI graalAPI = GraalAPI.getSmartScriptingInstance();
    private boolean shareNamespace = true;
    private boolean closeSharedContext = true;

    private GraalPerformerContainer performerContainer = null;
    private final GraalSourceContainer javaScriptInitializingOperator = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultA = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultB = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultC = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultD = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultI = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultJ = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultK = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX1 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX2 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX3 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptStatus = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptDoOperator = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptWhileCondition = GraalSourceContainer.newLiteral();
    private boolean isFirstIteration = false;
    private boolean isLastIteration = false;

    private final Object lock = new Object();

    public RepeatJS() {
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

    public RepeatJS setInitializingOperator(String initializingOperator) {
        this.initializingOperator = nonNull(initializingOperator);
        return this;
    }

    public String getResultA() {
        return resultA;
    }

    public RepeatJS setResultA(String resultA) {
        this.resultA = nonNull(resultA);
        return this;
    }

    public String getResultB() {
        return resultB;
    }

    public RepeatJS setResultB(String resultB) {
        this.resultB = nonNull(resultB);
        return this;
    }

    public String getResultC() {
        return resultC;
    }

    public RepeatJS setResultC(String resultC) {
        this.resultC = nonNull(resultC);
        return this;
    }

    public String getResultD() {
        return resultD;
    }

    public RepeatJS setResultD(String resultD) {
        this.resultD = nonNull(resultD);
        return this;
    }

    public String getResultI() {
        return resultI;
    }

    public RepeatJS setResultI(String resultI) {
        this.resultI = nonNull(resultI);
        return this;
    }

    public String getResultJ() {
        return resultJ;
    }

    public RepeatJS setResultJ(String resultJ) {
        this.resultJ = nonNull(resultJ);
        return this;
    }

    public String getResultK() {
        return resultK;
    }

    public RepeatJS setResultK(String resultK) {
        this.resultK = nonNull(resultK);
        return this;
    }

    public String getResultX1() {
        return resultX1;
    }

    public RepeatJS setResultX1(String resultX1) {
        this.resultX1 = nonNull(resultX1);
        return this;
    }

    public String getResultX2() {
        return resultX2;
    }

    public RepeatJS setResultX2(String resultX2) {
        this.resultX2 = nonNull(resultX2);
        return this;
    }

    public String getResultX3() {
        return resultX3;
    }

    public RepeatJS setResultX3(String resultX3) {
        this.resultX3 = nonNull(resultX3);
        return this;
    }

    public int getResultX1BlockLength() {
        return resultX1BlockLength;
    }

    public RepeatJS setResultX1BlockLength(int resultX1BlockLength) {
        this.resultX1BlockLength = positive(resultX1BlockLength);
        return this;
    }

    public int getResultX2BlockLength() {
        return resultX2BlockLength;
    }

    public RepeatJS setResultX2BlockLength(int resultX2BlockLength) {
        this.resultX2BlockLength = positive(resultX2BlockLength);
        return this;
    }

    public int getResultX3BlockLength() {
        return resultX3BlockLength;
    }

    public RepeatJS setResultX3BlockLength(int resultX3BlockLength) {
        this.resultX3BlockLength = positive(resultX3BlockLength);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public RepeatJS setStatus(String status) {
        this.status = nonNull(status);
        return this;
    }

    public String getDoOperator() {
        return doOperator;
    }

    public RepeatJS setDoOperator(String doOperator) {
        this.doOperator = nonNull(doOperator);
        return this;
    }

    public String getWhileCondition() {
        return whileCondition;
    }

    public RepeatJS setWhileCondition(String whileCondition) {
        this.whileCondition = nonNull(whileCondition);
        return this;
    }

    public boolean isConvertInputScalarToNumber() {
        return graalAPI.isConvertInputScalarToNumber();
    }

    public RepeatJS setConvertInputScalarToNumber(boolean convertInputScalarToNumber) {
        graalAPI.setConvertInputScalarToNumber(convertInputScalarToNumber);
        return this;
    }

    public boolean isConvertInputNumbersToArray() {
        return graalAPI.isConvertInputNumbersToArray();
    }

    public RepeatJS setConvertInputNumbersToArray(boolean convertInputNumbersToArray) {
        graalAPI.setConvertInputNumbersToArray(convertInputNumbersToArray);
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return graalAPI.isConvertInputArraysToDouble();
    }

    public RepeatJS setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        graalAPI.setConvertInputArraysToDouble(convertInputArraysToDouble);
        return this;
    }

    public boolean isConvertOutputIntegerToBriefForm() {
        return graalAPI.isConvertOutputIntegerToBriefForm();
    }

    public RepeatJS setConvertOutputIntegersToBriefForm(boolean convertOutputIntegersToBriefForm) {
        graalAPI.setConvertOutputIntegersToBriefForm(convertOutputIntegersToBriefForm);
        return this;
    }

    public boolean isShareNamespace() {
        return shareNamespace;
    }

    public RepeatJS setShareNamespace(boolean shareNamespace) {
        if (shareNamespace != this.shareNamespace) {
            closePerformerContainer();
            this.shareNamespace = shareNamespace;
        }
        return this;
    }

    public boolean isCloseSharedContext() {
        return closeSharedContext;
    }

    public RepeatJS setCloseSharedContext(boolean closeSharedContext) {
        this.closeSharedContext = closeSharedContext;
        return this;
    }

    @Override
    public void initialize() {
        isFirstIteration = true;
        try (GraalPerformer performer = performerContainer().performer(getContextId())) {
            final Value bindings = performer.bindingsJS();
            if (!initializingOperator.isEmpty()) {
                javaScriptInitializingOperator.setCommonJS(initializingOperator);
                putAllInputs(bindings, true);
                performer.perform(javaScriptInitializingOperator);
            }
        }
    }

    @Override
    public void process() {
        final GraalPerformer performer = performerContainer().performer(getContextId());
        putAllInputs(performer.bindingsJS(), false);
        javaScriptResultA.setCommonJS(resultA);
        javaScriptResultB.setCommonJS(resultB);
        javaScriptResultC.setCommonJS(resultC);
        javaScriptResultD.setCommonJS(resultD);
        javaScriptResultI.setCommonJS(resultI);
        javaScriptResultJ.setCommonJS(resultJ);
        javaScriptResultK.setCommonJS(resultK);
        javaScriptResultX1.setCommonJS(resultX1);
        javaScriptResultX2.setCommonJS(resultX2);
        javaScriptResultX3.setCommonJS(resultX3);
        javaScriptStatus.setCommonJS(status);
        javaScriptDoOperator.setCommonJS(doOperator);
        javaScriptWhileCondition.setCommonJS(whileCondition);
        final Value result = performer.perform(javaScriptDoOperator);
        graalAPI.storeScalar(this, defaultOutputPortName(), result);
        getScalar().setTo(result);
        graalAPI.storeScalar(this, OUTPUT_A, performer.perform(javaScriptResultA));
        graalAPI.storeScalar(this, OUTPUT_B, performer.perform(javaScriptResultB));
        graalAPI.storeScalar(this, OUTPUT_C, performer.perform(javaScriptResultC));
        graalAPI.storeScalar(this, OUTPUT_D, performer.perform(javaScriptResultD));
        getScalar(OUTPUT_I).setTo(GraalValues.toSmartDouble(performer.perform(javaScriptResultI)));
        getScalar(OUTPUT_J).setTo(GraalValues.toSmartDouble(performer.perform(javaScriptResultJ)));
        getScalar(OUTPUT_K).setTo(GraalValues.toSmartDouble(performer.perform(javaScriptResultK)));
        if (!status.isBlank()) {
            uploadStatus(performer, javaScriptStatus);
        }
        graalAPI.storeNumbers(this, OUTPUT_X1, performer.perform(javaScriptResultX1), resultX1BlockLength);
        graalAPI.storeNumbers(this, OUTPUT_X2, performer.perform(javaScriptResultX2), resultX2BlockLength);
        graalAPI.storeNumbers(this, OUTPUT_X3, performer.perform(javaScriptResultX3), resultX3BlockLength);
        final boolean whileCondition = GraalValues.toSmartBoolean(performer.perform(javaScriptWhileCondition));
        isLastIteration = !whileCondition;
        getScalar(OUTPUT_IS_FIRST).setTo(isFirstIteration);
        getScalar(OUTPUT_IS_LAST).setTo(isLastIteration);
        getScalar(OUTPUT_IS_NOT_FIRST).setTo(!isFirstIteration);
        getScalar(OUTPUT_IS_NOT_LAST).setTo(!isLastIteration);
        isFirstIteration = false;
    }

    @Override
    public void close() {
        super.close();
        closePerformerContainer();
    }

    @Override
    public boolean needToRepeat() {
        logDebug(() -> (!isLastIteration ? "Repeating loop" : "FINISHING loop")
                + " according formula \"" + whileCondition + "\"");
        return !isLastIteration;
    }

    private void closePerformerContainer() {
        synchronized (lock) {
            if (this.performerContainer != null) {
                this.performerContainer.freeResources(closeSharedContext);
                this.performerContainer = null;
            }
        }
    }

    private GraalPerformerContainer performerContainer() {
        synchronized (lock) {
            if (performerContainer == null) {
                this.performerContainer = GraalAPI.getJSContainer(shareNamespace, GraalSafety.SAFE);
            }
            return performerContainer;
        }
    }

    private void putAllInputs(Value bindings, boolean putNullBindingForUninitialized) {
        bindings.putMember(FIRST_ITERATION_VARIABLE, isFirstIteration);
        // - but there is no sense to add variable for lastIteration: it is
        // usually calculated IN A PROCESS of executing main script
        graalAPI.loadScalar(bindings, this, INPUT_A, null);
        graalAPI.loadScalar(bindings, this, INPUT_B, null);
        graalAPI.loadScalar(bindings, this, INPUT_C, null);
        graalAPI.loadScalar(bindings, this, INPUT_D, null);
        graalAPI.loadScalar(bindings, this, INPUT_E, null);
        graalAPI.loadScalar(bindings, this, INPUT_F, null);
        graalAPI.loadNumbers(bindings, this, INPUT_X1, putNullBindingForUninitialized, X1_VARIABLE_ALT);
        graalAPI.loadNumbers(bindings, this, INPUT_X2, putNullBindingForUninitialized);
        graalAPI.loadNumbers(bindings, this, INPUT_X3, putNullBindingForUninitialized);
    }

    private void uploadStatus(GraalPerformer performer, GraalSourceContainer statusFormula) {
        final ExecutionBlock caller = getCaller();
        if (!(caller instanceof final Executor executor)) {
            return;
        }
        final Object status = performer.perform(statusFormula);
        if (status == null) {
            return;
        }
        executor.status().setMessageString(status.toString());
    }
}
