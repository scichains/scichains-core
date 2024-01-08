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

package net.algart.json;

import jakarta.json.*;

public class CommonJsonTest {
    public static void main(String[] args) {
        final JsonString stringValue = Jsons.toJsonStringValue("");
        final JsonNumber intValue = Jsons.toJsonIntValue(123);
        final JsonNumber longValue = Jsons.toJsonLongValue(Long.MAX_VALUE);
        final JsonNumber doubleValue = Jsons.toJsonDoubleValue(1.1);
        final JsonValue booleanValue = Jsons.toJsonBooleanValue(false);
        System.out.printf("String: %s = %s%n", stringValue, stringValue.getString());
        System.out.printf("int: %s = %s%n", intValue, intValue.intValue());
        System.out.printf("long: %s = %s%n", longValue, longValue.longValue());
        System.out.printf("double: %s = %s%n", doubleValue, doubleValue.doubleValue());
        System.out.printf("boolean: %s (%s)%n", booleanValue, booleanValue == JsonValue.FALSE);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("String", stringValue);
        builder.add("int", intValue);
        builder.add("long", longValue);
        builder.add("double", doubleValue);
        builder.add("boolean", booleanValue);
        builder.add("object", Jsons.newEmptyJson());
        JsonObject json = builder.build();
        System.out.println(Jsons.toPrettyString(json));
        System.out.println(Jsons.getJsonObject(json, "long_"));
        System.out.println(Jsons.getJsonObject(null, "long_"));
        System.out.println();

        builder = Json.createObjectBuilder();
        Jsons.addDouble(builder, "negative_infinity", Double.NEGATIVE_INFINITY);
        Jsons.addDouble(builder, "positive_infinity", Double.POSITIVE_INFINITY);
        Jsons.addDouble(builder, "mot-a-number", Double.NaN);
        Jsons.addDouble(builder, "157", 157);
        Jsons.addDouble(builder, "max_value", Double.MAX_VALUE);
        builder.add("boolean", true);
        json = builder.build();
        System.out.println(Jsons.toPrettyString(json));
        System.out.println(Jsons.getDouble(json,"negative_infinity", -1.0));
        System.out.println(Jsons.reqDouble(json,"negative_infinity"));
        System.out.println(Jsons.getDouble(json,"positive_infinity", -1.0));
        System.out.println(Jsons.reqDouble(json,"positive_infinity"));
        System.out.println(Jsons.getDouble(json,"mot-a-number", -1.0));
        System.out.println(Jsons.reqDouble(json,"mot-a-number"));
        System.out.println(Jsons.getDouble(json,"157", -1.0));
        System.out.println(Jsons.reqDouble(json,"157"));
        System.out.println(Jsons.getDouble(json,"max_value", -1.0));
        System.out.println(Jsons.reqDouble(json,"max_value"));
        System.out.println(Jsons.getDouble(json,"xxxxxxxxx", -1.0));
        System.out.println(Jsons.getDouble(json,"boolean", -1.0));
//        System.out.println(Jsons.reqDouble(json,"boolean"));
    }
}
