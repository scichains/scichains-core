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

package net.algart.jep.tests;

import jep.*;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

/**
 * Illustrates possible conflicts between Python packages and available Java packages.
 */
public class SimpleJepForImport {
    private static void tryToImport(Interpreter interp, String moduleName) {
        tryToImport(interp, moduleName, null);
    }

    private static void tryToImport(Interpreter interp, String moduleName, String from) {
        try {
            System.out.printf("Importing %s%s (isJavaPackage: %s)...%n",
                    moduleName,
                    from == null ? "" : " from " + from,
                    ClassList.getInstance().isJavaPackage(moduleName));
            interp.exec((from == null ? "" : "from " + from + " ")
                    + "import " + moduleName);
        } catch (JepException e) {
            System.out.println(e);
        }
    }

    private static void correctClassLoader(Thread thread) {
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        if (contextClassLoader == null) {
            thread.setContextClassLoader(SimpleJepForImport.class.getClassLoader());
            // - avoiding bug in Jep: it uses getContextClassLoader, but
            // it can be null while calling from JNI
        }
    }
    public static void main(String[] args) throws FileNotFoundException {
        correctClassLoader(Thread.currentThread());
        final String root = "src/test/resources/python_tests";
//        final String root = "build/python/lib";
        final String root2 = args.length < 1 ? null : Paths.get(args[0]).toAbsolutePath().toString();
        System.out.printf("Adding new root: %s%n", root);
        JepConfig config = new JepConfig();
        config.addIncludePaths(root);
        if (root2 != null) {
            config.addIncludePaths(root2);
        }
        SharedInterpreter.setConfig(config);
        Interpreter interp = new SharedInterpreter();
        tryToImport(interp, SimpleJepForImport.class.getSimpleName(), SimpleJepForImport.class.getPackageName());
        tryToImport(interp, "tests.SimpleTest");
        tryToImport(interp, "algart");
        tryToImport(interp, "algart.api");
        tryToImport(interp, "net");
        tryToImport(interp, "net.algart");
        tryToImport(interp, "net.algart.pyth");
        tryToImport(interp, "net.algart.pyth.api");
        tryToImport(interp, "net.algart.pyth.api.my_lib");

    }
}
