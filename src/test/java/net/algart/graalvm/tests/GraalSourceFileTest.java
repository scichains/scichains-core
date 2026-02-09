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

package net.algart.graalvm.tests;

import net.algart.graalvm.GraalContextCustomizer;
import net.algart.graalvm.GraalPerformer;
import net.algart.graalvm.GraalPerformerContainer;
import net.algart.graalvm.GraalSourceContainer;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class GraalSourceFileTest {

    public static void main(String[] args) throws ScriptException {
        final Path currentDirectory = Paths.get("src/test/java/net/algart/graalvm/tests");
        final String moduleFile = "./js/sometest.mjs";
        final Path modulePath = currentDirectory.resolve(Paths.get(moduleFile));

        for (int test = 1; test <= 10; test++) {
            System.out.printf("%n%nTest #%d%n", test);
            long t1 = System.nanoTime();
            @SuppressWarnings("resource") final GraalPerformerContainer.Local performerContainer =
                    GraalPerformerContainer
                            .getLocal(GraalContextCustomizer.ALL_ACCESS)
                            .setJsEsmEvalReturnsExports(true)
                            .setWorkingDirectory(currentDirectory.toAbsolutePath());
            long t2 = System.nanoTime();
            GraalSourceContainer sourceContainer = GraalSourceContainer.newFileContainer()
                    .setModuleJS(modulePath, "test");
            long t3 = System.nanoTime();
            final GraalPerformer performer = performerContainer.performer();
            long t4 = System.nanoTime();
            final Source source = sourceContainer.source();
            long t5 = System.nanoTime();
            Value func = performer.perform(source).getMember("simpleTest");
            long t6 = System.nanoTime();
            Value result = func.execute();
            long t7 = System.nanoTime();

            System.out.printf(Locale.US, "Creating context container: %.3f mcs%n" +
                            "Creating source container: %.3f mcs%n" +
                            "Getting performer: %.3f mcs%n" +
                            "Getting source: %.3f mcs%n" +
                            "Performing code: %.3f mcs%n" +
                            "Calling function: %.3f mcs%n%n",
                    (t2 - t1) * 1e-3, (t3 - t2) * 1e-3, (t4 - t3) * 1e-3, (t5 - t4) * 1e-3,
                    (t6 - t5) * 1e-3, (t7 - t6) * 1e-3);

            System.out.println("execute result: " + result);
            System.out.println("Function: " + func);
        }
    }
}