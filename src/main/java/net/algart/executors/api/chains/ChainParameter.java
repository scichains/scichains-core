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

package net.algart.executors.api.chains;

import jakarta.json.JsonValue;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.executors.api.system.ControlSpecification;
import net.algart.executors.api.system.ExecutorSpecification;

import java.util.Objects;

public final class ChainParameter {
    private final String name;
    private Object value = null;

    private ChainParameter(String name) {
        this.name = Objects.requireNonNull(name, "Null name");
    }

    public static ChainParameter newInstance(String name) {
        return new ChainParameter(name);
    }

    public static ChainParameter of(
            ChainBlock block,
            ChainSpecification.Block.Parameter parameter) {
        final ChainParameter result = newInstance(parameter.getName());
        assert result.value == null : "newInstance must set null value";
        result.loadValue(block, parameter.getValue());
        return result;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public ParameterValueType probableType(ChainBlock block, ParameterValueType defaultType) {
        Objects.requireNonNull(block, "Null block");
        final ControlSpecification control = controlSpecification(block);
        return control != null ? control.getValueType() : defaultType;
    }

    public String toScalar() {
        return value == null ? null : value.toString();
    }

    @Override
    public String toString() {
        return "ChainParameter}{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    private void loadValue(ChainBlock block, JsonValue parameterJsonValue) {
        ControlSpecification control = controlSpecification(block);
        if (control != null) {
            // can be null, for example, for system properties (obsolete concept)
            final ParameterValueType valueType = control.getValueType();
            final JsonValue defaultJsonValue = control.getDefaultJsonValue();
            final Object defaultValue = valueType.toParameter(defaultJsonValue);
            if (defaultValue != null) {
                this.value = defaultValue;
            }
            final Object parameterValue = valueType.toParameter(parameterJsonValue);
            if (parameterValue != null) {
                // - if the parameter is a correctly written value, it is returned
                this.value = parameterValue;
                return;
            }
        }
        // - if there is no information (from executor control) to set non-null value
        // with properly (efficient) type, we will treat the value as a string:
        // this is a suitable variant for string, boolean, integer and floating-point values
        Object stringValue = ParameterValueType.STRING.toParameter(parameterJsonValue);
        if (stringValue != null) {
            this.value = stringValue;
        }
        // - but if it is null, we keep the default value
    }

    private ControlSpecification controlSpecification(ChainBlock block) {
        ExecutorSpecification specification = block.getExecutorSpecification();
        return specification != null ? specification.getControl(this.name) : null;
    }
}
