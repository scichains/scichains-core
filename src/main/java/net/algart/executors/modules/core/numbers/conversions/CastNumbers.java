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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.arrays.PArray;
import net.algart.arrays.PNumberArray;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.arrays.UpdatablePNumberArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumberArrayFilter;

public final class CastNumbers extends NumberArrayFilter implements ReadOnlyExecutionInput {
    private Class<?> elementType = float.class;

    public Class<?> getElementType() {
        return elementType;
    }

    public CastNumbers setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public CastNumbers setElementType(String elementType) {
        return setElementType(SNumbers.elementType(elementType));
    }

    @Override
    public SNumbers process(SNumbers input) {
        return input.toPrecision(elementType);
        // - better than default call of process(PNumberArray array)
    }

    @Override
    public PArray process(UpdatablePNumberArray array, int blockLength, int numberOfBlocks) {
        SNumbers numbers = SNumbers.valueOfArray(array.toJavaArray(), 1);
        numbers = numbers.toPrecision(elementType);
        return (PNumberArray) SimpleMemoryModel.asUpdatableArray(numbers.getArray());
    }
}
