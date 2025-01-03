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

package net.algart.executors.modules.core.scalars.conversions;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.NumbersToScalar;

import java.util.Locale;

public class JoinNumbersToScalar extends NumbersToScalar implements ReadOnlyExecutionInput {
    private int blockIndex = -1;
    private SNumbers.FormattingType formattingType = SNumbers.FormattingType.SIMPLE;
    private String locale = null;
    private String fixedPointFormat = "%7d";
    private String floatingPointFormat = "%10.3f";
    private String elementsDelimiter = ", ";
    private int minimalElementLength = 0;
    private boolean addLineIndexes = false;
    private String lineIndexFormat = "%d";
    private String lineIndexDelimiter = ": ";
    private int minimalLineIndexLength = 0;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private String linesDelimiter = "\\n";
    private boolean addEndingLinesDelimiter = true;
    private boolean simpleFormatForIntegers = false;
    private boolean parallelExecution = true;

    public JoinNumbersToScalar() {
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public JoinNumbersToScalar setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
        return this;
    }

    public SNumbers.FormattingType getFormattingType() {
        return formattingType;
    }

    public JoinNumbersToScalar setFormattingType(SNumbers.FormattingType formattingType) {
        this.formattingType = nonNull(formattingType);
        return this;
    }

    public String getLocale() {
        return locale;
    }

    public JoinNumbersToScalar setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    public String getFixedPointFormat() {
        return fixedPointFormat;
    }

    public JoinNumbersToScalar setFixedPointFormat(String fixedPointFormat) {
        this.fixedPointFormat = nonEmpty(fixedPointFormat);
        return this;
    }

    public String getFloatingPointFormat() {
        return floatingPointFormat;
    }

    public JoinNumbersToScalar setFloatingPointFormat(String floatingPointFormat) {
        this.floatingPointFormat = nonEmpty(floatingPointFormat);
        return this;
    }

    public String getElementsDelimiter() {
        return elementsDelimiter;
    }

    public JoinNumbersToScalar setElementsDelimiter(String elementsDelimiter) {
        this.elementsDelimiter = nonNull(elementsDelimiter);
        return this;
    }

    public int getMinimalElementLength() {
        return minimalElementLength;
    }

    public JoinNumbersToScalar setMinimalElementLength(int minimalElementLength) {
        this.minimalElementLength = nonNegative(minimalElementLength);
        return this;
    }

    public boolean isAddLineIndexes() {
        return addLineIndexes;
    }

    public JoinNumbersToScalar setAddLineIndexes(boolean addLineIndexes) {
        this.addLineIndexes = addLineIndexes;
        return this;
    }

    public String getLineIndexFormat() {
        return lineIndexFormat;
    }

    public JoinNumbersToScalar setLineIndexFormat(String lineIndexFormat) {
        this.lineIndexFormat = nonNull(lineIndexFormat);
        return this;
    }

    public String getLineIndexDelimiter() {
        return lineIndexDelimiter;
    }

    public JoinNumbersToScalar setLineIndexDelimiter(String lineIndexDelimiter) {
        this.lineIndexDelimiter = nonNull(lineIndexDelimiter);
        return this;
    }

    public int getMinimalLineIndexLength() {
        return minimalLineIndexLength;
    }

    public JoinNumbersToScalar setMinimalLineIndexLength(int minimalLineIndexLength) {
        this.minimalLineIndexLength = nonNegative(minimalLineIndexLength);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public JoinNumbersToScalar setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public String getLinesDelimiter() {
        return linesDelimiter;
    }

    public JoinNumbersToScalar setLinesDelimiter(String linesDelimiter) {
        this.linesDelimiter = nonNull(linesDelimiter);
        return this;
    }

    public boolean isAddEndingLinesDelimiter() {
        return addEndingLinesDelimiter;
    }

    public JoinNumbersToScalar setAddEndingLinesDelimiter(boolean addEndingLinesDelimiter) {
        this.addEndingLinesDelimiter = addEndingLinesDelimiter;
        return this;
    }

    public boolean isSimpleFormatForIntegers() {
        return simpleFormatForIntegers;
    }

    public JoinNumbersToScalar setSimpleFormatForIntegers(boolean simpleFormatForIntegers) {
        this.simpleFormatForIntegers = simpleFormatForIntegers;
        return this;
    }

    public boolean isParallelExecution() {
        return parallelExecution;
    }

    public JoinNumbersToScalar setParallelExecution(boolean parallelExecution) {
        this.parallelExecution = parallelExecution;
        return this;
    }

    @Override
    public SScalar analyse(SNumbers source) {
        return SScalar.valueOf(join(source));
    }

    public String join(SNumbers source) {
        if (!source.isInitialized()) {
            return null;
        }
        long t1 = debugTime();
//        if (formattingType == SNumbers.FormattingType.SIMPLE) {
//            return (blockIndex >= 0 ? new SNumbers().setToSingleBlock(source, blockIndex) : source)
//                    .toString(true);
//        }
        final String locale = trimOrNull(this.locale);
        final String elementsFormat = source.isFloatingPoint() ? floatingPointFormat : fixedPointFormat;
        final SNumbers.Formatter formatter = source
                .getFormatter(formattingType, locale == null ? null : Locale.forLanguageTag(locale))
                .setElementsFormat(elementsFormat)
                .setElementsDelimiter(elementsDelimiter)
                .setMinimalElementLength(minimalElementLength)
                .setAddLineIndexes(addLineIndexes)
                .setLineIndexFormat(lineIndexFormat)
                .setLineIndexDelimiter(lineIndexDelimiter)
                .setMinimalLineIndexLength(minimalLineIndexLength)
                .setLineIndexStart(indexingBase.start)
                .setLinesDelimiter(linesDelimiter
                        .replace("\\n", "\n")
                        .replace("\\r", "\r"))
                .setAddEndingLinesDelimiter(addEndingLinesDelimiter)
                .setSimpleFormatForIntegers(simpleFormatForIntegers)
                .setParallelExecution(parallelExecution);
        long t2 = debugTime();
        final String result = blockIndex >= 0 ?
                formatter.formatRange(blockIndex, 1) :
                formatter.format();
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Joining %dx%d numbers (%s): %.3f ms = %.3f ms initializing + %.3f ms processing",
                source.getBlockLength(), source.n(), formattingType,
                (t3 - t1) * 1e-6,
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6));
        return result;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        } else {
            return (s = s.trim()).isEmpty() ? null : s;
        }
    }
}
