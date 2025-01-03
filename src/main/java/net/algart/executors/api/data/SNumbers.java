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

package net.algart.executors.api.data;

import net.algart.arrays.*;
import net.algart.contours.Contours;
import net.algart.external.UsedForExternalCommunication;
import net.algart.math.*;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simple number array (multi-column).
 *
 * <p>Note: this class <b>does not</b> contain methods, providing access to file system or other unsafe
 * resources. It is important while using inside user-defined scripts.
 */
public final class SNumbers extends Data implements Cloneable {
    public static List<Class<?>> SUPPORTED_ELEMENT_TYPES = List.of(
            byte.class, short.class, int.class, long.class, float.class, double.class);

    public enum FormattingType {
        SIMPLE(Formatter::createSimpleElementFormatter),
        PRINTF(Formatter::createPrintfElementFormatter),
        DECIMAL_FORMAT(Formatter::createDecimalElementFormatter);

        private final BiFunction<Formatter, Integer, ElementFormatter> creator;

        FormattingType(BiFunction<Formatter, Integer, ElementFormatter> creator) {
            this.creator = creator;
        }

        ElementFormatter elementFormatter(Formatter formatter, int estimatedCapacity) {
            return creator.apply(formatter, estimatedCapacity);
        }
    }

    public class Formatter {
        //        private static final int PARALLEL_LOG = 12;
//        private static final int PARALLEL_BUNDLE = 1 << PARALLEL_LOG;
        private static final int MIN_NUMBER_FOR_PARALLEL = 4096;

        private final int n;
        private final int blockLength;
        private final FormattingType formattingType;
        private final Locale locale;
        String elementsFormat = "%s";
        private String elementsDelimiter = ", ";
        private int minimalElementLength = 0;
        private boolean addLineIndexes = false;
        private String lineIndexFormat = "%s";
        private String lineIndexDelimiter = ": ";
        private int minimalLineIndexLength = 0;
        private int lineIndexStart = 0;
        private String linesDelimiter = "\n";
        private boolean addEndingLinesDelimiter = true;
        private boolean simpleFormatForIntegers = false;
        private boolean parallelExecution = true;

        private Formatter(FormattingType formattingType, Locale locale) {
            this.formattingType = Objects.requireNonNull(formattingType, "Null formatter type");
            this.locale = locale;
            this.n = n();
            this.blockLength = blockLength();
        }

        public String getElementsFormat() {
            return elementsFormat;
        }

        public Formatter setElementsFormat(String elementsFormat) {
            this.elementsFormat = Objects.requireNonNull(elementsFormat, "Null elements format");
            return this;
        }

        public String getElementsDelimiter() {
            return elementsDelimiter;
        }

        public Formatter setElementsDelimiter(String elementsDelimiter) {
            this.elementsDelimiter = Objects.requireNonNull(elementsDelimiter, "Null elements delimiter");
            return this;
        }

        public int getMinimalElementLength() {
            return minimalElementLength;
        }

        public Formatter setMinimalElementLength(int minimalElementLength) {
            if (minimalElementLength < 0) {
                throw new IllegalArgumentException("Negative minimalElementLength = " + minimalElementLength);
            }
            this.minimalElementLength = minimalElementLength;
            return this;
        }

        public boolean isAddLineIndexes() {
            return addLineIndexes;
        }

        public Formatter setAddLineIndexes(boolean addLineIndexes) {
            this.addLineIndexes = addLineIndexes;
            return this;
        }

        public String getLineIndexFormat() {
            return lineIndexFormat;
        }

        public Formatter setLineIndexFormat(String lineIndexFormat) {
            this.lineIndexFormat = Objects.requireNonNull(lineIndexFormat, "Null line index format");
            return this;
        }

        public String getLineIndexDelimiter() {
            return lineIndexDelimiter;
        }

        public Formatter setLineIndexDelimiter(String lineIndexDelimiter) {
            this.lineIndexDelimiter = Objects.requireNonNull(lineIndexDelimiter, "Null line index delimiter");
            return this;
        }

        public int getMinimalLineIndexLength() {
            return minimalLineIndexLength;
        }

        public Formatter setMinimalLineIndexLength(int minimalLineIndexLength) {
            if (minimalLineIndexLength < 0) {
                throw new IllegalArgumentException("Negative minimalLineIndexLength = " + minimalLineIndexLength);
            }
            this.minimalLineIndexLength = minimalLineIndexLength;
            return this;
        }

        public int getLineIndexStart() {
            return lineIndexStart;
        }

        public Formatter setLineIndexStart(int lineIndexStart) {
            this.lineIndexStart = lineIndexStart;
            return this;
        }

        public Formatter setLinesDelimiter(String linesDelimiter) {
            this.linesDelimiter = Objects.requireNonNull(linesDelimiter, "Null lines delimiter");
            return this;
        }

        public boolean isAddEndingLinesDelimiter() {
            return addEndingLinesDelimiter;
        }

        public Formatter setAddEndingLinesDelimiter(boolean addEndingLinesDelimiter) {
            this.addEndingLinesDelimiter = addEndingLinesDelimiter;
            return this;
        }

        public boolean isSimpleFormatForIntegers() {
            return simpleFormatForIntegers;
        }

        public Formatter setSimpleFormatForIntegers(boolean simpleFormatForIntegers) {
            this.simpleFormatForIntegers = simpleFormatForIntegers;
            return this;
        }

        public boolean isParallelExecution() {
            return parallelExecution;
        }

        public Formatter setParallelExecution(boolean parallelExecution) {
            this.parallelExecution = parallelExecution;
            return this;
        }

        public String format() {
            return formatRange(0, n);
        }

        public String formatRange(int blockIndex, int numberOfBlocks) {
            String result = parallelExecution ?
                    parallelFormatRange(blockIndex, numberOfBlocks) :
                    singleThreadFormatRange(blockIndex, numberOfBlocks);
            if (!addEndingLinesDelimiter && result.endsWith(linesDelimiter)) {
                // - endsWith check is necessary for a case numberOfBlocks=0
                result = result.substring(0, result.length() - linesDelimiter.length());
            }
            return result;
        }

        private String parallelFormatRange(int blockIndex, int numberOfBlocks) {
            if (blockIndex < 0) {
                throw new IllegalArgumentException("Negative block index: " + blockIndex);
            }
            if (blockIndex + numberOfBlocks > n) {
                throw new IllegalArgumentException("Start block index and number of blocks = "
                        + blockIndex + " and " + blockLength
                        + " are out of range 0..n-1 = 0.." + (n - 1));
            }
            if (numberOfBlocks < MIN_NUMBER_FOR_PARALLEL) {
                return singleThreadFormatRange(blockIndex, numberOfBlocks);
            }
            final int numberOfRanges = recommendedNumberOfRanges(numberOfBlocks);
            final int[] splitters = splitToRanges(numberOfBlocks, numberOfRanges);
            return IntStream.range(0, numberOfRanges).parallel().mapToObj(
                    rangeIndex -> {
                        int from = splitters[rangeIndex];
                        int to = splitters[rangeIndex + 1];
                        return singleThreadFormatRange(from + blockIndex, to - from);
                    }).collect(Collectors.joining());

// The following variant would be better for numbers, but for Strings we prefer to split to relatively large blocks
// (see splitToRanges): it guarantees that we will not use a lot of extra memory
// and usually provides better performance (less number of memory allocations)
//
//            return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToObj(
//                    block -> {
//                        int from = block << PARALLEL_LOG;
//                        int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
//                        return formatRange(from + blockIndex, size);
//                    }).collect(Collectors.joining());
        }

        private String singleThreadFormatRange(int blockIndex, int numberOfBlocks) {
            final int estimatedCapacity = 5 * blockLength * numberOfBlocks;
            // - not too accurate, but probably better than 0
            final ElementFormatter elementFormatter = elementFormatter(estimatedCapacity);
            final int blockLengthM1 = this.blockLength - 1;
            final int lineIndexStart = this.lineIndexStart;
            final boolean addLineIndexes = this.addLineIndexes;
            final int minimalLineIndexLength = this.minimalLineIndexLength;
            final int minimalElementLength = this.minimalElementLength;
            // - JVM works better with local variables, not fields of an object
            for (int k = blockIndex, disp = k * blockLength, kTo = k + numberOfBlocks; k < kTo; k++) {
                if (addLineIndexes) {
                    if (minimalLineIndexLength > 0) {
                        elementFormatter.formatLineIndex(
                                lineIndexStart + k, lineIndexDelimiter, minimalLineIndexLength);
                    } else {
                        elementFormatter.formatLineIndex(
                                lineIndexStart + k, lineIndexDelimiter);
                    }
                }
                if (minimalElementLength > 0) {
                    for (int dispTo = disp + blockLengthM1; disp < dispTo; disp++) {
                        elementFormatter.formatElement(disp, elementsDelimiter, minimalElementLength);
                    }
                    elementFormatter.formatElement(disp++, linesDelimiter, minimalElementLength);
                } else {
                    for (int dispTo = disp + blockLengthM1; disp < dispTo; disp++) {
                        elementFormatter.formatElement(disp, elementsDelimiter);
                    }
                    elementFormatter.formatElement(disp++, linesDelimiter);
                }

            }
            return elementFormatter.result();
        }

        private ElementFormatter elementFormatter(int estimatedCapacity) {
            if (simpleFormatForIntegers && !isFloatingPoint()) {
                return FormattingType.SIMPLE.elementFormatter(this, estimatedCapacity);
            }
            final ElementFormatter formatter = formattingType.elementFormatter(this, estimatedCapacity);
            if (simpleFormatForIntegers) {
                if (isFloatArray()) {
                    return new SimpleForIntegersFloatsWrapper(formatter);
                } else if (isDoubleArray()) {
                    return new SimpleForIntegersDoublesWrapper(formatter);
                } else {
                    throw new AssertionError("Unsupported floating-point Java array type: " + array);
                }
            }
            return formatter;
        }

        private ElementFormatter createSimpleElementFormatter(int estimatedCapacity) {
            if (isByteArray()) {
                return new SimpleBytesElementFormatter(estimatedCapacity);
            } else if (isShortArray()) {
                return new SimpleShortsElementFormatter(estimatedCapacity);
            } else if (isIntArray()) {
                return new SimpleIntsElementFormatter(estimatedCapacity);
            } else if (isLongArray()) {
                return new SimpleLongsElementFormatter(estimatedCapacity);
            } else if (isFloatArray()) {
                return new SimpleFloatsElementFormatter(estimatedCapacity);
            } else if (isDoubleArray()) {
                return new SimpleDoublesElementFormatter(estimatedCapacity);
            } else {
                throw new AssertionError("Unsupported Java array type: " + array);
            }
        }

        private ElementFormatter createPrintfElementFormatter(int estimatedCapacity) {
            if (isByteArray()) {
                return new PrintfBytesElementFormatter(this, estimatedCapacity);
            } else if (isShortArray()) {
                return new PrintfShortsElementFormatter(this, estimatedCapacity);
            } else if (isIntArray()) {
                return new PrintfIntsElementFormatter(this, estimatedCapacity);
            } else if (isLongArray()) {
                return new PrintfLongsElementFormatter(this, estimatedCapacity);
            } else if (isFloatArray()) {
                return new PrintfFloatsElementFormatter(this, estimatedCapacity);
            } else if (isDoubleArray()) {
                return new PrintfDoublesElementFormatter(this, estimatedCapacity);
            } else {
                throw new AssertionError("Unsupported Java array type: " + array);
            }
        }

        private ElementFormatter createDecimalElementFormatter(int estimatedCapacity) {
            if (isByteArray()) {
                return new DecimalBytesElementFormatter(this, estimatedCapacity);
            } else if (isShortArray()) {
                return new DecimalShortsElementFormatter(this, estimatedCapacity);
            } else if (isIntArray()) {
                return new DecimalIntsElementFormatter(this, estimatedCapacity);
            } else if (isLongArray()) {
                return new DecimalBytesElementFormatter(this, estimatedCapacity);
            } else if (isFloatArray()) {
                return new DecimalFloatsElementFormatter(this, estimatedCapacity);
            } else if (isDoubleArray()) {
                return new DecimalDoublesElementFormatter(this, estimatedCapacity);
            } else {
                throw new AssertionError("Unsupported Java array type: " + array);
            }
        }
    }

    public static boolean isSupportedJavaArray(Object javaArray) {
        return javaArray instanceof byte[]
                || javaArray instanceof short[]
                || javaArray instanceof int[]
                || javaArray instanceof long[]
                || javaArray instanceof float[]
                || javaArray instanceof double[];
    }

    public static boolean isSupportedJavaElementType(Class<?> elementType) {
        return elementType == byte.class
                || elementType == short.class
                || elementType == int.class
                || elementType == long.class
                || elementType == float.class
                || elementType == double.class;
    }

