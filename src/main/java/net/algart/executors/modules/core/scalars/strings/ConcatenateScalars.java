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

package net.algart.executors.modules.core.scalars.strings;

import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.SeveralScalarsOperation;

import java.util.List;

public final class ConcatenateScalars extends SeveralScalarsOperation {
    public static final String PROPERTY_PREFIX = "defaultS";

    private String separator = ", ";

    public ConcatenateScalars() {
    }

    public String getSeparator() {
        return separator;
    }

    public ConcatenateScalars setSeparator(String separator) {
        this.separator = nonNull(separator);
        return this;
    }

    @Override
    public void onChangeParameter(String name) {
        if (name == null || !name.startsWith(PROPERTY_PREFIX)) {
            // - skipping standard processing to avoid warning "has no setter"
            super.onChangeParameter(name);
        }
    }

    @Override
    public SScalar process(List<SScalar> sources) {
        final String separator = this.separator
                .replace("\\n", "\n")
                .replace("\\r", "\r");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0, n = sources.size(); i < n; i++) {
            String s;
            final SScalar scalar = sources.get(i);
            if (scalar != null && scalar.isInitialized()) {
                s = scalar.getValue();
            } else {
                s = parameters().getString(PROPERTY_PREFIX + (i + 1), null);
                if (s != null && s.isEmpty()) {
                    s = null;
                    // - unlike input scalars, empty parameter is not concatenated
                    // (there is no ability to enter null parameter)
                }
            }
            if (s != null) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(s);
            }
        }
        return SScalar.valueOf(sb);
    }
}
