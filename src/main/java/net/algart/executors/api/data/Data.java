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

package net.algart.executors.api.data;

import net.algart.external.UsedByNativeCode;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public abstract class Data implements Cloneable {
    public static final long FLAG_INITIALIZED = 0x1L;
    public static final long FLAG_CANCEL_EXECUTION = 0x10L;

    long flags = 0L;

    @UsedByNativeCode
    public long getFlags() {
        return flags;
    }

    @UsedByNativeCode
    public void setFlags(long flags) {
        this.flags = flags;
    }

    @UsedByNativeCode
    public final boolean isInitialized() {
        return (this.flags & FLAG_INITIALIZED) != 0L;
    }

    /**
     * Set's initialized status and <b>frees resources</b> if new value is <tt>false</tt>.
     * @param initialized new initialized status.
     */
    @UsedByNativeCode
    public final void setInitialized(boolean initialized) {
        if (initialized) {
            this.flags |= FLAG_INITIALIZED;
        } else {
            this.flags &= ~FLAG_INITIALIZED;
            this.freeResources();
        }
    }

    public void setInitializedAndResetFlags(boolean initialized) {
        this.flags = 0L;
        setInitialized(initialized);
    }

    public void remove() {
        setInitializedAndResetFlags(false);
    }

    public boolean isFlagCancelExecution() {
        return (this.flags & FLAG_CANCEL_EXECUTION) != 0L;
    }

    public void setFlagCancelExecution(boolean flagCancelExecution) {
        if (flagCancelExecution) {
            this.flags |= FLAG_CANCEL_EXECUTION;
        } else {
            this.flags &= ~FLAG_CANCEL_EXECUTION;
        }
    }

    /**
     * Returns type of this data. Never returns <tt>null</tt>.
     * @return type of this data (non-null).
     */
    @UsedByNativeCode
    public abstract DataType type();

    /**
     * Copies content of <tt>other</tt> to this object. Note: if this object is immutable
     * or considered to be immutable, this method may just copy a reference to data.
     *
     * @param other some other data of the same type.
     */
    public final void setTo(Data other) {
        setTo(other, true);
    }

    /**
     * Copies content of <tt>other</tt> to this object. If <tt>cloneData=true</tt>, equivalent to
     * {@link #setTo(Data)} method.
     *
     * <p><b>Please be very accurate with calling this method with <tt>cloneData=false</tt>.</b>
     * It is allowed only if you are sure that the client will not try to modify the content of the data.
     *
     * @param other some other data of the same type.
     * @param cloneData if <tt>false</tt>, this method performs shallow copying and works as quickly as possible.
     */
    public abstract void setTo(Data other, boolean cloneData);

    /**
     * Completely exchange content of this and other object of the same type.
     * Note: this method works very quickly, unlike <tt>setTo</tt> methods (it never performs data copying).
     *
     * @param other some other data of the same type.
     * @return      reference to this object.
     */
    public abstract Data exchange(Data other);

    /**
     * If this data or part of this data is stored not in usual RAM (for example, in GPU memory),
     * this method must copies it into usual memory. More precisely, this method guarantees
     * that all data, stored in this object, can be freely used in any Java environment, for example, including
     * usage from different Java threads (if they are correctly synchronized by Java synchronization).
     *
     * <p>For example, if this object contains OpenCV <tt>UMat</tt> object, stored in OpenCL GPU,
     * this method should replace <tt>UMat</tt> with <tt>Mat</tt> or some equivalent form like <tt>ByteBuffer</tt>:
     * GPU <tt>UMat</tt>, by default, can be unavailable from other Java threads.
     */
    public void serializeMemory() {
    }

    public JsonObject toJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("type", type().typeName());
        builder.add("initialized", isInitialized());
        return builder.build();
    }

    @Override
    public Data clone() {
        final Data result = type().createEmpty();
        result.setTo(this, true);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + (isInitialized() ? "" : " [NOT INITIALIZED]")
                + (isFlagCancelExecution() ? " [CANCELLING REQUESTED]" : "");
    }

    /**
     * Called from {@link #setInitialized(boolean) setInitialized(false)}.
     * Must allow garbage collection of the resources and
     * free (destroy) external non-Java resources like native OpenCV Mat.
     */
    protected abstract void freeResources();
}
