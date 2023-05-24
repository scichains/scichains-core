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

package net.algart.additions.math.tests;

import net.algart.arrays.JArrays;
import net.algart.additions.math.IRangeFinder;
import net.algart.additions.math.IntArrayAppender;

import java.util.Arrays;
import java.util.Random;

public class IRangeFinderTest {
    private static void compareArrays(int[] result1, int m1, int[] result2, int m2, int request, int min, int max) {
        Arrays.sort(result1, 0, m1);
        Arrays.sort(result2, 0, m2);
        if (m1 != m2 || !JArrays.arrayEquals(Arrays.copyOf(result1, m1), 0,
                Arrays.copyOf(result2, m2), 0, m1)) {
            throw new AssertionError("Different results at request #" + request
                    + "! " + m1 + " and " + m2
                    + " for " + min + ".." + max
                    + ":\n    " + JArrays.toString(Arrays.copyOf(result1, m1),
                    ", ", 512)
                    + "\n    " + JArrays.toString(Arrays.copyOf(result2, m2),
                    ", ", 512));
        }
    }

    private static void compareArrays(int[] result1, int m1, int[] result2, int m2, int request, double point) {
        Arrays.sort(result1, 0, m1);
        Arrays.sort(result2, 0, m2);
        if (m1 != m2 || !JArrays.arrayEquals(Arrays.copyOf(result1, m1), 0,
                Arrays.copyOf(result2, m2), 0, m1)) {
            throw new AssertionError("Different results at request #" + request
                    + "! " + m1 + " and " + m2
                    + " for " + point
                    + ":\n    " + JArrays.toString(Arrays.copyOf(result1, m1),
                    ", ", 512)
                    + "\n    " + JArrays.toString(Arrays.copyOf(result2, m2),
                    ", ", 512));
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.printf("Usage: %s numberOfIntervals totalWidth numberOfTests numberOfRequests%n",
                    IRangeFinderTest.class.getName());
            return;
        }
        final int numberOfIntervals = Integer.parseInt(args[0]);
        final int totalWidth = Integer.parseInt(args[1]);
        final int numberOfTests = Integer.parseInt(args[2]);
        final int numberOfRequests = Integer.parseInt(args[3]);
        final long seed = 4153;
        final Random rnd = new Random(seed);
        System.out.printf("Testing %d intervals: %d tests for %d requests/test; start seed %d%n",
                numberOfIntervals, numberOfTests, numberOfRequests, seed);
        for (int test = 0; test < numberOfTests; test++) {
            System.out.printf("Test #%d...\r", test);
            final int[] allMin = new int[numberOfIntervals];
            final int[] allMax = new int[numberOfIntervals];

            for (int k = 0; k < numberOfIntervals; k++) {
                allMin[k] = -50 + rnd.nextInt(totalWidth);
                allMax[k] = allMin[k] + rnd.nextInt(200);
            }
            IRangeFinder finder = IRangeFinder.getEmptyInstance();
            IRangeFinder finderSlow = IRangeFinder.getEmptyUnoptimizedInstance();
            final int[] result1 = new int[numberOfIntervals];
            final int[] result2 = new int[numberOfIntervals];
            final int[] result3 = new int[numberOfIntervals];
            final IntArrayAppender result3Appender = new IntArrayAppender(result3);
            final int[] indexes = new int[numberOfIntervals];
            final int n = rnd.nextInt(numberOfIntervals + 1);
            for (int k = 0; k < n; k++) {
                indexes[k] = rnd.nextInt(numberOfIntervals);
            }
            final boolean useCheckedIndexes = rnd.nextBoolean();
            if (n > 0 || rnd.nextBoolean()) {
                if (useCheckedIndexes) {
                    finder.setIndexedRanges(allMin, allMax, indexes, n);
                    finderSlow.setIndexedRanges(allMin, allMax, indexes, n);
                } else {
                    finder.setRanges(allMin, allMax);
                    finderSlow.setRanges(allMin, allMax);
                }
            }
            finder.compact();
            finderSlow.compact();
            for (int request = 0; request < numberOfRequests; request++) {
//                System.out.printf("\r%d", request);
                final int min = -50 + rnd.nextInt(totalWidth);
                final int max = min - 5 + rnd.nextInt(20);
                int m1 = finderSlow.findIntersecting(min, max, result1);
                int m2 = finder.findIntersecting(min, max, result2);
                compareArrays(result1, m1, result2, m2, request, min, max);
                result3Appender.reset();
                finder.findIntersecting(min, max, (index, left, right) -> {
                    result3Appender.accept(index);
                    if (useCheckedIndexes) {
                        index = indexes[index];
                    }
                    if (left != allMin[index]) {
                        throw new AssertionError("Illegal left " + left + " for index " + index);
                    }
                    if (right != allMax[index]) {
                        throw new AssertionError("Illegal right " + right + " for index " + index);
                    }
                });
                compareArrays(result1, m1, result3, result3Appender.offset(), request, min, max);
                double point = min + (rnd.nextBoolean() ? 0.0 : 3.0 * rnd.nextDouble());
                m1 = finder.findContaining(point, result1);
                m2 = 0;
                for (int k = 0; k < finder.numberOfRanges(); k++) {
                    final int index = useCheckedIndexes ? indexes[k] : k;
                    if (allMax[index] >= point && allMin[index] <= point) {
                        result2[m2++] = k;
                    }
                }
                compareArrays(result1, m1, result2, m2, request, point);
                result3Appender.reset();
                finder.findContaining(point, (index, left, right) -> {
                    result3Appender.accept(index);
                    if (useCheckedIndexes) {
                        index = indexes[index];
                    }
                    if (left != allMin[index]) {
                        throw new AssertionError("Illegal left " + left + " for index " + index);
                    }
                    if (right != allMax[index]) {
                        throw new AssertionError("Illegal right " + right + " for index " + index);
                    }
                });
                compareArrays(result1, m1, result3, result3Appender.offset(), request, point);
            }
        }
        System.out.println("             \rO'k");
    }
}
