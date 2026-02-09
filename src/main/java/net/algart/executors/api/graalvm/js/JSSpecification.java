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

package net.algart.executors.api.graalvm.js;

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.graalvm.GraalSourceContainer;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class JSSpecification extends ExecutorSpecification {
    public static final class JS extends AbstractConvertibleToJson {

        public static final String DEFAULT_FUNCTION = "execute";

        private String module;
        private String function = DEFAULT_FUNCTION;

        public JS() {
        }

        private JS(JsonObject json, Path file) {
            this.module = Jsons.reqString(json, "module", file);
            this.function = json.getString("function", function);
        }

        public String getModule() {
            return module;
        }

        public JS setModule(String module) {
            this.module = nonEmpty(module);
            return this;
        }

        public String getFunction() {
            return function;
        }

        public JS setFunction(String function) {
            this.function = nonEmpty(function);
            return this;
        }

        @Override
        public void checkCompleteness() {
            checkNull(module, "module");
        }

        @Override
        public String toString() {
            return "JS{" +
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

    private JS js = null;

    public JSSpecification() {
    }

    protected JSSpecification(JsonObject json, Path file) {
        super(json, file);
        final JsonObject jsJson = json.getJsonObject("js");
        if (isJSExecutor() && jsJson == null) {
            throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                    + ": \"js\" section required when \"language\" is \"js\"");
        }
        this.js = jsJson == null ? null : new JS(jsJson, file);
    }

    public static JSSpecification read(Path specificationFile) throws IOException {
        Objects.requireNonNull(specificationFile, "Null specificationFile");
        final JsonObject json = Jsons.readJson(specificationFile);
        return new JSSpecification(json, specificationFile);
    }

    public static JSSpecification readIfValid(Path specificationFile) {
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
        return new JSSpecification(json, specificationFile);
    }

    public static List<JSSpecification> readAllIfValid(Path containingJsonPath) throws IOException {
        return readAllIfValid(null, containingJsonPath);
    }

    public static List<JSSpecification> readAllIfValid(
            List<JSSpecification> result,
            Path containingJsonPath)
            throws IOException {
        return ExecutorSpecification.readAllJsonIfValid(
                result, containingJsonPath, JSSpecification::readIfValid);
    }

    public static JSSpecification of(JsonObject specificationJson) {
        return new JSSpecification(specificationJson, null);
    }

    public static JSSpecification of(String specificationString) {
        Objects.requireNonNull(specificationString, "Null specificationString");
        final JsonObject executorSpecification = Jsons.toJson(specificationString);
        return new JSSpecification(executorSpecification, null);
    }

    public final boolean isJSExecutor() {
        return GraalSourceContainer.JAVASCRIPT_LANGUAGE.equals(getLanguage());
    }

    public JS getJS() {
        return js;
    }

    public JSSpecification setJS(JS js) {
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
        return "JSSpecification{" +
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

// Obsolete idea: the logic of ofIfValid is not in line with Java traditions,
// and if we are going to do something similar, it would be better to make
// OptionalJSSpecification> from(...)
//
//    public static JSSpecification ofIfValid(String specificationString) {
//        Objects.requireNonNull(specificationString, "Null specificationString");
//        final JsonObject json = Jsons.toJson(specificationString);
//        if (!isExecutorSpecification(json)) {
//            return null;
//        }
//        return new JSSpecification(json, null);
//    }
}
