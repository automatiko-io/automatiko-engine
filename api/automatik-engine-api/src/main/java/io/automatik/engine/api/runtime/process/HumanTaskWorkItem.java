
package io.automatik.engine.api.runtime.process;

import java.util.Set;

/**
 * Dedicated extension to WorkItem to cover needs of human tasks
 *
 */
public interface HumanTaskWorkItem extends WorkItem {

	/**
	 * Returns name of the task
	 * 
	 * @return task name
	 */
	String getTaskName();

	/**
	 * Returns optional description of the task
	 * 
	 * @return task description if present
	 */
	String getTaskDescription();

	/**
	 * Returns optional priority of the task
	 * 
	 * @return task priority if present
	 */
	String getTaskPriority();

	/**
	 * Returns reference name of the task
	 * 
	 * @return task reference
	 */
	String getReferenceName();

	/**
	 * Returns actual owner assigned to the task
	 * 
	 * @return task actual owner
	 */
	String getActualOwner();

	/**
	 * Returns potential users that can work on this task
	 * 
	 * @return potential users
	 */
	Set<String> getPotentialUsers();

	/**
	 * Returns potential groups that can work on this task
	 * 
	 * @return potential groups
	 */
	Set<String> getPotentialGroups();

	/**
	 * Returns admin users that can administer this task
	 * 
	 * @return admin users
	 */
	Set<String> getAdminUsers();

	/**
	 * Returns admin groups that can administer this task
	 * 
	 * @return admin groups
	 */
	Set<String> getAdminGroups();

	/**
	 * Returns excluded users that cannot work on this task
	 * 
	 * @return excluded users
	 */
	Set<String> getExcludedUsers();
}
