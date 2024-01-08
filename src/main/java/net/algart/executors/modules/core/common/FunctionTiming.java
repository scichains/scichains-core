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

package net.algart.executors.modules.core.common;

import java.util.Locale;

public class FunctionTiming {
    private int maximalNumberOfAnalysedCalls;
    private TimingStatistics execution;
    private TimingStatistics passingData;
    private TimingStatistics summary;

    private FunctionTiming(int maximalNumberOfAnalysedCalls) {
        if (maximalNumberOfAnalysedCalls < 0) {
            throw new IllegalArgumentException("Negative number of analysed calls for timing execution: "
                    + maximalNumberOfAnalysedCalls);
        }
        this.maximalNumberOfAnalysedCalls = maximalNumberOfAnalysedCalls;
        resetStatistics();
    }

    public static FunctionTiming newDisabledInstance() {
        return new FunctionTiming(0);
    }

    public static FunctionTiming newInstance(int maximalNumberOfAnalysedCalls) {
        return new FunctionTiming(maximalNumberOfAnalysedCalls);
    }

    public int getMaximalNumberOfAnalysedCalls() {
        return maximalNumberOfAnalysedCalls;
    }

    // Note: resets statistics if maximalNumberOfAnalysedCalls is changed
    public FunctionTiming setMaximalNumberOfAnalysedCalls(int maximalNumberOfAnalysedCalls) {
        if (maximalNumberOfAnalysedCalls != this.maximalNumberOfAnalysedCalls) {
            this.maximalNumberOfAnalysedCalls = maximalNumberOfAnalysedCalls;
            resetStatistics();
        }
        return this;
    }

    public FunctionTiming setSettings(TimingStatistics.Settings settings) {
        execution.setSettings(settings);
        passingData.setSettings(settings);
        summary.setSettings(settings);
        return this;
    }

    public FunctionTiming setSettings(int maximalNumberOfAnalysedCalls, TimingStatistics.Settings settings) {
        setMaximalNumberOfAnalysedCalls(maximalNumberOfAnalysedCalls);
        setSettings(settings);
        return this;
    }

    public final void resetStatistics() {
        this.execution = TimingStatistics.newInstance(maximalNumberOfAnalysedCalls);
        this.passingData = TimingStatistics.newInstance(maximalNumberOfAnalysedCalls);
        this.summary = TimingStatistics.newInstance(maximalNumberOfAnalysedCalls);
    }

    public long currentTime() {
        return execution.currentTime();
    }

    public void updateExecution(long time) {
        execution.update(time);
    }

    public void updatePassingData(long time) {
        passingData.update(time);
    }

    public void updateSummary(long time) {
        summary.update(time);
    }

    public void analyse() {
        execution.analyse();
        passingData.analyse();
        summary.analyse();
    }

    public TimingStatistics execution() {
        return execution;
    }

    public TimingStatistics passingData() {
        return passingData;
    }

    public TimingStatistics summary() {
        return summary;
    }

    public double summaryTimeOfLastAnalysedCalls() {
        return summary.summaryTimeOfLastAnalysedCalls();
    }

    public boolean isEmpty() {
        return execution.isEmpty();
    }

    public String toSimpleStringForSummary() {
        return toSimpleStringForSummary(null);
    }

    public String toSimpleStringForSummary(Double totalTimeOfLastAnalysedCalls) {
        return isEmpty() ? "was not executed" : "summary: " + summary.toSimpleString(totalTimeOfLastAnalysedCalls);
    }

    public String toString(String lineSeparator) {
        final int n = execution.numberOfStoredTimes();
        return n == 0 ? "was not executed" :
                "timing for " + n + " last calls:"
                        + lineSeparator + "summary:   " + summary + ", including"
                        + lineSeparator + "execution: " + execution + " and"
                        + lineSeparator + "copying:   " + passingData;
    }

    @Override
    public String toString() {
        return toString(String.format("%n      "));
    }

    public static void main(String[] args) {
        TimingStatistics.Settings settings = new TimingStatistics.Settings().setUniformPercentileLevels(5);
        FunctionTiming timing = FunctionTiming.newInstance(100).setSettings(settings);
        for (int k = 1; k < 20; k++) {
            if (k % 5 == 0) {
                timing.analyse();
            }
            System.out.println(timing.toString(String.format("%n    ")));
            timing.updateExecution(100 * k);
            timing.updatePassingData(30 - k);
            timing.updateSummary(200 * k);
        }
        for (int test = 1; test <= 15; test++) {
            System.out.println();
            long info = 0;
            final int n = 1000000;
            long t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                timing.updateExecution(100);
                timing.updatePassingData(100);
                timing.updateSummary(100);
                info += timing.execution.sumOfAllCalls + timing.passingData.sumOfAllCalls + timing.summary.sumOfAllCalls;
            }
            long t2 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                long tt1 = timing.currentTime();
                long tt2 = timing.currentTime();
                long tt3 = timing.currentTime();
                timing.updateExecution(tt1 - tt2);
                timing.updatePassingData(tt3 - tt2);
                timing.updateSummary(tt3 - tt1);
                info += timing.execution.sumOfAllCalls + timing.passingData.sumOfAllCalls + timing.summary.sumOfAllCalls;
            }
            long t3 = System.nanoTime();
            System.out.printf(Locale.US, "%d timings without actual currentTime: %.3f ms, %.2f ns/call%n",
                    n, (t2 - t1) * 1e-6, (t2 - t1) / (double) n);
            System.out.printf(Locale.US, "%d normal timings: %.3f ms, %.2f ns/call (dummy: %d)%n",
                    n, (t3 - t2) * 1e-6, (t3 - t2) / (double) n, info);
        }
    }
}
