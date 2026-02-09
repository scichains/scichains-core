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

package net.algart.executors.api.data;

import net.algart.external.UsedForExternalCommunication;

import java.util.*;

public enum DataType {
    MAT("mat", UUID.fromString("031FC202-0193-4933-AB2E-D81492CE67E0")) {
        @Override
        public Class<? extends Data> typeClass() {
            return SMat.class;
        }

        @Override
        public Data createEmpty() {
            return new SMat();
        }
    },

    SCALAR("scalar", UUID.fromString("869bc442-bd01-4094-afc1-783b9ed1c24e")) {
        @Override
        public Class<? extends Data> typeClass() {
            return SScalar.class;
        }

        @Override
        public Data createEmpty() {
            return new SScalar();
        }
    },

    NUMBERS("numbers", UUID.fromString("C72A2A31-75BA-4E09-A02B-A9CBC4AC62D2")) {
        @Override
        public Class<? extends Data> typeClass() {
            return SNumbers.class;
        }

        @Override
        public Data createEmpty() {
            return new SNumbers();
        }
    };

    private final String typeName;

    @UsedForExternalCommunication
    private final UUID uuid;

    DataType(String name, UUID uuid) {
        this.typeName = Objects.requireNonNull(name, "Null type name");
        this.uuid = Objects.requireNonNull(uuid, "Null uuid");
    }

    @UsedForExternalCommunication
    public final String typeName() {
        return typeName;
    }

    @UsedForExternalCommunication
    public final UUID uuid() {
        return uuid;
    }

    public abstract Data createEmpty();

    public abstract Class<? extends Data> typeClass();

    public static Collection<String> typeNames() {
        return Arrays.stream(values()).map(DataType::typeName).toList();
    }

    @UsedForExternalCommunication
    public static DataType ofTypeName(String typeName) {
        Objects.requireNonNull(typeName, "Null type name");
        return fromTypeName(typeName).orElseThrow(
                () -> new IllegalArgumentException("Unknown type name \"" + typeName + "\""));
    }

    /**
     * Returns an {@link Optional} containing the {@link DataType} with the given {@link #typeName()}.
     * <p>If no data type with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param typeName the type name; may be {@code null}.
     * @return an optional data type.
     */
    public static Optional<DataType> fromTypeName(String typeName) {
        for (DataType type : values()) {
            if (type.typeName.equals(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<DataType> fromUUID(UUID uuid) {
        for (DataType type : values()) {
            if (type.uuid.equals(uuid)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<DataType> fromUUID(String uuid) {
        for (DataType type : values()) {
            if (type.uuid.toString().equalsIgnoreCase(uuid)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
