
package io.automatik.engine.codegen.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Just a holder for informative or random labels that the Generators might want
 * to add
 */
public class DefaultLabeler implements Labeler {

	private final Map<String, String> labels = new HashMap<>();

	public final void addLabel(final String key, final String value) {
		this.labels.put(key, value);
	}

	@Override
	public Map<String, String> generateLabels() {
		return labels;
	}

}
