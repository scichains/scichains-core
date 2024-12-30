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

package net.algart.executors.api.model;

import java.util.Objects;

// Note: this class is useful for listing links in the chain.
// It is actually used in Chain.cleanCopy method only.
public final class ChainLink {
    final String srcPortId;
    final String destPortId;

    private ChainLink(String srcPortId, String destPortId) {
        this.srcPortId = Objects.requireNonNull(srcPortId, "Null srcPortId");
        this.destPortId = Objects.requireNonNull(destPortId, "Null destPortId");
    }

    public static ChainLink newInstance(String srcPortId, String destPortId) {
        return new ChainLink(srcPortId, destPortId);
    }

    public static ChainLink valueOf(ChainSpecification.ChainLinkConf linkConf) {
        return newInstance(linkConf.getSrcPortUuid(), linkConf.getDestPortUuid());
    }

    public String getSrcPortId() {
        return srcPortId;
    }

    public String getDestPortId() {
        return destPortId;
    }

    @Override
    public String toString() {
        return "ChainLink{" +
                "srcPortId='" + srcPortId + '\'' +
                ", destPortId='" + destPortId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChainLink chainLink = (ChainLink) o;
        return srcPortId.equals(chainLink.srcPortId) &&
                destPortId.equals(chainLink.destPortId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcPortId, destPortId);
    }
}
