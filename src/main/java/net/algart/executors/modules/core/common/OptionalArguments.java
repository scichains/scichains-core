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

package net.algart.executors.modules.core.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class OptionalArguments<T> {
    private final List<T> nullableArguments;

    public OptionalArguments(List<T> nullableArguments) {
        Objects.requireNonNull(nullableArguments, "Null arguments list");
        this.nullableArguments = new ArrayList<>(nullableArguments);
    }

    public List<T> extract() {
        return nullableArguments.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public <P> List<P> extractParallel(Map<Integer, P> parallelArguments) {
        final List<P> result = new ArrayList<>();
        for (int k = 0, n = nullableArguments.size(), m = parallelArguments.size(); k < n; k++) {
            if (nullableArguments.get(k) != null) {
                result.add(k < m ? parallelArguments.get(k) : null);
            }
        }
        return result;
    }

    public int[] extractParallelIntegers(Map<Integer, Integer> parallelArguments, int defaultValue) {
        return extractParallel(parallelArguments).stream().mapToInt(v -> v == null ? defaultValue : v).toArray();
    }

    public double[] extractParallelDoubles(Map<Integer, Double> parallelArguments, double defaultValue) {
        return extractParallel(parallelArguments).stream().mapToDouble(v -> v == null ? defaultValue : v).toArray();
    }
}
