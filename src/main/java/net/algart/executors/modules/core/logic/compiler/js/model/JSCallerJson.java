/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.executors.api.model.ExecutorJson;
import net.algart.executors.api.model.ExtensionJson;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class JSCallerJson extends ExecutorJson {
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

    public JSCallerJson() {
    }

    protected JSCallerJson(JsonObject json, Path file) {
        super(json, file);
        final JsonObject jsJson = json.getJsonObject("js");
        if (isJSExecutor() && jsJson == null) {
            throw new JsonException("Invalid executor configuration JSON" + (file == null ? "" : " " + file)
                    + ": \"js\" section required when \"language\" is \"js\"");
        }
        this.js = jsJson == null ? null : new JSConf(jsJson, file);
    }

    public static JSCallerJson read(Path executorJsonFile) throws IOException {
        Objects.requireNonNull(executorJsonFile, "Null executorJsonFile");
        final JsonObject json = Jsons.readJson(executorJsonFile);
        return new JSCallerJson(json, executorJsonFile);
    }

    public static JSCallerJson readIfValid(Path executorJsonFile) {
        Objects.requireNonNull(executorJsonFile, "Null executorJsonFile");
        final JsonObject json;
        try {
            json = Jsons.readJson(executorJsonFile);
        } catch (IOException e) {
            // - usually called while scanning folder with .json-files, so, exception should not occur here
            throw new IOError(e);
        }
        if (!isExecutorJson(json)) {
            return null;
        }
        return new JSCallerJson(json, executorJsonFile);
    }

    public static List<JSCallerJson> readAllIfValid(Path containingJsonPath) throws IOException {
        return readAllIfValid(null, containingJsonPath);
    }

    public static List<JSCallerJson> readAllIfValid(
            List<JSCallerJson> result,
            Path containingJsonPath)
            throws IOException {
        return ExtensionJson.readAllJsonIfValid(result, containingJsonPath, JSCallerJson::readIfValid);
    }

    public static JSCallerJson valueOf(JsonObject executorJson) {
        return new JSCallerJson(executorJson, null);
    }

    public static JSCallerJson valueOf(String executorJsonString) {
        Objects.requireNonNull(executorJsonString, "Null executorJsonString");
        final JsonObject executorJson = Jsons.toJson(executorJsonString);
        return new JSCallerJson(executorJson, null);
    }

    public static JSCallerJson valueOfIfValid(String executorJsonString) {
        Objects.requireNonNull(executorJsonString, "Null executorJsonString");
        final JsonObject json = Jsons.toJson(executorJsonString);
        if (!isExecutorJson(json)) {
            return null;
        }
        return new JSCallerJson(json, null);
    }

    public final boolean isJSExecutor() {
        return GraalSourceContainer.JAVASCRIPT_LANGUAGE.equals(getLanguage());
    }

    public JSConf getJS() {
        return js;
    }

    public JSCallerJson setJS(JSConf js) {
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
        return "JSCallerJson{" +
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
