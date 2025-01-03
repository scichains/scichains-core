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

import net.algart.executors.api.Executor;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class TimingSpeed {
    static final int N = 10000000;

    private static void test() {
        long t1 = System.nanoTime();
        long sum = 0;
        for (int k = 0; k < N; k++) {
            sum += k;
        }
        long t2 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += System.nanoTime();
        }
        long t3 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += System.currentTimeMillis();
        }
        long t4 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += Executor.debugTime() + k;
        }
        long t5 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += Executor.allocatedMemory() + k;
        }
        long t6 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += Executor.fineAllocatedMemory() + k;
        }
        long t7 = System.nanoTime();
        Instant firstInstant = Instant.now();
        for (int k = 0; k < N; k++) {
            firstInstant = Instant.now();
        }
        long t8 = System.nanoTime();
        for (int k = 0; k < N; k++) {
            sum += Duration.between(firstInstant, Instant.now()).getNano();
        }
        long t9 = System.nanoTime();
        System.out.printf(Locale.US, "Empty loop: %.3f ms, %.2f ns/element (result = %d)%n",
                (t2 - t1) * 1e-6, (t2 - t1) / (double) N, sum);
        // - printing result to avoid eliminating empty loop
        System.out.printf(Locale.US, "System.nanoTime(): %.3f ms, %.2f ns/call%n",
                (t3 - t2) * 1e-6, (t3 - t2) / (double) N);
        System.out.printf(Locale.US, "System.currentTimeMillis(): %.3f ms, %.2f ns/call%n",
                (t4 - t3) * 1e-6, (t4 - t3) / (double) N);
        System.out.printf(Locale.US, "Executor.debugTime(): %.3f ms, %.2f ns/call%n",
                (t5 - t4) * 1e-6, (t5 - t4) / (double) N);
        System.out.printf(Locale.US, "Executor.allocatedMemory(): %.3f ms, %.2f ns/call%n",
                (t6 - t5) * 1e-6, (t6 - t5) / (double) N);
        System.out.printf(Locale.US, "Executor.fineAllocatedMemory(): %.3f ms, %.2f ns/call%n",
                (t7 - t6) * 1e-6, (t7 - t6) / (double) N);
        System.out.printf(Locale.US, "Instant.now(): %.3f ms, %.2f ns/call%n",
                (t8 - t7) * 1e-6, (t8 - t7) / (double) N);
        System.out.printf(Locale.US, "Duration.between(..., Instant.now()): %.3f ms, %.2f ns/call%n",
                (t9 - t8) * 1e-6, (t9 - t8) / (double) N);
        System.out.println();
    }

    public static void main(String[] args) {
        for (int iteration = 0; iteration < 15; iteration++) {
            System.out.printf("%nTest %d:%n", iteration);
            test();
        }
    }
}
