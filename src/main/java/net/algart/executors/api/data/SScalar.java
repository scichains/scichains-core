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

package net.algart.executors.api.data;

import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import net.algart.external.UsedForExternalCommunication;
import net.algart.json.Jsons;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple scalar: some string, probably representing number, boolean or other simple value.
 *
 * <p>Note: this class <b>does not</b> contain methods, providing access to file system or other unsafe
 * resources. It is important while using inside user-defined scripts.
 */
public final class SScalar extends Data {
    private String value = null;
    // - note: null means "non-initialized"

    @UsedForExternalCommunication
    public SScalar() {
    }

    public SScalar(String value) {
        setValue(value);
    }

    /**
     * Returns the value of this scalar.
     *
     * <p>Always returns  <code>null</code> for non-{@link #isInitialized() initialized} object
     * and non-<code>null</code> for initialized one.
     *
     * @return the stored value.
     */
    @UsedForExternalCommunication
    public String getValue() {
        return value;
    }

    public String getValueOrDefault(String defaultValue) {
        return isInitialized() ? value : defaultValue;
    }

    public SScalar setTo(SScalar scalar) {
        Objects.requireNonNull(scalar, "Null scalar");
        this.value = scalar.value;
        this.flags = scalar.flags;
        setInitialized(scalar.isInitialized());
        return this;
    }

    public SScalar setTo(String value) {
        setValue(value);
        return this;
    }

    public SScalar setTo(boolean value) {
        setValue(String.valueOf(value));
        return this;
    }

    public SScalar setTo(int value) {
        setValue(String.valueOf(value));
        return this;
    }

    public SScalar setTo(long value) {
        setValue(String.valueOf(value));
        return this;
    }

    public SScalar setTo(double value) {
        if (value == (long) value) {
            setTo((long) value);
        } else {
            setValue(String.valueOf(value));
        }
        return this;
    }

    //[[Repeat() int\[\] ==> long[],,float[],,double[] ]]
    public SScalar setTo(int[] values) {
        return setTo(values, 0);
    }

