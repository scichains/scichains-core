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

package net.algart.executors.modules.core.common.matrices;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.ChannelOperation;

import java.util.ArrayList;
import java.util.List;

public abstract class SeveralMultiMatricesChannelOperation
    extends SeveralMultiMatricesOperation
    implements ChannelOperation
{
    private int minimalRequiredNumberOfChannels = 0;

    private MultiMatrix sampleMultiMatrix = null;
    private Class<? extends PArray> sampleType = null;
    private int indexOfSampleInputForEqualizing = 0;
    private int currentChannel = 0;
    private int numberOfChannels = 0;

    protected SeveralMultiMatricesChannelOperation(String... predefinedInputPortNames) {
        super(predefinedInputPortNames);
    }

    public int getMinimalRequiredNumberOfChannels() {
        return minimalRequiredNumberOfChannels;
    }

    public SeveralMultiMatricesChannelOperation setMinimalRequiredNumberOfChannels(
            int minimalRequiredNumberOfChannels) {
        this.minimalRequiredNumberOfChannels = nonNegative(minimalRequiredNumberOfChannels);
        return this;
    }

    public final int currentChannel() {
        return currentChannel;
    }

    public final int numberOfChannels() {
        return numberOfChannels;
    }

    protected final Class<? extends PArray> sampleType() {
        return sampleType;
    }

    protected final MultiMatrix sampleMultiMatrix() {
        return sampleMultiMatrix;
    }

    protected final int indexOfSampleInputForEqualizing() {
        return indexOfSampleInputForEqualizing;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        this.indexOfSampleInputForEqualizing = findSampleInputForEqualizing(sources);
        if (indexOfSampleInputForEqualizing >= sources.size()) {
            throw new IllegalStateException("Invalid sample input mmatrix index " + indexOfSampleInputForEqualizing);
        }
        this.sampleMultiMatrix = sources.get(indexOfSampleInputForEqualizing);
        if (sampleMultiMatrix == null) {
            throw new IllegalArgumentException("The input matrix #" + indexOfSampleInputForEqualizing
                + " is not initialized, but it is required");
        }
        try {
            this.numberOfChannels = Math.max(sampleMultiMatrix.numberOfChannels(), minimalRequiredNumberOfChannels);
            this.sampleType = sampleMultiMatrix.arrayType();
            final Class<?> resultElementType = sampleMultiMatrix.elementType();
            final double resultMax = sampleMultiMatrix.maxPossibleValue();
            final List<List<Matrix<? extends PArray>>> sourceMultiMatrices = new ArrayList<>();
            for (final MultiMatrix source : sources) {
                final List<Matrix<? extends PArray>> channels;
                if (source == null) {
                    channels = null;
                } else {
                    channels = source.asOtherNumberOfChannels(numberOfChannels).allChannels();
                    assert channels.size() == numberOfChannels;
                }
                sourceMultiMatrices.add(channels);
            }
            final List<Matrix<? extends PArray>> result = new ArrayList<>();
            final boolean equalize = equalizePrecision();
            for (this.currentChannel = 0; currentChannel < numberOfChannels; currentChannel++) {
                final List<Matrix<? extends PArray>> sourceMatrices = new ArrayList<>();
                for (List<Matrix<? extends PArray>> channels : sourceMultiMatrices) {
                    if (channels == null) {
                        sourceMatrices.add(null);
                    } else {
                        Matrix<? extends PArray> sourceMatrix = channels.get(currentChannel);
                        final Class<?> elementType = sourceMatrix.elementType();
                        double max = sourceMatrix.array().maxPossibleValue(1.0);
                        if (equalize && elementType != resultElementType) {
                            // - important: we need to do this even when max==resultMax (for example, float/double)
                            sourceMatrix = Matrices.asFuncMatrix(
                                LinearFunc.getInstance(0.0, resultMax / max), sampleType, sourceMatrix);
                        }
                        sourceMatrices.add(sourceMatrix);
                    }
                }
                final Matrix<? extends PArray> resultChannel = processChannel(sourceMatrices);
                result.add(resultChannel);
            }
            return MultiMatrix.valueOf(result);
        } finally {
            this.sampleMultiMatrix = null;
            // - allow garbage collector to free this memory
        }
    }

    // This method is not public: usually it's direct call, not from process(), can lead to
    // incorrect results, because currentChannel and numberOfChannels are not set properly
    protected abstract Matrix<? extends PArray> processChannel(List<Matrix<? extends PArray>> m);

    // May be overridden!
    protected boolean equalizePrecision() {
        return true;
    }

    // Unlike SeveralMultiMatricesOperation, this class requires at least 1 existing input:
    // this method returns its index.
    protected int findSampleInputForEqualizing(List<MultiMatrix> sources) {
        return 0;
    }
}