
package io.automatiko.engine.api.workflow.workitem;

import java.util.List;

/**
 * Defines work item life cycle phase transition. Including data and policies to
 * be enforced during transition.
 *
 * @param <T> type of data the transition is carrying
 */
public interface Transition<T> {

	/**
	 * Returns target phase where work item should be transitioned
	 * 
	 * @return target life cycle phase
	 */
	String phase();

	/**
	 * Optional data to be associated with the transition. This usually means
	 * appending given data into the work item.
	 * 
	 * @return data if given otherwise null
	 */
	T data();

	/**
	 * Optional list of policies to be enforced during transition
	 * 
	 * @return list of policies or an empty list, should never be null
	 */
	List<Policy<?>> policies();
}
