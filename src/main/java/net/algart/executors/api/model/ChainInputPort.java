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

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.Executor;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.SScalar;

public final class ChainInputPort extends ChainPort<ChainOutputPort> {
    private static final boolean OPTIMIZE_COPYING_DATA = true;

    private ChainInputPort(ChainBlock block, String id, String name, ChainPortType portType, DataType dataType) {
        super(block, id, name, portType, dataType);
        if (portType.actualPortType() != Port.Type.INPUT) {
            throw new IllegalArgumentException("Non-input port type");
        }
    }

    public static ChainInputPort newInstance(
            ChainBlock block,
            String id,
            String name,
            ChainPortType portType,
            DataType dataType) {
        return new ChainInputPort(block, id, name, portType, dataType);
    }

    public static ChainInputPort valueOf(ChainBlock block, ChainJson.ChainBlockConf.PortConf portConf) {
        return newInstance(
                block,
                portConf.getUuid(),
                portConf.getName(),
                portConf.getPortType(),
                portConf.getDataType());
    }

    public static ChainInputPort valueOf(ChainBlock block, ExecutorJson.PortConf portConf) {
        return newInstance(
                block,
                null,
                portConf.getName(),
                ChainPortType.INPUT_PORT,
                portConf.getValueType());
    }

    public Boolean necessary() {
        if (portType.isVirtual()) {
            return null;
        }
        final Executor executor = block.executor;
        // Here we use executor directly to allow calling this method
        // even when executors are not available. It cannot lead to any problems:
        // optimization, provided by this flag, cannot be used without actual executors in any case.
        if (executor == null) {
            return null;
        }
        final Port executorPort = executor.getInputPort(this.name);
        return executorPort == null ? null : executor.checkInputNecessary(executorPort);
    }

    public void copyToExecutorPort() {
        final ExecutionBlock executor = block.getExecutor();
        switch (portType) {
            case INPUT_PORT -> {
                synchronized (chain.blocksInteractionLock) {
                    // exchanging/moving data between all ports blocks must be synchronized globally
                    executor.getRequiredInputPort(this.name).setData(data);
                    // - copying reference to data, not content
                }
            }
            case INPUT_CONTROL_AS_PORT -> {
                final String value;
                synchronized (chain.blocksInteractionLock) {
                    if (!(data instanceof SScalar)) {
                        throw new IllegalArgumentException("Invalid port: virtual data port contains "
                                + "non-scalar data " + data + " (" + this + ")");
                        // - should not occur: it was already checked in ChainJson.ChainBlockConf.PortConf constructor
                    }
                    // exchanging/moving data between all ports blocks must be synchronized globally
                    if (!data.isInitialized()) {
                        // don't try to override parameter from non-initialized port
                        break;
                    }
                    value = ((SScalar) data).getValue();
                }
                final ChainProperty chainProperty = block.properties.get(this.name);
                if (chainProperty == null) {
                    // - abnormal situation: virtual port without actual control;
                    // we prefer not to disable this in the constructor, but just to set the usual scalar value
                    executor.parameters().put(this.name, value);
                } else {
                    chainProperty.getType().setParameter(executor.parameters(), this.name, value);
                }
                executor.onChangeParameter(this.name);
            }
            default -> {
                throw new AssertionError("Unknown input port type: " + portType);
            }
        }
    }

    public void copyFromConnectedPort() {
        synchronized (chain.blocksInteractionLock) {
            // Exchanging/moving data between all ports blocks must be synchronized globally
            final ChainOutputPort connectedSource = connectedOutputPort();
            final int countOfConnectedInputs = connectedSource.reduceCountOfConnectedInputs();
            // - note: reduce the counter in any case!
//            if (true) {
//                this.data.setTo(connectedSource.getData(), true);
//                return;
//            }
            final boolean hasConnectedReadOnlyExecutors = connectedSource.hasConnectedReadOnlyExecutors();
            if (OPTIMIZE_COPYING_DATA
                    && !hasConnectedReadOnlyExecutors
                    && countOfConnectedInputs == 0
                    && !connectedSource.isStandardOutput()) {
                // Note: if connected source port has connected read-only executors,
                // we must not use this "exchange" technique at all.
                // In this case, we will copy the reference for some links (shallow copy without cloning),
                // and exchanging the last link with non-read-only executor will lead to damaging data
                // while multithreading execution.
                // Also note: we need to preserve all standard outputs if they are connected (abnormal,
                // but possible situation): they are the final results of the chain and will
                // be read from "standard-output" ports.
//                System.out.println("!!! Exchange with " + block.getExecutor().getClass().getSimpleName());
                this.data.exchange(connectedSource.getData());
            } else {
                final boolean shallowCopy = OPTIMIZE_COPYING_DATA
                        && block.getExecutor().isReadOnlyInput() && hasConnectedReadOnlyExecutors;
                // We can skip cloning for read-only executors. But we must be sure
                // that "hasConnectedReadOnlyExecutors" flag is also set: it disables
                // final data exchange for the last connection (when countOfConnectedInputs == 0).
                // Of course, "hasConnectedReadOnlyExecutors" should be true if this block
                // is read-only, but it is theoretically possible that isReadOnly() method
                // returns dynamic result, depending on the executor settings.
//                System.out.println("!!! Copying to " + block.getExecutor().getClass()
//                        + (shallowCopy ? ": reference only" : ": deep"));
                this.data.setTo(connectedSource.getData(), !shallowCopy);
                // Note: even for read-only-input executors, we cannot just copy a reference this.data:
                // it can lead to errors as a result of exchanging contents (data.exchange).
            }
        }
    }

    public ChainBlock connectedSourceBlock() {
        return connectedOutputPort().block;
    }

    @Override
    public ChainInputPort cleanCopy(ChainBlock newBlock) {
        return new ChainInputPort(newBlock, id, name, portType, dataType);
    }

    ChainOutputPort connectedOutputPort() {
        if (!isConnected()) {
            throw new IllegalStateException("No connected ports");
        }
        if (connected.size() != 1) {
            throw new AssertionError("Internal error! Input port has >1 connections: " + this);
        }
        return connected.values().iterator().next();
    }
}
