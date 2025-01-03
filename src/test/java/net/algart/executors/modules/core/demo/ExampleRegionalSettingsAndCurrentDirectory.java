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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.Executor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public final class ExampleRegionalSettingsAndCurrentDirectory extends Executor {
    @Override
    public void process() {
        Path currentRelativePath = Paths.get("");
        double v = 1234567.89123;
        String s = String.format("Current OS directory: %s%n" +
                        "Current chain directory: %s%n" +
                        "%s in current format: %f",
                currentRelativePath.toAbsolutePath(),
                getCurrentDirectory(),
                v, v);
        getScalar().setTo(s);
    }

    public static void main(String[] args) {
        System.out.println("Default locate: " + Locale.getDefault());
        double v = 1234567.89123;
        System.out.printf(v + " = (default locale) %.4f%n", v);
        System.out.printf(Locale.FRANCE, v + " = (FRANCE locale) %.4f%n", v);
    }
}
