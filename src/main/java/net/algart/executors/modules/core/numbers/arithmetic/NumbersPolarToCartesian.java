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

package net.algart.executors.modules.core.numbers.arithmetic;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;
import net.algart.math.IRange;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.IntStream;

public final class NumbersPolarToCartesian extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_R = "r";
    public static final String INPUT_FI = "fi";
    public static final String INPUT_R_FI = "r_fi";
    public static final String OUTPUT_X = "x";
    public static final String OUTPUT_Y = "y";
    public static final String OUTPUT_X_Y = "x_y";

    public enum AngleMultiplier {
        NONE((angle, custom) -> angle),
        MULTIPLIER_TO_RADIANS((angle, custom) -> Math.toRadians(angle)),
        MULTIPLIER_PI((angle, custom) -> Math.PI * angle),
        MULTIPLIER_2PI((angle, custom) -> PI_2 * angle),
        CUSTOM((angle, custom) -> angle * custom);

        private final DoubleBinaryOperator multiplier;

        AngleMultiplier(DoubleBinaryOperator multiplier) {
            this.multiplier = multiplier;
        }
    }

    private static final double PI_2 = 2.0 * StrictMath.PI;

    private AngleMultiplier angleMultiplier = AngleMultiplier.NONE;
    private double customAngleMultiplier = 1.0;

    public NumbersPolarToCartesian() {
        super(INPUT_R, INPUT_FI, INPUT_R_FI);
        useVisibleResultParameter();
        setDefaultOutputNumbers(OUTPUT_X_Y);
        addOutputNumbers(OUTPUT_X);
        addOutputNumbers(OUTPUT_Y);
    }

    public AngleMultiplier getAngleMultiplier() {
        return angleMultiplier;
    }

    public NumbersPolarToCartesian setAngleMultiplier(AngleMultiplier angleMultiplier) {
        this.angleMultiplier = nonNull(angleMultiplier);
        return this;
    }

    public double getCustomAngleMultiplier() {
        return customAngleMultiplier;
    }

    public NumbersPolarToCartesian setCustomAngleMultiplier(double customAngleMultiplier) {
        this.customAngleMultiplier = customAngleMultiplier;
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        SNumbers r = sources.get(0);
        SNumbers fi = sources.get(1);
        SNumbers r_fi = sources.get(2);
        if (r_fi != null) {
            r_fi.requireBlockLength(2, "r/fi");
        }
        if (r != null) {
            r.requireInitialized("r").requireBlockLength(1, "r");
        } else {
            r = checkRFi(r_fi, "r").columnRange(0, 1);
        }
        if (fi != null) {
            fi.requireInitialized("fi").requireBlockLength(1, "fi");
        } else {
            fi = checkRFi(r_fi, "fi").columnRange(1, 1);
        }
        final float[] rArray = r.toFloatArray();
        final float[] fiArray = fi.toFloatArray();
        final SNumbers result = new SNumbers();
        if (isOutputNecessary(OUTPUT_X_Y)) {
            final float[] xy = polarToXY(rArray, fiArray);
            result.setTo(xy, 2);
            if (isOutputNecessary(OUTPUT_X)) {
                getNumbers(OUTPUT_X).replaceColumnRange(0, result, 0, 1);
            }
            if (isOutputNecessary(OUTPUT_Y)) {
                getNumbers(OUTPUT_Y).replaceColumnRange(0, result, 1, 1);
            }
        } else {
            if (isOutputNecessary(OUTPUT_X)) {
                getNumbers(OUTPUT_X).setTo(polarToX(rArray, fiArray), 1);
            }
            if (isOutputNecessary(OUTPUT_Y)) {
                getNumbers(OUTPUT_Y).setTo(polarToY(rArray, fiArray), 1);
            }
        }
        return result;
    }

    public float[] polarToX(float[] r, float[] fi) {
        Objects.requireNonNull(r, "Null r");
        Objects.requireNonNull(fi, "Null fi");
        if (r.length != fi.length) {
            throw new IllegalArgumentException("Different lengths of r and fi");
        }
        float[] result = new float[r.length];
        IntStream.range(0, (r.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, to = (int) Math.min((long) i + 256, r.length); i < to; i++) {
                final double rValue = r[i];
                final double fiValue = angleMultiplier.multiplier.applyAsDouble(fi[i], customAngleMultiplier);
                result[i] = (float) (rValue * Math.cos(fiValue));
            }
        });
        return result;
    }

    public float[] polarToY(float[] r, float[] fi) {
        Objects.requireNonNull(r, "Null r");
        Objects.requireNonNull(fi, "Null fi");
        if (r.length != fi.length) {
            throw new IllegalArgumentException("Different lengths of r and fi");
        }
        float[] result = new float[r.length];
        IntStream.range(0, (r.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, to = (int) Math.min((long) i + 256, r.length); i < to; i++) {
                final double rValue = r[i];
                final double fiValue = angleMultiplier.multiplier.applyAsDouble(fi[i], customAngleMultiplier);
                result[i] = (float) (rValue * Math.sin(fiValue));
            }
        });
        return result;
    }

    public float[] polarToXY(float[] r, float[] fi) {
        Objects.requireNonNull(r, "Null r");
        Objects.requireNonNull(fi, "Null fi");
        if (r.length != fi.length) {
            throw new IllegalArgumentException("Different lengths of r and fi");
        }
        float[] result = new float[2 * r.length];
        IntStream.range(0, (r.length + 255) >>> 8).parallel().forEach(block -> {
            for (int i = block << 8, disp = 2 * i, to = (int) Math.min((long) i + 256, r.length); i < to; i++) {
                final double rValue = r[i];
                final double fiValue = angleMultiplier.multiplier.applyAsDouble(fi[i], customAngleMultiplier);
                result[disp++] = (float) (rValue * Math.cos(fiValue));
                result[disp++] = (float) (rValue * Math.sin(fiValue));
            }
        });
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

    @Override
    protected IRange selectedColumnRange(int inputIndex) {
        final int start = getIndexInBlock(inputIndex);
        return IRange.of(start, inputIndex == 2 ? start + 1 : start);
    }

    private static SNumbers checkRFi(SNumbers rFi, String rOrFiName) {
        if (rFi == null || !rFi.isInitialized()) {
            throw new IllegalArgumentException("The \"r/fi\" port has no initialized data, but they are required, "
                    + "because \"" + rOrFiName + "\" is also empty");
        }
        return rFi;
    }

}
