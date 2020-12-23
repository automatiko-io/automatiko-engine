
package io.automatiko.engine.api;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides direct information about addons installed and available within the
 * running service.
 *
 */
public class Addons {
	/**
	 * Default empty addons instance
	 */
	public static final Addons EMTPY = new Addons(Collections.emptyList());

	private final List<String> availableAddons;

	public Addons(List<String> availableAddons) {
		this.availableAddons = availableAddons;
	}

	/**
	 * Returns all available addons
	 * 
	 * @return returns addons
	 */
	public List<String> availableAddons() {
		return availableAddons;
	}

	@Override
	public String toString() {
		return availableAddons.stream().collect(Collectors.joining(","));
	}
}
