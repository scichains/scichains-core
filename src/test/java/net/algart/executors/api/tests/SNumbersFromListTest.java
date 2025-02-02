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

import net.algart.executors.api.data.SNumbers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SNumbersFromListTest {
    private static void test(Collection<?> numbers, int blockLength) {
        final SNumbers result;
        try {
            result = SNumbers.of(numbers, blockLength);
            System.out.println(numbers + " converted to " + result);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        final List<Integer> numbers1 = List.of(1, 2, 3, 4);
        test(numbers1, 2);

        final List<? extends Number> numbers2 = List.of(1, 2.0, 3, 4);
        test(numbers2, 1);

        List<Object> numbers3 = new ArrayList<>();
        numbers3.add(1.0);
        numbers3.add(2.0);
        numbers3.add(null);
        numbers3.add(4.0);
        test(numbers3, 2);

        test(List.of(), 2);

        test(List.of((byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1), 2);

        test(List.of(1, 2, "123"), 1);
    }
}
