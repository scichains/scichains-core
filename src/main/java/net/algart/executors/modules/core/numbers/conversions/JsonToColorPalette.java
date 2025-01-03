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

package net.algart.executors.modules.core.numbers.conversions;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.ChannelOperation;
import net.algart.json.Jsons;

import java.util.Map;
import java.util.Objects;

public final class JsonToColorPalette extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_JSON_PALETTE = "json_palette";
    public static final String INPUT_JSON_NAMED_INDEXES = "json_named_indexes";
    public static final String OUTPUT_PALETTE = "palette";

    private int numberOfChannels = 3;
    private String defaultColor = "#000000";
    private double defaultValueForExtraChannels = 1.0;
    private int indexingBase = 0;

    public JsonToColorPalette() {
        setDefaultInputScalar(INPUT_JSON_PALETTE);
        addInputScalar(INPUT_JSON_NAMED_INDEXES);
        setDefaultOutputNumbers(OUTPUT_PALETTE);
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public JsonToColorPalette setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = positive(numberOfChannels);
        return this;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public JsonToColorPalette setDefaultColor(String defaultColor) {
        this.defaultColor = nonNull(defaultColor);
        return this;
    }

    public double getDefaultValueForExtraChannels() {
        return defaultValueForExtraChannels;
    }

    public JsonToColorPalette setDefaultValueForExtraChannels(double defaultValueForExtraChannels) {
        this.defaultValueForExtraChannels = defaultValueForExtraChannels;
        return this;
    }

    public int getIndexingBase() {
        return indexingBase;
    }

    public JsonToColorPalette setIndexingBase(int indexingBase) {
        this.indexingBase = nonNegative(indexingBase);
        return this;
    }

    @Override
    public void process() {
        final SNumbers result = process(
                getInputScalar(INPUT_JSON_PALETTE).getValue(),
                getInputScalar(INPUT_JSON_NAMED_INDEXES, true).getValue());
        getNumbers(OUTPUT_PALETTE).exchange(result);
    }

    public SNumbers process(String jsonPaletteString, String jsonIndexNamesString) {
        Objects.requireNonNull(jsonPaletteString, "Null jsonPaletteString");
        final JsonObject jsonPalette = Jsons.toJson(jsonPaletteString.trim());
        final JsonObject jsonIndexNames = Jsons.toJson(jsonIndexNamesString, true);
        final int tableLength = JsonToIndexValueTable.findTableLength(
                jsonPalette, jsonIndexNames, indexingBase, INPUT_JSON_NAMED_INDEXES, true);
        // - allow too low indexes: here it is better than throwing an incomprehensible exception
        final double[] resultTable = new double[numberOfChannels * tableLength];
        final double[] filler = new double[numberOfChannels];
        fillColor(filler, 0, ChannelOperation.decodeRGBA(defaultColor));
        for (int disp = 0; disp < resultTable.length; ) {
            for (int j = 0; j < numberOfChannels; j++) {
                resultTable[disp++] = filler[j];
            }
        }
        for (Map.Entry<String, JsonValue> entry : jsonPalette.entrySet()) {
            final int index = JsonToIndexValueTable.toIndex(
                    jsonIndexNames, entry.getKey(), INPUT_JSON_NAMED_INDEXES);
            if (index < indexingBase) {
                // - possible when allowTooLowIndexes=true parameter
                continue;
            }
            final int offset = (index - indexingBase) * numberOfChannels;
            final JsonValue jsonValue = entry.getValue();
            switch (jsonValue.getValueType()) {
                case NUMBER: {
                    final double value = ((JsonNumber) jsonValue).doubleValue();
                    fillColor(resultTable, offset, ChannelOperation.decodeRGBAForSingleNumber(value));
                    break;
                }
                case STRING: {
                    final String value = ((JsonString) jsonValue).getString();
                    fillColor(resultTable, offset, ChannelOperation.decodeRGBA(value));
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Palette JSON  must contain double or string values, " +
                            "but for key \"" + entry.getKey() + "\" it contains \"" + jsonValue + "\"");
                }
            }
        }
        return SNumbers.valueOfArray(resultTable, numberOfChannels);
    }

    private void fillColor(double[] values, int offset, double[] color) {
        for (int i = 0; i < numberOfChannels; i++) {
            values[offset++] = ChannelOperation.colorChannel(
                    color, i, numberOfChannels, defaultValueForExtraChannels);
        }
    }
}
