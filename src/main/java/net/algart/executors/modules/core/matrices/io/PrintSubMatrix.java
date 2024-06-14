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

package net.algart.executors.modules.core.matrices.io;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.LogLevel;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToScalar;
import net.algart.executors.modules.core.scalars.io.WriteScalar;

import java.util.List;
import java.util.Locale;

public final class PrintSubMatrix extends MultiMatrixToScalar {
    private static final int MAX_STRING_LENGTH = 50000;

    private long startX = 0;
    private long startY = 0;
    private long startZ = 0;
    private long sizeX = 100;
    private long sizeY = 100;
    private long sizeZ = 1;
    private boolean printLineIndexes = true;
    private String delimiter = " ";
    private String format = "%3s";
    private LogLevel logLevel = LogLevel.INFO;
    private String file = FileOperation.DEFAULT_EMPTY_FILE;

    public long getStartX() {
        return startX;
    }

    public PrintSubMatrix setStartX(long startX) {
        this.startX = nonNegative(startX);
        return this;
    }

    public long getStartY() {
        return startY;
    }

    public PrintSubMatrix setStartY(long startY) {
        this.startY = nonNegative(startY);
        return this;
    }

    public long getStartZ() {
        return startZ;
    }

    public PrintSubMatrix setStartZ(long startZ) {
        this.startZ = startZ;
        return this;
    }

    public long getSizeX() {
        return sizeX;
    }

    public PrintSubMatrix setSizeX(long sizeX) {
        this.sizeX = positive(sizeX);
        return this;
    }

    public long getSizeY() {
        return sizeY;
    }

    public PrintSubMatrix setSizeY(long sizeY) {
        this.sizeY = positive(sizeY);
        return this;
    }

    public long getSizeZ() {
        return sizeZ;
    }

    public PrintSubMatrix setSizeZ(long sizeZ) {
        this.sizeZ = sizeZ;
        return this;
    }

    public boolean isPrintLineIndexes() {
        return printLineIndexes;
    }

    public PrintSubMatrix setPrintLineIndexes(boolean printLineIndexes) {
        this.printLineIndexes = printLineIndexes;
        return this;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public PrintSubMatrix setDelimiter(String delimiter) {
        this.delimiter = nonNull(delimiter);
        return this;
    }

    public String getFormat() {
        return format;
    }

    public PrintSubMatrix setFormat(String format) {
        this.format = nonNull(format);
        return this;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public PrintSubMatrix setLogLevel(LogLevel logLevel) {
        this.logLevel = nonNull(logLevel);
        return this;
    }

    public String getFile() {
        return file;
    }

    public PrintSubMatrix setFile(String file) {
        this.file = nonNull(file);
        return this;
    }


    @Override
    public Object process(MultiMatrix source) {
        StringBuilder sb = new StringBuilder();
        if (source == null) {
            sb.append("No input matrix");
        } else {
            final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
            final long sizeX = Math.min(this.sizeX, Math.max(0, source.dim(0) - startX));
            final long sizeY = Math.min(this.sizeY, Math.max(0, source.dim(1) - startY));
            final long sizeZ = Math.min(this.sizeZ, Math.max(0, source.dim(2) - startZ));
            sb.append(String.format("Printing matrix elements %dx%dx%d from (%d,%d,%d) for %s%n",
                    sizeX, sizeY, sizeZ, startX, startY, startZ, source));
            for (int k = 0; k < sourceChannels.size(); k++) {
                sb.append(String.format("Channel #%d:%n", k));
                Matrix<? extends PArray> channel = sourceChannels.get(k);
                if (source.bitsPerElement() <= 16) {
                    channel = Matrices.asFuncMatrix(Func.IDENTITY, IntArray.class, channel);
                    // boolean -> 0/1, byte and short -> unsigned
                }
                long[] position = new long[channel.dimCount()];
                // - zero-filled by Java
                long[] sizes = new long[position.length];
                JArrays.fillLongArray(sizes, 1);
                position[0] = startX;
                sizes[0] = sizeX;
                for (int z = 0; z < sizeZ; z++) {
                    if (source.dim(2) > 1) {
                        sb.append(String.format("z=%d:%n", startZ + z));
                    }
                    if (position.length >= 3) {
                        position[2] = startZ + z;
                    }
                    for (long y = 0; y < sizeY; y++) {
                        if (position.length >= 2) {
                            position[1] = startY + y;
                        }
                        if (printLineIndexes) {
                            sb.append(String.format(Locale.US, "  y=%d: ", startY + y));
                        }
                        final Array array = channel.subMatr(position, sizes,
                                Matrix.ContinuationMode.NAN_CONSTANT).array();
                        final Object javaArray = array.ja();
                        String s;
                        if (javaArray instanceof int[]) {
                            s = JArrays.toString((int[]) javaArray, Locale.US, format, delimiter, MAX_STRING_LENGTH);
                        } else if (javaArray instanceof long[]) {
                            s = JArrays.toString((long[]) javaArray, Locale.US, format, delimiter, MAX_STRING_LENGTH);
                        } else if (javaArray instanceof float[]) {
                            s = JArrays.toString((float[]) javaArray, Locale.US, format, delimiter, MAX_STRING_LENGTH);
                        } else if (javaArray instanceof double[]) {
                            s = JArrays.toString((double[]) javaArray, Locale.US, format, delimiter, MAX_STRING_LENGTH);
                        } else {
                            s = Arrays.toString(array, delimiter, MAX_STRING_LENGTH);
                        }
                        sb.append(s).append(String.format("%n"));
                    }
                }
            }
        }
        final String result = sb.toString();
        logLevel.log(result);
        if (!file.isEmpty()) {
            WriteScalar.getInstance().setFile(file).writeString(result);
        }
        return result;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
