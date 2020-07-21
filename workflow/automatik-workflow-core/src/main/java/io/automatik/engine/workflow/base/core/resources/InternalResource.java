package io.automatik.engine.workflow.base.core.resources;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import io.automatik.engine.api.io.Resource;
import io.automatik.engine.api.io.ResourceType;

public interface InternalResource extends Resource {
	InternalResource setResourceType(ResourceType resourceType);

	ResourceType getResourceType();

	URL getURL() throws IOException;

	boolean hasURL();

	boolean isDirectory();

	Collection<Resource> listResources();

	String getDescription();

	void setDescription(String description);

	List<String> getCategories();

	void setCategories(String categories);

	void addCategory(String category);

	byte[] getBytes();

	String getEncoding();
}
