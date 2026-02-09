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

package net.algart.executors.api.system;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public enum ExecutionStage {
    LOADING_TIME("loading_time"),
    RUN_TIME("run_time");

    private final String stageName;

    ExecutionStage(String stageName) {
        this.stageName = Objects.requireNonNull(stageName);
    }

    public static Collection<String> stageNames() {
        return Arrays.stream(values()).map(ExecutionStage::stageName).toList();
    }

    public String stageName() {
        return stageName;
    }

    public static ExecutionStage ofStageName(String stageName) {
        Objects.requireNonNull(stageName, "Null stage name");
        return fromStageName(stageName).orElseThrow(
                () -> new IllegalArgumentException("Unknown stage name: \"" + stageName + "\""));
    }

    /**
     * Returns an {@link Optional} containing the {@link ExecutionStage} with the given {@link #stageName()}.
     * <p>If no execution stage with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param stageName the stage name; may be {@code null}.
     * @return an optional execution stage.
     */
    public static Optional<ExecutionStage> fromStageName(String stageName) {
        for (ExecutionStage stage : values()) {
            if (stage.stageName.equals(stageName)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        for (ExecutionStage stage : values()) {
            System.out.printf("%s: %s, %s%n", stage, stage.stageName(), ofStageName(stage.stageName()));
        }
    }
}
