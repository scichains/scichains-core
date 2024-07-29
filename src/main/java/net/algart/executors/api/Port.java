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

package net.algart.executors.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.DataType;
import net.algart.external.UsedForExternalCommunication;

import java.util.Objects;
import java.util.UUID;

/**
 * @author mnogono
 * Created on 11.05.2017.
 */
public final class Port {
    public enum Type {
        INPUT(1, "input") {
            @Override
            public Port getPort(ExecutionBlock executionBlock, String name) {
                return executionBlock.getInputPort(name);
            }
        },
        OUTPUT(2, "output") {
            @Override
            public Port getPort(ExecutionBlock executionBlock, String name) {
                return executionBlock.getOutputPort(name);
            }
        };

        private final int code;
        private final String typeName;

        Type(int code, String typeName) {
            this.code = code;
            this.typeName = typeName;
        }

        public final int code() {
            return code;
        }

        public final String typeName() {
            return typeName;
        }

        public static Type valueOf(int code) {
            switch (code) {
                case 1:
                    return INPUT;
                case 2:
                    return OUTPUT;
            }
            throw new IllegalArgumentException("Unsupported type code: " + code);
        }

        public abstract Port getPort(ExecutionBlock executionBlock, String name);
    }

    @UsedForExternalCommunication
    private Port() {
    }

    /**
     * port name
     */
    @UsedForExternalCommunication
    private String name;

    /**
     * port data
     */
    @UsedForExternalCommunication
    private Data data;

    /**
     * input / output port
     */
    @UsedForExternalCommunication
    private Type portType;

    /**
     * port data type
     */
    @UsedForExternalCommunication
    private DataType dataType;

    /**
     * port uuid
     */
    private UUID uuid;

    /**
     * flag shows does current port has connection with other port(s)
     * connected flag modify by c++ host application
     */
    @UsedForExternalCommunication
    private boolean connected = false;

    public String getName() {
        return name;
    }

    @UsedForExternalCommunication
    public Port setName(String name) {
        this.name = Objects.requireNonNull(name, "Null port name");
        return this;
    }

    public Data getData() {
        return data;
    }

    @UsedForExternalCommunication
    public Port setData(Data portData) {
        this.data = Objects.requireNonNull(portData, "Null port data");
        return this;
    }

    public Type getPortType() {
        return portType;
    }

    public Port setPortType(Type portType) {
        this.portType = Objects.requireNonNull(portType, "Null type");
        return this;
    }

    @UsedForExternalCommunication
    public Port setPortTypeCode(int code) {
        this.portType = Type.valueOf(code);
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Port setDataType(DataType dataType) {
        this.dataType = Objects.requireNonNull(dataType, "Null data type");
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    @UsedForExternalCommunication
    public Port setUuid(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "Null uuid");
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isInput() {
        return portType == Type.INPUT;
    }

    public boolean isOutput() {
        return portType == Type.OUTPUT;
    }

    @UsedForExternalCommunication
    public Port setConnected(boolean connected) {
        this.connected = connected;
        return this;
    }

    public <T extends Data> T getData(Class<? extends T> dataClass) {
        return getData(dataClass, false);
    }

    public <T extends Data> T getData(Class<? extends T> dataClass, boolean allowUninitializedData) {
        Objects.requireNonNull(dataClass, "Null requested data class");
        final Data result = getData();
        if (result == null) {
            throw new IllegalArgumentException("The " + portType.typeName + " port \""
                    + name + "\" has no data container");
            // External execution system should set up both input and output for any execution block
            // with suitable data type. Note: Data is only a container, that is usually filled
            // by actual data in execute() method.
        }
        if (!allowUninitializedData && !result.isInitialized()) {
            throw new IllegalArgumentException("The " + portType.typeName + " port \""
                    + name + "\" has no initialized data");
        }
        if (!dataClass.isInstance(result)) {
            throw new IllegalArgumentException("Data in the " + portType.typeName + " port \""
                    + name + "\" is not an instance of " + dataClass.getSimpleName()
                    + " (it is " + result.getClass().getSimpleName() + ")");
        }
        return dataClass.cast(result);
    }

    public boolean hasData() {
        final Data data = this.data;
        return data != null && data.isInitialized();
        // - "!= null" to be on the safe side (while correct usage, it must be non-null)
    }

    public void removeData() {
        final Data data = this.data;
        if (data != null) {
            // - "!= null" to be on the safe side (while correct usage, it must be non-null)
            data.remove();
        }
    }

    public boolean isCancellingExecutionRequested() {
        final Data data = this.data;
        return data != null && data.isFlagCancelExecution();
        // - "!= null" to be on the safe side (while correct usage, it must be non-null)
    }

    public void requestCancellingExecution() {
        final Data data = this.data;
        if (data != null) {
            // - "!= null" to be on the safe side (while correct usage, it must be non-null)
            data.setFlagCancelExecution(true);
        }
    }

    public void requestContinuingExecution() {
        final Data data = this.data;
        if (data != null) {
            // - "!= null" to be on the safe side (while correct usage, it must be non-null)
            data.setFlagCancelExecution(false);
        }
    }

    public JsonObject toJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (name != null) {
            builder.add("name", name);
        }
        if (portType != null) {
            builder.add("type", portType.typeName());
        }
        if (data != null) {
            builder.add("data", data.toJson());
        }
        if (uuid != null) {
            builder.add("id", uuid.toString());
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "Port \"" + name + "\" [" + portType + " for " + dataType + "], "
                + (connected ? "connected" : "unconnected")
                + ": " + data
                + (uuid == null ? "" : " [uuid " + uuid + "]");
    }

    public static Port newInput(String name, Data data) {
        Objects.requireNonNull(name, "Null port name");
        Objects.requireNonNull(name, "Null port data");
        return new Port().setPortType(Type.INPUT).setName(name)
                .setDataType(data.type()).setData(data);
    }

    public static Port newInput(String name, DataType dataType) {
        Objects.requireNonNull(name, "Null port name");
        Objects.requireNonNull(dataType, "Null data type");
        return new Port().setPortType(Type.INPUT).setName(name).setDataType(dataType).setData(dataType.createEmpty());
    }

    public static Port newOutput(String name, DataType dataType) {
        Objects.requireNonNull(name, "Null port name");
        Objects.requireNonNull(dataType, "Null data type");
        return new Port().setPortType(Type.OUTPUT).setName(name)
                .setDataType(dataType).setData(dataType.createEmpty());
    }
}
