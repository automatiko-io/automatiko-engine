package io.automatik.engine.services.io;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import io.automatik.engine.api.io.Resource;
import io.automatik.engine.services.utils.IoUtils;

public class ByteArrayResource extends BaseResource implements InternalResource {

	private String encoding;

	public ByteArrayResource() {
	}

	public ByteArrayResource(byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("Provided byte array can not be null");
		}
		this.bytes = bytes;
	}

	public ByteArrayResource(byte[] bytes, String encoding) {
		this(bytes);
		this.encoding = encoding;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.bytes);
	}

	public Reader getReader() throws IOException {
		if (this.encoding != null) {
			return new InputStreamReader(getInputStream(), encoding);
		} else {
			return new InputStreamReader(getInputStream(), IoUtils.UTF8_CHARSET);
		}
	}

	@Override
	public byte[] getBytes() {
		return bytes;
	}

	public boolean hasURL() {
		return false;
	}

	public URL getURL() throws IOException {
		throw new FileNotFoundException("byte[] cannot be resolved to URL");
	}

	public boolean isDirectory() {
		return false;
	}

	public Collection<Resource> listResources() {
		throw new RuntimeException("This Resource cannot be listed, or is not a directory");
	}

	public boolean equals(Object object) {
		return (object == this || (object instanceof ByteArrayResource
				&& Arrays.equals(((ByteArrayResource) object).bytes, this.bytes)));
	}

	public int hashCode() {
		return Arrays.hashCode(this.bytes);
	}

	public String toString() {
		return "ByteArrayResource[bytes=" + firstNBytesToString(10) + ", encoding=" + this.encoding + "]";
	}

	private String firstNBytesToString(int nrOfBytes) {
		// this.bytes cannot be empty or null (enforced by constructors)
		String str = Arrays.toString(Arrays.copyOf(this.bytes, Math.min(this.bytes.length, nrOfBytes)));
		// append the dots ("...") only if the resource has more bytes than requested
		return this.bytes.length > nrOfBytes ? str.substring(0, str.length() - 1) + ", ...]" : str;
	}

}
