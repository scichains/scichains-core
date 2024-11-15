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

package net.algart.executors.modules.core.scalars.conversions;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.scalars.copying.CopyScalar;

public class NumericRangeToConstant extends CopyScalar {
    private Double min = 0.0;
    private Double max = 0.0;
    private String replacement = "";

    public Double getMin() {
        return min;
    }

    public NumericRangeToConstant setMin(Double min) {
        this.min = min;
        return this;
    }

    public Double getMax() {
        return max;
    }

    public NumericRangeToConstant setMax(Double max) {
        this.max = max;
        return this;
    }

    public String getReplacement() {
        return replacement;
    }

    public NumericRangeToConstant setReplacement(String replacement) {
        this.replacement = nonNull(replacement);
        return this;
    }

    @Override
    public void process() {
        final SScalar input = getInputScalar(true);
        if (needToReplace(input.getValue())) {
            logDebug(() -> "Replacing scalar " + input + " with \"" + replacement + "\"");
            getScalar().setTo(replacement);
        } else {
            super.process();
        }
    }

    private boolean needToReplace(String inputValue) {
        if (inputValue == null) {
            return false;
        }
        final double value;
        try {
            value = Double.parseDouble(inputValue);
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (min != null && value < min) {
            return false;
        }
        if (max != null && value > max) {
            return false;
        }
        return min != null || max != null;
    }
}
