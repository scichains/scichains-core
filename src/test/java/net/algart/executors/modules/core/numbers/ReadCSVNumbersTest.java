/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.numbers;

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.numbers.io.ReadCSVNumbers;
import net.algart.executors.modules.core.numbers.io.WriteCSVNumbers;

public final class ReadCSVNumbersTest {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("Usage: %s file1.csv file2.csv...%n", ReadCSVNumbersTest.class.getName());
            return;
        }
        for (String file : args) {
            try {
                final SNumbers numbers = ReadCSVNumbers.getInstance().setFile(file).readCSV();
                final String fileCopy = file + "-copy";
                WriteCSVNumbers.getInstance().setFile(fileCopy).setDelimiter(", ").writeCSV(numbers, new String[0]);
                System.out.printf("%s written to %s%n", numbers, fileCopy);
            } catch (Exception e) {
                System.err.println("Cannot read " + file);
                System.err.println(e.getMessage());
            }
        }
    }
}
