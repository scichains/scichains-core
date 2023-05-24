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

package net.algart.executors.modules.core.common;

import net.algart.arrays.Arrays;

import java.util.stream.IntStream;

public class Multithreading {
    private Multithreading() {
    }

    public static IntStream loopStream(int n) {
        return loopStream(n, true);
    }

    public static IntStream loopStream(int n, boolean enableMultithreading) {
        return enableMultithreading && net.algart.arrays.Arrays.SystemSettings.cpuCount() > 1 ?
                IntStream.range(0, n).parallel() :
                IntStream.range(0, n);
    }

    public static int recommendedNumberOfParallelRanges() {
        return recommendedNumberOfParallelRanges(Integer.MAX_VALUE);
    }

    public static int recommendedNumberOfParallelRanges(int n) {
        final int cpuCount = Arrays.SystemSettings.cpuCount();
        return cpuCount == 1 ? 1 : (int) Math.min(n, (long) 4 * (long) cpuCount);
        // - splitting into more than cpuCount ranges provides better performance
    }

    public static int[] splitToRanges(int n, int numberOfRanges) {
        if (numberOfRanges < 0) {
            throw new IllegalArgumentException("Negative number of ranges: " + numberOfRanges);
        }
        final int[] result = new int[numberOfRanges + 1];
        Arrays.splitToRanges(result, n);
        return result;
    }

    public static long[] splitToRanges(long n, int numberOfRanges) {
        if (numberOfRanges < 0) {
            throw new IllegalArgumentException("Negative number of ranges: " + numberOfRanges);
        }
        final long[] result = new long[numberOfRanges + 1];
        Arrays.splitToRanges(result, n);
        return result;
    }

    //    public static void main(String[] args) {
//        int[] ranges1 = splitToRanges(1111, 1);
//        int[] ranges2 = new int[ranges1.length];
//        splitToRanges(ranges2, 1111);
//        if (!java.util.Arrays.equals(ranges1, ranges2)) {
//            throw new AssertionError();
//        }
//    }
}
