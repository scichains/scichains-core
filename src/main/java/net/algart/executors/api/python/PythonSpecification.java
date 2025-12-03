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
import net.algart.executors.api.jep.JepAPI;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.jep.additions.JepType;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class PythonSpecification extends ExecutorSpecification {
    public static final class Python extends AbstractConvertibleToJson {
        public static final String DEFAULT_FUNCTION = "execute";

        private String module;
        private String parametersClass = JepAPI.STANDARD_API_PARAMETERS_CLASS;
        private String inputsClass = JepAPI.STANDARD_API_INPUTS_CLASS;
        private String outputsClass = JepAPI.STANDARD_API_OUTPUTS_CLASS;
        private String className = null;
        private String function = DEFAULT_FUNCTION;
        private JepType jepType = JepType.NORMAL;

        public Python() {
        }

        private Python(JsonObject json, Path file) {
            this.module = Jsons.reqString(json, "module", file);
            this.parametersClass = json.getString("parameters_class", parametersClass);
            this.inputsClass = json.getString("inputs_class", inputsClass);
            this.outputsClass = json.getString("outputs_class", outputsClass);
            this.className = json.getString("class", null);
            this.function = json.getString("function", function);
            final String jepTypeName = json.getString("jepType", JepType.NORMAL.typeName());
            this.jepType = JepType.fromTypeName(jepTypeName).orElseThrow(
                    () -> Jsons.badValue(json, "jepType", jepTypeName, file));
        }

        public String getModule() {
            return module;
        }

        public Python setModule(String module) {
            this.module = nonEmpty(module);
            return this;
        }

        public String getParametersClass() {
            return parametersClass;
        }

        public Python setParametersClass(String parametersClass) {
            this.parametersClass = nonEmpty(parametersClass);
            return this;
        }

        public String getInputsClass() {
            return inputsClass;
        }

        public Python setInputsClass(String inputsClass) {
            this.inputsClass = nonEmpty(inputsClass);
            return this;
        }

        public String getOutputsClass() {
            return outputsClass;
        }

        public Python setOutputsClass(String outputsClass) {
            this.outputsClass = nonEmpty(outputsClass);
            return this;
        }

        public String getClassName() {
            return className;
        }

        public Python setClassName(String className) {
            this.className = className;
            return this;
        }

        public String getFunction() {
            return function;
        }

        public Python setFunction(String function) {
            this.function = nonEmpty(function);
            return this;
        }

        public JepType getJepType() {
            return jepType;
        }

        public Python setJepType(JepType jepType) {
            this.jepType = nonNull(jepType);
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
            return "Python{" +
                   "module='" + module + '\'' +
                   ", parametersClass='" + parametersClass + '\'' +
                   ", inputsClass='" + inputsClass + '\'' +
                   ", outputsClass='" + outputsClass + '\'' +
                   ", className='" + className + '\'' +
                   ", function='" + function + '\'' +
                   ", jepType=" + jepType +
                   '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("module", module);
            builder.add("parameters_class", parametersClass);
            builder.add("inputs_class", inputsClass);
            builder.add("outputs_class", outputsClass);
            if (className != null) {
                builder.add("class", className);
            }
            builder.add("function", function);
            builder.add("jepType", jepType.name());
        }
    }

    private Python python = null;

    public PythonSpecification() {
    }

    protected PythonSpecification(JsonObject json, Path file) {
        super(json, file);
        final JsonObject pythonJson = json.getJsonObject("python");
        if (isPythonExecutor() && pythonJson == null) {
            throw new JsonException(
                    "Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                    + ": \"python\" section required when \"language\" is \"python\"");
        }
        this.python = pythonJson == null ? null : new Python(pythonJson, file);
    }

    public static PythonSpecification read(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        return new PythonSpecification(json, specificationFile);
    }

    public static PythonSpecification readIfValid(Path specificationFile) {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(specificationFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new PythonSpecification(json, specificationFile);
    }

    public static List<PythonSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return readAllIfValid(null, containingJsonPath);
    }

    public static List<PythonSpecification> readAllIfValid(
            List<PythonSpecification> result,
            Path containingJsonPath)
            throws IOException {
        return ExecutorSpecification.readAllJsonIfValid(
                result, containingJsonPath, PythonSpecification::readIfValid);
    }

    public static PythonSpecification of(JsonObject specificationJson) {
        return new PythonSpecification(specificationJson, null);
    }

    public static PythonSpecification of(String specificationString) {
        Objects.requireNonNull(specificationString, "Null specificationString");
        final JsonObject executorSpecification = Jsons.toJson(specificationString);
        return new PythonSpecification(executorSpecification, null);
    }

    public final boolean isPythonExecutor() {
        return "python".equals(getLanguage());
    }

    public Python getPython() {
        return python;
    }

    public PythonSpecification setPython(Python python) {
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
        return "PythonSpecification{" +
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
