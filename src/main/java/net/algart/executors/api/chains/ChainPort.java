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

package net.algart.executors.api.chains;

import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.Port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

abstract class ChainPort<REVERSE extends ChainPort<?>> {
    final Chain chain;
    final ChainBlock block;
    final String name;
    Data data;
    // - never null
    final String id;
    // - may be null if this port is not used in any links in the chain
    final ChainPortType portType;
    final DataType dataType;
    final ChainPortKey key;

    final Map<String, REVERSE> connected;

    ChainPort(ChainBlock block, String id, String name, ChainPortType portType, DataType dataType) {
        this.block = Objects.requireNonNull(block, "Null containing block");
        this.chain = block.chain;
        this.id = id;
        this.name = Objects.requireNonNull(name, "Null name");
        this.portType = Objects.requireNonNull(portType, "Null port type");
        this.dataType = Objects.requireNonNull(dataType, "Null data type");
        this.data = dataType.createEmpty();
        this.key = new ChainPortKey(portType, name);
        this.connected = new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChainPortType getPortType() {
        return portType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Data getData() {
        if (data == null) {
            throw new AssertionError("Null data in " + this);
        }
        return data;
    }

    public Map<String, REVERSE> getConnected() {
        return Collections.unmodifiableMap(connected);
    }

    public void addConnection(REVERSE connectedPort) {
        Objects.requireNonNull(connectedPort, "Null connectedPort");
        if (portType.actualPortType() == Port.Type.INPUT && !this.connected.isEmpty()) {
            throw new IllegalArgumentException("Cannot add more than 1 connection to the input port: " + this);
        }
        if (this.connected.putIfAbsent(connectedPort.id, connectedPort) != null) {
            throw new IllegalArgumentException("Duplicate connected port id: " + connectedPort.id);
        }
    }

    public boolean isConnected() {
        return !connected.isEmpty();
    }

    public void removeData() {
        this.data.setInitialized(false);
    }

    /**
     * Returns a new copy of this port, belonging to the specified block,
     * with an empty data and empty connections.
     *
     * @param newBlock new port will be a port of this block.
     * @return new clean copy of this port (without any data and connections).
     */
    public abstract ChainPort<REVERSE> cleanCopy(ChainBlock newBlock);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", portType=" + portType +
                ", dataType=" + dataType +
                ", data=" + data +
                ", connected=" + connected.keySet() +
                " (belongs to block " + System.identityHashCode(block) +
                " and chain " + System.identityHashCode(chain) +
                "}'";
    }
}
