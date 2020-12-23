
package io.automatiko.engine.api;

import java.util.Map;

/**
 * Represents data model type of objects that are usually descriptor of data
 * holders.
 *
 */
public interface Model {

	/**
	 * Returns model representation as map of members of this model type
	 * 
	 * @return non null map of data extracted from the model
	 */
	Map<String, Object> toMap();

	void fromMap(Map<String, Object> params);
}
