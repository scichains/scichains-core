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

package net.algart.executors.modules.core.files;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.scalars.ScalarFilter;
import net.algart.io.MatrixIO;

public final class ChangeFileExtension extends ScalarFilter implements ReadOnlyExecutionInput {
    private String extension = "";
    private boolean preserveWhenEmpty = false;

    public ChangeFileExtension() {
    }

    public String getExtension() {
        return extension;
    }

    public ChangeFileExtension setExtension(String extension) {
        this.extension = nonNull(extension);
        return this;
    }

    public boolean isPreserveWhenEmpty() {
        return preserveWhenEmpty;
    }

    public ChangeFileExtension setPreserveWhenEmpty(boolean preserveWhenEmpty) {
        this.preserveWhenEmpty = preserveWhenEmpty;
        return this;
    }

    @Override
    public SScalar process(SScalar source) {
        String s = source.getValue();
        s = changeExtension(s);
        return SScalar.of(s);
    }

    public String changeExtension(String s) {
        if (s == null) {
            return null;
        }
        final String extension = this.extension.trim();
        if (!extension.isEmpty()) {
            if (!extension.matches("^[A-Za-z0-9_\\-.]*$")) {
                throw new IllegalArgumentException("Non-allowed extension \"" + extension
                        + "\": only latin letters A-Z, a-z, digits 0-9 "
                        + "and characters '_', '-', '.' are permitted");
            }
            return MatrixIO.removeExtension(s) + "." + extension;
        } else {
            return preserveWhenEmpty ? s : MatrixIO.removeExtension(s);
        }
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
