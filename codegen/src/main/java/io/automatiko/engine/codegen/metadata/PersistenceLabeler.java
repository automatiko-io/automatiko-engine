
package io.automatiko.engine.codegen.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for add persistence related labels to the image metadata
 */
public class PersistenceLabeler implements Labeler {

	static final String PERSISTENCE_LABEL_PREFIX = ImageMetaData.LABEL_PREFIX + "persistence/required";
	private final Map<String, String> labels = new HashMap<>();

	public PersistenceLabeler() {
		labels.put(PERSISTENCE_LABEL_PREFIX, "true");
	}

	@Override
	public Map<String, String> generateLabels() {
		return labels;
	}

}
