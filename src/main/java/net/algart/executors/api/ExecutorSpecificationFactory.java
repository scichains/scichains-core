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

package net.algart.executors.api;

import net.algart.executors.api.model.ExecutorSpecification;

@FunctionalInterface
public interface ExecutorSpecificationFactory {
    /**
     * Returns the specification of the given executor.
     *
     * <p>Note that the main goal of this function
     * is only to return minimal description, enough for building new executor by {@link #newExecutor(String)} method.
     * However, this function usually returns full specification.
     *
     * <p>The main source of information about all JSON models is another:
     * {@link ExecutionBlock#availableExecutorSpecifications(String)}.
     *
     * @param executorId unique executor ID.
     * @return minimal JSON model, enough for creating Java class {@link ExecutionBlock}.
     */
    ExecutorSpecification specification(String executorId);
}