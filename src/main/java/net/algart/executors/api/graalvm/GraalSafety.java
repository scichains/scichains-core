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

package net.algart.executors.api.graalvm;

import net.algart.graalvm.GraalContextCustomizer;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.IOAccess;

import java.util.*;

/**
 * Levels of GraalVM safety for SciChains environment.
 * In addition to {@link GraalContextCustomizer} constants,
 * this enum provides additional level {@link #SAFE} which provides access
 * to the standard Java class and to the classes necessary for SciChains:
 * {@link SScalar}, {@link SNumbers}, {@link SMat}.
 */
public enum GraalSafety implements GraalContextCustomizer {
    PURE(false, "pure") {
        @Override
        public void customize(Context.Builder builder) {
        }
    },

    SAFE(true, "safe") {
        @Override
        public void customize(Context.Builder builder) {
            builder.allowIO(IOAccess.ALL);
            // - necessary to import JavaScript modules
            builder.allowHostAccess(HostAccess.ALL);
            builder.allowHostClassLookup(GraalSafety::isSafeClass);
        }
    },

    ALL_ACCESS(true, "all-access") {
        @Override
        public void customize(Context.Builder builder) {
            GraalContextCustomizer.ALL_ACCESS.customize(builder);
        }

        @Override
        public boolean isAllAccess() {
            return true;
        }
    };

    private static final Set<String> SAFE_CLASSES = new HashSet<>(Arrays.asList(
            Object.class.getCanonicalName(),
            String.class.getCanonicalName(),
            Locale.class.getCanonicalName(),
            Float.class.getCanonicalName(),
            Double.class.getCanonicalName(),
            // - but not Integer/Long: they have "getInteger"/"getLong" methods,
            // allowing to read some system properties (it is not secure operation)
            Math.class.getCanonicalName(),
            StrictMath.class.getCanonicalName(),
            Arrays.class.getCanonicalName(),
            char.class.getCanonicalName(),
            boolean.class.getCanonicalName(),
            byte.class.getCanonicalName(),
            short.class.getCanonicalName(),
            int.class.getCanonicalName(),
            long.class.getCanonicalName(),
            float.class.getCanonicalName(),
            double.class.getCanonicalName(),
            char[].class.getCanonicalName(),
            boolean[].class.getCanonicalName(),
            byte[].class.getCanonicalName(),
            short[].class.getCanonicalName(),
            int[].class.getCanonicalName(),
            long[].class.getCanonicalName(),
            float[].class.getCanonicalName(),
            double[].class.getCanonicalName(),
            // - previous types are necessary for creating primitive Java arrays from JavaScript,
            // like in the following code:
            //      var IntsC = Java.type("int[]");
            //      var ja = new IntsC(100);
            SScalar.class.getCanonicalName(),
            SNumbers.class.getCanonicalName(),
            SMat.class.getCanonicalName()
    ));

    private final boolean supportedJavaAccess;
    private final String safetyName;

    GraalSafety(boolean supportedJavaAccess, String safetyName) {
        this.supportedJavaAccess = supportedJavaAccess;
        this.safetyName = safetyName;
    }


    public String safetyName() {
        return safetyName;
    }

    public static GraalSafety ofOrNull(String name) {
        Objects.requireNonNull(name, "Null safety name");
        for (GraalSafety safety : values()) {
            if (name.equals(safety.safetyName)) {
                return safety;
            }
        }
        return null;
    }

    @Override
    public boolean isJavaAccessSupported() {
        return supportedJavaAccess;
    }

    public boolean isWorkingDirectorySupported() {
        return this != PURE;
    }

    @Override
    public String toString() {
        return safetyName;
    }

    private static boolean isSafeClass(String className) {
        return SAFE_CLASSES.contains(className);
    }
}
