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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraalSourceModuleSimpleTest {
    private static final String source = """
            export function simpleTest() {
                return "Hello!";
            }
            simpleTest
            """;

    public static void main(String[] args) throws ScriptException, IOException {
        boolean useReturnExports = args.length > 0 && args[0].equals("exports");
        System.out.println("useReturnExports: " + useReturnExports);
        Source.Builder sourceBuilder = Source.newBuilder("js", source,"myTest");
        sourceBuilder.mimeType("application/javascript+module");
        Source source = sourceBuilder.build();
        Context.Builder contextBuilder = Context.newBuilder("js");
        if (useReturnExports) {
            contextBuilder.option("js.esm-eval-returns-exports", "true");
            // - without this, the last line in mjs should return the necessary function
        }
        // Note: we use maximally pure mode! No allowAllAccess or similar calls.
        // So, we can freely use "js.esm-eval-returns-exports" even for GraalSafety.PURE
        try (Context context = contextBuilder.build()) {
            Value module = context.eval(source);
            System.out.println("Module/last line: " + module);
            Value func = module.getMember("simpleTest");
            if (func == null) {
                func = module;
            }
            // - This code will work in both cases:
            // useReturnExports=true (module.getMember)
            // and =false (then it uses the last line of sometest.mjs)
            System.out.println("Function simpleTest: " + func);
            Value funcSecond = module.getMember("toJsonString");
            System.out.println("Function toJsonString: " + funcSecond);
            Value result = func.execute();
            System.out.println("execute result: " + result);
        }
    }
}