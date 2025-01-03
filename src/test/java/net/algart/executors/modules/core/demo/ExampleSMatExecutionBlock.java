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

package net.algart.executors.modules.core.demo;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.ExecutionVisibleResultsInformation;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.Data;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.SMat;

import java.nio.ByteBuffer;

/**
 * Simple execution block example: copies input image.
 */
public final class ExampleSMatExecutionBlock extends ExecutionBlock {
    private static final System.Logger LOG = System.getLogger(ExampleSMatExecutionBlock.class.getName());
    private String operation = "zeros";

    @Override
    public void execute() {
        //getting input / output port
        Port input = getInputPort("image");
        Port output = getOutputPort("image");

        if (!input.getDataType().typeName().equals("mat")) {
            throw new IllegalArgumentException("unsupported input data type");
        }

        if (!output.getDataType().typeName().equals("mat")) {
            throw new IllegalArgumentException("unsupported output data type");
        }

        // getting container with data
        Data inData = input.getData();
        Data outData = output.getData();

        // also, a type of data container can be received
        if (inData.type() != DataType.MAT) {
            throw new IllegalArgumentException("Unsupported input data type");
        }

        // because of data type already checked, we can cast Data into a special case of the data type
        SMat mat = (SMat) inData;

        // extract byte buffer from data container
        ByteBuffer inByteBuffer = mat.getByteBuffer();
        if (inByteBuffer == null) {
            throw new IllegalArgumentException("input byte buffer is null");
        }

        // finally, make some logic operations over input data and put the result into output

        inByteBuffer.rewind();

        // here we just make a copy of input data and store it into output
        ByteBuffer outByteBuffer = ByteBuffer.allocateDirect(mat.getByteBuffer().capacity());
        if (operation.equals("copy")) {
            while (inByteBuffer.position() < inByteBuffer.limit()) {
                outByteBuffer.put(inByteBuffer.get());
            }
        }

        // fill output port data, and this data can be passed to another execution block
        SMat outMat = (SMat) outData;
        outMat.setAll(
                mat.getDimensions(), mat.getDepth(), mat.getNumberOfChannels(),
                outByteBuffer, false);
        output.setData(outMat);
        LOG.log(System.Logger.Level.INFO, "Operation code: " + operation);
    }

    @Override
    public ExecutionVisibleResultsInformation visibleResultsInformation() {
        return new ExecutionVisibleResultsInformation().setPorts(getOutputPort("image"));
    }

    @Override
    public void onChangeParameter(String name) {
        if (name.equals("operation")) {
            operation = parameters().getString(name);
        }
    }
}
