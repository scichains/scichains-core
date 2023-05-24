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

package net.algart.executors.modules.core.logic.scripting.python;

import net.algart.bridges.jep.JepPerformer;
import net.algart.bridges.jep.JepPerformerContainer;
import net.algart.bridges.jep.additions.AtomicPyObject;
import net.algart.bridges.jep.additions.JepInterpreterKind;
import net.algart.bridges.jep.api.JepPlatforms;
import net.algart.bridges.jep.api.JepAPI;
import net.algart.executors.api.Executor;
import net.algart.executors.api.Port;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Should be public for normal using setters in PropertySetter
public abstract class AbstractCallPython extends Executor {
    private static final List<String> PARAMETERS_NAMES = List.of(
            "a", "b", "c", "d", "e", "f", "p", "q", "r", "s", "t", "u");
    private static final List<String> INPUTS_NAMES = List.of(
            "x1", "x2", "x3", "x4", "x5", "m1", "m2", "m3", "m4", "m5");
    private static final List<String> OUTPUTS_NAMES = List.of(
            DEFAULT_OUTPUT_PORT,
            "a", "b", "c", "d", "e", "f", "x1", "x2", "x3", "x4", "x5", "m1", "m2", "m3", "m4", "m5");

    public enum CompilerKind {
        JEP_LOCAL(e -> e.localContainer),
        JEP_SHARED(e -> e.sharedContainer);

        private final Function<AbstractCallPython, JepPerformerContainer> containerSupplier;

        CompilerKind(Function<AbstractCallPython, JepPerformerContainer> containerSupplier) {
            this.containerSupplier = containerSupplier;
        }
    }

    public static final String INPUT_X1 = "x1";
    public static final String INPUT_X2 = "x2";
    public static final String INPUT_X3 = "x3";
    public static final String INPUT_X4 = "x4";
    public static final String INPUT_X5 = "x5";
    public static final String INPUT_M1 = "m1";
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
    public static final String OUTPUT_X1 = "x1";
    public static final String OUTPUT_X2 = "x2";
    public static final String OUTPUT_X3 = "x3";
    public static final String OUTPUT_X4 = "x4";
    public static final String OUTPUT_X5 = "x5";
    public static final String OUTPUT_M1 = "m1";
    public static final String OUTPUT_M2 = "m2";
    public static final String OUTPUT_M3 = "m3";
    public static final String OUTPUT_M4 = "m4";
    public static final String OUTPUT_M5 = "m5";
    public static final String OUTPUT_SUPPLIED_PYTHON_ROOTS = "supplied_python_roots";

    private String mainFunctionName = "execute";
    private String paramsClassName = "";
    private String inputsClassName = "";
    private String outputsClassName = "";
    private String a = "";
    private String b = "";
    private String c = "";
    private String d = "";
    private String e = "";
    private String f = "";
    private double p = 0.0;
    private double q = 0.0;
    private double r = 0.0;
    private double s = 0.0;
    private double t = 0.0;
    private double u = 0.0;
    private final JepAPI jepAPI = JepAPI.getInstance();
    private CompilerKind compilerKind = CompilerKind.JEP_SHARED;

    final JepPerformerContainer sharedContainer = JepAPI.getContainer();
    final JepPerformerContainer localContainer = JepAPI.getContainer(JepInterpreterKind.LOCAL);
    private JepPerformer performer = null;

    public AbstractCallPython() {
        useVisibleResultParameter();
        addInputNumbers(INPUT_X1);
        addInputNumbers(INPUT_X2);
        addInputNumbers(INPUT_X3);
        addInputNumbers(INPUT_X4);
        addInputNumbers(INPUT_X5);
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
        addOutputNumbers(OUTPUT_X1);
        addOutputNumbers(OUTPUT_X2);
        addOutputNumbers(OUTPUT_X3);
        addOutputNumbers(OUTPUT_X4);
        addOutputNumbers(OUTPUT_X5);
        addOutputMat(OUTPUT_M1);
        addOutputMat(OUTPUT_M2);
        addOutputMat(OUTPUT_M3);
        addOutputMat(OUTPUT_M4);
        addOutputMat(OUTPUT_M5);
        addOutputScalar(OUTPUT_SUPPLIED_PYTHON_ROOTS);
    }

    public final String getMainFunctionName() {
        return mainFunctionName;
    }

    public final AbstractCallPython setMainFunctionName(String mainFunctionName) {
        this.mainFunctionName = nonEmptyTrimmed(mainFunctionName).trim();
        return this;
    }

    public final String getParamsClassName() {
        return paramsClassName;
    }

    public final AbstractCallPython setParamsClassName(String paramsClassName) {
        this.paramsClassName = nonNull(paramsClassName).trim();
        return this;
    }

    public final String getInputsClassName() {
        return inputsClassName;
    }

    public final AbstractCallPython setInputsClassName(String inputsClassName) {
        this.inputsClassName = nonNull(inputsClassName).trim();
        return this;
    }

    public final String getOutputsClassName() {
        return outputsClassName;
    }

    public final AbstractCallPython setOutputsClassName(String outputsClassName) {
        this.outputsClassName = nonNull(outputsClassName).trim();
        return this;
    }

    public final String getA() {
        return a;
    }

