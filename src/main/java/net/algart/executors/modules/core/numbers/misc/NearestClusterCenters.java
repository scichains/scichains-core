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

package net.algart.executors.modules.core.numbers.misc;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.Arrays;
import java.util.List;

public final class NearestClusterCenters extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_VALUES = "values";
    public static final String INPUT_CENTERS = "centers";
    public static final String OUTPUT_INDEXES = "indexes";
    public static final String OUTPUT_DISTANCES = "distances";

    private ValuesDistanceMetric distanceMetric = ValuesDistanceMetric.NORMALIZED_EUCLIDEAN;
    private double[] valuesWeights = {};
    private double maxDistance = Double.POSITIVE_INFINITY;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public NearestClusterCenters() {
        super(INPUT_VALUES, INPUT_CENTERS);
        setDefaultOutputNumbers(OUTPUT_INDEXES);
        addOutputNumbers(OUTPUT_DISTANCES);
    }

    public ValuesDistanceMetric getDistanceMetric() {
        return distanceMetric;
    }

    public NearestClusterCenters setDistanceMetric(ValuesDistanceMetric distanceMetric) {
        this.distanceMetric = nonNull(distanceMetric);
        if (!distanceMetric.isSingleNumber()) {
            throw new IllegalArgumentException("Illegal " + distanceMetric
                    + ": distance metric must be single-number");
        }
        return this;
    }

    public double[] getValuesWeights() {
        return valuesWeights.clone();
    }

    public NearestClusterCenters setValuesWeights(double[] valuesWeights) {
        this.valuesWeights = nonNull(valuesWeights).clone();
        return this;
    }

    public NearestClusterCenters setValuesWeights(String valueWeights) {
        this.valuesWeights = new SScalar(nonNull(valueWeights)).toDoubles();
        return this;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public NearestClusterCenters setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    public NearestClusterCenters setMaxDistance(String maxDistance) {
        this.maxDistance = doubleOrPositiveInfinity(maxDistance);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public NearestClusterCenters setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        final SNumbers values = sources.get(0);
        final SNumbers centers = sources.get(1);
        if (values.getBlockLength() != centers.getBlockLength()) {
            // - to be on the safe side (should be checked by superclass)
            throw new IllegalArgumentException("Different blockLength");
        }
        final float[] valuesArray = values.toFloatArray();
        final float[] centersArray = centers.toFloatArray();
        final float[] distanceArray = new float[values.n()];
        Arrays.fill(distanceArray, Float.NaN);
        final int[] result = process(valuesArray, centersArray, values.getBlockLength(), distanceArray);
        getNumbers(OUTPUT_DISTANCES).setTo(distanceArray, 1);
        return SNumbers.valueOfArray(result);
    }

    public int[] process(float[] values, float[] centers, final int blockLength, float[] minDistances) {
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Zero or negative blockLength");
        }
        if (values.length % blockLength != 0) {
            throw new IllegalArgumentException("values length % blockLength != 0");
        }
        if (centers.length % blockLength != 0) {
            throw new IllegalArgumentException("centers length % blockLength != 0");
        }
        double[] value = new double[blockLength];
        double[] center = new double[blockLength];
        final double[] weights = new double[blockLength];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = i >= this.valuesWeights.length ? 1.0 : this.valuesWeights[i];
        }
        int[] result = new int[values.length / blockLength];
        for (int k = 0, valueDisp = 0; k < result.length; k++) {
            for (int j = 0; j < blockLength; j++) {
                value[j] = values[valueDisp++];
            }
            double minDistance = Double.POSITIVE_INFINITY;
            int betsIndex = -1;
            for (int index = 0, centerDisp = 0; centerDisp < centers.length; index++) {
                for (int j = 0; j < blockLength; j++) {
                    center[j] = centers[centerDisp++];
                }
                final double distance = distanceMetric.distance(value, center, weights);
                if (distance > maxDistance) {
                    continue;
                }
                if (distance <= minDistance) {
                    minDistance = distance;
                    betsIndex = index + indexingBase.start;
                }
            }
            if (minDistances != null) {
                minDistances[k] = (float) minDistance;
            }
            result[k] = betsIndex;
        }
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return false;
    }

    @Override
    protected boolean numberOfBlocksEqualityRequired() {
        return false;
    }
}
