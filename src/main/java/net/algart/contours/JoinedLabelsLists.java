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

package net.algart.contours;

import net.algart.arrays.TooLargeArrayException;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

class JoinedLabelsLists {
    final int[] indexes;
    final int[] offsets;
    final int maxListLength;
    final int numberOfLists;
    int[] clusterMinX = null;
    int[] clusterMaxX = null;
    int[] clusterMinY = null;
    int[] clusterMaxY = null;

    // reindexedLabels.length is the number of contours
    JoinedLabelsLists(int[] reindexedLabels) {
        int maxReindexedLabel = 0;
        for (int label : reindexedLabels) {
            if (label > maxReindexedLabel) {
                maxReindexedLabel = label;
            }
        }
        if (maxReindexedLabel > Integer.MAX_VALUE - 1) {
            throw new TooLargeArrayException("Cannot process reindexed object label > Integer.MAX_VALUE-1");
        }
        this.numberOfLists = maxReindexedLabel + 1;
        offsets = new int[numberOfLists + 1];
        // - zero-filled by Java
        indexes = new int[reindexedLabels.length];
        for (int label : reindexedLabels) {
            offsets[label]++;
        }
        int offset = 0;
        int maxLength = 0;
        for (int i = 0; i < numberOfLists; i++) {
            int length = offsets[i];
            offsets[i] = offset;
            offset += length;
            if (length > maxLength) {
                maxLength = length;
            }
        }
        assert offset == reindexedLabels.length;
        maxListLength = maxLength;
        for (int i = 0; i < reindexedLabels.length; i++) {
            int label = reindexedLabels[i];
            indexes[offsets[label]++] = i;
        }
        System.arraycopy(offsets, 0, offsets, 1, numberOfLists);
        offsets[0] = 0;
    }

    int length(int reindexedLabel) {
        return offsets[reindexedLabel + 1] - offsets[reindexedLabel];
    }

    boolean hasNeighboursToJoin(int reindexedLabel) {
        return length(reindexedLabel) > 1;
    }

    void initializeNonEmptyClusterRectangles(int[] allMinX, int[] allMaxX, int[] allMinY, int[] allMaxY) {
        this.clusterMinX = new int[numberOfLists];
        this.clusterMaxX = new int[numberOfLists];
        this.clusterMinY = new int[numberOfLists];
        this.clusterMaxY = new int[numberOfLists];
        // - zero-filled by Java
        IntStream.range(0, (numberOfLists + 255) >>> 8).parallel().forEach(block -> {
            for (int label = block << 8, to = (int) Math.min((long) label + 256, numberOfLists); label < to; label++) {
                if (!hasNeighboursToJoin(label)) {
                    continue;
                }
                int clusterMinX = Integer.MAX_VALUE;
                int clusterMaxX = Integer.MIN_VALUE;
                int clusterMinY = Integer.MAX_VALUE;
                int clusterMaxY = Integer.MIN_VALUE;
                for (int k = offsets[label], kTo = offsets[label + 1]; k < kTo; k++) {
                    final int index = indexes[k];
                    final int minX = allMinX[index];
                    final int maxX = allMaxX[index];
                    final int minY = allMinY[index];
                    final int maxY = allMaxY[index];
                    if (minX < clusterMinX) {
                        clusterMinX = minX;
                    }
                    if (maxX > clusterMaxX) {
                        clusterMaxX = maxX;
                    }
                    if (minY < clusterMinY) {
                        clusterMinY = minY;
                    }
                    if (maxY > clusterMaxY) {
                        clusterMaxY = maxY;
                    }
                }
                this.clusterMinX[label] = clusterMinX;
                this.clusterMaxX[label] = clusterMaxX;
                this.clusterMinY[label] = clusterMinY;
                this.clusterMaxY[label] = clusterMaxY;
            }
        });
    }

    long estimateMaxClusterMatrixSize() {
        return LongStream.range(0, (numberOfLists + 255) >>> 8).parallel().map(block -> {
            long max = -1;
            for (int i = (int) block << 8, to = (int) Math.min((long) i + 256, numberOfLists); i < to; i++) {
                final int diffX = clusterMaxX[i] - clusterMinX[i];
                final int diffY = clusterMaxY[i] - clusterMinY[i];
                assert diffX >= 0 && diffY >= 0 : "Overflow in sizes of containing rectangle for cluster #"
                        + i + "; it must be impossible due to check of Contour2DArray.MAX_ABSOLUTE_COORDINATE";
                final long matrixSize = (1 + (long) diffX) * (1 + (long) diffY);
                // - will be 1x1 for degenerated clusters (1 element in list)
                if (matrixSize > max) {
                    max = matrixSize;
                }
            }
            return max;
        }).max().orElse(0);
    }

    long maxClusterGridMatrixSize(int gridStepLog, boolean require31BitResult) {
        if (gridStepLog < 0) {
            throw new IllegalArgumentException("Negative gridStepLog");
        }
        final long result = LongStream.range(0, (numberOfLists + 255) >>> 8).parallel().map(block -> {
            long max = -1;
            final int log = gridStepLog;
            for (int i = (int) block << 8, to = (int) Math.min((long) i + 256, numberOfLists); i < to; i++) {
                final int diffX = (clusterMaxX[i] >> log) - (clusterMinX[i] >> log);
                final int diffY = (clusterMaxY[i] >> log) - (clusterMinY[i] >> log);
                // - note: it is floor(xxx / 2^gridStepLog) even for negative numbers
                assert diffX >= 0 && diffY >= 0 : "Overflow in sizes of containing rectangle for cluster #"
                        + i + "; it must be impossible due to check of Contour2DArray.MAX_ABSOLUTE_COORDINATE";
                final long matrixSize = (1 + (long) diffX) * (1 + (long) diffY);
                // - will be 1x1 for degenerated clusters (1 element in list)
                if (matrixSize > Integer.MAX_VALUE && require31BitResult) {
                    throw new TooLargeArrayException("Too large maximal cluster "
                            + "(set of contours that should be joined): its containing rectangle "
                            + (clusterMinX[i] << log) + ".." + (clusterMaxX[i] << log)
                            + " x " + (clusterMinY[i] << log) + ".." + (clusterMaxY[i] << log)
                            + " requires grid " + (1 + (long) diffX) + " x " + (1 + (long) diffY)
                            + " >= 2^31 elements; probably you must increase gridStepLog = " + log
                            + " and, so, reduce scale of the grid 2^gridStepLog = " + (1 << log));
                }
                if (matrixSize > max) {
                    max = matrixSize;
                }
            }
            return max;
        }).max().orElse(0);
        if (require31BitResult) {
            assert result == (int) result;
        }
        return result;
    }

    public static void main(String[] args) {
        int[] labels = {0, 1, 2, 1, 1, 0};
        final JoinedLabelsLists result = new JoinedLabelsLists(labels);
        System.out.println(java.util.Arrays.toString(result.indexes));
        System.out.println(java.util.Arrays.toString(result.offsets));
        System.out.println(result.maxListLength);
    }
}
