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

package net.algart.executors.modules.core.logic.scripting.js;

import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalAPI;
import net.algart.bridges.graalvm.api.GraalSafety;
import net.algart.bridges.standard.JavaScriptContextContainer;
import net.algart.executors.api.Executor;
import org.graalvm.polyglot.Value;

import java.util.Locale;

public final class CommonJS extends Executor {
    public static final String CALLABLE_EXECUTOR_FACTORY_VARIABLE = "executorFactory";
    public static final String CALLABLE_EXECUTOR_VARIABLE_1_ALT = "exec";
    public static final String CALLABLE_EXECUTOR_VARIABLE_1 = "exec1";
    public static final String CALLABLE_EXECUTOR_VARIABLE_2 = "exec2";
    public static final String CALLABLE_EXECUTOR_VARIABLE_3 = "exec3";
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
    public static final String INPUT_M1 = "m1";
    public static final String M1_VARIABLE_ALT = "m";
    public static final String INPUT_M2 = "m2";
    public static final String INPUT_M3 = "m3";
    public static final String INPUT_M4 = "m4";
    public static final String INPUT_M5 = "m5";
    public static final String OUTPUT_A = "a";
    public static final String OUTPUT_B = "b";
    public static final String OUTPUT_C = "c";
    public static final String OUTPUT_D = "d";
    public static final String OUTPUT_E = "e";
    public static final String OUTPUT_F = "f";
    public static final String OUTPUT_G = "g";
    public static final String OUTPUT_H = "h";
    public static final String OUTPUT_X1 = "x1";
    public static final String OUTPUT_X2 = "x2";
    public static final String OUTPUT_X3 = "x3";
    public static final String OUTPUT_M1 = "m1";
    public static final String OUTPUT_M2 = "m2";
    public static final String OUTPUT_M3 = "m3";
    public static final String OUTPUT_M4 = "m4";
    public static final String OUTPUT_M5 = "m5";

    public static class ExecutorFactory {
        private final String sessionId;
        private final boolean callableExecutorOutputsNecessaryAlways;
        private net.algart.executors.api.system.ExecutorFactory executorFactory = null;

        private ExecutorFactory(String sessionId, boolean callableExecutorOutputsNecessaryAlways) {
            this.sessionId = sessionId;
            this.callableExecutorOutputsNecessaryAlways = callableExecutorOutputsNecessaryAlways;
        }

        public Executor get(String callableExecutorId) {
            final Executor result = factory().newExecutor(Executor.class, callableExecutorId);
            result.setAllOutputsNecessary(callableExecutorOutputsNecessaryAlways);
            return result;
        }

        public net.algart.executors.api.system.ExecutorFactory factory() {
            if (this.executorFactory == null) {
                this.executorFactory = globalLoaders().newFactory(sessionId);
            }
            return this.executorFactory;
        }
    }

    private String initializingOperator = "";
    private String formula = "a";
    private String resultA = "";
    private String resultB = "";
    private String resultC = "";
    private String resultD = "";
    private String resultE = "";
    private String resultF = "";
    private String resultG = "";
    private String resultH = "";
    private String resultX1 = "";
    private String resultX2 = "";
    private String resultX3 = "";
    private String resultM1 = "";
    private String resultM2 = "";
    private String resultM3 = "";
    private String resultM4 = "";
    private String resultM5 = "";
    private String defaultA = "0";
    private String defaultB = "0";
    private String defaultC = "0";
    private String defaultD = "0";
    private String defaultE = "0";
    private String defaultF = "0";
    private int resultX1BlockLength = 1;
    private int resultX2BlockLength = 1;
    private int resultX3BlockLength = 1;
    private String callableExecutorId1 = "";
    private String callableExecutorId2 = "";
    private String callableExecutorId3 = "";
    private boolean callableExecutorOutputsNecessaryAlways = true;
    private final GraalAPI graalAPI = GraalAPI.getSmartScriptingInstance();
    private GraalSafety safety = GraalSafety.SAFE;
    private boolean shareNamespace = false;
    private boolean closeSharedContext = true;

    private GraalPerformerContainer performerContainer = null;
    private final GraalSourceContainer javaScriptInitializingOperator = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptFormula = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultA = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultB = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultC = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultD = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultE = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultF = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultG = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultH = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX1 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX2 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultX3 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultM1 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultM2 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultM3 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultM4 = GraalSourceContainer.newLiteral();
    private final GraalSourceContainer javaScriptResultM5 = GraalSourceContainer.newLiteral();
    private Executor callableExecutor1 = null;
    private Executor callableExecutor2 = null;
    private Executor callableExecutor3 = null;

