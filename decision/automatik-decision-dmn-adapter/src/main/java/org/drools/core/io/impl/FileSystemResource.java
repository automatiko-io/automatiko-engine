package org.drools.core.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;

public class FileSystemResource implements Resource {
	private io.automatik.engine.services.io.FileSystemResource delegate;

	public FileSystemResource(String location) {
		this.delegate = new io.automatik.engine.services.io.FileSystemResource(location);
	}

	public FileSystemResource(io.automatik.engine.services.io.FileSystemResource resource) {
		this.delegate = resource;
	}

	@Override
	public org.kie.api.io.ResourceType getResourceType() {
		return ResourceType.DMN;
	}

	@Override
	public org.kie.api.io.ResourceConfiguration getConfiguration() {
		return null;
	}

	@Override
	public org.kie.api.io.Resource setSourcePath(String path) {
		return this;
	}

	@Override
	public org.kie.api.io.Resource setTargetPath(String path) {
		return this;
	}

	@Override
	public Resource setResourceType(org.kie.api.io.ResourceType type) {

		return this;
	}

	@Override
	public Resource setConfiguration(org.kie.api.io.ResourceConfiguration conf) {
		return this;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.delegate.getInputStream();
	}

	@Override
	public Reader getReader() throws IOException {
		return this.delegate.getReader();
	}

	@Override
	public String getSourcePath() {
		return this.delegate.getSourcePath();
	}

	@Override
	public String getTargetPath() {
		return this.delegate.getTargetPath();
	}
}
