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

package net.algart.executors.modules.core.common.matrices;

import net.algart.executors.api.Executor;
import net.algart.multimatrix.MultiMatrix;

import java.util.Objects;

public abstract class MultiMatrixGenerator extends Executor {
    public static final String SAME_ELEMENT_TYPE = "unchanged";

    private int numberOfChannels = 1;
    private long dimX = 64;
    private long dimY = 64;
    private long dimZ = 0;
    private Class<?> elementType = byte.class;

    protected MultiMatrixGenerator() {
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public final int getNumberOfChannels() {
        return numberOfChannels;
    }

    public final void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = positive(numberOfChannels);
    }

    public final long getDimX() {
        return dimX;
    }

    public final void setDimX(long dimX) {
        this.dimX = nonNegative(dimX);
    }

    public final long getDimY() {
        return dimY;
    }

    public final void setDimY(long dimY) {
        this.dimY = nonNegative(dimY);
    }

    public long getDimZ() {
        return dimZ;
    }

    public void setDimZ(long dimZ) {
        this.dimZ = nonNegative(dimZ);
    }

    public final Class<?> getElementType() {
        return elementType;
    }

    public final void setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
    }

    public final void setElementType(String elementType) {
        setElementType(elementType(elementType));
    }

    @Override
    public void process() {
        setStartProcessingTimeStamp();
        final MultiMatrix result = create();
        setEndProcessingTimeStamp();
        getMat().setTo(result);
    }

    public abstract MultiMatrix create();

    public static Class<?> elementType(String primitiveElementTypeName) {
        return elementType(primitiveElementTypeName, false);
    }

    public static Class<?> elementType(String primitiveElementTypeName, boolean nullForUnchanged) {
        Objects.requireNonNull(primitiveElementTypeName, "Null element type name");
        if (nullForUnchanged && primitiveElementTypeName.equals(SAME_ELEMENT_TYPE)) {
            return null;
        }
        return switch (primitiveElementTypeName) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            default -> throw new IllegalArgumentException("Illegal name of element type");
        };
    }

}
