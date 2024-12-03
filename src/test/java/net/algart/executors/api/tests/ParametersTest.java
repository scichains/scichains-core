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

package net.algart.executors.api.tests;

import net.algart.executors.api.parameters.NoValidParameterException;
import net.algart.executors.api.parameters.Parameters;

public class ParametersTest {
    private static void show(Parameters p, String name) {
        System.out.printf(
                "%n%s: [%s] = \"%s\" (string) = %s (int) = %s (long) = %s (double) = %s (boolean) = " +
                        "%s (json) [type: %s]%n",
                name,
                p.get(name),
                p.getString(name, null),
                p.getInteger(name, -1),
                p.getLong(name, -1),
                p.getDouble(name, Double.NaN),
                p.getBoolean(name, false),
                p.getJsonValue(name),
                p.get(name) == null ? "null" : p.get(name).getClass().getName());
        try {
            if (!p.getString(name).equals(p.getString(name, null))) {
                throw new AssertionError();
            }
        } catch (NoValidParameterException e) {
            System.out.printf("%s cannot be read as String: %s%n", name, e);
        }
        try {
            if (p.getInteger(name) != p.getInteger(name, -1)) {
                throw new AssertionError();
            }
        } catch (NoValidParameterException e) {
            System.out.printf("%s cannot be read as integer: %s%n", name, e);
        }
        try {
            if (p.getLong(name) != p.getLong(name, -1)) {
                throw new AssertionError();
            }
        } catch (NoValidParameterException e) {
            System.out.printf("%s cannot be read as long: %s%n", name, e);
        }
        try {
            if (p.getDouble(name) != p.getDouble(name, -1)) {
                throw new AssertionError();
            }
        } catch (NoValidParameterException e) {
            System.out.printf("%s cannot be read as double: %s%n", name, e);
        }
        try {
            if (p.getBoolean(name) != p.getBoolean(name, false)) {
                throw new AssertionError();
            }
        } catch (NoValidParameterException e) {
            System.out.printf("%s cannot be read as boolean: %s%n", name, e);
        }
    }

    public static void main(String[] args) {
        Parameters p = new Parameters();
        System.out.println("Testing normal setters:");
        p.setLong("a", 123);
        p.setInteger("b", 124);
        p.setBoolean("c", true);
        p.setDouble("d", 125);
        p.setString("e", "eeee");
        System.out.println(p);
        for (String name : p.keySet()) {
            show(p, name);
        }
        show(p, "absent");

        System.out.println();
        System.out.println();
        System.out.println("Testing string-value setters:");
        p = new Parameters();
        p.setLong("a", "123.00");
        p.setInteger("b", "12422222");
        p.setBoolean("c", "True");
        p.setDouble("d", "125e10");
        p.setString("e", null);
        System.out.println(p);
        for (String name : p.keySet()) {
            show(p, name);
        }
    }
}
