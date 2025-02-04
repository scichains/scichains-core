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

package net.algart.executors.api.python;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.bridges.jep.api.JepAPI;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class PythonCallerSpecification extends ExecutorSpecification {
    public static final class PythonConf extends AbstractConvertibleToJson {
        public static final String DEFAULT_FUNCTION = "execute";

        private String module;
        private String paramsClass = JepAPI.STANDARD_API_PARAMETERS_CLASS_NAME;
        private String inputsClass = JepAPI.STANDARD_API_INPUTS_CLASS_NAME;
        private String outputsClass = JepAPI.STANDARD_API_OUTPUTS_CLASS_NAME;
        private String className = null;
        private String function = DEFAULT_FUNCTION;

        public PythonConf() {
        }

        private PythonConf(JsonObject json, Path file) {
            this.module = Jsons.reqString(json, "module", file);
            this.paramsClass = json.getString("params_class", paramsClass);
            this.inputsClass = json.getString("inputs_class", inputsClass);
            this.outputsClass = json.getString("outputs_class", outputsClass);
            this.className = json.getString("class", null);
            this.function = json.getString("function", function);
        }

        public String getModule() {
            return module;
        }

        public PythonConf setModule(String module) {
            this.module = nonEmpty(module);
            return this;
        }

        public String getParamsClass() {
            return paramsClass;
        }

        public PythonConf setParamsClass(String paramsClass) {
            this.paramsClass = nonEmpty(paramsClass);
            return this;
        }

        public String getInputsClass() {
            return inputsClass;
        }

        public PythonConf setInputsClass(String inputsClass) {
            this.inputsClass = nonEmpty(inputsClass);
            return this;
        }

        public String getOutputsClass() {
            return outputsClass;
        }

        public PythonConf setOutputsClass(String outputsClass) {
            this.outputsClass = nonEmpty(outputsClass);
            return this;
        }

        public String getClassName() {
            return className;
        }

        public PythonConf setClassName(String className) {
            this.className = className;
            return this;
        }

        public String getFunction() {
            return function;
        }

        public PythonConf setFunction(String function) {
            this.function = nonEmpty(function);
            return this;
        }

        public boolean isClassMethod() {
            return className != null;
        }

        @Override
        public void checkCompleteness() {
            checkNull(module, "module");
        }

        @Override
        public String toString() {
            return "PythonConf{" +
                    "module='" + module + '\'' +
                    ", paramsClass='" + paramsClass + '\'' +
                    ", inputsClass='" + inputsClass + '\'' +
                    ", outputsClass='" + outputsClass + '\'' +
                    ", className='" + className + '\'' +
                    ", function='" + function + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("module", module);
            builder.add("params_class", paramsClass);
            builder.add("inputs_class", inputsClass);
            builder.add("outputs_class", outputsClass);
            if (className != null) {
                builder.add("class", className);
            }
            builder.add("function", function);
        }
    }

    private PythonConf python = null;

    public PythonCallerSpecification() {
    }

    protected PythonCallerSpecification(JsonObject json, Path file) {
        super(json, file);
        final JsonObject pythonJson = json.getJsonObject("python");
        if (isPythonExecutor() && pythonJson == null) {
            throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                    + ": \"python\" section required when \"language\" is \"python\"");
        }
        this.python = pythonJson == null ? null : new PythonConf(pythonJson, file);
    }

    public static PythonCallerSpecification read(Path executorSpecificationFile) throws IOException {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        final JsonObject json = Jsons.readJson(executorSpecificationFile);
        return new PythonCallerSpecification(json, executorSpecificationFile);
    }

    public static PythonCallerSpecification readIfValid(Path executorSpecificationFile) {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(executorSpecificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new PythonCallerSpecification(json, executorSpecificationFile);
    }

    public static List<PythonCallerSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return readAllIfValid(null, containingJsonPath);
    }

    public static List<PythonCallerSpecification> readAllIfValid(
            List<PythonCallerSpecification> result,
            Path containingJsonPath)
            throws IOException {
        return ExtensionSpecification.readAllIfValid(result, containingJsonPath, PythonCallerSpecification::readIfValid);
    }

    public static PythonCallerSpecification of(JsonObject executorSpecification) {
        return new PythonCallerSpecification(executorSpecification, null);
    }

    public static PythonCallerSpecification of(String executorSpecificationString) {
        Objects.requireNonNull(executorSpecificationString, "Null executorSpecificationString");
        final JsonObject executorSpecification = Jsons.toJson(executorSpecificationString);
        return new PythonCallerSpecification(executorSpecification, null);
    }

    public static PythonCallerSpecification ofIfValid(String executorSpecificationString) {
        Objects.requireNonNull(executorSpecificationString, "Null executorSpecificationString");
        final JsonObject json = Jsons.toJson(executorSpecificationString);
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new PythonCallerSpecification(json, null);
    }

    public final boolean isPythonExecutor() {
        return "python".equals(getLanguage());
    }

    public PythonConf getPython() {
        return python;
    }

    public PythonCallerSpecification setPython(PythonConf python) {
        this.python = python;
        return this;
    }

    @Override
    public void checkCompleteness() {
        super.checkCompleteness();
        if (isPythonExecutor()) {
            checkNull(python, "python");
        }
    }

    @Override
    public String toString() {
        return "PythonCallerSpecification{" +
                "python=" + python +
                "}, extending " + super.toString();
    }

    @Override
    protected void buildLanguageJson(JsonObjectBuilder builder) {
        if (python != null) {
            builder.add("python", python.toJson());
        }
        super.buildLanguageJson(builder);
    }
}
