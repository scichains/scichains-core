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

import net.algart.graalvm.GraalPerformer;
import net.algart.graalvm.GraalPerformerContainer;
import net.algart.graalvm.GraalSourceContainer;
import net.algart.executors.api.graalvm.GraalAPI;
import net.algart.executors.api.graalvm.GraalSafety;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.Port;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class CallJSFunction extends Executor {
    private static final List<String> PARAMETERS_NAMES = List.of(
            "a", "b", "c", "d", "e", "f", "p", "q", "r", "s", "t", "u");
    private static final List<String> INPUTS_NAMES = List.of(
            "x1", "x2", "x3", "x4", "x5", "m1", "m2", "m3", "m4", "m5");
    private static final List<String> OUTPUTS_NAMES = List.of(
            DEFAULT_OUTPUT_PORT,
            "a", "b", "c", "d", "e", "f", "x1", "x2", "x3", "x4", "x5", "m1", "m2", "m3", "m4", "m5");

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

    private String code =
            "function execute(params, inputs, outputs) {\n" +
                    "    return \"Hello from JavaScript function!\"\n" +
                    "}\n";
    private String mainFunctionName = "execute";
    private String workingDirectory = ".";
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
    private final GraalAPI graalAPI = GraalAPI.getInstance();
    private GraalSafety safety = GraalSafety.SAFE;

    private final GraalPerformerContainer.Local performerContainer = GraalPerformerContainer.getLocalPure();
    private final GraalSourceContainer javaScriptCode = GraalSourceContainer.newLiteral();
    private volatile Path translatedDirectory = null;
    private volatile Value mainFunction = null;
    private volatile Value createEmptyObjectFunction = null;

    private final Object lock = new Object();

    public CallJSFunction() {
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
    }

    public String getCode() {
        return code;
    }

    public CallJSFunction setCode(String code) {
        this.code = nonEmptyTrimmed(code);
        return this;
    }

    public String getMainFunctionName() {
        return mainFunctionName;
    }

    public CallJSFunction setMainFunctionName(String mainFunctionName) {
        this.mainFunctionName = nonEmptyTrimmed(mainFunctionName);
        return this;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public CallJSFunction setWorkingDirectory(String workingDirectory) {
        workingDirectory = nonEmptyTrimmed(workingDirectory);
        if (!workingDirectory.equals(this.workingDirectory)) {
            closePerformerContainer();
            this.workingDirectory = workingDirectory;
        }
        return this;
    }

    public String getA() {
        return a;
    }

    public CallJSFunction setA(String a) {
        this.a = a;
        return this;
    }

    public String getB() {
        return b;
    }

    public CallJSFunction setB(String b) {
        this.b = b;
        return this;
    }

    public String getC() {
        return c;
    }

    public CallJSFunction setC(String c) {
        this.c = c;
        return this;
    }

    public String getD() {
        return d;
    }

    public CallJSFunction setD(String d) {
        this.d = d;
        return this;
    }

    public String getE() {
        return e;
    }

    public CallJSFunction setE(String e) {
        this.e = e;
        return this;
    }

    public String getF() {
        return f;
    }

    public CallJSFunction setF(String f) {
        this.f = f;
        return this;
    }

    public double getP() {
        return p;
    }

    public CallJSFunction setP(double p) {
        this.p = p;
        return this;
    }

    public double getQ() {
        return q;
    }

    public CallJSFunction setQ(double q) {
        this.q = q;
        return this;
    }

    public double getR() {
        return r;
    }

    public CallJSFunction setR(double r) {
        this.r = r;
        return this;
    }

    public double getS() {
        return s;
    }

    public CallJSFunction setS(double s) {
        this.s = s;
        return this;
    }

    public double getT() {
        return t;
    }

    public CallJSFunction setT(double t) {
        this.t = t;
        return this;
    }

    public double getU() {
        return u;
    }

    public CallJSFunction setU(double u) {
        this.u = u;
        return this;
    }

    public boolean isConvertInputScalarToNumber() {
        return graalAPI.isConvertInputScalarToNumber();
    }

    public CallJSFunction setConvertInputScalarToNumber(boolean convertInputScalarToNumber) {
        graalAPI.setConvertInputScalarToNumber(convertInputScalarToNumber);
        return this;
    }

    public boolean isConvertInputNumbersToArray() {
        return graalAPI.isConvertInputNumbersToArray();
    }

    public CallJSFunction setConvertInputNumbersToArray(boolean convertInputNumbersToArray) {
        graalAPI.setConvertInputNumbersToArray(convertInputNumbersToArray);
        return this;
    }

    public boolean isConvertInputArraysToDouble() {
        return graalAPI.isConvertInputArraysToDouble();
    }

    public CallJSFunction setConvertInputArraysToDouble(boolean convertInputArraysToDouble) {
        graalAPI.setConvertInputArraysToDouble(convertInputArraysToDouble);
        return this;
    }

    public boolean isConvertOutputIntegerToBriefForm() {
        return graalAPI.isConvertOutputIntegerToBriefForm();
    }

    public CallJSFunction setConvertOutputIntegersToBriefForm(boolean convertOutputIntegersToBriefForm) {
        graalAPI.setConvertOutputIntegersToBriefForm(convertOutputIntegersToBriefForm);
        return this;
    }

    public GraalSafety getSafety() {
        return safety;
    }

    public CallJSFunction setSafety(GraalSafety safety) {
        nonNull(safety);
        if (safety != this.safety) {
            closePerformerContainer();
            this.safety = safety;
        }
        return this;
    }

    @Override
    public void initialize() {
        long t1 = debugTime();
        javaScriptCode.setModuleJS(GraalPerformer.addReturningJSFunction(code, mainFunctionName), "main_code");
        // - name "main_code" is not important: we will not use share this performer (Graal context) with other
        // executors; but if we want to use several scripts INSIDE this executor, they must have different module names
        final boolean changed = javaScriptCode.changed();
        if (changed) {
            logDebug(() -> "Changing code/settings of \"" + mainFunctionName + "\" detected: rebuilding performer");
            closePerformerContainer();
        }
        long t2 = debugTime();
        translatedDirectory = translateWorkingDirectory();
        performerContainer.setCustomizer(safety);
        performerContainer.setWorkingDirectory(safety.isWorkingDirectorySupported() ? translatedDirectory : null);
        // - in PURE mode we cannot set the working directory in Context.Builder;
        // note: we must explicitly set it to null if !isWorkingDirectorySupported -
        // maybe the user changed the safety parameter!
        GraalAPI.initializeJS(performerContainer);
        final GraalPerformer performer = performerContainer.performer();
        long t3 = debugTime();
        if (mainFunction == null) {
            // no sense to perform ECMA module, if it was not changed: re-executing will have no effect
            mainFunction = performer.perform(javaScriptCode);
        }
        createEmptyObjectFunction = GraalAPI.storedCreateEmptyObjectJSFunction(performer);
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript function \"%s\" initialized in %.5f ms:"
                        + " %.6f ms recompiling + %.6f ms getting performer + %.6f ms executing module",
                mainFunctionName,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    @Override
    public void process() {
        long t1 = debugTime(), t2, t3, t4;
        if (mainFunction == null || createEmptyObjectFunction == null) {
            throw new IllegalStateException(getClass() + " is not initialized");
        }
        final Value parameters = createEmptyObjectFunction.execute();
        final Value inputs = createEmptyObjectFunction.execute();
        final Value outputs = createEmptyObjectFunction.execute();
        if (safety.isJavaAccessSupported()) {
            // - no sense to create _env if we have no ways to read its fields from a Java Map object
            graalAPI.loadSystemParameters(this, parameters, translatedDirectory);
        }
        graalAPI.loadParameters(subMap(parameters(), PARAMETERS_NAMES), parameters);
        graalAPI.readInputPorts(subSet(inputPorts(), INPUTS_NAMES), inputs);
        t2 = debugTime();
        final Value result = mainFunction.execute(parameters, inputs, outputs);
        t3 = debugTime();
        graalAPI.writeOutputPorts(subSet(outputPorts(), OUTPUTS_NAMES), outputs);
        graalAPI.writeOutputPort(getOutputPort(DEFAULT_OUTPUT_PORT), result, true);
        // - note: direct assignment "outputs.output = xxx" overrides simple returning result
        t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "JavaScript function \"%s\" executed in %.5f ms:"
                        + " %.6f ms loading inputs + %.6f ms calling + %.6f ms returning outputs",
                mainFunctionName,
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));
    }

    @Override
    public void close() {
        super.close();
        closePerformerContainer();
    }

    private void closePerformerContainer() {
        this.mainFunction = null;
        // - enforce re-creating this function by perform()
        this.performerContainer.freeResources();
    }

    private Path translateWorkingDirectory() {
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(workingDirectory, this);
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
