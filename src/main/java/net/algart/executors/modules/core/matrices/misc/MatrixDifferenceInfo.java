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

package net.algart.executors.modules.core.matrices.misc;

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesToSeveralScalars;
import net.algart.executors.modules.core.matrices.arithmetic.MatrixDifference;
import net.algart.multimatrix.MultiMatrix;

import java.util.List;
import java.util.Map;

public final class MatrixDifferenceInfo extends SeveralMultiMatricesToSeveralScalars {
    private MatrixDifference.Operation differenceOperation = MatrixDifference.Operation.ABSOLUTE_DIFFERENCE;
    private boolean sameDimensionsRequired = true;
    private boolean rawValues = false;

    public MatrixDifferenceInfo() {
        useVisibleResultParameter();
        for (SimpleImageStatistics statistics : SimpleImageStatistics.values()) {
            addOutputNumbers(statistics.statisticsName());
        }
    }

    public MatrixDifference.Operation getDifferenceOperation() {
        return differenceOperation;
    }

    public void setDifferenceOperation(MatrixDifference.Operation differenceOperation) {
        this.differenceOperation = nonNull(differenceOperation);
    }

    public boolean isSameDimensionsRequired() {
        return sameDimensionsRequired;
    }

    public MatrixDifferenceInfo setSameDimensionsRequired(boolean sameDimensionsRequired) {
        this.sameDimensionsRequired = sameDimensionsRequired;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public void setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
    }

    @Override
    public void analyse(Map<String, SScalar> results, List<MultiMatrix> sources) {
        final MultiMatrix differenceMatrix;
        if (sources.get(0) == null || sources.get(1) == null) {
            differenceMatrix = null;
        } else if (!sameDimensionsRequired && !sources.get(0).dimEquals(sources.get(1))) {
            differenceMatrix = null;
        } else {
            try (MatrixDifference matrixDifference = new MatrixDifference()) {
                differenceMatrix = matrixDifference.setOperation(differenceOperation).process(sources);
            }
        }
        try (MatrixInfo matrixInfo = new MatrixInfo()) {
            matrixInfo.setRawValues(rawValues);
            matrixInfo.analyse(results, allOutputContainers(SNumbers.class, true), differenceMatrix);
        }
    }

    @Override
    public String translateLegacyParameterAlias(String name) {
        return name.equals("requireSameDimensions") ? "sameDimensionsRequired" : name;
    }

    @Override
    protected Integer requiredNumberOfInputs() {
        return 2;
    }

    @Override
    protected boolean dimensionsEqualityRequired() {
        return sameDimensionsRequired;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }
}
