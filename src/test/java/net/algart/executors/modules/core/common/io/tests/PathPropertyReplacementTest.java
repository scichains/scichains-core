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

package net.algart.executors.modules.core.common.io.tests;

import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.system.Gc;

import java.nio.file.Path;
import java.nio.file.Paths;

import static net.algart.executors.modules.core.common.io.PathPropertyReplacement.*;

public class PathPropertyReplacementTest {
    public static void main(String[] args) {
        for (String s : new String[]{
                "1234.dat", "%TEMP%", "$a", "%", "${a}", "$$$", "a${something}", "b${%some%}xxx"}) {
            System.out.printf("%s %s properties, probably %s%n",
                    s,
                    hasProperties(s) ? "CONTAINS" : "does not contain",
                    hasProbableProperties(s) ? "CONTAINS" : "does not contain");
        }
        System.out.println();
        Executor e = new Gc();
        e.setContextPath("c:/tmp/chain.json");
        System.out.println(translatePathProperties("myPath/${file.name.ext}; myPath/${file.name}/", e));
        System.out.println(translatePathProperties("myPath/${path.name.ext}; myPath/${path.name}/",
                Paths.get("chain.json")));
        e.setContextPath("/123.dir");
        System.out.println(translateProperties("${java.io.tmpdir}tmp/${file.name.ext}; ${path.name}/123",
                Paths.get("/123.dir")));
        // but path="/" will lead to exception
        System.out.println("First path property: " +
                firstPathProperty("${java.io.tmpdir}tmp/${file.name.ext}; ${path.name}/123"));
        System.out.println();

        String s = "c:/tmp/";
        final Path p = Paths.get(s).getFileSystem().getPath("\\aaa");
        System.out.println(Paths.get(s).resolve(p));
        System.out.println(p.isAbsolute() + " - " + p);
        System.out.println(translateTmpDir("%TEMP%\\test.dat"));
        System.out.println(translateTmpDir("%TEMP%test.dat"));
        System.out.println(translateTmpDir("%TEMP%/test.dat"));
        System.out.println(translateTmpDir("%TEMP%//test.dat"));
    }
}
