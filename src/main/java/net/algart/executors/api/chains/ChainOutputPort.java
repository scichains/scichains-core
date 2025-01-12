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

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.Port;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.api.system.ExecutorSpecification;

import java.util.concurrent.atomic.AtomicInteger;

public final class ChainOutputPort extends ChainPort<ChainInputPort> {
    private final AtomicInteger countOfConnectedInputs = new AtomicInteger(0);
    private volatile boolean hasConnectedReadOnlyExecutors = false;

    private ChainOutputPort(ChainBlock block, String id, String name, ChainPortType portType, DataType dataType) {
        super(block, id, name, portType, dataType);
        if (portType.actualPortType() != Port.Type.OUTPUT) {
            throw new IllegalArgumentException("Non-output port type");
        }
    }

    public static ChainOutputPort newInstance(
            ChainBlock block,
            String id,
            String name,
            ChainPortType portType,
            DataType dataType) {
        return new ChainOutputPort(block, id, name, portType, dataType);
    }

    public static ChainOutputPort valueOf(ChainBlock block, ChainSpecification.ChainBlockConf.PortConf portConf) {
        return newInstance(
                block,
                portConf.getUuid(),
                portConf.getName(),
                portConf.getPortType(),
                portConf.getDataType());
    }

    public static ChainOutputPort valueOf(ChainBlock block, ExecutorSpecification.PortConf portConf) {
        return newInstance(
                block,
                null,
                portConf.getName(),
                ChainPortType.OUTPUT_PORT,
                portConf.getValueType());
    }

    public void copyFromExecutorPort() {
        final ExecutionBlock executor = block.getExecutor();
        switch (portType) {
            case OUTPUT_PORT -> {
                synchronized (chain.blocksInteractionLock) {
                    // exchanging/moving data between all ports blocks must be synchronized globally
                    this.data = executor.getData(name);
                    // - copying reference to data, not content
                }
            }
            case OUTPUT_CONTROL_AS_PORT -> {
                synchronized (chain.blocksInteractionLock) {
                    // exchanging/moving data between all ports blocks must be synchronized globally
                    this.data.setTo(SScalar.valueOf(executor.parameters().getString(name)));
                }
            }
            default -> {
                throw new AssertionError("Unknown output port type: " + portType);
            }
        }
    }

    public void resetConnectedInputsInformation() {
        boolean hasConnectedReadOnlyExecutors = false;
        int count = 0;
        for (ChainInputPort inputPort : connected.values()) {
            if (inputPort.block.isExecutedAtRunTime()) {
                final var executor = inputPort.block.executor;
                // Here we use executor directly to allow calling this method
                // even when executors are not available. It cannot lead to any problems:
                // we will just set hasConnectedReadOnlyExecutors to false (this optimization
                // cannot be used without actual executors in any case).
                hasConnectedReadOnlyExecutors |= executor != null && executor.isReadOnlyInput();
                count++;
            }
        }
        this.hasConnectedReadOnlyExecutors = hasConnectedReadOnlyExecutors;
        this.countOfConnectedInputs.set(count);
    }

    public int reduceCountOfConnectedInputs() {
        final int result = countOfConnectedInputs.decrementAndGet();
        if (result < 0) {
            throw new IllegalStateException("Negative counter of connected input ports in " + this);
        }
        return result;
    }

    public int getCountOfConnectedInputs() {
        final int result = countOfConnectedInputs.get();
        if (result < 0) {
            throw new IllegalStateException("Negative counter of connected input ports in " + this);
        }
        return result;
    }

    public boolean hasConnectedReadOnlyExecutors() {
        return hasConnectedReadOnlyExecutors;
    }

    public boolean isStandardOutput() {
        return this.block.isStandardOutput();
    }

    @Override
    public ChainOutputPort cleanCopy(ChainBlock newBlock) {
        return new ChainOutputPort(newBlock, id, name, portType, dataType);
    }
}
