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

package net.algart.executors.api.graalvm.js.core.arrays;

import jakarta.json.JsonObject;
import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptContextContainer;
import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptException;
import net.algart.executors.api.graalvm.js.scriptengine.JavaScriptPerformer;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.json.Jsons;

import javax.script.ScriptEngine;
import java.util.Arrays;

// Note: it does not implement ReadOnlyExecutionInput!
public final class BlockJSModifyingNamedNumbers extends NumbersFilter {
    public static final String IN_OUT_TAGS_1 = "tags_1";
    public static final String IN_OUT_TAGS_2 = "tags_2";
    public static final String INPUT_COLUMN_NAMES = "column_names";
    public static final String INPUT_JSON_1 = "o1";
    public static final String INPUT_JSON_2 = "o2";
    public static final String INPUT_JSON_3 = "o3";
    public static final String INPUT_A = "a";
    public static final String INPUT_B = "b";
    public static final String INPUT_C = "c";
    public static final String JSON_1_VARIABLE_ALT = "o";

    private String initializingOperator = "";
    private String mainOperator = "x0 = x1 * p";
    private boolean useColumnNames = true;
    private boolean useK = false;
    private String tags1Name = "tag";
    private String tags2Name = "kind";
    private boolean simpleAccessToJson1 = false;
    private boolean simpleAccessToJson2 = false;
    private boolean simpleAccessToJson3 = false;
    private double p = 0.0;
    private double q = 0.0;
    private double r = 0.0;
    private double s = 0.0;
    private double t = 0.0;
    private double u = 0.0;

    private final JavaScriptContextContainer contextContainer = JavaScriptContextContainer.getInstance();
    private JavaScriptPerformer javaScriptInitializingOperator = null;
    private JavaScriptPerformer javaScriptMainOperator = null;
    private JavaScriptPerformer javaScriptCreateJson = null;

    public BlockJSModifyingNamedNumbers() {
        addInputNumbers(IN_OUT_TAGS_1);
        addInputNumbers(IN_OUT_TAGS_2);
        addInputScalar(INPUT_COLUMN_NAMES);
        addInputScalar(INPUT_JSON_1);
        addInputScalar(INPUT_JSON_2);
        addInputScalar(INPUT_JSON_3);
        addInputNumbers(INPUT_A);
        addInputNumbers(INPUT_B);
        addInputNumbers(INPUT_C);
        addOutputNumbers(IN_OUT_TAGS_1);
        addOutputNumbers(IN_OUT_TAGS_2);
    }

    public String getInitializingOperator() {
        return initializingOperator;
    }

    public BlockJSModifyingNamedNumbers setInitializingOperator(String initializingOperator) {
        this.initializingOperator = nonNull(initializingOperator).trim();
        return this;
    }

    public String getMainOperator() {
        return mainOperator;
    }

    public BlockJSModifyingNamedNumbers setMainOperator(String mainOperator) {
        this.mainOperator = nonNull(mainOperator).trim();
        return this;
    }

    public boolean isUseColumnNames() {
        return useColumnNames;
    }

    public BlockJSModifyingNamedNumbers setUseColumnNames(boolean useColumnNames) {
        this.useColumnNames = useColumnNames;
        return this;
    }

    public boolean isUseK() {
        return useK;
    }

    public BlockJSModifyingNamedNumbers setUseK(boolean useK) {
        this.useK = useK;
        return this;
    }

    public String getTags1Name() {
        return tags1Name;
    }

    public BlockJSModifyingNamedNumbers setTags1Name(String tags1Name) {
        this.tags1Name = nonEmpty(tags1Name);
        return this;
    }

    public String getTags2Name() {
        return tags2Name;
    }

    public BlockJSModifyingNamedNumbers setTags2Name(String tags2Name) {
        this.tags2Name = nonEmpty(tags2Name);
        return this;
    }

    public boolean isSimpleAccessToJson1() {
        return simpleAccessToJson1;
    }

    public BlockJSModifyingNamedNumbers setSimpleAccessToJson1(boolean simpleAccessToJson1) {
        this.simpleAccessToJson1 = simpleAccessToJson1;
        return this;
    }

    public boolean isSimpleAccessToJson2() {
        return simpleAccessToJson2;
    }

    public BlockJSModifyingNamedNumbers setSimpleAccessToJson2(boolean simpleAccessToJson2) {
        this.simpleAccessToJson2 = simpleAccessToJson2;
        return this;
    }

    public boolean isSimpleAccessToJson3() {
        return simpleAccessToJson3;
    }

    public BlockJSModifyingNamedNumbers setSimpleAccessToJson3(boolean simpleAccessToJson3) {
        this.simpleAccessToJson3 = simpleAccessToJson3;
        return this;
    }

