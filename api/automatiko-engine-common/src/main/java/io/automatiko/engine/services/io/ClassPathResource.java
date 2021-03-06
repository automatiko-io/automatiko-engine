package io.automatiko.engine.services.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.io.ResourceType;
import io.automatiko.engine.services.utils.StringUtils;

public class ClassPathResource extends BaseResource implements InternalResource {

	private String path;
	private String encoding;
	private ClassLoader classLoader;
	private Class<?> clazz;

	public ClassPathResource() {

	}

	public ClassPathResource(String path) {
		this(path, null, null, null);
	}

	public ClassPathResource(String path, Class<?> clazz) {
		this(path, null, clazz, null);
	}

	public ClassPathResource(String path, ClassLoader classLoader) {
		this(path, null, null, classLoader);
	}

	public ClassPathResource(String path, String encoding) {
		this(path, encoding, null, null);
	}

	public ClassPathResource(String path, String encoding, Class<?> clazz) {
		this(path, encoding, clazz, null);
	}

	public ClassPathResource(String path, String encoding, ClassLoader classLoader) {
		this(path, encoding, null, classLoader);
	}

	public ClassPathResource(String path, String encoding, Class<?> clazz, ClassLoader classLoader) {
		if (path == null) {
			throw new IllegalArgumentException("path cannot be null");
		}
		this.path = path;
		this.encoding = encoding;
		this.clazz = clazz;
		this.classLoader = classLoader == null ? this.getClass().getClassLoader() : classLoader;
		setSourcePath(path);
		setResourceType(ResourceType.determineResourceType(path));
	}

	/**
	 * This implementation opens an InputStream for the given class path resource.
	 * 
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.lang.Class#getResourceAsStream(String)
	 */
	public InputStream getInputStream() throws IOException {
		return bytes != null ? new ByteArrayInputStream(this.bytes) : this.getURL().openStream();
	}

	/**
	 * This implementation returns a URL for the underlying class path resource.
	 * 
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	public URL getURL() throws IOException {
		URL url = null;
		if (this.clazz != null) {
			url = this.clazz.getResource(this.path);
		}

		if (url == null) {
			url = this.classLoader.getResource(this.path);
		}

		if (url == null) {
			throw new FileNotFoundException("'" + this.path + "' cannot be opened because it does not exist");
		}
		return url;
	}

	public boolean hasURL() {
		return true;
	}

	public String getEncoding() {
		return encoding;
	}

	public Reader getReader() throws IOException {
		if (this.encoding != null) {
			return new InputStreamReader(getInputStream(), encoding);
		} else {
			return new InputStreamReader(getInputStream(), StandardCharsets.UTF_8);
		}
	}

	public boolean isDirectory() {
		try {
			URL url = getURL();

			if (!"file".equals(url.getProtocol())) {
				return false;
			}

			File file = new File(StringUtils.toURI(url.toString()).getSchemeSpecificPart());

			return file.isDirectory();
		} catch (Exception e) {
			return false;
		}
	}

	public Collection<Resource> listResources() {
		try {
			URL url = getURL();

			if ("file".equals(url.getProtocol())) {
				File dir = new File(StringUtils.toURI(url.toString()).getSchemeSpecificPart());

				List<Resource> resources = new ArrayList<Resource>();

				for (File file : dir.listFiles()) {
					resources.add(new FileSystemResource(file));
				}

				return resources;
			}
		} catch (Exception e) {
			// swollow as we'll throw an exception anyway
		}

		throw new RuntimeException("This Resource cannot be listed, or is not a directory");
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Class<?> getClazz() {
		return this.clazz;
	}

	public String getPath() {
		return path;
	}

	public boolean equals(Object object) {
		if (!(object instanceof ClassPathResource)) {
			return false;
		}

		ClassPathResource other = (ClassPathResource) object;
		return this.path.equals(other.path) && this.clazz == other.clazz && this.classLoader == other.classLoader;
	}

	public int hashCode() {
		return this.path.hashCode();
	}

	public String toString() {
		return "ClassPathResource[path=" + this.path + "]";
	}

}