
package io.automatik.engine.api.workflow.workitem;

/**
 * Top level of a policy that should be applied to work items. Most of the cases
 * it is used to restrict access or operations on top of the work item.
 *
 * @param <T> type of the policy object to be used to react to it.
 */
public interface Policy<T> {

	/**
	 * Actual type of policy data used to enforce this policy
	 * 
	 * @return policy data
	 */
	T value();
}
