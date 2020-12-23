package io.automatiko.engine.api.internal.io;

import java.io.Serializable;

import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.io.ResourceType;

public interface ResourceTypePackage<T> extends Iterable<T>, Serializable {
	ResourceType getResourceType();

	/**
	 * Remove artifacts inside this ResourceTypePackage which belong to the resource
	 * passed as parameter. Concrete implementation of this interface shall extend
	 * this method in order to properly support incremental KieContainer updates.
	 * 
	 * @param resource
	 * @return true if this ResourceTypePackage mutated as part of this method
	 *         invocation.
	 */
	default boolean removeResource(Resource resource) {
		return false;
	}

	void add(T element);

}