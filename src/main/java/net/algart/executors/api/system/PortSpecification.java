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

package net.algart.executors.api.system;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.data.DataType;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;

import java.nio.file.Path;
import java.util.Objects;

public final class PortSpecification extends AbstractConvertibleToJson {
    private String name;
    private DataType valueType;
    private String caption = null;
    private String hint = null;
    private boolean advanced = false;

    public PortSpecification() {
    }

    public PortSpecification(JsonObject json, Path file) {
        this.name = Jsons.reqString(json, "name", file);
        this.valueType = DataType.ofTypeNameOrNull(Jsons.reqString(json, "value_type", file));
        Jsons.requireNonNull(valueType, json, "value_type", file);
        this.caption = json.getString("caption", null);
        this.hint = json.getString("hint", null);
        this.advanced = json.getBoolean("advanced", false);
    }

    public String getName() {
        return name;
    }

    public PortSpecification setName(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
        return this;
    }

    public DataType getValueType() {
        return valueType;
    }

    public PortSpecification setValueType(DataType valueType) {
        this.valueType = Objects.requireNonNull(valueType, "Null valueType");
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public PortSpecification setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getHint() {
        return hint;
    }

    public PortSpecification setHint(String hint) {
        this.hint = hint;
        return this;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public PortSpecification setAdvanced(boolean advanced) {
        this.advanced = advanced;
        return this;
    }

    public boolean isCompatible(PortSpecification other) {
        Objects.requireNonNull(other, "Null other");
        return other.valueType == valueType;
    }

    @Override
    public void checkCompleteness() {
        checkNull(name, "name");
        checkNull(valueType, "valueType");
    }

    @Override
    public String toString() {
        return "Port{" +
                "name='" + name + '\'' +
                ", valueType=" + valueType +
                ", caption=" + caption +
                ", hint=" + hint +
                ", advanced=" + advanced +
                '}';
    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("name", name);
        builder.add("value_type", valueType.typeName());
        if (caption != null) {
            builder.add("caption", caption);
        }
        if (hint != null) {
            builder.add("hint", hint);
        }
        if (advanced) {
            builder.add("advanced", advanced);
        }
    }
}
