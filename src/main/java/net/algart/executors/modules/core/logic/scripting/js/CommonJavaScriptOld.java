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

import net.algart.bridges.standard.JavaScriptContextContainer;
import net.algart.bridges.standard.JavaScriptPerformer;
import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.ExecutorNotFoundException;
import net.algart.executors.api.system.InstantiationMode;

import javax.script.ScriptEngine;
import java.util.Locale;

@Deprecated
public final class CommonJavaScriptOld extends Executor {
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
            final net.algart.executors.api.system.ExecutorFactory executorFactory = executorFactory();
            final ExecutionBlock executionBlock;
            try {
                executionBlock = executorFactory.newExecutor(callableExecutorId, InstantiationMode.NORMAL);
            } catch (ClassNotFoundException | ExecutorNotFoundException e) {
                throw new IllegalStateException("Cannot initialize block with executor ID " + callableExecutorId
                        + (e instanceof ClassNotFoundException ?
                        " - Java class not found: " + e.getMessage() :
                        " - non-registered ID"),
                        e);
            }
            if (!(executionBlock instanceof Executor)) {
                throw new IllegalStateException("Unsupported executor class "
                        + executionBlock.getClass().getName() + ": it must be subclass of "
                        + Executor.class.getName() + " in " + this);
            }
            executionBlock.setAllOutputsNecessary(callableExecutorOutputsNecessaryAlways);
            return (Executor) executionBlock;
        }

        private net.algart.executors.api.system.ExecutorFactory executorFactory() {
            if (executorFactory == null) {
                executorFactory = globalExecutorLoaders().newFactory(sessionId);
            }
            return executorFactory;
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
    private boolean convertInputArraysToDouble = true;
    private boolean shareNamespace = false;

    private final JavaScriptContextContainer contextContainer = JavaScriptContextContainer.getInstance();
    private JavaScriptPerformer javaScriptInitializingOperator = null;
    private JavaScriptPerformer javaScriptFormula = null;
    private JavaScriptPerformer javaScriptResultA = null;
    private JavaScriptPerformer javaScriptResultB = null;
    private JavaScriptPerformer javaScriptResultC = null;
    private JavaScriptPerformer javaScriptResultD = null;
    private JavaScriptPerformer javaScriptResultE = null;
    private JavaScriptPerformer javaScriptResultF = null;
    private JavaScriptPerformer javaScriptResultG = null;
    private JavaScriptPerformer javaScriptResultH = null;
    private JavaScriptPerformer javaScriptResultX1 = null;
    private JavaScriptPerformer javaScriptResultX2 = null;
    private JavaScriptPerformer javaScriptResultX3 = null;
    private JavaScriptPerformer javaScriptResultM1 = null;
    private JavaScriptPerformer javaScriptResultM2 = null;
    private JavaScriptPerformer javaScriptResultM3 = null;
    private JavaScriptPerformer javaScriptResultM4 = null;
    private JavaScriptPerformer javaScriptResultM5 = null;
    private Executor callableExecutor1 = null;
    private Executor callableExecutor2 = null;
    private Executor callableExecutor3 = null;

    public CommonJavaScriptOld() {
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

    public CommonJavaScriptOld setInitializingOperator(String initializingOperator) {
        this.initializingOperator = nonNull(initializingOperator).trim();
        return this;
    }

    public String getFormula() {
        return formula;
    }

    public CommonJavaScriptOld setFormula(String formula) {
        this.formula = nonNull(formula).trim();
        return this;
    }

    public String getResultA() {
        return resultA;
    }

    public CommonJavaScriptOld setResultA(String resultA) {
        this.resultA = nonNull(resultA).trim();
        return this;
    }

    public String getResultB() {
        return resultB;
    }

    public CommonJavaScriptOld setResultB(String resultB) {
        this.resultB = nonNull(resultB).trim();
        return this;
    }

    public String getResultC() {
        return resultC;
    }

    public CommonJavaScriptOld setResultC(String resultC) {
        this.resultC = nonNull(resultC).trim();
        return this;
    }

    public String getResultD() {
        return resultD;
    }

    public CommonJavaScriptOld setResultD(String resultD) {
        this.resultD = nonNull(resultD).trim();
        return this;
    }

    public String getResultE() {
        return resultE;
    }

    public CommonJavaScriptOld setResultE(String resultE) {
        this.resultE = nonNull(resultE).trim();
        return this;
    }

    public String getResultF() {
        return resultF;
    }

    public CommonJavaScriptOld setResultF(String resultF) {
        this.resultF = nonNull(resultF).trim();
        return this;
    }

    public String getResultG() {
        return resultG;
    }

    public CommonJavaScriptOld setResultG(String resultG) {
        this.resultG = nonNull(resultG).trim();
        return this;
    }

    public String getResultH() {
        return resultH;
    }

    public CommonJavaScriptOld setResultH(String resultH) {
        this.resultH = nonNull(resultH).trim();
        return this;
    }

    public String getResultX1() {
        return resultX1;
    }

    public CommonJavaScriptOld setResultX1(String resultX1) {
        this.resultX1 = nonNull(resultX1).trim();
        return this;
    }

    public String getResultX2() {
        return resultX2;
    }

    public CommonJavaScriptOld setResultX2(String resultX2) {
        this.resultX2 = nonNull(resultX2).trim();
        return this;
    }

    public String getResultX3() {
        return resultX3;
    }

    public CommonJavaScriptOld setResultX3(String resultX3) {
        this.resultX3 = nonNull(resultX3).trim();
        return this;
    }

    public String getResultM1() {
        return resultM1;
    }

    public CommonJavaScriptOld setResultM1(String resultM1) {
        this.resultM1 = nonNull(resultM1).trim();
        return this;
    }

    public String getResultM2() {
        return resultM2;
    }

    public CommonJavaScriptOld setResultM2(String resultM2) {
        this.resultM2 = nonNull(resultM2).trim();
        return this;
    }

    public String getResultM3() {
        return resultM3;
    }

    public CommonJavaScriptOld setResultM3(String resultM3) {
        this.resultM3 = nonNull(resultM3).trim();
        return this;
    }

    public String getResultM4() {
        return resultM4;
    }

    public CommonJavaScriptOld setResultM4(String resultM4) {
        this.resultM4 = nonNull(resultM4).trim();
        return this;
    }

    public String getResultM5() {
        return resultM5;
    }

    public CommonJavaScriptOld setResultM5(String resultM5) {
        this.resultM5 = nonNull(resultM5).trim();
        return this;
    }

    public String getDefaultA() {
        return defaultA;
    }

    public CommonJavaScriptOld setDefaultA(String defaultA) {
        this.defaultA = defaultA;
        return this;
    }

    public String getDefaultB() {
        return defaultB;
    }

    public CommonJavaScriptOld setDefaultB(String defaultB) {
        this.defaultB = defaultB;
        return this;
    }

    public String getDefaultC() {
        return defaultC;
    }

    public CommonJavaScriptOld setDefaultC(String defaultC) {
        this.defaultC = defaultC;
        return this;
    }

    public String getDefaultD() {
        return defaultD;
    }

    public CommonJavaScriptOld setDefaultD(String defaultD) {
        this.defaultD = defaultD;
        return this;
    }

    public String getDefaultE() {
        return defaultE;
    }

    public CommonJavaScriptOld setDefaultE(String defaultE) {
        this.defaultE = defaultE;
        return this;
    }

    public String getDefaultF() {
        return defaultF;
    }

    public CommonJavaScriptOld setDefaultF(String defaultF) {
        this.defaultF = defaultF;
        return this;
    }

    public int getResultX1BlockLength() {
        return resultX1BlockLength;
    }

    public CommonJavaScriptOld setResultX1BlockLength(int resultX1BlockLength) {
        this.resultX1BlockLength = positive(resultX1BlockLength);
        return this;
    }

    public int getResultX2BlockLength() {
        return resultX2BlockLength;
    }

    public CommonJavaScriptOld setResultX2BlockLength(int resultX2BlockLength) {
        this.resultX2BlockLength = positive(resultX2BlockLength);
        return this;
    }

    public int getResultX3BlockLength() {
        return resultX3BlockLength;
    }

    public CommonJavaScriptOld setResultX3BlockLength(int resultX3BlockLength) {
        this.resultX3BlockLength = positive(resultX3BlockLength);
        return this;
    }

    public String getCallableExecutorId1() {
        return callableExecutorId1;
    }

    public CommonJavaScriptOld setCallableExecutorId1(String callableExecutorId1) {
        this.callableExecutorId1 = nonNull(callableExecutorId1).trim();
        return this;
    }

    public String getCallableExecutorId2() {
        return callableExecutorId2;
    }

    public CommonJavaScriptOld setCallableExecutorId2(String callableExecutorId2) {
        this.callableExecutorId2 = nonNull(callableExecutorId2).trim();
        return this;
    }

    public String getCallableExecutorId3() {
        return callableExecutorId3;
    }

    public CommonJavaScriptOld setCallableExecutorId3(String callableExecutorId3) {
        this.callableExecutorId3 = nonNull(callableExecutorId3).trim();
        return this;
    }

    public boolean isCallableExecutorOutputsNecessaryAlways() {
        return callableExecutorOutputsNecessaryAlways;
    }

    public CommonJavaScriptOld setCallableExecutorOutputsNecessaryAlways(boolean callableExecutorOutputsNecessaryAlways) {
        this.callableExecutorOutputsNecessaryAlways = callableExecutorOutputsNecessaryAlways;
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return convertInputArraysToDouble;
    }

    public CommonJavaScriptOld setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        this.convertInputArraysToDouble = convertInputArraysToDouble;
        return this;
    }

    public boolean isShareNamespace() {
        return shareNamespace;
    }

    public CommonJavaScriptOld setShareNamespace(boolean shareNamespace) {
        this.shareNamespace = shareNamespace;
        return this;
    }

    @Override
    public void initialize() {
        long t1 = debugTime();
        final ScriptEngine context = contextContainer.getContext(shareNamespace, getContextId());
        long t2 = debugTime();
        closeExecutors();
        ExecutorFactory executorFactory = new ExecutorFactory(getSessionId(), callableExecutorOutputsNecessaryAlways);
        context.put(CALLABLE_EXECUTOR_FACTORY_VARIABLE, executorFactory);
        if (!callableExecutorId1.isEmpty()) {
            callableExecutor1 = executorFactory.get(callableExecutorId1);
            context.put(CALLABLE_EXECUTOR_VARIABLE_1, callableExecutor1);
            context.put(CALLABLE_EXECUTOR_VARIABLE_1_ALT, callableExecutor1);
        }
        if (!callableExecutorId2.isEmpty()) {
            callableExecutor2 = executorFactory.get(callableExecutorId2);
            context.put(CALLABLE_EXECUTOR_VARIABLE_2, callableExecutor2);
        }
        if (!callableExecutorId3.isEmpty()) {
            callableExecutor3 = executorFactory.get(callableExecutorId3);
            context.put(CALLABLE_EXECUTOR_VARIABLE_3, callableExecutor3);
        }
        long t3 = debugTime();
        if (!initializingOperator.isEmpty()) {
            javaScriptInitializingOperator = getScript(initializingOperator, javaScriptInitializingOperator, context);
            putAllInputs(context, true);
            javaScriptInitializingOperator.perform();
        }
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript reset in %.5f ms:"
                        + " %.2f mcs getting context + %.2f mcs adding executors + %.2f mcs initializing script",
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3));
    }

    @Override
    public void process() {
        long t1 = debugTime();
        final ScriptEngine context = contextContainer.getContext(shareNamespace, getContextId());
        javaScriptFormula = getScript(formula, javaScriptFormula, context);
        javaScriptResultA = getScript(resultA, javaScriptResultA, context);
        javaScriptResultB = getScript(resultB, javaScriptResultB, context);
        javaScriptResultC = getScript(resultC, javaScriptResultC, context);
        javaScriptResultD = getScript(resultD, javaScriptResultD, context);
        javaScriptResultE = getScript(resultE, javaScriptResultE, context);
        javaScriptResultF = getScript(resultF, javaScriptResultF, context);
        javaScriptResultG = getScript(resultG, javaScriptResultG, context);
        javaScriptResultH = getScript(resultH, javaScriptResultH, context);
        javaScriptResultX1 = getScript(resultX1, javaScriptResultX1, context);
        javaScriptResultX2 = getScript(resultX2, javaScriptResultX2, context);
        javaScriptResultX3 = getScript(resultX3, javaScriptResultX3, context);
        javaScriptResultM1 = getScript(resultM1, javaScriptResultM1, context);
        javaScriptResultM2 = getScript(resultM2, javaScriptResultM2, context);
        javaScriptResultM3 = getScript(resultM3, javaScriptResultM3, context);
        javaScriptResultM4 = getScript(resultM4, javaScriptResultM4, context);
        javaScriptResultM5 = getScript(resultM5, javaScriptResultM5, context);
        long t2 = debugTime();
        putAllInputs(context, false);
        long t3 = debugTime();
        final String result = javaScriptFormula.calculateStringOrNumber();
        getScalar().setTo(result);
        long t4 = debugTime();
        getScalar(OUTPUT_A).setTo(javaScriptResultA.calculateStringOrNumber());
        getScalar(OUTPUT_B).setTo(javaScriptResultB.calculateStringOrNumber());
        getScalar(OUTPUT_C).setTo(javaScriptResultC.calculateStringOrNumber());
        getScalar(OUTPUT_D).setTo(javaScriptResultD.calculateStringOrNumber());
        getScalar(OUTPUT_E).setTo(javaScriptResultE.calculateStringOrNumber());
        getScalar(OUTPUT_F).setTo(javaScriptResultF.calculateStringOrNumber());
        getScalar(OUTPUT_G).setTo(javaScriptResultG.calculateStringOrNumber());
        getScalar(OUTPUT_H).setTo(javaScriptResultH.calculateStringOrNumber());
        uploadOutputNumbers(OUTPUT_X1, javaScriptResultX1, resultX1BlockLength);
        uploadOutputNumbers(OUTPUT_X2, javaScriptResultX2, resultX2BlockLength);
        uploadOutputNumbers(OUTPUT_X3, javaScriptResultX3, resultX3BlockLength);
        uploadOutputMat(OUTPUT_M1, javaScriptResultM1);
        uploadOutputMat(OUTPUT_M2, javaScriptResultM2);
        uploadOutputMat(OUTPUT_M3, javaScriptResultM3);
        uploadOutputMat(OUTPUT_M4, javaScriptResultM4);
        uploadOutputMat(OUTPUT_M5, javaScriptResultM5);
        long t5 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript \"%s\" executed in %.5f ms:" +
                        " %.2f mcs compiling + %.2f mcs adding vars + %.2f mcs main script + " +
                        "%.2f mcs additional " +
                        "outputs (%d stored actual script engines)",
                scriptToShortString(formula),
                (t5 - t1) * 1e-6,
                (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, (t5 - t4) * 1e-3,
                JavaScriptContextContainer.numberOfStoredScriptEngines()));
    }

    @Override
    public void close() {
        super.close();
        closeExecutors();
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

    private void putAllInputs(ScriptEngine context, boolean alsoUninitialized) {
        putInputScalar(context, INPUT_A, defaultA);
        putInputScalar(context, INPUT_B, defaultB);
        putInputScalar(context, INPUT_C, defaultC);
        putInputScalar(context, INPUT_D, defaultD);
        putInputScalar(context, INPUT_E, defaultE);
        putInputScalar(context, INPUT_F, defaultF);
        putInputNumbers(context, INPUT_X1, X1_VARIABLE_ALT, alsoUninitialized);
        putInputNumbers(context, INPUT_X2, null, alsoUninitialized);
        putInputNumbers(context, INPUT_X3, null, alsoUninitialized);
        putInputMat(context, INPUT_M1, M1_VARIABLE_ALT, alsoUninitialized);
        putInputMat(context, INPUT_M2, null, alsoUninitialized);
        putInputMat(context, INPUT_M3, null, alsoUninitialized);
        putInputMat(context, INPUT_M4, null, alsoUninitialized);
        putInputMat(context, INPUT_M5, null, alsoUninitialized);
    }

    private void putInputScalar(ScriptEngine context, String inputPortName, String defaultValue) {
        SScalar scalar = getInputScalar(inputPortName, true);
        context.put(inputPortName, parseDoubleIfPossible(scalar.getValueOrDefault(defaultValue)));
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

    private void putInputMat(ScriptEngine context, String inputPortName, String altName, boolean alsoUninitialized) {
        SMat mat = getInputMat(inputPortName, true);
        final SMat value = mat.isInitialized() ? mat : null;
        if (alsoUninitialized || value != null) {
            context.put(inputPortName, value);
            if (altName != null) {
                context.put(altName, value);
            }
        }
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

    private void uploadOutputMat(String outputPortName, JavaScriptPerformer resultFormula) {
        final Object result = resultFormula.perform();
        if (result != null) {
            getMat(outputPortName).setTo((SMat) result);
        }
    }

    private static Object parseDoubleIfPossible(String s) {
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private static JavaScriptPerformer getScript(
            String formula,
            JavaScriptPerformer previous,
            ScriptEngine context) {
        return JavaScriptPerformer.newInstanceIfChanged(formula, previous, context);
    }

    private static String scriptToShortString(String script) {
        if (script.length() > 30) {
            script = script.substring(0, 30) + "...";
        }
        return script.replaceAll("(?:\\r(?!\\n)|\\n|\\r\\n)", " \\\\n ");
    }
}
