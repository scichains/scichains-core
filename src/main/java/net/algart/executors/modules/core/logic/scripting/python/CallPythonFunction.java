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

package net.algart.executors.modules.core.logic.scripting.python;

public final class CallPythonFunction extends AbstractCallPython {
    private String code =
            "# from java.lang import System # - you may import Java classes\n" +
            "\n" +
            "def execute(params, inputs, outputs):\n" +
            "    # m = params._sys.local_import(\"my_module.py\")\n" +
            "    # - Uncomment the previous line to import \"my_module.py\" from the current chain directory\n" +
            "\n" +
            "    # outputs.x1 = inputs.x1 # - you can access inputs and outputs\n" +
            "    return \"Hello from Python function!\"\n";

    public CallPythonFunction() {
    }

    public String getCode() {
        return code;
    }

    public CallPythonFunction setCode(String code) {
        this.code = nonNull(code).trim();
        return this;
    }

    @Override
    protected String code() {
        return code;
    }

    @Override
    protected String executorName() {
        return "Python function";
    }
}
