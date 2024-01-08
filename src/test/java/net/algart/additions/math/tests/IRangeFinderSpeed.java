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

import net.algart.additions.math.IRangeConsumer;
import net.algart.additions.math.IRangeFinder;
import net.algart.additions.math.IRangeFinderMeasuringTime;
import net.algart.additions.math.IntArrayAppender;

import java.util.Locale;
import java.util.Random;
import java.util.function.IntConsumer;

public class IRangeFinderSpeed {
    private static void printTime(long time, IRangeFinder finder, int numberOfRequests, int count, String msg) {
        System.out.printf(Locale.US,
                "%d %s requests found %d from %d, %s: %.5f ms, %.5f mcs/request%n",
                numberOfRequests, finder.getClass() == IRangeFinder.class ? "optimized" : "custom",
                count, finder.numberOfRanges(), msg,
                time * 1e-6, time * 1e-3 / numberOfRequests);
        if (finder instanceof IRangeFinderMeasuringTime) {
            System.out.printf(Locale.US,
                    "  first point: %.5f mcs/request; additional interval processing: %.5f mcs/request%n",
                    ((IRangeFinderMeasuringTime) finder).timePoint() * 1e-3 / numberOfRequests,
                    ((IRangeFinderMeasuringTime) finder).timeIntervalsAddition() * 1e-3 / numberOfRequests);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Usage: %s numberOfIntervals numberOfRequests%n",
                    IRangeFinderSpeed.class.getName());
            return;
        }
        final int numberOfIntervals = Integer.parseInt(args[0]);
        final int numberOfRequests = Integer.parseInt(args[1]);

        final Random rnd = new Random(33);
        long minTimePointToArray = Long.MAX_VALUE;
        long minTimeInterval1ToArray = Long.MAX_VALUE;
        for (int test = 1; test <= 32; test++) {
            System.out.printf("%nTest #%d for %d intervals and %d requestst...%n",
                    test, numberOfIntervals, numberOfRequests);
            final int[] allMin = new int[numberOfIntervals];
            final int[] allMax = new int[numberOfIntervals];
            final int[] rMin = new int[numberOfRequests];
            final int[] rMax = new int[numberOfRequests];
            for (int k = 0; k < numberOfIntervals; k++) {
                allMin[k] = -500 + rnd.nextInt(10000);
                allMax[k] = allMin[k] + rnd.nextInt(500);
            }
            for (int k = 0; k < numberOfRequests; k++) {
                rMin[k] = -300 + rnd.nextInt(8000);
                final int inc = rnd.nextInt(4) == 0 ? rnd.nextInt(300) : rnd.nextInt(30);
                rMax[k] = rMin[k] + inc;
            }
            long t1 = System.nanoTime();
            IRangeFinder finderSlow = IRangeFinder.getEmptyUnoptimizedInstance().setRanges(allMin, allMax);
            long t2 = System.nanoTime();
            IRangeFinder finder = IRangeFinder.getInstance(allMin, allMax);
            long t3 = System.nanoTime();
            System.out.printf(Locale.US, "Optimized: %s, simple: %s%n", finder, finderSlow);
            System.out.printf(Locale.US, "Building tree: %.5f ms simple, %.5f ms optimized%n",
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
            final int[] result = new int[numberOfIntervals];
            final int[] resultLeft = new int[numberOfIntervals];
            final int[] resultRight = new int[numberOfIntervals];
            final IntArrayAppender resultFiller = new IntArrayAppender(result);
            final IRangeConsumer allQuickFiller = (index, left, right) -> {
                final int offset = resultFiller.offset();
                resultFiller.accept(index);
                resultLeft[offset] = left;
                resultRight[offset] = right;
            };
            final IntConsumer allSimpleFiller = index -> {
                final int offset = resultFiller.offset();
                resultFiller.accept(index);
                resultLeft[offset] = allMin[index];
                resultRight[offset] = allMax[index];
            };
            IRangeFinderMeasuringTime finderMeasuringTime = new IRangeFinderMeasuringTime();
            finderMeasuringTime.setRanges(finder.left(), finder.right(), finder.numberOfRanges());

            t1 = System.nanoTime();
            int count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finderSlow.findIntersecting(rMin[request], rMax[request], resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finderSlow, numberOfRequests, count, "intersecting an interval");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finder.findIntersecting(rMin[request], rMax[request], IRangeConsumer.of(resultFiller.reset()));
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count,
                    "intersecting an interval (index+left+right)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finder.findIntersecting(rMin[request], rMax[request], resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting an interval");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                count += finder.findIntersecting(rMin[request], rMax[request], result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting an interval (> array)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                resultFiller.reset();
                finder.findIntersecting(rMin[request], rMax[request], allQuickFiller);
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count,
                    "intersecting an interval (> 3 arrays, detailed)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                resultFiller.reset();
                finder.findIntersecting(rMin[request], rMax[request], allSimpleFiller);
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting an interval (> 3 arrays)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finderSlow.findContaining(rMin[request], resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finderSlow, numberOfRequests, count, "containing a point");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                count += finderSlow.findContaining(rMin[request], result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finderSlow, numberOfRequests, count, "containing a point (> array)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finder.findContaining(rMin[request], IRangeConsumer.of(resultFiller.reset()));
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "containing a point (index+left+right)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                finder.findContaining(rMin[request], resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "containing a point");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                count += finder.findContaining(rMin[request], result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "containing a point (> array)");
            minTimePointToArray = Math.min(minTimePointToArray, t2 - t1);

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                finder.findIntersecting(min, min, resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting +0 intervals");

//            finder.setMeasureTiming(false);
            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                count += finder.findIntersecting(min, min, result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting +0 intervals (> array)");

            finderMeasuringTime.resetTiming();
            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                count += finderMeasuringTime.findIntersecting(min, min, result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finderMeasuringTime, numberOfRequests, count,
                    "intersecting +0 intervals (> array, with measuring)");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                finder.findIntersecting(min, min + 1, resultFiller.reset());
                count += resultFiller.offset();
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting +1 intervals");

            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                count += finder.findIntersecting(min, min + 1, result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finder, numberOfRequests, count, "intersecting +1 intervals (> array)");
            minTimeInterval1ToArray = Math.min(minTimeInterval1ToArray, t2 - t1);

            finderMeasuringTime.resetTiming();
            t1 = System.nanoTime();
            count = 0;
            for (int request = 0; request < numberOfRequests; request++) {
                final int min = rMin[request];
                count += finderMeasuringTime.findIntersecting(min, min + 1, result);
            }
            t2 = System.nanoTime();
            printTime(t2 - t1, finderMeasuringTime, numberOfRequests, count,
                    "intersecting +1 intervals (> array, with measuring)");

        }
        System.out.println("             \rO'k");
        System.out.printf(Locale.US,
                "%nMinimal time for containing a point (> array): %.5f ms, %.5f mcs/request%n",
                minTimePointToArray * 1e-6, minTimePointToArray * 1e-3 / numberOfRequests);
        System.out.printf(Locale.US,
                "Minimal time for intersecting +1 intervals (> array): %.5f ms, %.5f mcs/request%n",
                minTimeInterval1ToArray * 1e-6,minTimeInterval1ToArray * 1e-3 / numberOfRequests);
    }
}
