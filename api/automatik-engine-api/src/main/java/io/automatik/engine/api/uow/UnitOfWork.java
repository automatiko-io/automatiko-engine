
package io.automatik.engine.api.uow;

/**
 * Unit of Work allows to group related activities and operation into single
 * unit. It it can be then completed or aborted as one making the execution
 * consistent.
 * 
 * Depending on the implementation it can rely on some additional frameworks or
 * capabilities to carry on with the execution semantics.
 *
 */
public interface UnitOfWork {

	/**
	 * Initiates this unit of work if not already started. It is safe to call start
	 * multiple times unless the unit has already been completed or aborted.
	 */
	void start();

	/**
	 * Completes this unit of work ensuring all awaiting work is invoked.
	 */
	void end();

	/**
	 * Aborts this unit of work and ignores any awaiting work.
	 */
	void abort();

	/**
	 * Intercepts work that should be done as part of this unit of work.
	 * 
	 * @param work actual work to be invoked as part of this unit of work.
	 */
	void intercept(WorkUnit work);
}
