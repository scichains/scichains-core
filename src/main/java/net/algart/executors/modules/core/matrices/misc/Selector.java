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

package net.algart.executors.modules.core.matrices.misc;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesChannelOperation;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Selector extends SeveralMultiMatricesChannelOperation {
    public static final String SELECTOR = "selector";
    public static final String INPUT_MATRIX_PORT_PREFIX = "m";

    public enum SelectorType {
        BINARY_MATRIX,
        INT_MATRIX,
        INT_VALUE
    }

    private SelectorType selectorType = SelectorType.BINARY_MATRIX;
    private int selectorValue = 0;
    private Map<Integer, String> filler = new HashMap<>();

    private Matrix<? extends PArray> selector = null;

    public Selector() {
        super();
    }

    public SelectorType getSelectorType() {
        return selectorType;
    }

    public Selector setSelectorType(SelectorType selectorType) {
        this.selectorType = nonNull(selectorType);
        return this;
    }

    public int getSelectorValue() {
        return selectorValue;
    }

    public Selector setSelectorValue(int selectorValue) {
        this.selectorValue = nonNegative(selectorValue);
        return this;
    }

    public String getFiller(int index) {
        return filler.getOrDefault(index, "#000000");
    }

    public Selector setFiller(int index, String color) {
        filler.put(index, color);
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.startsWith("filler_")) {
            final int index;
            try {
                index = Integer.parseInt(name.substring("filler_".length()));
            } catch (NumberFormatException ignored) {
                return;
            }
            //noinspection resource
            setFiller(index, parameters().getString(name));
            return;
        }
        super.onChangeParameter(name);
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        if (!sources.isEmpty()) {
            // - to be on the safe side
            if (selectorType == SelectorType.INT_VALUE) {
                if (selectorValue > sources.size()) {
                    throw new IllegalArgumentException("Selector value " + selectorValue
                            + " > number of source matrices");
                }
                final MultiMatrix result = sources.get(selectorValue + 1);
                if (result != null) {
                    return result;
                } // else we need to build it from filler
            } else {
                if (sources.getFirst() == null) {
                    throw new IllegalArgumentException("Selector matrix must be initialized in mode " + selectorType);
                }
            }
            switch (selectorType) {
                case BINARY_MATRIX: {
                    this.selector = sources.getFirst().nonZeroRGBMatrix();
                    break;
                }
                case INT_MATRIX: {
                    this.selector = sources.getFirst().intensityChannel();
                    break;
                }
                case INT_VALUE: {
                    this.selector = null;
                    break;
                }
                default: {
                    throw new AssertionError("Unknown " + selectorType);
                }
            }
        }
        return super.process(sources);
    }

    @Override
    protected Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m) {
        final Class<? extends PArray> requiredType =
                indexOfSampleInputForEqualizing() == 0 ? ByteArray.class : sampleType();
        // - if we used the selector as sample (no other inputs), let's set the result type to byte:
        // it is a degenerated case of building two-color image from the selector
        final double maxPossibleValue = Arrays.maxPossibleValue(requiredType, 1.0);
        if (currentChannel() == 0) {
            logDebug(() -> "Combining " + (m.size() - 1) + " images to " + Arrays.elementType(requiredType) + "["
                    + numberOfChannels() + " channels, "
                    + JArrays.toString(sampleMultiMatrix().dimensions(), "x", 1000) + "]");
            if (LOGGABLE_DEBUG) {
                for (int k = 1, n = m.size(); k < n; k++) {
                    if (m.get(k) == null) {
                        logTrace("    No image #" + k + "/" + (n - 1) + ": filling by color "
                                + getFiller(k - 1));
                    } else {
                        logDebug("    Combined image #" + k + "/" + (n - 1) + ": " + m.get(k).elementType());
                    }
                }
            }
        }
        if (selector != null) {
            m.set(0, selector);
        } else {
            assert selectorType == SelectorType.INT_VALUE;
            m.set(0, Matrices.constantMatrix(selectorValue, IntArray.class, sampleMultiMatrix().dimensions()));
        }
        // - ignoring preprocessing of the first matrix
        for (int k = 1, n = m.size(); k < n; k++) {
            if (m.get(k) == null) {
                double filler = colorChannel(getFiller(k - 1), maxPossibleValue);
                m.set(k, Matrices.constantMatrix(filler, requiredType, sampleMultiMatrix().dimensions()));
            }
        }
        return Matrices.clone(Matrices.asFuncMatrix(Func.SELECT, requiredType, m));
    }

    @Override
    protected String inputPortName(int inputIndex) {
        return inputIndex == 0 ? SELECTOR : INPUT_MATRIX_PORT_PREFIX + (inputIndex - 1);
    }

    @Override
    protected boolean equalizePrecision() {
        return false;
    }

    @Override
    protected int findSampleInputForEqualizing(List<MultiMatrix> sources) {
        for (int k = 1; k < sources.size(); k++) {
            if (sources.get(k) != null) {
                return k;
            }
        }
        // If there are no other arguments, let's use the selector for equalization
        return 0;
    }
}
