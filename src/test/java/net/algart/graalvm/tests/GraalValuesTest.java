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

package net.algart.graalvm.tests;

import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalValues;
import net.algart.bridges.standard.JavaScriptException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Objects;
import java.util.function.Supplier;

public class GraalValuesTest {
    private static final ScriptEngine ENGINE = new ScriptEngineManager(GraalValuesTest.class.getClassLoader())
            .getEngineByName("javascript");
    private static final GraalPerformer PERFORMER = GraalPerformer.newPerformer(Context.create());

    private static void test(String script) {
        System.out.println("**********");
        System.out.printf("Performing \"%s\"...%n", script);
        final Value value;
        try {
            value = PERFORMER.performJS(script);
        } catch (RuntimeException e) {
            System.out.printf("%s%n%n%n", e);
            return;
        }
        System.out.printf("Result value \"%s\"%n", value);
        System.out.printf("isNull: %s%n", value.isNull());
        System.out.printf("isBoolean: %s%n", value.isBoolean());
        System.out.printf("isNumber: %s%n", value.isNumber());
        System.out.printf("fitsInDouble: %s%n", value.fitsInDouble());
        System.out.printf("fitsInInt: %s%n", value.fitsInInt());
        System.out.printf("hasArrayElements: %s%n", value.hasArrayElements());
        System.out.printf("as(Object.class): %s%n", objectInfo(value.as(Object.class)));
        final String smartString = GraalValues.toSmartString(value, true);
        System.out.printf("toSmartString(true): \"%s\"%n", smartString);
        System.out.printf("toSmartString(false): \"%s\"%n", GraalValues.toSmartString(value, false));
        final boolean smartBoolean = GraalValues.toSmartBoolean(value);
        System.out.printf("toSmartBoolean: %s%n", smartBoolean);
        final Object smartDouble = wrapException(() -> GraalValues.toSmartDouble(value));
        System.out.printf("toSmartDouble: %s%n", smartDouble);
        System.out.println();
        final Object jsResult = perform(script);
        System.out.printf("Result object via ScriptEngine: %s%n", objectInfo(jsResult));
        final String calculateStringOrNumber = calculateStringOrNumber(script);
        System.out.printf("calculateStringOrNumber: %s%n", calculateStringOrNumber);
        final boolean calculateBoolean = calculateBoolean(script);
        System.out.printf("calculateBoolean: %s%n", calculateBoolean);
        final Object calculateDouble = wrapException(() -> calculateDouble(script));
        System.out.printf("calculateDouble: %s%n", calculateDouble);
        if (!Objects.equals(smartString, calculateStringOrNumber)) {
            throw new AssertionError("toSmartString/calculateStringOrNumber mismatch");
        }
        if (calculateBoolean != smartBoolean) {
            throw new AssertionError("toSmartBoolean/calculateBoolean mismatch");
        }
        if ((smartDouble instanceof Double || calculateDouble instanceof Double)
                && !Objects.equals(smartDouble, calculateDouble)) {
            throw new AssertionError("toSmartDouble/calculateDouble mismatch");
        }
        System.out.println();
        System.out.println();
    }

    private static Object wrapException(Supplier<Object> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static String objectInfo(Object object) {
        return object == null ?
                "null" :
                String.format("\"%s\" (%s)", object, object.getClass());
    }

    public static Object perform(String script) {
        try {
            return ENGINE.eval(script);
        } catch (ScriptException e) {
            throw JavaScriptException.wrap(e, script);
        }
    }

    public static boolean calculateBoolean(String script) {
        final Object result = perform(script);
        if (result == null || result.equals("")) {
            return false;
        }
        return Boolean.parseBoolean(result.toString());
    }

    public static double calculateDouble(String script) {
        final Object result = perform(script);
        if (result == null) {
            return Double.NaN;
        }
        return Double.parseDouble(result.toString());
    }

    // It is more old function, created for ScriptEngine
    public static String calculateStringOrNumber(String script) {
        Object result = perform(script);
        if (result == null) {
            return null;
        }
        if (result instanceof Double || result instanceof Float) {
            final double v = ((Number) result).doubleValue();
            if (v == (int) v) {
                result = (int) v;
            }
        }
        return result.toString();
    }


    public static void main(String[] args) throws ScriptException, InterruptedException {
        test(null);
        test("null");
        test("");
        test("''");
        test("' '");
        test("true");
        test("'true'");
        test("'True'");
        test("false");
        test("'false'");
        test("'False'");
        test("3+5");
        test("parseFloat('8.0')");
        // - enforce JavaScript to create Double value instead of integer
        test("0.0");
        test("-0");
        test("NaN");
        test("1/0");
        test("-1/0");
        test("3+5.1");
        test("[1,2,3]");
        test("32341235414352345234661.23");
        test("32341235414352345234661");
        test("avc");
        test("Math.abs");
        test("Math.abss");
    }
}
