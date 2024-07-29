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

import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Range;
import net.algart.math.RectangularArea;

public class SNumbersRectangularAreasTest {
    private static void test(IRectangularArea area) {
        SNumbers numbers = new SNumbers().setTo(area);
        IRectangularArea copy = numbers.toIRectangularArea();
        if (!area.equals(copy)) {
            throw new AssertionError(area);
        }
        System.out.println("Tested " + area + " <-> " + numbers);
    }

    private static void test(RectangularArea area) {
        SNumbers numbers = new SNumbers().setTo(area);
        RectangularArea copy = numbers.toRectangularArea();
        if (!area.equals(copy)) {
            throw new AssertionError(area);
        }
        System.out.println("Tested " + area + " <-> " + numbers);
    }

    public static void main(String[] args) {
        test(IRectangularArea.valueOf(1, 2, 10, 11));
        test(IRectangularArea.valueOf(IRange.valueOf(1, 10)));
        test(IRectangularArea.valueOf(123, 5, 523, 54125, 73, 1000));
        test(RectangularArea.valueOf(0.6, 2, 10, 11.3));
        test(RectangularArea.valueOf(Range.valueOf(1, 1.1)));
        test(RectangularArea.valueOf(1.23, 5, 52.3, 541.25, 7.3, 100.6));
    }
}
