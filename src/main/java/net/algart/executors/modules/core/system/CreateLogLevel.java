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

package net.algart.executors.modules.core.system;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SScalar;

import java.lang.System.Logger.Level;

public final class CreateLogLevel extends Executor {
    public static final String OUTPUT_LOGGABLE = "loggable";

    private String logLevel = "DEBUG";
    private String propertyNameToOverwriteLogLevel = "";

    public CreateLogLevel() {
        addInputScalar(DEFAULT_INPUT_PORT);
        addOutputScalar(DEFAULT_OUTPUT_PORT);
        addOutputScalar(OUTPUT_LOGGABLE);
    }

    public String getLogLevel() {
        return logLevel;
    }

    public CreateLogLevel setLogLevel(String logLevel) {
        this.logLevel = nonNull(logLevel);
        return this;
    }

    public CreateLogLevel setLogLevel(Level logLevel) {
        this.logLevel = nonNull(logLevel).getName();
        return this;
    }

    public String getPropertyNameToOverwriteLogLevel() {
        return propertyNameToOverwriteLogLevel;
    }

    public CreateLogLevel setPropertyNameToOverwriteLogLevel(String propertyNameToOverwriteLogLevel) {
        this.propertyNameToOverwriteLogLevel = nonNull(propertyNameToOverwriteLogLevel);
        return this;
    }

    @Override
    public void process() {
        SScalar input = getInputScalar(defaultInputPortName(), true);
        Level level = parseOrNull(input.getValue());
        if (level == null) {
            String propertyName = propertyNameToOverwriteLogLevel.trim();
            if (!propertyName.isEmpty()) {
                try {
                    level = parseOrNull(System.getProperty(propertyName));
                } catch (SecurityException ignored) {
                    // assert level == null;
                    // - if we have no permission to read a system property,
                    // it is better not to control over logging level, but allow normal operation
                }
                if (level == null) {
                    try {
                        level = parseOrNull(System.getenv(propertyName));
                    } catch (Exception ignored) {
                        // assert level == null;
                        // - mostly probable IllegalArgumentException while parsing;
                        // ignore it to reduces a risk of unsecure usage and/or problem due to
                        // accidental match with the name of existing environment variable;
                        // also ignore SecurityException: if we have no permission to read  an environment
                        // variable, it is better not to control over logging level, but allow normal operation
                    }
                }
            }
        }
        if (level == null) {
            level = parseOrNull(this.logLevel);
        }
        getScalar().setTo(level.getName());
        getScalar(OUTPUT_LOGGABLE).setTo(LOG.isLoggable(level));
    }

    private static Level parseOrNull(String s) {
        if (s == null) {
            return null;
        }
        return ofLogLevel(s);
    }
}