    public static Object convertToArray(Collection<?> numbers) {
        Object result = null;
        byte[] bytes = null;
        short[] shorts = null;
        int[] ints = null;
        long[] longs = null;
        float[] floats = null;
        double[] doubles = null;
        final int size = numbers.size();
        int k = 0;
        for (Object e : numbers) {
            if (e == null) {
                throw new IllegalArgumentException("Null elements #" + k + " in a collection of numbers");
            }
            if (e instanceof Byte v) {
                if (bytes == null) {
                    checkChangingElementType(result, bytes, k, e);
                    result = bytes = new byte[size];
                }
                bytes[k++] = v;
            } else if (e instanceof Short v) {
                if (shorts == null) {
                    checkChangingElementType(result, shorts, k, e);
                    result = shorts = new short[size];
                }
                shorts[k++] = v;
            } else if (e instanceof Integer v) {
                if (ints == null) {
                    checkChangingElementType(result, ints, k, e);
                    result = ints = new int[size];
                }
                ints[k++] = v;
            } else if (e instanceof Long v) {
                if (longs == null) {
                    checkChangingElementType(result, longs, k, e);
                    result = longs = new long[size];
                }
                longs[k++] = v;
            } else if (e instanceof Float v) {
                if (floats == null) {
                    checkChangingElementType(result, floats, k, e);
                    result = floats = new float[size];
                }
                floats[k++] = v;
            } else if (e instanceof Double v) {
                if (doubles == null) {
                    checkChangingElementType(result, doubles, k, e);
                    result = doubles = new double[size];
                }
                doubles[k++] = v;
            } else {
                throw new IllegalArgumentException(
                        "Illegal non-numeric type of elements #" + k + " in the collection of numbers: "
                                + e.getClass());
            }
        }
        return result;
    }

    private static void checkChangingElementType(Object result, Object expected, int k, Object e) {
        if (result != null && result != expected) {
            throw new IllegalArgumentException(
                    "Collection of numbers contain elements of different types: " +
                            "element #" + k + " (" + e + ") has another type for storing in "
                            + result.getClass().getCanonicalName());
        }
    }

    public static Class<?> elementType(String primitiveElementTypeName) {
        Objects.requireNonNull(primitiveElementTypeName, "Null element type name");
        return switch (primitiveElementTypeName) {
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            default ->
                    throw new IllegalArgumentException(
                            "Illegal or unsupported element type: " + primitiveElementTypeName);
        };
    }

    @UsedForExternalCommunication
    private Object array = null;

    @UsedForExternalCommunication
    private int blockLength = 1;
    // The length of Java array is always divided by blockLength.
    // The sense on blockLength may be any, but usually it is the size of some
    // little logical unit like point, rectangle, triangle, pair of related value etc.

    @UsedForExternalCommunication
    public SNumbers() {
    }

    @UsedForExternalCommunication
    public Object getArray() {
        return cloneJavaArray(array);
    }

    /**
     * Returns a reference to the internal Java array
     * <code>byte[]</code>, <code>short[]</code>, <code>int[]</code>, <code>long[]</code>,
     * <code>float[]</code> or <code>double[]</code>.
     *
     * <p>Always returns  <code>null</code> for non-{@link #isInitialized() initialized} object
     * and non-<code>null</code> for initialized one.
     *
     * <p>Please use this function carefully, only if you need maximal performance. Usually it is better idea
     * to use one of methods {@link #toIntArray()}, {@link #toFloatArray()} and analogous.</p>
     *
     * @return the reference to stored Java array.
     */
    public Object arrayReference() {
        return array;
    }

    public int[] toIntArrayOrReference() {
        return isIntArray() ? (int[]) array : toIntArray();
    }

    public int getArrayLength() {
        return array == null ? 0 : Array.getLength(array);
    }

    @UsedForExternalCommunication
    public int getBlockLength() {
        return blockLength;
    }