    public double getP() {
        return p;
    }

    public BlockJSModifyingNamedNumbers setP(double p) {
        this.p = p;
        return this;
    }

    public double getQ() {
        return q;
    }

    public BlockJSModifyingNamedNumbers setQ(double q) {
        this.q = q;
        return this;
    }

    public double getR() {
        return r;
    }

    public BlockJSModifyingNamedNumbers setR(double r) {
        this.r = r;
        return this;
    }

    public double getS() {
        return s;
    }

    public BlockJSModifyingNamedNumbers setS(double s) {
        this.s = s;
        return this;
    }

    public double getT() {
        return t;
    }

    public BlockJSModifyingNamedNumbers setT(double t) {
        this.t = t;
        return this;
    }

    public double getU() {
        return u;
    }

    public BlockJSModifyingNamedNumbers setU(double u) {
        this.u = u;
        return this;
    }

    @Override
    public void initialize() {
        final ScriptEngine context = contextContainer.getLocalContext();
        final String initializingOperator = buildInitializingOperator();
        javaScriptInitializingOperator = getScript(initializingOperator, javaScriptInitializingOperator, context);
        putAllInputs(context, true);
        javaScriptInitializingOperator.perform();
    }

    @Override
    public SNumbers processNumbers(SNumbers source) {
        final SNumbers tags1 = getInputNumbers(IN_OUT_TAGS_1, true);
        final SNumbers tags2 = getInputNumbers(IN_OUT_TAGS_2, true);
        final SNumbers result = processNumbers(source, tags1, tags2);
        getNumbers(IN_OUT_TAGS_1).exchange(tags1);
        getNumbers(IN_OUT_TAGS_2).exchange(tags2);
        return result;
    }

    public SNumbers processNumbers(SNumbers source, SNumbers tags1Numbers, SNumbers tags2Numbers) {
        final boolean useTags1 = tags1Numbers != null && tags1Numbers.isInitialized();
        final boolean useTags2 = tags2Numbers != null && tags2Numbers.isInitialized();
        if (useTags1 && source.n() != tags1Numbers.n()) {
            throw new IllegalArgumentException("Different lengths of source array and 1st tags array: "
                    + source.n() + " != " + tags1Numbers.n());
        }
        if (useTags2 && source.n() != tags2Numbers.n()) {
            throw new IllegalArgumentException("Different lengths of source array and 2nd tags array: "
                    + source.n() + " != " + tags2Numbers.n());
        }
        final ScriptEngine context = contextContainer.getLocalContext();
        putAllInputs(context, false);
        final double[] x = createDoubleBlock(source);
        final Object tags1 = createCompatibleBlock(tags1Numbers);
        final Object tags2 = createCompatibleBlock(tags2Numbers);
        final String[] columnNames = getColumnNames(x.length);
        final String mainOperator = buildMainOperator(columnNames, useTags1, useTags2);
        javaScriptMainOperator = getScript(mainOperator, javaScriptMainOperator, context);
        javaScriptMainOperator.putVariable("x", x);
        if (useTags1) {
            javaScriptMainOperator.putVariable("tags1", tags1);
        }
        if (useTags2) {
            javaScriptMainOperator.putVariable("tags2", tags2);
        }
        javaScriptMainOperator.putVariable("x", x);
        for (int k = 0, n = source.n(); k < n; k++) {
            if (useK) {
                javaScriptMainOperator.putVariable("k", k);
            }
            readDoubleBlock(x, source, k);
            if (useTags1) {
                readCompatibleBlock(tags1, tags1Numbers, k);
            }
            if (useTags2) {
                readCompatibleBlock(tags2, tags2Numbers, k);
            }
            javaScriptMainOperator.perform();
            writeDoubleBlock(x, source, k);
            if (useTags1) {
                writeCompatibleBlock(tags1, tags1Numbers, k);
            }
            if (useTags2) {
                writeCompatibleBlock(tags2, tags2Numbers, k);
            }
        }
        return source;
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return super.visibleResultsInformation().addPorts(getInputPort(INPUT_COLUMN_NAMES));
    }

