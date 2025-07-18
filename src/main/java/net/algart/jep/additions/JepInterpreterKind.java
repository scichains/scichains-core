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

package net.algart.jep.additions;

import jep.JepConfig;

public enum JepInterpreterKind {
    LOCAL("local") {
        @Override
        ConfiguredInterpreter doNewInterpreter(JepConfig configuration) {
            return new ConfiguredInterpreter(JepCreationTools.newSubInterpreter(configuration), configuration);
        }
    },
    SHARED("shared") {
        @Override
        ConfiguredInterpreter doNewInterpreter(JepConfig configuration) {
            return new ConfiguredInterpreter(JepCreationTools.newSharedInterpreter(configuration), configuration);
        }
    };

    private final String kindName;

    JepInterpreterKind(String kindName) {
        this.kindName = kindName;
    }

    public String kindName() {
        return kindName;
    }

    public ConfiguredInterpreter newInterpreter() {
        return newInterpreter(null);
    }

    public ConfiguredInterpreter newInterpreter(JepConfig configuration) {
        return doNewInterpreter(configuration == null ? new JepExtendedConfiguration() : configuration);
    }

    abstract ConfiguredInterpreter doNewInterpreter(JepConfig configuration);

}
