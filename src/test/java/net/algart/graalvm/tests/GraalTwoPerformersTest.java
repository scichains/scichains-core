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

import net.algart.graalvm.GraalPerformer;
import net.algart.graalvm.GraalPerformerContainer;
import net.algart.graalvm.GraalSourceContainer;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;

public class GraalTwoPerformersTest {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws ScriptException, InterruptedException {
        GraalPerformerContainer container1 = GraalPerformerContainer.getContainer(false);
        GraalPerformerContainer container2 = GraalPerformerContainer.getContainer(false);
        GraalPerformer performer1 = container1.performer("dummy");
        GraalPerformer performer2 = container2.performer("dummy");
        performer1.performJS("var a = 'a'; var b = 'b'; print(a)");
        performer2.performJS("print(typeof(a))");
        System.out.println();

        GraalSourceContainer source1 = GraalSourceContainer.newLiteral();
        source1.setModuleJS("function exec() { print(a, 'module1') }\nexec\n", "module");
        GraalSourceContainer source2 = GraalSourceContainer.newLiteral();
        source2.setModuleJS("function exec() { print('module2') }\nexec\n", "module");
        // - if we use here the same module name, it will work normally only under DIFFERENT performes
        final Value exec1 = performer1.perform(source1);
        final Value exec2 = performer2.perform(source2);
        exec1.execute();
        exec2.execute();
        exec1.execute();
        exec2.execute();
    }
}
