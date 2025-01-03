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

package net.algart.bridges.graalvm.tests;

import net.algart.bridges.graalvm.GraalJSType;
import net.algart.bridges.graalvm.GraalPerformer;
import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.bridges.graalvm.api.GraalSafety;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class GraalContextImportTest {

    public static void main(String[] args) throws ScriptException {
        final Path currentDirectory = Paths.get("src/test/java/net/algart/bridges/graalvm/tests");
        final String moduleFile = "./js/sometest.mjs";
        // - no difference, whether we use ./ in the beginning
        final Path modulePath = currentDirectory.resolve(Paths.get(moduleFile));

        String src = "import {test} from '" + moduleFile + "';\n" +
                "const foo = test(['Literal script','A','B']);\n" +
                "export function myFunc(o) { return test(o); }\n" +
                "export function myFunc2() { return externalValue; }\n" +
//                "externalValue = 'overridden in JS'\n" +
//                "console.log('----- 5+3=' + (5+3));" +
//                "console.log(foo);\n" +
                "myFunc2";
        System.out.printf("Evaluating:%n*****%n%s%n*****%n", src);

        for (int test = 1; test <= 10; test++) {
            System.out.printf("%n%nTest #%d%n", test);
            long t1 = System.nanoTime();
            final GraalPerformerContainer.Local performerContainer = GraalPerformerContainer
                    .getLocal(GraalSafety.ALL_ACCESS)
//                    .setJS()
                    .setWorkingDirectory(currentDirectory.toAbsolutePath());
            long t2 = System.nanoTime();
            GraalSourceContainer sourceContainer = GraalSourceContainer.newLiteral()
                    .setModuleJS(src, "test");
//                    .setJS(GraalJSType.COMMON, src, "test.mjs");
            // - second variant is also possible
            long t3 = System.nanoTime();
            final GraalPerformer performer = performerContainer.performer();
            long t4 = System.nanoTime();
            final Source source = sourceContainer.source();
            long t5 = System.nanoTime();
            final Value bindings = performer.bindingsJS();
            // - will be slow without setJS()
            long t6 = System.nanoTime();
            bindings.putMember("externalValue", "**Value from java**");
            long t7 = System.nanoTime();
            Value func = performer.perform(source);
            long t8 = System.nanoTime();
            func.execute();
            long t9 = System.nanoTime();
//            final Value binding = performer.bindingsJS();
            final Context simpleContext = Context.create();
            simpleContext.getBindings("js");
            long t10 = System.nanoTime();

            System.out.printf(Locale.US, "Creating context container: %.3f mcs%n" +
                            "Creating source container: %.3f mcs%n" +
                            "Getting performer: %.3f mcs%n" +
                            "Getting source: %.3f mcs%n" +
                            "Getting JavaScript bindings: %.3f mcs%n" +
                            "Putting value into bindings: %.3f mcs%n" +
                            "Performing code: %.3f mcs%n" +
                            "Calling function: %.3f mcs%n" +
                            "Creating simple test context: %.3f mcs%n%n",
                    (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, (t5 - t4) * 1e-3,
                    (t6 - t5) * 1e-3, (t7 - t6) * 1e-3, (t8 - t7) * 1e-3, (t9 - t8) * 1e-3, (t10 - t9) * 1e-3);

            System.out.println("eval result: " + func);
            System.out.println("Function: " + func);

            System.out.println();
            System.out.println("Again, same source:");
            performer.perform(sourceContainer);
            // - does nothing!

            System.out.println();
            System.out.println("Again, new source with same name:");
            sourceContainer = GraalSourceContainer.newLiteral();
            sourceContainer.setJS(GraalJSType.MODULE, src, "test");
            performer.perform(sourceContainer);

            System.out.println();
            System.out.println("Again, new source with other name:");
            sourceContainer.setName("test1.mjs");
            performer.perform(sourceContainer);

            System.out.println();
            System.out.println("Calling function");
            t1 = System.nanoTime();
            Object intArray = new int[] {11, 12, 13};
            Value execute = func.execute(intArray);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calling function: %.3f mcs%n",
                    (t2 - t1) * 1e-3);
            System.out.println(execute);

//        final String jsObjectScript = "c = {a: 1, b: 'bb'}";
            final String jsObjectScript = "new Object()";
            final Value jsObject = performer.perform(Source.create("js", jsObjectScript));
            jsObject.putMember("c", new double[]{111.0, 111.1});
            jsObject.putMember("d", "Java-string");
            System.out.println(jsObject);
            System.out.println("Caling function");
            execute = func.execute(jsObject);
            System.out.println(execute);
        }
    }
}