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

package net.algart.executors.api.chains;

import net.algart.executors.api.data.Port;

import java.util.Optional;

public enum ChainPortType {
    INPUT_PORT(1, Port.Type.INPUT, true),
    OUTPUT_PORT(2, Port.Type.OUTPUT, true),
    INPUT_PARAMETER_AS_PORT(3, Port.Type.INPUT, false),
    OUTPUT_PARAMETER_AS_PORT(4, Port.Type.OUTPUT, false);

    private final int code;
    private final Port.Type actualPortType;
    private final boolean actual;

    ChainPortType(int code, Port.Type actualPortType, boolean actual) {
        this.code = code;
        this.actualPortType = actualPortType;
        this.actual = actual;
    }

    public int code() {
        return code;
    }

    /**
     * Returns an {@link Optional} containing the {@link ChainPortType} with the given {@link #code()}.
     * <p>If no chain port type with the specified code exists,
     * an empty optional is returned.
     *
     * @param code the integer code.
     * @return an optional chain port type.
     */
    public static Optional<ChainPortType> fromCode(int code) {
        for (ChainPortType portType : values()) {
            if (portType.code == code) {
                return Optional.of(portType);
            }
        }
        return Optional.empty();
    }

    public boolean isActual() {
        return actual;
    }

    public boolean isVirtual() {
        return !actual;
    }

    public Port.Type actualPortType() {
        return actualPortType;
    }
}