    @UsedForExternalCommunication
    public void setBlockLength(int blockLength) {
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Block length " + blockLength + " is not positive");
        }
        this.blockLength = blockLength;
    }

    public int blockLength() {
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot call blockLength(): numbers array is not initialized. "
                    + "You may use getBlockLength() or blockLengthOrZero() instead.");
        }
        return blockLength;
    }

    public int blockLengthOrZero() {
        return isInitialized() ? blockLength : 0;
    }

    public Class<?> elementType() {
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot get element type: numbers array is not initialized");
        }
        return array.getClass().getComponentType();
    }

    public int n() {
        return array == null ? 0 : Array.getLength(array) / blockLength;
    }

    public boolean isEmpty() {
        return getArrayLength() == 0;
    }

    public boolean isUnsigned() {
        return isByteArray() || isShortArray();
    }

    public double getValue(int blockIndex, int indexInBlock) {
        checkGetSetIndex(blockIndex, indexInBlock, 1);
        int indexInArray = blockIndex * blockLength + indexInBlock;
        if (isByteArray()) {
            return ((byte[]) array)[indexInArray] & 0xFF;
        } else if (isShortArray()) {
            return ((short[]) array)[indexInArray] & 0xFFFF;
        } else if (isIntArray()) {
            return ((int[]) array)[indexInArray];
        } else if (isLongArray()) {
            return ((long[]) array)[indexInArray];
        } else if (isFloatArray()) {
            return ((float[]) array)[indexInArray];
        } else if (isDoubleArray()) {
            return ((double[]) array)[indexInArray];
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public void setValue(int blockIndex, int indexInBlock, double value) {
        checkGetSetIndex(blockIndex, indexInBlock, 1);
        int indexInArray = blockIndex * blockLength + indexInBlock;
        if (isByteArray()) {
            ((byte[]) array)[indexInArray] = (byte) value;
        } else if (isShortArray()) {
            ((short[]) array)[indexInArray] = (short) value;
        } else if (isIntArray()) {
            ((int[]) array)[indexInArray] = (int) value;
        } else if (isLongArray()) {
            ((long[]) array)[indexInArray] = (long) value;
        } else if (isFloatArray()) {
            ((float[]) array)[indexInArray] = (float) value;
        } else if (isDoubleArray()) {
            ((double[]) array)[indexInArray] = value;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public double getValue(int indexInArray) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            return ((byte[]) array)[indexInArray] & 0xFF;
        } else if (isShortArray()) {
            return ((short[]) array)[indexInArray] & 0xFFFF;
        } else if (isIntArray()) {
            return ((int[]) array)[indexInArray];
        } else if (isLongArray()) {
            return ((long[]) array)[indexInArray];
        } else if (isFloatArray()) {
            return ((float[]) array)[indexInArray];
        } else if (isDoubleArray()) {
            return ((double[]) array)[indexInArray];
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public void setValue(int indexInArray, double value) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            ((byte[]) array)[indexInArray] = (byte) value;
        } else if (isShortArray()) {
            ((short[]) array)[indexInArray] = (short) value;
        } else if (isIntArray()) {
            ((int[]) array)[indexInArray] = (int) value;
        } else if (isLongArray()) {
            ((long[]) array)[indexInArray] = (long) value;
        } else if (isFloatArray()) {
            ((float[]) array)[indexInArray] = (float) value;
        } else if (isDoubleArray()) {
            ((double[]) array)[indexInArray] = value;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public long getLongValue(int blockIndex, int indexInBlock) {
        checkGetSetIndex(blockIndex, indexInBlock, 1);
        int indexInArray = blockIndex * blockLength + indexInBlock;
        if (isByteArray()) {
            return ((byte[]) array)[indexInArray] & 0xFF;
        } else if (isShortArray()) {
            return ((short[]) array)[indexInArray] & 0xFFFF;
        } else if (isIntArray()) {
            return ((int[]) array)[indexInArray];
        } else if (isLongArray()) {
            return ((long[]) array)[indexInArray];
        } else if (isFloatArray()) {
            return (long) ((float[]) array)[indexInArray];
        } else if (isDoubleArray()) {
            return (long) ((double[]) array)[indexInArray];
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public void setLongValue(int blockIndex, int indexInBlock, long value) {
        checkGetSetIndex(blockIndex, indexInBlock, 1);
        int indexInArray = blockIndex * blockLength + indexInBlock;
        if (isByteArray()) {
            ((byte[]) array)[indexInArray] = (byte) value;
        } else if (isShortArray()) {
            ((short[]) array)[indexInArray] = (short) value;
        } else if (isIntArray()) {
            ((int[]) array)[indexInArray] = (int) value;
        } else if (isLongArray()) {
            ((long[]) array)[indexInArray] = value;
        } else if (isFloatArray()) {
            ((float[]) array)[indexInArray] = (float) value;
        } else if (isDoubleArray()) {
            ((double[]) array)[indexInArray] = value;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public long getLongValue(int indexInArray) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            return ((byte[]) array)[indexInArray] & 0xFF;
        } else if (isShortArray()) {
            return ((short[]) array)[indexInArray] & 0xFFFF;
        } else if (isIntArray()) {
            return ((int[]) array)[indexInArray];
        } else if (isLongArray()) {
            return ((long[]) array)[indexInArray];
        } else if (isFloatArray()) {
            return (long) ((float[]) array)[indexInArray];
        } else if (isDoubleArray()) {
            return (long) ((double[]) array)[indexInArray];
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public void setLongValue(int indexInArray, long value) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            ((byte[]) array)[indexInArray] = (byte) value;
        } else if (isShortArray()) {
            ((short[]) array)[indexInArray] = (short) value;
        } else if (isIntArray()) {
            ((int[]) array)[indexInArray] = (int) value;
        } else if (isLongArray()) {
            ((long[]) array)[indexInArray] = value;
        } else if (isFloatArray()) {
            ((float[]) array)[indexInArray] = (float) value;
        } else if (isDoubleArray()) {
            ((double[]) array)[indexInArray] = value;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public SNumbers fillValue(double value) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            java.util.Arrays.fill((byte[]) array, (byte) value);
        } else if (isShortArray()) {
            java.util.Arrays.fill((short[]) array, (short) value);
        } else if (isIntArray()) {
            java.util.Arrays.fill((int[]) array, (int) value);
        } else if (isLongArray()) {
            java.util.Arrays.fill((long[]) array, (long) value);
        } else if (isFloatArray()) {
            java.util.Arrays.fill((float[]) array, (float) value);
        } else if (isDoubleArray()) {
            java.util.Arrays.fill((double[]) array, value);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
        return this;
    }

    public SNumbers fillLongValue(long value) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (isByteArray()) {
            java.util.Arrays.fill((byte[]) array, (byte) value);
        } else if (isShortArray()) {
            java.util.Arrays.fill((short[]) array, (short) value);
        } else if (isIntArray()) {
            java.util.Arrays.fill((int[]) array, (int) value);
        } else if (isLongArray()) {
            java.util.Arrays.fill((long[]) array, value);
        } else if (isFloatArray()) {
            java.util.Arrays.fill((float[]) array, (float) value);
        } else if (isDoubleArray()) {
            java.util.Arrays.fill((double[]) array, value);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
        return this;
    }

    public double[] getBlockDoubleValues(int blockIndex, double[] result) {
        return getBlockDoubleValues(blockIndex, 0, blockLength, result);
    }

    public double[] getBlockDoubleValues(int blockIndex, int indexInBlock, int lengthInBlock, double[] result) {
        checkGetSetIndex(blockIndex, indexInBlock, lengthInBlock);
        return getDoubleValues(blockIndex * blockLength + indexInBlock, lengthInBlock, result);
    }

    public void setBlockDoubleValues(int blockIndex, double... values) {
        setBlockDoubleValues(blockIndex, 0, blockLength, values);
    }

    public void setBlockDoubleValues(int blockIndex, int indexInBlock, int lengthInBlock, double[] values) {
        checkGetSetIndex(blockIndex, indexInBlock, lengthInBlock);
        setDoubleValues(blockIndex * blockLength + indexInBlock, lengthInBlock, values);
    }

    public Object getBlockValues(int blockIndex, Object resultJavaArray) {
        return getBlockValues(blockIndex, 0, blockLength, resultJavaArray);
    }

    public Object getBlockValues(int blockIndex, int indexInBlock, int lengthInBlock, Object resultJavaArray) {
        checkGetSetIndex(blockIndex, indexInBlock, lengthInBlock);
        return getValues(blockIndex * blockLength + indexInBlock, lengthInBlock, resultJavaArray);
    }

    public void setBlockValues(int blockIndex, Object valuesJavaArray) {
        setBlockValues(blockIndex, 0, blockLength, valuesJavaArray);
    }

    public void setBlockValues(int blockIndex, int indexInBlock, int lengthInBlock, Object valuesJavaArray) {
        checkGetSetIndex(blockIndex, indexInBlock, lengthInBlock);
        setValues(blockIndex * blockLength + indexInBlock, lengthInBlock, valuesJavaArray);
    }

    public Object getValues(int indexInArray, int length, Object resultJavaArray) {
        if (resultJavaArray == null) {
            resultJavaArray = newCompatibleJavaArray(length);
        }
        System.arraycopy(this.array, indexInArray, resultJavaArray, 0, length);
        return resultJavaArray;
    }

    public void setValues(int indexInArray, int length, Object valuesJavaArray) {
        Objects.requireNonNull(valuesJavaArray, "Null values array");
        System.arraycopy(valuesJavaArray, 0, this.array, indexInArray, length);
    }

    public double[] getDoubleValues(int indexInArray, int length, double[] result) {
        if (length < 0) {
            throw new IllegalArgumentException("Negative length = " + length);
        }
        if (result == null) {
            result = new double[length];
        }
        if (isByteArray()) {
            final byte[] array = (byte[]) this.array;
            for (int k = 0; k < length; k++) {
                result[k] = array[indexInArray++] & 0xFF;
            }
        } else if (isShortArray()) {
            final short[] array = (short[]) this.array;
            for (int k = 0; k < length; k++) {
                result[k] = array[indexInArray++] & 0xFFFF;
            }
        } else if (isIntArray()) {
            final int[] array = (int[]) this.array;
            for (int k = 0; k < length; k++) {
                result[k] = array[indexInArray++];
            }
        } else if (isLongArray()) {
            final long[] array = (long[]) this.array;
            for (int k = 0; k < length; k++) {
                result[k] = array[indexInArray++];
            }
        } else if (isFloatArray()) {
            final float[] array = (float[]) this.array;
            for (int k = 0; k < length; k++) {
                result[k] = array[indexInArray++];
            }
        } else if (isDoubleArray()) {
            System.arraycopy(this.array, indexInArray, result, 0, length);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
        return result;
    }

    public void setDoubleValues(int indexInArray, int length, double[] values) {
        Objects.requireNonNull(values, "Null values array");
        if (isByteArray()) {
            final byte[] array = (byte[]) this.array;
            for (int k = 0; k < length; k++) {
                array[indexInArray++] = (byte) values[k];
            }
        } else if (isShortArray()) {
            final short[] array = (short[]) this.array;
            for (int k = 0; k < length; k++) {
                array[indexInArray++] = (short) values[k];
            }
        } else if (isIntArray()) {
            final int[] array = (int[]) this.array;
            for (int k = 0; k < length; k++) {
                array[indexInArray++] = (int) values[k];
            }
        } else if (isLongArray()) {
            final long[] array = (long[]) this.array;
            for (int k = 0; k < length; k++) {
                array[indexInArray++] = (long) values[k];
            }
        } else if (isFloatArray()) {
            final float[] array = (float[]) this.array;
            for (int k = 0; k < length; k++) {
                array[indexInArray++] = (float) values[k];
            }
        } else if (isDoubleArray()) {
            System.arraycopy(values, 0, this.array, indexInArray, length);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /**
     * Check does container store byte elements
     *
     * @return true if container store byte elements otherwise false
     */
    @UsedForExternalCommunication
    public boolean isByteArray() {
        return array instanceof byte[];
    }

    public byte[] toByteArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isByteArray()) {
            return ((byte[]) array).clone();
        } else if (isShortArray()) {
            final short[] a = (short[]) array;
            final byte[] result = new byte[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (byte) (a[k] < 0 ? 0 : a[k] > 255 ? 255 : a[k]);
            }
            return result;
        } else if (isIntArray()) {
            final int[] a = (int[]) array;
            final byte[] result = new byte[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (byte) (a[k] < 0 ? 0 : a[k] > 255 ? 255 : a[k]);
            }
            return result;
        } else if (isLongArray()) {
            final long[] a = (long[]) array;
            final byte[] result = new byte[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (byte) (a[k] < 0 ? 0 : a[k] > 255L ? 255L : a[k]);
            }
            return result;
        } else if (isFloatArray()) {
            final float[] a = (float[]) array;
            final byte[] result = new byte[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (byte) (a[k] < 0 ? 0 : a[k] > 255 ? 255 : (int) a[k]);
            }
            return result;
        } else if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final byte[] result = new byte[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (byte) (a[k] < 0 ? 0 : a[k] > 255 ? 255 : (int) a[k]);
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /**
     * Check does container store byte elements
     *
     * @return true if container store byte elements otherwise false
     */
    @UsedForExternalCommunication
    public boolean isShortArray() {
        return array instanceof short[];
    }

    public short[] toShortArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isShortArray()) {
            return ((short[]) array).clone();
        } else if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final short[] result = new short[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (short) (a[k] & 0xFF);
            }
            return result;
        } else if (isIntArray()) {
            final int[] a = (int[]) array;
            final short[] result = new short[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (short) (a[k] < 0 ? 0 : a[k] > 0xFFFF ? 0xFFFF : a[k]);
            }
            return result;
        } else if (isLongArray()) {
            final long[] a = (long[]) array;
            final short[] result = new short[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (short) (a[k] < 0 ? 0 : a[k] > 0xFFFF ? 0xFFFF : a[k]);
            }
            return result;
        } else if (isFloatArray()) {
            final float[] a = (float[]) array;
            final short[] result = new short[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (short) (a[k] < 0 ? 0 : a[k] > 0xFFFF ? 0xFFFF : a[k]);
            }
            return result;
        } else if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final short[] result = new short[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (short) (a[k] < 0 ? 0 : a[k] > 0xFFFF ? 0xFFFF : a[k]);
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /**
     * Check does container store int elements
     *
     * @return true if container store int elements otherwise false
     */
    @UsedForExternalCommunication
    public boolean isIntArray() {
        return array instanceof int[];
    }

    public int[] toIntArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isIntArray()) {
            return ((int[]) array).clone();
        } else if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final int[] result = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFF;
            }
            return result;
        } else if (isShortArray()) {
            final short[] a = (short[]) array;
            final int[] result = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFFFF;
            }
            return result;
        } else if (isLongArray()) {
            final long[] a = (long[]) array;
            final int[] result = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] < Integer.MIN_VALUE ? Integer.MIN_VALUE :
                        a[k] > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) a[k];
            }
            return result;
        } else if (isFloatArray()) {
            final float[] a = (float[]) array;
            final int[] result = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (int) a[k];
            }
            return result;
        } else if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final int[] result = new int[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (int) a[k];
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /**
     * Check does container store long elements
     *
     * @return true if container store long elements otherwise false
     */
    @UsedForExternalCommunication
    public boolean isLongArray() {
        return array instanceof long[];
    }

    public long[] toLongArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isLongArray()) {
            return ((long[]) array).clone();
        } else if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final long[] result = new long[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFF;
            }
            return result;
        } else if (isShortArray()) {
            final short[] a = (short[]) array;
            final long[] result = new long[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFFFF;
            }
            return result;
        } else if (isIntArray()) {
            final int[] a = (int[]) array;
            final long[] result = new long[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else if (isFloatArray()) {
            final float[] a = (float[]) array;
            final long[] result = new long[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (long) a[k];
            }
            return result;
        } else if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final long[] result = new long[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (long) a[k];
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    @UsedForExternalCommunication
    public boolean isFloatArray() {
        return array instanceof float[];
    }

    public float[] toFloatArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isFloatArray()) {
            return ((float[]) array).clone();
        } else if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final float[] result = new float[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFF;
            }
            return result;
        } else if (isShortArray()) {
            final short[] a = (short[]) array;
            final float[] result = new float[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFFFF;
            }
            return result;
        } else if (isIntArray()) {
            final int[] a = (int[]) array;
            final float[] result = new float[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else if (isLongArray()) {
            final long[] a = (long[]) array;
            final float[] result = new float[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final float[] result = new float[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = (float) a[k];
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /**
     * Check does container store double elements
     *
     * @return true if container store double elements otherwise false
     */
    @UsedForExternalCommunication
    public boolean isDoubleArray() {
        return array instanceof double[];
    }

    public double[] toDoubleArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isDoubleArray()) {
            return ((double[]) array).clone();
        } else if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final double[] result = new double[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFF;
            }
            return result;
        } else if (isShortArray()) {
            final short[] a = (short[]) array;
            final double[] result = new double[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k] & 0xFFFF;
            }
            return result;
        } else if (isIntArray()) {
            final int[] a = (int[]) array;
            final double[] result = new double[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else if (isLongArray()) {
            final long[] a = (long[]) array;
            final double[] result = new double[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else if (isFloatArray()) {
            final float[] a = (float[]) array;
            final double[] result = new double[a.length];
            for (int k = 0; k < a.length; k++) {
                result[k] = a[k];
            }
            return result;
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public PNumberArray asNumberArray() {
        if (!isInitialized()) {
            return null;
        }
        if (isByteArray()) {
            return SimpleMemoryModel.asUpdatableByteArray((byte[]) array);
        } else if (isShortArray()) {
            return SimpleMemoryModel.asUpdatableShortArray((short[]) array);
        } else if (isIntArray()) {
            return SimpleMemoryModel.asUpdatableIntArray((int[]) array);
        } else if (isLongArray()) {
            return SimpleMemoryModel.asUpdatableLongArray((long[]) array);
        } else if (isFloatArray()) {
            return SimpleMemoryModel.asUpdatableFloatArray((float[]) array);
        } else if (isDoubleArray()) {
            return SimpleMemoryModel.asUpdatableDoubleArray((double[]) array);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public ByteBuffer toByteBuffer(ByteOrder order) {
        if (!isInitialized()) {
            return null;
        }
        if (isByteArray()) {
            return bytesToByteBuffer((byte[]) array, order);
        } else if (isShortArray()) {
            return shortsToByteBuffer((short[]) array, order);
        } else if (isIntArray()) {
            return intsToByteBuffer((int[]) array, order);
        } else if (isLongArray()) {
            return longsToByteBuffer((long[]) array, order);
        } else if (isFloatArray()) {
            return floatsToByteBuffer((float[]) array, order);
        } else if (isDoubleArray()) {
            return doublesToByteBuffer((double[]) array, order);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public boolean isProbableRectangularArea() {
        return isInitialized() && blockLength == 2;
    }

    //[[Repeat() IRectangularArea ==> RectangularArea;;
    //           IRange ==> Range;;
    //           IPoint ==> Point;;
    //           long ==> double;;
    //           Long ==> Double ]]
    public IPoint toIPoint() {
        final SNumbers numbers = new SNumbers();
        numbers.setToIdentical(this, false);
        // - to be on the safe side while multithreading
        if (!numbers.isInitialized()) {
            return null;
        }
        if (numbers.blockLength != 1) {
            throw new IllegalStateException("Numbers array has invalid block length " + numbers.blockLength
                    + ": 1 element per block required for conversion to IPoint");
        }
        final long[] array = numbers.toLongArray();
        assert array != null;
        if (array.length == 0) {
            throw new IllegalStateException("Numbers array is empty and cannot be converted to IPoint");
        }
        return IPoint.valueOf(array);
    }

    public IRectangularArea toIRectangularArea() {
        final SNumbers numbers = new SNumbers();
        numbers.setToIdentical(this, false);
        // - to be on the safe side while multithreading
        if (!numbers.isInitialized()) {
            return null;
        }
        if (numbers.blockLength != 2) {
            throw new IllegalStateException("Numbers array has invalid block length " + numbers.blockLength
                    + ": 2 elements per block required for conversion to IRectangularArea");
        }
        final long[] array = numbers.toLongArray();
        assert array != null;
        assert array.length % 2 == 0;
        if (array.length == 0) {
            throw new IllegalStateException("Numbers array is empty and cannot be converted to IRectangularArea");
        }
        final IRange[] ranges = new IRange[array.length >> 1];
        for (int k = 0, i = 0; i < array.length; k++) {
            final long min = array[i++];
            final long max = array[i++];
            ranges[k] = IRange.valueOf(min, max);
        }
        return IRectangularArea.valueOf(ranges);
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public Point toPoint() {
        final SNumbers numbers = new SNumbers();
        numbers.setToIdentical(this, false);
        // - to be on the safe side while multithreading
        if (!numbers.isInitialized()) {
            return null;
        }
        if (numbers.blockLength != 1) {
            throw new IllegalStateException("Numbers array has invalid block length " + numbers.blockLength
                    + ": 1 element per block required for conversion to Point");
        }
        final double[] array = numbers.toDoubleArray();
        assert array != null;
        if (array.length == 0) {
            throw new IllegalStateException("Numbers array is empty and cannot be converted to Point");
        }
        return Point.valueOf(array);
    }

    public RectangularArea toRectangularArea() {
        final SNumbers numbers = new SNumbers();
        numbers.setToIdentical(this, false);
        // - to be on the safe side while multithreading
        if (!numbers.isInitialized()) {
            return null;
        }
        if (numbers.blockLength != 2) {
            throw new IllegalStateException("Numbers array has invalid block length " + numbers.blockLength
                    + ": 2 elements per block required for conversion to RectangularArea");
        }
        final double[] array = numbers.toDoubleArray();
        assert array != null;
        assert array.length % 2 == 0;
        if (array.length == 0) {
            throw new IllegalStateException("Numbers array is empty and cannot be converted to RectangularArea");
        }
        final Range[] ranges = new Range[array.length >> 1];
        for (int k = 0, i = 0; i < array.length; k++) {
            final double min = array[i++];
            final double max = array[i++];
            ranges[k] = Range.valueOf(min, max);
        }
        return RectangularArea.valueOf(ranges);
    }

    //[[Repeat.AutoGeneratedEnd]]

    public SNumbers column(int indexInEachBlock) {
        return columnRange(indexInEachBlock, 1);
    }

    public SNumbers columnRange(int startIndexInEachBlock, int lengthInEachBlock) {
        final SNumbers result = new SNumbers();
        result.replaceColumnRange(0, this, startIndexInEachBlock, lengthInEachBlock);
        return result;
    }

    public SNumbers columnsByIndexes(int[] columnsIndexes) {
        Objects.requireNonNull(columnsIndexes, "Null columnsIndexes");
        final SNumbers result = columnsIndexes.length == 0 || !isInitialized() ?
                null :
                // - will be checked later
                zeros(elementType(), n(), columnsIndexes.length);
        columnsByIndexes(result, null, columnsIndexes);
        return result;
    }

    public Object[] allColumnsArrays() {
        return columnRangeArrays(0, blockLength);
    }

    public Object[] columnRangeArrays(int startIndexInEachBlock, int lengthInEachBlock) {
        checkStartIndexAndLenthInBlock(startIndexInEachBlock, lengthInEachBlock, true);
        final Object[] javaArrays = new Object[lengthInEachBlock];
        final int[] columnsIndexes = new int[lengthInEachBlock];
        final int n = n();
        java.util.Arrays.setAll(javaArrays, k -> newCompatibleJavaArray(n));
        java.util.Arrays.setAll(columnsIndexes, k -> startIndexInEachBlock + k);
        columnsByIndexes(null, javaArrays, columnsIndexes);
        return javaArrays;
    }

    public void columnsByIndexes(Object[] javaArraysForResultColumns, int[] columnsIndexes) {
        Objects.requireNonNull(javaArraysForResultColumns, "Null javaArraysForResultColumns");
        Objects.requireNonNull(columnsIndexes, "Null columnsIndexes");
        columnsByIndexes(null, javaArraysForResultColumns, columnsIndexes);
    }

    public void columnsByIndexes(SNumbers result, Object[] javaArraysForResultColumns, int[] columnsIndexes) {
        Objects.requireNonNull(columnsIndexes, "Null columnsIndexes");
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot extract columns from uninitialized numbers array");
        }
        final int[] indexes = columnsIndexes.clone();
        checkColumnIndexes(indexes);
        final int resultBlockLength = checkMulticolumnResult(result, indexes);
        if (resultBlockLength == 0) {
            // - nothing to do (impossible when result != null and initialized)
            return;
        }
        final Object[] columns = javaArraysForResultColumns == null ? null : javaArraysForResultColumns.clone();
        final Object[] necessaryColumns = new Object[indexes.length];
        final int[] necessaryColumnsIndexes = new int[indexes.length];
        final int necessaryColumnsCount = checkColumns(necessaryColumns, necessaryColumnsIndexes, indexes, columns);
        assert !(necessaryColumnsCount != 0 && columns == null) : "necessaryColumnsCount must be 0 if columns==null";
        if (result == null && necessaryColumnsCount == 0) {
            // - nothing to do
            return;
        }
        final int n = n();
        //[[Repeat() byte ==> short,,int,,long,,float,,double;;
        //           Byte ==> Short,,Int,,Long,,Float,,Double]]
        if (isByteArray()) {
            final byte[] a = (byte[]) array;
            final byte[][] c = new byte[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (byte[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final byte[] r = result != null ? (byte[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final byte[] r = (byte[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final byte[] r = (byte[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final byte[] r = (byte[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
        if (isShortArray()) {
            final short[] a = (short[]) array;
            final short[][] c = new short[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (short[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final short[] r = result != null ? (short[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final short[] r = (short[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final short[] r = (short[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final short[] r = (short[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        if (isIntArray()) {
            final int[] a = (int[]) array;
            final int[][] c = new int[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (int[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final int[] r = result != null ? (int[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final int[] r = (int[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final int[] r = (int[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final int[] r = (int[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        if (isLongArray()) {
            final long[] a = (long[]) array;
            final long[][] c = new long[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (long[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final long[] r = result != null ? (long[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final long[] r = (long[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final long[] r = (long[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final long[] r = (long[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        if (isFloatArray()) {
            final float[] a = (float[]) array;
            final float[][] c = new float[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (float[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final float[] r = result != null ? (float[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final float[] r = (float[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final float[] r = (float[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final float[] r = (float[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        if (isDoubleArray()) {
            final double[] a = (double[]) array;
            final double[][] c = new double[necessaryColumnsCount][];
            java.util.Arrays.setAll(c, k -> (double[]) necessaryColumns[k]);
            if (indexes.length == 1) {
//                System.out.println("BRANCH 1, " + (result == null) + "/" + necessaryColumnsCount);
                assert result != null || java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final double[] r = result != null ? (double[]) result.array : c[0];
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength + indexes[0];
                    final int toInThis = fromInThis + (to - from) * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc, i++) {
                        r[i] = a[j];
                    }
                });
                if (result != null && necessaryColumnsCount > 0) {
                    assert necessaryColumnsCount == 1;
                    System.arraycopy(r, 0, c[0], 0, n);
                }
            } else if (necessaryColumnsCount == 0) {
//                System.out.println("BRANCH 2");
                final double[] r = (double[]) result.array;
                assert r != null : "checking, that nothing to do, has failed";
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    final int from = block << 8;
                    final int to = (int) Math.min((long) from + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = from * blockLength;
                    final int toInThis = to * blockLength;
                    for (int j = fromInThis, i = from * resultBlockLength; j < toInThis; j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                    }
                });
            } else if (result == null) {
//                System.out.println("BRANCH 3");
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else if (necessaryColumnsCount == indexes.length) {
//                System.out.println("BRANCH 4");
                assert java.util.Arrays.equals(indexes, necessaryColumnsIndexes);
                final double[] r = (double[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            r[i++] = c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            } else {
//                System.out.println("BRANCH 5");
                final double[] r = (double[]) result.array;
                IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                    int k = block << 8;
                    final int to = (int) Math.min((long) k + 256, n);
                    final int jInc = this.blockLength;
                    final int fromInThis = k * blockLength;
                    final int count = necessaryColumnsCount;
                    for (int j = fromInThis, i = k * resultBlockLength; k < to; k++, j += jInc) {
                        for (int index : indexes) {
                            r[i++] = a[j + index];
                        }
                        for (int cIndex = 0; cIndex < count; cIndex++) {
                            c[cIndex][k] = a[j + necessaryColumnsIndexes[cIndex]];
                        }
                    }
                });
            }
            return;
        }
        //[[Repeat.AutoGeneratedEnd]]
        throw new AssertionError("Unsupported Java array type: " + array);
    }

    public SNumbers blockRange(int startBlockIndex, int numberOfBlocks) {
        final SNumbers result = new SNumbers();
        result.replaceBlockRange(0, this, startBlockIndex, numberOfBlocks);
        return result;
    }

    public SNumbers selectBlockSet(BitArray selector) {
        Objects.requireNonNull(selector, "Null selector");
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot select blocks in uninitialized numbers array");
        }
        final int n = n();
        if (selector.length() < n) {
            throw new IllegalArgumentException("Not enough length of bit array: " + selector.length() + "<" + n);
        }
        if (selector.length() > n) {
            selector = (BitArray) selector.subArray(0, n);
        }
        final SNumbers result = zeros(elementType(), (int) Arrays.cardinality(selector), blockLength);
        if (isByteArray()) {
            final byte[] r = (byte[]) result.array;
            final byte[] a = (byte[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else if (isShortArray()) {
            final short[] r = (short[]) result.array;
            final short[] a = (short[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else if (isIntArray()) {
            final int[] r = (int[]) result.array;
            final int[] a = (int[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else if (isLongArray()) {
            final long[] r = (long[]) result.array;
            final long[] a = (long[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else if (isFloatArray()) {
            final float[] r = (float[]) result.array;
            final float[] a = (float[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else if (isDoubleArray()) {
            final double[] r = (double[]) result.array;
            final double[] a = (double[]) array;
            for (int k = 0, disp = 0, dispResult = 0; k < n; k++) {
                if (selector.getBit(k)) {
                    for (int dispTo = disp + blockLength; disp < dispTo; disp++, dispResult++) {
                        r[dispResult] = a[disp];
                    }
                } else {
                    disp += blockLength;
                }
            }
        } else {
            throw new AssertionError("Unsupported Java array type: " + this);
        }
        return result;
    }

    public SNumbers setPrecision(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (elementType == elementType()) {
            return this;
        } else if (elementType == byte.class) {
            return setToArray(toByteArray(), blockLength, false);
        } else if (elementType == short.class) {
            return setToArray(toShortArray(), blockLength, false);
        } else if (elementType == int.class) {
            return setToArray(toIntArray(), blockLength, false);
        } else if (elementType == long.class) {
            return setToArray(toLongArray(), blockLength, false);
        } else if (elementType == float.class) {
            return setToArray(toFloatArray(), blockLength, false);
        } else if (elementType == double.class) {
            return setToArray(toDoubleArray(), blockLength, false);
        } else {
            throw new IllegalArgumentException("The element type " + elementType + " is not supported");
        }
    }

    public SNumbers toPrecision(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (elementType == byte.class) {
            return new SNumbers().setToArray(toByteArray(), blockLength, false);
        } else if (elementType == short.class) {
            return new SNumbers().setToArray(toShortArray(), blockLength, false);
        } else if (elementType == int.class) {
            return new SNumbers().setToArray(toIntArray(), blockLength, false);
        } else if (elementType == long.class) {
            return new SNumbers().setToArray(toLongArray(), blockLength, false);
        } else if (elementType == float.class) {
            return new SNumbers().setToArray(toFloatArray(), blockLength, false);
        } else if (elementType == double.class) {
            return new SNumbers().setToArray(toDoubleArray(), blockLength, false);
        } else {
            throw new IllegalArgumentException("The element type " + elementType + " is not supported");
        }
    }

    public boolean isFloatingPoint() {
        return isFloatArray() || isDoubleArray();
    }

    public boolean isInteger() {
        return !isFloatingPoint();
    }

    public boolean isInteger(boolean includingLong64) {
        return isByteArray() || isShortArray() || isIntArray() || (includingLong64 && isLongArray());
    }

    @Override
    public DataType type() {
        return DataType.NUMBERS;
    }

    /*Repeat() min ==> max,,maxAbs;;
               maxAbs(Bytes|Shorts) ==> max$1,,... */

    public double min() {
        return min(false);
    }

    public double min(boolean onlyFinite) {
        return minInRange(0, n(), 0, blockLength, onlyFinite);
    }

    public double minInColumnRange(int indexInBlock, int lengthInBlock, boolean onlyFinite) {
        return minInRange(0, n(), indexInBlock, lengthInBlock, onlyFinite);
    }

    public double minInRange(
            int blockIndex,
            int numberOfBlocks,
            int indexInBlock,
            int lengthInBlock,
            boolean onlyFinite) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array has no initialized data");
        }
        if (blockIndex < 0) {
            throw new IllegalArgumentException("Negative block index: " + blockIndex);
        }
        if (blockIndex + numberOfBlocks > n()) {
            throw new IllegalArgumentException("Start block index and number of blocks = "
                    + blockIndex + " and " + blockLength
                    + " are out of range 0..n-1 = 0.." + (n() - 1));
        }
        checkStartIndexAndLenthInBlock(indexInBlock, lengthInBlock, true);
        if (isByteArray()) {
            return minBytesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isShortArray()) {
            return minShortsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isIntArray()) {
            return minIntsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isLongArray()) {
            return minLongsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isFloatArray()) {
            return onlyFinite ?
                    minFiniteFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    minFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isDoubleArray()) {
            return onlyFinite ?
                    minFiniteDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    minDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    public double max() {
        return max(false);
    }

    public double max(boolean onlyFinite) {
        return maxInRange(0, n(), 0, blockLength, onlyFinite);
    }

    public double maxInColumnRange(int indexInBlock, int lengthInBlock, boolean onlyFinite) {
        return maxInRange(0, n(), indexInBlock, lengthInBlock, onlyFinite);
    }

    public double maxInRange(
            int blockIndex,
            int numberOfBlocks,
            int indexInBlock,
            int lengthInBlock,
            boolean onlyFinite) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array has no initialized data");
        }
        if (blockIndex < 0) {
            throw new IllegalArgumentException("Negative block index: " + blockIndex);
        }
        if (blockIndex + numberOfBlocks > n()) {
            throw new IllegalArgumentException("Start block index and number of blocks = "
                    + blockIndex + " and " + blockLength
                    + " are out of range 0..n-1 = 0.." + (n() - 1));
        }
        checkStartIndexAndLenthInBlock(indexInBlock, lengthInBlock, true);
        if (isByteArray()) {
            return maxBytesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isShortArray()) {
            return maxShortsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isIntArray()) {
            return maxIntsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isLongArray()) {
            return maxLongsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isFloatArray()) {
            return onlyFinite ?
                    maxFiniteFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    maxFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isDoubleArray()) {
            return onlyFinite ?
                    maxFiniteDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    maxDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    public double maxAbs() {
        return maxAbs(false);
    }

    public double maxAbs(boolean onlyFinite) {
        return maxAbsInRange(0, n(), 0, blockLength, onlyFinite);
    }

    public double maxAbsInColumnRange(int indexInBlock, int lengthInBlock, boolean onlyFinite) {
        return maxAbsInRange(0, n(), indexInBlock, lengthInBlock, onlyFinite);
    }

    public double maxAbsInRange(
            int blockIndex,
            int numberOfBlocks,
            int indexInBlock,
            int lengthInBlock,
            boolean onlyFinite) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array has no initialized data");
        }
        if (blockIndex < 0) {
            throw new IllegalArgumentException("Negative block index: " + blockIndex);
        }
        if (blockIndex + numberOfBlocks > n()) {
            throw new IllegalArgumentException("Start block index and number of blocks = "
                    + blockIndex + " and " + blockLength
                    + " are out of range 0..n-1 = 0.." + (n() - 1));
        }
        checkStartIndexAndLenthInBlock(indexInBlock, lengthInBlock, true);
        if (isByteArray()) {
            return maxBytesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isShortArray()) {
            return maxShortsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isIntArray()) {
            return maxAbsIntsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isLongArray()) {
            return maxAbsLongsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isFloatArray()) {
            return onlyFinite ?
                    maxAbsFiniteFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    maxAbsFloatsParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else if (isDoubleArray()) {
            return onlyFinite ?
                    maxAbsFiniteDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock) :
                    maxAbsDoublesParallel(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        } else {
            throw new AssertionError("Unsupported Java array type: " + array);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    @Override
    public void setTo(Data other, boolean cloneData) {
        if (other instanceof SNumbers) {
            setToIdentical((SNumbers) other, cloneData);
        } else if (other instanceof SScalar) {
            setArray(((SScalar) other).toDoubles());
            // - sets initialized to true
        } else {
            throw new IllegalArgumentException("Cannot assign " + other.getClass() + " to " + getClass());
        }
    }

    @Override
    public SNumbers exchange(Data other) {
        Objects.requireNonNull(other, "Null other objects");
        if (!(other instanceof SNumbers)) {
            throw new IllegalArgumentException("Cannot exchange with another data type: " + other.getClass());
        }
        return exchange((SNumbers) other);
    }

    public SNumbers exchange(SNumbers other) {
        Objects.requireNonNull(other, "Null other numbers");
        final long tempFlags = this.flags;
        final Object tempArray = this.array;
        final int tempBlockLength = this.blockLength;
        this.flags = other.flags;
        this.array = other.array;
        this.blockLength = other.blockLength;
        other.flags = tempFlags;
        other.array = tempArray;
        other.blockLength = tempBlockLength;
        return this;
    }

    public SNumbers setTo(SNumbers dataNumbers) {
        return setToIdentical(dataNumbers, true);
    }

    public SNumbers setToColumnRange(
            final SNumbers otherNumbers,
            final int startIndexInEachBlockOfOther,
            final int lengthInEachBlock) {
        replaceColumnRange(
                0, otherNumbers, startIndexInEachBlockOfOther, lengthInEachBlock, true);
        return this;
    }

    public SNumbers setToSingleBlock(SNumbers dataNumbers, int blockIndex) {
        Objects.requireNonNull(dataNumbers, "Null dataNumbers");
        if (!dataNumbers.isInitialized()) {
            throw new IllegalArgumentException("Cannot extract block from uninitialized numbers array");
        }
        if (blockIndex < 0 || blockIndex >= dataNumbers.n()) {
            throw new IndexOutOfBoundsException("Index of the block = " + blockIndex
                    + " is out of range 0..n()-1 = 0.." + (dataNumbers.n() - 1));
        }
        this.blockLength = dataNumbers.blockLength;
        this.array = Array.newInstance(dataNumbers.elementType(), dataNumbers.blockLength);
        System.arraycopy(
                dataNumbers.array, blockIndex * dataNumbers.blockLength,
                this.array, 0,
                dataNumbers.blockLength);
        setInitializedAndResetFlags(true);
        return this;
    }

    public void replaceColumnRange(
            final int startIndexInThis,
            SNumbers otherNumbers,
            final int startIndexInOther,
            final int lengthInEachBlock) {
        replaceColumnRange(startIndexInThis, otherNumbers, startIndexInOther, lengthInEachBlock, false);
    }

    public void replaceBlockRange(
            final int startBlockIndexInThis,
            final SNumbers otherNumbers,
            final int startBlockIndexInOther,
            final int numberOfReplacedBlocks) {
        if (startBlockIndexInThis < 0) {
            throw new IllegalArgumentException("Negative start block index in this array: " + startBlockIndexInThis);
        }
        Objects.requireNonNull(otherNumbers, "Null otherNumbers");
        if (!otherNumbers.isInitialized()) {
            throw new IllegalArgumentException("Cannot extract blocks from uninitialized numbers array");
        }
        if (numberOfReplacedBlocks < 0) {
            throw new IndexOutOfBoundsException("Negative number of blocks: " + numberOfReplacedBlocks);
        }
        if (startBlockIndexInOther < 0
                || startBlockIndexInOther + numberOfReplacedBlocks > otherNumbers.n()) {
            throw new IndexOutOfBoundsException("Start block index and number of blocks = "
                    + startBlockIndexInOther + " and " + numberOfReplacedBlocks
                    + " (range " + startBlockIndexInOther + ".."
                    + (startBlockIndexInOther + numberOfReplacedBlocks - 1) + ")"
                    + " are out of range 0..n-1 = 0.." + (otherNumbers.n() - 1));
        }
        if (!isInitialized()) {
            if (startBlockIndexInThis == 0) {
                setToZeros(otherNumbers.elementType(), numberOfReplacedBlocks, otherNumbers.blockLength);
                // ...and continue normal processing
            } else {
                throw new IllegalStateException("Cannot replace blocks in uninitialized numbers array");
            }
        }
        final Class<?> elementType = elementType();
        if (elementType != otherNumbers.elementType()) {
            throw new IllegalArgumentException("Element type mismatch: cannot assign "
                    + otherNumbers.elementType() + "[] to " + elementType + "[]");
        }
        if (blockLength != otherNumbers.blockLength) {
            throw new IllegalArgumentException("Block lengths mismatch: this array contains "
                    + blockLength + " columns, but the other contains " + otherNumbers.blockLength + " columns");
        }
        assert numberOfReplacedBlocks <= otherNumbers.n();
        final int newN = Math.max(this.n(), startBlockIndexInThis + numberOfReplacedBlocks);
        if (newN > this.n()) {
            // Increasing size of the current array:
            final SNumbers result = zeros(elementType, newN, blockLength);
            // ...copying existing content to new array:
            result.replaceBlockRange(
                    0, this, 0, this.n());
            this.setToIdentical(result, false);
            // ...and continue replacing
        }
        // Normal situation: need to override part of elements
        assert numberOfReplacedBlocks <= this.n();
        assert isInitialized();
        assert blockLength == otherNumbers.blockLength;
        System.arraycopy(otherNumbers.array, startBlockIndexInOther * blockLength,
                this.array, startBlockIndexInThis * blockLength,
                numberOfReplacedBlocks * blockLength);
    }

    public SNumbers setTo(byte[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(short[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(int[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(long[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(float[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(double[] javaArray, int blockLength) {
        return setToArray(javaArray, blockLength);
    }

    public SNumbers setTo(ByteBuffer byteBuffer, Class<?> elementType, int blockLength) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        Objects.requireNonNull(elementType, "Null elementType");
        if (elementType == byte.class) {
            return setTo(byteBufferToBytes(byteBuffer), blockLength);
        } else if (elementType == short.class) {
            return setTo(byteBufferToShorts(byteBuffer), blockLength);
        } else if (elementType == int.class) {
            return setTo(byteBufferToInts(byteBuffer), blockLength);
        } else if (elementType == long.class) {
            return setTo(byteBufferToLongs(byteBuffer), blockLength);
        } else if (elementType == float.class) {
            return setTo(byteBufferToFloats(byteBuffer), blockLength);
        } else if (elementType == double.class) {
            return setTo(byteBufferToDoubles(byteBuffer), blockLength);
        } else {
            throw new IllegalArgumentException("The element type is not byte, short, int, long, float "
                    + "or double (it is " + elementType + ")");
        }
    }

    public SNumbers setTo(PNumberArray array, int blockLength) {
        Objects.requireNonNull(array, "Null array");
        if (!isSupportedJavaElementType(array.elementType())) {
            throw new IllegalArgumentException("The element type of passed array is not supported (it is "
                    + array + ")");
        }
        return setToArray(array.toJavaArray(), blockLength, false);
    }

    public SNumbers setTo(IPoint point) {
        Objects.requireNonNull(point, "Null point");
        return setToArray(point.coordinates(), 1, false);
    }

    public SNumbers setTo(IRectangularArea area) {
        Objects.requireNonNull(area, "Null area");
        final long[] array = new long[2 * area.coordCount()];
        for (int k = 0, i = 0; i < array.length; k++) {
            array[i++] = area.min(k);
            array[i++] = area.max(k);
        }
        return setToArray(array, 2, false);
    }

    public SNumbers setToOrRemove(IRectangularArea area) {
        if (area == null) {
            remove();
            return this;
        }
        return setTo(area);
    }

    public SNumbers setTo(Point point) {
        Objects.requireNonNull(point, "Null point");
        return setToArray(point.coordinates(), 1, false);
    }

    public SNumbers setTo(RectangularArea area) {
        Objects.requireNonNull(area, "Null area");
        final double[] array = new double[2 * area.coordCount()];
        for (int k = 0, i = 0; i < array.length; k++) {
            array[i++] = area.min(k);
            array[i++] = area.max(k);
        }
        return setToArray(array, 2, false);
    }

    public SNumbers setToOrRemove(RectangularArea area) {
        if (area == null) {
            remove();
            return this;
        }
        return setTo(area);
    }

    public SNumbers setTo(Contours contours) {
        Objects.requireNonNull(contours, "Null contours");
        setToArray(contours.serialize(), Contours.BLOCK_LENGTH, false);
        // - cloning is not necessary, because contours.serialize() performs cloning
        return this;
    }

    public SNumbers setTo(Collection<?> numbers, int blockLength) {
        Objects.requireNonNull(numbers, "Null numbers");
        final int size = numbers.size();
        if (size == 0) {
            return setToZeros(float.class, 0, blockLength);
        }
        if (size % blockLength != 0) {
            throw new IllegalArgumentException(
                    "List size " + size + " is not divisible by block length " + blockLength);
        }
        final Object array = convertToArray(numbers);
        return setToArray(array, blockLength);
    }

    public SNumbers setToArray(Object javaArray, int blockLength) {
        return setToArray(javaArray, blockLength, true);
    }

    // This method is convenient for usage outside Java
    public SNumbers setToZeros(String elementTypeName, int n, int blockLength) {
        return setToZeros(elementType(elementTypeName), n, blockLength);
    }

    public SNumbers setToZeros(Class<?> elementType, int n, int blockLength) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (!isSupportedJavaElementType(elementType)) {
            throw new IllegalArgumentException("The element type is not byte, short, int, long, float "
                    + "or double (it is " + elementType + ")");
        }
        checkDimensions(n, blockLength);
        final Object javaArray = Array.newInstance(elementType, n * blockLength);
        // - Java fills array by zeros
        setArray(javaArray);
        // - sets initialized to true
        setBlockLength(blockLength);
        return this;
    }

    public Object newCompatibleJavaArray(int arrayLength) {
        return java.lang.reflect.Array.newInstance(elementType(), arrayLength);
    }

    public SNumbers requireInitialized(String name) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array \"" + name + "\" has no initialized data");
        }
        return this;
    }

    public SNumbers requireBlockLengthOne(String name) {
        return requireBlockLength(1, name);
    }

    public SNumbers requireBlockLength(int blockLength, String name) {
        if (isInitialized() && this.blockLength != blockLength) {
            throw new IllegalStateException("Numbers array \""
                    + name + "\" has invalid block length " + this.blockLength
                    + " (" + blockLength + " elements per block required)");
        }
        return this;
    }

    public boolean checkStartIndexAndLenthInBlock(
            int startIndexInEachBlock,
            int lengthInEachBlock,
            boolean strictlyInside) {
        if (!isInitialized()) {
            throw new IllegalArgumentException("Cannot process uninitialized numbers array");
        }
        if (lengthInEachBlock < 0) {
            throw new IndexOutOfBoundsException("Negative length inside the block: " + lengthInEachBlock);
        }
        if (startIndexInEachBlock < 0) {
            throw new IndexOutOfBoundsException("Negative start index inside the block: " + startIndexInEachBlock);
        }
        if (startIndexInEachBlock + lengthInEachBlock > this.blockLength) {
            if (strictlyInside) {
                throw new IndexOutOfBoundsException("Start index and length inside the block = "
                        + startIndexInEachBlock + " and " + lengthInEachBlock
                        + " (range " + startIndexInEachBlock + ".."
                        + (startIndexInEachBlock + lengthInEachBlock - 1) + ")"
                        + " are out of range 0..blockLength-1 = 0.." + (this.blockLength - 1));
            } else {
                return false;
            }
        }
        return true;
    }

    public Formatter getFormatter(FormattingType formattingType, Locale locale) {
        Objects.requireNonNull(formattingType, "Null formatter type");
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot call getFormatter(): numbers array is not initialized");
        }
        return new Formatter(formattingType, locale);
    }

    public String toFormattedString(
            FormattingType formattingType,
            Locale locale,
            String format,
            String elementsDelimiter) {
        return getFormatter(formattingType, locale)
                .setElementsFormat(format)
                .setElementsDelimiter(elementsDelimiter).format();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Returns some string representation of this numbers array.
     * <p>Note: its exact format is not specified and may vary in future versions.
     *
     * @param includeNumbersInResults if <code>true</code>,
     *                                the result will contain numeric representation of all numbers.
     * @return some string representation of this numbers array.
     */
    public String toString(boolean includeNumbersInResults) {
        if (!isInitialized()) {
            return super.toString();
        }
        if (includeNumbersInResults) {
            final StringBuilder sb = new StringBuilder();
            if (isFloatingPoint()) {
                final double[] a = toDoubleArray();
                if (a == null) {
                    // - very unlikely case of parallel modification
                    return super.toString();
                }
                for (int k = 0; k < a.length; k++) {
                    if (k > 0) {
                        sb.append(k % blockLength == 0 ? "\n" : ", ");
                    }
                    sb.append(a[k]);
                }
            } else {
                final long[] a = toLongArray();
                if (a == null) {
                    // - very unlikely case of parallel modification
                    return super.toString();
                }
                for (int k = 0; k < a.length; k++) {
                    if (k > 0) {
                        sb.append(k % blockLength == 0 ? "\n" : ", ");
                    }
                    sb.append(a[k]);
                }
            }
            return sb.toString();
        } else {
            return super.toString() + " " + elementType() + "[" + blockLength + "*" + n() + "]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SNumbers numbers = (SNumbers) o;
        final int arrayLength = getArrayLength();
        return blockLength == numbers.blockLength
                && arrayLength == numbers.getArrayLength()
                && (array == numbers.array || (array != null
                && JArrays.arrayEquals(array, 0, numbers.array, 0, arrayLength)));
    }

    @Override
    public int hashCode() {
        final int hash;
        if (isByteArray()) {
            hash = java.util.Arrays.hashCode((byte[]) array);
        } else if (isShortArray()) {
            hash = java.util.Arrays.hashCode((short[]) array);
        } else if (isIntArray()) {
            hash = java.util.Arrays.hashCode((int[]) array);
        } else if (isLongArray()) {
            hash = java.util.Arrays.hashCode((long[]) array);
        } else if (isFloatArray()) {
            hash = java.util.Arrays.hashCode((float[]) array);
        } else if (isDoubleArray()) {
            hash = java.util.Arrays.hashCode((double[]) array);
        } else {
            hash = 0;
        }
        return ((isInitialized() ? elementType().hashCode() : 1352) ^ hash) * 31 + (blockLength ^ 621);
    }

    @Override
    public SNumbers clone() {
        return new SNumbers().setTo(this);
    }

    public static SNumbers valueOf(IPoint point) {
        return new SNumbers().setTo(point);
    }

    public static SNumbers valueOf(IRectangularArea area) {
        return new SNumbers().setTo(area);
    }

    public static SNumbers valueOf(Point point) {
        return new SNumbers().setTo(point);
    }

    public static SNumbers valueOf(RectangularArea area) {
        return new SNumbers().setTo(area);
    }

    public static SNumbers valueOf(Contours contours) {
        return new SNumbers().setTo(contours);
    }

    public static SNumbers valueOf(Collection<?> numbers, int blockLength) {
        return new SNumbers().setTo(numbers, blockLength);
    }

    public static SNumbers valueOfArray(Object javaArray) {
        return valueOfArray(javaArray, 1);
    }

    public static SNumbers valueOfArray(Object javaArray, int blockLength) {
        return new SNumbers().setToArray(javaArray, blockLength);
    }

    public static SNumbers arrayAsNumbers(Object javaArray, int blockLength) {
        return new SNumbers().setToArray(javaArray, blockLength, false);
    }

    // This method is convenient for usage outside Java
    public static SNumbers zeros(String elementTypeName, int n, int blockLength) {
        return new SNumbers().setToZeros(elementTypeName, n, blockLength);
    }

    public static SNumbers zeros(Class<?> elementType, int n, int blockLength) {
        return new SNumbers().setToZeros(elementType, n, blockLength);
    }

    public static void checkDimensions(long n, long blockLength) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative n (number of blocks");
        }
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Block length " + blockLength + " is not positive");
        }
        if (n > Integer.MAX_VALUE || blockLength > Integer.MAX_VALUE || n * blockLength > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large required array: more that 2^31-1 elements");
        }
    }

    public static ByteBuffer bytesToByteBuffer(byte[] data) {
        return bytesToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer bytesToByteBuffer(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] byteBufferToBytes(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        byte[] result = new byte[byteBuffer.capacity()];
        byteBuffer.rewind();
        byteBuffer.get(result);
        return result;
    }

    //[[Repeat() short ==> int,,long,,float,,double;;
    //           Short ==> Int,,Long,,Float,,Double;;
    //           2     ==> 4,,8,,4,,8]]
    public static ByteBuffer shortsToByteBuffer(short[] data) {
        return shortsToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer shortsToByteBuffer(short[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2 * data.length);
        byteBuffer.order(order);
        byteBuffer.asShortBuffer().put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] shortsToBytes(short[] data) {
        return shortsToBytes(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static byte[] shortsToBytes(short[] data, ByteOrder order) {
        final ByteBuffer byteBuffer = shortsToByteBuffer(data, order);
        byte[] result = new byte[2 * data.length];
        byteBuffer.get(result);
        return result;
    }

    public static short[] byteBufferToShorts(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        if (byteBuffer.capacity() % 2 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + byteBuffer.capacity() + " is not divisible by 2");
        }
        short[] result = new short[byteBuffer.capacity() / 2];
        byteBuffer.rewind();
        byteBuffer.asShortBuffer().get(result);
        return result;
    }

    public static short[] bytesToShorts(byte[] data) {
        return bytesToShorts(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static short[] bytesToShorts(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        if (data.length % 2 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + data.length + " is not divisible by 2");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        return byteBufferToShorts(byteBuffer);
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public static ByteBuffer intsToByteBuffer(int[] data) {
        return intsToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer intsToByteBuffer(int[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * data.length);
        byteBuffer.order(order);
        byteBuffer.asIntBuffer().put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] intsToBytes(int[] data) {
        return intsToBytes(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static byte[] intsToBytes(int[] data, ByteOrder order) {
        final ByteBuffer byteBuffer = intsToByteBuffer(data, order);
        byte[] result = new byte[4 * data.length];
        byteBuffer.get(result);
        return result;
    }

    public static int[] byteBufferToInts(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        if (byteBuffer.capacity() % 4 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + byteBuffer.capacity() + " is not divisible by 4");
        }
        int[] result = new int[byteBuffer.capacity() / 4];
        byteBuffer.rewind();
        byteBuffer.asIntBuffer().get(result);
        return result;
    }

    public static int[] bytesToInts(byte[] data) {
        return bytesToInts(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static int[] bytesToInts(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        if (data.length % 4 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + data.length + " is not divisible by 4");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        return byteBufferToInts(byteBuffer);
    }

    public static ByteBuffer longsToByteBuffer(long[] data) {
        return longsToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer longsToByteBuffer(long[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8 * data.length);
        byteBuffer.order(order);
        byteBuffer.asLongBuffer().put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] longsToBytes(long[] data) {
        return longsToBytes(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static byte[] longsToBytes(long[] data, ByteOrder order) {
        final ByteBuffer byteBuffer = longsToByteBuffer(data, order);
        byte[] result = new byte[8 * data.length];
        byteBuffer.get(result);
        return result;
    }

    public static long[] byteBufferToLongs(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        if (byteBuffer.capacity() % 8 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + byteBuffer.capacity() + " is not divisible by 8");
        }
        long[] result = new long[byteBuffer.capacity() / 8];
        byteBuffer.rewind();
        byteBuffer.asLongBuffer().get(result);
        return result;
    }

    public static long[] bytesToLongs(byte[] data) {
        return bytesToLongs(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static long[] bytesToLongs(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        if (data.length % 8 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + data.length + " is not divisible by 8");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        return byteBufferToLongs(byteBuffer);
    }

    public static ByteBuffer floatsToByteBuffer(float[] data) {
        return floatsToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer floatsToByteBuffer(float[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * data.length);
        byteBuffer.order(order);
        byteBuffer.asFloatBuffer().put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] floatsToBytes(float[] data) {
        return floatsToBytes(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static byte[] floatsToBytes(float[] data, ByteOrder order) {
        final ByteBuffer byteBuffer = floatsToByteBuffer(data, order);
        byte[] result = new byte[4 * data.length];
        byteBuffer.get(result);
        return result;
    }

    public static float[] byteBufferToFloats(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        if (byteBuffer.capacity() % 4 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + byteBuffer.capacity() + " is not divisible by 4");
        }
        float[] result = new float[byteBuffer.capacity() / 4];
        byteBuffer.rewind();
        byteBuffer.asFloatBuffer().get(result);
        return result;
    }

    public static float[] bytesToFloats(byte[] data) {
        return bytesToFloats(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static float[] bytesToFloats(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        if (data.length % 4 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + data.length + " is not divisible by 4");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        return byteBufferToFloats(byteBuffer);
    }

    public static ByteBuffer doublesToByteBuffer(double[] data) {
        return doublesToByteBuffer(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static ByteBuffer doublesToByteBuffer(double[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8 * data.length);
        byteBuffer.order(order);
        byteBuffer.asDoubleBuffer().put(data);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public static byte[] doublesToBytes(double[] data) {
        return doublesToBytes(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static byte[] doublesToBytes(double[] data, ByteOrder order) {
        final ByteBuffer byteBuffer = doublesToByteBuffer(data, order);
        byte[] result = new byte[8 * data.length];
        byteBuffer.get(result);
        return result;
    }

    public static double[] byteBufferToDoubles(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer, "Null byteBuffer");
        if (byteBuffer.capacity() % 8 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + byteBuffer.capacity() + " is not divisible by 8");
        }
        double[] result = new double[byteBuffer.capacity() / 8];
        byteBuffer.rewind();
        byteBuffer.asDoubleBuffer().get(result);
        return result;
    }

    public static double[] bytesToDoubles(byte[] data) {
        return bytesToDoubles(data, ByteOrder.BIG_ENDIAN);
        // BIG_ENDIAN is default in Java
    }

    public static double[] bytesToDoubles(byte[] data, ByteOrder order) {
        Objects.requireNonNull(data, "Null data array");
        Objects.requireNonNull(order, "Null byte order");
        if (data.length % 8 != 0) {
            throw new IllegalArgumentException("Illegal data: number of bytes "
                    + data.length + " is not divisible by 8");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
        byteBuffer.order(order);
        byteBuffer.put(data);
        return byteBufferToDoubles(byteBuffer);
    }

    //[[Repeat.AutoGeneratedEnd]]

    @Override
    protected void freeResources() {
        array = null;
    }

    @UsedForExternalCommunication
    private void setArray(Object javaArray) {
        Objects.requireNonNull(javaArray, "Null java array");
        if (isSupportedJavaArray(javaArray)) {
            this.array = javaArray;
        } else {
            throw new IllegalArgumentException("The passed java-array argument is not byte[], short[], int[], "
                    + "long[], float[] or double[] (it is " + javaArray.getClass().getSimpleName() + ")");
        }
        setInitializedAndResetFlags(true);
        // - no sense to keep uninitialized state
    }

    private SNumbers setToArray(Object javaArray, int blockLength, boolean doClone) {
        Objects.requireNonNull(javaArray, "Null java array");
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Block length " + blockLength + " is not positive");
        }
        if (!isSupportedJavaArray(javaArray)) {
            throw new IllegalArgumentException("The passed java-array argument is not byte[], short[], int[], "
                    + "long[], float[] or double[] (it is " + javaArray.getClass().getSimpleName() + ")");
        }
        if (Array.getLength(javaArray) % blockLength != 0) {
            throw new IllegalArgumentException("Java array length " + Array.getLength(javaArray)
                    + " is not divisible by block length " + blockLength);
        }
        setArray(doClone ? cloneJavaArray(javaArray) : javaArray);
        // - sets initialized to true
        setBlockLength(blockLength);
        return this;
    }

    private SNumbers setToIdentical(SNumbers dataNumbers, boolean doClone) {
        Objects.requireNonNull(dataNumbers, "Null dataNumbers");
        this.array = doClone && dataNumbers.isInitialized() ?
                cloneJavaArray(dataNumbers.array) :
                dataNumbers.array;
        this.blockLength = dataNumbers.blockLength;
        this.flags = dataNumbers.flags;
        setInitialized(dataNumbers.isInitialized());
        return this;
    }

    private void checkGetSetIndex(int blockIndex, int indexInBlock, int lengthInBlock) {
        if (!isInitialized()) {
            throw new IllegalStateException("Numbers array is not initialized");
        }
        if (indexInBlock < 0 || indexInBlock >= blockLength) {
            throw new IndexOutOfBoundsException("Index in block = " + indexInBlock
                    + " is out of range 0..blockLength-1 = 0.." + (blockLength - 1));
        }
        if (lengthInBlock < 0) {
            throw new IllegalArgumentException("Negative length in block = " + lengthInBlock);
        }
        if (indexInBlock + lengthInBlock > blockLength) {
            throw new IndexOutOfBoundsException("Index in block + length in block = "
                    + (indexInBlock + lengthInBlock)
                    + " is out of range 0..blockLength = 0.." + blockLength);
        }
        if (blockIndex < 0 || (long) blockIndex * (long) blockLength >= Array.getLength(array)) {
            throw new IndexOutOfBoundsException("Index of the block = " + blockIndex
                    + " is out of range 0..n()-1 = 0.." + (n() - 1));
        }
    }

    private void checkColumnIndexes(int[] indexes) {
        for (int j = 0; j < indexes.length; j++) {
            if (indexes[j] < 0 || indexes[j] >= blockLength) {
                throw new IndexOutOfBoundsException("Index " + indexes[j] + " of the column #" + j
                        + " is out of range 0..blockLength-1=" + (blockLength - 1));
            }
        }
    }

    private int checkMulticolumnResult(SNumbers result, int[] indexes) {
        if (result != null) {
            if (!result.isInitialized()) {
                if (indexes.length > 0) {
                    // else nothing to return: stay non-initialized
                    result.setToZeros(elementType(), n(), indexes.length);
                }
            } else {
                if (elementType() != result.elementType()) {
                    throw new IllegalArgumentException("Element type mismatch: cannot assign "
                            + elementType() + "[] to " + result.elementType() + "[]");
                }
                if (n() != result.n()) {
                    throw new IllegalArgumentException("Array lengths mismatch: this array contains "
                            + n() + " blocks, but the result contains " + result.n() + " blocks");
                }
                if (result.blockLength != indexes.length) {
                    // - we require exact equality to simplify usage and copying to
                    // javaArraysForResultColumns[0] in a case of only 1 column
                    throw new IllegalArgumentException("Number of columns in the result "
                            + result.blockLength + " is not equal to number of column indexes " + indexes.length);
                }
            }
        }
        return indexes.length;
    }

    private int checkColumns(Object[] resultColumns, int[] resultIndexes, int[] indexes, Object[] columns) {
        final Class<?> elementType = elementType();
        final int n = n();
        int count = 0;
        if (columns != null) {
            for (int j = 0; j < indexes.length; j++) {
                if (j < columns.length && columns[j] != null) {
                    final Class<?> componentType = columns[j].getClass().getComponentType();
                    if (componentType == null) {
                        throw new IllegalArgumentException("javaArraysForResultColumns[" + j
                                + "] is not a Java array");
                    } else if (componentType != elementType) {
                        throw new IllegalArgumentException("javaArraysForResultColumns[" + j + "] is "
                                + componentType.getCanonicalName() + "[] array instead of required "
                                + elementType.getCanonicalName() + "[] array");
                    }
                    if (Array.getLength(columns[j]) < n) {
                        throw new IllegalArgumentException("Too short passed array javaArraysForResultColumns["
                                + j + "]: only " + Array.getLength(columns[j]) + " elements, but at least "
                                + n + " elements required");
                    }
                    resultColumns[count] = columns[j];
                    resultIndexes[count] = indexes[j];
                    count++;
                }
            }
        }
        return count;
    }

    private void replaceColumnRange(
            final int startIndexInThis,
            SNumbers otherNumbers,
            final int startIndexInOther,
            final int lengthInEachBlock,
            boolean reinitialize) {
        if (startIndexInThis < 0) {
            throw new IllegalArgumentException("Negative start index in this array: " + startIndexInOther);
        }
        if (!otherNumbers.checkStartIndexAndLenthInBlock(startIndexInOther, lengthInEachBlock, false)) {
            // Increasing size of the other array:
            final int newBlockLength = startIndexInOther + lengthInEachBlock;
            final SNumbers expanded = zeros(otherNumbers.elementType(), otherNumbers.n(), newBlockLength);
            // ...copying existing content to new array:
            expanded.replaceColumnRange(0, otherNumbers, 0, otherNumbers.blockLength);
            otherNumbers = expanded;
            // ...and continue replacing
        }
        if (reinitialize || !isInitialized()) {
            if (startIndexInThis == 0) {
                setToZeros(otherNumbers.elementType(), otherNumbers.n(), lengthInEachBlock);
                // ...and continue normal processing
            } else {
                throw new IllegalStateException("Cannot replace columns from non-zero index " + startIndexInOther
                        + " in uninitialized numbers array");
            }
        }
        final Class<?> elementType = elementType();
        if (elementType != otherNumbers.elementType()) {
            throw new IllegalArgumentException("Element type mismatch: cannot assign "
                    + otherNumbers.elementType() + "[] to " + elementType + "[]");
        }
        final int n = n();
        if (n != otherNumbers.n()) {
            throw new IllegalArgumentException("Array lengths mismatch: this array contains "
                    + n + " blocks, but the other contains " + otherNumbers.n() + " blocks");
        }
        assert lengthInEachBlock <= otherNumbers.blockLength : "Error in checkStartIndexAndLenthInBlock";
        final int newBlockLength = Math.max(this.blockLength, startIndexInThis + lengthInEachBlock);
        if (newBlockLength > this.blockLength) {
            // Increasing size of the current array:
            final SNumbers result = zeros(elementType, n, newBlockLength);
            // ...copying existing content to new array:
            result.replaceColumnRange(
                    0, this, 0, blockLength);
            this.setToIdentical(result, false);
            // ...and continue replacing
        }
        // Normal situation: need to override part of elements
        assert lengthInEachBlock <= this.blockLength;
        assert isInitialized();
        final int otherBlockLength = otherNumbers.blockLength;
        if (lengthInEachBlock == this.blockLength && lengthInEachBlock == otherNumbers.blockLength) {
            System.arraycopy(otherNumbers.array, 0, this.array, 0, getArrayLength());
        } else {
            if (lengthInEachBlock == 1) {
                //[[Repeat() byte ==> short,,int,,long,,float,,double;;
                //           Byte ==> Short,,Int,,Long,,Float,,Double]]
                if (otherNumbers.isByteArray()) {
                    final byte[] result = (byte[]) this.array;
                    final byte[] a = (byte[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if (otherNumbers.isShortArray()) {
                    final short[] result = (short[]) this.array;
                    final short[] a = (short[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isIntArray()) {
                    final int[] result = (int[]) this.array;
                    final int[] a = (int[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isLongArray()) {
                    final long[] result = (long[]) this.array;
                    final long[] a = (long[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isFloatArray()) {
                    final float[] result = (float[]) this.array;
                    final float[] a = (float[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isDoubleArray()) {
                    final double[] result = (double[]) this.array;
                    final double[] a = (double[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength;
                        final int jInc = otherBlockLength;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            result[i] = a[j];
                        }
                    });
                    this.array = result;
                    return;
                }
                //[[Repeat.AutoGeneratedEnd]]
                throw new AssertionError("Unsupported Java array type: " + otherNumbers);

            } else {
                //[[Repeat() byte ==> short,,int,,long,,float,,double;;
                //           Byte ==> Short,,Int,,Long,,Float,,Double]]
                if (otherNumbers.isByteArray()) {
                    final byte[] result = (byte[]) this.array;
                    final byte[] a = (byte[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                if (otherNumbers.isShortArray()) {
                    final short[] result = (short[]) this.array;
                    final short[] a = (short[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isIntArray()) {
                    final int[] result = (int[]) this.array;
                    final int[] a = (int[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isLongArray()) {
                    final long[] result = (long[]) this.array;
                    final long[] a = (long[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isFloatArray()) {
                    final float[] result = (float[]) this.array;
                    final float[] a = (float[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                if (otherNumbers.isDoubleArray()) {
                    final double[] result = (double[]) this.array;
                    final double[] a = (double[]) otherNumbers.array;
                    IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
                        final int from = block << 8, to = (int) Math.min((long) from + 256, n);
                        final int iInc = this.blockLength - lengthInEachBlock;
                        final int jInc = otherBlockLength - lengthInEachBlock;
                        final int length = lengthInEachBlock;
                        final int fromInThis = startIndexInThis + from * blockLength;
                        final int toInThis = fromInThis + (to - from) * blockLength;
                        final int fromInOther = startIndexInOther + from * otherBlockLength;
                        for (int i = fromInThis, j = fromInOther; i < toInThis; i += iInc, j += jInc) {
                            for (int iTo = i + length; i < iTo; ) {
                                result[i++] = a[j++];
                            }
                        }
                    });
                    this.array = result;
                    return;
                }
                //[[Repeat.AutoGeneratedEnd]]
                throw new AssertionError("Unsupported Java array type: " + otherNumbers);
            }
        }
    }

    private static final int PARALLEL_LOG = 8;
    private static final int PARALLEL_BUNDLE = 1 << PARALLEL_LOG;

    /*Repeat() byte ==> short,,int,,long,,float,,double;;
               Bytes ==> Shorts,,Ints,,Longs,,Floats,,Doubles;;
               (\/\*~~.*?)(\r(?!\n)|\n|\r\n) ==> $1$2,,$2,,...;;
               (\}\s*~~\*\/) ==> $1,,},,...;;
               (\s*\&) 0xFF ==> $1 0xFFFF,, ,,...;;
               (int\s+m) ==> $1,,$1,,long m,,double m,,...;;
               (int\s+value) ==> $1,,$1,,long value,,double value,,...;;
               (Integer\.MIN_VALUE) ==> $1,,$1,,Long.MIN_VALUE,,Double.NEGATIVE_INFINITY,,...;;
               (Integer\.MAX_VALUE) ==> $1,,$1,,Long.MAX_VALUE,,Double.POSITIVE_INFINITY,,... */

    private double minBytesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minBytes(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minBytes(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minBytes(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minBytes(int blockIndex, int numberOfBlocks) {
        final byte[] array = (byte[]) this.array;
        int min = Integer.MAX_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i] & 0xFF;
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minBytes(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final byte[] array = (byte[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int min = Integer.MAX_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp] & 0xFF;
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxBytesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxBytes(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxBytes(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxBytes(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxBytes(int blockIndex, int numberOfBlocks) {
        final byte[] array = (byte[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i] & 0xFF;
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxBytes(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final byte[] array = (byte[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp] & 0xFF;
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }
    /*~~ for auto-generated code: actual for int, long, float, double
    private double maxAbsBytesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsBytes(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsBytes(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsBytes(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsBytes(int blockIndex, int numberOfBlocks) {
        final byte[] array = (byte[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = Math.abs(array[i] & 0xFF);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsBytes(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final byte[] array = (byte[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = Math.abs(array[disp] & 0xFF);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }
    ~~*/

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private double minShortsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minShorts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minShorts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minShorts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minShorts(int blockIndex, int numberOfBlocks) {
        final short[] array = (short[]) this.array;
        int min = Integer.MAX_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i] & 0xFFFF;
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minShorts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final short[] array = (short[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int min = Integer.MAX_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp] & 0xFFFF;
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxShortsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxShorts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxShorts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxShorts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxShorts(int blockIndex, int numberOfBlocks) {
        final short[] array = (short[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i] & 0xFFFF;
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxShorts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final short[] array = (short[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp] & 0xFFFF;
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }
    /*~~ for auto-generated code: actual for int, long, float, double
    private double maxAbsShortsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsShorts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsShorts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsShorts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsShorts(int blockIndex, int numberOfBlocks) {
        final short[] array = (short[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = Math.abs(array[i] & 0xFFFF);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsShorts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final short[] array = (short[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = Math.abs(array[disp] & 0xFFFF);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }
    ~~*/

    private double minIntsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minInts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minInts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minInts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minInts(int blockIndex, int numberOfBlocks) {
        final int[] array = (int[]) this.array;
        int min = Integer.MAX_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i];
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minInts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final int[] array = (int[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int min = Integer.MAX_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp];
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxIntsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxInts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxInts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxInts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxInts(int blockIndex, int numberOfBlocks) {
        final int[] array = (int[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = array[i];
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxInts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final int[] array = (int[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = array[disp];
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsIntsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsInts(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsInts(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsInts(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsInts(int blockIndex, int numberOfBlocks) {
        final int[] array = (int[]) this.array;
        int max = Integer.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final int value = Math.abs(array[i]);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsInts(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final int[] array = (int[]) this.array;
        final int increment = blockLength - lengthInBlock;
        int max = Integer.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final int value = Math.abs(array[disp]);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double minLongsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minLongs(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minLongs(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minLongs(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minLongs(int blockIndex, int numberOfBlocks) {
        final long[] array = (long[]) this.array;
        long min = Long.MAX_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final long value = array[i];
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minLongs(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final long[] array = (long[]) this.array;
        final int increment = blockLength - lengthInBlock;
        long min = Long.MAX_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final long value = array[disp];
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxLongsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxLongs(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxLongs(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxLongs(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxLongs(int blockIndex, int numberOfBlocks) {
        final long[] array = (long[]) this.array;
        long max = Long.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final long value = array[i];
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxLongs(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final long[] array = (long[]) this.array;
        final int increment = blockLength - lengthInBlock;
        long max = Long.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final long value = array[disp];
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsLongsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsLongs(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsLongs(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsLongs(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsLongs(int blockIndex, int numberOfBlocks) {
        final long[] array = (long[]) this.array;
        long max = Long.MIN_VALUE;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final long value = Math.abs(array[i]);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsLongs(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final long[] array = (long[]) this.array;
        final int increment = blockLength - lengthInBlock;
        long max = Long.MIN_VALUE;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final long value = Math.abs(array[disp]);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double minFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double min = Double.POSITIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double min = Double.POSITIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = Math.abs(array[i]);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = Math.abs(array[disp]);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double minDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double min = Double.POSITIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double min = Double.POSITIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = Math.abs(array[i]);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxAbsDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = Math.abs(array[disp]);
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() float ==> double;;
               Floats ==> Doubles */

    private double minFiniteFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minFiniteFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFiniteFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFiniteFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minFiniteFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double min = Double.POSITIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (Double.isFinite(value) && value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minFiniteFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double min = Double.POSITIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (Double.isFinite(value) && value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxFiniteFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxFiniteFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFiniteFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFiniteFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxFiniteFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (Double.isFinite(value) && value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxFiniteFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (Double.isFinite(value) && value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsFiniteFloatsParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsFiniteFloats(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFiniteFloats(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFiniteFloats(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsFiniteFloats(int blockIndex, int numberOfBlocks) {
        final float[] array = (float[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = Math.abs(array[i]);
            if (value <= Double.MAX_VALUE && value > max) {
                // "value <= Double.MAX_VALUE": see implementation of Double.isFinite method
                max = value;
            }
        }
        return max;
    }

    private double maxAbsFiniteFloats(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final float[] array = (float[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = Math.abs(array[disp]);
                if (value <= Double.MAX_VALUE && value > max) {
                    // "value <= Double.MAX_VALUE": see implementation of Double.isFinite method
                    max = value;
                }
            }
        }
        return max;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private double minFiniteDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return minFiniteDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFiniteDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return minFiniteDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).min().orElse(Double.POSITIVE_INFINITY);
    }

    private double minFiniteDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double min = Double.POSITIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (Double.isFinite(value) && value < min) {
                min = value;
            }
        }
        return min;
    }

    private double minFiniteDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double min = Double.POSITIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (Double.isFinite(value) && value < min) {
                    min = value;
                }
            }
        }
        return min;
    }

    private double maxFiniteDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxFiniteDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFiniteDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxFiniteDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxFiniteDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = array[i];
            if (Double.isFinite(value) && value > max) {
                max = value;
            }
        }
        return max;
    }

    private double maxFiniteDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = array[disp];
                if (Double.isFinite(value) && value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    private double maxAbsFiniteDoublesParallel(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        if (numberOfBlocks < PARALLEL_BUNDLE) {
            return maxAbsFiniteDoubles(blockIndex, numberOfBlocks, indexInBlock, lengthInBlock);
        }
        return IntStream.range(0, (numberOfBlocks + PARALLEL_BUNDLE - 1) >>> PARALLEL_LOG).parallel().mapToDouble(
                indexInBlock == 0 && lengthInBlock == blockLength ?
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFiniteDoubles(from + blockIndex, size);
                        } :
                        block -> {
                            int from = block << PARALLEL_LOG;
                            int size = Math.min(PARALLEL_BUNDLE, numberOfBlocks - from);
                            return maxAbsFiniteDoubles(from + blockIndex, size, indexInBlock, lengthInBlock);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
    }

    private double maxAbsFiniteDoubles(int blockIndex, int numberOfBlocks) {
        final double[] array = (double[]) this.array;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = blockIndex * blockLength, to = i + numberOfBlocks * blockLength; i < to; i++) {
            final double value = Math.abs(array[i]);
            if (value <= Double.MAX_VALUE && value > max) {
                // "value <= Double.MAX_VALUE": see implementation of Double.isFinite method
                max = value;
            }
        }
        return max;
    }

    private double maxAbsFiniteDoubles(int blockIndex, int numberOfBlocks, int indexInBlock, int lengthInBlock) {
        final double[] array = (double[]) this.array;
        final int increment = blockLength - lengthInBlock;
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0, disp = blockIndex * blockLength + indexInBlock; k < numberOfBlocks; k++, disp += increment) {
            for (int dispTo = disp + lengthInBlock; disp < dispTo; disp++) {
                final double value = Math.abs(array[disp]);
                if (value <= Double.MAX_VALUE && value > max) {
                    // "value <= Double.MAX_VALUE": see implementation of Double.isFinite method
                    max = value;
                }
            }
        }
        return max;
    }

    /*Repeat.AutoGeneratedEnd*/

    private static final char[][] SPACES = new char[64][];

    static {
        for (int k = 0; k < SPACES.length; k++) {
            SPACES[k] = new char[k];
            java.util.Arrays.fill(SPACES[k], ' ');
        }
    }

    private static char[] spaces(int numberOfSpaces) {
        if (numberOfSpaces < SPACES.length) {
            return SPACES[numberOfSpaces];
        } else {
            char[] spaces = new char[numberOfSpaces];
            java.util.Arrays.fill(spaces, ' ');
            return spaces;
        }
    }

    private abstract static class ElementFormatter {
        abstract int length();

        abstract void append(String s);

        abstract void insertSpaces(int position, int numberOfSpaces);

        abstract void formatLineIndex(int index);

        abstract void formatElement(int disp);

        abstract String result();

        void formatLineIndex(int index, String delimiter) {
            formatLineIndex(index);
            append(delimiter);
        }

        void formatLineIndex(int index, String delimiter, int minimalLength) {
            final int p = length();
            formatLineIndex(index);
            final int q = length() - p;
            if (q < minimalLength) {
                insertSpaces(p, minimalLength - q);
            }
            append(delimiter);
        }

        void formatElement(int disp, String delimiter) {
            formatElement(disp);
            append(delimiter);
        }

        void formatElement(int disp, String delimiter, int minimalLength) {
            final int p = length();
            formatElement(disp);
            final int q = length() - p;
            if (q < minimalLength) {
                insertSpaces(p, minimalLength - q);
            }
            append(delimiter);
        }
    }

    private abstract class SimpleElementFormatter extends ElementFormatter {
        final StringBuilder out;

        private SimpleElementFormatter(int estimatedCapacity) {
            this.out = new StringBuilder(estimatedCapacity);
        }

        @Override
        int length() {
            return out.length();
        }

        @Override
        void append(String s) {
            out.append(s);
        }

        @Override
        void insertSpaces(int position, int numberOfSpaces) {
            out.insert(position, spaces(numberOfSpaces));
        }

        @Override
        void formatLineIndex(int index) {
            out.append(index);
        }

        @Override
        abstract void formatElement(int disp);

        @Override
        String result() {
            return out.toString();
        }
    }

    private abstract class PrintfElementFormatter extends ElementFormatter {
        final StringBuilder out;
        final java.util.Formatter utilFormatter;
        final String elementsFormat;
        private final String lineIndexFormat;

        private PrintfElementFormatter(Formatter formatter, int estimatedCapacity) {
            this.out = new StringBuilder(estimatedCapacity);
            this.utilFormatter = formatter.locale == null ?
                    new java.util.Formatter(out) :
                    new java.util.Formatter(out, formatter.locale);
            this.elementsFormat = formatter.elementsFormat;
            this.lineIndexFormat = formatter.lineIndexFormat;
        }

        @Override
        int length() {
            return out.length();
        }

        @Override
        void append(String s) {
            out.append(s);
        }

        @Override
        void insertSpaces(int position, int numberOfSpaces) {
            out.insert(position, spaces(numberOfSpaces));
        }

        @Override
        void formatLineIndex(int index) {
            utilFormatter.format(lineIndexFormat, index);
        }

        @Override
        abstract void formatElement(int disp);

        @Override
        String result() {
            return out.toString();
        }
    }

    private abstract class DecimalElementFormatter extends ElementFormatter {
        final StringBuffer out;
        final DecimalFormat lineIndexFormat;
        final DecimalFormat elementsFormat;
        final FieldPosition dummy;

        private DecimalElementFormatter(Formatter formatter, int estimatedCapacity) {
            this.out = new StringBuffer(estimatedCapacity);
            if (formatter.locale == null) {
                this.lineIndexFormat = new DecimalFormat(formatter.lineIndexFormat);
                this.elementsFormat = new DecimalFormat(formatter.elementsFormat);
            } else {
                this.lineIndexFormat = new DecimalFormat(
                        formatter.lineIndexFormat, DecimalFormatSymbols.getInstance(formatter.locale));
                this.elementsFormat = new DecimalFormat(
                        formatter.elementsFormat, DecimalFormatSymbols.getInstance(formatter.locale));
            }
            this.dummy = new FieldPosition(0);
            // - we don't use it
        }

        @Override
        int length() {
            return out.length();
        }

        @Override
        void append(String s) {
            out.append(s);
        }

        @Override
        void insertSpaces(int position, int numberOfSpaces) {
            out.insert(position, spaces(numberOfSpaces));
        }

        @Override
        void formatLineIndex(int index) {
            lineIndexFormat.format(index, out, dummy);
        }

        @Override
        abstract void formatElement(int disp);

        @Override
        String result() {
            return out.toString();
        }
    }

    /*Repeat() byte ==> short,,int,,long,,float,,double;;
               Bytes ==> Shorts,,Ints,,Longs,,Floats,,Doubles;;
               (\s*\&) 0xFF ==> $1 0xFFFF,, ,,... */

    private final class SimpleBytesElementFormatter extends SimpleElementFormatter {
        private final byte[] array;

        private SimpleBytesElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (byte[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp] & 0xFF);
        }
    }

    private final class PrintfBytesElementFormatter extends PrintfElementFormatter {
        private final byte[] array;

        private PrintfBytesElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (byte[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp] & 0xFF);
        }
    }

    private final class DecimalBytesElementFormatter extends DecimalElementFormatter {
        private final byte[] array;

        private DecimalBytesElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (byte[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp] & 0xFF, out, dummy);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private final class SimpleShortsElementFormatter extends SimpleElementFormatter {
        private final short[] array;

        private SimpleShortsElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (short[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp] & 0xFFFF);
        }
    }

    private final class PrintfShortsElementFormatter extends PrintfElementFormatter {
        private final short[] array;

        private PrintfShortsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (short[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp] & 0xFFFF);
        }
    }

    private final class DecimalShortsElementFormatter extends DecimalElementFormatter {
        private final short[] array;

        private DecimalShortsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (short[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp] & 0xFFFF, out, dummy);
        }
    }

    private final class SimpleIntsElementFormatter extends SimpleElementFormatter {
        private final int[] array;

        private SimpleIntsElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (int[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp]);
        }
    }

    private final class PrintfIntsElementFormatter extends PrintfElementFormatter {
        private final int[] array;

        private PrintfIntsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (int[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp]);
        }
    }

    private final class DecimalIntsElementFormatter extends DecimalElementFormatter {
        private final int[] array;

        private DecimalIntsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (int[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp], out, dummy);
        }
    }

    private final class SimpleLongsElementFormatter extends SimpleElementFormatter {
        private final long[] array;

        private SimpleLongsElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (long[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp]);
        }
    }

    private final class PrintfLongsElementFormatter extends PrintfElementFormatter {
        private final long[] array;

        private PrintfLongsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (long[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp]);
        }
    }

    private final class DecimalLongsElementFormatter extends DecimalElementFormatter {
        private final long[] array;

        private DecimalLongsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (long[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp], out, dummy);
        }
    }

    private final class SimpleFloatsElementFormatter extends SimpleElementFormatter {
        private final float[] array;

        private SimpleFloatsElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (float[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp]);
        }
    }

    private final class PrintfFloatsElementFormatter extends PrintfElementFormatter {
        private final float[] array;

        private PrintfFloatsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (float[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp]);
        }
    }

    private final class DecimalFloatsElementFormatter extends DecimalElementFormatter {
        private final float[] array;

        private DecimalFloatsElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (float[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp], out, dummy);
        }
    }

    private final class SimpleDoublesElementFormatter extends SimpleElementFormatter {
        private final double[] array;

        private SimpleDoublesElementFormatter(int estimatedCapacity) {
            super(estimatedCapacity);
            this.array = (double[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            out.append(array[disp]);
        }
    }

    private final class PrintfDoublesElementFormatter extends PrintfElementFormatter {
        private final double[] array;

        private PrintfDoublesElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (double[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            utilFormatter.format(elementsFormat, array[disp]);
        }
    }

    private final class DecimalDoublesElementFormatter extends DecimalElementFormatter {
        private final double[] array;

        private DecimalDoublesElementFormatter(Formatter formatter, int estimatedCapacity) {
            super(formatter, estimatedCapacity);
            this.array = (double[]) arrayReference();
        }

        @Override
        void formatElement(int disp) {
            elementsFormat.format(array[disp], out, dummy);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() float ==> double;;
               Floats ==> Doubles */

    private final class SimpleForIntegersFloatsWrapper extends ElementFormatter {
        private final ElementFormatter parent;
        private final float[] array;

        private SimpleForIntegersFloatsWrapper(ElementFormatter parent) {
            this.parent = parent;
            this.array = (float[]) arrayReference();
        }

        @Override
        int length() {
            return parent.length();
        }

        @Override
        void append(String s) {
            parent.append(s);
        }

        @Override
        void insertSpaces(int position, int numberOfSpaces) {
            parent.insertSpaces(position, numberOfSpaces);
        }

        @Override
        void formatLineIndex(int index) {
            parent.append(String.valueOf(index));
        }

        @Override
        void formatElement(int disp) {
            final double value = array[disp];
            final long longValue = (long) value;
            if (value == longValue) {
                parent.append(String.valueOf(longValue));
            } else {
                parent.formatElement(disp);
            }
        }

        @Override
        String result() {
            return parent.result();
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private final class SimpleForIntegersDoublesWrapper extends ElementFormatter {
        private final ElementFormatter parent;
        private final double[] array;

        private SimpleForIntegersDoublesWrapper(ElementFormatter parent) {
            this.parent = parent;
            this.array = (double[]) arrayReference();
        }

        @Override
        int length() {
            return parent.length();
        }

        @Override
        void append(String s) {
            parent.append(s);
        }

        @Override
        void insertSpaces(int position, int numberOfSpaces) {
            parent.insertSpaces(position, numberOfSpaces);
        }

        @Override
        void formatLineIndex(int index) {
            parent.append(String.valueOf(index));
        }

        @Override
        void formatElement(int disp) {
            final double value = array[disp];
            final long longValue = (long) value;
            if (value == longValue) {
                parent.append(String.valueOf(longValue));
            } else {
                parent.formatElement(disp);
            }
        }

        @Override
        String result() {
            return parent.result();
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private static int recommendedNumberOfRanges(int n) {
        final int cpuCount = Arrays.SystemSettings.cpuCount();
        return cpuCount == 1 ? 1 : Math.min(n, 4 * cpuCount);
        // - splitting into more than cpuCount ranges provides better performance
    }

    private static int[] splitToRanges(int n, int numberOfRanges) {
        final int[] result = new int[numberOfRanges + 1];
        result[0] = 0;
        result[numberOfRanges] = n;
        for (int k = 1; k < numberOfRanges; k++) {
            result[k] = (int) ((double) n * (double) k / (double) numberOfRanges);
        }
        return result;
    }

    private static Object cloneJavaArray(Object value) {
        if (value == null) {
            return null;
        }
        final int length = java.lang.reflect.Array.getLength(value);
        final Object result = java.lang.reflect.Array.newInstance(value.getClass().getComponentType(), length);
        System.arraycopy(value, 0, result, 0, length);
        return result;
    }

//    public static void main(String[] args) {
//        final int k = Integer.MAX_VALUE - 255;
//        final int n = Integer.MAX_VALUE - 25;
//        final int to = (int) Math.min((long) k + 256, n);
//        System.out.println(to);
//        final DecimalFormat format = new DecimalFormat("###,###,###.0", DecimalFormatSymbols.getInstance(Locale.US));
//        System.out.println(format.format(33333333333333.4, new StringBuffer(), new FieldPosition(0)));
//        System.out.println(format.format(333333333, new StringBuffer(), new FieldPosition(0)));
//    }
}
