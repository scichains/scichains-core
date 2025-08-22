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

package net.algart.executors.api.graalvm.js.core;

import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.graalvm.GraalSourceContainer;
import net.algart.graalvm.JSInterpretation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CallJSExternalFunction extends AbstractCallJS {
    private final GraalSourceContainer jsFileContainer = GraalSourceContainer.newFileContainer();

    private String jsFile = "";

    public CallJSExternalFunction() {
        addOutputScalar(OUTPUT_CODE);
    }

    public String getJsFile() {
        return jsFile;
    }

    public CallJSExternalFunction setJsFile(String jsFile) {
        this.jsFile = nonEmptyTrimmed(jsFile);
        return this;
    }

    @Override
    protected String code() {
        return JSInterpretation.importJSCode(translateJsFile(), getMainFunctionName());
    }

    /*
    // Possible alternative way, requiring calling
    // .option("js.esm-eval-returns-exports", "true")
    // in the Context builder.
    @Override
    protected void compileSource() {
        jsFileContainer.setModuleJS(translateJsFile(), "main_module");
        // - name "main_module" is not important: we will not share this performer (Graal context) with other
        // executors; but if we want to use several scripts INSIDE the executor, they must have different module names
        final boolean changed = jsFileContainer.changed();
        if (changed) {
            logDebug(() -> "Changing file/settings \"" + jsFile + "\" detected: rebuilding performer");
            closePerformerContainer();
        }
    }

    @Override
    protected void executeSource(GraalPerformer performer ) {
        final String mainFunctionName = getMainFunctionName();
        if (mainFunction == null) {
            // no sense to perform ECMA module if it was not changed: re-executing will have no effect
            final Value module = performer.perform(jsFileContainer);
            mainFunction = module.getMember(mainFunctionName);
            if (mainFunction == null) {
                throw new IllegalArgumentException("JS module \"" + jsFile +
                        "\" does not export function \"" + mainFunctionName + "\"");
            }
        }
    }
    */

    @Override
    protected String executorName() {
        return "JavaScript external function";
    }

    private Path translateJsFile() {
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(jsFile, this);
    }

    //TODO!!
    public static String toJsModulePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Module file does not exist: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("Not a regular file: " + path);
        }

        String uri = path.toAbsolutePath().normalize().toUri().toString();
        // we have "file:///C:/dir/module.mjs" or "file:///home/user/module.mjs"

        uri = uri.replace("\\", "\\\\")
                .replace("'", "\\'");

        return uri;
    }
}
