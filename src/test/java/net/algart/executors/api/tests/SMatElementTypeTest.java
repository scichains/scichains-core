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

import net.algart.executors.api.data.SMat;

public class SMatElementTypeTest {
    public static void main(String[] args) {
        for (SMat.Depth depth : SMat.Depth.values()) {
            System.out.printf("%s; reverse valueOf is the same: %s, %s%n",
                    depth,
                    SMat.Depth.valueOf(depth.code()) == depth,
                    SMat.Depth.valueOf(depth.elementType(), depth.isUnsigned()) == depth);
        }
        System.out.println();
        for (int code = -5; code <= 199; code++) {
            try {
                final SMat.Depth depth = SMat.Depth.valueOf(code);
                System.out.printf("Depth code %d: %s%n", code, depth);
            } catch (Exception e) {
                System.out.printf("Depth code %d leads to exception: %s%n", code, e);
            }
        }
    }
}
