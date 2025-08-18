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

package net.algart.executors.api.js.core;

import net.algart.graalvm.GraalPerformer;
import net.algart.graalvm.GraalSourceContainer;
import org.graalvm.polyglot.Value;

public final class CallJSFunction extends AbstractCallJS {
    private String code =
            """
                    function execute(params, inputs, outputs) {
                        return "Hello from JavaScript function!"
                    }
                    """;

    private final GraalSourceContainer javaScriptCode = GraalSourceContainer.newLiteral();
    private volatile Value mainFunction = null;

    public CallJSFunction() {
    }

    public String getCode() {
        return code;
    }

    public CallJSFunction setCode(String code) {
        this.code = nonEmptyTrimmed(code);
        return this;
    }

    @Override
    protected String code() {
        return code;
    }

    @Override
    protected void compileSource() {
        final String mainFunctionName = getMainFunctionName();
        final String code = GraalPerformer.addReturningJSFunction(code(), mainFunctionName);
        javaScriptCode.setModuleJS(code, "main_code");
        // - name "main_code" is not important: we will not share this performer (Graal context) with other
        // executors; but if we want to use several scripts INSIDE the executor, they must have different module names
        final boolean changed = javaScriptCode.changed();
        if (changed) {
            logDebug(() -> "Changing code/settings of \"" + mainFunctionName + "\" detected: rebuilding performer");
            closePerformerContainer();
        }
    }

    @Override
    protected void executeSource(GraalPerformer performer ) {
        if (mainFunction == null) {
            // no sense to perform ECMA module if it was not changed: re-executing will have no effect
            mainFunction = performer.perform(javaScriptCode);
        }
    }

    @Override
    protected void closePerformerContainer() {
        this.mainFunction = null;
        // - enforce re-creating this function by perform()
        super.closePerformerContainer();
    }

    @Override
    protected Value callFunction(Value graalParameters, Value graalInputs, Value graalOutputs) {
        if (mainFunction == null) {
            throw new IllegalStateException(getClass() + " is not initialized");
        }
        return mainFunction.execute(graalParameters, graalInputs, graalOutputs);
    }

    @Override
    protected String executorName() {
        return "JavaScript function";
    }
}
