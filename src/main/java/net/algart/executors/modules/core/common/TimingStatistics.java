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

package net.algart.executors.modules.core.common;

import net.algart.arrays.ArraySelector;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public abstract class TimingStatistics {
    public static class Settings {
        public static final int MAXIMAL_NUMBER_OF_PERCENTILES = 1000;
        // - to avoid too slow showing string representation

        private double[] percentileLevels = new double[0];

        public double[] getPercentileLevels() {
            return percentileLevels.clone();
        }

        public Settings setPercentileLevels(double[] percentileLevels) {
            Objects.requireNonNull(percentileLevels, "Null percentileLevels");
            if (percentileLevels.length > MAXIMAL_NUMBER_OF_PERCENTILES) {
                throw new IllegalArgumentException("Requested number of percentiles " + percentileLevels.length
                        + " > maximal possible value " + MAXIMAL_NUMBER_OF_PERCENTILES);
            }
            percentileLevels = percentileLevels.clone();
            if (percentileLevels.length > 0) {
                ArraySelector.checkPercentileLevels(percentileLevels);
            }
            this.percentileLevels = percentileLevels;
            return this;
        }

        public Settings setUniformPercentileLevels(int numberOfPercentiles) {
            if (numberOfPercentiles < 0) {
                throw new IllegalArgumentException("Negative number of percentiles for timing  execution: "
                        + numberOfPercentiles);
            }
            if (numberOfPercentiles > MAXIMAL_NUMBER_OF_PERCENTILES) {
                throw new IllegalArgumentException("Requested number of percentiles " + numberOfPercentiles
                        + " > maximal possible value " + MAXIMAL_NUMBER_OF_PERCENTILES);
            }
            if (this.percentileLevels.length != numberOfPercentiles) {
                this.percentileLevels = new double[numberOfPercentiles];
            }
            if (percentileLevels.length == 1) {
                this.percentileLevels[0] = 0.5;
                // - if only 1 percentile requested, we prefer median
            } else if (percentileLevels.length != 0) {
                Arrays.setAll(this.percentileLevels, k -> (double) k / (double) (percentileLevels.length - 1));
            }
            return this;
        }
    }

    final long[] times;
    int current = 0;
    boolean full = false;
    long last = 0;
    long sumOfAllCalls = 0;
    long numberOfAllCalls = 0;

    private int numberOfAnalysedTimes = 0;
    private long sum = 0;
    private double[] percentileLevels = new double[0];
    private long[] percentiles = new long[0];
    private int minIndex = -1;
    private int maxIndex = -1;
    private int medianIndex = -1;

    private TimingStatistics(int maximalNumberOfStoredTimes) {
        if (maximalNumberOfStoredTimes < 0) {
            throw new IllegalArgumentException("Negative number of stored times");
        }
        this.times = new long[maximalNumberOfStoredTimes];
    }

    public static TimingStatistics newInstance(int maximalNumberOfStoredTimes) {
        return maximalNumberOfStoredTimes == 0 ? new Empty() : new NonEmpty(maximalNumberOfStoredTimes);
    }

    public void setSettings(Settings settings) {
        Objects.requireNonNull(settings, "Null settings");
        this.percentileLevels = settings.percentileLevels.clone();
        this.percentiles = new long[this.percentileLevels.length];
        this.minIndex = this.maxIndex = this.medianIndex = -1;
        for (int k = 0; k < percentiles.length; k++) {
            final double level = this.percentileLevels[k];
            if (level == 0.0) {
                minIndex = k;
            } else if (level == 0.5) {
                medianIndex = k;
            } else if (level == 1.0) {
                maxIndex = k;
            }
        }
    }

    public TimingStatistics analyse() {
        // Note: if numberOfTimes == times.length, the order of its elements is important
        // only to get times[lastTimeIndex];
        // for summing and finding percentiles, we may just use all elements ignoring lastTimeIndex

        final int n = numberOfStoredTimes();
        sum = Arrays.stream(times, 0, n).sum();
        findPercentiles(percentiles, times, n, percentileLevels);
        // Note: this method destroys (reorders) times array, but it is not a problem for further calls.
        numberOfAnalysedTimes = n;
        return this;
    }

    public abstract long currentTime();

    public abstract void update(long time);

    public boolean isEmpty() {
        return numberOfStoredTimes() == 0;
    }

    public int numberOfStoredTimes() {
        return full ? times.length : current;
    }

    public int numberOfAnalysedTimes() {
        return numberOfAnalysedTimes;
    }

    public long lastTime() {
        return last;
    }

    public double numberOfAllCalls() {
        return numberOfAllCalls;
    }

    public double summaryTimeOfAllCalls() {
        return sumOfAllCalls;
    }

    public double averageTimeOfAllCalls() {
        return numberOfAllCalls == 0 ? 0.0 : (double) sumOfAllCalls / (double) numberOfAllCalls;
    }

    public double summaryTimeOfLastAnalysedCalls() {
        return sum;
    }

    public double averageTimeOfLastAnalysedCalls() {
        return numberOfAnalysedTimes == 0 ? 0.0 : sum / (double) numberOfAnalysedTimes;
    }

    public Double medianTimeOrNull() {
        return medianIndex == -1 ? null : numberOfAnalysedTimes == 0 ? 0.0 : percentiles[medianIndex];
    }

    public long[] percentiles() {
        return percentiles.clone();
    }

    public double[] percentileLevels() {
        return percentileLevels.clone();
    }

    public String toSimpleString() {
        return toSimpleString(null);
    }

    public String toSimpleString(Double totalTimeOfLastAnalysedCalls) {
        final StringBuilder sb = new StringBuilder();
        if (minIndex != -1 && maxIndex != -1) {
            sb.append(leftPad(String.format(Locale.US, "%.6f..%.6f, ",
                    percentiles[minIndex] * 1e-6, percentiles[maxIndex] * 1e-6), 26));
        }
        if (medianIndex != -1) {
            sb.append(leftPad(String.format(Locale.US, "median %.6f, ",
                    percentiles[medianIndex] * 1e-6), 20));
        }
        sb.append(leftPad(String.format(Locale.US, "mean ~%.6f ms",
                averageTimeOfLastAnalysedCalls() * 1e-6), 20));
        if (totalTimeOfLastAnalysedCalls != null) {
            sb.append(leftPad(String.format(Locale.US, " (%.3f%%)",
                    summaryTimeOfLastAnalysedCalls() * 100.0 / totalTimeOfLastAnalysedCalls), 10));
        }
        return sb.toString();
    }

    public String toString() {
        if (isEmpty()) {
            return "no information";
        }
        if (numberOfAnalysedTimes == 0) {
            return "not analysed yet";
        }
        final StringBuilder sb = new StringBuilder();
        if (percentileLevels.length > 0) {
            sb.append(" [");
            for (int k = 0; k < percentiles.length; k++) {
                if (k > 0) {
                    sb.append(", ");
                }
                final double level = percentileLevels[k];
                if (level == 0.0) {
                    sb.append("min ");
                } else if (level == 0.5) {
                    sb.append("median ");
                } else if (level == 1.0) {
                    sb.append("max ");
                }
                sb.append(String.format(Locale.US, "%.6f", percentiles[k] * 1e-6));
            }
            sb.append("]");
        }
        final String allInfo;
        if (numberOfAllCalls != numberOfAnalysedTimes) {
            // assert numberOfAllCalls > numberOfAnalysedTimes;
            // - such assert would be invalid while 63-bit overflow
            allInfo = String.format(Locale.US, " [from start: %d calls, sum %.6f, mean ~%.6f ms]",
                    numberOfAllCalls, summaryTimeOfAllCalls() * 1e-6, averageTimeOfAllCalls() * 1e-6);
        } else {
//            assert sumOfAllCalls == sum : "sumOfAllCalls = " + sumOfAllCalls + " != sum = " + sum;
            // - such assert would be invalid while 63-bit overflow
            allInfo = "";
        }
        return String.format(Locale.US, "%d calls: last %.6f%s, sum %.6f, mean ~%.6f ms%s",
                numberOfAnalysedTimes,
                lastTime() * 1e-6,
                sb,
                summaryTimeOfLastAnalysedCalls() * 1e-6,
                averageTimeOfLastAnalysedCalls() * 1e-6,
                allInfo);
    }

    private static void findPercentiles(long[] result, long[] times, int length, double[] percentileLevels) {
        assert length >= 0 && length <= times.length : "length=" + length + ", must be 0<length<=" + times.length;
        assert result.length == percentileLevels.length;
        if (length > 0 && percentileLevels.length > 0) {
            ArraySelector.getQuickSelector().select(percentileLevels, times, length);
            Arrays.setAll(result, k -> times[ArraySelector.percentileIndex(percentileLevels[k], length)]);
        }
    }

    private static String leftPad(String s, int length) {
        return s.length() >= length ? s : " ".repeat(length - s.length()) + s;
    }

    private static class Empty extends TimingStatistics {
        private Empty() {
            super(0);
        }

        @Override
        public long currentTime() {
            return 0;
        }

        @Override
        public void update(long time) {
        }
    }

    private static class NonEmpty extends TimingStatistics {
        private NonEmpty(int numberOfTimes) {
            super(numberOfTimes);
        }

        @Override
        public long currentTime() {
            return System.nanoTime();
        }

        public void update(long time) {
            sumOfAllCalls += time;
            numberOfAllCalls++;
            times[current++] = last = time;
            if (current == times.length) {
                current = 0;
                full = true;
            }
        }
    }
}