    private final Object lock = new Object();

    public CommonJS() {
        addInputScalar(INPUT_A);
        addInputScalar(INPUT_B);
        addInputScalar(INPUT_C);
        addInputScalar(INPUT_D);
        addInputScalar(INPUT_E);
        addInputScalar(INPUT_F);
        addInputNumbers(INPUT_X1);
        addInputNumbers(INPUT_X2);
        addInputNumbers(INPUT_X3);
        addInputMat(INPUT_M1);
        addInputMat(INPUT_M2);
        addInputMat(INPUT_M3);
        addInputMat(INPUT_M4);
        addInputMat(INPUT_M5);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_A);
        addOutputScalar(OUTPUT_B);
        addOutputScalar(OUTPUT_C);
        addOutputScalar(OUTPUT_D);
        addOutputScalar(OUTPUT_E);
        addOutputScalar(OUTPUT_F);
        addOutputScalar(OUTPUT_G);
        addOutputScalar(OUTPUT_H);
        addOutputNumbers(OUTPUT_X1);
        addOutputNumbers(OUTPUT_X2);
        addOutputNumbers(OUTPUT_X3);
        addOutputMat(OUTPUT_M1);
        addOutputMat(OUTPUT_M2);
        addOutputMat(OUTPUT_M3);
        addOutputMat(OUTPUT_M4);
        addOutputMat(OUTPUT_M5);
    }

    public String getInitializingOperator() {
        return initializingOperator;
    }

    public CommonJS setInitializingOperator(String initializingOperator) {
        this.initializingOperator = nonNull(initializingOperator).trim();
        return this;
    }

    public String getFormula() {
        return formula;
    }

    public CommonJS setFormula(String formula) {
        this.formula = nonNull(formula).trim();
        return this;
    }

    public String getResultA() {
        return resultA;
    }

    public CommonJS setResultA(String resultA) {
        this.resultA = nonNull(resultA).trim();
        return this;
    }

    public String getResultB() {
        return resultB;
    }

    public CommonJS setResultB(String resultB) {
        this.resultB = nonNull(resultB).trim();
        return this;
    }

    public String getResultC() {
        return resultC;
    }

    public CommonJS setResultC(String resultC) {
        this.resultC = nonNull(resultC).trim();
        return this;
    }

    public String getResultD() {
        return resultD;
    }

    public CommonJS setResultD(String resultD) {
        this.resultD = nonNull(resultD).trim();
        return this;
    }

    public String getResultE() {
        return resultE;
    }

    public CommonJS setResultE(String resultE) {
        this.resultE = nonNull(resultE).trim();
        return this;
    }

    public String getResultF() {
        return resultF;
    }

    public CommonJS setResultF(String resultF) {
        this.resultF = nonNull(resultF).trim();
        return this;
    }

    public String getResultG() {
        return resultG;
    }

    public CommonJS setResultG(String resultG) {
        this.resultG = nonNull(resultG).trim();
        return this;
    }

    public String getResultH() {
        return resultH;
    }

    public CommonJS setResultH(String resultH) {
        this.resultH = nonNull(resultH).trim();
        return this;
    }

    public String getResultX1() {
        return resultX1;
    }

    public CommonJS setResultX1(String resultX1) {
        this.resultX1 = nonNull(resultX1).trim();
        return this;
    }

    public String getResultX2() {
        return resultX2;
    }

    public CommonJS setResultX2(String resultX2) {
        this.resultX2 = nonNull(resultX2).trim();
        return this;
    }

    public String getResultX3() {
        return resultX3;
    }

    public CommonJS setResultX3(String resultX3) {
        this.resultX3 = nonNull(resultX3).trim();
        return this;
    }

    public String getResultM1() {
        return resultM1;
    }

    public CommonJS setResultM1(String resultM1) {
        this.resultM1 = nonNull(resultM1).trim();
        return this;
    }

    public String getResultM2() {
        return resultM2;
    }

    public CommonJS setResultM2(String resultM2) {
        this.resultM2 = nonNull(resultM2).trim();
        return this;
    }

    public String getResultM3() {
        return resultM3;
    }

    public CommonJS setResultM3(String resultM3) {
        this.resultM3 = nonNull(resultM3).trim();
        return this;
    }

    public String getResultM4() {
        return resultM4;
    }

    public CommonJS setResultM4(String resultM4) {
        this.resultM4 = nonNull(resultM4).trim();
        return this;
    }

    public String getResultM5() {
        return resultM5;
    }

    public CommonJS setResultM5(String resultM5) {
        this.resultM5 = nonNull(resultM5).trim();
        return this;
    }

    public String getDefaultA() {
        return defaultA;
    }

    public CommonJS setDefaultA(String defaultA) {
        this.defaultA = defaultA;
        return this;
    }

    public String getDefaultB() {
        return defaultB;
    }

    public CommonJS setDefaultB(String defaultB) {
        this.defaultB = defaultB;
        return this;
    }

    public String getDefaultC() {
        return defaultC;
    }

    public CommonJS setDefaultC(String defaultC) {
        this.defaultC = defaultC;
        return this;
    }

    public String getDefaultD() {
        return defaultD;
    }

    public CommonJS setDefaultD(String defaultD) {
        this.defaultD = defaultD;
        return this;
    }

    public String getDefaultE() {
        return defaultE;
    }

    public CommonJS setDefaultE(String defaultE) {
        this.defaultE = defaultE;
        return this;
    }

    public String getDefaultF() {
        return defaultF;
    }

    public CommonJS setDefaultF(String defaultF) {
        this.defaultF = defaultF;
        return this;
    }

    public int getResultX1BlockLength() {
        return resultX1BlockLength;
    }

    public CommonJS setResultX1BlockLength(int resultX1BlockLength) {
        this.resultX1BlockLength = positive(resultX1BlockLength);
        return this;
    }

    public int getResultX2BlockLength() {
        return resultX2BlockLength;
    }

    public CommonJS setResultX2BlockLength(int resultX2BlockLength) {
        this.resultX2BlockLength = positive(resultX2BlockLength);
        return this;
    }

    public int getResultX3BlockLength() {
        return resultX3BlockLength;
    }

    public CommonJS setResultX3BlockLength(int resultX3BlockLength) {
        this.resultX3BlockLength = positive(resultX3BlockLength);
        return this;
    }

    public String getCallableExecutorId1() {
        return callableExecutorId1;
    }

    public CommonJS setCallableExecutorId1(String callableExecutorId1) {
        this.callableExecutorId1 = nonNull(callableExecutorId1).trim();
        return this;
    }

    public String getCallableExecutorId2() {
        return callableExecutorId2;
    }

    public CommonJS setCallableExecutorId2(String callableExecutorId2) {
        this.callableExecutorId2 = nonNull(callableExecutorId2).trim();
        return this;
    }

    public String getCallableExecutorId3() {
        return callableExecutorId3;
    }

    public CommonJS setCallableExecutorId3(String callableExecutorId3) {
        this.callableExecutorId3 = nonNull(callableExecutorId3).trim();
        return this;
    }

    public boolean isCallableExecutorOutputsNecessaryAlways() {
        return callableExecutorOutputsNecessaryAlways;
    }

    public CommonJS setCallableExecutorOutputsNecessaryAlways(boolean callableExecutorOutputsNecessaryAlways) {
        this.callableExecutorOutputsNecessaryAlways = callableExecutorOutputsNecessaryAlways;
        return this;
    }

    public boolean isConvertInputScalarToNumber() {
        return graalAPI.isConvertInputScalarToNumber();
    }

    public CommonJS setConvertInputScalarToNumber(boolean convertInputScalarToNumber) {
        graalAPI.setConvertInputScalarToNumber(convertInputScalarToNumber);
        return this;
    }

    public boolean isConvertInputNumbersToArray() {
        return graalAPI.isConvertInputNumbersToArray();
    }

    public CommonJS setConvertInputNumbersToArray(boolean convertInputNumbersToArray) {
        graalAPI.setConvertInputNumbersToArray(convertInputNumbersToArray);
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return graalAPI.isConvertInputArraysToDouble();
    }

    public CommonJS setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        graalAPI.setConvertInputArraysToDouble(convertInputArraysToDouble);
        return this;
    }

    public boolean isConvertOutputIntegerToBriefForm() {
        return graalAPI.isConvertOutputIntegerToBriefForm();
    }

    public CommonJS setConvertOutputIntegersToBriefForm(boolean convertOutputIntegersToBriefForm) {
        graalAPI.setConvertOutputIntegersToBriefForm(convertOutputIntegersToBriefForm);
        return this;
    }

    public GraalSafety getSafety() {
        return safety;
    }

    public CommonJS setSafety(GraalSafety safety) {
        nonNull(safety);
        if (safety != this.safety) {
            closePerformerContainer();
            this.safety = safety;
        }
        return this;
    }

    public boolean isShareNamespace() {
        return shareNamespace;
    }

    public CommonJS setShareNamespace(boolean shareNamespace) {
        if (shareNamespace != this.shareNamespace) {
            closePerformerContainer();
            this.shareNamespace = shareNamespace;
        }
        return this;
    }

    public boolean isCloseSharedContext() {
        return closeSharedContext;
    }

    public CommonJS setCloseSharedContext(boolean closeSharedContext) {
        this.closeSharedContext = closeSharedContext;
        return this;
    }

    @Override
    public void initialize() {
        long t1 = debugTime();
        final GraalPerformer performer = performerContainer().performer(getContextId());
        long t2 = debugTime();
        closeExecutors();
        ExecutorFactory executorFactory = new ExecutorFactory(getSessionId(), callableExecutorOutputsNecessaryAlways);
        final Value bindings = performer.bindingsJS();
        bindings.putMember(CALLABLE_EXECUTOR_FACTORY_VARIABLE, executorFactory);
        if (!callableExecutorId1.isEmpty()) {
            callableExecutor1 = executorFactory.get(callableExecutorId1);
            bindings.putMember(CALLABLE_EXECUTOR_VARIABLE_1, callableExecutor1);
            bindings.putMember(CALLABLE_EXECUTOR_VARIABLE_1_ALT, callableExecutor1);
        }
        if (!callableExecutorId2.isEmpty()) {
            callableExecutor2 = executorFactory.get(callableExecutorId2);
            bindings.putMember(CALLABLE_EXECUTOR_VARIABLE_2, callableExecutor2);
        }
        if (!callableExecutorId3.isEmpty()) {
            callableExecutor3 = executorFactory.get(callableExecutorId3);
            bindings.putMember(CALLABLE_EXECUTOR_VARIABLE_3, callableExecutor3);
        }
        long t3 = debugTime();
        if (!initializingOperator.isEmpty()) {
            javaScriptInitializingOperator.setCommonJS(initializingOperator);
            putAllInputs(bindings, true);
            performer.perform(javaScriptInitializingOperator);
        }
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript reset in %.5f ms:"
                        + " %.2f mcs getting performer + %.2f mcs adding executors + %.2f mcs initializing script",
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
    }

    @Override
    public void process() {
        long t1 = debugTime();
        @SuppressWarnings("resource") final GraalPerformer performer = performerContainer().performer(getContextId());
//        System.out.println("!!! " + graalAPI.createEmptyObjectJSFunction(performer).execute());
        javaScriptFormula.setCommonJS(formula);
        javaScriptResultA.setCommonJS(resultA);
        javaScriptResultB.setCommonJS(resultB);
        javaScriptResultC.setCommonJS(resultC);
        javaScriptResultD.setCommonJS(resultD);
        javaScriptResultE.setCommonJS(resultE);
        javaScriptResultF.setCommonJS(resultF);
        javaScriptResultG.setCommonJS(resultG);
        javaScriptResultH.setCommonJS(resultH);
        javaScriptResultX1.setCommonJS(resultX1);
        javaScriptResultX2.setCommonJS(resultX2);
        javaScriptResultX3.setCommonJS(resultX3);
        javaScriptResultM1.setCommonJS(resultM1);
        javaScriptResultM2.setCommonJS(resultM2);
        javaScriptResultM3.setCommonJS(resultM3);
        javaScriptResultM4.setCommonJS(resultM4);
        javaScriptResultM5.setCommonJS(resultM5);
        long t2 = debugTime();
        putAllInputs(performer.bindingsJS(), false);
        long t3 = debugTime();
        final Value result = performer.perform(javaScriptFormula);
        graalAPI.storeScalar(this, defaultOutputPortName(), result);
        long t4 = debugTime();
        graalAPI.storeScalar(this, OUTPUT_A, performer.perform(javaScriptResultA));
        graalAPI.storeScalar(this, OUTPUT_B, performer.perform(javaScriptResultB));
        graalAPI.storeScalar(this, OUTPUT_C, performer.perform(javaScriptResultC));
        graalAPI.storeScalar(this, OUTPUT_D, performer.perform(javaScriptResultD));
        graalAPI.storeScalar(this, OUTPUT_E, performer.perform(javaScriptResultE));
        graalAPI.storeScalar(this, OUTPUT_F, performer.perform(javaScriptResultF));
        graalAPI.storeScalar(this, OUTPUT_H, performer.perform(javaScriptResultH));
        graalAPI.storeNumbers(this, OUTPUT_X1, performer.perform(javaScriptResultX1), resultX1BlockLength);
        graalAPI.storeNumbers(this, OUTPUT_X2, performer.perform(javaScriptResultX2), resultX2BlockLength);
        graalAPI.storeNumbers(this, OUTPUT_X3, performer.perform(javaScriptResultX3), resultX3BlockLength);
        graalAPI.storeMat(this, OUTPUT_M1, performer.perform(javaScriptResultM1));
        graalAPI.storeMat(this, OUTPUT_M2, performer.perform(javaScriptResultM2));
        graalAPI.storeMat(this, OUTPUT_M3, performer.perform(javaScriptResultM3));
        graalAPI.storeMat(this, OUTPUT_M4, performer.perform(javaScriptResultM4));
        graalAPI.storeMat(this, OUTPUT_M5, performer.perform(javaScriptResultM5));
        long t5 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript \"%s\" executed in %.5f ms:" +
                        " %.2f mcs compiling + %.2f mcs adding vars + %.2f mcs main script + " +
                        "%.2f mcs additional outputs (%d stored actual script engines)",
                scriptToShortString(formula),
                (t5 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, (t5 - t4) * 1e-3,
                JavaScriptContextContainer.numberOfStoredScriptEngines()));
    }

    @Override
    public void close() {
        super.close();
        closeExecutors();
        closePerformerContainer();
    }

    private void closeExecutors() {
        if (callableExecutor3 != null) {
            callableExecutor3.close();
            callableExecutor3 = null;
        }
        if (callableExecutor2 != null) {
            callableExecutor2.close();
            callableExecutor2 = null;
        }
        if (callableExecutor1 != null) {
            callableExecutor1.close();
            callableExecutor1 = null;
        }
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
                this.performerContainer = GraalAPI.getJSContainer(shareNamespace, safety);
                // - we should re-create the container here, because different shareNamespace
                // values correspond to different subclasses of GraalPerformerContainer
            }
            return performerContainer;
        }
    }

    private void putAllInputs(Value bindings, boolean putNullBindingForUninitialized) {
        graalAPI.loadScalar(bindings, this, INPUT_A, defaultA);
        graalAPI.loadScalar(bindings, this, INPUT_B, defaultB);
        graalAPI.loadScalar(bindings, this, INPUT_C, defaultC);
        graalAPI.loadScalar(bindings, this, INPUT_D, defaultD);
        graalAPI.loadScalar(bindings, this, INPUT_E, defaultE);
        graalAPI.loadScalar(bindings, this, INPUT_F, defaultF);
        graalAPI.loadNumbers(bindings, this, INPUT_X1, putNullBindingForUninitialized, X1_VARIABLE_ALT);
        graalAPI.loadNumbers(bindings, this, INPUT_X2, putNullBindingForUninitialized);
        graalAPI.loadNumbers(bindings, this, INPUT_X3, putNullBindingForUninitialized);
        graalAPI.loadMat(bindings, this, INPUT_M1, putNullBindingForUninitialized, M1_VARIABLE_ALT);
        graalAPI.loadMat(bindings, this, INPUT_M2, putNullBindingForUninitialized);
        graalAPI.loadMat(bindings, this, INPUT_M3, putNullBindingForUninitialized);
        graalAPI.loadMat(bindings, this, INPUT_M4, putNullBindingForUninitialized);
        graalAPI.loadMat(bindings, this, INPUT_M5, putNullBindingForUninitialized);
    }

    private static String scriptToShortString(String script) {
        if (script.length() > 30) {
            script = script.substring(0, 30) + "...";
        }
        return script.replaceAll("(?:\\r(?!\\n)|\\n|\\r\\n)", " \\\\n ");
    }
}
