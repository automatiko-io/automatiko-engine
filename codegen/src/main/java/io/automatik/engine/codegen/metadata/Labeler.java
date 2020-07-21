
package io.automatik.engine.codegen.metadata;

import java.util.Map;

/**
 * Base interface for providing labels to Generators
 */
public interface Labeler {

	/**
	 * Will create the labels specified by the given Labeler
	 */
	Map<String, String> generateLabels();

}
