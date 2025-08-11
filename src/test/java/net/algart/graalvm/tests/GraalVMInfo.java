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

import net.algart.bridges.graalvm.GraalPerformerContainer;
import net.algart.bridges.graalvm.api.GraalSafety;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import javax.script.ScriptException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GraalVMInfo {
    public static void main(String[] args) throws ScriptException, InterruptedException {
        var container = GraalPerformerContainer.getLocal(GraalSafety.ALL_ACCESS);
        Context context = container.performer().context();

        final Engine e = context.getEngine();
        System.out.println("Version: " + e.getVersion());
        System.out.println("Implementation name: " + e.getImplementationName());
        System.out.println("Languages: " + e.getLanguages());
        System.out.println("Instruments: " + e.getInstruments());

        System.out.printf("%nOptions:%n%s", StreamSupport.stream(e.getOptions().spliterator(), false)
                .map(Object::toString).collect(Collectors.joining(String.format("%n"))));


    }
}
