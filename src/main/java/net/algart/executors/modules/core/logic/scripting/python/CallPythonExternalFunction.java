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

import net.algart.jep.JepPerformer;

public final class CallPythonExternalFunction extends AbstractCallPython {
    private String moduleName = "python_lib_demo_simple.SimpleDemo";

    public CallPythonExternalFunction() {
    }

    public String getModuleName() {
        return moduleName;
    }

    public CallPythonExternalFunction setModuleName(String moduleName) {
        moduleName = nonNull(moduleName).trim();
        if (!moduleName.matches("\\S+")) {
            throw new IllegalArgumentException("Python module name must not contain space characters");
        }
        this.moduleName = moduleName;
        return this;
    }

    @Override
    protected String code() {
        return JepPerformer.importCode(moduleName, getMainFunctionName());
        //TODO!! create full code for calling external "mod."+getMainFunctionName()
    }

    @Override
    protected String executorName() {
        return "Python external function";
    }
}
