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

package net.algart.executors.api;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import net.algart.external.UsedForExternalCommunication;
import net.algart.json.Jsons;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class ExecutionVisibleResultsInformation {
    public static final String DEFAULT_MODEL = "default";
    public static final String CURRENT_VERSION = "1.0";

    private String model = DEFAULT_MODEL; //"boundaries", "default", "sphere-polyhedra"
    private Port[] ports = new Port[0];

    public ExecutionVisibleResultsInformation() {
    }

    public final int numberOfPorts() {
        return ports.length;
    }

    @UsedForExternalCommunication
    public final String getModel() {
        return model;
    }

    public final ExecutionVisibleResultsInformation setModel(String model) {
        this.model = model;
        return this;
    }

    @UsedForExternalCommunication
    public final Port[] getPorts() {
        return ports.clone();
    }

    public final ExecutionVisibleResultsInformation setPorts(Port... ports) {
        Objects.requireNonNull(ports, "Null ports");
        this.ports = Stream.of(ports).filter(Objects::nonNull).toArray(Port[]::new);
        return this;
    }

    public final ExecutionVisibleResultsInformation addPorts(Port... ports) {
        Objects.requireNonNull(ports, "Null ports");
        ports = Stream.of(ports).filter(Objects::nonNull).toArray(Port[]::new);
        final int currentLength = this.ports.length;
        this.ports = Arrays.copyOf(this.ports, currentLength + ports.length);
        System.arraycopy(ports, 0, this.ports, currentLength, ports.length);
        return this;
    }

    @UsedForExternalCommunication
    public String jsonString() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("model", model);
        builder.add("version", CURRENT_VERSION);
        final JsonArrayBuilder portsBuilder = Json.createArrayBuilder();
        for (Port port : ports) {
            portsBuilder.add(port.toJson());
        }
        builder.add("ports", portsBuilder.build());
        return Jsons.toPrettyString(builder.build());
    }
}
