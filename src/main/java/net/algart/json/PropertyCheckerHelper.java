/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

class PropertyCheckerHelper {
    static String findPropertyNameFromCurrentSetter(String defaultName) {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stack) {
            final String methodName = stackTraceElement.getMethodName();
            if (methodName.startsWith("set")) {
                String propertyName = methodName.substring("set".length());
                if (propertyName.isEmpty()) {
                    break;
                }
                propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                return propertyName;
            }
        }
        return defaultName;
    }

    static class TestStackTrace {
        static void setSomething(double value) {
            System.out.println("Current property name in setter: "
                    + findPropertyNameFromCurrentSetter("N/A"));
        }

        public static void main(String[] args) {
            setSomething(1.0);
            System.out.println("Current property name outside: "
                    + findPropertyNameFromCurrentSetter("N/A"));
        }
    }
}