    public SScalar setTo(int[] values, int blockLength) {
        Objects.requireNonNull(values, "Null values array");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(blockLength > 0 && i % blockLength == 0 ? "\n" : ", ");
            }
            sb.append(values[i]);
        }
        setValue(sb.toString());
        return this;
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public SScalar setTo(long[] values) {
        return setTo(values, 0);
    }

    public SScalar setTo(long[] values, int blockLength) {
        Objects.requireNonNull(values, "Null values array");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(blockLength > 0 && i % blockLength == 0 ? "\n" : ", ");
            }
            sb.append(values[i]);
        }
        setValue(sb.toString());
        return this;
    }

    public SScalar setTo(float[] values) {
        return setTo(values, 0);
    }

    public SScalar setTo(float[] values, int blockLength) {
        Objects.requireNonNull(values, "Null values array");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(blockLength > 0 && i % blockLength == 0 ? "\n" : ", ");
            }
            sb.append(values[i]);
        }
        setValue(sb.toString());
        return this;
    }

    public SScalar setTo(double[] values) {
        return setTo(values, 0);
    }

    public SScalar setTo(double[] values, int blockLength) {
        Objects.requireNonNull(values, "Null values array");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(blockLength > 0 && i % blockLength == 0 ? "\n" : ", ");
            }
            sb.append(values[i]);
        }
        setValue(sb.toString());
        return this;
    }

    //[[Repeat.AutoGeneratedEnd]]

    public SScalar setTo(Collection<?> values) {
        Objects.requireNonNull(values, "Null values collection");
        final String delimiter = String.format("%n");
        setValue(values.stream().map(String::valueOf).collect(Collectors.joining(delimiter)));
        return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public SScalar setTo(Optional<?> optional) {
        Objects.requireNonNull(optional, "Null optional value");
        if (optional.isEmpty()) {
            setToNull();
        } else {
            Object subValue = optional.get();
            setTo(subValue);
            // - for Optional<Optional<X>> it will be not be recognized as X
        }
        return this;
    }

    /**
     * Makes this scalar non-initialized (null).
     *
     * @return a reference to this object.
     */
    public SScalar setToNull() {
        setValue(null);
        return this;
    }

    public SScalar setTo(Object value) {
        if (value == null) {
            setToNull();
        } else if (value instanceof Data data) {
            setTo(data);
        } else if (value instanceof int[] v) {
            setTo(v);
        } else if (value instanceof long[] v) {
            setTo(v);
        } else if (value instanceof float[] v) {
            setTo(v);
        } else if (value instanceof double[] v) {
            setTo(v);
        } else if (value instanceof Collection<?> collection) {
            setTo(collection);
        } else if (value instanceof Optional<?> optional) {
            setTo(optional);
        } else {
            setValue(String.valueOf(value));
            // - note: setTo(double), setTo(long), setTo(int) are equivalent to this code
        }
        return this;
    }

    @Override
    public DataType type() {
        return DataType.SCALAR;
    }

    @Override
    public void setTo(Data other, boolean cloneData) {
        if (!(other instanceof SScalar)) {
            throw new IllegalArgumentException("Cannot assign " + other.getClass() + " to " + getClass());
        }
        setTo((SScalar) other);
    }

    @Override
    public SScalar exchange(Data other) {
        Objects.requireNonNull(other, "Null other objects");
        if (!(other instanceof final SScalar otherScalar)) {
            throw new IllegalArgumentException("Cannot exchange with another data type: " + other.getClass());
        }
        final long tempFlags = this.flags;
        final String tempValue = this.value;
        this.flags = otherScalar.flags;
        this.value = otherScalar.value;
        otherScalar.flags = tempFlags;
        otherScalar.value = tempValue;
        return this;
    }

    public boolean toBoolean() {
        if (value == null) {
            throw new IllegalStateException("Non-initialized scalar cannot be converted to boolean");
        }
        return toCommonBoolean(value);
    }

    public boolean toBoolean(boolean defaultValue) {
        return toCommonBoolean(value, defaultValue);
    }

    /**
     * Returns int value, stored in this scalar.
     *
     * <p>Note: this scalar also <b>may</b> contain floating-point double value, if it is actually 32-bit integer,
     * for example: <code>2.0</code>, <code>15623.0</code> etc.</p>
     *
     * @return int value, stored in this scalar.
     * @throws NumberFormatException if this scalar cannot be parsed as int value or actually integer double value.
     */
    public int toInt() {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to int");
        }
        final long value = Math.round(Double.parseDouble(this.value));
        if (value != (int) value) {
            throw new NumberFormatException("Scalar contain too large value for 32-bit int type: "
                    + Double.parseDouble(this.value));
        }
        return (int) value;
    }

    public Integer toIntOrNull() {
        return value == null ? null : toInt();
    }

    public int toIntOrDefault(int defaultValue) {
        return value == null ? defaultValue : toInt();
    }

    /**
     * Returns long value, stored in this scalar.
     *
     * <p>Note: unlike {@link #toInt()}, actually integer floating-point value like
     * <code>2.0</code>, <code>15623.0</code> is <b>not</b> allowed.</p>
     *
     * @return long value, stored in this scalar.
     * @throws NumberFormatException if this scalar cannot be parsed as long value by <code>Long.parseLong</code>.
     */
    public long toLong() {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to long");
        }
        return Long.parseLong(this.value);
    }

    public Long toLongOrNull() {
        return value == null ? null : toLong();
    }

    public long toLongOrDefault(long defaultValue) {
        return value == null ? defaultValue : toLong();
    }

    public double toDouble() {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to double");
        }
        return Double.parseDouble(value);
    }

    public Double toDoubleOrNull() {
        return value == null ? null : toDouble();
    }

    public double toDoubleOrDefault(double defaultValue) {
        return value == null ? defaultValue : toDouble();
    }

    //[[Repeat() int\[ ==> long[,,double[;;
    //           (Integer|Int) ==> Long,,Double]]
    public int[] toInts() throws NumberFormatException {
        return toInts(0);
    }

    public int[] toInts(int minRequiredNumberOfDoubles) throws NumberFormatException, IllegalStateException {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to int[]");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new int[0];
        }
        final int[] result = Stream.of(trimmed.split("[,;\\s]+"))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
        if (result.length < minRequiredNumberOfDoubles) {
            throw new IllegalStateException("Too little values in the scalar: only " + result.length
                    + " when " + minRequiredNumberOfDoubles + " are required");
        }
        return result;
    }

    //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
    public long[] toLongs() throws NumberFormatException {
        return toLongs(0);
    }

    public long[] toLongs(int minRequiredNumberOfDoubles) throws NumberFormatException, IllegalStateException {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to long[]");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new long[0];
        }
        final long[] result = Stream.of(trimmed.split("[,;\\s]+"))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .toArray();
        if (result.length < minRequiredNumberOfDoubles) {
            throw new IllegalStateException("Too little values in the scalar: only " + result.length
                    + " when " + minRequiredNumberOfDoubles + " are required");
        }
        return result;
    }

    public double[] toDoubles() throws NumberFormatException {
        return toDoubles(0);
    }

    public double[] toDoubles(int minRequiredNumberOfDoubles) throws NumberFormatException, IllegalStateException {
        if (value == null) {
            throw new NumberFormatException("Non-initialized scalar cannot be converted to double[]");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new double[0];
        }
        final double[] result = Stream.of(trimmed.split("[,;\\s]+"))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();
        if (result.length < minRequiredNumberOfDoubles) {
            throw new IllegalStateException("Too little values in the scalar: only " + result.length
                    + " when " + minRequiredNumberOfDoubles + " are required");
        }
        return result;
    }

    //[[Repeat.AutoGeneratedEnd]]

    public String[] toTrimmedLinesArray() {
        return value == null ? null : splitJsonOrTrimmedLinesArray(value);
    }

    public List<String> toTrimmedLines() {
        return value == null ? null : splitJsonOrTrimmedLines(value);
    }

    public String[] toTrimmedLinesWithoutCommentsArray() {
        return value == null ? null : splitJsonOrTrimmedLinesWithoutCommentsArray(value);
    }

    public List<String> toTrimmedLinesWithoutComments() {
        return value == null ? null : splitJsonOrTrimmedLinesWithoutComments(value);
    }

    public MultiLineOrJsonSplitter toTrimmedLinesWithComments() {
        return value == null ? null : splitJsonOrTrimmedLinesWithComments(value);
    }

    /**
     * Note: this method, like usual toString(), never returns <code>null</code>,
     * and also can reduce the stored string!
     * <p><b>If you need to get exact <code>String</code> value, please use {@link #getValue()} method.</b>
     *
     * @return scalar value for initialized non-null scalar
     * or something else for <code>null</code> or non-initialized scalars.
     */
    @Override
    public String toString() {
        if (!isInitialized()) {
            return super.toString();
        }
        assert value != null : "null initialized value";
        final int len = Math.min(value.length(), 128);
        for (int p = 0; p < len; p++) {
            final char c = value.charAt(p);
            if (c == '\r' || c == '\n') {
                return value.substring(0, p) + "...";
            }
        }
        return value.length() > len ? value.substring(0, 128) + "..." : value;
    }

    public static SScalar valueOf(Object value) {
        return new SScalar().setTo(value);
    }

    public static void setTo(Map<String, SScalar> scalars, String key, Supplier<?> supplier) {
        final SScalar scalar = scalars.get(key);
        if (scalar != null) {
            scalar.setTo(supplier.get());
        }
    }

    public static String[] splitJsonOrTrimmedLinesArray(String multiLines) {
        return new MultiLineOrJsonSplitter(multiLines).lines;
        // - no reasons to clone
    }

    public static List<String> splitJsonOrTrimmedLines(String multiLines) {
        return new MultiLineOrJsonSplitter(multiLines).lines();
    }

    public static String[] splitJsonOrTrimmedLinesWithoutCommentsArray(String multiLines) {
        return new MultiLineOrJsonSplitter(multiLines).extractComments(true).lines;
        // - no reasons to clone
    }

    public static List<String> splitJsonOrTrimmedLinesWithoutComments(String multiLines) {
        return new MultiLineOrJsonSplitter(multiLines).extractComments(true).lines();
    }

    public static MultiLineOrJsonSplitter splitJsonOrTrimmedLinesWithComments(String multiLines) {
        return new MultiLineOrJsonSplitter(multiLines).extractComments(false);
    }

    public static boolean toCLikeBoolean(String scalar) {
        Objects.requireNonNull(scalar, "Null scalar value");
        return Boolean.parseBoolean(scalar) || doubleToBoolean(scalar);
    }

    public static boolean toCommonBoolean(String scalar) {
        Objects.requireNonNull(scalar, "Null scalar value");
        return !scalar.equalsIgnoreCase("false") &&
                        (Boolean.parseBoolean(scalar) || doubleToBoolean(scalar));
    }

    public static boolean toCommonBoolean(String scalar, boolean defaultCondition) {
        return scalar == null ? defaultCondition : toCommonBoolean(scalar);
    }

    @Override
    protected void freeResources() {
        value = null;
    }

    @UsedForExternalCommunication
    private void setValue(String value) {
        this.value = value;
        setInitializedAndResetFlags(value != null);
        // - no sense to keep uninitialized state
    }

    private static boolean doubleToBoolean(String scalar) {
        try {
            return Double.parseDouble(scalar) != 0.0;
        } catch (NumberFormatException e) {
            return !scalar.isEmpty();
        }
    }


    public static class MultiLineOrJsonSplitter {
        private final String[] lines;
        private final boolean[] nonString;
        String[] comments = null;

        private MultiLineOrJsonSplitter(String multiLines) {
            Objects.requireNonNull(multiLines, "Null multi-line string");
            multiLines = multiLines.trim();
            JsonArray jsonArray;
            try {
                jsonArray = Jsons.toJsonArray(multiLines);
            } catch (JsonException e) {
                jsonArray = null;
            }
            if (jsonArray == null) {
                lines = multiLines.isEmpty() ? new String[0] : multiLines.split("(?:\\r(?!\\n)|\\n|\\r\\n)");
                nonString = null;
            } else {
                lines = new String[jsonArray.size()];
                nonString = new boolean[lines.length];
                int k = 0;
                for (JsonValue jsonValue : jsonArray) {
                    nonString[k] = jsonValue.getValueType() != JsonValue.ValueType.STRING;
                    lines[k] = Jsons.toPrettyString(jsonValue);
                    k++;
                }
            }
            for (int k = 0; k < lines.length; k++) {
                lines[k] = lines[k].trim();
            }
        }

        private MultiLineOrJsonSplitter extractComments(boolean removeComments) {
            if (!removeComments) {
                comments = new String[lines.length];
                // - filled by null by Java
            }
            for (int k = 0; k < lines.length; k++) {
                final String s = lines[k];
                final int p = (nonString != null && nonString[k]) ? -1 : s.indexOf("//");
                if (p != -1) {
                    lines[k] = s.substring(0, p).trim();
                    if (!removeComments) {
                        comments[k] = s.substring(p + 2).trim();
                    }
                }
            }
            return this;
        }

        private MultiLineOrJsonSplitter(String[] lines) {
            this.lines = Objects.requireNonNull(lines, "Null lines").clone();
            this.comments = new String[lines.length];
            // - filled by null by Java
            this.nonString = null;
        }

        private MultiLineOrJsonSplitter(String[] lines, String[] comments) {
            this.lines = Objects.requireNonNull(lines, "Null lines").clone();
            this.comments = Objects.requireNonNull(comments, "Null comments").clone();
            this.nonString = null;
        }

        public static MultiLineOrJsonSplitter valueOfSimpleLines(String[] lines) {
            return new MultiLineOrJsonSplitter(lines);
        }

        public static MultiLineOrJsonSplitter valueOfLinesAndComments(String[] lines, String[] comments) {
            return new MultiLineOrJsonSplitter(lines, comments);
        }

        public static MultiLineOrJsonSplitter valueOfCommentedLines(String[] linesWithComments) {
            return new MultiLineOrJsonSplitter(linesWithComments).extractComments(false);
        }

        public int numberOfLines() {
            return lines.length;
        }

        public String[] linesArray() {
            return lines.clone();
        }

        public List<String> lines() {
            return Collections.unmodifiableList(Arrays.asList(lines));
        }

        public String[] commentsArray() {
            assert comments != null : "comments cannot be null outside this class";
            return comments.clone();
        }

        public List<String> comments() {
            assert comments != null : "comments cannot be null outside this class";
            return Collections.unmodifiableList(Arrays.asList(comments));
            // - not List.of: some comments MAY be null
        }

        @Override
        public String toString() {
            return "MultiLineOrJsonSplitter: " +
                    "lines=" + Arrays.toString(lines) +
                    ", nonString=" + Arrays.toString(nonString) +
                    ", comments=" + Arrays.toString(comments);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MultiLineOrJsonSplitter that = (MultiLineOrJsonSplitter) o;
            return Arrays.equals(lines, that.lines) &&
                    Arrays.equals(comments, that.comments);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(lines);
            result = 31 * result + Arrays.hashCode(comments);
            return result;
        }
    }
}
