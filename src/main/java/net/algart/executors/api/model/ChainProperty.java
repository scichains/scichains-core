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

package net.algart.executors.api.model;

import jakarta.json.JsonValue;
import net.algart.executors.api.data.ParameterValueType;

import java.util.Objects;

public final class ChainProperty {
    private final String name;
    private final ParameterValueType type;
    private Object value = null;

    private ChainProperty(String name, ParameterValueType type) {
        this.name = Objects.requireNonNull(name, "Null name");
        this.type = Objects.requireNonNull(type, "Null property value type");
    }

    public static ChainProperty newInstance(String name, ParameterValueType type) {
        return new ChainProperty(name, type);
    }

    public static ChainProperty valueOf(ChainBlock block, ChainJson.ChainBlockConf.PropertyConf propertyConf) {
        final ChainProperty result = newInstance(propertyConf.getName(), propertyConf.getType());
        assert result.value == null : "newInstance must set null value";
        final JsonValue propertyValue = propertyConf.getValue();
        result.loadValue(block.executorJson, propertyValue);
        return result;
    }

    public String getName() {
        return name;
    }

    public ParameterValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String toScalar() {
        return value == null ? null : value.toString();
    }

    @Override
    public String toString() {
        return "ChainProperty{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", value=" + value +
                '}';
    }

    private void loadValue(ExecutorJson executorJson, JsonValue propertyJsonValue) {
        if (executorJson != null) {
            final ExecutorJson.ControlConf control = executorJson.getControl(this.name);
            if (control != null) {
                // can be null, for example, for system property (obsolete concept)
                final ParameterValueType valueType = control.getValueType();
                final JsonValue defaultJsonValue = control.getDefaultJsonValue();
                final Object defaultValue = valueType.toJavaObject(defaultJsonValue);
                // - null when defaultJsonValue=null
                if (defaultValue != null) {
                    this.value = defaultValue;
                }
                final Object propertyValue = valueType.toJavaObject(propertyJsonValue);
                // - null when propertyJsonValue=null
                if (propertyValue != null) {
                    // - if the property has a correctly written value, it overrides the default value
                    this.value = propertyValue;
                }
                return;
            }
        }
        this.value = ParameterValueType.STRING.toJavaObject(propertyJsonValue);
        // - if there is no information about executor control, we load the value as a string:
        // this is a suitable variant for string, boolean, integer and floating-point values
    }

    // Deprecated
    private void loadValue(ChainJson.ChainBlockConf.PropertyConf propertyConf) {
        final JsonValue jsonValue = propertyConf.getValue();
        Object value = type.toJavaObject(jsonValue);
        if (value != null) {
            this.value = value;
        }
        // Note: for ENUM_STRING we will have String property.
        // It is normal behavior: setter for enum should automatically convert String to enum,
        // like PropertySetter.EnumSetter class.
    }
}
