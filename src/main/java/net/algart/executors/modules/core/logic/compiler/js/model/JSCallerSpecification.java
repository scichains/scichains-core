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

package net.algart.executors.modules.core.logic.compiler.js.model;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.bridges.graalvm.GraalSourceContainer;
import net.algart.executors.api.extensions.ExtensionSpecification;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class JSCallerSpecification extends ExecutorSpecification {
    public static final class JSConf extends AbstractConvertibleToJson {

        public static final String DEFAULT_FUNCTION = "execute";

        private String module;
        private String function = DEFAULT_FUNCTION;

        public JSConf() {
        }

        private JSConf(JsonObject json, Path file) {
            this.module = Jsons.reqString(json, "module", file);
            this.function = json.getString("function", function);
        }

        public String getModule() {
            return module;
        }

        public JSConf setModule(String module) {
            this.module = nonEmpty(module);
            return this;
        }

        public String getFunction() {
            return function;
        }

        public JSConf setFunction(String function) {
            this.function = nonEmpty(function);
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(module, "module");
        }

        @Override
        public String toString() {
            return "JSConf{" +
                    "module='" + module + '\'' +
                    ", function='" + function + '\'' +
                    '}';
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("module", module);
            builder.add("function", function);
        }
    }

    private JSConf js = null;

    public JSCallerSpecification() {
    }

    protected JSCallerSpecification(JsonObject json, Path file) {
        super(json, file);
        final JsonObject jsJson = json.getJsonObject("js");
        if (isJSExecutor() && jsJson == null) {
            throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                    + ": \"js\" section required when \"language\" is \"js\"");
        }
        this.js = jsJson == null ? null : new JSConf(jsJson, file);
    }

    public static JSCallerSpecification read(Path executorSpecificationFile) throws IOException {
        Objects.requireNonNull(executorSpecificationFile, "Null executorSpecificationFile");
        final JsonObject json = Jsons.readJson(executorSpecificationFile);
        return new JSCallerSpecification(json, executorSpecificationFile);
    }

    public static JSCallerSpecification readIfValid(Path executorSpecificationFile) {
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
        return new JSCallerSpecification(json, executorSpecificationFile);
    }

    public static List<JSCallerSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return readAllIfValid(null, containingJsonPath);
    }

    public static List<JSCallerSpecification> readAllIfValid(
            List<JSCallerSpecification> result,
            Path containingJsonPath)
            throws IOException {
        return ExtensionSpecification.readAllJsonIfValid(result, containingJsonPath, JSCallerSpecification::readIfValid);
    }

    public static JSCallerSpecification valueOf(JsonObject executorSpecification) {
        return new JSCallerSpecification(executorSpecification, null);
    }

    public static JSCallerSpecification valueOf(String executorSpecificationString) {
        Objects.requireNonNull(executorSpecificationString, "Null executorSpecificationString");
        final JsonObject executorSpecification = Jsons.toJson(executorSpecificationString);
        return new JSCallerSpecification(executorSpecification, null);
    }

    public static JSCallerSpecification valueOfIfValid(String executorSpecificationString) {
        Objects.requireNonNull(executorSpecificationString, "Null executorSpecificationString");
        final JsonObject json = Jsons.toJson(executorSpecificationString);
        if (!isExecutorSpecification(json)) {
            return null;
        }
        return new JSCallerSpecification(json, null);
    }

    public final boolean isJSExecutor() {
        return GraalSourceContainer.JAVASCRIPT_LANGUAGE.equals(getLanguage());
    }

    public JSConf getJS() {
        return js;
    }

    public JSCallerSpecification setJS(JSConf js) {
        this.js = js;
        return this;
    }

    @Override
    public void checkCompleteness() {
        super.checkCompleteness();
        if (isJSExecutor()) {
            checkNull(js, "js");
        }
    }

    @Override
    public String toString() {
        return "JSCallerSpecification{" +
                "js=" + js +
                "}, extending " + super.toString();
    }

    @Override
    protected void buildLanguageJson(JsonObjectBuilder builder) {
        if (js != null) {
            builder.add("js", js.toJson());
        }
        super.buildLanguageJson(builder);
    }
}
