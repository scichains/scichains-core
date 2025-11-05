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

package net.algart.executors.modules.core.common.io;

import net.algart.executors.api.data.SNumbers;


// Executor that writes something to disk may have a "side effect" executor: it has no obvious result.
// So, it is convenient to add copying some data to this operation: it will be its "result".
// But usually this technique is not a good idea; if you do not want it, just don't call copyAdditionalData.
// Note: we do not provide an analogous ReadFileOperation, instead, we add such methods as setFileExistenceRequired()
// inside FileOperation: this allows creating abstract I/O superclasses for both read and write operation
// and inherit them from FileOperation.
public abstract class WriteFileOperation extends FileOperation {
    public static final String S1 = "s";
    public static final String X1 = "x";
    public static final String M1 = "m";
    public static final String S2 = "s2";
    public static final String X2 = "x2";
    public static final String M2 = "m2";
    public static final String S3 = "s3";
    public static final String X3 = "x3";
    public static final String M3 = "m3";

    private static final int ARRAY_SIZE_COPIED_ALWAYS = 16384;
    // - Copying little arrays ALWAYS is not necessary while correct usage of executors,
    // but can be useful for debugging (output ports will be filled even when they are not necessary).

    protected WriteFileOperation() {
        super(false);
    }

    public void copyAdditionalData() {
        // Note: we prefer to use setTo instead of the exchange method: it allows subclasses to implement
        // ReadOnlyExecutionInput interface. Usually these data are empty and copied very quickly,
        // but even if it is not, writing data is usually a slower operation.
        getScalar(S1).setTo(getInputScalar(S1, true));
        getScalar(S2).setTo(getInputScalar(S2, true));
        getScalar(S3).setTo(getInputScalar(S3, true));
        // - The operations above are VERY quick, no sense to check isOutputNecessary
        final SNumbers x1 = getInputNumbers(X1, true);
        if (x1.isInitialized() && (isOutputNecessary(X1) || x1.getArrayLength() < ARRAY_SIZE_COPIED_ALWAYS)) {
            getNumbers(X1).setTo(x1);
        }
        final SNumbers x2 = getInputNumbers(X2, true);
        if (x2.isInitialized() && (isOutputNecessary(X2) || x2.getArrayLength() < ARRAY_SIZE_COPIED_ALWAYS)) {
            getNumbers(X2).setTo(x2);
        }
        final SNumbers x3 = getInputNumbers(X3, true);
        if (x3.isInitialized() && (isOutputNecessary(X3) || x3.getArrayLength() < ARRAY_SIZE_COPIED_ALWAYS)) {
            getNumbers(X3).setTo(x3);
        }
        if (isOutputNecessary(M1)) {
            getMat(M1).setTo(getInputMat(M1, true));
        }
        if (isOutputNecessary(M2)) {
            getMat(M2).setTo(getInputMat(M2, true));
        }
        if (isOutputNecessary(M3)) {
            getMat(M3).setTo(getInputMat(M3, true));
        }
    }

    protected boolean nonEmptyPathRequired() {
        return true;
    }

    protected final void addWriteFileOperationPorts() {
        addInputScalar(S1);
        addInputNumbers(X1);
        addInputMat(M1);
        addInputScalar(S2);
        addInputNumbers(X2);
        addInputMat(M2);
        addInputScalar(S3);
        addInputNumbers(X3);
        addInputMat(M3);
        addOutputScalar(S1);
        addOutputNumbers(X1);
        addOutputMat(M1);
        addOutputScalar(S2);
        addOutputNumbers(X2);
        addOutputMat(M2);
        addOutputScalar(S3);
        addOutputNumbers(X3);
        addOutputMat(M3);
    }
}
