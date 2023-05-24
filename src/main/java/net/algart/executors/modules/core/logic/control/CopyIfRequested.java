/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.core.logic.control;

public final class CopyIfRequested extends AbstractCopyIfRequested {
    public static final int NUMBER_OF_PORTS = 5;

    public CopyIfRequested() {
        for (int k = 0; k < NUMBER_OF_PORTS; k++) {
            addInputScalar(ifSPortName(k));
            addInputScalar(sPortName(k));
            addInputScalar(ifXPortName(k));
            addInputNumbers(xPortName(k));
            addInputScalar(ifMPortName(k));
            addInputMat(mPortName(k));
        }
    }

    @Override
    public void process() {
        for (int k = 0; k < NUMBER_OF_PORTS; k++) {
            if (condition(ifSPortName(k))) {
                getScalar(sPortName(k)).exchange(getInputScalar(sPortName(k), true));
            }
            if (condition(ifXPortName(k))) {
                getNumbers(xPortName(k)).exchange(getInputNumbers(xPortName(k), true));
            }
            if (condition(ifMPortName(k))) {
                getMat(mPortName(k)).exchange(getInputMat(mPortName(k), true));
            }
        }
    }
}
