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

package net.algart.executors.modules.core.scalars.io;

import net.algart.json.Jsons;

import jakarta.json.JsonObject;

public class ReadJson extends ReadScalar {
    public ReadJson() {
    }

    public static ReadJson getInstance() {
        return new ReadJson();
    }

    public static ReadJson getSecureInstance() {
        final ReadJson result = new ReadJson();
        result.setSecure(true);
        return result;
    }

    @Override
    String checkResult(String result) {
        if (result == null) {
            return null;
        }
        final JsonObject json;
        try {
            json = Jsons.toJson(result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal JSON", e);
            // - simple message; detailed message will be probably too complex or strange,
            // it the file is actually not JSON
        }
        return Jsons.toPrettyString(json);
    }
}
