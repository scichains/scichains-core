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

package net.algart.executors.api.python.core;

import net.algart.jep.additions.AtomicPyObject;
import net.algart.jep.additions.JepInterpretation;

public final class CallPythonExternalFunction extends AbstractCallPython {
    private static final String STANDARD_API_FILE_TO_IMPORT_FIELD = "_env_file_to_import";
    private static final String EXTERNAL_EXECUTE_CODE = """
            def _execute_external(params, inputs, outputs):
                _external_module = params._env.import_file(params.%s)
                return _external_module.%s(params, inputs, outputs)
            """;

    private String pyFile = "";
    private String moduleName = "pyalgart_lib_demo_simple.simple_demo";

    public CallPythonExternalFunction() {
        addOutputScalar(OUTPUT_CODE);
    }

    public String getPyFile() {
        return pyFile;
    }

    public CallPythonExternalFunction setPyFile(String pyFile) {
        this.pyFile = nonNull(pyFile).trim();
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public CallPythonExternalFunction setModuleName(String moduleName) {
        moduleName = nonNull(moduleName).trim();
        if (!moduleName.matches("\\S+")) {
            throw new IllegalArgumentException("Python module name must not contain space characters");
            // - most typical mistake; it will be tested thoroughly in code()
        }
        this.moduleName = moduleName;
        return this;
    }

    @Override
    protected String code() {
        final String mainFunctionName = getMainFunctionName();
        JepInterpretation.checkValidPythonFunctionName(mainFunctionName);
        if (!pyFile.isEmpty()) {
            return EXTERNAL_EXECUTE_CODE.formatted(STANDARD_API_FILE_TO_IMPORT_FIELD, mainFunctionName);
        } else {
            return JepInterpretation.importPythonCode(moduleName, mainFunctionName);
        }
    }

    @Override
    protected Object callFunction(
            AtomicPyObject pythonParameters,
            AtomicPyObject pythonInputs,
            AtomicPyObject pythonOutputs) {
        if (!pyFile.isEmpty()) {
            // translatePropertiesAndCurrentDirectory should not be used here!
            // All translations were already done with the working directory.
            pythonParameters.setAttribute(STANDARD_API_FILE_TO_IMPORT_FIELD, pyFile);
            return performer.invokeFunction("_execute_external",
                    pythonParameters.pyObject(),
                    pythonInputs.pyObject(),
                    pythonOutputs.pyObject());
        } else {
            return super.callFunction(pythonParameters, pythonInputs, pythonOutputs);
        }
    }

    @Override
    protected String executorName() {
        return "Python external function";
    }
}
