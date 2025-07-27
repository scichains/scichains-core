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

package net.algart.jep.tests;

import jep.Interpreter;
import jep.JepConfig;
import jep.SharedInterpreter;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleJepTest {
    static String pythonRoot() {
        final String root = "src/test/resources/python_tests";
        final Path path = Path.of(root).toAbsolutePath();
        System.out.printf("Using Python home: %s%n", path);
        if (!Files.isDirectory(path)) {
            throw new AssertionError(path + " is not an existing folder!");
        }
        return root;
    }

    static void configurePython(String root) {
        JepConfig config = new JepConfig();
        config.addIncludePaths(root);
        config.redirectStdout(System.out);
        config.redirectStdErr(System.err);
        // - necessary for correct working "print" method from IDE
        SharedInterpreter.setConfig(config);
    }

    public static void main(String[] args) throws FileNotFoundException {
        final String root = pythonRoot();
        configurePython(root);
        try (Interpreter interp = new SharedInterpreter()) {
            System.out.println("Interpreter: " + interp);
            System.out.println();
            interp.exec("from tests import SimpleTest");
            interp.exec("from java.lang import System\n");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(\"(java:) \" + s)");
            interp.exec("print(sys.path)");
            interp.exec("print(s)");
            interp.exec("result = SimpleTest.demo()");
            interp.exec("print(result)");
            interp.exec("System.out.println(\"(java:) \" + result)");
        }
    }
}
