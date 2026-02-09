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

package net.algart.executors.api.tests;

import net.algart.executors.modules.core.common.io.FileOperation;

import java.nio.file.Path;
import java.util.List;

public class RelativeOSPathTest {
    public static void main(String[] args) {
        final String[] paths = {
                "",
                ".",
                "../.",
                "../",
                "/tmp/data.jpg",
                "some_folder/data.txt",
                "/some_folder/data.txt"
        };
        final Path current = FileOperation.currentOSPath();
        System.out.printf("Current OS path: %s%n", current);
        for (String s : paths) {
            System.out.printf("%nTesting \"%s\"...%n", s);
            for (Path p : List.of(Path.of(s), Path.of(s).toAbsolutePath(), Path.of(current + s))) {
                try {
                    System.out.printf("\"%s\" relative form:%n    \"%s\"%n", p,
                            FileOperation.relativizePathInsideCurrentOrParent(p));
                } catch (Exception e) {
                    System.out.printf("\"%s\" path cannot be relativized: %s%n", p, e.getMessage());
                }
            }
        }
    }
}
