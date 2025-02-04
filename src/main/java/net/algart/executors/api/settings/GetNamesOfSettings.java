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

package net.algart.executors.api.settings;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.system.ExecutorSpecification;
import net.algart.json.Jsons;

import java.util.List;
import java.util.stream.Collectors;

public class GetNamesOfSettings extends AbstractInterpretSettings {
    public enum ResultType {
        RAW_LINES() {
            @Override
            String result(List<String> names, String jsonKey) {
                return String.join("\n", names);
            }
        },
        JSON() {
            @Override
            String result(List<String> names, String jsonKey) {
                nonEmpty(jsonKey, "Empty result json key");
                final JsonObjectBuilder builder = Json.createObjectBuilder();
                final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String name : names) {
                    arrayBuilder.add(name);
                }
                builder.add(jsonKey, arrayBuilder.build());
                return Jsons.toPrettyString(builder.build());
            }
        };

        abstract String result(List<String> names, String jsonKey);
    }

    private ResultType resultType = ResultType.RAW_LINES;
    private String resultJsonKey = "names";
    private boolean extractIntType = true;
    private boolean extractLongType = true;
    private boolean extractFloatType = true;
    private boolean extractDoubleType = true;
    private boolean extractBooleanType = true;
    private boolean extractStringType = true;
    private boolean extractEnumType = true;
    private boolean extractSettingsType = true;

    public GetNamesOfSettings() {
        addOutputScalar(DEFAULT_OUTPUT_PORT);
    }

    public ResultType getResultType() {
        return resultType;
    }

    public GetNamesOfSettings setResultType(ResultType resultType) {
        this.resultType = nonNull(resultType);
        return this;
    }

    public String getResultJsonKey() {
        return resultJsonKey;
    }

    public GetNamesOfSettings setResultJsonKey(String resultJsonKey) {
        this.resultJsonKey = nonNull(resultJsonKey);
        return this;
    }

    public boolean isExtractIntType() {
        return extractIntType;
    }

    public GetNamesOfSettings setExtractIntType(boolean extractIntType) {
        this.extractIntType = extractIntType;
        return this;
    }

    public boolean isExtractLongType() {
        return extractLongType;
    }

    public GetNamesOfSettings setExtractLongType(boolean extractLongType) {
        this.extractLongType = extractLongType;
        return this;
    }

    public boolean isExtractFloatType() {
        return extractFloatType;
    }

    public GetNamesOfSettings setExtractFloatType(boolean extractFloatType) {
        this.extractFloatType = extractFloatType;
        return this;
    }

    public boolean isExtractDoubleType() {
        return extractDoubleType;
    }

    public GetNamesOfSettings setExtractDoubleType(boolean extractDoubleType) {
        this.extractDoubleType = extractDoubleType;
        return this;
    }

    public boolean isExtractBooleanType() {
        return extractBooleanType;
    }

    public GetNamesOfSettings setExtractBooleanType(boolean extractBooleanType) {
        this.extractBooleanType = extractBooleanType;
        return this;
    }

    public boolean isExtractStringType() {
        return extractStringType;
    }

    public GetNamesOfSettings setExtractStringType(boolean extractStringType) {
        this.extractStringType = extractStringType;
        return this;
    }

    public boolean isExtractEnumType() {
        return extractEnumType;
    }

    public GetNamesOfSettings setExtractEnumType(boolean extractEnumType) {
        this.extractEnumType = extractEnumType;
        return this;
    }

    public boolean isExtractSettingsType() {
        return extractSettingsType;
    }

    public GetNamesOfSettings setExtractSettingsType(boolean extractSettingsType) {
        this.extractSettingsType = extractSettingsType;
        return this;
    }

    @Override
    public void process() {
        setSystemOutputs();
        // - important to do this before other operations, for an improbable case
        // when there is user's port with the same name UseSettings.EXECUTOR_JSON_OUTPUT_NAME
        final SettingsCombiner combiner = settingsCombiner();
        final List<String> names = combiner.specification().getControls().values().stream()
                .filter(this::isMatched).map(ExecutorSpecification.ControlConf::getName)
                .collect(Collectors.toList());
        getScalar().setTo(resultType.result(names, resultJsonKey));
    }

    private boolean isMatched(ExecutorSpecification.ControlConf controlConf) {
        return switch (controlConf.getValueType()) {
            case INT -> extractIntType;
            case LONG -> extractLongType;
            case FLOAT -> extractFloatType;
            case DOUBLE -> extractDoubleType;
            case BOOLEAN -> extractBooleanType;
            case STRING -> extractStringType;
            case ENUM_STRING -> extractEnumType;
            case SETTINGS -> extractSettingsType;
            default -> false;
        };
    }
}
