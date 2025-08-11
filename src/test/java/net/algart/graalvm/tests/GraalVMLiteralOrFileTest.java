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

package net.algart.graalvm.tests;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraalVMLiteralOrFileTest {

    public static void main(String[] args) throws ScriptException, IOException {
//        final String moduleFile = "./src/test/java/net/algart/graalvm/tests/js/sometest.mjs";
        final Path currentDirectory = Paths.get("src/test/java/net/algart/graalvm/tests");
        final String moduleFile = "./js/sometest.mjs";
        // - no difference, whether we use ./ in the beginning
        final Path modulePath = currentDirectory.resolve(Paths.get(moduleFile));
        System.out.println("Loading " + modulePath.toAbsolutePath());

        String src = "import {test} from '" + moduleFile + "';\n" +
                "test";

        @SuppressWarnings("resource")
        Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .currentWorkingDirectory(currentDirectory.toAbsolutePath()).build();

        Source sourceLiteral = Source.newBuilder("js", src, "test.mjs").buildLiteral();
        System.out.printf("Evaluating literal:%n*****%n%s%n*****%n", sourceLiteral.getCharacters());
        System.out.println("Name: " + sourceLiteral.getName());
        System.out.println("Language: " + sourceLiteral.getLanguage());
        Source sourceLiteralOther = Source.newBuilder("js", src, "test.js").buildLiteral();
        System.out.println("Equality to other literal: " + sourceLiteral.equals(sourceLiteralOther));
        System.out.println();

        Value func = context.eval(sourceLiteral);
        System.out.println("Function: " + func);
        System.out.println();

        System.out.println("Caling function");
        Object intArray = new int[] {11, 12, 13};
        Value execute = func.execute(intArray);
        System.out.println("Function result: " + execute);

        System.out.println();
        System.out.println();
        File file = modulePath.toFile();
        Source sourceInFile = Source.newBuilder("js", file).build();
        System.out.printf("Evaluating file:%n*****%n%s%n*****%n", sourceInFile.getCharacters());
        System.out.println("Name: " + sourceInFile.getName());
        System.out.println("Language: " + sourceInFile.getLanguage());
        Source sourceInFileOther = Source.newBuilder("js", file).build();
        System.out.println("Equality to other file: " + sourceInFile.equals(sourceInFileOther));
        System.out.println();

        func = context.eval(sourceInFile);
        System.out.println("Function: " + func);
        System.out.println();

        System.out.println("Caling function");
        execute = func.execute();
        System.out.println("Function result: " + execute);

    }
}