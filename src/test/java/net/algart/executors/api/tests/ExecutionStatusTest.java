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

import net.algart.executors.api.ExecutionStatus;

import java.util.Locale;

public class ExecutionStatusTest {
    public static void main(String[] args) {
        final ExecutionStatus parent = ExecutionStatus.newNamedInstance("parent");
        parent.open(null);
        parent.setMessage(() -> "parent");
        System.out.println("parent: " + parent);

        final ExecutionStatus child1 = ExecutionStatus.newInstance();
        child1.open(parent);
        child1.setMessage(() -> "child1");
        System.out.println("parent: " + parent);
        child1.close();
        System.out.println("parent: " + parent);

        final ExecutionStatus child2 = ExecutionStatus.newInstance();
        child2.open(parent);
//        parent.open(child2);
        child2.setMessage(() -> "child2");
        child2.setExecutorSimpleClassName(ExecutionStatusTest.class.getName());
        child2.setStartProcessingTimeStamp();
        child2.setClassInformationIncluded(true);
        System.out.println("parent: " + parent);
        System.out.println("child2: " + child2);

        final ExecutionStatus child3 = ExecutionStatus.newInstance();
        final ExecutionStatus child4 = ExecutionStatus.newInstance();
        child3.open(child2);
//        parent.open(child2);
        child4.open(child3);
        child3.setMessage(() -> "child3").setComment(() -> "some executor");
        child4.setMessage(() -> "child4");
        System.out.println("parent: " + parent);
        System.out.println("child2: " + child2);
        child3.clear();
        System.out.println("parent: " + parent);
        System.out.println("child2: " + child2);
        child4.close();
        child3.close();
        System.out.println("parent: " + parent);
        child2.close();
        System.out.println("parent: " + parent);
        child1.open(parent);
        child1.setMessage(() -> "child1 again");

        for (int test = 1; test <= 16; test++) {
            System.out.printf("Test #%d...%n", test);
            final int n = 1000000;
            long t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                child1.setMessageString((k + 1) + "/" + n + " executed");
            }
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "setMessageString: %.2f ns/call - %s%n",
                    (double) (t2 - t1) / n, parent);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                int index = k;
                child1.setMessage(() -> (index + 1) + "/" + n + " executed");
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "setMessage:       %.2f ns/call - %s%n",
                    (double) (t2 - t1) / n, parent);
        }

        System.out.println();
        child2.open(child1);
        System.out.println("parent again: " + parent);
    }
}
