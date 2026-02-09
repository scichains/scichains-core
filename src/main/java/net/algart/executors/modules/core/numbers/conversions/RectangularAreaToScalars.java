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

package net.algart.executors.modules.core.numbers.conversions;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.math.RectangularArea;

public final class RectangularAreaToScalars extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_MIN_X = "min_x";
    public static final String OUTPUT_MAX_X = "max_x";
    public static final String OUTPUT_MIN_Y = "min_y";
    public static final String OUTPUT_MAX_Y = "max_y";
    public static final String OUTPUT_MIN_Z = "min_z";
    public static final String OUTPUT_MAX_Z = "max_z";

    public RectangularAreaToScalars() {
        setDefaultInputNumbers(DEFAULT_INPUT_PORT);
        addOutputScalar(OUTPUT_MIN_X);
        addOutputScalar(OUTPUT_MAX_X);
        addOutputScalar(OUTPUT_MIN_Y);
        addOutputScalar(OUTPUT_MAX_Y);
        addOutputScalar(OUTPUT_MIN_Z);
        addOutputScalar(OUTPUT_MAX_Z);
    }

    @Override
    public void process() {
        RectangularArea area = getInputNumbers(defaultInputPortName()).toRectangularArea();
        getScalar(OUTPUT_MIN_X).setTo(area.minX());
        getScalar(OUTPUT_MAX_X).setTo(area.maxX());
        if (area.coordCount() >= 2) {
            getScalar(OUTPUT_MIN_Y).setTo(area.minY());
            getScalar(OUTPUT_MAX_Y).setTo(area.maxY());
        }
        if (area.coordCount() >= 3) {
            getScalar(OUTPUT_MIN_Z).setTo(area.minZ());
            getScalar(OUTPUT_MAX_Z).setTo(area.maxZ());
        }
    }
}
