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

package net.algart.graalvm;

import org.graalvm.polyglot.Value;

import javax.lang.model.SourceVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

public class JSInterpretation {
    private JSInterpretation() {
    }

    public static String importJSCode(String from, String... importList) {
        Objects.requireNonNull(from, "Null from");
        Objects.requireNonNull(importList, "Null import list");
        if (importList.length == 0) {
            throw new IllegalArgumentException("Empty import list in not allowed");
        }
        StringBuilder sb = new StringBuilder();
        for (String name : importList) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            checkValidJSName(name, "import");
            sb.append(name);
        }
        return "import { " + sb + " } from \"" + from + "\";\n";
    }

    public static String importJSCode(Path moduleFile, String... importList) {
        Objects.requireNonNull(moduleFile, "Null module file");
        if (!Files.exists(moduleFile)) {
            throw new IllegalArgumentException("JS module file does not exist: " + moduleFile);
        }
        if (!Files.isRegularFile(moduleFile)) {
            throw new IllegalArgumentException("JS module file is not a regular file: " + moduleFile);
        }
        // So, we can be sure that it is not absolutely "random" string
        final String from = moduleFile.toAbsolutePath().normalize().toUri().toString();
        // toUri provides a guarantee that we will have a string with standard regular structure:
        // "file:///C:/dir/module.mjs" or "file:///home/user/module.mjs"
        // Such a string cannot contain a dangerous embedded JavaScript code
        return importJSCode(from, importList);
    }

    public static String addJSExportFunction(String jsCode, String functionName, GraalPerformer performer) {
        Objects.requireNonNull(performer, "Null performer");
        return addJSExportFunction(jsCode, functionName, performer.isJsEsmEvalReturnsExports());
    }

    public static String addJSExportFunction(String jsCode, String functionName, boolean jsEsmEvalReturnsExports) {
        Objects.requireNonNull(jsCode, "Null jsCode");
        JSInterpretation.checkValidJSFunctionName(functionName);
        if (jsEsmEvalReturnsExports) {
            return jsCode + "\n\nexport {" + functionName + " }";
        } else {
            return jsCode + "\n\n" + functionName;
        }
    }

    public static Value addedJSModuleMember(Value module, String functionName, GraalPerformer performer) {
        Objects.requireNonNull(performer, "Null performer");
        return addedJSModuleMember(module, functionName, performer.isJsEsmEvalReturnsExports());
    }

    public static Value addedJSModuleMember(Value module, String functionName, boolean jsEsmEvalReturnsExport) {
        Objects.requireNonNull(module, "Null module");
        return jsEsmEvalReturnsExport ? module.getMember(functionName) : module;
    }

    public static Value smartModuleMember(Value module, String name, Supplier<Value> defaultValue) {
        Objects.requireNonNull(module, "Null module");
        Objects.requireNonNull(name, "Null name");
        Objects.requireNonNull(defaultValue, "Null defaultValue");
        return module.hasMember(name) ? module.getMember(name) : defaultValue.get();
        // hasMember() is necessary if we built the context with .option("js.esm-eval-returns-exports", "true")
        // Note that "hasMembers" is not a suitable check:
        // the object/function returned in the last line can contain members!

    }

    public static void checkValidJSFunctionName(String name) {
        checkValidJSName(name, "function");
    }

    private static void checkValidJSName(String name, String kind) {
        Objects.requireNonNull(name, "Null " + kind + " name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Empty JavaScript " + kind + " name \"" + name + "\" is not allowed");
        }
        if (!SourceVersion.isIdentifier(name)) {
            // - Strictly speaking, Java and JavaScript's requirements for identifiers are not identical,
            // but in practice this is not a problem: our goal is only to disable truly dangerous JS code.
            throw new IllegalArgumentException("Invalid JavaScript " + kind + " name \"" + name + "\": " +
                    "it contains illegal characters");
        }
    }
}
