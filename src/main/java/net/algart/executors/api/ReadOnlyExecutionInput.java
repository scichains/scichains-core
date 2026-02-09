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

package net.algart.executors.api;

import net.algart.executors.api.data.Data;

/**
 * Optional interface, that can be implemented by {@link ExecutionBlock} class: see {@link #isReadOnly()} method.
 * <b>Warning: if this interface is implemented by a mistake, and really the executor modifies the input data,
 * it can lead to incorrect work and even to JVM crash, if the executor works with native memory like OpenCV Mat.</b>
 */
public interface ReadOnlyExecutionInput {
    /**
     * If <code>true</code>, this executor promises, that it will not try to modify the data, loaded from input ports,
     * in any way: correcting content of arrays or matrices, saving {@link Data} back to input ports, etc.
     * Also, this executor promises that it will not store the reference to the data from input ports
     * to output ports: all results, stored in the output ports, must be newly created data.
     *
     * <p>By default, this method returns <code>true</code>. So, you should implement this interface,
     * only if you are sure that this condition is fulfilled. This can improve performance of using
     * this executor (the client will not need to clone data).
     *
     * <p>If you implement this interface, but override this method, and your version of this method
     * returns <code>false</code>, it means that you don't provide any guarantees.
     *
     * @return is there a guarantee that this executor will only read the data from input ports
     * and will not try to modify their content.
     */
    default boolean isReadOnly() {
        return true;
    }
}
