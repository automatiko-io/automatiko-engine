
package io.automatik.engine.api.uow;

import io.automatik.engine.api.event.EventManager;

/**
 * Manager that controls and give access to UnitOfWork.
 * 
 * Main entry point for application usage to gain control about the execution
 * and grouping of work.
 *
 */
public interface UnitOfWorkManager {

	/**
	 * Returns current unit of work for this execution context (usually thread).
	 * 
	 * @return current unit of work
	 */
	UnitOfWork currentUnitOfWork();

	/**
	 * Returns new not started UnitOfWork that is associated with the manager to
	 * manage it's life cycle.
	 * 
	 * @return new, not started unit of work
	 */
	UnitOfWork newUnitOfWork();

	/**
	 * Returns instance of the event manager configured for this unit of work
	 * manager
	 * 
	 * @return event manager instance
	 */
	EventManager eventManager();
}
