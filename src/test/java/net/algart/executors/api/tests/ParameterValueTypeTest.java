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

package net.algart.executors.api.tests;

import net.algart.executors.api.parameters.ParameterValueType;

import java.util.Arrays;

public class ParameterValueTypeTest {
    public static void main(String[] args) {
        for (ParameterValueType type : ParameterValueType.values()) {
            System.out.printf("%s; %s, %s, reverse valueOf the same: %s, %s; empty value: %s (%s)%n",
                    type,
                    type.typeName(),
                    java.util.Arrays.toString(type.typeNameAliases()),
                    ParameterValueType.valueOf(type.name()),
                    ParameterValueType.ofTypeName(type.typeName()),
                    type.emptyJsonValue(),
                    type.emptyJsonValue().getClass());
            for (String v : Arrays.asList("TRUE", "12", "12.3", "axd", "{\"key\":12}")) {
                System.out.printf("  %s: %s%n", v, type.toJsonValue(v));
            }
        }
        try {
            ParameterValueType.ofTypeName("something");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
