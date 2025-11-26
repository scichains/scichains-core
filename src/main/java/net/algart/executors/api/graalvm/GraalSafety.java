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

import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.graalvm.GraalContextCustomizer;
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
    PURE("pure", false) {
        @Override
        public void customize(Context.Builder builder) {
        }
    },

    SAFE("safe", true) {
        @Override
        public void customize(Context.Builder builder) {
            builder.allowIO(IOAccess.ALL);
            // - necessary to import JavaScript modules
            builder.allowHostAccess(HostAccess.ALL);
            builder.allowHostClassLookup(GraalSafety::isSafeClass);
        }
    },

    ALL_ACCESS("all-access", true) {
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
            // allowing to read some system properties (it is not a secure operation)
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
            java.awt.Color.class.getCanonicalName(),
            java.awt.image.BufferedImage.class.getCanonicalName(),
            // - but not Graphics2D: that class can be used for getting access to some system information,
            // usually BufferedImage.getGraphics is absolutely enough
            java.awt.BasicStroke.class.getCanonicalName(),
            java.awt.RenderingHints.class.getCanonicalName(),
            java.awt.geom.Point2D.Float.class.getCanonicalName(),
            java.awt.geom.Point2D.Double.class.getCanonicalName(),
            java.awt.Polygon.class.getCanonicalName(),
            java.awt.geom.Rectangle2D.Float.class.getCanonicalName(),
            java.awt.geom.Rectangle2D.Double.class.getCanonicalName(),
            java.awt.Font.class.getCanonicalName(),
            java.awt.geom.Ellipse2D.Float.class.getCanonicalName(),
            java.awt.geom.Ellipse2D.Double.class.getCanonicalName(),
            java.awt.geom.Line2D.Float.class.getCanonicalName(),
            java.awt.geom.Line2D.Double.class.getCanonicalName(),
            java.awt.geom.AffineTransform.class.getCanonicalName(),
            java.awt.image.AffineTransformOp.class.getCanonicalName(),
            java.awt.image.RescaleOp.class.getCanonicalName(),
            java.awt.image.LookupOp.class.getCanonicalName(),
            java.awt.image.ColorConvertOp.class.getCanonicalName(),
            java.awt.image.ConvolveOp.class.getCanonicalName(),
            java.awt.GradientPaint.class.getCanonicalName(),
            SScalar.class.getCanonicalName(),
            SNumbers.class.getCanonicalName(),
            SMat.class.getCanonicalName()
    ));

    private final String safetyName;
    private final boolean supportedJavaAccess;

    GraalSafety(String safetyName, boolean supportedJavaAccess) {
        this.safetyName = Objects.requireNonNull(safetyName);
        this.supportedJavaAccess = supportedJavaAccess;
    }


    public String safetyName() {
        return safetyName;
    }

    /**
     * Returns an {@link Optional} containing the {@link GraalSafety} with the given {@link #safetyName()}.
     * <p>If no safety type with the specified name exists or if the argument is {@code null},
     * an empty optional is returned.
     *
     * @param safetyName the safety name; may be {@code null}.
     * @return an optional safety type.
     */
    public static Optional<GraalSafety> fromSafetyName(String safetyName) {
        for (GraalSafety safety : values()) {
            if (safety.safetyName.equals(safetyName)) {
                return Optional.of(safety);
            }
        }
        return Optional.empty();
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
