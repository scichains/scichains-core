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

package net.algart.executors.api.tests;

import net.algart.executors.api.data.SMat;

import java.nio.ByteBuffer;

public class SMatSpeed {
    private static void test() {
        final SMat mat = new SMat();
        mat.setAll(
                new long[] {10, 10},
                SMat.Depth.S8, 1, ByteBuffer.allocate(100), false);
        System.out.printf("Testing %s...%n", mat);

        long t1 = System.nanoTime();
        long width = mat.getDimX();
        long height = mat.getDimY();
        long t2 = System.nanoTime();
        System.out.printf("getDimX-1: %d ns%n", t2 - t1);
        for (int k = 0; k < 1000000; k++) {
            mat.getDimX();
            mat.getDimY();
        }

        t1 = System.nanoTime();
        width = mat.getDimX();
        height = mat.getDimY();
        t2 = System.nanoTime();
        System.out.printf("getDimX-2: %d ns%n", t2 - t1, t1, t2);

        t1 = System.nanoTime();
        for (int k = 0; k < 1000000; k++) {
            width = mat.getDimX();
        }
        t2 = System.nanoTime();
        System.out.printf("getDimX-3 in loop: %.6f ns%n", (t2 - t1) * 1e-6);

        t1 = System.nanoTime();
        width = mat.getDimX();
        height = mat.getDimY();
        t2 = System.nanoTime();
        System.out.printf("getDimX-4: %d ns%n", t2 - t1);
    }

    public static void main(String[] args) {
        for (int iteration = 0; iteration < 5; iteration++) {
            System.out.printf("%nText %d:%n", iteration);
            test();
        }
    }
}