    private String[] getColumnNames(int blockLength) {
        final String[] columnNames = getInputScalar(INPUT_COLUMN_NAMES, true)
                .toTrimmedLinesWithoutCommentsArray();
        final String[] result = columnNames == null ?
                new String[blockLength] :
                Arrays.copyOf(columnNames, blockLength);
        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                result[i] = "x" + i;
            }
        }
        return result;
    }

    private void putAllInputs(ScriptEngine context, boolean alsoUninitialized) {
        putInputNumbers(context, INPUT_A, alsoUninitialized);
        putInputNumbers(context, INPUT_B, alsoUninitialized);
        putInputNumbers(context, INPUT_C, alsoUninitialized);
        putInputJson(context, INPUT_JSON_1, JSON_1_VARIABLE_ALT, alsoUninitialized);
        putInputJson(context, INPUT_JSON_2, null, alsoUninitialized);
        putInputJson(context, INPUT_JSON_3, null, alsoUninitialized);
        context.put("p", p);
        context.put("q", q);
        context.put("r", r);
        context.put("s", s);
        context.put("t", t);
        context.put("u", u);
    }

    private void putInputJson(
            ScriptEngine context,
            String inputPortName,
            String altName,
            boolean alsoUninitialized) {
        javaScriptCreateJson = getScript("JSON.parse(jsonString)", javaScriptCreateJson, context);
        SScalar scalar = getInputScalar(inputPortName, true);
        if (alsoUninitialized || scalar.isInitialized()) {
            context.put("jsonString", scalar.getValue());
            final Object value;
            try {
                value = javaScriptCreateJson.perform();
            } catch (JavaScriptException e) {
                throw new AssertionError("Should not occur!", e);
            }
            context.put(inputPortName, value);
            if (altName != null) {
                context.put(altName, value);
            }
        }
    }

    private void putInputNumbers(
            ScriptEngine context,
            String inputPortName,
            boolean alsoUninitialized) {
        SNumbers numbers = getInputNumbers(inputPortName, true);
        final double[] value = numbers.toDoubleArray();
        if (alsoUninitialized || value != null) {
            context.put(inputPortName, value);
        }
    }

    private String buildInitializingOperator() {
        final JsonObject json1 = Jsons.toJson(
                getInputScalar(INPUT_JSON_1, true).getValue(), true);
        final JsonObject json2 = Jsons.toJson(
                getInputScalar(INPUT_JSON_2, true).getValue(), true);
        final JsonObject json3 = Jsons.toJson(
                getInputScalar(INPUT_JSON_3, true).getValue(), true);
        final StringBuilder sb = new StringBuilder();
        if (simpleAccessToJson1) {
            addJsonToVariables(sb, json1, INPUT_JSON_1);
        }
        if (simpleAccessToJson2) {
            addJsonToVariables(sb, json2, INPUT_JSON_2);
        }
        if (simpleAccessToJson3) {
            addJsonToVariables(sb, json3, INPUT_JSON_3);
        }
        sb.append(initializingOperator + "\n");
        return sb.toString();
    }


    private String buildMainOperator(
            String[] names,
            boolean useTags1,
            boolean useTags2) {
        final StringBuilder sb = new StringBuilder();
        if (useColumnNames) {
            for (int i = 0; i < names.length; i++) {
                sb.append("var " + names[i] + " = x[" + i + "];\n");
            }
        }
        if (useTags1) {
            sb.append("var " + tags1Name + " = tags1[0];\n");
        }
        if (useTags2) {
            sb.append("var " + tags2Name + " = tags2[0];\n");
        }
        sb.append(mainOperator + "\n");
        if (useColumnNames) {
            for (int i = 0; i < names.length; i++) {
                sb.append("x[" + i + "] = " + names[i] + ";\n");
            }
        }
        if (useTags1) {
            sb.append("tags1[0] = " + tags1Name + ";\n");
        }
        if (useTags2) {
            sb.append("tags2[0] = " + tags2Name + ";\n");
        }
        return sb.toString();
    }

    private static void addJsonToVariables(StringBuilder sb, JsonObject json, String jsonName) {
        for (String key : json.keySet()) {
            sb.append("var " + key + " = " + jsonName + "." + key + ";\n");
        }
    }

    private static double[] createDoubleBlock(SNumbers numbers) {
        return new double[numbers != null && numbers.isInitialized() ? numbers.getBlockLength() : 0];
    }

    private static Object createCompatibleBlock(SNumbers numbers) {
        return numbers != null && numbers.isInitialized() ?
                numbers.newCompatibleJavaArray(numbers.getArrayLength()) :
                null;
    }

    private static void readDoubleBlock(double[] block, SNumbers numbers, int k) {
        if (block.length > 0) {
            numbers.getBlockDoubleValues(k, block);
        }
    }

    private static void writeDoubleBlock(double[] block, SNumbers numbers, int k) {
        if (block.length > 0) {
            numbers.setBlockDoubleValues(k, block);
        }
    }

    private static void readCompatibleBlock(Object block, SNumbers numbers, int k) {
        numbers.getBlockValues(k, block);
    }

    private static void writeCompatibleBlock(Object block, SNumbers numbers, int k) {
        numbers.setBlockValues(k, block);
    }

    private static JavaScriptPerformer getScript(String formula, JavaScriptPerformer previous, ScriptEngine context) {
        return JavaScriptPerformer.newInstanceIfChanged(formula, previous, context);
    }

}
