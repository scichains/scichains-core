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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Range;
import net.algart.math.RectangularArea;

public final class CreateRectangularArea extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SIZE_X = "size_x";
    public static final String OUTPUT_SIZE_Y = "size_y";
    public static final String OUTPUT_SIZE_Z = "size_z";

    public enum ResultLongOrDoubleType {
        LONG(long.class) {
            @Override
            SNumbers rectangularArea(CreateRectangularArea e) {
                long x1 = (long) e.minX;
                long x2 = e.maxX == null ? 0 : (long) e.maxX.doubleValue();
                if (e.sizeX != null) {
                    x2 = x1 + (long) e.sizeX.doubleValue() - 1;
                }
                final IRange rangeX = IRange.valueOf(x1, x2);
                e.getScalar(OUTPUT_SIZE_X).setTo(rangeX.size());
                if (e.minY == null) {
                    return SNumbers.valueOf(IRectangularArea.valueOf(rangeX));
                }
                long y1 = (long) e.minY.doubleValue();
                long y2 = e.maxY == null ? 0 : (long) e.maxY.doubleValue();
                if (e.sizeY != null) {
                    y2 = y1 + (long) e.sizeY.doubleValue() - 1;
                }
                final IRange rangeY = IRange.valueOf(y1, y2);
                e.getScalar(OUTPUT_SIZE_Y).setTo(rangeY.size());
                if (e.minZ == null) {
                    return SNumbers.valueOf(IRectangularArea.valueOf(rangeX, rangeY));
                }
                long z1 = (long) e.minZ.doubleValue();
                long z2 = e.maxZ == null ? 0 : (long) e.maxZ.doubleValue();
                if (e.sizeZ != null) {
                    z2 = z1 + (long) e.sizeZ.doubleValue() - 1;
                }
                final IRange rangeZ = IRange.valueOf(z1, z2);
                e.getScalar(OUTPUT_SIZE_Z).setTo(rangeZ.size());
                return SNumbers.valueOf(IRectangularArea.valueOf(rangeX, rangeY, rangeZ));
            }
        },
        DOUBLE(double.class) {
            @Override
            SNumbers rectangularArea(CreateRectangularArea e) {
                double x1 = e.minX;
                double x2 = e.maxX == null ? 0.0 : e.maxX;
                if (e.sizeX != null) {
                    x2 = x1 + e.sizeX;
                }
                final Range rangeX = Range.valueOf(x1, x2);
                e.getScalar(OUTPUT_SIZE_X).setTo(rangeX.size());
                if (e.minY == null) {
                    return SNumbers.valueOf(RectangularArea.valueOf(rangeX));
                }
                double y1 = e.minY;
                double y2 = e.maxY == null ? 0.0 : e.maxY;
                if (e.sizeY != null) {
                    y2 = y1 + e.sizeY;
                }
                final Range rangeY = Range.valueOf(y1, y2);
                e.getScalar(OUTPUT_SIZE_Y).setTo(rangeY.size());
                if (e.minZ == null) {
                    return SNumbers.valueOf(RectangularArea.valueOf(rangeX, rangeY));
                }
                double z1 = e.minZ;
                double z2 = e.maxZ == null ? 0.0 : e.maxZ;
                if (e.sizeZ != null) {
                    z2 = z1 + e.sizeZ;
                }
                final Range rangeZ = Range.valueOf(z1, z2);
                e.getScalar(OUTPUT_SIZE_Z).setTo(rangeZ.size());
                return SNumbers.valueOf(RectangularArea.valueOf(rangeX, rangeY, rangeZ));
            }
        };

        final Class<?> elementType;

        ResultLongOrDoubleType(Class<?> elementType) {
            this.elementType = elementType;
        }

        public Class<?> elementType() {
            return elementType;
        }

        abstract SNumbers rectangularArea(CreateRectangularArea e);
    }

    private ResultLongOrDoubleType resultElementType;
    private double minX = 0.0;
    private Double maxX = 0.0;
    private Double sizeX = null;
    private Double minY = 0.0;
    private Double maxY = 0.0;
    private Double sizeY = null;
    private Double minZ = null;
    private Double maxZ = null;
    private Double sizeZ = null;

    public CreateRectangularArea() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public ResultLongOrDoubleType getResultElementType() {
        return resultElementType;
    }

    public CreateRectangularArea setResultElementType(ResultLongOrDoubleType resultElementType) {
        this.resultElementType = resultElementType;
        return this;
    }

    public double getMinX() {
        return minX;
    }

    public CreateRectangularArea setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    public Double getMaxX() {
        return maxX;
    }

    public CreateRectangularArea setMaxX(Double maxX) {
        this.maxX = maxX;
        return this;
    }

    public Double getSizeX() {
        return sizeX;
    }

    public CreateRectangularArea setSizeX(Double sizeX) {
        this.sizeX = sizeX;
        return this;
    }

    public Double getMinY() {
        return minY;
    }

    public CreateRectangularArea setMinY(Double minY) {
        this.minY = minY;
        return this;
    }

    public Double getMaxY() {
        return maxY;
    }

    public CreateRectangularArea setMaxY(Double maxY) {
        this.maxY = maxY;
        return this;
    }

    public Double getSizeY() {
        return sizeY;
    }

    public CreateRectangularArea setSizeY(Double sizeY) {
        this.sizeY = sizeY;
        return this;
    }

    public Double getMinZ() {
        return minZ;
    }

    public CreateRectangularArea setMinZ(Double minZ) {
        this.minZ = minZ;
        return this;
    }

    public Double getMaxZ() {
        return maxZ;
    }

    public CreateRectangularArea setMaxZ(Double maxZ) {
        this.maxZ = maxZ;
        return this;
    }

    public Double getSizeZ() {
        return sizeZ;
    }

    public CreateRectangularArea setSizeZ(Double sizeZ) {
        this.sizeZ = sizeZ;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            getNumbers().setTo(resultElementType.rectangularArea(this));
        }
    }
}
