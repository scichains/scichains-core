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

package net.algart.executors.api.tests;

import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SScalar;

import java.util.Arrays;
import java.util.Date;

public class SScalarTest {
    private static void show(SScalar scalar) {
        String toDoubles = null, toDoubles3 = null;
        try {
            final double[] doubles = scalar.toDoubles();
            toDoubles = "double[" + doubles.length + "] = " + Arrays.toString(doubles);
        } catch (NumberFormatException e) {
            toDoubles = "{{EXCEPTION: " + e.getMessage() + "}}";
        }
        try {
            final double[] doubles = scalar.toDoubles(3);
            toDoubles3 = "double[" + doubles.length + "] = " + Arrays.toString(doubles);
        } catch (NumberFormatException | IllegalStateException e) {
            toDoubles3 = "{{EXCEPTION: " + e.getMessage() + "}}";
        }
        System.out.printf("Scalar value: %s;%n  toString(): \"%s\";%n  toDoubles(): %s;%n  toDoubles(3): %s%n",
                scalar.getValue() == null ? "<<null>>" : "\"" + scalar.getValue() + "\"",
                scalar.toString(),
                toDoubles,
                toDoubles3);
    }

    public static void main(String[] args) {
        SScalar scalar = new SScalar();
        show(scalar);
        scalar.setTo(123);
        show(scalar);
        scalar.setTo(123.0);
        show(scalar);
        scalar.setTo("");
        show(scalar);
        scalar.setTo(Arrays.asList("A", "B", "C"));
        show(scalar);
        scalar.setTo(new double[]{255, 255, 0});
        show(scalar);
        scalar.setToNull();
        show(scalar);
        scalar.setTo(new SScalar().setTo("Other scalar"));
        show(scalar);
        try {
            scalar.setTo(new SMat());
            show(scalar);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        scalar.setTo(new Date()); // some random object
        show(scalar);
        scalar.setTo("");
        show(scalar);
        scalar.setTo(" 123\r");
        show(scalar);
        scalar.setTo(" 123\n\t124\t; 125,1 ,126.11111");
        show(scalar);

        scalar.setTo(123456789);
        System.out.printf("%ntoDouble(): %s, toInt(): %s%n", scalar.toDouble(), scalar.toInt());
        scalar.setTo(12345678912345.0);
        try {
            System.out.printf("%ntoDouble(): %s, toInt(): %s%n", scalar.toDouble(), scalar.toInt());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
