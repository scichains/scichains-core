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

package net.algart.tests;

public class MemoryUsageTest {
    private static class SimplestFloat {
        float a;
    }

    private static class SimplestDouble {
        double a;
    }

    private static class Float10 {
        float[] a = new float[10];
        double v;

        public double sum() {
            double sum = v;
            for (float v : a) {
                sum += v;
            }
            return sum;
        }
    }

    private static class Float100 {
        float[] a = new float[100];

        public double sum() {
            double sum = 0.0;
            for (float v : a) {
                sum += v;
            }
            return sum;
        }
    }


    private static final int COUNT = 1024 * 1024;

    private static void showMemory(String message) {
        System.gc();
        System.runFinalization();
        final Runtime rt = Runtime.getRuntime();
        for (int k = 0; k < 10; k++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            rt.freeMemory();
        }
        System.out.printf("%s: %.2f/%.2f MB, %.4f bytes/element%n",
                message,
                (rt.totalMemory() - rt.freeMemory()) * 1e-6,
                rt.maxMemory() * 1e-6,
                (double) (rt.totalMemory() - rt.freeMemory()) / COUNT);
    }

    private static void test1() {
        showMemory("Nothing");
        float[] data = new float[COUNT];
        showMemory("float[]");
        System.out.println();
    }

    private static void test2() {
        showMemory("Nothing");
        SimplestFloat[] data = new SimplestFloat[COUNT];
        showMemory("null[]");
        for (int k = 0; k < COUNT; k++) {
            data[k] = new SimplestFloat();
        }
        showMemory("SimplestFloat[]");
        double sum = 0.0;
        for (int k = 0; k < COUNT; k++) {
            sum += data[k].a;
        }
        System.out.println(sum);
        System.out.println();
    }

    private static void test3() {
        showMemory("Nothing");
        SimplestDouble[] data = new SimplestDouble[COUNT];
        showMemory("null[]");
        for (int k = 0; k < COUNT; k++) {
            data[k] = new SimplestDouble();
        }
        showMemory("SimplestDouble[]");
        double sum = 0.0;
        for (int k = 0; k < COUNT; k++) {
            sum += data[k].a;
        }
        System.out.println(sum);
        System.out.println();
    }

    private static void test4() {
        showMemory("Nothing");
        Float10[] data = new Float10[COUNT];
        showMemory("null[]");
        for (int k = 0; k < COUNT; k++) {
            data[k] = new Float10();
        }
        showMemory("Float10[]");
        double sum = 0.0;
        for (int k = 0; k < COUNT; k++) {
            sum += data[k].sum();
        }
        System.out.println(sum);
        System.out.println();
    }

    private static void test5() {
        showMemory("Nothing");
        Float100[] data = new Float100[COUNT];
        showMemory("null[]");
        for (int k = 0; k < COUNT; k++) {
            data[k] = new Float100();
        }
        showMemory("Float100[]");
        double sum = 0.0;
        for (int k = 0; k < COUNT; k++) {
            sum += data[k].sum();
        }
        System.out.println(sum);
        System.out.println();
    }

    public static void main(String[] args) {
        for (int testIndex = 1; testIndex <= 5; testIndex++) {
            System.out.printf("%nTest #%d%n", testIndex);
            test1();
            test2();
            test3();
            test4();
            test5();
        }
    }
}
