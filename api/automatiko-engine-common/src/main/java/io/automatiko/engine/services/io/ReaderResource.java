package io.automatiko.engine.services.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;

import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.io.ResourceType;

public class ReaderResource extends BaseResource implements InternalResource {
    private static final long serialVersionUID = -2554750160404141191L;

    private transient Reader reader;
    private String encoding;

    public ReaderResource() {
    }

    public ReaderResource(Reader reader) {
        this(reader, null, null);
    }

    public ReaderResource(Reader reader, ResourceType type) {
        this(reader, null, type);
    }

    public ReaderResource(Reader reader, String encoding) {
        this(reader, encoding, null);
    }

    public ReaderResource(Reader reader, String encoding, ResourceType type) {
        if (reader == null) {
            throw new IllegalArgumentException("reader cannot be null");
        }
        if (encoding == null && reader instanceof InputStreamReader) {
            this.encoding = ((InputStreamReader) reader).getEncoding();
        } else {
            this.encoding = encoding;
        }
        this.reader = reader;

        setResourceType(type);
    }

    public URL getURL() throws IOException {
        throw new FileNotFoundException("reader cannot be resolved to URL");
    }

    public InputStream getInputStream() throws IOException {
        try {
            // try to reset the reader.
            this.reader.reset();
        } catch (IOException ioe) {
            // nothing to do... intentionally left empty
        }
        if (this.encoding != null) {
            return new ReaderInputStream.NonCloseable(this.reader, this.encoding);
        } else {
            return new ReaderInputStream.NonCloseable(this.reader);
        }
    }

    public Reader getReader() {
        return this.reader;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public boolean isDirectory() {
        return false;
    }

    public Collection<Resource> listResources() {
        throw new RuntimeException("This Resource cannot be listed, or is not a directory");
    }

    public boolean hasURL() {
        return false;
    }

    public String toString() {
        return "ReaderResource[resource=" + this.reader + ",encoding=" + this.encoding + "]";
    }
}
