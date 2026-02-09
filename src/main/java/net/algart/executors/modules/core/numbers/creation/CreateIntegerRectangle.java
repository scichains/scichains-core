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

package net.algart.executors.modules.core.numbers.creation;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;

public final class CreateIntegerRectangle extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SIZE_X = "size_x";
    public static final String OUTPUT_SIZE_Y = "size_y";

    private long minX = -Long.MAX_VALUE / 2;
    private long maxX = Long.MAX_VALUE / 2;
    private Long sizeX = null;
    private long minY = -Long.MAX_VALUE / 2;
    private long maxY = Long.MAX_VALUE / 2;
    private Long sizeY = null;

    public CreateIntegerRectangle() {
        addInputNumbers(DEFAULT_INPUT_PORT);
        addOutputNumbers(DEFAULT_OUTPUT_PORT);
    }

    public long getMinX() {
        return minX;
    }

    public CreateIntegerRectangle setMinX(long minX) {
        this.minX = minX;
        return this;
    }

    public CreateIntegerRectangle setMinX(String minX) {
        return setMinX(longOrDefault(minX, -Long.MAX_VALUE / 2));
    }

    public long getMaxX() {
        return maxX;
    }

    public CreateIntegerRectangle setMaxX(long maxX) {
        this.maxX = maxX;
        return this;
    }

    public CreateIntegerRectangle setMaxX(String maxX) {
        return setMaxX(longOrDefault(maxX, Long.MAX_VALUE / 2));
    }

    public Long getSizeX() {
        return sizeX;
    }

    public CreateIntegerRectangle setSizeX(Long sizeX) {
        this.sizeX = sizeX;
        return this;
    }

    public long getMinY() {
        return minY;
    }

    public CreateIntegerRectangle setMinY(long minY) {
        this.minY = minY;
        return this;
    }

    public CreateIntegerRectangle setMinY(String minY) {
        return setMinY(longOrDefault(minY, -Long.MAX_VALUE / 2));
    }

    public long getMaxY() {
        return maxY;
    }

    public CreateIntegerRectangle setMaxY(long maxY) {
        this.maxY = maxY;
        return this;
    }

    public CreateIntegerRectangle setMaxY(String maxY) {
        return setMaxY(longOrDefault(maxY, Long.MAX_VALUE / 2));
    }

    public Long getSizeY() {
        return sizeY;
    }

    public CreateIntegerRectangle setSizeY(Long sizeY) {
        this.sizeY = sizeY;
        return this;
    }

    @Override
    public void process() {
        SNumbers input = getInputNumbers(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying number array: " + input);
            getNumbers().setTo(input);
        } else {
            getNumbers().exchange(rectangularArea());
        }
    }

    SNumbers rectangularArea() {
        long x1 = this.minX;
        long x2 = this.maxX;
        if (this.sizeX != null) {
            x2 = x1 + this.sizeX - 1;
        }
        final IRange rangeX = IRange.of(x1, x2);
        this.getScalar(OUTPUT_SIZE_X).setTo(rangeX.size());
        long y1 = this.minY;
        long y2 = this.maxY;
        if (this.sizeY != null) {
            y2 = y1 + this.sizeY - 1;
        }
        final IRange rangeY = IRange.of(y1, y2);
        this.getScalar(OUTPUT_SIZE_Y).setTo(rangeY.size());
        return SNumbers.of(IRectangularArea.of(rangeX, rangeY));
    }
}
