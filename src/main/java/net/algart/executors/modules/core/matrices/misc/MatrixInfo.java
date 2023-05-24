/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.matrices.misc;

import net.algart.arrays.JArrays;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToSeveralScalars;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MatrixInfo extends MultiMatrixToSeveralScalars {
    private static final String NUMBER_OF_DIMENSIONS = "number_of_dimensions";
    private static final String DIM_X = "dim_x";
    private static final String DIM_Y = "dim_y";
    private static final String DIM_Z = "dim_z";
    private static final String ELEMENT_TYPE = "element_type";
    private static final String MAX_POSSIBLE = "max_possible";
    private static final String NUMBER_OF_CHANNELS = "number_of_channels";
    private static final String DESCRIPTION = "description";

    private static final String[] QUICK_RESULT_PORTS = new String[]{
            DIM_X, DIM_Y, ELEMENT_TYPE, MAX_POSSIBLE, NUMBER_OF_CHANNELS, DESCRIPTION
    };

    private boolean rawValues = false;

    public MatrixInfo() {
        useVisibleResultParameter();
        for (String port : QUICK_RESULT_PORTS) {
            addOutputScalar(port);
            // - necessary to provide ports for process() even if they are not specified in JSON configuration
        }
        for (SimpleImageStatistics statistics : SimpleImageStatistics.values()) {
            addOutputNumbers(statistics.statisticsName());
        }
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public MatrixInfo setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    @Override
    public void analyse(Map<String, SScalar> results, MultiMatrix source) {
        analyse(results, allOutputContainers(SNumbers.class, true), source);
    }

    public void analyse(Map<String, SScalar> scalars, Map<String, SNumbers> numbers, MultiMatrix source) {
        Objects.requireNonNull(scalars, "Null scalars");
        if (source == null) {
            SScalar.setTo(scalars, DESCRIPTION, () -> "No input matrix");
        } else {
            SScalar.setTo(scalars, NUMBER_OF_DIMENSIONS, source::dimCount);
            SScalar.setTo(scalars, DIM_X, () -> source.dim(0));
            SScalar.setTo(scalars, DIM_Y, () -> source.dim(1));
            SScalar.setTo(scalars, DIM_Z, () -> source.dim(2));
            SScalar.setTo(scalars, ELEMENT_TYPE, source::elementType);
            SScalar.setTo(scalars, MAX_POSSIBLE, source::maxPossibleValue);
            SScalar.setTo(scalars, NUMBER_OF_CHANNELS, source::numberOfChannels);
            SScalar.setTo(scalars, DESCRIPTION, () -> String.format("%s[%d channels, %s] (%d-bit%s)",
                    source.elementType(),
                    source.numberOfChannels(),
                    JArrays.toString(source.dimensions(), "x", 1000),
                    source.bitsPerElement(),
                    source.isUnsigned() || source.isFloatingPoint() ? "" : ", signed"));
        }
        for (SimpleImageStatistics statistics : SimpleImageStatistics.values()) {
            final SNumbers result = numbers.get(statistics.statisticsName());
            if (result != null) {
                long t1 = debugTime();
                final Object statisticsResult = statistics.allChannelsStatistics(source, rawValues);
                long t2 = debugTime();
                logDebug(() -> String.format(Locale.US, "Calculating %s for %s: %.3f ms",
                        statistics, source, (t2 - t1) * 1e-6));
                result.setToArray(statisticsResult, statistics.channelBlockLength(source));
            }
        }
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