    public final AbstractCallPython setA(String a) {
        this.a = a;
        return this;
    }

    public final String getB() {
        return b;
    }

    public final AbstractCallPython setB(String b) {
        this.b = b;
        return this;
    }

    public final String getC() {
        return c;
    }

    public final AbstractCallPython setC(String c) {
        this.c = c;
        return this;
    }

    public final String getD() {
        return d;
    }

    public final AbstractCallPython setD(String d) {
        this.d = d;
        return this;
    }

    public final String getE() {
        return e;
    }

    public final AbstractCallPython setE(String e) {
        this.e = e;
        return this;
    }

    public final String getF() {
        return f;
    }

    public final AbstractCallPython setF(String f) {
        this.f = f;
        return this;
    }

    public final double getP() {
        return p;
    }

    public final AbstractCallPython setP(double p) {
        this.p = p;
        return this;
    }

    public final double getQ() {
        return q;
    }

    public final AbstractCallPython setQ(double q) {
        this.q = q;
        return this;
    }

    public final double getR() {
        return r;
    }

    public final AbstractCallPython setR(double r) {
        this.r = r;
        return this;
    }

    public final double getS() {
        return s;
    }

    public final AbstractCallPython setS(double s) {
        this.s = s;
        return this;
    }

    public final double getT() {
        return t;
    }

    public final AbstractCallPython setT(double t) {
        this.t = t;
        return this;
    }

    public final double getU() {
        return u;
    }

    public final AbstractCallPython setU(double u) {
        this.u = u;
        return this;
    }

    public final CompilerKind getCompilerKind() {
        return compilerKind;
    }

    public final AbstractCallPython setCompilerKind(CompilerKind compilerKind) {
        this.compilerKind = nonNull(compilerKind);
        return this;
    }

    public final String paramsClassName() {
        return paramsClassName.isEmpty() ? JepAPI.STANDARD_API_PARAMETERS_CLASS_NAME : paramsClassName;
    }

    public final String inputsClassName() {
        return inputsClassName.isEmpty() ? JepAPI.STANDARD_API_INPUTS_CLASS_NAME : inputsClassName;
    }

    public final String outputsClassName() {
        return outputsClassName.isEmpty() ? JepAPI.STANDARD_API_OUTPUTS_CLASS_NAME : outputsClassName;
    }

    @Override
    public final void initialize() {
        long t1 = debugTime();
        //noinspection resource
        performer = container().performer();
        long t2 = debugTime();
        final String code = code();
        if (!code.isEmpty()) {
            performer.perform(code);
        }
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Python reset in %.3f ms: %.6f ms getting context + %.6f ms initializing code",
                (t3 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
    }

    @Override
    public final void process() {
        long t1 = debugTime(), t2, t3, t4;
        if (performer == null) {
            throw new IllegalStateException(getClass() + " is not initialized");
        }
        final Object result;
        try (AtomicPyObject pythonParams = jepAPI.newAPIObject(performer, paramsClassName());
             AtomicPyObject pythonInputs = jepAPI.newAPIObject(performer, inputsClassName());
             AtomicPyObject pythonOutputs = jepAPI.newAPIObject(performer, outputsClassName())) {
            jepAPI.loadParameters(subMap(parameters(), PARAMETERS_NAMES), pythonParams);
            jepAPI.loadSystemParameters(this, pythonParams);
            jepAPI.readInputPorts(performer, subSet(allInputPorts(), INPUTS_NAMES), pythonInputs);
            t2 = debugTime();
            result = performer.invokeFunction(mainFunctionName,
                    pythonParams.pyObject(),
                    pythonInputs.pyObject(),
                    pythonOutputs.pyObject());
            t3 = debugTime();
            jepAPI.writeOutputPorts(performer, subSet(allOutputPorts(), OUTPUTS_NAMES), pythonOutputs);
            jepAPI.writeOutputPort(performer, getOutputPort(DEFAULT_OUTPUT_PORT), result, true);
            // - note: direct assignment "outputs.output = xxx" overrides simple returning result
            t4 = debugTime();
        }
        getScalar(OUTPUT_SUPPLIED_PYTHON_ROOTS).setTo(
                String.join(String.format("%n"), JepPlatforms.pythonRootFolders()));
        logDebug(() -> String.format(Locale.US,
                "%s \"%s\" executed in %.3f ms:"
                        + " %.6f ms loading inputs + %.6f ms calling + %.6f ms returning outputs",
                executorName(), mainFunctionName,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    @Override
    public final void close() {
        super.close();
        sharedContainer.close();;
        localContainer.close();;
    }

    protected abstract String code();

    protected abstract String executorName();

    private JepPerformerContainer container() {
        return compilerKind.containerSupplier.apply(this);
    }

    private static <K, V> Map<K, V> subMap(Map<K, V> map, Collection<K> requestedKeys) {
        final Map<K, V> result = new LinkedHashMap<>();
        for (K key : requestedKeys) {
            final V value = map.get(key);
            if (value != null) {
                // - may be null when called directly from Java code
                result.put(key, value);
            }
        }
        return result;
    }

    private static Collection<Port> subSet(Collection<Port> ports, Collection<String> requested) {
        return subMap(ports.stream().collect(Collectors.toMap(Port::getName, p1 -> p1)), requested).values();
    }
}
