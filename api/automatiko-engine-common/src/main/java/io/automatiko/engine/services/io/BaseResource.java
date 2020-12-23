package io.automatiko.engine.services.io;

import static io.automatiko.engine.services.utils.IoUtils.readBytesFromInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.io.ResourceConfiguration;
import io.automatiko.engine.api.io.ResourceType;

public abstract class BaseResource implements InternalResource {
	private ResourceType resourceType;

	private String sourcePath;
	private String targetPath;
	private String description;

	private List<String> categories;

	protected byte[] bytes;

	public InternalResource setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
		return this;
	}

	public ResourceType getResourceType() {
		return this.resourceType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	@Override
	public ResourceConfiguration getConfiguration() {
		return null;
	}

	@Override
	public Resource setConfiguration(ResourceConfiguration conf) {
		return null;
	}

	public InternalResource setSourcePath(String path) {
		this.sourcePath = path;
		return this;
	}

	public InternalResource setTargetPath(String path) {
		this.targetPath = path;
		return this;
	}

	public List<String> getCategories() {
		if (categories == null) {
			categories = new ArrayList<String>();
		}
		return categories;
	}

	public void setCategories(String categories) {
		List<String> list = getCategories();
		list.clear();
		if (categories != null) {
			StringTokenizer tok = new StringTokenizer(categories, ",");
			while (tok.hasMoreTokens()) {
				list.add(tok.nextToken());
			}
		}
	}

	public void addCategory(String tag) {
		getCategories().add(tag);
	}

	public byte[] getBytes() {
		if (bytes == null) {
			try {
				bytes = readBytesFromInputStream(getInputStream());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return bytes;
	}

	@Override
	public String toString() {
		return getSourcePath();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Resource)) {
			return false;
		}
		Resource that = (Resource) o;
		return sourcePath != null ? sourcePath.equals(that.getSourcePath()) : that.getSourcePath() == null;

	}

	@Override
	public int hashCode() {
		return sourcePath != null ? sourcePath.hashCode() : 0;
	}
}