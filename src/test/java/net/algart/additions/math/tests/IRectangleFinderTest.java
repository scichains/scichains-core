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

package net.algart.additions.math.tests;

import net.algart.arrays.JArrays;
import net.algart.additions.math.IRectangleFinder;

import java.util.Arrays;
import java.util.Random;

public class IRectangleFinderTest {
    private static void compareArrays(
            int[] result1,
            int m1,
            int[] result2,
            int m2,
            int request,
            int minX,
            int maxX,
            int minY,
            int maxY) {
        Arrays.sort(result1, 0, m1);
        Arrays.sort(result2, 0, m2);
        if (m1 != m2 || !JArrays.arrayEquals(Arrays.copyOf(result1, m1), 0,
                Arrays.copyOf(result2, m2), 0, m1)) {
            throw new AssertionError("Different results at request #" + request
                    + "! " + m1 + " and " + m2
                    + " for " + minX + ".." + maxX + " x " + minY + ".." + maxY
                    + ":\n    " + JArrays.toString(Arrays.copyOf(result1, m1),
                    ", ", 512)
                    + "\n    " + JArrays.toString(Arrays.copyOf(result2, m2),
                    ", ", 512));
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.printf("Usage: %s numberOfRectangles totalWidth numberOfTests numberOfRequests%n",
                    IRectangleFinderTest.class.getName());
            return;
        }
        final int numberOfRectangles = Integer.parseInt(args[0]);
        final int totalWidth = Integer.parseInt(args[1]);
        final int numberOfTests = Integer.parseInt(args[2]);
        final int numberOfRequests = Integer.parseInt(args[3]);
        final long seed = 44153;
        final Random rnd = new Random(seed);
        System.out.printf("Testing %d rectangles: %d tests for %d requests/test; start seed %d%n",
                numberOfRectangles, numberOfTests, numberOfRequests, seed);
        for (int test = 0; test < numberOfTests; test++) {
            System.out.printf("Test #%d...\r", test);
            final int[] allMinX = new int[numberOfRectangles];
            final int[] allMaxX = new int[numberOfRectangles];
            final int[] allMinY = new int[numberOfRectangles];
            final int[] allMaxY = new int[numberOfRectangles];

            for (int k = 0; k < numberOfRectangles; k++) {
                allMinX[k] = -50 + rnd.nextInt(totalWidth);
                allMaxX[k] = allMinX[k] + rnd.nextInt(200);
                allMinY[k] = -50 + rnd.nextInt(totalWidth);
                allMaxY[k] = allMinY[k] + rnd.nextInt(200);
            }
            IRectangleFinder finder = IRectangleFinder.getEmptyInstance(rnd.nextBoolean());
            IRectangleFinder finderSlow = IRectangleFinder.getEmptyUnoptimizedInstance();
            final int[] result1 = new int[numberOfRectangles];
            final int[] result2 = new int[numberOfRectangles];
            final int[] indexes = new int[numberOfRectangles];
            final int n = rnd.nextInt(numberOfRectangles + 1);
            for (int k = 0; k < n; k++) {
                indexes[k] = rnd.nextInt(numberOfRectangles);
            }
            final boolean useCheckedIndexes = rnd.nextBoolean();
            if (n > 0 || rnd.nextBoolean()) {
                if (useCheckedIndexes) {
                    finder.setIndexedRectangles(allMinX, allMaxX, allMinY, allMaxY, indexes, n);
                    finderSlow.setIndexedRectangles(allMinX, allMaxX, allMinY, allMaxY, indexes, n);
                } else {
                    finder.setRectangles(allMinX, allMaxX, allMinY, allMaxY);
                    finderSlow.setRectangles(allMinX, allMaxX, allMinY, allMaxY);
                }
            }
            for (int request = 0; request < numberOfRequests; request++) {
//                System.out.printf("\r%d", request);
                final int minX = -50 + rnd.nextInt(totalWidth);
                final int maxX = minX + rnd.nextInt(20);
                final int minY = -50 + rnd.nextInt(totalWidth);
                final int maxY = minY - 50 + rnd.nextInt(20);
                int m1 = finder.findIntersecting(minX, maxX, minY, maxY, result1);
                int m2 = finderSlow.findIntersecting(minX, maxX, minY, maxY, result2);
                compareArrays(result1, m1, result2, m2, request, minX, maxX, minY, maxY);
                double x = minX + 3.0 * rnd.nextDouble();
                double y = minY + 3.0 * rnd.nextDouble();
                m1 = finder.findContaining(x, y, result1);
                m2 = 0;
                for (int k = 0; k < finder.numberOfRectangles(); k++) {
                    final int index = useCheckedIndexes ? indexes[k] : k;
                    if (allMaxX[index] >= x && allMinX[index] <= x && allMaxY[index] >= y && allMinY[index] <= y) {
                        result2[m2++] = k;
                    }
                }
                compareArrays(result1, m1, result2, m2, request, minX, maxX, minY, maxY);
            }
        }
        System.out.println("             \rO'k");
    }
}
