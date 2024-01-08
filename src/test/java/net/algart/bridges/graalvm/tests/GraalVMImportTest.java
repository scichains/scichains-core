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

package net.algart.bridges.graalvm.tests;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraalVMImportTest {

    public static void main(String[] args) throws ScriptException {
//        final String moduleFile = "./src/test/java/net/algart/bridges/graalvm/tests/js/sometest.mjs";
        final Path currentDirectory = Paths.get("src/test/java/net/algart/bridges/graalvm/tests");
        final String moduleFile = "./js/sometest.mjs";
        // - no difference, whether we use ./ in the beginning
        final Path modulePath = currentDirectory.resolve(Paths.get(moduleFile));
        System.out.println("Loading " + modulePath.toAbsolutePath());
        if (!Files.exists(modulePath)) {
            throw new IllegalStateException("No file at " + modulePath);
        }

        String src = "import {test} from '" + moduleFile + "';\n" +
                "const foo = test(['Literal script','A','B']);\n" +
                "export function myFunc(o) { return test(o); }\n" +
                "export function myFunc2(o) { return test(o); }\n" +
                "console.log('----- 5+3=' + (5+3));" +
                "console.log(foo);\n" +
                "[test, myFunc]";
        System.out.printf("Evaluating:%n*****%n%s%n*****%n", src);


        boolean useExports = false;
        final Context.Builder builder = Context.newBuilder("js")
                .allowAllAccess(true)
                .currentWorkingDirectory(currentDirectory.toAbsolutePath());
        if (useExports) {
            builder.allowExperimentalOptions(true).option("js.esm-eval-returns-exports", "true");

        }
        final Context context = builder.build();

        Source source = Source.newBuilder("js", src, "test.mjs").buildLiteral();
        Value result = context.eval(source);
        System.out.println("eval result: " + result);
        Value func = useExports ?
                result.getMember("myFunc2") :
                context.eval(source).getArrayElement(1);
        System.out.println("Function: " + func);

        System.out.println();
        System.out.println("Again, same source:");
        context.eval(source);
        // - does nothing!

        System.out.println();
        System.out.println("Again, new source with same name:");
        source = Source.newBuilder("js", src, "test.mjs").buildLiteral();
        context.eval(source);

        System.out.println();
        System.out.println("Again, new source with other name:");
        source = Source.newBuilder("js", src, "test1.mjs").buildLiteral();
        context.eval(source);

        System.out.println();
        System.out.println("Caling function");
        Value execute = func.execute(new int[]{11, 12, 13});
        System.out.println(execute);

//        final String jsObjectScript = "c = {a: 1, b: 'bb'}";
        final String jsObjectScript = "new Object()";
        final Value jsObject = context.eval(Source.create("js", jsObjectScript));
        jsObject.putMember("c", new double[] {111.0,111.1});
        jsObject.putMember("d", "Java-string");
        System.out.println(jsObject);
        System.out.println("Caling function");
        execute = func.execute(jsObject);
        System.out.println(execute);
    }
}