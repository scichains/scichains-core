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

public enum ExecutionStage {
    LOADING_TIME("loading_time"),
    RUN_TIME("run_time");

    private final String stageName;

    ExecutionStage(String stageName) {
        this.stageName = stageName;
    }

    public String stageName() {
        return stageName;
    }

    public static ExecutionStage of(String name) {
        final ExecutionStage result = ofOrNull(name);
        if (result == null) {
            throw new IllegalArgumentException("Unknown stage name: " + name);
        }
        return result;
    }

    public static ExecutionStage ofOrNull(String name) {
        for (ExecutionStage stage : values()) {
            if (stage.stageName.equals(name)) {
                return stage;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        for (ExecutionStage stage : values()) {
            System.out.printf("%s: %s, %s%n", stage, stage.stageName(), of(stage.stageName()));
        }
    }

}
