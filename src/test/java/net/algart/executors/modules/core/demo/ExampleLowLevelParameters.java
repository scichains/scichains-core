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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.parameters.Parameters;

import java.util.Map;

/**
 * Testing access to parameters without exceptions.
 */
public final class ExampleLowLevelParameters extends ExecutionBlock {
    private static final System.Logger LOG = System.getLogger(ExampleLowLevelParameters.class.getName());

    private void checkParameter(StringBuilder sb, String name) {
        final Parameters parameters = parameters();
        sb.append("    ").append("getString: ");
        try {
            final String s = "\"" + parameters.getString(name) + "\"";
            sb.append(s);
        } catch (RuntimeException e) {
            sb.append(e);
        }
        sb.append("\n");

        sb.append("    ").append("getInteger: ");
        try {
            sb.append(parameters.getInteger(name));
        } catch (RuntimeException e) {
            sb.append(e);
        }
        sb.append("\n");

        sb.append("    ").append("getLong: ");
        try {
            sb.append(parameters.getLong(name));
        } catch (RuntimeException e) {
            sb.append(e);
        }
        sb.append("\n");

        sb.append("    ").append("getDouble: ");
        try {
            sb.append(parameters.getDouble(name));
        } catch (RuntimeException e) {
            sb.append(e);
        }
        sb.append("\n");

        sb.append("    ").append("getBoolean: ");
        try {
            sb.append(parameters.getBoolean(name));
        } catch (RuntimeException e) {
            sb.append(e);
        }
        sb.append("\n");
    }

    @Override
    public void execute() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : parameters().entrySet()) {
            final Object value = entry.getValue();
            sb.append(entry.getKey()).append("=").append(value);
            if (value != null) {
                sb.append(" - ").append(value.getClass().getName());
            }
            sb.append("\n");
            checkParameter(sb, entry.getKey());
        }
        getScalar().setTo(sb.toString());
    }

    /**
     * Example how block properties can be set from outside.
     */
    @Override
    public void onChangeParameter(String name) {
        LOG.log(System.Logger.Level.INFO, "Setting parameter " + name + " to " + parameters().get(name));
    }
}
