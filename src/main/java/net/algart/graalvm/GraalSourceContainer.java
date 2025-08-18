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

package net.algart.graalvm;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.lang.System.Logger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class GraalSourceContainer {
    public static final String JAVASCRIPT_LANGUAGE = "js";

    private static final Logger LOG = System.getLogger(GraalSourceContainer.class.getName());

    public enum SourceKind {
        LITERAL(Literal::new),
        BYTE_SEQUENCE(ForByteSequence::new),
        FILE(ForFile::new),
        URL(ForURL::new),
        READER(ForReader::new);

        private final Supplier<GraalSourceContainer> creator;

        SourceKind(Supplier<GraalSourceContainer> creator) {
            this.creator = creator;
        }
    }

    private final AtomicBoolean changed = new AtomicBoolean(false);
    private volatile String language = null;
    private volatile Object origin = null;
    private volatile String name = null;
    private volatile String mimeType = null;
    private volatile Charset fileEncoding = null;
    private volatile Source source = null;

    GraalSourceContainer() {
    }

    public static GraalSourceContainer newContainer(SourceKind sourceKind) {
        Objects.requireNonNull(sourceKind, "Null source kind");
        return sourceKind.creator.get();
    }

    public static GraalSourceContainer newLiteralContainer() {
        return newContainer(SourceKind.LITERAL);
    }

    public static GraalSourceContainer newFileContainer() {
        return newContainer(SourceKind.FILE);
    }

    public GraalSourceContainer setCommonJS(CharSequence script) {
        return setJS(GraalJSType.COMMON, script);
    }

    public GraalSourceContainer setModuleJS(CharSequence script, String name) {
        return setJS(GraalJSType.MODULE, script, name);
    }

    public GraalSourceContainer setModuleJS(Path scriptFile, String name) {
        return setJS(GraalJSType.MODULE, scriptFile, name);
    }

    public GraalSourceContainer setJS(GraalJSType type, CharSequence script) {
        return setJS(type, script, null);
    }

    public GraalSourceContainer setJS(GraalJSType type, CharSequence script, String name) {
        Objects.requireNonNull(type, "Null type");
        type.configure(this, script, name);
        return this;
    }

    public GraalSourceContainer setJS(GraalJSType type, Path scriptFile, String name) {
        Objects.requireNonNull(type, "Null type");
        type.configure(this, scriptFile, name);
        return this;
    }

    public boolean isChanged() {
        return changed.get();
    }

    public boolean setChanged(boolean changed) {
        return this.changed.getAndSet(changed);
    }

    /**
     * Returns <code>true</code> if some settings were changed after last call of this method
     * or calling {@link #setChanged(boolean) setChanged(false)}.
     * This method also clears "changed" status to <code>false</code>.
     *
     * <p>Note: if this method returns <code>true</code>, it may be a reason to create again also the performer,
     * in particular, in a case of ECMA modules (inside the same context, the content of a module
     * is executed only once).</p>
     *
     * @return whether this container's settings were changed.
     */
    public boolean changed() {
        return setChanged(false);
    }

    public String getLanguage() {
        return language;
    }

    public GraalSourceContainer setLanguage(String language) {
        clearCache(!Objects.equals(this.language, language));
        this.language = language;
        return this;
    }

    public Object getOrigin() {
        return origin;
    }

    /**
     * Sets the script origin: string, byte sequence, File object, etc. (see Source.newBuilder methods).
     *
     * <p><b>Note:</b> this method also sets the name!
     *
     * @param origin new script origin.
     * @return <code>true</code> if new origin differs from an existing one; <code>false</code> if is not changed.
     */
    public GraalSourceContainer setOrigin(Object origin, String name) {
        if (origin != null && !isOriginCorrect(origin)) {
            throw new IllegalArgumentException("Illegal type of source origin (" + origin.getClass() + ")");
        }
        clearCache(!Objects.equals(this.origin, origin));
        this.origin = origin;
        return setName(name);
    }

    public String getName() {
        return name;
    }

    public GraalSourceContainer setName(String name) {
        clearCache(!Objects.equals(this.name, name));
        this.name = name;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public GraalSourceContainer setMimeType(String mimeType) {
        clearCache(!Objects.equals(this.mimeType, mimeType));
        this.mimeType = mimeType;
        return this;
    }

    public Charset getFileEncoding() {
        return fileEncoding;
    }

    public GraalSourceContainer setFileEncoding(Charset fileEncoding) {
        clearCache(!Objects.equals(this.fileEncoding, fileEncoding));
        this.fileEncoding = fileEncoding;
        return this;
    }

    public Source source() {
        Source source = this.source;
        if (source == null) {
            source = createSource();
            // - exception possible
            this.source = source;
            LOG.log(Logger.Level.TRACE, "Creating new Graal source: " + source);
        }
        return source;
    }

    abstract boolean isOriginCorrect(Object origin);

    abstract Source.Builder newBuilder(String language, Object origin);

    private Source createSource() {
        final Object origin = this.origin;
        final String language = this.language;
        if (origin == null || language == null) {
            throw new IllegalStateException("Source container is not initialized: source origin is not set");
        }
        final Source.Builder builder = newBuilder(language, origin);
        // - always sets name
        builder.mimeType(mimeType);
        builder.encoding(fileEncoding);
        if (this instanceof Literal) {
            return builder.buildLiteral();
        } else {
            try {
                return builder.build();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
    }

    private void clearCache(boolean changed) {
        if (changed) {
            setChanged(true);
            source = null;
        }
    }

    static class Literal extends GraalSourceContainer {
        Literal() {
            super();
        }

        @Override
        boolean isOriginCorrect(Object origin) {
            return origin instanceof CharSequence;
        }

        @Override
        Source.Builder newBuilder(String language, Object origin) {
            return Source.newBuilder(language, (CharSequence) origin, getName());
        }
    }

    static class ForByteSequence extends GraalSourceContainer {
        ForByteSequence() {
            super();
        }

        @Override
        boolean isOriginCorrect(Object origin) {
            return origin instanceof ByteSequence;
        }

        @Override
        Source.Builder newBuilder(String language, Object origin) {
            return Source.newBuilder(language, (ByteSequence) origin, getName());
        }
    }

    static class ForFile extends GraalSourceContainer {
        ForFile() {
            super();
        }

        @Override
        boolean isOriginCorrect(Object origin) {
            return origin instanceof File || origin instanceof Path;
        }

        @Override
        Source.Builder newBuilder(String language, Object origin) {
            if (!isOriginCorrect(origin)) {
                throw new ClassCastException("Illegal type of source origin (" + origin.getClass() + ")");
            }
            final File file = origin instanceof Path path ? path.toFile() : (File) origin;
            return Source.newBuilder(language, file).name(getName());
        }
    }

    static class ForURL extends GraalSourceContainer {
        ForURL() {
            super();
        }

        @Override
        boolean isOriginCorrect(Object origin) {
            return origin instanceof URL;
        }

        @Override
        Source.Builder newBuilder(String language, Object origin) {
            return Source.newBuilder(language, (URL) origin).name(getName());
        }
    }

    static class ForReader extends GraalSourceContainer {
        ForReader() {
            super();
        }

        @Override
        boolean isOriginCorrect(Object origin) {
            return origin instanceof URL;
        }

        @Override
        Source.Builder newBuilder(String language, Object origin) {
            return Source.newBuilder(language, (Reader) origin, getName());
        }
    }
}
