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

package net.algart.executors.modules.core.system;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;

import java.io.File;

public class CheckClassExistence extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_IS_EXISTING_CLASS = "is_existing_class";
    public static final String OUTPUT_CLASS_PATH = "class_path";

    private String className = "java.lang.System";

    public CheckClassExistence() {
        addOutputScalar(OUTPUT_IS_EXISTING_CLASS);
        addOutputScalar(OUTPUT_CLASS_PATH);
    }

    public String getClassName() {
        return className;
    }

    public CheckClassExistence setClassName(String className) {
        this.className = nonEmpty(className);
        return this;
    }

    @Override
    public void process() {
        Class<?> clazz = null;
        final StringBuilder sb = new StringBuilder();
        try {
            clazz = Class.forName(className);
            sb.append("O'k, found\n").append(clazz).append("\n\n");
        } catch (Throwable e) {
            sb.append("Not found \"").append(className).append("\"\nException:\n").append(e).append("\n\n");
        }
        final String classPath = System.getProperty("java.class.path");
        sb.append("Java class path:\n").append(
                classPath.replace(File.pathSeparator, File.pathSeparator + "\n"));
        getScalar(OUTPUT_IS_EXISTING_CLASS).setTo(clazz != null);
        getScalar(OUTPUT_CLASS_PATH).setTo(classPath);
        getScalar().setTo(sb);
    }
}
