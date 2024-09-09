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

package net.algart.executors.modules.core.logic;

import net.algart.executors.api.data.SScalar;

public enum ConditionStyle {
    C_LIKE() {
        @Override
        public boolean toBoolean(String scalar, boolean defaultCondition) {
            return SScalar.toCLikeBoolean(scalar, defaultCondition);
        }

        @Override
        public void setScalar(SScalar result, boolean value) {
            result.setTo(value ? 1 : 0);
        }

    },
    JAVA_LIKE() {
        @Override
        public boolean toBoolean(String scalar, boolean defaultCondition) {
            return scalar == null ? defaultCondition : Boolean.parseBoolean(scalar);
        }

        @Override
        public void setScalar(SScalar result, boolean value) {
            result.setTo(value);
        }
    };

    public abstract boolean toBoolean(String scalar, boolean defaultCondition);

    public abstract void setScalar(SScalar result, boolean value);
}
